package com.nextlabs.rms.viewer.command;

import com.nextlabs.nxl.NxlException;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.viewer.exception.NotAuthorizedException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.json.SharedFile;
import com.nextlabs.rms.viewer.json.ShowFileResponse;
import com.nextlabs.rms.viewer.manager.ViewFileManager;
import com.nextlabs.rms.viewer.manager.ViewFileManager.ShowFileResult;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.Audit;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShowSharedFileCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);
    private static final String PARAM_TRANSACTION_ID = "d";
    private static final String PARAM_HMAC_ID = "c";

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        int offset = Integer.parseInt(request.getParameter("offset"));
        String transactionId = request.getParameter(PARAM_TRANSACTION_ID);
        String hmacId = request.getParameter(PARAM_HMAC_ID);
        String spaceId = request.getParameter("spaceId");
        String fromSpace = request.getParameter("fromSpace");
        request.getSession(true);
        String viewingSessionId = UUID.randomUUID().toString();

        User user = ViewerUtil.extractUserFromRequest(request);
        if (user == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Cannot retrieve userId and ticket from cookies.");
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized.view", 401);
            return;
        }
        user.setEmail(request.getParameter("userName"));
        user.setTenantName(request.getParameter("tenantName"));
        user.setIpAddress(request.getRemoteAddr());
        String userId = user.getId();

        Audit.audit(request, "Command", "ShowSharedFile", "ShowSharedFile", 0, userId, user.getEmail(), user.getTenantName(), transactionId, hmacId);
        try {
            SharedFile sharedFile = new SharedFile(transactionId, hmacId);
            ShowFileResult showFileResult = ViewFileManager.getInstance().showSharedFile(sharedFile, user, offset, viewingSessionId, request, response, spaceId, fromSpace);
            ShowFileResponse result = new ShowFileResponse(HTTPUtil.getURI(request));
            result.setViewerUrl(showFileResult.getViewerURL());
            result.setRights(showFileResult.getRights());
            result.setOwner(showFileResult.isOwner());
            JsonUtil.writeJsonToResponse(result, response);
        } catch (NxlException | GeneralSecurityException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error when user opened shared file (user ID: {}, txn ID: {}): {}", userId, transactionId, e.getMessage());
            }
            ViewerUtil.sendErrorResponse(request, response, viewingSessionId, "err.unauthorized");
        } catch (RMSException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred when user opened shared file (user ID: {}, txn ID: {}): {}", userId, transactionId, e.getMessage());
            }
            String errId = ViewerUtil.setError(viewingSessionId, e.getMessage());
            ViewerUtil.sendErrorResponse(errId, request, response, viewingSessionId);
        } catch (NotAuthorizedException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred as authorization fails for shared file (user ID: {}, txn ID: {}): {}", userId, transactionId, e.getMessage());
            }
            ViewerUtil.sendErrorResponse(request, response, viewingSessionId, "err.unauthorized.view");
        } catch (UnsupportedFormatException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User opened unsupported shared file (user ID: {}, txn ID: {})", userId, transactionId);
            }
            ViewerUtil.sendErrorResponse(request, response, viewingSessionId, "err.unsupported");
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IO Error occurred when user opened shared file (user ID: {}, txn ID: {}): {}", userId, transactionId, e.getMessage(), e);
            }
            ViewerUtil.sendErrorResponse(request, response, viewingSessionId, "err.generic");
        } catch (Exception e) {
            LOGGER.error("Error occurred when user opened shared file (user ID: {}, txn ID: {}): {}", userId, transactionId, e.getMessage(), e);
            ViewerUtil.sendErrorResponse(request, response, viewingSessionId, "err.generic");
        }
    }
}
