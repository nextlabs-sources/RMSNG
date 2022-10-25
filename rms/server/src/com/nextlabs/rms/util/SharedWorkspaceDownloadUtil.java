package com.nextlabs.rms.util;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.MembershipException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.service.SharedWorkspaceService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SharedWorkspaceDownloadUtil extends DownloadUtil {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private SharedWorkspaceService sharedWorkspaceService = new SharedWorkspaceService();

    public HttpServletResponse downloadFile(String repoId, RMSUserPrincipal principal, String path, String filePath,
        int start, long length, DownloadType downloadType, HttpServletResponse response, UserSession us)
            throws IOException, ValidateException, InvalidDefaultRepositoryException, RepositoryException, NxlException,
            GeneralSecurityException, UnauthorizedOperationException, MembershipException, OneDriveServiceException,
            ApplicationRepositoryException, TokenGroupNotFoundException {

        boolean partialDownload = start >= 0 && length >= 0;

        File outputFile = new File(RepositoryFileUtil.getTempOutputFolder().getPath());

        if (filePath.endsWith(".nxl")) {
            getUserFileRights(principal, principal.getTenantId(), us, path, filePath, outputFile, downloadType, partialDownload, repoId);
        }

        if (partialDownload && downloadType != DownloadType.NORMAL) {
            byte[] data;
            try {
                data = sharedWorkspaceService.downloadPartialFile(repoId, principal, null, filePath, start, start + length - 1);

                if (data != null) {
                    long contentLength = data.length;
                    response.setHeader("x-rms-file-size", Long.toString(contentLength));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                    response.getOutputStream().write(data);
                }
            } catch (ApplicationRepositoryException | RepositoryException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        } else {
            String outputPath;
            try {
                outputPath = RepositoryFileUtil.getTempOutputFolder().getPath();

                File output = sharedWorkspaceService.downloadfile(us, repoId, principal, path, filePath, outputPath, null, downloadType);
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(output.getName()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            } catch (IOException | OneDriveServiceException | ApplicationRepositoryException | RepositoryException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return response;
    }

    public Rights[] getUserFileRights(RMSUserPrincipal principal, String loginTenantId, UserSession us, String path,
        String filePath, File outputPath, Constants.DownloadType downloadType, boolean partialDownload,
        String repoId) throws InvalidDefaultRepositoryException, RepositoryException, IOException, NxlException,
            GeneralSecurityException, UnauthorizedOperationException, MembershipException, ValidateException,
            OneDriveServiceException, ApplicationRepositoryException, TokenGroupNotFoundException {

        Map<String, String[]> tags;
        String originalMembership;
        Rights[] rightsList;
        Operations ops = getDownloadOps(downloadType, partialDownload);
        File input = sharedWorkspaceService.downloadfile(us, repoId, principal, path, filePath, outputPath.getPath(), null, downloadType);
        String fileName = input.getName();
        String duid = null;
        try (InputStream is = new FileInputStream(input); NxlFile nxl = NxlFile.parse(is)) {
            originalMembership = nxl.getOwner();
            duid = nxl.getDuid();
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
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, null, userEval, tags);
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
