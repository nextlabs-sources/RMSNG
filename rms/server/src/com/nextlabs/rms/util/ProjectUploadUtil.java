package com.nextlabs.rms.util;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.ProjectUploadType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonRepositoryFileEntry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.ProjectDownloadCrypto;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.UnsupportedNxlVersionException;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.exception.MembershipException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.rs.TokenMgmt;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

public class ProjectUploadUtil {

    public JsonRepositoryFileEntry getJsonRepoFileEntry(UploadedFileMetaData metadata, File tmpFile) {
        JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
        Date lastModifiedTime = metadata.getLastModifiedTime();
        if (lastModifiedTime != null) {
            entry.setLastModified(lastModifiedTime.getTime());
        }
        entry.setFolder(false);
        entry.setName(tmpFile.getName());
        entry.setPathDisplay(metadata.getPathDisplay());
        entry.setPathId(metadata.getPathId());
        entry.setSize(tmpFile.length());
        return entry;
    }

    public Map<String, String> createCustomMetaDeta(String duid, Rights[] rights, String createdBy,
        String lastModifiedBy, Long creationTime, Long lastModified) {
        Map<String, String> customMetadata = new HashMap<>();
        customMetadata.put("duid", duid);
        customMetadata.put("rights", String.valueOf(Rights.toInt(rights)));
        customMetadata.put("createdBy", createdBy);
        if (lastModifiedBy != null) {
            customMetadata.put("lastModifiedBy", lastModifiedBy);
        }
        if (creationTime != null) {
            customMetadata.put("creationTime", creationTime.toString());
        }
        if (lastModified != null) {
            customMetadata.put("lastModified", lastModified.toString());
        }
        return customMetadata;
    }

    public static byte[] requestToken(int userId, String userTicket, String clientId, Integer platformId,
        String userTenant, String ownerMembership, String duid, BigInteger agreement, int tokenMaintenanceLevel,
        ProtectionType protectionType, String filePolicy, String fileTags, UserSession us)
            throws JsonException, IOException {
        platformId = platformId != null ? platformId : DeviceType.WEB.getLow();
        JsonRequest req = new JsonRequest();
        req.addParameter("userId", String.valueOf(userId));
        req.addParameter("ticket", userTicket);
        req.addParameter("tenant", userTenant);
        req.addParameter("owner", ownerMembership);
        req.addParameter("agreement", agreement.toString(16));
        req.addParameter("ml", String.valueOf(tokenMaintenanceLevel));
        req.addParameter("duid", duid);
        req.addParameter("protectionType", protectionType.ordinal());
        req.addParameter("filePolicy", filePolicy == null ? "{}" : filePolicy);
        req.addParameter("fileTags", fileTags);

        String ret = (new TokenMgmt()).getTokenInternal(us, userId, userTicket, clientId, platformId, req.toJson());
        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }

