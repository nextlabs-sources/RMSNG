package com.nextlabs.rms.application;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.application.sharepoint.SharePointApplicationRepository;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.serviceprovider.SupportedProvider;
import com.nextlabs.rms.shared.LogConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ApplicationRepositoryFactory {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final List<String> ALLOWED_REPOSITORIES = Collections.unmodifiableList(Arrays.asList(ServiceProviderType.ONE_DRIVE_APPLICATION.name()));

    private static final ApplicationRepositoryFactory INSTANCE = new ApplicationRepositoryFactory();
    Map<String, IApplicationRepository> repositoryMap = new HashMap<>();

    private ApplicationRepositoryFactory() {
    }

    public static ApplicationRepositoryFactory getInstance() {
        return INSTANCE;
    }

    public IApplicationRepository createRepository(RMSUserPrincipal userPrincipal, Repository repoDTO,
        StorageProvider sp) throws ApplicationRepositoryException, RepositoryException {
        IApplicationRepository repo = null;
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
        if (repoType == ServiceProviderType.SHAREPOINT_ONLINE) {
            SharePointApplicationRepository sharePointApplicationRepository = new SharePointApplicationRepository(setting.getAttributes().get(ServiceProviderSetting.APP_TENANT_ID), setting.getAttributes().get(ServiceProviderSetting.APP_ID), setting.getAttributes().get(ServiceProviderSetting.APP_SECRET));
            sharePointApplicationRepository.configDrive(setting.getAttributes().get(ServiceProviderSetting.SITE_URL), setting.getAttributes().get(ServiceProviderSetting.DRIVE_NAME));
            repo = sharePointApplicationRepository;
        } else {
            LOGGER.error("Unsupported repository type: {}", repoType);
            return null;
        }
        return repo;
    }

    public SharePointApplicationRepository createSharepointOnlineRepository(ServiceProviderSetting setting) {
        SharePointApplicationRepository sharePointApplicationRepository = null;
        sharePointApplicationRepository = new SharePointApplicationRepository(setting.getAttributes().get(ServiceProviderSetting.APP_TENANT_ID), setting.getAttributes().get(ServiceProviderSetting.APP_ID), setting.getAttributes().get(ServiceProviderSetting.APP_SECRET));
        return sharePointApplicationRepository;
    }

    public IApplicationRepository getRepository(DbSession session, RMSUserPrincipal userPrincipal, String repoId)
            throws RepositoryException, ApplicationRepositoryException {
        Repository repository = RepositoryManager.getRepository(session, repoId);
        return getRepository(session, userPrincipal, repository);
    }

    public IApplicationRepository getRepository(DbSession session, RMSUserPrincipal userPrincipal, Repository repo)
            throws RepositoryException, ApplicationRepositoryException {
        if (repo == null || repo.getProviderClass() != SupportedProvider.ProviderClass.APPLICATION.ordinal()) {
            LOGGER.error("Invalid application repository access (userId:{}, repoId: {})", userPrincipal.getUserId(), repo != null ? repo.getId() : "<none>");
            throw new RepositoryException("Invalid Repository");
        }
        StorageProvider sp = session.get(StorageProvider.class, repo.getProviderId());
        if (repositoryMap.containsKey(repo.getId())) {
            return repositoryMap.get(repo.getId());
        } else {
            IApplicationRepository repository = INSTANCE.createRepository(userPrincipal, repo, sp);
            repositoryMap.put(repo.getId(), repository);
            return repository;
        }
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

    public void removeRepositoryInstance(String repoId) {
        this.repositoryMap.remove(repoId);
    }
}
