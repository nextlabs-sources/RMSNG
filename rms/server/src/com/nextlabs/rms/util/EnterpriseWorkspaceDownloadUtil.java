package com.nextlabs.rms.util;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonExpiry;
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
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.MembershipException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

public class EnterpriseWorkspaceDownloadUtil extends DownloadUtil {

    public File reEncryptDocument(int userId, String ticket, String clientId, String duid,
        RMSUserPrincipal principal, Rights[] rightsList, String ownerMembership, String watermark,
        JsonExpiry validity, Map<String, String[]> tags, String path, File output,
        Integer platformId, String tenantId, boolean isAdhocPolicy)
            throws IOException, NxlException, GeneralSecurityException,
            TokenGroupNotFoundException, UnauthorizedOperationException {

        File decryptedFile = null;
        try (InputStream is = new FileInputStream(output);
                NxlFile nxl = NxlFile.parse(is)) {
            try {
                byte[] token = DecryptUtil.requestToken(path, principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), duid, nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                String originalFileName = null;
                if (fileInfo != null) {
                    originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : EnterpriseWorkspaceService.getFileNameWithoutNXL(output.getName());
                }
                if (originalFileName != null) {
                    decryptedFile = new File(output.getParent(), originalFileName);
                    try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                        DecryptUtil.decrypt(nxl, token, fos);
                    }
                }

            } catch (JsonException e) {
                if (e.getStatusCode() == 403) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), ownerMembership, principal.getUserId(), Operations.DOWNLOAD, principal.getDeviceId(), principal.getPlatformId(), tenantId, output.getPath(), output.getName(), output.getPath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.ENTERPRISEWS);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    throw new UnauthorizedOperationException("You are not authorised to perform this operation", e);
                }
                throw new NxlException("Invalid token", e);
            }
        }
        String membership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, tenantId);
        File outputFile = null;
        if (decryptedFile != null) {
            outputFile = new File(output.getParent(), RepositoryFileUtil.getConflictFileName(decryptedFile.getName()) + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        }
        NxlFile nxl = null;
        if (outputFile != null) {
            try (OutputStream os = new FileOutputStream(outputFile); DbSession session = DbSession.newSession()) {
                try {
                    RemoteCrypto.RemoteEncryptionRequest request = new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membership, isAdhocPolicy ? rightsList : null, watermark, validity, tags, decryptedFile, os);
                    nxl = EncryptUtil.create(new ProjectDownloadCrypto(ownerMembership)).encrypt(request);
                    NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, nxl.getDuid());
                    if (nxlMetadataDB != null) {
                        nxlMetadataDB.setOwner(ownerMembership);
                        if (request.getProtectionType() == Constants.ProtectionType.ADHOC) {
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
        }
        return outputFile;
    }

    public File downloadOriginal(RMSUserPrincipal principal, String loginTenantId, String filePath, File outputPath)
            throws InvalidDefaultRepositoryException, RepositoryException {
        DefaultRepositoryTemplate repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
        return repository.getFile(filePath, filePath, outputPath.getPath());
    }

    public File downloadCopy(RMSUserPrincipal principal, String loginTenantId,
        UserSession us, String filePath, String path, File outputPath)
            throws RepositoryException, InvalidDefaultRepositoryException, IOException, NxlException,
            MembershipException, GeneralSecurityException, TokenGroupNotFoundException,
            UnauthorizedOperationException {

        Map<String, String[]> tags;
        String watermark;
        JsonExpiry validity;
        String originalMembership;
        Membership ownerMembership;
        boolean isAdhocPolicy = false;
        Rights[] rightsList;
        File output;

        DefaultRepositoryTemplate repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
        RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
        File input = repository.getFile(filePath, filePath, outputPath.getPath());
        String fileName = fileMetadata.getName();
        String duid = EnterpriseWorkspaceService.validateFileDuid(loginTenantId, filePath);
        Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);

        try (InputStream is = new FileInputStream(input);
                NxlFile nxl = NxlFile.parse(is)) {
            originalMembership = nxl.getOwner();
            ownerMembership = Validator.validateMembership(originalMembership);
            User owner = new User();
            owner.setId(Integer.parseInt(originalMembership.substring("user".length(), originalMembership.indexOf('@'))));
            ownerMembership.setUser(owner);
            tags = DecryptUtil.getTags(nxl, null);
            EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
            rightsList = evalResponse.getRights();
            if (rightsList.length > 0) {
                isAdhocPolicy = true;
            } else {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, loginTenant.getName(), userEval, tags);
                evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                rightsList = evalResponse.getRights();
            }
            watermark = evalResponse.getEffectiveWatermark();
            FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
            validity = getValidity(policy);
            if (ArrayUtils.contains(rightsList, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                String tenantName = AbstractLogin.getDefaultTenant().getName();
                watermark = WatermarkConfigManager.getWaterMarkText(path, tenantName, principal.getTicket(), principal.getPlatformId(), String.valueOf(principal.getUserId()), principal.getClientId());
            }
            output = reEncryptDocument(principal.getUserId(), principal.getTicket(), principal.getClientId(), duid, principal, rightsList, ownerMembership.getName(), watermark, validity, tags, path, input, principal.getPlatformId(), loginTenantId, isAdhocPolicy);
        }
        return output;
    }

    public boolean validateMembership(NxlFile nxlMetadata, String uploadWorkspaceTokenGroupName) {
        String fileTokenGroupName = StringUtils.substringAfter(nxlMetadata.getOwner(), "@");
        return fileTokenGroupName.equals(uploadWorkspaceTokenGroupName);
    }

    public Rights[] getUserFileRights(RMSUserPrincipal principal, String loginTenantId,
        UserSession us, String filePath, File outputPath, Constants.DownloadType downloadType, boolean partialDownload)
            throws InvalidDefaultRepositoryException, RepositoryException,
            IOException, NxlException, GeneralSecurityException, UnauthorizedOperationException, MembershipException,
            ValidateException {

        Map<String, String[]> tags;
        String originalMembership;
        Rights[] rightsList;
        Operations ops = getDownloadOps(downloadType, partialDownload);
        DefaultRepositoryTemplate repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
        RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
        File input = repository.getFile(filePath, filePath, outputPath.getPath());
        String fileName = fileMetadata.getName();
        String duid = EnterpriseWorkspaceService.validateFileDuid(loginTenantId, filePath);
        Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);

        try (InputStream is = new FileInputStream(input);
                NxlFile nxl = NxlFile.parse(is)) {
            originalMembership = nxl.getOwner();
            Membership ownerMembership = Validator.validateMembership(originalMembership);
            User owner = new User();
            owner.setId(Integer.parseInt(originalMembership.substring("user".length(), originalMembership.indexOf('@'))));
            ownerMembership.setUser(owner);
            tags = DecryptUtil.getTags(nxl, null);
            EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
            rightsList = evalResponse.getRights();
            FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
            boolean isExpired;
            boolean isNotYetValid;
            if (rightsList.length > 0) {
                isExpired = AdhocEvalAdapter.isFileExpired(policy);
                isNotYetValid = AdhocEvalAdapter.isNotYetValid(policy);
                if (isExpired || isNotYetValid) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(originalMembership), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), loginTenantId, filePath, fileName, filePath, null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.ENTERPRISEWS);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    throw new ValidateException(403, "File expired or set for future validation", fileName);
                }
            } else {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, loginTenant.getName(), userEval, tags);
                evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                rightsList = evalResponse.getRights();

            }

        }
        if (!checkUserFileRights(downloadType, rightsList) && ops != null) {
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(originalMembership), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), loginTenantId, filePath, fileName, filePath, null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.ENTERPRISEWS);
            RemoteLoggingMgmt.saveActivityLog(activity);
            throw new UnauthorizedOperationException("Access Denied");
        }
        return rightsList;
    }
}
