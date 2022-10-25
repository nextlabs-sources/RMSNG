package com.nextlabs.rms.command;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
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

public class UpdateRepositoryCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        OperationResult result = new OperationResult();
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String repoId = request.getParameter("repoId");
            String repoName = request.getParameter("repoName");
            RepositoryManager.updateRepositoryName(session, userPrincipal, repoId, repoName);
            session.commit();
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("status_success_update_repository"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (DuplicateRepositoryNameException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_error_duplicate_repo_name"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (RepositoryNotFoundException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_error_repo_not_found"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (BadRequestException e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("invalidRepoName"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (UnauthorizedOperationException e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_failure_unauthorized_operation"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (ForbiddenOperationException e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("status_failure_forbidden_operation"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        }
    }
}
