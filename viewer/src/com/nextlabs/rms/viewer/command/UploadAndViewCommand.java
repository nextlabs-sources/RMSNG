package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.shared.UploadFileRequest;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.FileConversionHandler;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.NotAuthorizedException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.json.ShowFileResponse;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.Audit;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UploadAndViewCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        processReq(request, response);
    }

    public void processReq(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UploadFileRequest fileRequest = null;
        request.getSession(true);
        String viewingSessionId = UUID.randomUUID().toString();
        String tempDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + viewingSessionId;

        try {
            int thresholdSize = ViewerConfigManager.getInstance().getIntProperty(ViewerConfigManager.FILE_UPLD_THRESHOLD_SIZE);
            long requestSize = ViewerConfigManager.getInstance().getIntProperty(ViewerConfigManager.FILE_UPLD_MAX_REQUEST_SIZE);
            fileRequest = UploadUtil.readFile(request, thresholdSize, requestSize);
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when view local file.");
            }
            UploadUtil.sendErrorResponse("", ViewerMessageHandler.getClientString("err.file.upload"), request.getHeader("Accept"), response);
            return;
        } catch (UnsupportedEncodingException e) {
            UploadUtil.sendErrorResponse("", ViewerMessageHandler.getClientString("err.file.upload"), request.getHeader("Accept"), response);
            return;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            UploadUtil.sendErrorResponse("", ViewerMessageHandler.getClientString("err.file.upload"), request.getHeader("Accept"), response);
            return;
        }

        String originalName = fileRequest.getFileName();
        byte[] content = fileRequest.getBytes();
        String userName = fileRequest.getUserName();
        int offset = Integer.parseInt(fileRequest.getOffset());
        String tenantName = fileRequest.getTenantName();
        String op = request.getParameter("operations");

        Audit.audit(request, "Command", "UploadAndView", "UploadAndView", 0, userName, tenantName, originalName);

        String value = "";
        if (request.getHeader("Accept") != null) {
            value = request.getHeader("Accept");
        }
        int operations = -1;
        if (StringUtils.hasText(op)) {
            operations = Integer.parseInt(op);
        }
        String destFilePath = tempDir + File.separator + originalName;
        File file = UploadUtil.writeFileToDisk(content, destFilePath);
        User user = ViewerUtil.extractUserFromRequest(request);
        if (user == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Cannot retrieve userId and ticket from cookies.");
            }
            UploadUtil.sendErrorResponse(originalName, ViewerMessageHandler.getClientString("err.unauthorized.view"), request.getHeader("Accept"), response);
            return;
        }
        user.setTenantName(tenantName);
        user.setEmail(userName);
        user.setIpAddress(request.getRemoteAddr());

        String ticket = user.getTicket();
        String uid = user.getId();
        Integer platformId = user.getPlatformId();
        String deviceId = user.getDeviceId();
        String clientId = user.getClientId();
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        FileOutputStream output = null;
        FileInputStream inputFile = null;
        String redirectURL = null;
        try {
            EvaluationHandler evalHandler = new EvaluationHandler();
            CachedFile cachedFile = evalHandler.evaluateAndDecrypt(file, user, offset);
            cachedFile.setLastModifiedDate(file.lastModified());

            String efsId = UUID.randomUUID().toString();
            File tmpFolder = new File(ViewerConfigManager.getInstance().getCommonSharedTempDir(), efsId);
            FileUtils.mkdir(tmpFolder);
            output = new FileOutputStream(new File(tmpFolder, cachedFile.getOriginalFileName()));
            inputFile = new FileInputStream(file);
            IOUtils.copy(inputFile, output);
            cachedFile.setEfsId(efsId);

            String docId = System.currentTimeMillis() + UUID.randomUUID().toString();
            FileCacheId fileCacheId = new FileCacheId(viewingSessionId, docId);

            String duid = cachedFile.getDuid();
            String membership = cachedFile.getMembership();
            if (StringUtils.hasText(duid) && StringUtils.hasText(membership)) {
                String ownerTenantName = StringUtils.substringAfter(membership, "@");
                if (!ownerTenantName.equals(user.getTenantName())) {
                    cachedFile.setProjectId("0");
                }
                String rmsURL = ViewerUtil.getRMSInternalURL(ownerTenantName);
                RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, duid, membership, Integer.parseInt(uid), clientId, deviceId, platformId, Operations.VIEW, " ", " ", " ", AccessResult.ALLOW, request, prop, null, AccountType.PERSONAL);
            }

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
            FileConversionHandler conversionHandler = new FileConversionHandler();
            redirectURL = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), file);
            if (operations >= 0) {
                String separator = redirectURL.contains("?") ? "&" : "?";
                redirectURL = new StringBuilder(redirectURL).append(separator).append("operations=").append(operations).toString();
            }

            response.setStatus(HttpServletResponse.SC_OK);
            ShowFileResponse fileResponse = new ShowFileResponse();
            fileResponse.setRights(cachedFile.getRights());
            fileResponse.setOwner(cachedFile.isOwner());
            fileResponse.setName(originalName);
            fileResponse.setViewerUrl(HTTPUtil.getURI(request) + redirectURL);
            if (StringUtils.hasText(duid)) {
                fileResponse.setDuid(duid);
            }
            if (StringUtils.hasText(membership)) {
                fileResponse.setMembership(membership);
            }
            if (!StringUtils.hasText(value) || !value.contains("application/json")) {
                response.setContentType("text/plain");
            }
            JsonUtil.writeJsonToResponse(fileResponse, response);
        } catch (UnsupportedFormatException e) {
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unsupported", 415);
            return;
        } catch (RMSException e) {
            LOGGER.error(e.getMessage());
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized.view", 500);
            return;
        } catch (NotAuthorizedException e) {
            try {
                String ownerTenantName = StringUtils.substringAfter(e.getOwner(), "@");
                String rmsURL = ViewerUtil.getRMSInternalURL(ownerTenantName);
                RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, e.getDuid(), e.getOwner(), Integer.parseInt(uid), clientId, deviceId, platformId, Operations.VIEW, " ", " ", " ", AccessResult.DENY, request, prop, null, AccountType.PERSONAL);
            } catch (IOException ex) {
                LOGGER.error("Error occurred when sending activity log: {}", ex.getMessage(), ex);
            } finally {
                ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized.view", 403);
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error(e.getMessage());
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized.view", 403);
            return;
        } catch (NxlException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e.getMessage());
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unsupported.nxl", 5007);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage());
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized.view", 500);
            return;
        } finally {
            IOUtils.closeQuietly(inputFile);
            FileUtils.deleteQuietly(new File(tempDir));
            IOUtils.closeQuietly(output);
        }
    }
}
