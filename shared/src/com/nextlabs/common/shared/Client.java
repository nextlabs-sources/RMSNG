package com.nextlabs.common.shared;

public final class Client {

    private String clientId;
    private String deviceId;
    private DeviceType deviceType;
    private String manufacturer;
    private String model;
    private String osVersion;
    private String appName;
    private String appVersion;
    private String pushToken;

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getPushToken() {
        return pushToken;
    }
}
