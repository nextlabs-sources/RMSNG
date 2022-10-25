/**
 *
 */
package com.nextlabs.rms.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.exception.FolderCreationFailedException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.CreateFolderResponse;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author cfeng
 *
 */
public class CreateFolderCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        CreateFolderResponse createFolderResponse = new CreateFolderResponse();
        RMSUserPrincipal userPrincipal = null;
        IRepository repo = null;
        String repoId = request.getParameter("repoId");
        String filePathId = request.getParameter("filePathId");
        String filePathDisplay = request.getParameter("filePathDisplay");
        String folderName = request.getParameter("createFolderName");
        if (!StringUtils.hasText(repoId) || !StringUtils.hasText(filePathId) || !StringUtils.hasText(filePathDisplay) || !StringUtils.hasText(folderName)) {
            createFolderResponse.setError("Error Occured while creating the folder.");
            JsonUtil.writeJsonToResponse(createFolderResponse, response);
            return;
        }
        DbSession session = DbSession.newSession();
        try {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            repo = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
        } catch (RepositoryException e1) {
            if (logger.isDebugEnabled()) {
                logger.debug(e1.getMessage(), e1);
            }
            createFolderResponse.setError(e1.getMessage());
            JsonUtil.writeJsonToResponse(createFolderResponse, response);
            return;
        } finally {
            session.close();
        }
        // createFolderName format should be checked in the frontend already
        try {
            repo.createFolder(folderName, filePathId, filePathDisplay, true);
            createFolderResponse.setName(folderName);
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            if (e instanceof FolderCreationFailedException) {
                createFolderResponse.setError(RMSMessageHandler.getClientString("createFolderErr", folderName));
            } else {
                createFolderResponse.setError(RMSMessageHandler.getClientString("inaccessibleRepository"));
            }
        } catch (Exception e) {
            logger.error("Error occurred when creating folder '{}' (user ID:{}, repository ID: {}): {}", folderName, userPrincipal.getUserId(), repoId, e.getMessage(), e);
            createFolderResponse.setError(RMSMessageHandler.getClientString("createFolderErr", folderName));
        } finally {
            if (repo instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repo));
            }
            JsonUtil.writeJsonToResponse(createFolderResponse, response);
        }
    }

}
