package com.nextlabs.rms.serviceprovider;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.ServiceProviderRepoManager;

import java.util.Date;
import java.util.List;

public class DefaultServiceProviderCreator implements IServiceProviderCreator {

    @Override
    public String createServiceProvider(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal) {
        String serviceProviderId = null;

        try (DbSession session = DbSession.newSession()) {
            List<ServiceProviderSetting> existingServiceProviderSettings = ServiceProviderRepoManager.getStorageProvider(session, serviceProviderSetting.getTenantId(), serviceProviderSetting.getProviderType().ordinal());
            if (existingServiceProviderSettings != null && !existingServiceProviderSettings.isEmpty()) {
                throw new ValidateException(4004, "Service provider settings already exist");
            }
            StorageProvider sp = new StorageProvider();
            sp.setTenantId(serviceProviderSetting.getTenantId());
            sp.setType(serviceProviderSetting.getProviderType().ordinal());
            sp.setName(ServiceProviderSetting.getProviderTypeDisplayName(serviceProviderSetting.getProviderType().toString()));
            sp.setAttributes(GsonUtils.GSON.toJson(serviceProviderSetting.getAttributes()));
            sp.setCreationTime(new Date());

            serviceProviderId = ServiceProviderRepoManager.addServiceProvider(session, sp);
        }
        return serviceProviderId;

    }

}
