package com.nextlabs.rms.serviceprovider;

import com.nextlabs.rms.entity.setting.ServiceProviderType;

import java.util.Map;

public final class ServiceProviderCreationFactory {

    private ServiceProviderCreationFactory() {

    }

    public static IServiceProviderCreator getCreator(ServiceProviderType providerType,
        Map<String, SupportedProvider> supportedProviderMap) {
        IServiceProviderCreator serviceProviderCreator = null;
        if (supportedProviderMap.get(providerType.name()).getProviderClass().equals(SupportedProvider.ProviderClass.APPLICATION)) {
            serviceProviderCreator = new ApplicationServiceProviderCreator();
        } else {
            serviceProviderCreator = new DefaultServiceProviderCreator();
        }
        return serviceProviderCreator;
    }

}
