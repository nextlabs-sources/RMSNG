package com.nextlabs.rms.command;

import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.FileExpiredException;
import com.nextlabs.rms.exception.FileTenantMismatchException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.json.SharedFileResponse;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.share.SharePersonalMapper;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShareFileCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String PARAM_SHARE_WITH = "shareWith";
    private static final String PARAM_RIGHTS_GRANTED = "rightsGranted";
    private static final String PARAM_FILE_PATH = "filePath";
    private static final String PARAM_FILE_PATH_ID = "filePathId";
    private static final String PARAM_REPO_ID = "repoId";
    private static final String PARAM_REPO_NAME = "repoName";
    private static final String PARAM_EFS_ID = "efsId";
    private static final String PARAM_DUID = "duid";
    public static final String PARAM_IS_FILE_ATTACHED = "isFileAttached";
    public static final String DATE_FORMAT = "MMMM d, yyyy";
    private static final String PARAM_FILE_NAME = "fileName";
    private static final String PARAM_UPDATE_RECIPIENTS = "updateRecipients";
    private static final String PARAM_FROM_VIEWER = "fromViewer";
    private static final String PARAM_COMMENT = "comment";
    private static final String PARAM_WATERMARK = "watermark";
    private static final String PARAM_EXPIRY = "expiry";
    private static final String PARAM_FROM_SPACE = "fromSpace";
    private static final String PARAM_FROM_SPACE_ID = "fromSpaceId";
    private static final String PARAM_SHARE_WITH_PROJECT = "shareWithProject";
    private static final String PARAM_USER_FILE_OVERWRITE = "userConfirmedFileOverwrite";

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String repoId = null;
        String filePath = null;
        String filePathId = null;
        File outputPath = null;
        RMSUserPrincipal userPrincipal = authenticate(request);
        if (userPrincipal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SharedFileResponse result = new SharedFileResponse();
        try {
            //boolean isFileAttached = Boolean.parseBoolean(request.getParameter(PARAM_IS_FILE_ATTACHED));
            boolean isFileAttached = false;
            String updateRecipients = request.getParameter(PARAM_UPDATE_RECIPIENTS);
            boolean update = Boolean.parseBoolean(updateRecipients);
            List<String> shareWith = new ArrayList<String>(Arrays.asList(request.getParameter(PARAM_SHARE_WITH).split(",")));

            String fromSpace = request.getParameter(PARAM_FROM_SPACE);
            String fromSpaceId = request.getParameter(PARAM_FROM_SPACE_ID);
            Set<String> recipients = new LinkedHashSet<>();
            SHARESPACE fromShareSpace = (!StringUtils.hasText(fromSpace)) ? null : SHARESPACE.valueOf(fromSpace);
            if (!SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
                if (!EmailUtils.validateEmails(shareWith)) {
                    result.setResult(false);
                    result.addMessage(RMSMessageHandler.getClientString("shareFileEmailError"));
                    JsonUtil.writeJsonToResponse(result, response);
                    return;
                }
                recipients = new LinkedHashSet<>(shareWith);
            }
            String[] rightsGranted = request.getParameter(PARAM_RIGHTS_GRANTED).split(",");
            String fileName = request.getParameter(PARAM_FILE_NAME);
            String repoName = request.getParameter(PARAM_REPO_NAME);
            String efsId = request.getParameter(PARAM_EFS_ID);
            String fromViewer = request.getParameter(PARAM_FROM_VIEWER);
            String comment = request.getParameter(PARAM_COMMENT);
            String watermark = request.getParameter(PARAM_WATERMARK);
            String expiry = request.getParameter(PARAM_EXPIRY);
            String userConfirmedFileOverwriteStr = request.getParameter(PARAM_USER_FILE_OVERWRITE);
            boolean userConfirmedFileOverwrite = false;
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
            String duid = request.getParameter(PARAM_DUID);
            outputPath = RepositoryFileUtil.getTempOutputFolder();
            if (update) {
                StoreItem item = new SharePersonalMapper().getStoreItemByDuid(duid);
                filePath = item.getFilePathDisplay();
                filePathId = item.getFilePath();
                repoId = item.getRepoId();
            } else {
                validateInput(request);
                filePath = request.getParameter(PARAM_FILE_PATH);
                filePathId = request.getParameter(PARAM_FILE_PATH_ID);
                repoId = request.getParameter(PARAM_REPO_ID);
            }
            String tenantId = userPrincipal.getTenantId();
            int userId = userPrincipal.getUserId();
            String ticket = userPrincipal.getTicket();
            String clientId = userPrincipal.getClientId();
            String deviceId = userPrincipal.getDeviceId();
            Integer platformId = userPrincipal.getPlatformId();
            String sharingUserEmail = userPrincipal.getEmail();
            String deviceType = com.nextlabs.common.shared.DeviceType.WEB.toString();
            String path = HTTPUtil.getInternalURI(request);

            if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
                rightsGranted = null;
            }
            if (StringUtils.hasText(userConfirmedFileOverwriteStr)) {
                userConfirmedFileOverwrite = Boolean.valueOf(userConfirmedFileOverwriteStr);
            }
            Rights[] desiredRights = SharedFileManager.toRights(rightsGranted);
            String ownerMembership = SharedFileManager.getMembership(path, userPrincipal);
            SharedFileDTO dto = SharedFileManager.generateSharedFileDTO(deviceId, deviceType, duid, filePath, filePathId, fileName, repoId, repoName, tenantId, userId, ticket, sharingUserEmail, recipients, desiredRights, new Date(), false, ownerMembership, clientId, platformId, comment, null, userConfirmedFileOverwrite);
            if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
                dto.setSourceProjectId(Integer.parseInt(fromSpaceId));
                List<String> sharedWithProject = new ArrayList<String>(Arrays.asList(request.getParameter(PARAM_SHARE_WITH_PROJECT).split(",")));
                dto.setSharedWithProject(sharedWithProject);
            }
            try {
                String baseURL = SharedFileManager.getBaseShareURL(request, userPrincipal.getLoginTenant());
                SharedFileManager.shareFile(path, dto, baseURL, result, efsId, isFileAttached, watermark, expiry, request, response);
            } catch (FileAlreadyRevokedException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileRevokedError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (UnauthorizedOperationException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("unAuthorizedError"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileExpiredException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileExpiredException"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (VaultStorageExceededException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("vaultShareStorageExceedException"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileNotFoundException e) {
                result.setResult(false);
                result.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
                result.addMessage(RMSMessageHandler.getClientString("err.file.download.missing"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (FileTenantMismatchException e) {
                result.setResult(false);
                result.addMessage(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            result.setResult(true);
            boolean isNxl = fileName.toLowerCase().endsWith(Constants.NXL_FILE_EXTN);
            if (isNxl) {
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
                if (!"true".equals(fromViewer)) {
                    result.addMessage(RMSMessageHandler.getClientString("shareFileLinkMyVault"));
                }
            }
            result.setProtectionType(ProtectionType.ADHOC.ordinal());
            JsonUtil.writeJsonToResponse(result, response);
        } catch (BadRequestException e) {
            result.setResult(false);
            result.addMessage(e.getMessage());
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (GeneralSecurityException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (RMSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.addMessage(RMSMessageHandler.getClientString("shareFileError"));
            JsonUtil.writeJsonToResponse(result, response);
        } finally {
            FileUtils.deleteQuietly(outputPath);
        }
    }

    private void validateInput(HttpServletRequest request) throws BadRequestException {
        String shareWith = request.getParameter(PARAM_SHARE_WITH);
        String rightsGranted = request.getParameter(PARAM_RIGHTS_GRANTED);
        String filePath = request.getParameter(PARAM_FILE_PATH);
        String filePathId = request.getParameter(PARAM_FILE_PATH_ID);
        String repoId = request.getParameter(PARAM_REPO_ID);
        String efsId = request.getParameter(PARAM_EFS_ID);
        String fromSpace = request.getParameter(PARAM_FROM_SPACE);
        SHARESPACE fromShareSpace = (!StringUtils.hasText(fromSpace)) ? null : SHARESPACE.valueOf(fromSpace);
        if (!StringUtils.hasText(shareWith)) {
            throw new BadRequestException(RMSMessageHandler.getClientString("shareFileWithPeopleRequired"));
        } else if (!SHARESPACE.PROJECTSPACE.equals(fromShareSpace) && !StringUtils.hasText(rightsGranted)) {
            throw new BadRequestException(RMSMessageHandler.getClientString("shareFileWithRightsRequired"));
        } else if ((!StringUtils.hasText(filePath) || !StringUtils.hasText(filePathId) || !StringUtils.hasText(repoId)) && !StringUtils.hasText(efsId)) {
            throw new BadRequestException(RMSMessageHandler.getClientString("shareFileError"));
        }
    }

}
