package com.nextlabs.rms.share;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.ProjectDownloadCrypto;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.SharingRecipientProject;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryProject;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public class ShareToProject implements ShareStrategy {

    @Override
    public Map<String, Set<?>> processRecipientsList(List<?> existingRecipients,
        List<JsonSharing.JsonRecipient> recipients) {
        Set<Integer> alreadyShared = new LinkedHashSet<>(recipients.size());
        Set<Integer> newShared = new LinkedHashSet<>(recipients.size());
        Set<Integer> totalShared = new LinkedHashSet<>(recipients.size());

        recipients.forEach((recipient) -> {
            Integer projectId = recipient.getProjectId();
            newShared.add(projectId);
            totalShared.add(projectId);
        });
        existingRecipients.forEach((existing) -> {
            Integer projectId = ((SharingRecipientProject)existing).getId().getProjectId();
            alreadyShared.add(projectId);
            newShared.remove(projectId);
            totalShared.add(projectId);
        });

        Map<String, Set<?>> sharedMap = new HashMap<>();
        sharedMap.put("alreadyShared", alreadyShared);
        sharedMap.put("newShared", newShared);
        sharedMap.put("totalShared", totalShared);
        return sharedMap;
    }

    @Override
    public String createRecipientsActivityData(Collection<?> recipients) {
        JsonObject data = new JsonObject();
        data.addProperty("recipients", Joiner.on(',').join(recipients));
        return data.toString();
    }

    @Override
    public void sendNotification(HttpServletRequest request, SharedFileDTO dto, String transactionId, String emailFrom,
        String loginTenant) throws IOException, GeneralSecurityException {

    }

    @Override
    public boolean isRevoked(StoreItem storeItem) {
        try (DbSession session = DbSession.newSession()) {
            ProjectSpaceItem item = ProjectService.getProjectFileByDUID(session, storeItem.getDuid());
            return (ProjectSpaceItem.Status.REVOKED.equals(item.getStatus()));
        }
    }

    @Override
    public boolean isExpired(StoreItem storeItem) {
        return false;
    }

    @Override
    public boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right) {
        return false;
    }

    @Override
    public boolean hasAdminRight(StoreItem storeItem, RMSUserPrincipal principal, UserSession us) {
        try (DbSession session = DbSession.newSession()) {
            ProjectSpaceItem item = ProjectService.getProjectFileByDUID(session, storeItem.getDuid());
            boolean isAdmin = item.getProject().getParentTenant().isProjectAdmin(principal.getEmail());
            Project project = ProjectService.getProject(session, us, item.getProject().getId());
            return isAdmin && project != null;
        }
    }

    @Override
    public NxlMetaData resolveNxlMetadataFromStoreItem(StoreItem storeItem, RMSUserPrincipal principal)
            throws RMSException {
        try (DbSession session = DbSession.newSession()) {
            DefaultRepositoryProject repo = new DefaultRepositoryProject(session, principal, storeItem.getProjectId());
            byte[] downloadedFileBytes = RepositoryFileUtil.downloadPartialFileFromRepo(repo, storeItem.getFilePath(), storeItem.getFilePathDisplay());
            NxlFile metaData = NxlFile.parse(downloadedFileBytes);
            String membership = metaData.getOwner();
            Map<String, String[]> tags = DecryptUtil.getTags(metaData, null);
            FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
            return new NxlMetaData(storeItem.getDuid(), membership, Constants.TokenGroupType.TOKENGROUP_PROJECT, isRevoked(storeItem), storeItem.getUserId() == principal.getUserId(), policy, tags);
        } catch (InvalidDefaultRepositoryException | RepositoryException | NxlException | GeneralSecurityException
                | IOException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    @Override
    public SharedFileDTO updateSharedFileDTO(SharedFileDTO dto, RMSUserPrincipal principal, JsonSharing shareReq,
        ShareSource source) {
        dto.setProjectId(shareReq.getProjectId());
        return dto;
    }

    @Override
    public boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right, Object src) {
        return ProjectService.checkRight(storeItem.getDuid(), right);
    }

    @Override
    public void validateReshareInputs(SharingTransaction sharingTransaction, List<JsonRecipient> recipientList,
        Object src, UserSession us) {

        if (recipientList == null || recipientList.isEmpty()) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        if (src == null) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        List<Integer> recipientProjIds = new ArrayList<Integer>(recipientList.size());
        for (JsonRecipient jsonRecipient : recipientList) {
            if (StringUtils.hasText(jsonRecipient.getEmail()) || StringUtils.hasText(jsonRecipient.getTenantName())) {
                throw new ValidateException(400, "Invalid recipient");
            }
            recipientProjIds.add(jsonRecipient.getProjectId());
        }

        if (!ProjectService.isValidMemberProjectIds(recipientProjIds, us)) {
            throw new ValidateException(400, "Invalid recipient");
        }

    }

    @Override
    public void validateReshareRights(NxlMetaData nxl, Object src, StoreItem item, RMSUserPrincipal principal,
        UserSession us, String transactionId) throws RMSException {
        if (src == null) {
            throw new ValidateException(400, "Missing required parameters.");
        }
        if (isRevoked(item)) {
            throw new ValidateException(4001, "File sharing has been revoked");
        }
        if (!SharedFileManager.isRecipientProject(item.getDuid(), (Integer)src)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }
        if (hasAdminRightAtSource(item, principal, src, us)) {
            return;
        }

        if (hasNoAccessValidity(nxl)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }

        JsonSharedWithMeFile sharedWithMeFile = new ShareProjectMapper().getSharedWithMeFile(transactionId, principal, getRecipientMembership(principal, ((Integer)src).toString()));
        int protectionType = sharedWithMeFile.getProtectionType();
        Rights[] rights = Rights.fromStrings(sharedWithMeFile.getRights());
        if (protectionType == ProtectionType.ADHOC.ordinal() && !checkRight(item, principal, Rights.SHARE, src) || (protectionType == ProtectionType.CENTRAL.ordinal() && !ArrayUtils.contains(rights, Rights.SHARE))) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }
    }

    @Override
    public void validateDecryptRight(StoreItem item, RMSUserPrincipal principal, String transactionId, Object src,
        NxlMetaData nxlMetaData)
            throws RMSException {

        if (isRevoked(item)) {
            throw new ValidateException(4001, "File has been revoked");
        }

        if (hasNoAccessValidity(nxlMetaData)) {
            throw new ValidateException(403, "You are not allowed to decrypt this file.");
        }

        if (!SharedFileManager.isRecipientProject(item.getDuid(), (Integer)src)) {
            throw new ValidateException(403, "You are not allowed to decrypt this file.");
        }

        JsonSharedWithMeFile sharedWithMeFile = new ShareProjectMapper().getSharedWithMeFile(transactionId, principal, getRecipientMembership(principal, ((Integer)src).toString()));
        int protectionType = sharedWithMeFile.getProtectionType();
        Rights[] rights = Rights.fromStrings(sharedWithMeFile.getRights());
        if ((protectionType == ProtectionType.ADHOC.ordinal() && !checkRight(item, principal, Rights.DECRYPT, src)) || (protectionType == ProtectionType.CENTRAL.ordinal() && !ArrayUtils.contains(rights, Rights.DECRYPT))) {
            throw new ValidateException(403, "You are not allowed to decrypt this file.");
        }
    }

    @Override
    public boolean hasAdminRightAtSource(StoreItem storeItem, RMSUserPrincipal principal, Object src, UserSession us) {
        try (DbSession session = DbSession.newSession()) {
            Project project = session.get(Project.class, (Integer)src);
            boolean isAdmin = project.getParentTenant().isProjectAdmin(principal.getEmail());
            Project isValidProject = ProjectService.getProject(session, us, (Integer)src);
            return (isAdmin && (isValidProject != null));
        }
    }

    @Override
    public String getRecipientMembership(RMSUserPrincipal principal, String recipient) throws RMSException {
        try (DbSession session = DbSession.newSession()) {
            Project project = session.get(Project.class, Integer.valueOf(recipient));

            if (!isProjectMember(session, principal, project)) {
                throw new ValidateException(403, "You are not allowed to access this file");
            }

            return ProjectService.getMembership(principal, project);
        } catch (TokenGroupException | IOException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    private boolean isProjectMember(DbSession session, RMSUserPrincipal principal, Project project) {
        Membership membership = ProjectService.getActiveMembership(session, principal.getUserId(), project.getId());
        if (membership == null) {
            boolean isDynamicMember = isProjectDynamicallyAccessible(session, principal, project.getId());
            if (!isDynamicMember) {
                return false;
            }
        }
        return true;
    }

    private static boolean isProjectDynamicallyAccessible(DbSession session, RMSUserPrincipal principal,
        Integer projectId) {
        com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(principal.getUserId())).clientId(principal.getClientId()).email(principal.getEmail()).build();
        Tenant loginTenant = session.get(Tenant.class, principal.getLoginTenant());
        Set<Integer> abacProjects = MembershipPoliciesEvaluationHandler.evalABACMembershipPolicies(userEval, loginTenant.getName());
        if (abacProjects.isEmpty()) {
            return false;
        }
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.in("id", abacProjects.toArray()));
        criteria.setProjection(Projections.property("id"));
        List<?> projectIds = criteria.list();

        return projectIds != null && projectIds.contains(projectId);
    }

    @Override
    public IRepository getDefaultRepository(RMSUserPrincipal principal, ShareSource source)
            throws InvalidDefaultRepositoryException {
        try (DbSession session = DbSession.newSession()) {
            return new DefaultRepositoryProject(session, principal, source.storeItem.getProjectId());
        }

    }

    @Override
    public void validateDownloadRights(NxlMetaData nxl, Object src, StoreItem storeItem, RMSUserPrincipal principal,
        String transactionId, boolean downloadForView, NxlMetaData nxlMetaData) throws RMSException {
        if (src == null) {
            throw new ValidateException(400, "Missing required parameters.");
        }
        if (isRevoked(storeItem)) {
            throw new ValidateException(4001, "File has been revoked");
        }
        if (hasNoAccessValidity(nxlMetaData)) {
            throw new ValidateException(403, "You are not authorized to perform this operation");
        }
        if (!SharedFileManager.isRecipientProject(storeItem.getDuid(), (Integer)src)) {
            throw new ValidateException(403, "You are not allowed to access this file");
        }
        JsonSharedWithMeFile sharedWithMeFile = new ShareProjectMapper().getSharedWithMeFile(transactionId, principal, getRecipientMembership(principal, ((Integer)src).toString()));
        int protectionType = sharedWithMeFile.getProtectionType();
        Rights[] rights = Rights.fromStrings(sharedWithMeFile.getRights());

        if (!downloadForView && !((protectionType == ProtectionType.ADHOC.ordinal() && checkRight(storeItem, principal, Rights.DOWNLOAD, src)) || (protectionType == ProtectionType.CENTRAL.ordinal() && ArrayUtils.contains(rights, Rights.DOWNLOAD)))) {
            throw new ValidateException(403, "You are not allowed to download this file.");
        }

        if (downloadForView && !((protectionType == ProtectionType.ADHOC.ordinal() && checkRight(storeItem, principal, Rights.VIEW, src)) || (protectionType == ProtectionType.CENTRAL.ordinal() && ArrayUtils.contains(rights, Rights.VIEW)))) {
            throw new ValidateException(403, "You are not allowed to view this file.");
        }
    }

    @Override
    public Response downloadFile(RMSUserPrincipal principal, StoreItem storeItem, ShareSource source,
        HttpServletRequest request,
        HttpServletResponse response, AccountType accountType, int start, long length, boolean downloadForView,
        Object spaceId) throws InvalidDefaultRepositoryException, RepositoryException, IOException {
        IRepository repository = getDefaultRepository(principal, source);
        boolean partialDownload = start >= 0 && length >= 0;
        if (partialDownload) {
            byte[] data = repository.downloadPartialFile(storeItem.getFilePath(), storeItem.getFilePath(), start, start + length - 1);
            if (data != null) {
                long contentLength = data.length;
                response.setHeader("x-rms-file-size", Long.toString(contentLength));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                response.getOutputStream().write(data);
            }
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } else {
            if (downloadForView) {
                File outputPath = RepositoryFileUtil.getTempOutputFolder();
                File output = repository.getFile(storeItem.getFilePath(), storeItem.getFilePath(), outputPath.getPath());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(FileUtils.getName(storeItem.getFilePathDisplay())));
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
                return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
            } else {
                File outputPath = null;
                try {
                    Rights[] rightsList = null;
                    String requestURI = HTTPUtil.getInternalURI(request);
                    outputPath = RepositoryFileUtil.getTempOutputFolder();
                    File output = repository.getFile(storeItem.getFilePath(), storeItem.getFilePath(), outputPath.getPath());
                    Map<String, String[]> tags = null;
                    String watermark = null;
                    JsonExpiry validity = null;
                    boolean isAdhocPolicy = false;
                    String duid = source.getExistingTransaction().getProjectNxl().getDuid();
                    try (InputStream is = new FileInputStream(output); NxlFile nxl = NxlFile.parse(is)) {
                        tags = DecryptUtil.getTags(nxl, null);
                        EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
                        JsonSharedWithMeFile sharedWithMeFile = new ShareProjectMapper().getSharedWithMeFile(source.getExistingTransaction().getId(), principal, getRecipientMembership(principal, ((Integer)spaceId).toString()));
                        isAdhocPolicy = (sharedWithMeFile.getProtectionType() == ProtectionType.ADHOC.ordinal());
                        rightsList = Rights.fromStrings(sharedWithMeFile.getRights());
                        watermark = evalResponse.getEffectiveWatermark();
                        if (ArrayUtils.contains(rightsList, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                            watermark = WatermarkConfigManager.getWaterMarkText(requestURI, AbstractLogin.getDefaultTenant().getName(), principal.getTicket(), principal.getPlatformId(), String.valueOf(principal.getUserId()), principal.getClientId());
                        }
                        FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
                        validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                        if (validity.getStartDate() == null && validity.getEndDate() == null) {
                            validity.setOption(0);
                        } else if (validity.getStartDate() == null && validity.getEndDate() != null) {
                            validity.setOption(2);
                        } else if (validity.getStartDate() != null && validity.getEndDate() != null) {
                            validity.setOption(3);
                        }
                    }
                    Project sourceProject = source.getExistingTransaction().getProjectNxl().getProject();
                    String membership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, principal.getTenantId());
                    output = reEncryptDocument(principal.getUserId(), principal.getTicket(), principal.getClientId(), duid, principal, rightsList, membership, watermark, validity, tags, requestURI, output, principal.getDeviceId(), principal.getPlatformId(), sourceProject, isAdhocPolicy, (Integer)spaceId);
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(output.getName()));
                    if (output.length() > 0) {
                        response.setHeader("x-rms-file-size", Long.toString(output.length()));
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                        try (InputStream fis = new FileInputStream(output)) {
                            IOUtils.copy(fis, response.getOutputStream());
                        }
                    }
                    return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
                } catch (Throwable e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "Internal Server Error.").toJson()).build();
                } finally {
                    if (repository instanceof Closeable) {
                        IOUtils.closeQuietly(Closeable.class.cast(repository));
                    }
                    if (outputPath != null) {
                        FileUtils.deleteQuietly(outputPath);
                    }
                }
            }

        }
    }

    private File reEncryptDocument(int userId, String ticket, String clientId, String duid,
        RMSUserPrincipal principal, Rights[] rightsList, String ownerMembership, String watermark,
        JsonExpiry validity, Map<String, String[]> tags, String path, File output, String deviceId,
        Integer platformId, Project project, boolean isAdhocPolicy, Integer spaceId)
            throws IOException, NxlException, GeneralSecurityException, RepositoryException, RMSException,
            TokenGroupNotFoundException, TokenGroupException {

        File decryptedFile = null;
        try (InputStream is = new FileInputStream(output);
                NxlFile nxl = NxlFile.parse(is)) {
            try {
                String userMembership = getRecipientMembership(principal, spaceId.toString());
                byte[] token = DecryptUtil.requestToken(path, principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), duid, nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null), Integer.toString(SHARESPACE.PROJECTSPACE.ordinal()), spaceId.toString(), userMembership);
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                String originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : getFileNameWithoutNXL(output.getName());
                decryptedFile = new File(output.getParent(), originalFileName);
                try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                    DecryptUtil.decrypt(nxl, token, fos);
                }
            } catch (JsonException e) {
                NxlException ne = new NxlException("Invalid token");
                ne.initCause(e);
                throw ne;
            }
        }
        String membership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, principal.getTenantId());
        File outputFile = new File(output.getParent(), decryptedFile.getName() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        NxlFile nxl = null;
        try (OutputStream os = new FileOutputStream(outputFile); DbSession session = DbSession.newSession()) {
            try {
                RemoteCrypto.RemoteEncryptionRequest request = new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membership, isAdhocPolicy ? rightsList : null, watermark, validity, tags, decryptedFile, os);
                nxl = EncryptUtil.create(new ProjectDownloadCrypto(ownerMembership)).encrypt(request);
                NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, nxl.getDuid());
                if (nxlMetadataDB != null) {
                    nxlMetadataDB.setOwner(ownerMembership);
                    if (request.getProtectionType() == ProtectionType.ADHOC) {
                        MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
                        byte[] issuerUpdatedFilePolicyChecksum = md.digest(GsonUtils.GSON.toJson(request.getFilePolicy()).getBytes((StandardCharsets.UTF_8)));
                        nxlMetadataDB.setFilePolicyChecksum(Hex.toHexString(issuerUpdatedFilePolicyChecksum));
                    }
                    session.beginTransaction();
                    session.update(nxlMetadataDB);
                    session.commit();
                }
            } finally {
                IOUtils.closeQuietly(nxl);
            }
        }
        RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), ownerMembership, principal.getUserId(), Operations.PROTECT, deviceId, platformId, String.valueOf(project.getId()), outputFile.getPath(), outputFile.getName(), outputFile.getPath(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PERSONAL);
        RemoteLoggingMgmt.saveActivityLog(activity);
        return outputFile;
    }

    private static String getFileNameWithoutNXL(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return fileName;
        }
        if (!fileName.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            return fileName;
        }
        int index = fileName.toLowerCase().lastIndexOf(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        return fileName.substring(0, index);
    }

    private EvalResponse getAdhocEvaluationResponse(NxlFile nxlMetadata)
            throws GeneralSecurityException, IOException, NxlException {
        EvalResponse evalResponse = new EvalResponse();
        FilePolicy policy = DecryptUtil.getFilePolicy(nxlMetadata, null);
        List<Policy> adhocPolicies = policy.getPolicies();
        if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
            evalResponse = AdhocEvalAdapter.evaluate(policy, true);
        }
        return evalResponse;
    }

    @Override
    public byte[] requestDecryptToken(HttpServletRequest request, RMSUserPrincipal principal, NxlFile nxl,
        String spaceId) throws JsonException, IOException, GeneralSecurityException, NxlException, RMSException {
        String userMembership = getRecipientMembership(principal, spaceId);
        return DecryptUtil.requestToken(HTTPUtil.getInternalURI(request), principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), nxl.getDuid(), nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null), Integer.toString(SHARESPACE.PROJECTSPACE.ordinal()), spaceId, userMembership);
    }

    @Override
    public void validateShareInputs(JsonSharing shareReq, UserSession us) {
        List<JsonRecipient> recipientList = shareReq.getRecipients();
        if (recipientList == null || recipientList.isEmpty()) {
            throw new ValidateException(400, "Missing required parameters");
        }

        List<Integer> validateProjIds = new ArrayList<Integer>(recipientList.size());
        for (JsonRecipient jsonRecipient : recipientList) {
            if (StringUtils.hasText(jsonRecipient.getEmail()) || StringUtils.hasText(jsonRecipient.getTenantName())) {
                throw new ValidateException(400, "Invalid recipient");
            }
            validateProjIds.add(jsonRecipient.getProjectId());
        }

        if (!ProjectService.isValidMemberProjectIds(validateProjIds, us)) {
            throw new ValidateException(400, "Invalid recipient project");
        }

    }

    @Override
    public void validateShareRights(Integer projectId, String filePathId, String fileName, RMSUserPrincipal principal,
        StoreItem item, UserSession us, NxlMetaData nxlMetaData) throws RMSException {
        try (DbSession session = DbSession.newSession()) {
            File outputPath = RepositoryFileUtil.getTempOutputFolder();
            IRepository repository = new DefaultRepositoryProject(session, principal, projectId);
            File output = repository.getFile(filePathId, filePathId, outputPath.getPath());
            try (InputStream is = new FileInputStream(output); NxlFile nxl = NxlFile.parse(is)) {
                String userMembership = getRecipientMembership(principal, Integer.toString(projectId));

                if (isRevoked(item)) {
                    throw new ValidateException(4001, "File has been revoked");
                }

                if (hasAdminRight(item, principal, us)) {
                    return;
                }

                if (hasNoAccessValidity(nxlMetaData)) {
                    throw new ValidateException(403, "You are not allowed to share this file.");
                }

                EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
                Rights[] rightsList = evalResponse.getRights();
                if (rightsList.length == 0) {
                    com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(principal.getUserId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(principal.getEmail()).displayName(principal.getName()).build();
                    Map<String, String[]> tags = DecryptUtil.getTags(nxl, null);
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, userMembership, principal.getTenantName(), userEval, tags);
                    evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rightsList = evalResponse.getRights();
                }
                if (!ArrayUtils.contains(rightsList, Rights.SHARE)) {
                    throw new ValidateException(403, "You are not allowed to share this file.");
                }
            }
        } catch (GeneralSecurityException | IOException | InvalidDefaultRepositoryException | RepositoryException
                | NxlException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    @Override
    public Operations getRemoveRecipientOperation() {
        return Operations.REMOVE_PROJECT;
    }

    @Override
    public void validateListSharedFiles(Object projectId, RMSUserPrincipal principal) {
        checkProjectMembershipForUser(projectId, principal);
    }

    private void checkProjectMembershipForUser(Object projectId, RMSUserPrincipal principal) {
        try (DbSession session = DbSession.newSession()) {
            Project project = session.get(Project.class, (Integer)projectId);
            if (!isProjectMember(session, principal, project)) {
                throw new ValidateException(403, "You are not allowed access");
            }
        }
    }

    private boolean hasNoAccessValidity(NxlMetaData nxlMetaData) {
        return (nxlMetaData.isExpired() || nxlMetaData.isNotYetValid());
    }

    @Override
    public void validateRevokedFiles(StoreItem item, RMSUserPrincipal principal, UserSession us) {
        if (item == null) {
            throw new ValidateException(404, "Nxl file not found");
        }
        if (isRevoked(item)) {
            throw new ValidateException(304, "File already revoked");
        }

        if (!hasAdminRight(item, principal, us)) {
            throw new ValidateException(403, "You are not allowed to revoke the file");
        }
        checkProjectMembershipForUser(item.getProjectId(), principal);
    }

    @Override
    public String getSharedLink(HttpServletRequest request, String loginTenant, String transactionId)
            throws GeneralSecurityException {
        return null;
    }

}
