package com.nextlabs.rms.command;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.FileExpiredException;
import com.nextlabs.rms.exception.FileTenantMismatchException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.json.SharedFileResponse;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.UploadFileRequest;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProtectAndShareCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        UploadFileRequest fileRequest = null;
        request.getSession(true);
        SharedFileResponse result = new SharedFileResponse();
        File tempDir = RepositoryFileUtil.getTempOutputFolder();
        int thresholdSize = UploadUtil.THRESHOLD_SIZE;
        long requestSize = UploadUtil.REQUEST_SIZE;
        try {
            fileRequest = UploadUtil.readFile(request, thresholdSize, requestSize);
            String originalName = fileRequest.getFileName();
            byte[] content = fileRequest.getBytes();

            String destFilePath = tempDir + File.separator + originalName;
            File file = UploadUtil.writeFileToDisk(content, destFilePath);
            RMSUserPrincipal userPrincipal = authenticate(request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            try {
                DefaultRepositoryManager.checkVaultStorageExceeded(userPrincipal);
            } catch (VaultStorageExceededException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("vaultStorageExceedException"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (InvalidDefaultRepositoryException | RepositoryException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("shareFileEmailError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            if (UploadFileCommand.invalidNxlUploadMembership(file)) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            String duid = "";
            Gson gson = new Gson();

            // boolean isFileAttached = fileRequest.isFileAttached();
            boolean isFileAttached = false;
            int userId = userPrincipal.getUserId();
            String tenantId = userPrincipal.getTenantId();
            String ticket = userPrincipal.getTicket();
            String clientId = userPrincipal.getClientId();
            Integer platformId = userPrincipal.getPlatformId();
            String deviceId = userPrincipal.getDeviceId();
            String sharingUserEmail = userPrincipal.getEmail();
            String deviceType = com.nextlabs.common.shared.DeviceType.WEB.toString();
            String path = HTTPUtil.getInternalURI(request);

            String jsonRights = fileRequest.getRightsJSON();
            String watermark = fileRequest.getWatermark();
            String expiry = fileRequest.getExpiry();
            try {
                if (StringUtils.hasText(expiry) && !ExpiryUtil.validateExpiry(expiry)) {
                    result.setResult(false);
                    result.addMessage(RMSMessageHandler.getClientString("fileValidityErr"));
                    JsonUtil.writeJsonToResponse(result, response);
                    return;
                }
            } catch (JsonSyntaxException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("err.invalid.params"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            String[] rightsArray = gson.fromJson(jsonRights, GsonUtils.STRING_ARRAY_TYPE);
            Rights[] desiredRights = SharedFileManager.toRights(rightsArray);
            String ownerMembership = SharedFileManager.getMembership(path, userPrincipal);
            String emails = fileRequest.getShareWithJSON();
            List<String> shareWith = gson.fromJson(emails, GsonUtils.GENERIC_LIST_TYPE);

            if (shareWith == null || !EmailUtils.validateEmails(shareWith)) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("shareFileEmailError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            Set<String> recipients = new LinkedHashSet<>(shareWith);
            boolean userConfirmedFileOverwrite = fileRequest.isUserConfirmedFileOverwrite();
            SharedFileDTO dto = SharedFileManager.generateSharedFileDTO(deviceId, deviceType, duid, destFilePath, destFilePath, fileRequest.getFileName(), null, null, tenantId, userId, ticket, sharingUserEmail, recipients, desiredRights, new Date(), false, ownerMembership, clientId, platformId, fileRequest.getComment(), null, userConfirmedFileOverwrite);

            try {
                String baseURL = SharedFileManager.getBaseShareURL(request, userPrincipal.getLoginTenant());
                SharedFileManager.shareFile(path, dto, baseURL, result, null, isFileAttached, watermark, expiry, request, response);
            } catch (FileNotFoundException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("err.file.download.missing"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (UnauthorizedOperationException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileExpiredException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileExpiredException"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileAlreadyRevokedException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileRevokedError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (VaultStorageExceededException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("vaultShareStorageExceedException"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileTenantMismatchException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }

            byte[] nxlHeader = Arrays.copyOf(fileRequest.getBytes(), NxlFile.BASIC_HEADER_SIZE);
            boolean isFileNXL = NxlFile.isNxl(nxlHeader);

            result.setProtectionType(ProtectionType.ADHOC.ordinal());

            if (isFileNXL) {
                if (isFileAttached) {
                    result.addMessage(RMSMessageHandler.getClientString("shareFileRecipientsAdded"));
                } else {
                    if (StringUtils.hasText(result.getNewSharedEmailsStr())) {
                        result.addMessage(RMSMessageHandler.getClientString("fileSharedMsg", result.getNewSharedEmailsStr()));
                    }
                    if (StringUtils.hasText(result.getAlreadySharedEmailStr())) {
                        result.addMessage(RMSMessageHandler.getClientString("fileAlreadySharedMsg", result.getAlreadySharedEmailStr()));
                    }
                }
            } else {
                if (isFileAttached) {
                    result.addMessage(RMSMessageHandler.getClientString("shareFileAttachmentSent"));
                } else {
                    result.addMessage(RMSMessageHandler.getClientString("shareFileEmailSent"));
                }
                result.addMessage(RMSMessageHandler.getClientString("shareFileLinkMyVault"));
            }
            result.setResult(true);
            JsonUtil.writeJsonToResponse(result, response);
        } catch (FileUploadException | UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("fileUploadErr"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (RMSException | GeneralSecurityException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

}
