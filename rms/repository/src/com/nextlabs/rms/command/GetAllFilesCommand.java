package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.FileListResult;
import com.nextlabs.rms.json.Repository;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.FileListRetriever;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryAccessException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetAllFilesCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static ExecutorService execSvc = Executors.newCachedThreadPool();

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<IRepository> repoList = null;
        try {
            boolean result = true;
            try (DbSession session = DbSession.newSession()) {
                RMSUserPrincipal userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                boolean isOnlyPersonalRepo = false;
                repoList = RepositoryManager.getRepositoryList(session, userPrincipal, isOnlyPersonalRepo);
                if (repoList.isEmpty()) {
                    FileListResult out = new FileListResult();
                    List<String> errorMessages = new ArrayList<String>();
                    errorMessages.add(RMSMessageHandler.getClientString("repoNonePresent"));
                    out.setMessages(errorMessages);
                    out.setResult(result);
                    out.setContent(Collections.emptyList());
                    JsonUtil.writeJsonToResponse(out, response);
                    return;
                }
            }

            List<RepositoryContent> fileList = new ArrayList<RepositoryContent>();
            List<Callable<List<RepositoryContent>>> retrievers = new ArrayList<Callable<List<RepositoryContent>>>(repoList.size());
            for (IRepository repo : repoList) {
                retrievers.add(new FileListRetriever(repo));
            }
            List<Future<List<RepositoryContent>>> fileListResults = null;
            fileListResults = execSvc.invokeAll(retrievers);

            List<String> errorMessages = new ArrayList<String>();
            List<Repository> invalidTokenRepos = new ArrayList<Repository>();
            List<String> authRepoNames = new ArrayList<String>();
            List<String> accessRepoNames = new ArrayList<String>();
            for (Future<List<RepositoryContent>> fileListResult : fileListResults) {
                try {
                    fileList.addAll(fileListResult.get());
                } catch (ExecutionException e) {
                    result = false;
                    if (e.getCause() instanceof InvalidTokenException) {
                        InvalidTokenException ex = ((InvalidTokenException)e.getCause());
                        Repository repository = ex.getRepo();
                        invalidTokenRepos.add(repository);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cannot connect to repository due to invalid token: {}", ex.getRepoName());
                        }
                    } else if (e.getCause() instanceof UnauthorizedRepositoryException) {
                        String repoName = ((UnauthorizedRepositoryException)e.getCause()).getRepoName();
                        authRepoNames.add(repoName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cannot connect to repository due to Authorization error: {}", repoName);
                        }
                    } else if (e.getCause() instanceof RepositoryAccessException) {
                        String repoName = ((RepositoryAccessException)e.getCause()).getRepoName();
                        accessRepoNames.add(repoName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cannot connect to repository due to general error: {}", repoName);
                        }
                    }
                }
            }
            if (!authRepoNames.isEmpty()) {
                String authMessage = StringUtils.join(authRepoNames, ",");
                if (authRepoNames.size() == 1) {
                    errorMessages.add(RMSMessageHandler.getClientString("unauthorizedRepositoryAccessOne", authMessage));
                } else {
                    errorMessages.add(RMSMessageHandler.getClientString("unauthorizedRepositoryAccessMany", authMessage));
                }
            }
            if (!accessRepoNames.isEmpty()) {
                String accessMessage = StringUtils.join(accessRepoNames, ",");
                if (accessRepoNames.size() == 1) {
                    errorMessages.add(RMSMessageHandler.getClientString("inaccessibleRepositoryOne", accessMessage));
                } else {
                    errorMessages.add(RMSMessageHandler.getClientString("inaccessibleRepositoryMany", accessMessage));
                }
            }

            FileListResult out = new FileListResult();
            out.setMessages(errorMessages);
            out.setResult(result);
            out.setContent(fileList);
            out.setInvalidTokenRepos(invalidTokenRepos);
            JsonUtil.writeJsonToResponse(out, response);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt(); //"InterruptedException" should not be ignored (Sonar)
            //TODO return error to client
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
