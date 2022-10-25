package com.nextlabs.rms.util;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonWraper;
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
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectInvitation;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.mail.Mail;
import com.nextlabs.rms.mail.Sender;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.HTTPUtil;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

public class ProjectDownloadUtil extends DownloadUtil {

    public void updateProjectActionTime(int userId, int projectId) {
        try (DbSession session = DbSession.newSession()) {
            Membership membership = ProjectService.getActiveMembership(session, userId, projectId);
            if (membership != null) {
                membership.setProjectActionTime(new Date());
                session.beginTransaction();
                session.saveOrUpdate(membership);
                session.commit();
            }
        }
    }

    public File reEncryptDocument(int userId, String ticket, String clientId, String duid,
        RMSUserPrincipal principal, Rights[] rightsList, String ownerMembership, String watermark,
        JsonExpiry validity, Map<String, String[]> tags, String path, File output,
        Integer platformId, Project project, boolean isAdhocPolicy, Operations ops, DownloadType downloadType)
            throws IOException, NxlException, GeneralSecurityException,
            TokenGroupNotFoundException, TokenGroupException, UnauthorizedOperationException {

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
                    originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : getFileNameWithoutNXL(output.getName());
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
        if (DownloadType.FOR_SYSTEMBUCKET.equals(downloadType)) {
            membership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, principal.getTenantId());
            ownerMembership = membership;
        }
        File outputFile = null;
        if (decryptedFile != null) {
            outputFile = new File(output.getParent(), decryptedFile.getName() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
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
        }
        return outputFile;
    }

    public File downloadOriginal(RMSUserPrincipal principal, Project project, String filePath, File outputPath)
            throws InvalidDefaultRepositoryException, RepositoryException {
        DefaultRepositoryTemplate repository = ProjectService.getRepository(principal, project.getId());
        updateProjectActionTime(principal.getUserId(), project.getId());
        return repository.getFile(filePath, filePath, outputPath.getPath());
    }

    public File downloadCopy(RMSUserPrincipal principal, Project project,
        UserSession us, String filePath, String path, File outputPath, Operations ops, DownloadType downloadType)
            throws InvalidDefaultRepositoryException, RepositoryException,
            IOException, NxlException, GeneralSecurityException, TokenGroupException, TokenGroupNotFoundException,
            UnauthorizedOperationException, MembershipException {

        Map<String, String[]> tags;
        String watermark;
        JsonExpiry validity;
        String originalMembership;
        Membership ownerMembership;
        boolean isAdhocPolicy = false;
        Rights[] rightsList;

        Tenant parentTenant = getParentTenant(project.getParentTenant().getId());
        DefaultRepositoryTemplate repository = ProjectService.getRepository(principal, project.getId());
        updateProjectActionTime(principal.getUserId(), project.getId());
        RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
        String duid = ProjectService.validateFileDuid(project.getId(), filePath);

        String fileName = fileMetadata.getName();
        File inputFile = repository.getFile(filePath, filePath, outputPath.getPath());
        File output;

        try (InputStream is = new FileInputStream(inputFile);
                NxlFile nxl = NxlFile.parse(is)) {
            originalMembership = nxl.getOwner();
            ownerMembership = Validator.validateMembership(originalMembership);
            tags = DecryptUtil.getTags(nxl, null);
            EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
            rightsList = evalResponse.getRights();
            if (rightsList.length > 0) {
                isAdhocPolicy = true;
            } else {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, parentTenant.getName(), userEval, tags);
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
            output = reEncryptDocument(principal.getUserId(), principal.getTicket(), principal.getClientId(), duid, principal, rightsList, ownerMembership.getName(), watermark, validity, tags, path, inputFile, principal.getPlatformId(), project, isAdhocPolicy, ops, downloadType);

        }
        return output;
    }

