package com.nextlabs.rms.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.json.FileListResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetFilesWithPathCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String pathId = request.getParameter("path");
        String repoId = request.getParameter("repoId");
        if (!StringUtils.hasText(pathId) || !StringUtils.hasText(repoId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        FilterOptions filters = new FilterOptions(request.getParameter("filter"));
        String path = URLDecoder.decode(pathId, "UTF-8");

        FileListResult result = new FileListResult();
        Repository repo = null;
        IRepository repository = null;
        StorageProvider sp = null;
        try {

            try (DbSession session = DbSession.newSession()) {
                RMSUserPrincipal userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                repo = session.get(Repository.class, repoId);
                if (repo == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to find repository (ID: {}) for user ID: {}", repoId, userPrincipal.getUserId());
                    }
                    result.setResult(false);
                    result.setMessages(Collections.singletonList(RMSMessageHandler.getClientString("repoDeleted")));
                    JsonUtil.writeJsonToResponse(result, response);
                    return;
                } else if (repo.getShared() == 0 && repo.getUserId() != userPrincipal.getUserId()) {
                    result.setResult(false);
                    result.setMessages(Collections.singletonList(RMSMessageHandler.getClientString("unauthorizedRepositoryAccess")));
                    JsonUtil.writeJsonToResponse(result, response);
                    return;
                }
                sp = session.get(StorageProvider.class, repo.getProviderId());
                repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repo);
            }
            result.setRepoId(repository.getRepoId());
            result.setRepoName(repository.getRepoName());
            result.setRepoType(repository.getRepoType());
            List<RepositoryContent> fileList = repository.getFileList(path, filters);

            if (fileList.isEmpty()) {
                result.setResult(true);
                result.setContent(Collections.emptyList());
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            if (StringUtils.equals(path, "/")) {
                addElementsToRoot(response, repository, fileList);
            } else {
                result.setResult(true);
                result.setContent(fileList);
                JsonUtil.writeJsonToResponse(result, response);
            }
        } catch (InvalidTokenException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invalid token for given repository (ID: {}): {}", repoId, e.getMessage());
            }
            String error = RMSMessageHandler.getClientString("invalidRepositoryToken");
            String redirectUrl = null;
            if (repository != null) {
                ServiceProviderType providerType = ServiceProviderType.getByOrdinal(sp.getType());
                redirectUrl = RepositoryManager.getAuthorizationUrl(request, providerType, repo, false);
                RepositoryManager.setCookieRedirectParameters(response, repository.getAccountId());
            }
            result.setResult(false);
            result.setMessages(Collections.singletonList(error));
            result.setRedirectUrl(redirectUrl);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (UnauthorizedRepositoryException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Unauthorized access for given repository (ID: {}): {}", repoId, e.getMessage());
            }
            String error = RMSMessageHandler.getClientString("unauthorizedRepositoryAccess");
            String redirectUrl = null;
            if (repository != null) {
                ServiceProviderType providerType = ServiceProviderType.getByOrdinal(sp.getType());
                if (ServiceProviderType.SHAREPOINT_ONPREMISE.equals(repository.getRepoType())) {
                    redirectUrl = repository.getAccountName();
                } else {
                    redirectUrl = RepositoryManager.getAuthorizationUrl(request, providerType, repo, false);
                }
                RepositoryManager.setCookieRedirectParameters(response, repository.getAccountId());
            }
            result.setResult(false);
            result.setMessages(Collections.singletonList(error));
            result.setRedirectUrl(redirectUrl);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (RepositoryFolderAccessException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
            String error = RMSMessageHandler.getClientString("spMaxUrlLengthExceeded");
            result.setResult(false);
            result.setMessages(Collections.singletonList(error));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (Exception e) {
            logger.error("Error occurred when accessing repository (ID: {}): {}", repoId, e.getMessage(), e);
            String error = RMSMessageHandler.getClientString("inaccessibleRepository");
            result.setResult(false);
            result.setMessages(Collections.singletonList(error));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
        }
    }

    private void addElementsToRoot(HttpServletResponse response, IRepository repository,
        List<RepositoryContent> fileList) {
        RepositoryContent content = new RepositoryContent();
        content.setChildren(fileList);
        content.setName("Root");
        FileListResult result = new FileListResult();
        result.setResult(true);
        result.setContent(content);
        result.setRepoId(repository.getRepoId());
        result.setRepoName(repository.getRepoName());
        result.setRepoType(repository.getRepoType());
        JsonUtil.writeJsonToResponse(result, response);
    }
}
