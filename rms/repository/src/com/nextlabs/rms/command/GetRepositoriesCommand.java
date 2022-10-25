package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.Repository;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetRepositoriesCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<IRepository> repoList = null;
        try (DbSession session = DbSession.newSession()) {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            repoList = RepositoryManager.getRepositoryList(session, userPrincipal, false);
            Repository[] repoArr = getRepoArr(repoList, userPrincipal);
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

    public static Repository[] getRepoArr(List<IRepository> repoList, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException, RepositoryException {
        Repository[] repoArr = new Repository[repoList.size()];
        int i = 0;
        for (IRepository repo : repoList) {
            repoArr[i] = new Repository();
            repoArr[i].setRepoId(repo.getRepoId());
            repoArr[i].setRepoName(repo.getRepoName());
            repoArr[i].setRepoType(repo.getRepoType().name());
            repoArr[i].setRepoTypeDisplayName(ServiceProviderSetting.getProviderTypeDisplayName(repo.getRepoType().name()));
            repoArr[i].setSid(String.valueOf(repo.getUser().getUserId()));
            repoArr[i].setAccountName(repo.getAccountName());
            repoArr[i].setShared(repo.isShared());
            if (DefaultRepositoryManager.isDefaultRepo(repo)) {
                Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(userPrincipal);
                repoArr[i].setQuota(myDriveStatus.get(RepoConstants.USER_QUOTA));
                repoArr[i].setUsage(myDriveStatus.get(RepoConstants.STORAGE_USED));
                repoArr[i].setMyVaultUsage(myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
                repoArr[i].setVaultQuota(myDriveStatus.get(RepoConstants.USER_VAULT_QUOTA));
            }
            i++;
        }
        return repoArr;
    }
}
