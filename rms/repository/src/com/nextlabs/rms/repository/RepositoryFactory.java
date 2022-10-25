package com.nextlabs.rms.repository;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.box.BoxRepository;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryPersonal;
import com.nextlabs.rms.repository.dropbox.DropboxRepository;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.googledrive.GoogleDriveRepository;
import com.nextlabs.rms.repository.local.LocalRepository;
import com.nextlabs.rms.repository.onedrive.OneDriveRepository;
import com.nextlabs.rms.repository.s3.S3Repository;
import com.nextlabs.rms.repository.sharepoint.online.SharePointOnlineRepository;
import com.nextlabs.rms.repository.sharepoint.onpremise.OD4BRepository;
import com.nextlabs.rms.repository.sharepoint.onpremise.SharePointOnPremiseRepository;
import com.nextlabs.rms.shared.LogConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RepositoryFactory {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final List<String> ALLOWED_REPOSITORIES = Collections.unmodifiableList(Arrays.asList(ServiceProviderType.DROPBOX.name(), ServiceProviderType.GOOGLE_DRIVE.name(), ServiceProviderType.ONE_DRIVE.name(), ServiceProviderType.SHAREPOINT_ONPREMISE.name(), ServiceProviderType.SHAREPOINT_ONLINE.name(), ServiceProviderType.S3.name(), ServiceProviderType.BOX.name()));

    private static final RepositoryFactory INSTANCE = new RepositoryFactory();

    private RepositoryFactory() {

    }

    public static RepositoryFactory getInstance() {
        return INSTANCE;
    }

    public IRepository createRepository(RMSUserPrincipal userPrincipal, Repository repoDTO, StorageProvider sp) {
        IRepository repo = null;
        ServiceProviderType repoType = ServiceProviderType.values()[sp.getType()];
        String repoId = repoDTO.getId();
        boolean isDefaultRepo = false;
        try (DbSession session = DbSession.newSession()) {
            Repository repository = session.get(Repository.class, repoId);
            isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(repoType) && repository.getName().equals(DefaultRepositoryManager.IN_BUILT_REPO_NAME);
        }
        if (isDefaultRepo && !StringUtils.hasText(sp.getAttributes())) {
            sp.setAttributes(WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER_ATTRIBUTES));
        }
        ServiceProviderSetting setting = SettingManager.toServiceProviderSetting(sp);
        if (repoType == ServiceProviderType.DROPBOX) {
            repo = new DropboxRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.GOOGLE_DRIVE) {
            repo = new GoogleDriveRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.ONE_DRIVE) {
            repo = new OneDriveRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.SHAREPOINT_ONLINE) {
            repo = new SharePointOnlineRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.SHAREPOINT_ONPREMISE) {
            repo = new SharePointOnPremiseRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.S3) {
            repo = new S3Repository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.BOX) {
            repo = new BoxRepository(userPrincipal, repoId, setting);
            if (StringUtils.hasText(repoDTO.getState())) {
                repo.getAttributes().put(RepositoryManager.BOX_REPOSIOTRY_STATE, repoDTO.getState());
            }
        } else if (repoType == ServiceProviderType.ONEDRIVE_FORBUSINESS) {
            repo = new OD4BRepository(userPrincipal, repoId, setting);
        } else if (repoType == ServiceProviderType.LOCAL_DRIVE) {
            repo = new LocalRepository(userPrincipal, repoId, setting);
        } else {
            LOGGER.error("Unsupported repository type: {}", repoType);
            return null;
        }
        String token = repoDTO.getToken();
        if (StringUtils.hasText(token)) {
            repo.getAttributes().put(RepositoryManager.REFRESH_TOKEN, token);
        }
        repo.setAccountId(repoDTO.getAccountId());
        repo.setAccountName(repoDTO.getAccountName());
        repo.setShared(repoDTO.getShared() == 1);
        repo.setRepoName(repoDTO.getName());
        return repo;
    }

    public IRepository getRepository(DbSession session, RMSUserPrincipal userPrincipal, String repoId)
            throws RepositoryException {
        Repository repository = RepositoryManager.getRepository(session, repoId);
        return getRepository(session, userPrincipal, repository);
    }

    public IRepository getRepository(DbSession session, RMSUserPrincipal userPrincipal, Repository repo)
            throws RepositoryException {
        if (repo == null || (repo.getUserId() != userPrincipal.getUserId() && repo.getShared() == 0)) {
            LOGGER.error("Invalid repository access (userId:{}, repoId: {})", userPrincipal.getUserId(), repo != null ? repo.getId() : "<none>");
            throw new RepositoryException("Invalid Repository");
        }
        StorageProvider sp = session.get(StorageProvider.class, repo.getProviderId());
        ServiceProviderType repoType = ServiceProviderType.values()[sp.getType()];
        boolean isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(repoType) && repo.getName().equals(DefaultRepositoryManager.IN_BUILT_REPO_NAME);

        IRepository repoInstance = INSTANCE.createRepository(userPrincipal, repo, sp);
        return isDefaultRepo ? new DefaultRepositoryPersonal(repoInstance) : repoInstance;
    }

    public RMSUserPrincipal getRepoOwner(DbSession session, RMSUserPrincipal userPrincipal, String repoId)
            throws RepositoryException {
        Repository repo = RepositoryManager.getRepository(session, repoId);
        if (repo == null) {
            LOGGER.error("Invalid repository access (userId:{}, repoId: {})", userPrincipal.getUserId(), repoId);
            throw new RepositoryException("Invalid Repository");
        }
        if (repo.getUserId() == userPrincipal.getUserId()) {
            return userPrincipal;
        } else {
            User user = session.get(User.class, repo.getUserId());
            RMSUserPrincipal repoUser = new RMSUserPrincipal();
            StorageProvider sp = session.get(StorageProvider.class, repo.getProviderId());
            repoUser.setUserId(user.getId());
            repoUser.setEmail(user.getEmail());
            repoUser.setName(user.getDisplayName());
            repoUser.setTenantId(sp.getTenantId());
            return repoUser;
        }
    }
}
