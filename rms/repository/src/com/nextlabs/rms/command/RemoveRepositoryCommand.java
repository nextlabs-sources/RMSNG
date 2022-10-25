package com.nextlabs.rms.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoveRepositoryCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String repoId = request.getParameter("repoId");
        if (!StringUtils.hasText(repoId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        OperationResult result = new OperationResult();
        try (DbSession session = DbSession.newSession()) {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            RepositoryManager.removeRepository(session, userPrincipal, repoId);
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("success_delete_repo"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (ForbiddenOperationException e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_failure_forbidden_operation"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (UnauthorizedOperationException e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_failure_unauthorized_operation"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (RepositoryNotFoundException e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_error_repo_not_found"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (Exception e) {
            logger.error("Error occurred when deleting repository (ID: {}): {}", repoId, e.getMessage(), e);
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_failure_forbidden_operation"));
            JsonUtil.writeJsonToResponse(result, response);
        }
    }
}
