package com.nextlabs.rms.repository.defaultrepo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DriveStorageExceededException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.HibernateUtils;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.LogConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

public final class DefaultRepositoryManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final DefaultRepositoryManager INSTANCE = new DefaultRepositoryManager();
    public static final String IN_BUILT_REPO_NAME = "MyDrive";

    private DefaultRepositoryManager() {
    }

    public static DefaultRepositoryManager getInstance() {
        return INSTANCE;
    }

    public DefaultRepositoryTemplate getDefaultPersonalRepository(RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        try (DbSession session = DbSession.newSession()) {
            return getDefaultPersonalRepository(session, userPrincipal);
        }
    }

    public DefaultRepositoryTemplate getDefaultPersonalRepository(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        return new DefaultRepositoryPersonal(session, userPrincipal);
    }

    public int getNumberDefaultRepositories(DbSession session, String tenantId)
            throws InvalidDefaultRepositoryException {
        ServiceProviderType defaultRepositoryType = getDefaultServiceProvider();
        Criteria criteria = session.createCriteria(Repository.class);
        DetachedCriteria subCriteria = DetachedCriteria.forClass(StorageProvider.class);
        subCriteria.add(Property.forName("type").eq(defaultRepositoryType.ordinal()));
        subCriteria.add(Property.forName("tenantId").eq(tenantId));
        subCriteria.setProjection(Projections.property("id"));
        criteria.add(Property.forName("providerId").in(subCriteria));
        criteria.setProjection(Projections.rowCount());

        Long count = (Long)criteria.uniqueResult();

        return (count == null) ? 0 : count.intValue();
    }

    public List<DefaultRepositoryTemplate> getDefaultRepositories(DbSession session, String tenantId, int offset,
        int limit)
            throws InvalidDefaultRepositoryException, RepositoryException {

        List<DefaultRepositoryTemplate> repos = new ArrayList<>();
        ServiceProviderType defaultRepositoryType = getDefaultServiceProvider();
        Criteria criteria = session.createCriteria(Repository.class);
        DetachedCriteria subCriteria = DetachedCriteria.forClass(StorageProvider.class);
        subCriteria.add(Property.forName("type").eq(defaultRepositoryType.ordinal()));
        subCriteria.add(Property.forName("tenantId").eq(tenantId));
        subCriteria.setProjection(Projections.property("id"));
        criteria.add(Property.forName("providerId").in(subCriteria));
        HibernateUtils.setLimit(criteria, offset, limit);

        @SuppressWarnings("unchecked")
        List<Repository> repoList = criteria.list();
        if (repoList != null && !repoList.isEmpty()) {
            for (Repository repo : repoList) {
                RMSUserPrincipal user = new RMSUserPrincipal(repo.getUserId(), tenantId, null, null);
                repos.add((DefaultRepositoryTemplate)RepositoryFactory.getInstance().getRepository(session, user, repo));
            }
        }
        return repos;
    }

    public void createDefaultRepository(DbSession session, int userId, String tenantId) {
        try {
            User user = session.load(User.class, userId);
            Tenant tenant = session.load(Tenant.class, tenantId);
            Repository repository = getDefaultRepository(session, user, tenant);
            if (repository == null) {
                RMSUserPrincipal principal = new RMSUserPrincipal(userId, tenantId, null, null);
                createDefaultRepository(principal);
            }
            addEWSRootPathIfAbsent(tenantId, userId);
        } catch (InvalidDefaultRepositoryException | RMSRepositorySearchException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void addEWSRootPathIfAbsent(String tenantId, int userId) throws RMSRepositorySearchException {
        IRMSRepositorySearcher searcher = new RMSEnterpriseDBSearcherImpl();
        if (!searcher.pathExists(tenantId, "/")) {
            StoreItem data = new StoreItem();
            data.setDirectory(true);
            data.setRepoId(tenantId);
            data.setFilePath("/");
            data.setFilePathDisplay("/");
            data.setSize(0);
            data.setCreationTime(new Date());
            data.setLastModified(new Date());
            data.setTenantId(tenantId);
            data.setUserId(userId);
            data.setLastModifiedUserId(userId);
            data.setFilePathSearchSpace("/");
            searcher.addRepoItem(data);
        }
    }

    private void createDefaultRepository(RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        ServiceProviderType type = getDefaultServiceProvider();
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(StorageProvider.class);
            criteria.add(Restrictions.eq("tenantId", userPrincipal.getTenantId()));
            criteria.add(Restrictions.eq("type", type.ordinal()));
            StorageProvider sp = (StorageProvider)criteria.uniqueResult();
            if (sp == null) {
                sp = new StorageProvider();
                sp.setTenantId(userPrincipal.getTenantId());
                sp.setType(type.ordinal());
                sp.setCreationTime(new Date());
                sp.setName(ServiceProviderSetting.getProviderTypeDisplayName(type.toString()));
            }
            sp.setAttributes(WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER_ATTRIBUTES));
            session.save(sp);
            session.commit();
            @SuppressWarnings("unchecked")
            Map<String, String> attrs = GsonUtils.GSON.fromJson(sp.getAttributes(), Map.class);
            Repository repo = new Repository();
            repo.setName(IN_BUILT_REPO_NAME);
            repo.setProviderId(sp.getId());
            repo.setUserId(userPrincipal.getUserId());
            repo.setShared(0);
            if (type == ServiceProviderType.SHAREPOINT_ONPREMISE || type == ServiceProviderType.ONEDRIVE_FORBUSINESS) {
                repo.setAccountName(attrs.get(ServiceProviderSetting.SP_ONPREMISE_SITE));
                repo.setAccountId(attrs.get(ServiceProviderSetting.SP_ONPREMISE_SITE));
            } else {
                repo.setAccountId("");
            }
            repo.setToken("");
            repo.setCreationTime(new Date());
            try {
                Repository repository = RepositoryManager.addRepository(session, userPrincipal, repo);
                addRootPath(repository.getId());
            } catch (RepositoryAlreadyExists | DuplicateRepositoryNameException | BadRequestException
                    | RMSRepositorySearchException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void updateStorageProvider(String tenantId) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ServiceProviderType type = getDefaultServiceProvider();
            Criteria criteria = session.createCriteria(StorageProvider.class);
            criteria.add(Restrictions.eq("tenantId", tenantId));
            criteria.add(Restrictions.eq("type", type.ordinal()));
            criteria.setMaxResults(1);
            StorageProvider sp = (StorageProvider)criteria.uniqueResult();
            if (sp != null) {
                sp.setAttributes(WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER_ATTRIBUTES));
                session.saveOrUpdate(sp);
                session.commit();
                List<Tenant> tenants = (List<Tenant>)session.createCriteria(Tenant.class).list();
                if (tenants != null && !tenants.isEmpty()) {
                    for (Tenant tenant : tenants) {
                        addEWSRootPathIfAbsent(tenant.getId(), 0); // 0 should be SYSTEM user
                    }
                }
            }
        } catch (InvalidDefaultRepositoryException | RMSRepositorySearchException e) {
            LOGGER.error("Unable to update storage provider on context initialization ");
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void checkVaultStorageExceeded(RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException, RepositoryException, VaultStorageExceededException {
        Map<String, Long> myDriveStatus = getMyDriveStatus(userPrincipal);
        if (myDriveStatus.get(RepoConstants.STORAGE_USED) >= myDriveStatus.get(RepoConstants.USER_VAULT_QUOTA)) {
            throw new VaultStorageExceededException(myDriveStatus.get(RepoConstants.STORAGE_USED));
        }
    }

    public static void checkStorageExceeded(RMSUserPrincipal userPrincipal) throws DriveStorageExceededException,
            InvalidDefaultRepositoryException, RepositoryException {
        Map<String, Long> myDriveStatus = getMyDriveStatus(userPrincipal);
        if (myDriveStatus.get(RepoConstants.STORAGE_USED) >= myDriveStatus.get(RepoConstants.USER_QUOTA)) {
            throw new DriveStorageExceededException(myDriveStatus.get(RepoConstants.STORAGE_USED));
        }
    }

    public static Map<String, Long> getMyDriveStatus(RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException, RepositoryException {
        String myDrivePrefs = null;
        try (DbSession session = DbSession.newSession()) {
            User user = session.load(User.class, userPrincipal.getUserId());
            Tenant tenant = session.load(Tenant.class, userPrincipal.getTenantId());
            Repository repo = getDefaultRepository(session, user, tenant);
            myDrivePrefs = repo == null ? null : repo.getPreference();
        } catch (HibernateException e) {
            throw new RepositoryException("Error occured when getting storage size for MyDrive of User: " + userPrincipal.getUserId(), e);
        }
        Long usage = 0L;
        Long myVaultUsage = 0L;
        JsonObject myDriveAttrs = null;
        if (StringUtils.hasText(myDrivePrefs)) {
            myDriveAttrs = GsonUtils.GSON.fromJson(myDrivePrefs, JsonObject.class);
            if (myDriveAttrs.has(RepoConstants.DB_STORAGE_USED)) {
                JsonElement elem = myDriveAttrs.get(RepoConstants.DB_STORAGE_USED);
                usage = elem.getAsLong();
            }
            if (myDriveAttrs.has(RepoConstants.DB_MYVAULT_STORAGE_USED)) {
                JsonElement ele = myDriveAttrs.get(RepoConstants.DB_MYVAULT_STORAGE_USED);
                myVaultUsage = ele.getAsLong();
            }
        }
        Long userQuota = RepoConstants.USER_MYSPACE_QUOTA;
        Long vaultQuota = RepoConstants.USER_MYSPACE_GRACE_STORAGE;
        if (StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.USER_MYSPACE_QUOTA))) {
            userQuota = Long.parseLong(WebConfig.getInstance().getProperty(WebConfig.USER_MYSPACE_QUOTA));
        }
        if (StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.USER_MYSPACE_GRACE_STORAGE))) {
            vaultQuota = Long.parseLong(WebConfig.getInstance().getProperty(WebConfig.USER_MYSPACE_GRACE_STORAGE));
        }

        Map<String, Long> myDriveStatus = new HashMap<>(4);
        myDriveStatus.put(RepoConstants.MY_VAULT_STORAGE_USED, myVaultUsage);
        myDriveStatus.put(RepoConstants.STORAGE_USED, usage);
        myDriveStatus.put(RepoConstants.USER_QUOTA, userQuota);
        myDriveStatus.put(RepoConstants.USER_VAULT_QUOTA, vaultQuota);
        return myDriveStatus;
    }

    public static ServiceProviderType getDefaultServiceProvider() throws InvalidDefaultRepositoryException {
        String inbuiltSPType = WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER);
        try {
            return ServiceProviderType.valueOf(inbuiltSPType);
        } catch (IllegalArgumentException e) {
            throw new InvalidDefaultRepositoryException("Inbuilt service provider type: " + inbuiltSPType, e);
        }
    }

    public static boolean isDefaultRepo(IRepository repo) {
        return isDefaultServiceProvider(repo.getRepoType()) && repo.getRepoName().equals(IN_BUILT_REPO_NAME);
    }

    public static boolean isDefaultServiceProvider(ServiceProviderType repoType) {
        try {
            return repoType == getDefaultServiceProvider();
        } catch (InvalidDefaultRepositoryException e) {
            return false;
        }
    }

    public static StorageProvider getDefaultStorageProvider(DbSession session, String tenantId)
            throws InvalidDefaultRepositoryException {
        ServiceProviderType defaultServiceProviderType = getDefaultServiceProvider();
        Criteria criteria = session.createCriteria(StorageProvider.class);
        criteria.add(Property.forName("type").eq(defaultServiceProviderType.ordinal()));
        criteria.add(Property.forName("tenantId").eq(tenantId));
        @SuppressWarnings("unchecked")
        List<StorageProvider> storageProviders = criteria.list();
        if (storageProviders.isEmpty()) {
            return null;
        }
        return storageProviders.get(0);
    }

    @SuppressWarnings("unchecked")
    public static Repository getDefaultRepository(DbSession session, User user, Tenant tenant)
            throws InvalidDefaultRepositoryException {
        StorageProvider storageProvider = getDefaultStorageProvider(session, tenant.getId());
        if (storageProvider == null) {
            return null;
        }
        Criteria criteria = session.createCriteria(Repository.class);
        criteria.add(Property.forName("userId").eq(user.getId()));
        criteria.add(Property.forName("providerId").eq(storageProvider.getId()));
        List<Repository> repositories = criteria.list();
        if (!repositories.isEmpty()) {
            return repositories.get(0);
        }
        return null;
    }

    private static void addRootPath(String repoId) throws RMSRepositorySearchException {
        StoreItem data = new StoreItem();
        data.setDirectory(true);
        data.setRepoId(repoId);
        data.setFilePath("/");
        data.setFilePathDisplay("/");
        data.setFilePathSearchSpace("/");
        data.setSize(0);
        data.setLastModified(new Date());
        data.setFileParentPath("");
        RMSRepositoryDBSearcherImpl searcher = new RMSRepositoryDBSearcherImpl();
        searcher.addRepoItem(data);
    }

}
