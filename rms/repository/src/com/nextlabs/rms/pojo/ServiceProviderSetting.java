package com.nextlabs.rms.pojo;

import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.locale.RMSMessageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nnallagatla
 */
public class ServiceProviderSetting {

    public static final String APP_ID = "APP_ID";
    public static final String APP_SECRET = "APP_SECRET";
    public static final String SITE_URL = "SITE_URL";
    public static final String DRIVE_NAME = "DRIVE_NAME";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_SECRET = "USER_SECRET";
    public static final String REDIRECT_URL = "REDIRECT_URL";
    public static final String APP_TENANT_ID = "APP_TENANT_ID";
    public static final String DISPLAY_NAME = "DISPLAY_NAME";
    public static final String APP_NAME = "APP_NAME";
    public static final String REMOTE_WEB_URL = "REMOTE_WEB_URL";
    public static final String ALLOW_PERSONAL_REPO = "ALLOW_PERSONAL_REPO";
    public static final String APP_DISPLAY_MENU = "APP_DISPLAY_MENU";
    public static final String SP_ONLINE_APP_CONTEXT_ID = "SP_ONLINE_APP_CONTEXT_ID";
    public static final String SP_ONPREMISE_SITE = "SHAREPOINT_SITE";
    public static final String MYSPACE_BUCKET_NAME = "MYSPACE_BUCKET_NAME";
    public static final String PROJECT_BUCKET_NAME = "PROJECT_BUCKET_NAME";
    public static final String ENTERPRISE_BUCKET_NAME = "ENTERPRISE_BUCKET_NAME";
    public static final String OD4B_ROOTFOLDER = "OD4B_ROOTFOLDER";
    public static final String LOCAL_DRIVE_PATH = "LOCAL_PATH";

    private String id;
    private String tenantId;
    private ServiceProviderType providerType;
    private String providerTypeDisplayName;
    private Map<String, String> attributes = new HashMap<>();
    private String displayMenuString;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public ServiceProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ServiceProviderType providerType) {
        this.providerType = providerType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderTypeDisplayName() {
        return providerTypeDisplayName;
    }

    public void setProviderTypeDisplayName(String providerTypeDisplayName) {
        this.providerTypeDisplayName = providerTypeDisplayName;
    }

    public static String getProviderTypeDisplayName(String type) {
        return RMSMessageHandler.getClientString(type + "_display_name");
    }

    public String getDisplayMenuString() {
        return displayMenuString;
    }

    public void setDisplayMenuString(String displayMenu) {
        this.displayMenuString = displayMenu;
    }
}
