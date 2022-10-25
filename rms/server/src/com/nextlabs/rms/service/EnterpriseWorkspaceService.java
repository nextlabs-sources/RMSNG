package com.nextlabs.rms.service;

import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonEnterpriseSpaceFile;
import com.nextlabs.common.shared.JsonEnterpriseSpaceFileInfo;
import com.nextlabs.common.shared.JsonEnterpriseSpaceFileList;
import com.nextlabs.common.shared.JsonEnterpriseSpaceMember;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.EnterpriseSpaceStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryEnterprise;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.IRMSRepositorySearcher;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.RMSEnterpriseDBSearcherImpl;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.TenantMgmt;
import com.nextlabs.rms.rs.UserMgmt;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public final class EnterpriseWorkspaceService {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String SEARCH_FIELD_FILENAME = "name";
    public static final Long ENTERPRISE_SPACE_DEFAULT_QUOTA = 26843545600L;
    public static final String ENTERPRISE_SPACE_QUOTA = "quota";

    public static boolean isPathExist(String tenantId, String searchPath) {
        boolean result = false;
        try {
            IRMSRepositorySearcher searcher = new RMSEnterpriseDBSearcherImpl();
            result = searcher.pathExists(tenantId, searchPath);
        } catch (Throwable e) {
            LOGGER.error("Error occurred while deleting files in EnterpriseWS under {} : {}", tenantId, e.getMessage(), e);
        }
        return !result;
    }

    public static JsonEnterpriseSpaceFileList getJsonFileList(DbSession session, String tenantId,
        Integer page,
        Integer size, List<Order> orders, String parentPath, List<String> searchFieldList, String searchString) {
        JsonEnterpriseSpaceFileList result = new JsonEnterpriseSpaceFileList();
        long totalFiles = getTotalFiles(session, tenantId, parentPath, searchFieldList, searchString);
        result.setTotalFiles(totalFiles);
        List<JsonEnterpriseSpaceFile> fileInfo = totalFiles > 0 ? getFileInformation(session, tenantId, page, size, orders, parentPath, searchString) : Collections.emptyList();
        result.setFiles(fileInfo);
        return result;
    }

    private static long getTotalFiles(DbSession session, String tenantId, String parentPath,
        List<String> searchFieldList, String searchString) {
        Criteria criteria = getEnterpriseSpaceItemCriteria(session, tenantId, searchString, parentPath, searchFieldList);
        criteria.setProjection(Projections.rowCount());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    private static Criteria getEnterpriseSpaceItemCriteria(DbSession session, String tenantId, String searchString,
        String parentPath, List<String> searchFieldList) {
        DetachedCriteria dc = getEnterpriseSpaceItemDetachedCriteria(tenantId, searchString, parentPath);
        if (searchFieldList != null && !searchFieldList.isEmpty() && StringUtils.hasText(searchString)) {
            Disjunction disjunction = Restrictions.disjunction();
            for (String searchField : searchFieldList) {
                if (StringUtils.equals(searchField, SEARCH_FIELD_FILENAME)) {
                    disjunction.add(EscapedLikeRestrictions.ilike("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
                }
            }
            dc.add(disjunction);
        }
        return dc.getExecutableCriteria(session.getSession());
    }

    private static DetachedCriteria getEnterpriseSpaceItemDetachedCriteria(String tenantId, String searchString,
        String parentPath) {
        DetachedCriteria dc = DetachedCriteria.forClass(EnterpriseSpaceItem.class, "p");
        dc.add(Restrictions.eq("tenant.id", tenantId));
        if (!StringUtils.hasText(searchString)) {
            if (StringUtils.hasText(parentPath)) {
                dc.add(Restrictions.eq("fileParentPath", parentPath));
            } else {
                dc.add(Restrictions.eq("directory", false));
            }
        }
        return dc;
    }

    @SuppressWarnings("unchecked")
    private static List<JsonEnterpriseSpaceFile> getFileInformation(DbSession session, String tenantId, Integer page,
        Integer size, List<Order> orders, String parentPath, String searchString) {
        DetachedCriteria dc = getEnterpriseSpaceItemDetachedCriteria(tenantId, searchString, parentPath);
        if (StringUtils.hasText(searchString)) {
            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add(EscapedLikeRestrictions.ilike("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
            dc.add(disjunction);
        }
        dc.setFetchMode("uploader", FetchMode.JOIN);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                dc.addOrder(order);
            }
        }
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        if (size > 0) {
            criteria.setMaxResults(size);
            criteria.setFirstResult((page - 1) * size);
        }
        List<EnterpriseSpaceItem> list = criteria.list();
        List<JsonEnterpriseSpaceFile> result = new ArrayList<>(list.size());
        for (EnterpriseSpaceItem item : list) {
            User uploadUser = item.getUploader();
            User lastUser = item.getLastModifiedUser();
            JsonEnterpriseSpaceMember uploader = new JsonEnterpriseSpaceMember();
            uploader.setUserId(uploadUser.getId());
            uploader.setDisplayName(uploadUser.getDisplayName());
            uploader.setEmail(uploadUser.getEmail());
            JsonEnterpriseSpaceMember lastModifiedUser = null;
            if (lastUser != null) {
                lastModifiedUser = new JsonEnterpriseSpaceMember();
                lastModifiedUser.setUserId(lastUser.getId());
                lastModifiedUser.setDisplayName(lastUser.getDisplayName());
                lastModifiedUser.setEmail(lastUser.getEmail());
            }
            JsonEnterpriseSpaceFile file = new JsonEnterpriseSpaceFile();
            file.setCreationTime(item.getCreationTime().getTime());
            file.setSize(item.getSize());
            file.setFolder(item.isDirectory());
            Date lastModified = item.getLastModified();
            if (lastModified != null) {
                file.setLastModified(lastModified.getTime());
            }
            file.setId(item.getId());
            file.setDuid(item.getDuid());
            file.setPathDisplay(item.getFilePathDisplay());
            file.setPathId(item.getFilePath());
            if (!item.isDirectory()) {
                file.setName(StringUtils.substringAfterLast(item.getFilePathDisplay(), "/"));
                file.setFileType(RepositoryFileUtil.getOriginalFileExtension(file.getName()));
            } else {
                String filePathDisplay = item.getFilePathDisplay();
                String fileName = getFileName(filePathDisplay);
                file.setName(fileName);
            }
            file.setLastModifiedUser(lastModifiedUser);
            file.setUploader(uploader);
            result.add(file);
        }
        return result;
    }

    private static String getFileName(String filePathDisplay) {
        String fileName;
        if (filePathDisplay.endsWith("/")) { //to display correct folder name on the ui create folder api path is /test whereas list files api is /test/
            fileName = StringUtils.substringAfterLast(filePathDisplay.substring(0, filePathDisplay.length() - 1), "/");
        } else {
            fileName = StringUtils.substringAfterLast(filePathDisplay, "/");
        }
        return fileName;
    }

    public static String getFileNameWithoutNXL(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return fileName;
        }
        if (!fileName.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            return fileName;
        }
        int index = fileName.toLowerCase().lastIndexOf(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        return fileName.substring(0, index);
    }

    public static Map<String, Long> getEnterpriseSpaceStatus(DbSession session, String tenantId) {
        Tenant tenant = session.get(Tenant.class, tenantId);
        Long usage = tenant.getEwsSizeUsed();
        Long enterpriseSpaceQuota = ENTERPRISE_SPACE_DEFAULT_QUOTA;
        if (StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.ENTERPRISE_SPACE_QUOTA))) {
            enterpriseSpaceQuota = Long.parseLong(WebConfig.getInstance().getProperty(WebConfig.ENTERPRISE_SPACE_QUOTA));
        }
        Map<String, Long> enterpriseSpaceStatus = new HashMap<>(2);
        enterpriseSpaceStatus.put(RepoConstants.STORAGE_USED, usage);
        enterpriseSpaceStatus.put(ENTERPRISE_SPACE_QUOTA, enterpriseSpaceQuota);
        return enterpriseSpaceStatus;
    }

    public static Long getFolderLastUpdatedTime(DbSession session, String tenantId, String folderPath) {
        Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
        criteria.add(Restrictions.and(Restrictions.eq("tenant.id", tenantId), Restrictions.eq("filePath", folderPath)));
        EnterpriseSpaceItem folder = (EnterpriseSpaceItem)criteria.uniqueResult();
        return folder.getLastModified().getTime();
    }

    public static void checkEnterpriseSpaceStorageExceeded(DbSession session, String tenantId)
            throws EnterpriseSpaceStorageExceededException {
        Map<String, Long> enterpriseSpaceStatus = getEnterpriseSpaceStatus(session, tenantId);
        if (enterpriseSpaceStatus.get(RepoConstants.STORAGE_USED) >= enterpriseSpaceStatus.get(ENTERPRISE_SPACE_QUOTA)) {
            throw new EnterpriseSpaceStorageExceededException(enterpriseSpaceStatus.get(RepoConstants.STORAGE_USED));
        }
    }

    public static String getEnterpriseSpaceFileDUID(String tenantId, String filePath) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            criteria.add(Restrictions.eq("filePath", filePath));
            EnterpriseSpaceItem item = (EnterpriseSpaceItem)criteria.uniqueResult();
            if (item == null) {
                return null;
            }
            return item.getDuid();
        }
    }

    public static Tenant getTenant(String loginTenantId) {
        try (DbSession session = DbSession.newSession()) {
            return session.get(Tenant.class, loginTenantId);
        }
    }

    public static String validateFileDuid(String tenantId, String filePath) throws FileNotFoundException {
        String duid = EnterpriseWorkspaceService.getEnterpriseSpaceFileDUID(tenantId, filePath);
        if (!StringUtils.hasText(duid)) {
            throw new FileNotFoundException("Missing File.");
        }
        return duid;
    }

    public static EnterpriseSpaceItem getEnterpriseSpaceFileByDUID(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
        criteria.add(Restrictions.eq("duid", duid));
        criteria.setMaxResults(1);
        List<EnterpriseSpaceItem> items = (List<EnterpriseSpaceItem>)criteria.list();
        if (!items.isEmpty()) {
            return items.get(0);
        }
        return null;
    }

    public static DefaultRepositoryTemplate getRepository(RMSUserPrincipal principal, String tenantId)
            throws InvalidDefaultRepositoryException {
        DefaultRepositoryTemplate repository;
        try (DbSession session = DbSession.newSession()) {
            repository = new DefaultRepositoryEnterprise(session, principal, tenantId);
        }
        return repository;
    }

    public static boolean checkFolderUpdated(DbSession session, String tenantId, String folderPath,
        long lastModified) {
        Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
        criteria.add(Restrictions.and(Restrictions.eq("tenant.id", tenantId), Restrictions.eq("filePath", folderPath), Restrictions.gt("lastModified", new Date(lastModified))));
        EnterpriseSpaceItem folder = (EnterpriseSpaceItem)criteria.uniqueResult();
        return folder != null;
    }

    public static String getMembershipByTenantId(RMSUserPrincipal principal, String tenantId) {
        try (DbSession session = DbSession.newSession()) {
            return getMembershipByTenantName(principal, session.load(Tenant.class, tenantId).getName());
        }
    }

    public static String getMembershipByTenantName(RMSUserPrincipal principal, String tenantName) {
        return UserMgmt.generateDynamicMemberName(principal.getUserId(), new SystemBucketManagerImpl().constructSystemBucketName(tenantName));
    }

    public static JsonEnterpriseSpaceFileInfo getEnterpriseSpaceFileInfo(DbSession session, RMSUserPrincipal principal,
        com.nextlabs.rms.eval.User user, String tenantId,
        String filePath) {
        Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("filePath", filePath));
        EnterpriseSpaceItem item = (EnterpriseSpaceItem)criteria.uniqueResult();
        if (item == null) {
            return null;
        } else {
            JsonEnterpriseSpaceFileInfo info = toJsonEnterpriseSpaceFileInfo(item, session, principal, user, tenantId);
            boolean isUploader = item.getUploader().getId() == principal.getUserId();
            info.setUploader(isUploader);
            info.setNxl(true);
            return info;
        }
    }

    private static JsonEnterpriseSpaceFileInfo toJsonEnterpriseSpaceFileInfo(EnterpriseSpaceItem file,
        DbSession session,
        RMSUserPrincipal principal, com.nextlabs.rms.eval.User user, String tenantId) {
        DefaultRepositoryTemplate repository = null;
        byte[] downloadedFileBytes = null;
        Map<String, String[]> tags = null;
        JsonEnterpriseSpaceFileInfo fileInfo = new JsonEnterpriseSpaceFileInfo();
        Rights[] rights = null;
        JsonExpiry expiry = new JsonExpiry();
        fileInfo.setSize(file.getSize());
        String filePathDisplay = file.getFilePathDisplay();
        String filePath = file.getFilePath();
        try {
            repository = new DefaultRepositoryEnterprise(session, principal, tenantId);
        } catch (InvalidDefaultRepositoryException e) {
            LOGGER.error("Error occurred while getting the EnterpriseWS repository", e.getMessage(), e);
        }
        try {
            downloadedFileBytes = RepositoryFileUtil.downloadPartialFileFromRepo(repository, filePath, filePathDisplay);
        } catch (RepositoryException e) {
            LOGGER.error("Error occurred while downloading from the EnterpriseWS repository", e.getMessage(), e);
        }
        try (NxlFile metaData = NxlFile.parse(downloadedFileBytes)) {
            try {
                String membership = metaData.getOwner();
                tags = DecryptUtil.getTags(metaData, null);
                FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
                List<Policy> adhocPolicies = policy != null ? policy.getPolicies() : null;
                String tokenGroupName = StringUtils.substringAfter(membership, "@");
                if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                    rights = Rights.fromInt(file.getPermissions());
                    expiry = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                    fileInfo.setProtectionType(ProtectionType.ADHOC.ordinal());
                } else if (EvaluationAdapterFactory.isInitialized()) {
                    fileInfo.setProtectionType(ProtectionType.CENTRAL.ordinal());
                    Tenant parentTenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(file.getFilePathSearchSpace(), membership, parentTenant.getName(), user, tags);
                    EvalResponse evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rights = evalResponse.getRights();
                }
                String[] rightsList = SharedFileManager.toString(rights);
                fileInfo.setRights(rightsList);
                fileInfo.setExpiry(expiry);
            } catch (GeneralSecurityException | IOException | NxlException e) {
                LOGGER.error("Error occurred while getting the tags", e.getMessage(), e);
            }
            fileInfo.setClassification(tags);
        } catch (IOException | NxlException e1) {
            LOGGER.error("Error occurred while parsing the nxl file", e1.getMessage(), e1);
        }
        fileInfo.setName(getFileName(filePathDisplay));
        fileInfo.setPathId(file.getFilePath());
        fileInfo.setPathDisplay(filePathDisplay);
        String fileExt;
        if (filePathDisplay.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            String filePathNoNxl = filePathDisplay.substring(0, filePathDisplay.lastIndexOf('.'));
            fileExt = StringUtils.substringAfterLast(filePathNoNxl, ".");
        } else {
            fileExt = StringUtils.substringAfterLast(filePathDisplay, ".");
        }
        fileInfo.setFileType(fileExt);
        fileInfo.setLastModified(file.getLastModified().getTime());
        fileInfo.setCreationTime(file.getCreationTime().getTime());
        JsonEnterpriseSpaceMember lastModifiedUser = null;
        User lastUser = file.getLastModifiedUser();
        if (lastUser != null) {
            lastModifiedUser = new JsonEnterpriseSpaceMember();
            lastModifiedUser.setUserId(lastUser.getId());
            lastModifiedUser.setDisplayName(lastUser.getDisplayName());
            lastModifiedUser.setEmail(lastUser.getEmail());
        }
        JsonEnterpriseSpaceMember uploadedBy = new JsonEnterpriseSpaceMember();
        uploadedBy.setUserId(file.getUploader().getId());
        uploadedBy.setDisplayName(file.getUploader().getDisplayName());
        uploadedBy.setEmail(file.getUploader().getEmail());
        fileInfo.setLastModifiedUser(lastModifiedUser);
        fileInfo.setUploadedBy(uploadedBy);
        return fileInfo;
    }

    private EnterpriseWorkspaceService() {
    }

    public static boolean checkFileExists(UserSession us, String loginTenantId, String filePath)
            throws InvalidDefaultRepositoryException, RepositoryException {
        try (DbSession session = DbSession.newSession()) {
            Tenant loginTenant = session.get(Tenant.class, loginTenantId);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
            DefaultRepositoryTemplate repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
            String id = repository.getExistingSpaceItemIdWithFilePath(filePath, loginTenantId);
            return id != null;
        }
    }
}
