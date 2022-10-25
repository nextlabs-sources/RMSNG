package com.nextlabs.rms.serviceprovider;

import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.application.onedrive.authentication.ApplicationOneDriveOAuthHandler;
import com.nextlabs.rms.application.onedrive.authentication.ApplicationOneDriveOAuthHandler.OneDriveApplicationInfo;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.ServiceProviderRepoManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.OneDriveTokenResponse;
import com.nextlabs.rms.serviceprovider.SupportedProvider.ProviderClass;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

public class ApplicationServiceProviderCreator implements IServiceProviderCreator {

    @Override
    public String createServiceProvider(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal)
            throws RepositoryAlreadyExists, DuplicateRepositoryNameException, BadRequestException, RepositoryException {
        String serviceProviderId = null;
        Repository repo = createRepositoryForApplicationServiceProvider(serviceProviderSetting, principal);

        try (DbSession session = DbSession.newSession()) {
            List<ServiceProviderSetting> existingServiceProviderSettings = ServiceProviderRepoManager.getStorageProvider(session, serviceProviderSetting.getTenantId(), serviceProviderSetting.getProviderType().ordinal());
            if (existingServiceProviderSettings != null && !existingServiceProviderSettings.isEmpty()) {
                if (serviceProviderSetting.getProviderType() != ServiceProviderType.SHAREPOINT_ONLINE) {
                    throw new ValidateException(4004, "Service provider settings already exist");
                }
                String displayName = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME);
                if (existingServiceProviderSettings.parallelStream().anyMatch(s -> s.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME).equals(displayName))) {
                    throw new ValidateException(4004, String.format("Service provider settings with display name '%s' already exist", displayName));
                }
            }

            StorageProvider sp = new StorageProvider();
            sp.setTenantId(serviceProviderSetting.getTenantId());
            sp.setType(serviceProviderSetting.getProviderType().ordinal());
            sp.setName(ServiceProviderSetting.getProviderTypeDisplayName(serviceProviderSetting.getProviderType().toString()));
            sp.setAttributes(GsonUtils.GSON.toJson(serviceProviderSetting.getAttributes()));
            sp.setCreationTime(new Date());

            serviceProviderId = ServiceProviderRepoManager.addServiceProvider(session, sp);

            repo.setProviderId(serviceProviderId);
            // There could be multiple sharepoint online repositories and (user_id, account_id, account_name) combination needs to be unique
            // AccountId is set to null for all application repos in createRepositoryForApplicationServiceProvider, this causes error in MSSQL and OracleDB
            repo.setAccountId(serviceProviderId);
            RepositoryManager.addRepository(session, principal, repo);
        }
        return serviceProviderId;
    }

    public boolean validateRepoName(String repoName) {
        Matcher matcher = RegularExpressions.REPO_NAME_PATTERN.matcher(repoName);
        return matcher.matches();
    }

    private Repository createRepositoryForApplicationServiceProvider(ServiceProviderSetting serviceProviderSetting,
        RMSUserPrincipal principal) throws RepositoryException {
        Repository repo = new Repository();
        JsonRepository jsonRepo = null;
        jsonRepo = getRepoParameters(serviceProviderSetting, principal);

        if (!validateRepoName(jsonRepo.getName())) {
            throw new ValidateException(4003, "Repository Name contains illegal special characters");
        }

        repo.setAccountId(jsonRepo.getAccountId());
        repo.setName(jsonRepo.getName());
        repo.setUserId(principal.getUserId());
        repo.setShared(jsonRepo.isShared() ? 1 : 0);
        repo.setAccountName(jsonRepo.getAccountName());
        repo.setProviderClass(ProviderClass.APPLICATION.ordinal());
        if (principal.getDeviceType().isIOS()) {
            repo.setIosToken(jsonRepo.getToken());
        } else if (principal.getDeviceType().isAndroid()) {
            repo.setAndroidToken(jsonRepo.getToken());
        } else {
            repo.setToken(jsonRepo.getToken());
        }
        if (jsonRepo.getCreationTime() != null) {
            repo.setCreationTime(new Date(jsonRepo.getCreationTime()));
        } else {
            repo.setCreationTime(new Date());
        }
        return repo;
    }

    public JsonRepository getRepoParameters(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal)
            throws RepositoryException {
        JsonRepository repository = new JsonRepository();

        String clientId = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String clientSecret = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        String tenantId = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_TENANT_ID);

        OneDriveApplicationInfo info = new OneDriveApplicationInfo(tenantId, clientId, clientSecret);

        try {
            OneDriveTokenResponse result = ApplicationOneDriveOAuthHandler.getAccessToken(info);
            if (result != null) {
                repository.setToken(result.getAccessToken());
            } else {
                throw new ValidateException(4004, "Invalid App setup data");
            }
        } catch (RepositoryException | ApplicationRepositoryException ex) {
            ValidateException ve = new ValidateException(4005, "Unable to get token for configuration data provided");
            ve.initCause(ex);
            throw ve;
        }

        repository.setName(serviceProviderSetting.getAttributes().get("DISPLAY_NAME"));
        repository.setAccountName(principal.getEmail());
        repository.setAccountId(null);
        return repository;
    }
}
