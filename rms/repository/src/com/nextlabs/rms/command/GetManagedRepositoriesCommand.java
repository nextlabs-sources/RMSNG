package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.Repository;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetManagedRepositoriesCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<IRepository> repoList = null;
        try {
            RMSUserPrincipal userPrincipal = null;
            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                boolean isOnlyPersonalRepo = userPrincipal.isAdmin();
                repoList = RepositoryManager.getRepositoryList(session, userPrincipal, isOnlyPersonalRepo);
            }
            for (IRepository repo : repoList) {
                if (repo.getRepoType().name().equals(WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER))) {
                    repoList.remove(repo);
                    break;
                }
            }
            Repository[] repoArr = GetRepositoriesCommand.getRepoArr(repoList, userPrincipal);
            JsonUtil.writeJsonToResponse(repoArr, response);
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            logger.error("Error occured while getting repository list.", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } finally {
            if (repoList != null && !repoList.isEmpty()) {
                for (IRepository repository : repoList) {
                    if (repository instanceof Closeable) {
                        IOUtils.closeQuietly(Closeable.class.cast(repository));
                    }
                }
            }
        }
    }
}
