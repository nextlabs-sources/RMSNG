package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.conversion.RMSViewerContentManager;
import com.nextlabs.rms.viewer.exception.CacheNotFoundException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetDocContentCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pageNum = request.getParameter("p");
        String docId = request.getParameter("d");
        String viewingSessionId = request.getParameter("s");
        String z = request.getParameter("z");
        if (!StringUtils.hasText(docId) || !StringUtils.hasText(pageNum) || !StringUtils.hasText(viewingSessionId)) {
            response.sendError(400, "Missing parameter");
            return;
        }
        String documentId = null;
        int page = -1;
        int zoom = 0;
        try {
            documentId = URLDecoder.decode(docId, "UTF-8");
            page = Integer.parseInt(pageNum);
            if (StringUtils.hasText(z)) {
                zoom = Integer.parseInt(z);
            }
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

        RMSViewerContentManager manager = RMSViewerContentManager.getInstance();
        try {
            response.setDateHeader("Expires", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
            response.setHeader("Cache-Control", "max-age=" + TimeUnit.DAYS.toSeconds(2));
            manager.generateContent(documentId, page, viewingSessionId, zoom, response);
            response.flushBuffer();
        } catch (ReflectiveOperationException e) {
            if (!response.isCommitted()) {
                response.sendError(501, "Internal server error.");
            }
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
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred: {}", e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(400, "IO error.");
            }
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Error occurred (pageNo: {}, docId: {}): {}", pageNum, docId, e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(501, "Internal server error.");
            }
        }
    }

}
