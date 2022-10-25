package com.nextlabs.rms.viewer.command;

import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.RMSViewerContentManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoveFromCacheCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        String documentId = request.getParameter("documentId");
        String viewingSessionId = request.getParameter("s");
        try {
            if (documentId == null || documentId.length() == 0) {
                return;
            }
            RMSViewerContentManager mgr = RMSViewerContentManager.getInstance();

            mgr.removeDocumentFromCache(documentId, viewingSessionId);
            final File parentPath = new File(ViewerConfigManager.getInstance().getWebDir() + File.separator + ViewerConfigManager.TEMPDIR_NAME);
            File docTempDir = new File(parentPath, viewingSessionId);
            if (parentPath.equals(docTempDir.getParentFile()) && docTempDir.exists()) {
                try {
                    FileUtils.deleteDirectory(docTempDir);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Deleted doc temp dir:" + docTempDir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error occurred while deleting doc temp dir:" + docTempDir.getAbsolutePath(), e);
                }
            }
            if (ViewerCacheManager.getInstance().isStatelessMode()) {
                File tempFile = new File(ViewerConfigManager.getInstance().getViewerSharedTempDir(), viewingSessionId);
                if (tempFile.exists()) {
                    FileUtils.deleteQuietly(tempFile);
                }
            }
        } finally {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }
}
