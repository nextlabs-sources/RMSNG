package com.nextlabs.rms.manager;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonMyVaultMetadata;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.JsonUser;
import com.nextlabs.common.util.AuthUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.cache.TokenGroupCache;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.dto.repository.SharedNxlFile;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.FileExpiredException;
import com.nextlabs.rms.exception.FileTenantMismatchException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.AllNxl.Status;
import com.nextlabs.rms.hibernate.model.BlackList;
import com.nextlabs.rms.hibernate.model.FavoriteFile;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.SharingRecipientKeyPersonal;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.SharingRecipientProject;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.json.SharedFile;
import com.nextlabs.rms.json.SharedFileResponse;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.mail.Mail;
import com.nextlabs.rms.mail.Sender;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.share.SharePersonalMapper;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

/**
 * @author nnallagatla
 *
 */
public final class SharedFileManager {

    private static final String EQUALS = "=";
    private static final String OR_OPERATION = " OR ";
    public static final String DEFAULT_KEY = "/KPUP6LKTZ0akrIfLBXk7tmFjNF1B+ZlvW9ZnSOFmq4=";
    public static final String PARAM_TRANSACTION_ID = "d";
    public static final String PARAM_HMAC_ID = "c";
    public static final String DEFAULT_POLICY_CONDITION_EMAIL = "user.email";
    public static final String SHARED_FILE_LANDING_PAGE = "/main#/app/viewSharedFile";
    public static final List<String> SORT_FIELDS = Collections.unmodifiableList(Arrays.asList("creationTime", "filePath", "fileName"));
    private static final String TEMPLATE_SHARE_URL = "shareURL";
    private static final String TEMPLATE_OWNER_EMAIL = "ownerEmail";
    private static final String TEMPLATE_OWNER_FULLNAME = "ownerFullName";
    private static final String TEMPLATE_FILENAME = "fileName";
    private static final String TEMPLATE_ATTACHMENT_FILENAME = "attachmentFilename";
    private static final String TEMPLATE_ATTACHMENT = "attachment";
    private static final String TEMPLATE_COMMENT = "comment";
    private static final String TEMPLATE_COMMENT_PREFIX = "commentPrefix";
    private static final String TEMPLATE_VALIDITY = "validity";
    private static final String GET_PROFILE_URL = "/rs/usr/v2/profile/";

    public static final String NO_WATERMARK = "NO_WATERMARK";

    private SharedFileManager() {
    }

    public static String getTransactionCode(String transactionId) throws GeneralSecurityException {
        return Hex.toHexString(AuthUtils.hmac(transactionId.getBytes(StandardCharsets.UTF_8), SharedFileManager.DEFAULT_KEY.getBytes(StandardCharsets.UTF_8), null));
    }

    public static Long getTotalFileSharedByUser(DbSession session, int userId, List<Criterion> search, String filter) {
        return SharedFileDAO.getTotalFileSharedByUser(session, userId, search, filter);
    }

    public static List<SharedNxlFile> getFavoriteFilesByUser(DbSession session, int sharingUserId, int page, int size,
        List<Criterion> search, List<Order> orders,
        String filterOptions, List<FavoriteFile> pagedFavFiles) {
        Map<String, FavoriteFile> favMap = pagedFavFiles.stream().collect(Collectors.toMap(FavoriteFile::getFilePathId, Function.identity()));

        List<RepoItemMetadata> sharedFileList = SharedFileDAO.getSharedFilesBySharingUserInBuiltInRepo(session, sharingUserId, 0, 0, search, orders, filterOptions);

        List<RepoItemMetadata> filteredList = sharedFileList.stream().filter(f -> favMap.containsKey(f.getFilePath())).collect(Collectors.toList());

        // Handle pagination manually
        List<RepoItemMetadata> pagedList;
        if (page > 0 && size > 0) {
            int startIndex = size * (page - 1);
            int endIndex = size * page;

            // Return empty list if index >= size of list
            if (startIndex >= filteredList.size()) {
                pagedList = new ArrayList<RepoItemMetadata>();
            } else {
                if (endIndex > filteredList.size()) {
                    endIndex = filteredList.size();
                }
                pagedList = filteredList.subList(startIndex, endIndex);
            }
        } else {
            pagedList = filteredList;
        }

        return toSharedNxlFileFromRepoItem(session, pagedList);
    }

    public static List<SharedNxlFile> getFilesSharedByUser(DbSession session, int sharingUserId, int page, int size,
        List<Criterion> search, List<Order> orders,
        String filterOptions) {
        List<RepoItemMetadata> sharedFileList = SharedFileDAO.getSharedFilesBySharingUserInBuiltInRepo(session, sharingUserId, page, size, search, orders, filterOptions);
        return toSharedNxlFileFromRepoItem(session, sharedFileList);
    }

