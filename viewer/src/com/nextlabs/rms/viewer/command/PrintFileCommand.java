package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.FileTypeDetector;
import com.nextlabs.rms.viewer.conversion.RMSViewerContentManager;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.json.PrintFileUrl;
import com.nextlabs.rms.viewer.json.RepoFile;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.Audit;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

public class PrintFileCommand extends AbstractCommand {

    private static Logger logger = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        User user = ViewerUtil.extractUserFromRequest(request);
        if (user == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Cannot retrieve userId and ticket from cookies.");
            }
            PrintFileUrl printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.unauthorized.print", "");
            JsonUtil.writeJsonToResponse(printFileUrl, response);
            return;
        }

        String viewingSessionId = request.getParameter("s");
        String documentId = request.getParameter("documentId");
        if (!StringUtils.hasText(documentId) || !StringUtils.hasText(viewingSessionId)) {
            PrintFileUrl printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.cache.not.found", "");
            JsonUtil.writeJsonToResponse(printFileUrl, response);
            return;
        }
        Audit.audit(request, "Command", "PrintFile", "Print", 0, user.getId(), documentId);

        CachedFile cachedFile = null;
        Properties prop = null;
        AccountType type = null;
        String rmsURL = null;
        boolean shouldLog = true;
        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, documentId);
        try {
            cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
            shouldLog = StringUtils.hasText(cachedFile.getDuid()) && StringUtils.hasText(cachedFile.getMembership());
            if (shouldLog) {
                String ownerTenantName = StringUtils.substringAfter(cachedFile.getMembership(), "@");
                rmsURL = ViewerUtil.getRMSInternalURL(ownerTenantName);
                prop = new Properties();
                prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
                type = cachedFile.getProjectId() != null ? AccountType.PROJECT : AccountType.PERSONAL;
            }
        } catch (RMSException e) {
            logger.error("Unable to find document in cache (Document ID: " + documentId + ")", e);
            PrintFileUrl printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.cache.not.found", "");
            JsonUtil.writeJsonToResponse(printFileUrl, response);
            return;
        }

        RepoFile repoFileDetails = cachedFile.getRepoFile();
        final String repoId = repoFileDetails != null ? repoFileDetails.getRepoId() : null;
        final String fileId = repoFileDetails != null ? repoFileDetails.getFileId() : null;
        final String filePathDisplay = repoFileDetails != null ? repoFileDetails.getFilePathDisplay() : null;
        RMSViewerContentManager contentMgr = RMSViewerContentManager.getInstance();
        boolean isPrintAllowed = contentMgr.checkPrintPermission(documentId, viewingSessionId);
        String deviceId = user.getDeviceId();
        Integer platformId = user.getPlatformId();
        if (!isPrintAllowed) {
            if (shouldLog) {
                RMSRestHelper.sendActivityLogToRMS(rmsURL, user.getTicket(), cachedFile.getDuid(), cachedFile.getMembership(), Integer.parseInt(user.getId()), user.getClientId(), deviceId, platformId, Operations.PRINT, repoId, fileId, filePathDisplay, AccessResult.DENY, request, prop, null, type);
            }
            PrintFileUrl printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.unauthorized.print", "");
            JsonUtil.writeJsonToResponse(printFileUrl, response);
            return;
        }
        File parentPath = new File(ViewerConfigManager.getInstance().getWebDir(), ViewerConfigManager.TEMPDIR_NAME);
        File tempWebDir = new File(parentPath, viewingSessionId);
        if (!parentPath.equals(tempWebDir.getParentFile())) {
            PrintFileUrl printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.generic", "");
            JsonUtil.writeJsonToResponse(printFileUrl, response);
            return;
        }
        if (!tempWebDir.exists()) {
            FileUtils.mkdir(tempWebDir);
        }
        PrintFileUrl printFileUrl = null;
        try {
            printFileUrl = contentMgr.generatePDF(documentId, tempWebDir.getAbsolutePath(), viewingSessionId);
            File convertedFile = new File(ViewerConfigManager.getInstance().getWebDir() + File.separator + URLDecoder.decode(printFileUrl.getUrl(), "UTF-8"));
            final int bufferSize = (int)(convertedFile.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : convertedFile.length());
            byte[] fileContent = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            InputStream is = null;
            try {
                is = new FileInputStream(convertedFile);
                IOUtils.copy(is, baos);
                fileContent = baos.toByteArray();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(baos);
            }
            cachedFile.setFileContent(fileContent);
            MediaType mediaType = FileTypeDetector.getMimeType(fileContent, convertedFile.getName());
            cachedFile.setMediaType(mediaType);
            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
            String redirectURL = new StringBuilder("/viewer/RMSViewer/GetFileContent?d=").append(documentId).append("&s=").append(viewingSessionId).toString();
            printFileUrl = new PrintFileUrl(redirectURL, printFileUrl.getError());
            if (shouldLog) {
                RMSRestHelper.sendActivityLogToRMS(rmsURL, user.getTicket(), cachedFile.getDuid(), cachedFile.getMembership(), Integer.parseInt(user.getId()), user.getClientId(), deviceId, platformId, Operations.PRINT, repoId, fileId, filePathDisplay, AccessResult.ALLOW, request, prop, null, type);
            }
        } catch (RMSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            printFileUrl = new PrintFileUrl(ViewerConfigManager.VIEWER_CONTEXT_NAME + "/ShowError.jsp?code=err.generic", "");
        }
        JsonUtil.writeJsonToResponse(printFileUrl, response);
    }
}