        String token = resp.getResultAsString("token");
        return Hex.toByteArray(token);
    }

    public File reEncryptDocument(int userId, String ticket, String clientId, String duid,
        RMSUserPrincipal principal, Rights[] rightsList, String ownerMembership, String watermark,
        JsonExpiry validity, Map<String, String[]> tags, String path, File output,
        Integer platformId, Project project, boolean isAdhocPolicy, Operations ops, ProjectUploadType uploadType,
        UserSession us)
            throws IOException, NxlException, GeneralSecurityException,
            TokenGroupNotFoundException, TokenGroupException, UnauthorizedOperationException {

        File decryptedFile = null;
        try (InputStream is = new FileInputStream(output);
                NxlFile nxl = NxlFile.parse(is)) {
            try {
                byte[] token = requestToken(principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), duid, nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null), us);
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                String originalFileName = null;
                if (fileInfo != null) {
                    originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : (new ProjectDownloadUtil()).getFileNameWithoutNXL(output.getName());
                }
                if (originalFileName != null) {
                    decryptedFile = new File(output.getParent(), originalFileName);
                }
                if (decryptedFile != null) {
                    try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                        DecryptUtil.decrypt(nxl, token, fos);
                    }
                }
            } catch (JsonException e) {
                if (e.getStatusCode() == 403) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), ownerMembership, principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), principal.getTenantId(), output.getPath(), output.getName(), output.getPath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    throw new UnauthorizedOperationException("You are not authorised to perform this operation", e);
                }
                throw new NxlException("Invalid token", e);
            }
        }
        String membership = ProjectService.getMembership(principal, project);
        File outputFile = null;
        if (decryptedFile != null) {
            outputFile = new File(output.getParent(), output.getName());
        }
        NxlFile nxl = null;
        if (outputFile != null) {
            try (OutputStream os = new FileOutputStream(outputFile); DbSession session = DbSession.newSession()) {
                try {
                    RemoteCrypto.RemoteEncryptionRequest request = new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membership, isAdhocPolicy ? rightsList : null, watermark, validity, tags, decryptedFile, os);
                    if (ProjectUploadType.UPLOAD_PROJECT_SYSBUCKET.equals(uploadType)) {
                        ownerMembership = membership;
                    }
                    nxl = EncryptUtil.create(new ProjectDownloadCrypto(ownerMembership)).encrypt(request);

                    NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, nxl.getDuid());
                    if (nxlMetadataDB != null) {
                        nxlMetadataDB.setOwner(ownerMembership);
                        if (request.getProtectionType() == Constants.ProtectionType.ADHOC) {
                            MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
                            byte[] issuerUpdatedFilePolicyChecksum = md.digest(GsonUtils.GSON.toJson(request.getFilePolicy()).getBytes((StandardCharsets.UTF_8.name())));
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
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), ownerMembership, principal.getUserId(), Operations.PROTECT, principal.getDeviceId(), platformId, String.valueOf(project.getId()), outputFile.getPath(), outputFile.getName(), outputFile.getPath(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            RemoteLoggingMgmt.saveActivityLog(activity);
        }
        return outputFile;
    }

    public static Constants.ProjectUploadType validateUploadType(int uploadTypeOrdinal) {
        if (uploadTypeOrdinal < 0 || uploadTypeOrdinal > 4) {
            throw new ValidateException(400, "Missing/Wrong upload type.");
        }
        ProjectUploadType projectUploadType = Constants.ProjectUploadType.values()[uploadTypeOrdinal];

        // changing incoming UPLOAD_VIEW to UPLOAD_EDIT as a temporary fix for RMD 
        projectUploadType = (projectUploadType == ProjectUploadType.UPLOAD_VIEW) ? ProjectUploadType.UPLOAD_EDIT : projectUploadType;

        return projectUploadType;
    }

    public File transformNxlFileForUpload(File tmpFile, ProjectUploadType uploadType, RMSUserPrincipal principal,
        String path, Project project, UserSession us, Tenant parentTenant, String fileName)
            throws MembershipException, GeneralSecurityException, IOException, TokenGroupNotFoundException,
            TokenGroupException, UnauthorizedOperationException {
        boolean isUploadedFileNxl = false;
        boolean isAdhocPolicy = false;
        ProjectDownloadUtil util = new ProjectDownloadUtil();

        if (ProjectUploadType.UPLOAD_EDIT == uploadType || ProjectUploadType.UPLOAD_PROJECT == uploadType) {
            return tmpFile;
        }

        try (InputStream is = new FileInputStream(tmpFile);
                NxlFile nxlMetadata = NxlFile.parse(is)) {
            if (nxlMetadata.getContentLength() == 0) {
                throw new ValidateException(5005, "Empty files are not allowed to be uploaded.");
            }
            String uploadedFileOwner = nxlMetadata.getOwner();
            String uploadedFileDuid = nxlMetadata.getDuid();
            if (StringUtils.hasText(uploadedFileOwner)) {
                isUploadedFileNxl = true;

                if (ProjectUploadType.UPLOAD_VIEW == uploadType) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(uploadedFileDuid, uploadedFileOwner, principal.getUserId(), Operations.UPLOAD_VIEW, principal.getDeviceId(), principal.getPlatformId(), principal.getTenantId(), tmpFile.getPath(), tmpFile.getName(), tmpFile.getPath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    throw new ValidateException(4009, "Cannot upload file for type 1");
                }

                int projectId = project.getId();
                if (ProjectUploadType.UPLOAD_NORMAL == uploadType && !Validator.validateMembership(nxlMetadata, projectId)) {
                    throw new ValidateException(5003, "The nxl file does not belong to this project");
                }

                String membershipWorkspace = EnterpriseWorkspaceService.getMembershipByTenantName(principal, principal.getTenantName());
                String uploadWorkspaceTokenGroupName = StringUtils.substringAfter(membershipWorkspace, "@");

                if (ProjectUploadType.UPLOAD_PROJECT_SYSBUCKET == uploadType && !((new EnterpriseWorkspaceDownloadUtil()).validateMembership(nxlMetadata, uploadWorkspaceTokenGroupName)) && !(Validator.validateMembership(nxlMetadata, projectId))) {
                    throw new ValidateException(5003, "The nxl file does not belong to this project or system bucket token group");
                }

                String originalMembership = nxlMetadata.getOwner();
                Membership ownerMembership = Validator.validateMembership(originalMembership);
                Map<String, String[]> tags = DecryptUtil.getTags(nxlMetadata, null);
                EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxlMetadata);
                Rights[] rightsList = evalResponse.getRights();
                if (rightsList.length > 0) {
                    isAdhocPolicy = true;
                } else {
                    User user = us.getUser();
                    com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, parentTenant.getName(), userEval, tags);
                    evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rightsList = evalResponse.getRights();
                }
                String watermark = evalResponse.getEffectiveWatermark();
                FilePolicy policy = DecryptUtil.getFilePolicy(nxlMetadata, null);
                JsonExpiry validity = util.getValidity(policy);
                if (ArrayUtils.contains(rightsList, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                    String tenantName = AbstractLogin.getDefaultTenant().getName();
                    watermark = WatermarkConfigManager.getWaterMarkText(path, tenantName, principal.getTicket(), principal.getPlatformId(), String.valueOf(principal.getUserId()), principal.getClientId());
                }
                tmpFile = reEncryptDocument(principal.getUserId(), principal.getTicket(), principal.getClientId(), uploadedFileDuid, principal, rightsList, ownerMembership.getName(), watermark, validity, tags, path, tmpFile, principal.getPlatformId(), project, isAdhocPolicy, Operations.UPLOAD_NORMAL, uploadType, us);
            }
        } catch (NxlException e) {
            if (isUploadedFileNxl) {
                if (e instanceof UnsupportedNxlVersionException) {
                    ValidateException ve = new ValidateException(5006, "Unsupported NXL version");
                    ve.initCause(e);
                    throw ve;
                } else {
                    ValidateException ve = new ValidateException(5001, "Invalid NXL format.");
                    ve.initCause(e);
                    throw ve;
                }
            }
        }
        return tmpFile;
    }

    public static Operations getUploadOps(ProjectUploadType uploadType) {
        Operations ops = null;
        if (uploadType == Constants.ProjectUploadType.UPLOAD_EDIT) {
            ops = Operations.UPLOAD_EDIT;
        } else if (uploadType == Constants.ProjectUploadType.UPLOAD_PROJECT_SYSBUCKET) {
            ops = Operations.UPLOAD_PROJECT_SYSBUCKET;
        } else if (uploadType == Constants.ProjectUploadType.UPLOAD_VIEW) {
            ops = Operations.UPLOAD_VIEW;
        } else if (uploadType == ProjectUploadType.UPLOAD_PROJECT) {
            ops = Operations.UPLOAD_PROJECT;
        } else {
            ops = Operations.UPLOAD_NORMAL;
        }
        return ops;
    }

}
