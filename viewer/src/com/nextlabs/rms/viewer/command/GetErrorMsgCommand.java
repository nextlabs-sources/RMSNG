package com.nextlabs.rms.viewer.command;

import com.google.gson.JsonObject;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.viewer.conversion.RMSViewerContentManager;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetErrorMsgCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException {
        String errMsg = null;
        String errId = request.getParameter("errorId");

        String viewingSessionId = request.getParameter("s");

        RMSViewerContentManager contentMgr = RMSViewerContentManager.getInstance();
        try {
            errMsg = contentMgr.getErrorMsg(errId, viewingSessionId);
        } catch (Exception e) {
            LOGGER.error("Error occurred while getting error message with error ID:" + errId);
            errMsg = ViewerMessageHandler.getClientString("err.generic.viewer");
        }

        JsonObject result = new JsonObject();
        result.addProperty("result", true);
        result.addProperty("message", errMsg);
        JsonUtil.writeJsonToResponse(result, response);
    }
}
