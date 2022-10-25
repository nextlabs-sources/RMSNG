package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.exception.CacheNotFoundException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetFileContentCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String docId = request.getParameter("d");
        String viewingSessionId = request.getParameter("s");
        if (!StringUtils.hasText(docId) || !StringUtils.hasText(viewingSessionId)) {
            response.sendError(400, "Missing parameter");
            return;
        }
        String documentId = null;
        try {
            documentId = URLDecoder.decode(docId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            if (!response.isCommitted()) {
                response.sendError(501, "Internal server error.");
            }
            return;
        } catch (NumberFormatException e) {
            if (!response.isCommitted()) {
                response.sendError(501, "Internal server error.");
            }
            return;
        } catch (IllegalArgumentException e) {
            if (!response.isCommitted()) {
                response.sendError(501, "Internal server error.");
            }
            return;
        }

        try {
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(new FileCacheId(viewingSessionId, documentId));
            LOGGER.debug("Getting content from document: {} with documentId: {}", cachedFile.getFileName(), documentId);

            response.setContentType(cachedFile.getMediaType().toString());
            response.getOutputStream().write(cachedFile.getFileContent());
            response.flushBuffer();
        } catch (RMSException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                if (e instanceof CacheNotFoundException) {
                    response.sendError(404);
                } else {
                    response.sendError(400);
                }
            }
        }
    }

}
