package com.nextlabs.rms.service;

import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.application.ApplicationRepositoryFactory;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.ServiceProviderRepoManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.serviceprovider.ApplicationServiceProviderCreator;
import com.nextlabs.rms.serviceprovider.IServiceProviderCreator;
import com.nextlabs.rms.serviceprovider.ServiceProviderCreationFactory;
import com.nextlabs.rms.serviceprovider.SupportedProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ServiceProviderService {

    private static final String DISPLAY_NAME = "_display_name";
    private static Map<String, SupportedProvider> supportedProvidersMap = new HashMap<>();
    private final ApplicationRepositoryFactory applicationRepositoryFactory = ApplicationRepositoryFactory.getInstance();

    static {
        supportedProvidersMap.put(ServiceProviderType.DROPBOX.name(), new SupportedProvider(RMSMessageHandler.getClientString(ServiceProviderType.DROPBOX.name() + DISPLAY_NAME), "DropBox", SupportedProvider.ProviderClass.PERSONAL));
        supportedProvidersMap.put(ServiceProviderType.GOOGLE_DRIVE.name(), new SupportedProvider(RMSMessageHandler.getClientString(ServiceProviderType.GOOGLE_DRIVE.name() + DISPLAY_NAME), "Google", SupportedProvider.ProviderClass.PERSONAL));
        supportedProvidersMap.put(ServiceProviderType.ONE_DRIVE.name(), new SupportedProvider(RMSMessageHandler.getClientString(ServiceProviderType.ONE_DRIVE.name() + DISPLAY_NAME), "OneDrive", SupportedProvider.ProviderClass.PERSONAL));
        supportedProvidersMap.put(ServiceProviderType.SHAREPOINT_ONLINE.name(), new SupportedProvider(RMSMessageHandler.getClientString(ServiceProviderType.SHAREPOINT_ONLINE.name() + DISPLAY_NAME), "SharepointOnline", SupportedProvider.ProviderClass.APPLICATION));
    }

    /**
     * Provides a map of all supported providers and their names
     * @return a map of all supported providers and their names.
     */
    public Map<String, String> getProviderTypeDisplayNames() {
        Map<String, String> typetoNameMap = new HashMap<>();
        for (String type : supportedProvidersMap.keySet()) {
            typetoNameMap.put(type, RMSMessageHandler.getClientString(type + DISPLAY_NAME));
        }
        return typetoNameMap;
    }

    /**
     * Provides a map of all supported providers and their details like name, class etc.
     * @return a map of all supported providers and their details like name, class etc.
     */
    public static Map<String, SupportedProvider> getSupportedProviderMap() {
        return supportedProvidersMap;
    }

    /**
     * Provides a list of configured service providers with the configuration details.
     * @param loginTenant for which the configurations need to be returned
     * @return a list of configured service providers with the configuration details.
     */
    public List<ServiceProviderSetting> getConfiguredServiceProviders(String loginTenant) {
        List<ServiceProviderSetting> configuredServiceProviderSettingList = new ArrayList<>();
        try (DbSession session = DbSession.newSession()) {
            for (ServiceProviderSetting serviceProviderSetting : ServiceProviderRepoManager.getStorageProviderSettings(session, loginTenant)) {
                if (!StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER)) && !StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), ServiceProviderType.ONEDRIVE_FORBUSINESS.name()) && !StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), ServiceProviderType.S3.name())) {
                    configuredServiceProviderSettingList.add(serviceProviderSetting);
                }
            }
        }
        return configuredServiceProviderSettingList;
    }

    /**
     * Adds a service provider configuration . 
     * 
     * @param serviceProviderSetting
     * @param principal
     * @return
     * @throws RepositoryAlreadyExists
     * @throws DuplicateRepositoryNameException
     * @throws BadRequestException
     * @throws RepositoryException
     */
    public String addServiceProvider(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal)
            throws RepositoryAlreadyExists, DuplicateRepositoryNameException, BadRequestException, RepositoryException {
        IServiceProviderCreator serviceProviderCreator = ServiceProviderCreationFactory.getCreator(serviceProviderSetting.getProviderType(), getSupportedProviderMap());
        return serviceProviderCreator.createServiceProvider(serviceProviderSetting, principal);
    }

    /**
     * Deletes a service provider configuration . 
     * @param serviceProviderId
     */
    public void deleteServiceProviderConfiguration(String serviceProviderId) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ServiceProviderSetting existingSetting = ServiceProviderRepoManager.getStorageProvider(session, serviceProviderId);
            if (getSupportedProviderMap().get(existingSetting.getProviderType().name()).getProviderClass().equals(SupportedProvider.ProviderClass.APPLICATION)) {
                List<String> repoIds = RepositoryManager.getRepoIdsWithTenantAndServiceProviderId(session, serviceProviderId);
                if (repoIds.size() != 1) {
                    throw new ValidateException(4004, "Couldn't find exactly one repository for the application service provider");
                }
                applicationRepositoryFactory.removeRepositoryInstance(repoIds.get(0));
            }
            ServiceProviderRepoManager.removeServiceProvider(session, serviceProviderId);
        }
    }

    /**
     * Provides a list of configured service providers
     * @param loginTenant for which the configurations need to be returned
     * @return a list of configured service providers 
     */
    public List<String> getConfiguredServiceProviderNames(String loginTenant) {
        List<String> configuredServiceProviderList = new ArrayList<>();
        try (DbSession session = DbSession.newSession()) {
            for (ServiceProviderSetting serviceProviderSetting : SettingManager.getStorageProviderSettings(session, loginTenant)) {
                if (shouldReturnServiceProvider(serviceProviderSetting.getProviderType().name()) && !configuredServiceProviderList.contains(serviceProviderSetting.getProviderType().name())) {
                    configuredServiceProviderList.add(serviceProviderSetting.getProviderType().name());
                }
            }
        }
        return configuredServiceProviderList;
    }

    private boolean shouldReturnServiceProvider(String providerType) {
        Stream<String> nonReturnableProviderTypes = Stream.of(WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER), ServiceProviderType.ONEDRIVE_FORBUSINESS.name(), ServiceProviderType.S3.name());
        return nonReturnableProviderTypes.noneMatch(p -> StringUtils.equalsIgnoreCase(providerType, p));
    }

    /**
     * Updates the service provider configuration after doing some basic validations 
     * 
     * @param serviceProviderSetting
     * @param principal
     * @return
     * @throws RepositoryAlreadyExists
     * @throws DuplicateRepositoryNameException
     * @throws BadRequestException
     * @throws RepositoryException
     * @throws RepositoryNotFoundException
     * @throws UnauthorizedOperationException
     * @throws ForbiddenOperationException
     */
    public String updateServiceProvider(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal)
            throws RepositoryAlreadyExists, DuplicateRepositoryNameException, BadRequestException, RepositoryException,
            RepositoryNotFoundException, UnauthorizedOperationException, ForbiddenOperationException {
        ServiceProviderSetting existingSetting = null;
        try (DbSession session = DbSession.newSession()) {
            existingSetting = ServiceProviderRepoManager.getStorageProvider(session, serviceProviderSetting.getId());
        }
        validateUpdate(serviceProviderSetting, existingSetting);

        try (DbSession session = DbSession.newSession()) {
            boolean existingAllowed = Boolean.parseBoolean(existingSetting.getAttributes().get(ServiceProviderSetting.ALLOW_PERSONAL_REPO));
            boolean newAllowed = Boolean.parseBoolean(serviceProviderSetting.getAttributes().get(ServiceProviderSetting.ALLOW_PERSONAL_REPO));
            String newName = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME);

            if (existingAllowed && !newAllowed) {
                session.createNamedQuery("deleteUnsharedRepoByProviderId").setParameter("providerId", existingSetting.getId()).executeUpdate();
            }

            List<String> repoIds = null;
            JsonRepository repository = null;
            if (getSupportedProviderMap().get(serviceProviderSetting.getProviderType().name()).getProviderClass().equals(SupportedProvider.ProviderClass.APPLICATION)) {
                validateDisplayNameOnUpdate(session, serviceProviderSetting);
                repoIds = RepositoryManager.getRepoIdsWithTenantAndServiceProviderId(session, serviceProviderSetting.getId());
                if (repoIds.size() > 1) {
                    throw new ValidateException(4004, "More than 1 repo configured for application class service provider");
                }
                repository = new ApplicationServiceProviderCreator().getRepoParameters(serviceProviderSetting, principal);
            }

            StorageProvider toUpdate = ServiceProviderRepoManager.toStorageProvider(serviceProviderSetting, session.get(StorageProvider.class, serviceProviderSetting.getId()));
            String id = ServiceProviderRepoManager.updateServiceProvider(session, toUpdate);
            if (getSupportedProviderMap().get(serviceProviderSetting.getProviderType().name()).getProviderClass().equals(SupportedProvider.ProviderClass.APPLICATION)) {
                session.beginTransaction();
                RepositoryManager.updateRepositoryName(session, principal, repoIds.get(0), newName);
                RepositoryManager.updateClientToken(session, principal, repoIds.get(0), repository.getToken(), principal.getDeviceType());
                applicationRepositoryFactory.removeRepositoryInstance(repoIds.get(0));
                session.commit();
            }

            return id;
        }
    }

    private void validateUpdate(ServiceProviderSetting serviceProviderSetting, ServiceProviderSetting existingSetting) {
        if (existingSetting == null) {
            throw new ValidateException(4001, "Service Provider configuration not found.");
        }
        if (!existingSetting.getProviderType().equals(serviceProviderSetting.getProviderType())) {
            throw new ValidateException(4003, "Service Provider type cannot be changed.");
        }
        if (!existingSetting.getTenantId().equals(serviceProviderSetting.getTenantId())) {
            throw new ValidateException(4004, "Service Provider tenant cannot be changed.");
        }
    }

    private void validateDisplayNameOnUpdate(DbSession session, ServiceProviderSetting serviceProviderSetting) {
        String newName = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME);
        List<ServiceProviderSetting> existingServiceProviderSettings = ServiceProviderRepoManager.getStorageProvider(session, serviceProviderSetting.getTenantId(), serviceProviderSetting.getProviderType().ordinal());
        if (existingServiceProviderSettings != null && existingServiceProviderSettings.parallelStream().anyMatch(s -> s.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME).equals(newName) && !s.getId().equals(serviceProviderSetting.getId()))) {
            throw new ValidateException(4004, String.format("Service provider settings with display name '%s' already exist", newName));
        }
    }
}
