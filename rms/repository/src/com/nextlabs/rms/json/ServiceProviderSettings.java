package com.nextlabs.rms.json;

import com.nextlabs.rms.pojo.ServiceProviderSetting;

import java.util.List;
import java.util.Map;

/**
 * @author nnallagatla
 *
 */
public class ServiceProviderSettings {

    private List<ServiceProviderSetting> serviceProviderSettingList;
    private Map<String, String> supportedProvidersMap;
    private String redirectUrl;

    /**
     * @param crossLaunchProviders
     *
     */
    public ServiceProviderSettings(List<ServiceProviderSetting> serviceProviderSettingList,
        Map<String, String> supportedProviders, String redirectUrl) {
        this.serviceProviderSettingList = serviceProviderSettingList;
        this.supportedProvidersMap = supportedProviders;
        this.redirectUrl = redirectUrl;
    }

    public List<ServiceProviderSetting> getServiceProviderSettingList() {
        return serviceProviderSettingList;
    }

    public Map<String, String> getSupportedProvidersMap() {
        return supportedProvidersMap;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
