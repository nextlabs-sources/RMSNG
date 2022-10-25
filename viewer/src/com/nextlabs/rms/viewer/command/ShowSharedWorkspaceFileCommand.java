package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.viewer.exception.NotAuthorizedException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.json.RepoFile;
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

public class ShowSharedWorkspaceFileCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String filePathDisplay = request.getParameter("filePathDisplay");
        int offset = Integer.parseInt(request.getParameter("offset"));
        String repoName = request.getParameter("repoName");
        String repoType = request.getParameter("repoType");
        String lastModifiedDateStr = request.getParameter("lastModifiedDate");
        String op = request.getParameter("operations");
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
        int operations = -1;
        if (StringUtils.hasText(op)) {
            operations = Integer.parseInt(op);
        }
        user.setEmail(request.getParameter("userName"));
        user.setTenantName(request.getParameter("tenantName"));
        user.setIpAddress(request.getRemoteAddr());
        String uid = user.getId();

        Audit.audit(request, "Command", "ShowFile", "ShowFile", 0, uid, user.getEmail(), user.getTenantName(), repoId, repoName, repoType, filePath, filePathDisplay);
        try {
            RepoFile repoFileDetails = new RepoFile(repoId, repoType, repoName, filePath, filePathDisplay);
            ShowFileResult showFileResult = ViewFileManager.getInstance().showSharedWorkspaceFile(repoFileDetails, user, offset, lastModifiedDateStr, viewingSessionId, request, response);
            ShowFileResponse result = new ShowFileResponse(HTTPUtil.getURI(request));
            String redirectURL = showFileResult.getViewerURL();
            if (StringUtils.hasText(redirectURL) && operations >= 0) {
                String separator = redirectURL.contains("?") ? "&" : "?";
                redirectURL = new StringBuilder(redirectURL).append(separator).append("operations=").append(operations).toString();
            }
            result.setViewerUrl(redirectURL);
            result.setRights(showFileResult.getRights());
            result.setOwner(showFileResult.isOwner());
            String duid = showFileResult.getDuid();
            String membership = showFileResult.getMembership();
            if (StringUtils.hasText(duid)) {
                result.setDuid(duid);
            }
            if (StringUtils.hasText(membership)) {
                result.setMembership(membership);
            }
            JsonUtil.writeJsonToResponse(result, response);
        } catch (GeneralSecurityException | NotAuthorizedException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error when user opened file (user ID: {}): {}", uid, e.getMessage());
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unauthorized", 403);
        } catch (NxlException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error when user opened invalid nxl file (user ID: {}, repository: {}, file path: {}): {}", uid, repoId, filePath, e.getMessage());
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unsupported.nxl", 5007);
        } catch (RMSException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred when user opened file (user ID: {}, repository: {}, file path: {}): {}", uid, repoId, filePath, e.getMessage());
            }
            String errId = ViewerUtil.setError(viewingSessionId, e.getMessage());
            ViewerUtil.sendErrorResponseWithStatusCode(errId, request, response, viewingSessionId, 404);
        } catch (UnsupportedFormatException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User opened unsupported file (user ID: {}): {}", uid, filePath);
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.unsupported", 415);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IO Error occurred when user opened file (user ID: {}, repository: {}, file path: {}): {}", uid, repoId, filePath, e.getMessage(), e);
            }
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.generic", 500);
        } catch (Exception e) {
            LOGGER.error("Error occurred when user opened file (user ID: {}, repository: {}, file path: {}): {}", uid, repoId, filePath, e.getMessage(), e);
            ViewerUtil.sendErrorResponseWithStatusCode(request, response, viewingSessionId, "err.generic", 500);
        }
    }
}
