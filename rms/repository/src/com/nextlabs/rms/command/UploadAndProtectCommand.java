package com.nextlabs.rms.command;

import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.shared.UploadFileRequest;
import com.nextlabs.rms.shared.UploadFileResponse;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UploadAndProtectCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        processReq(request, response);
    }

    public void processReq(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DbSession session = DbSession.newSession();
        RMSUserPrincipal userPrincipal;
        try {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } finally {
            session.close();
        }
        try {
            DefaultRepositoryManager.checkVaultStorageExceeded(userPrincipal);
        } catch (VaultStorageExceededException e) {
            UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("vaultStorageExceedException"), request.getHeader("Accept"), response);
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileProcessErr"), request.getHeader("Accept"), response);
        }

        String value = "";
        if (request.getHeader("Accept") != null) {
            value = request.getHeader("Accept");
        }

        String path = HTTPUtil.getInternalURI(request);
        String membershipId = SharedFileManager.getMembership(path, userPrincipal);

        UUID uuid = UUID.randomUUID();
        WebConfig webConfig = WebConfig.getInstance();
        File tmpDir = new File(webConfig.getCommonSharedTempDir(), uuid.toString());
        if (!tmpDir.exists()) {
            FileUtils.mkdir(tmpDir);
        }
        String rightsJSON;
        String watermark;
        String expiry;
        String originalName;
        File file = null;
        boolean userConfirmedFileOverwrite = false;

        String efsId = request.getParameter("efsId");
        if (StringUtils.hasText(efsId)) {
            originalName = request.getParameter("originalName");
            rightsJSON = request.getParameter("rightsJSON");
            watermark = request.getParameter("watermark");
            expiry = request.getParameter("expiry");
            File folder = new File(webConfig.getCommonSharedTempDir(), efsId);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    file = files[0];
                }
            }

            if (file == null) {
                UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileProcessErr"), request.getHeader("Accept"), response);
                return;
            }

        } else {
            UploadFileRequest fileRequest;
            try {
                int thresholdSize = Constants.FILE_UPLD_THRESHOLD_SIZE;
                long requestSize = Constants.FILE_UPLD_MAX_REQUEST_SIZE;
                fileRequest = UploadUtil.readFile(request, thresholdSize, requestSize);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileUploadErr"), request.getHeader("Accept"), response);
                return;
            }

            originalName = fileRequest.getFileName();
            boolean nxFile = originalName.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
            if (nxFile) {
                UploadUtil.sendErrorResponse(originalName, RMSMessageHandler.getClientString("error.protect.already.nxl.file.ext"), request.getHeader("Accept"), response);
                return;
            }
            byte[] bytes = fileRequest.getBytes();
            rightsJSON = fileRequest.getRightsJSON();
            watermark = fileRequest.getWatermark();
            expiry = fileRequest.getExpiry();
            userConfirmedFileOverwrite = fileRequest.isUserConfirmedFileOverwrite();
            try {
                if (StringUtils.hasText(expiry) && !ExpiryUtil.validateExpiry(expiry)) {
                    UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileValidityErr"), request.getHeader("Accept"), response);
                    return;
                }
            } catch (JsonSyntaxException e) {
                UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("err.invalid.params"), request.getHeader("Accept"), response);
                return;
            }
            String destFilePath = tmpDir + File.separator + originalName;
            file = UploadUtil.writeFileToDisk(bytes, destFilePath);
        }

        Rights[] rights;
        if (!StringUtils.hasText(rightsJSON)) {
            UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileUploadErr"), request.getHeader("Accept"), response);
            return;
        } else {
            try {
                List<String> rightsList = GsonUtils.GSON.fromJson(rightsJSON, GsonUtils.GENERIC_LIST_TYPE);
                rights = SharedFileManager.toRights(rightsList.toArray(new String[rightsList.size()]));
            } catch (Exception e) {
                logger.error("Error in converting rights: {}", e.getMessage(), e);
                UploadUtil.sendErrorResponse("", RMSMessageHandler.getClientString("fileUploadErr"), request.getHeader("Accept"), response);
                return;
            }
        }
        if (ArrayUtils.contains(rights, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
            JsonWatermark item = null;
            if (userPrincipal != null) {
                item = WatermarkConfigManager.getWaterMarkConfig(HTTPUtil.getInternalURI(request), userPrincipal.getTenantName(), userPrincipal.getTicket(), userPrincipal.getPlatformId(), String.valueOf(userPrincipal.getUserId()), userPrincipal.getClientId());
            }
            watermark = item != null ? item.getText() : "";
        }

        boolean retainUploadedFile = Constants.RETAIN_UPLOADED_FILE || StringUtils.hasText(efsId);

        File outputFile = new File(tmpDir, originalName + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        Map<String, String[]> emptyTagsHashMap = new HashMap<>();
        int userId = 0;
        String clientId = null;
        String ticket = null;
        Integer platformId = null;
        if (userPrincipal != null) {
            userId = userPrincipal.getUserId();
            ticket = userPrincipal.getTicket();
            clientId = userPrincipal.getClientId();
            platformId = userPrincipal.getPlatformId();
        }
        NxlFile nxl = null;
        try {
            try (OutputStream os = new FileOutputStream(outputFile)) {
                nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membershipId, rights, watermark, GsonUtils.GSON.fromJson(expiry, JsonExpiry.class), emptyTagsHashMap, file, os));
            }
            if (outputFile.exists() && outputFile.length() >= 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Uploaded file encrypted at {}", outputFile.getAbsolutePath());
                }
                String duid = nxl.getDuid();
                RMSRestHelper.sendActivityLogToRMS(path, ticket, duid, nxl.getOwner(), userId, clientId, Operations.PROTECT, " ", " ", " ", AccessResult.ALLOW, request, null, null, AccountType.PERSONAL);

                String filePolicy = DecryptUtil.getFilePolicyStr(nxl, null);
                SharedFileDAO.updateNxlDBProtect(userId, duid, nxl.getOwner(), Rights.toInt(rights), outputFile.getName(), filePolicy);

                response.setStatus(HttpServletResponse.SC_OK);
                UploadFileResponse fileResponse = new UploadFileResponse();
                fileResponse.setName(outputFile.getName());

                if (!StringUtils.hasText(value) || !value.contains("application/json")) {
                    response.setContentType("text/plain");
                }

                fileResponse.setProtectionType(ProtectionType.ADHOC.ordinal());

                try {
                    RepositoryFileUtil.uploadFileToMyVault(userPrincipal, null, null, null, null, originalName, outputFile, false, FileUtils.getName(outputFile.getName()), false, userConfirmedFileOverwrite, request);
                    JsonUtil.writeJsonToResponse(fileResponse, response);
                    return;
                } catch (IOException | RepositoryException | NxlException e) {
                    logger.error(e.getMessage(), e);
                }
                UploadUtil.sendErrorResponse(originalName, RMSMessageHandler.getClientString("fileProcessErr"), request.getHeader("Accept"), response);
            } else {
                UploadUtil.sendErrorResponse(originalName, RMSMessageHandler.getClientString("fileProcessErr"), request.getHeader("Accept"), response);
            }
        } catch (Throwable e) {
            logger.error("Error in encrypting: {}", e.getMessage(), e);
            UploadUtil.sendErrorResponse(originalName, RMSMessageHandler.getClientString("fileProtectErr", file.getName()), value, response);
        } finally {
            IOUtils.closeQuietly(nxl);
            if (!retainUploadedFile) {
                boolean deleted = file.delete();
                if (!deleted) {
                    logger.warn("Unable to delete uploaded file: {}", file.getAbsolutePath());
                }
            }
        }
    }
}
