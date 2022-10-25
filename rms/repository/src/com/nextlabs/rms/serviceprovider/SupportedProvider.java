package com.nextlabs.rms.serviceprovider;

public class SupportedProvider {

    private String name;
    private String provider;
    private ProviderClass providerClass;

    public enum ProviderClass {
        PERSONAL,
        APPLICATION,
        BUSINESS
    }

    public SupportedProvider(String name, String provider,
        ProviderClass providerClass) {
        super();
        this.name = name;
        this.provider = provider;
        this.providerClass = providerClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public ProviderClass getProviderClass() {
        return providerClass;
    }

    public void setProviderClass(ProviderClass providerClass) {
        this.providerClass = providerClass;
    }

}