    public Rights[] getUserFileRights(RMSUserPrincipal principal, String id,
        UserSession us, String filePath, File outputPath, Constants.DownloadType downloadType, boolean partialDownload)
            throws InvalidDefaultRepositoryException, RepositoryException,
            IOException, NxlException, GeneralSecurityException, UnauthorizedOperationException {

        Map<String, String[]> tags;
        String originalMembership;
        Rights[] rightsList;
        int projectId = Integer.parseInt(id);

        Project project = Validator.validateProject(projectId, us);
        Tenant parentTenant = getParentTenant(project.getParentTenant().getId());
        DefaultRepositoryTemplate repository = ProjectService.getRepository(principal, projectId);
        RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
        String fileName = fileMetadata.getName();
        File inputFile = repository.getFile(filePath, filePath, outputPath.getPath());
        Operations ops = getDownloadOps(downloadType, partialDownload);
        String duid = ProjectService.validateFileDuid(projectId, filePath);

        try (InputStream is = new FileInputStream(inputFile);
                NxlFile nxl = NxlFile.parse(is)) {
            originalMembership = nxl.getOwner();
            tags = DecryptUtil.getTags(nxl, null);
            EvalResponse evalResponse = getAdhocEvaluationResponse(nxl);
            rightsList = evalResponse.getRights();

            boolean isExpired;
            boolean isNotYetValid;
            if (rightsList.length > 0) {
                FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
                isExpired = AdhocEvalAdapter.isFileExpired(policy);
                isNotYetValid = AdhocEvalAdapter.isNotYetValid(policy);
                if (isExpired || isNotYetValid) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(originalMembership), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(projectId), filePath, fileName, filePath, null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    throw new ValidateException("File expired or set for future validation");
                }
            } else {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, parentTenant.getName(), userEval, tags);
                rightsList = PolicyEvalUtil.getUnionRights(responses);
            }

        }
        if (!checkUserFileRights(downloadType, rightsList) && ops != null) {
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(originalMembership), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(projectId), filePath, fileName, filePath, null, null, null, Constants.AccessResult.DENY, new Date(), null, Constants.AccountType.PROJECT);
            RemoteLoggingMgmt.saveActivityLog(activity);
            throw new UnauthorizedOperationException("Access Denied");
        }

        return rightsList;
    }

    public boolean checkProjectExist(int projectId) {
        try (DbSession session = DbSession.newSession()) {
            return session.get(Project.class, projectId) != null;
        }
    }

    public Set<String> extractEmails(JsonRequest request) {
        Set<String> strList = new HashSet<>();
        List<JsonWraper> wrapperList = request.getParameterAsList("emails");
        if (wrapperList != null) {
            for (JsonWraper wrapper : wrapperList) {
                if (wrapper != null) {
                    strList.add(wrapper.stringValue().toLowerCase());
                }
            }
        }
        return strList;
    }

    public void sendInvitationAcceptedMail(HttpServletRequest request, ProjectInvitation invitation,
        User invitee) {
        Properties prop = new Properties();
        String path = HTTPUtil.getURI(request);
        prop.setProperty(Mail.BASE_URL, path);
        prop.setProperty(Mail.KEY_RECIPIENT, invitation.getInviter().getEmail());
        prop.setProperty(Mail.KEY_FULL_NAME, invitation.getInviter().getDisplayName());
        prop.setProperty(Mail.PROJECT_NAME, StringEscapeUtils.escapeHtml4(invitation.getProject().getDisplayName()));
        prop.setProperty("inviteeName", invitee.getDisplayName());
        prop.setProperty("inviteeEmail", invitee.getEmail());
        Locale locale = request.getLocale();
        Sender.send(prop, "invitationAccepted", locale);
    }

    public void sendInvitationDeclinedMail(HttpServletRequest request, User inviter, String inviteeEmail,
        String reason, Project project) {
        Properties prop = new Properties();
        String path = HTTPUtil.getURI(request);
        prop.setProperty(Mail.BASE_URL, path);
        prop.setProperty(Mail.KEY_RECIPIENT, inviter.getEmail());
        prop.setProperty(Mail.KEY_FULL_NAME, inviter.getDisplayName());
        prop.setProperty("inviteeEmail", inviteeEmail);
        prop.setProperty(Mail.PROJECT_NAME, StringEscapeUtils.escapeHtml4(project.getDisplayName()));
        Locale locale = request.getLocale();
        if (StringUtils.hasText(reason)) {
            prop.setProperty(Mail.DECLINE_REASON, reason);
            Sender.send(prop, "invitationDeclinedWithReason", locale);
        } else {
            Sender.send(prop, "invitationDeclined", locale);
        }
    }

    public void sendInvitationReminderMail(HttpServletRequest request, ProjectInvitation invitation)
            throws GeneralSecurityException {
        Properties prop = new Properties();
        String path = HTTPUtil.getURI(request);
        String userName = invitation.getInviter().getDisplayName();
        if (!StringUtils.hasText(userName)) {
            userName = invitation.getInviter().getEmail();
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        String expiryDate = formatter.format(invitation.getExpireDate());
        String queryParams = ProjectService.getInvitationURLQueryString(String.valueOf(invitation.getId()));
        String inviteURL = path + "/invitation" + queryParams;
        prop.setProperty(Mail.KEY_RECIPIENT, invitation.getInviteeEmail());
        prop.setProperty("projectName", StringEscapeUtils.escapeHtml4(invitation.getProject().getDisplayName()));
        prop.setProperty("ownerFullName", userName);
        prop.setProperty("ownerEmail", invitation.getInviter().getEmail());
        prop.setProperty(Mail.BASE_URL, path);
        prop.setProperty("inviteURL", inviteURL);
        prop.setProperty("expiryDate", expiryDate);
        if (StringUtils.hasText(invitation.getInvitationMsg())) {
            prop.setProperty(Mail.INVITATION_MSG_PREFIX, RMSMessageHandler.getClientString(Mail.INVITATION_MSG_PREFIX));
            prop.setProperty(Mail.INVITATION_MSG, "<p><b>\"</b>" + StringEscapeUtils.escapeHtml4(invitation.getInvitationMsg()) + "<b>\"</b></p>");
        } else {
            prop.setProperty(Mail.INVITATION_MSG, "");
            prop.setProperty(Mail.INVITATION_MSG_PREFIX, "");
        }
        Locale locale = request.getLocale();
        Sender.send(prop, "invitationReminder", locale);

    }

}
