package com.nextlabs.rms.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.json.FileListResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetSearchResultsCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String searchString = request.getParameter("searchString");
        String repoId = request.getParameter("repoId");
        // Apostrophe should be specially handled. Currently OneDrive/SharePoint cannot accept '. For GoogleDrive, you have to append a backslash before '.
        // Currently, RMS will also ignore Apostrophe. Please remove this from regex if this has to be enabled.
        String processedSearchString = StringUtils.hasText(searchString) ? searchString.replace("^\\.+", "").replaceAll("[\\\\/:*?\"\'<>|]", "") : "";
        boolean doSearch = StringUtils.hasText(processedSearchString);
        List<IRepository> repoList = new ArrayList<>();
        List<RepositoryContent> searchRes = new ArrayList<RepositoryContent>();
        List<String> errorList = new ArrayList<String>();
        FileListResult list = new FileListResult();
        boolean result = true;
        try (DbSession session = DbSession.newSession()) {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (doSearch) {
                if (StringUtils.hasText(repoId)) {
                    Repository repo = session.get(Repository.class, repoId);
                    if (repo != null) {
                        if (repo.getUserId() != userPrincipal.getUserId() && repo.getShared() == 0) {
                            errorList.add(RMSMessageHandler.getClientString("unauthorizedRepositoryAccess"));
                            list.setMessages(errorList);
                            list.setContent(searchRes);
                            list.setResult(false);
                            JsonUtil.writeJsonToResponse(list, response);
                            return;
                        }
                        try {
                            IRepository repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repo);
                            repoList.add(repository);
                        } catch (RepositoryException e) {
                            logger.error("Error occurred during searching in repository (ID: {}): {}", repo.getId(), e.getMessage(), e);
                        }
                    }
                } else {
                    repoList = RepositoryManager.getRepositoryList(session, userPrincipal, false);
                }

                if (repoList.isEmpty()) {
                    errorList.add(RMSMessageHandler.getClientString("repoDeleted"));
                }

            }
        }
        if (doSearch && !repoList.isEmpty()) {
            int errorCount = 0;
            StringBuilder builder = new StringBuilder();
            for (IRepository repo : repoList) {
                try {
                    List<RepositoryContent> res = repo.search(processedSearchString);
                    if (res != null && !res.isEmpty()) {
                        searchRes.addAll(res);
                    }
                } catch (RepositoryException e) {
                    if (errorCount > 0) {
                        builder.append(", ");
                    }
                    builder.append(repo.getRepoName());
                    errorCount++;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Repository error occurred during searching in repository (ID: {}): {}", repo.getRepoId(), e.getMessage(), e);
                    }
                } catch (Exception e) {
                    if (errorCount > 0) {
                        builder.append(", ");
                    }
                    builder.append(repo.getRepoName());
                    errorCount++;
                    logger.error("Error occurred during searching in repository (ID: {}): {}", repo.getRepoId(), e.getMessage(), e);
                }
            }
            String error = builder.toString();
            if (StringUtils.hasText(error)) {
                if (errorCount > 1) {
                    error = RMSMessageHandler.getClientString("repo_not_reachable_error_in_plural", error);
                } else {
                    error = RMSMessageHandler.getClientString("repo_not_reachable_error_in_singular", error);
                }
                errorList.add(error);
            }
        }
        if (!errorList.isEmpty()) {
            result = false;
            list.setMessages(errorList);
        }
        list.setContent(searchRes);
        list.setResult(result);
        JsonUtil.writeJsonToResponse(list, response);
    }
}