    public static List<SharedFile> getSharedFileArray(DbSession session, String repoId, List<SharedNxlFile> list) {
        Set<String> favoritedSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, repoId, RepoConstants.MY_VAULT_FOLDER_PATH_ID);
        List<SharedFile> sfArr = new ArrayList<SharedFile>();
        if (list != null) {
            for (SharedNxlFile dto : list) {
                SharedFile sFile = new SharedFile();
                sFile.setSharedOn(dto.getCreationTime().getTime());
                sFile.setName(dto.getFileName());
                sFile.setFileType(RepositoryFileUtil.getOriginalFileExtension(dto.getFileName()));
                sFile.setDuid(dto.getDuid());
                Set<String> userEmailList = dto.getShareWith();
                sFile.setSharedWith(userEmailList);
                sFile.setRevoked(dto.getStatus() == Status.REVOKED);
                sFile.setFavorited(favoritedSet.contains(dto.getFilePath()));
                sfArr.add(sFile);
                sFile.setDeleted(dto.isDeleted());
                sFile.setShared(dto.isShared());
                sFile.setPathId(dto.getFilePath());
                String filePathDisplay = StringUtils.substringAfter(dto.getFilePathDisplay(), RepoConstants.MY_VAULT_FOLDER_PATH_DISPLAY);
                sFile.setPathDisplay(filePathDisplay);
                sFile.setSize(dto.getSize());
                sFile.setRights(Rights.toStrings(dto.getRights()));
                sFile.setRepoId(dto.getRepoId());
                Map<String, String> metadataMap = GsonUtils.GSON.fromJson(dto.getCustomMetadata(), GsonUtils.GENERIC_MAP_TYPE);
                sFile.setCustomMetadata(metadataMap);
            }
        }
        return sfArr;
    }

    private static SharedFileDTO toSharingTransactionFile(DbSession session, SharingTransaction txn) {
        StoreItem item = new SharePersonalMapper().getStoreItemByTransaction(txn.getId());
        Repository repository = null;
        if (item.getRepoId() != null) {
            repository = session.get(Repository.class, item.getRepoId());
        }
        SharedFileDTO dto = new SharedFileDTO();
        if (repository != null) {
            String providerId = repository.getProviderId();
            if (StringUtils.hasText(providerId)) {
                StorageProvider storageProvider = session.get(StorageProvider.class, providerId);
                if (storageProvider != null) {
                    ServiceProviderType type = ServiceProviderType.getByOrdinal(storageProvider.getType());
                    dto.setRepositoryType(type.name());
                }
            }
            dto.setRepositoryId(repository.getId());
            dto.setRepositoryName(repository.getName());
        }
        dto.setCreatedDate(txn.getCreationTime());
        dto.setUpdatedDate(txn.getCreationTime());
        dto.setDeviceType(DeviceType.values()[txn.getDeviceType()].name());
        dto.setDeviceId(txn.getDeviceId());
        dto.setFilePath(item.getFilePathDisplay());
        dto.setFilePathId(item.getFilePath());
        dto.setFileName("");
        dto.setTransactionID(txn.getId());
        dto.setDocumentUID(txn.getMySpaceNxl().getDuid());
        dto.setRevoked(txn.getStatus() == 1);
        dto.setUserId(txn.getUser().getId());
        return dto;
    }

    private static SharedNxlFile toSharedNxlFile(DbSession session, AllNxl sharedNxl) {
        SharedNxlFile file = new SharedNxlFile();
        file.setCreationTime(sharedNxl.getCreationTime());
        file.setLastModified(sharedNxl.getLastModified());
        file.setFileName(sharedNxl.getFileName());
        file.setDuid(sharedNxl.getDuid());
        file.setUserId(sharedNxl.getUser().getId());
        file.setStatus(sharedNxl.getStatus());
        file.setPolicy(sharedNxl.getPolicy());
        int permissions = sharedNxl.getPermissions();
        Rights[] rights = Rights.fromInt(permissions);
        file.setRights(rights);

        String duid = sharedNxl.getDuid();
        List<SharingRecipientPersonal> recipientList = SharedFileDAO.getSharingRecipients(session, duid);
        Set<String> recipients = new HashSet<String>(recipientList != null ? recipientList.size() : 0);
        if (recipientList != null && !recipientList.isEmpty()) {
            for (SharingRecipientPersonal recipient : recipientList) {
                String email = recipient.getId().getEmail();
                recipients.add(email);
            }
        }
        file.setShareWith(recipients);
        return file;
    }

    private static SharedNxlFile toSharedNxlFileFromRepoItem(RepoItemMetadata repoItem,
        Set<String> recipientList) {
        SharedNxlFile file = new SharedNxlFile();
        file.setDuid(repoItem.getNxl().getDuid());
        file.setUserId(repoItem.getNxl().getUser().getId());
        String fileName = StringUtils.substringAfterLast(repoItem.getFilePathDisplay(), "/");
        AllNxl sharedNxl = repoItem.getNxl();
        file.setStatus(sharedNxl.getStatus());
        file.setFileName(fileName);
        file.setCreationTime(sharedNxl.getCreationTime());
        file.setLastModified(sharedNxl.getLastModified());
        int permissions = sharedNxl.getPermissions();
        Rights[] rights = Rights.fromInt(permissions);
        file.setRights(rights);
        file.setPolicy(repoItem.getNxl().getPolicy());
        file.setShareWith(recipientList);
        file.setSize(repoItem.getSize());
        file.setRepoId(repoItem.getRepository().getId());
        file.setFilePath(repoItem.getFilePath());
        file.setFilePathDisplay(repoItem.getFilePathDisplay());
        file.setCustomMetadata(repoItem.getCustomUserMetatdata());
        file.setDeleted(repoItem.isDeleted());
        file.setShared(repoItem.getNxl().isShared());
        return file;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> getRecipientMap(DbSession session, List<RepoItemMetadata> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> duidList = new ArrayList<String>();
        for (RepoItemMetadata repoItem : list) {
            duidList.add(repoItem.getNxl().getDuid());
        }
        Map<String, Set<String>> recipientMap = new HashMap<String, Set<String>>();
        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        criteria.add(Restrictions.in("id.duid", duidList));
        List<SharingRecipientPersonal> recipientList = criteria.list();
        for (SharingRecipientPersonal recipient : recipientList) {
            Set<String> recipients = recipientMap.get(recipient.getId().getDuid());
            if (null == recipients) {
                recipients = new HashSet<String>();
                recipients.add(recipient.getId().getEmail());
                recipientMap.put(recipient.getId().getDuid(), recipients);
            } else {
                recipients.add(recipient.getId().getEmail());
            }
        }
        return recipientMap;
    }

    private static List<SharedNxlFile> toSharedNxlFileFromRepoItem(DbSession session, List<RepoItemMetadata> list) {
        List<SharedNxlFile> result = new ArrayList<SharedNxlFile>(list != null ? list.size() : 0);
        if (list != null && !list.isEmpty()) {
            Map<String, Set<String>> recipientMap = getRecipientMap(session, list);
            for (RepoItemMetadata repoItem : list) {
                result.add(toSharedNxlFileFromRepoItem(repoItem, recipientMap.get(repoItem.getNxl().getDuid())));
            }
        }
        return result;
    }

    public static void shareFile(String rmsBaseURL, SharedFileDTO sharedFile, String baseURL,
        SharedFileResponse response, String efsId, boolean isFileAttached, String watermark, String expiry,
        HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, RMSException,
            GeneralSecurityException, UnauthorizedOperationException, FileAlreadyRevokedException,
            VaultStorageExceededException, com.nextlabs.rms.repository.exception.FileNotFoundException,
            FileExpiredException, FileTenantMismatchException {
        String path = rmsBaseURL + "/rs/share/repository";
        int permissions = sharedFile.getGrantedRights();
        JsonSharing sharing = new JsonSharing();
        sharing.setDuid(sharedFile.getDocumentUID());
        sharing.setFileName(sharedFile.getFileName());
        sharing.setFilePath(sharedFile.getFilePath());//Use this to get displayName
        sharing.setFilePathId(sharedFile.getFilePathId());
        sharing.setPermissions(permissions);
        sharing.setRepositoryId(sharedFile.getRepositoryId());
        sharing.setMembershipId(sharedFile.getOwner());
        sharing.setWatermark(watermark);
        sharing.setExpiry(GsonUtils.GSON.fromJson(expiry, JsonExpiry.class));
        sharing.setEfsId(efsId);
        if (sharedFile.getSourceProjectId() != null && sharedFile.getSourceProjectId() != 0) {
            sharing.setFromSpace(SHARESPACE.PROJECTSPACE);
            sharing.setToSpace(SHARESPACE.PROJECTSPACE);
            sharing.setProjectId(sharedFile.getSourceProjectId());
            List<JsonRecipient> recipientProjects = new ArrayList<JsonSharing.JsonRecipient>();
            List<String> sharedWithProject = sharedFile.getSharedWithProject();
            for (String projectId : sharedWithProject) {
                JsonRecipient recipientProject = new JsonRecipient();
                recipientProject.setDestType(SHARESPACE.PROJECTSPACE);
                recipientProject.setProjectId(Integer.parseInt(projectId));
                recipientProjects.add(recipientProject);
            }
            sharing.setRecipients(recipientProjects);
        } else {
            sharing.setFromSpace(SHARESPACE.MYSPACE);
            sharing.setToSpace(SHARESPACE.MYSPACE);
            Set<String> shareWith = (Set<String>)sharedFile.getShareWith();
            List<JsonRecipient> recipients = new ArrayList<JsonRecipient>(shareWith.size());
            for (String s : shareWith) {
                JsonRecipient recipient = new JsonRecipient();
                recipient.setEmail(s);
                recipients.add(recipient);
            }
            sharing.setRecipients(recipients);
            sharing.setUserConfirmedFileOverwrite(sharedFile.isUserConfirmedFileOverWrite());
        }

        String filePath = sharedFile.getFilePath();
        if (StringUtils.hasText(filePath)) {
            String[] displayNameArr = filePath.split("/");
            sharing.setDisplayName(displayNameArr[displayNameArr.length - 1]);
        }
        sharing.setComment(sharedFile.getComment());
        JsonRequest req = new JsonRequest();
        req.addParameter("asAttachment", String.valueOf(isFileAttached));
        //TODO update checksum after the checksum logic in sharing API is finalized.
        req.addParameter("sharedDocument", sharing);

        Properties prop = new Properties();
        Enumeration<String> headers = servletRequest.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            prop.setProperty(headerName, servletRequest.getHeader(headerName));
        }
        prop.setProperty(HTTPUtil.HEADER_X_FORWARDED_FOR, HTTPUtil.getRemoteAddress(servletRequest));
        prop.setProperty("userId", String.valueOf(sharedFile.getUserId()));
        prop.setProperty("ticket", String.valueOf(sharedFile.getTicket()));
        prop.setProperty("clientId", sharedFile.getClientId());
        if (StringUtils.hasText(sharedFile.getDeviceId())) {
            try {
                prop.setProperty("deviceId", URLEncoder.encode(sharedFile.getDeviceId(), "UTF-8"));
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
        }
        if (sharedFile.getPlatformId() != null) {
            prop.setProperty("platformId", String.valueOf(sharedFile.getPlatformId()));
        }

        String ret = RestClient.post(path, prop, req.toJson(), RepoConstants.CONNECTION_TIMEOUT, RepoConstants.READ_TIMEOUT);
        JsonResponse resp = JsonResponse.fromJson(ret);

        if (resp.hasError()) {
            if (resp.getStatusCode() == 403) {
                response.setDuid(resp.getResultAsString("duid"));
                throw new UnauthorizedOperationException(resp.getMessage());
            } else if (resp.getStatusCode() == 4001) {
                throw new FileAlreadyRevokedException(resp.getMessage());
            } else if (resp.getStatusCode() == 4003) {
                throw new FileExpiredException(resp.getMessage());
            } else if (resp.getStatusCode() == 6002) {
                throw new VaultStorageExceededException();
            } else if (resp.getStatusCode() == 404) {
                throw new com.nextlabs.rms.repository.exception.FileNotFoundException(resp.getMessage());
            } else if (resp.getStatusCode() == 405) {
                throw new FileTenantMismatchException(resp.getMessage());
            } else {
                throw new RMSException(resp.getMessage());
            }
        }
        response.setFileName(resp.getResultAsString("fileName"));
        response.setDuid(resp.getResultAsString("duid"));
        response.setFilePathId(resp.getResultAsString("filePathId"));
        response.setValidity(resp.getResult("expiry", JsonExpiry.class));
        if (resp.getResultAsString("transactionId") != null) {
            response.setSharedLink(baseURL + getSharingURLQueryString(resp.getResultAsString("transactionId")));
        }
        List<String> newSharedList = resp.getResult("newSharedList", GsonUtils.GENERIC_LIST_TYPE);
        if (newSharedList != null && !newSharedList.isEmpty()) {
            if (sharedFile.getSourceProjectId() != null && sharedFile.getSourceProjectId() != 0) {
                response.setNewSharedProjects(newSharedList);
            } else {
                response.setNewSharedEmailsStr(StringUtils.join(newSharedList, ", "));
            }
        }
        List<String> alreadySharedList = resp.getResult("alreadySharedList", GsonUtils.GENERIC_LIST_TYPE);
        if (alreadySharedList != null && !alreadySharedList.isEmpty()) {
            if (sharedFile.getSourceProjectId() != null && sharedFile.getSourceProjectId() != 0) {
                response.setAlreadySharedProjects(alreadySharedList);
            } else {
                response.setAlreadySharedEmailStr(StringUtils.join(alreadySharedList, ", "));
            }
        }
    }

    public static void sendEmailToUsers(SharedFileDTO dto, String shareURL, String recipient,
        String sharerDisplayName, String baseUrl, File attachment) throws IOException {
        Locale locale = Locale.getDefault();
        String attachmentContent = null;
        if (attachment != null) {
            if (!attachment.exists() || !attachment.isFile()) {
                throw new FileNotFoundException(attachment.getName() + " is not present");
            }
            try (InputStream is = new FileInputStream(attachment)) {
                attachmentContent = Base64.encodeBase64String(IOUtils.toByteArray(is));
            }
        }
        Properties prop = new Properties();
        prop.setProperty(TEMPLATE_OWNER_EMAIL, dto.getSharingUserEmail());
        prop.setProperty(TEMPLATE_OWNER_FULLNAME, sharerDisplayName);
        prop.setProperty(TEMPLATE_FILENAME, dto.getFileName());
        prop.setProperty(Mail.KEY_RECIPIENT, recipient);
        prop.setProperty(Mail.BASE_URL, baseUrl);
        if (StringUtils.hasText(dto.getComment())) {
            prop.setProperty(TEMPLATE_COMMENT, "<p><b>\"</b>" + StringEscapeUtils.escapeHtml4(dto.getComment()) + "<b>\"</b></p>");
            prop.setProperty(TEMPLATE_COMMENT_PREFIX, RMSMessageHandler.getClientString(TEMPLATE_COMMENT_PREFIX));
        } else {
            prop.setProperty(TEMPLATE_COMMENT, "");
            prop.setProperty(TEMPLATE_COMMENT_PREFIX, RMSMessageHandler.getClientString("noComment"));
        }
        if (StringUtils.hasText(dto.getExpiryStr())) {
            prop.setProperty(TEMPLATE_VALIDITY, dto.getExpiryStr());
        } else {
            prop.setProperty(TEMPLATE_VALIDITY, RMSMessageHandler.getClientString("expiry.never"));
        }
        if (attachment != null) {
            prop.setProperty(TEMPLATE_ATTACHMENT, attachmentContent);
            prop.setProperty(TEMPLATE_ATTACHMENT_FILENAME, dto.getFileName());
            Sender.send(prop, "shareWithAttachment", locale);
        } else {
            prop.setProperty(TEMPLATE_SHARE_URL, shareURL);
            Sender.send(prop, "share", locale);
        }
    }

    public static String getBaseShareURL(HttpServletRequest request, String loginTenant)
            throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        if (loginTenant != null) {
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, loginTenant);
                builder.append(TokenGroupCache.lookupTokenGroup(tenant.getName()));
            }
        } else {
            builder.append(HTTPUtil.getURI(request));
        }
        builder.append(SHARED_FILE_LANDING_PAGE);
        return builder.toString();
    }

    /**
     * This method takes sharing transactionId as parameter and returns the
     * queryString that should be used as part of sharing URL
     *
     * @param transactionId
     * @return
     * @throws GeneralSecurityException
     */
    public static String getSharingURLQueryString(String transactionId) throws GeneralSecurityException {
        String mac = getMAC();
        String calculatedHash = Hex.toHexString(AuthUtils.hmac(transactionId.getBytes(StandardCharsets.UTF_8), mac.getBytes(StandardCharsets.UTF_8), null));
        StringBuilder queryString = new StringBuilder("?").append(PARAM_TRANSACTION_ID).append(EQUALS).append(transactionId).append("&").append(PARAM_HMAC_ID).append(EQUALS).append(calculatedHash);
        return queryString.toString();
    }

    public static boolean isValidURLAccess(String transactionId, String hmac) throws GeneralSecurityException {
        if (!StringUtils.hasText(transactionId)) {
            throw new ValidateException(400, "Transaction ID cannot be null/empty");
        }

        if (!StringUtils.hasText(hmac)) {
            return false;
        }
        String mac = getMAC();
        String calculatedHash = Hex.toHexString(AuthUtils.hmac(transactionId.getBytes(StandardCharsets.UTF_8), mac.getBytes(StandardCharsets.UTF_8), null));
        return StringUtils.equals(calculatedHash, hmac);
    }

    public static String getMAC() {
        return DEFAULT_KEY;
    }

    public static SharedFileDTO lookupSharedFileById(String sharedFileId) {
        DbSession session = DbSession.newSession();
        try {
            SharingTransaction sharedFileDO = session.get(SharingTransaction.class, sharedFileId);
            return sharedFileDO != null ? toSharingTransactionFile(session, sharedFileDO) : null;
        } finally {
            session.close();
        }
    }

    public static SharingTransaction lookupSharedFileByTransactionId(DbSession session,
        String sharedFileTransactionId) {
        return session.get(SharingTransaction.class, sharedFileTransactionId);
    }

    public static AllNxl lookupSharedFileByDuid(DbSession session,
        int userId, String sharedFileDuid) {
        Criteria criteria = session.createCriteria(AllNxl.class);
        criteria.add(Restrictions.idEq(sharedFileDuid));
        criteria.add(Restrictions.eq("user.id", userId));
        return (AllNxl)criteria.uniqueResult();
    }

    public static List<String> getUsersFromPQL(String pql) {
        List<String> userEmails = new ArrayList<String>();
        String[] conditions = pql.substring(1, pql.length() - 1).split(OR_OPERATION);
        for (String condition : conditions) {
            String[] parts = condition.split(EQUALS);
            if (parts[0].contains(DEFAULT_POLICY_CONDITION_EMAIL)) {
                userEmails.add(parts[1].substring(1, parts[1].length() - 1));
            }
        }
        return userEmails;
    }

    public static SharedFileDTO getSharedFileByTransactionID(String transactionId) {
        DbSession session = DbSession.newSession();
        try {
            SharingTransaction sharedFile = lookupSharedFileByTransactionId(session, transactionId);
            return sharedFile != null ? toSharingTransactionFile(session, sharedFile) : null;
        } finally {
            session.close();
        }
    }

    public static boolean isBlacklisted(DbSession session, String duid) {
        return session.get(BlackList.class, duid) != null;
    }

    public static SharedNxlFile getSharedFileByDuid(int userId, String duid) {
        DbSession session = DbSession.newSession();
        try {
            AllNxl sharedFile = lookupSharedFileByDuid(session, userId, duid);
            return sharedFile != null ? toSharedNxlFile(session, sharedFile) : null;
        } finally {
            session.close();
        }
    }

    public static SharingTransaction getSharingTransactionByDuid(String duid) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(SharingTransaction.class);
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.eq("mySpaceNxl.duid", duid));
            or.add(Restrictions.eq("projectNxl.duid", duid));
            or.add(Restrictions.eq("ewsNxl.duid", duid));
            criteria.add(or);
            List<SharingTransaction> transactions = criteria.list();
            return transactions == null || transactions.isEmpty() ? null : transactions.get(0);
        }
    }

    public static String getMembership(String path, RMSUserPrincipal userPrincipal) throws IOException {
        List<JsonMembership> memberships = getMemberships(path, userPrincipal);
        if (memberships == null || memberships.isEmpty()) {
            return null;
        }
        String tenantId = userPrincipal.getTenantId();
        if (StringUtils.hasText(tenantId)) {
            for (JsonMembership membership : memberships) {
                if (tenantId.equals(membership.getTenantId())) {
                    return membership.getId();
                }
            }
            throw new IllegalArgumentException("Invalid tenant");
        }
        return memberships.get(0).getId();
    }

    public static List<JsonMembership> getMemberships(String path, RMSUserPrincipal principal) throws IOException {

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(principal.getUserId()));
        prop.setProperty("ticket", principal.getTicket());
        prop.setProperty("clientId", principal.getClientId());
        prop.setProperty("platformId", String.valueOf(principal.getPlatformId()));

        String respString = RestClient.get(path + GET_PROFILE_URL, prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        JsonUser jsonUser = resp.getExtra(JsonUser.class);
        return jsonUser.getMemberships();
    }

    public static Rights[] toRights(String[] rights) {
        if (rights == null || rights.length == 0) {
            return new Rights[0];
        }
        Set<String> set = new HashSet<String>(Arrays.asList(rights));
        return Rights.fromStrings(set.toArray(new String[set.size()]));
    }

    public static String[] toString(Rights[] rights) {
        if (rights != null) {
            Set<Rights> set = new HashSet<Rights>(Arrays.asList(rights));
            return Rights.toStrings(set.toArray(new Rights[set.size()]));
        }
        return new String[0];
    }

    public static boolean isOwner(String path, String owner, RMSUserPrincipal principal) throws IOException {
        List<JsonMembership> memberships = getMemberships(path, principal);
        if (memberships != null && !memberships.isEmpty()) {
            for (JsonMembership membership : memberships) {
                if (membership.getId().equals(owner)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static FilePolicy getFilePolicy(File file) {
        try (InputStream is = new FileInputStream(file);
                NxlFile nxl = NxlFile.parse(is)) {
            return DecryptUtil.getFilePolicy(nxl, null);
        } catch (IOException | GeneralSecurityException | NxlException e) {
            return null;
        }
    }

    public static String getFilePolicyStr(File file) {
        FilePolicy filePolicy = getFilePolicy(file);
        if (filePolicy != null) {
            return GsonUtils.GSON.toJson(filePolicy);
        }
        return null;
    }

    public static NxlMetaData getNxlSharableMetadata(File file, String path, RMSUserPrincipal userPrincipal) {
        String duid = null;
        String ownerMembership = null;
        Constants.TokenGroupType tgType = null;
        Map<String, String[]> tags = null;
        boolean owner = false;
        JsonExpiry validity = new JsonExpiry();
        try (InputStream is = new FileInputStream(file);
                NxlFile nxl = NxlFile.parse(is)) {
            ownerMembership = nxl.getOwner();
            duid = nxl.getDuid();
            tags = DecryptUtil.getTags(nxl, null);
            try (DbSession session = DbSession.newSession()) {
                owner = isOwner(path, nxl.getOwner(), userPrincipal);
                Membership membership = session.get(Membership.class, ownerMembership);
                if (membership == null || Constants.TokenGroupType.TOKENGROUP_TENANT != membership.getType()) {
                    NxlMetaData nxlMetaData = new NxlMetaData(duid, ownerMembership, (membership == null ? null : membership.getType()), tags, true, false, owner, validity);
                    nxlMetaData.setTenantMembershipInvalid(true);
                    return nxlMetaData;
                }
                tgType = membership.getType();
                if (SharedFileManager.isBlacklisted(session, duid)) {
                    return new NxlMetaData(duid, ownerMembership, tgType, tags, true, false, owner, validity);
                }
            }
            FilePolicy adhocPolicy = DecryptUtil.getFilePolicy(nxl, null);
            return new NxlMetaData(duid, ownerMembership, tgType, false, owner, adhocPolicy, tags);
        } catch (IOException e) {
            return new NxlMetaData(duid, ownerMembership, tgType, tags, false, false, owner, validity);
        } catch (GeneralSecurityException e) {
            return new NxlMetaData(duid, ownerMembership, tgType, tags, false, false, owner, validity);
        } catch (NxlException e) {
            return new NxlMetaData(duid, ownerMembership, tgType, tags, false, false, owner, validity);
        }
    }

    public static boolean isRecipient(String duid, Collection<String> emails, DbSession session) {
        if (emails == null || emails.isEmpty()) {
            return false;
        }
        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        List<SharingRecipientKeyPersonal> keys = new ArrayList<>(emails.size());
        for (String email : emails) {
            SharingRecipientKeyPersonal key = new SharingRecipientKeyPersonal();
            key.setDuid(duid);
            key.setEmail(email);
            keys.add(key);
        }
        criteria.add(Restrictions.in("id", keys));
        criteria.add(Restrictions.ne("status", 1));
        criteria.setMaxResults(1);
        SharingRecipientPersonal recipient = (SharingRecipientPersonal)criteria.uniqueResult();
        return recipient != null;
    }

    public static boolean isRecipient(String duid, String email, String transactionId, DbSession session) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(transactionId)) {
            return false;
        }

        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        SharingRecipientKeyPersonal key = new SharingRecipientKeyPersonal();
        key.setDuid(duid);
        key.setEmail(email);
        criteria.add(Restrictions.idEq(key));
        criteria.add(Restrictions.eq("transaction.id", transactionId));
        criteria.setMaxResults(1);
        SharingRecipientPersonal recipient = (SharingRecipientPersonal)criteria.uniqueResult();
        return recipient != null;
    }

    public static boolean isRecipientProject(String duid, int projectId, DbSession session) {
        Criteria criteria = session.createCriteria(SharingRecipientProject.class);
        criteria.add(Restrictions.eq("id.projectId", projectId));
        criteria.add(Restrictions.eq("id.duid", duid));
        criteria.add(Restrictions.ne("status", 1));
        criteria.setMaxResults(1);
        SharingRecipientProject recipient = (SharingRecipientProject)criteria.uniqueResult();
        return recipient != null;
    }

    public static SharedFileDTO generateSharedFileDTO(String deviceId, String deviceType, String documentUID,
        String filePath, String filePathId, String fileName, String repositoryId, String repositoryName,
        String tenantId, int userId, String ticket, String sharingUserEmail, Set<?> shareWith,
        Rights[] grantedRights, Date createdDate, boolean isRevoked, String owner, String clientId,
        Integer platformId, String comment, String policy) {
        return generateSharedFileDTO(deviceId, deviceType, documentUID, filePath, filePathId, fileName, repositoryId, repositoryName, tenantId, userId, ticket, sharingUserEmail, shareWith, grantedRights, createdDate, isRevoked, owner, clientId, platformId, comment, policy, false);
    }

    public static SharedFileDTO generateSharedFileDTO(String deviceId, String deviceType, String documentUID,
        String filePath, String filePathId, String fileName, String repositoryId, String repositoryName,
        String tenantId, int userId, String ticket, String sharingUserEmail, Set<?> shareWith,
        Rights[] grantedRights, Date createdDate, boolean isRevoked, String owner, String clientId,
        Integer platformId, String comment, String policy, boolean userConfirmedFileOverWrite) {
        SharedFileDTO dto = new SharedFileDTO();
        dto.setDeviceId(deviceId);
        dto.setDeviceType(deviceType);
        dto.setDocumentUID(documentUID);
        dto.setFilePath(filePath);
        dto.setFilePathId(filePathId);
        dto.setFileName(fileName);
        dto.setRepositoryId(repositoryId);
        dto.setRepositoryName(repositoryName);
        dto.setTenantId(tenantId);
        dto.setUserId(userId);
        dto.setSharingUserEmail(sharingUserEmail);
        dto.setShareWith(shareWith);
        dto.setGrantedRights(Rights.toInt(grantedRights));
        dto.setCreatedDate(createdDate);
        dto.setUpdatedDate(createdDate);
        dto.setRevoked(isRevoked);
        dto.setOwner(owner);
        dto.setTicket(ticket);
        dto.setClientId(clientId);
        dto.setPlatformId(platformId);
        dto.setComment(comment);
        dto.setPolicy(policy);
        dto.setUserConfirmedFileOverWrite(userConfirmedFileOverWrite);
        return dto;
    }

    @SuppressWarnings("unchecked")
    public static JsonMyVaultMetadata getMyVaultMetadata(int userId, String duid, String filePathId) {
        try (DbSession session = DbSession.newSession()) {
            DetachedCriteria dc = DetachedCriteria.forClass(RepoItemMetadata.class, "r");
            DetachedCriteria nxlDc = dc.createCriteria("nxl", "n", JoinType.INNER_JOIN);
            dc.add(Restrictions.eq("n.user.id", userId));
            dc.add(Restrictions.eq("filePath", filePathId));
            nxlDc.add(Restrictions.eq("duid", duid));
            Criteria criteria = dc.getExecutableCriteria(session.getSession());
            List<RepoItemMetadata> list = criteria.list();
            if (list.isEmpty()) {
                return null;
            }
            RepoItemMetadata repoItemMetadata = list.get(0);
            AllNxl nxl = repoItemMetadata.getNxl();
            String policy = nxl.getPolicy();
            Rights[] rights = Rights.fromInt(nxl.getPermissions());
            JsonExpiry validity = new JsonExpiry();
            if (StringUtils.hasText(policy)) {
                FilePolicy filePolicy = GsonUtils.GSON.fromJson(policy, FilePolicy.class);
                rights = AdhocEvalAdapter.evaluate(filePolicy, true).getRights();
                validity = AdhocEvalAdapter.getFirstPolicyExpiry(filePolicy);
            }
            String[] rightsStr = Rights.toStrings(rights);
            final String fileName = StringUtils.substringAfterLast(repoItemMetadata.getFilePathDisplay(), "/");
            List<SharingRecipientPersonal> recipients = SharedFileDAO.getSharingRecipients(session, userId, nxl);
            JsonMyVaultMetadata metadata = new JsonMyVaultMetadata();
            metadata.setFileName(fileName);
            metadata.setRights(rightsStr);
            metadata.setDeleted(repoItemMetadata.isDeleted());
            metadata.setRevoked(nxl.getStatus() == Status.REVOKED);
            metadata.setShared(nxl.isShared());
            metadata.setProtectedOn(nxl.getCreationTime().getTime());
            metadata.setValidity(validity);
            metadata.setProtectionType(ProtectionType.ADHOC.ordinal());
            if (recipients != null && !recipients.isEmpty()) {
                Set<String> emails = new HashSet<>(recipients.size());
                for (SharingRecipientPersonal recipient : recipients) {
                    emails.add(recipient.getId().getEmail());
                }
                metadata.setRecipients(emails);
            } else {
                metadata.setRecipients(Collections.<String> emptySet());
            }
            return metadata;
        }
    }

    public static String getExpiryString(JsonExpiry expiry) {
        if (expiry == null) {
            return null;
        }
        String expiryString = null;
        if (expiry.getStartDate() != null && expiry.getEndDate() != null) {
            expiryString = RMSMessageHandler.getClientString("expiry.date.range", formatDate(expiry.getStartDate()), formatDate(expiry.getEndDate()));
        } else if (expiry.getEndDate() != null) {
            expiryString = RMSMessageHandler.getClientString("expiry.until", formatDate(expiry.getEndDate()));
        } else {
            expiryString = RMSMessageHandler.getClientString("expiry.never");
        }
        return expiryString;
    }

    private static String formatDate(long date) {
        LocalDate localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        return localDate.format(formatter);
    }

    public static SharingTransaction getSharingTransactionByTransactionId(String transactionId) {
        SharingTransaction sharingTransaction = null;
        DbSession session = DbSession.newSession();
        try {
            sharingTransaction = lookupSharedFileByTransactionId(session, transactionId);
        } finally {
            session.close();
        }
        return sharingTransaction;
    }

    public static boolean isRecipientProject(String duid, int projectId) {
        try (DbSession session = DbSession.newSession()) {
            return isRecipientProject(duid, projectId, session);
        }
    }

    public static NxlMetaData getProjectFileNxlSharableMetadata(File file, String path,
        RMSUserPrincipal userPrincipal) {
        String duid = null;
        String ownerMembership = null;
        Map<String, String[]> tags = null;
        try (InputStream is = new FileInputStream(file);
                NxlFile nxl = NxlFile.parse(is)) {
            ownerMembership = nxl.getOwner();
            duid = nxl.getDuid();
            tags = DecryptUtil.getTags(nxl, null);
            FilePolicy adhocPolicy = DecryptUtil.getFilePolicy(nxl, null);
            return new NxlMetaData(duid, ownerMembership, Constants.TokenGroupType.TOKENGROUP_PROJECT, false, false, adhocPolicy, tags);
        } catch (IOException | GeneralSecurityException | NxlException e) {
            return null;
        }
    }
}
