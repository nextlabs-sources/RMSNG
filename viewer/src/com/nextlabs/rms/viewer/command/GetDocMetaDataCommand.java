package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.viewer.conversion.DocumentMetaData;
import com.nextlabs.rms.viewer.conversion.RMSViewerContentManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetDocMetaDataCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String userId = "";
        String source = request.getParameter("source");
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if ("userId".equals(cookie.getName())) {
                    userId = cookie.getValue();
                } else if ("rmx".equals(source) && "rmxUserId".equals(cookie.getName())) {
                    userId = cookie.getValue();
                    break;
                }
            }
        }
        String docId = request.getParameter("documentId");
        String viewingSessionId = request.getParameter("s");
        if (!StringUtils.hasText(docId) || !StringUtils.hasText(viewingSessionId)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        String documentId = URLDecoder.decode(docId, "UTF-8");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting metadata for documentId: {}", documentId);
        }
        RMSViewerContentManager contentMgr = RMSViewerContentManager.getInstance();
        DocumentMetaData metaData = contentMgr.getMetaData(documentId, viewingSessionId, userId);
        JsonUtil.writeJsonToResponse(metaData, response);
    }
}
