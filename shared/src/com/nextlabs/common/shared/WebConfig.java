package com.nextlabs.common.shared;

import java.io.File;
import java.util.Properties;

public final class WebConfig {

    public static final String SAAS = "saas";
    public static final String HIDE_WORKSPACE = "hide_workspace";
    public static final String DAP_SERVER_ENABLED = "dapserver.enabled";
    public static final String DEBUG = "debug";
    public static final String ROUTER_URL = "router_url";
    public static final String ROUTER_INTERNAL_URL = "router_internal_url";
    public static final String VIEWER_URL = "viewer_url";
    public static final String VIEWER_INTERNAL_URL = "viewer_internal_url";
    public static final String RMS_INTERNAL_URL = "rms_internal_url";
    public static final String IDP_RMS_ATTRIBUTES = "idp.rms.attributes";
    public static final String IDP_GOOGLE_ATTRIBUTES = "idp.google.attributes";
    public static final String IDP_FB_ATTRIBUTES = "idp.fb.attributes";
    public static final String IDP_SAML_COUNT = "idp.saml.count";
    public static final String IDP_LDAP_COUNT = "idp.ldap.count";
    public static final String IDP_AZUREAD_COUNT = "idp.azuread.count";
    public static final String IDP_SAML_ATTRIBUTES = "idp.saml.";
    public static final String IDP_LDAP_ATTRIBUTES = "idp.ldap.";
    public static final String IDP_AZUREAD_ATTRIBUTES = "idp.azuread.";
    public static final String DEFAULT_USER_ATTRIBUTES = "user.attributes.default";
    public static final String USER_ATTRIBUTES_SELECT_MAXNUM = "user.attributes.select.maxNum";
    public static final String CLASSIFICATION_SELECT_CATEGORY_MAXNUM = "classification.select.maxCategoryNum";
    public static final String CLASSIFICATION_SELECT_LABEL_MAXNUM = "classification.select.maxLabelNum";
    public static final String COOKIE_DOMAIN = "cookie_domain";
    public static final String TERMS_URL = "terms_url";
    public static final String PRIVACY_URL = "privacy_policy_url";
    public static final String PROJECT_TERMS_URL = "project_terms_url";
    public static final String PROJECT_PRIVACY_URL = "project_privacy_policy_url";
    public static final String MAX_MAIL_SIZE_MB = "max_mail_size_mb";
    public static final String INCOMPATIBLE_VERSIONS = "incompatible_versions";
    public static final String INBUILT_SERVICE_PROVIDER = "inbuilt_storage_provider";
    public static final String INBUILT_SERVICE_PROVIDER_ATTRIBUTES = "inbuilt_storage_provider.attributes";
    public static final String RMS_FEEDBACK_MAILID = "feedback_mailid";
    public static final String WELCOME_PAGE_VIDEO_URL = "welcome_page_video_url";
    public static final String ICENET_URL = "icenet.url";
    public static final String CC_AGENT_VERSION = "cc.agent.version";
    public static final String CC_CONSOLE_URL = "cc.console_url";
    public static final String CC_ADMIN_ID = "cc.admin.id";
    public static final String CC_ADMIN_SECRET = "cc.admin.secret";
    public static final String CC_SESSION_TIMEOUT = "cc.session.timeout";
    public static final String CC_OAUTH_CLIENT_ID = "cc.oauth.client_id";
    public static final String PDP_POLICY_CONTROLLER_URL = "pdp.policy_controller_url";
    public static final String PREFER_EXTERNAL_PDP = "prefer_external_pdp";
    public static final String CC_OAUTH_CLIENT_SECRET = "cc.oauth.client_secret";
    public static final String HEARTBEAT_FREQUENCY = "schedule.heartbeat.frequency";
    public static final String PROJECT_CLEANUP_FREQUENCY = "project.cleanup.frequency";
    public static final String REGISTER_AGENT_FREQUENCY = "schedule.register.agent.frequency";
    public static final String PUBLIC_TENANT = "publicTenant";
    public static final String PROJECT_TRIAL_DURATION = "project_trial_duration";
    public static final String USER_MYSPACE_QUOTA = "user_myspace_quota";
    public static final String USER_MYSPACE_GRACE_STORAGE = "user_myspace_grace_storage";
    public static final String PROJECT_SPACE_QUOTA = "project_space_quota";
    public static final String ENTERPRISE_SPACE_QUOTA = "enterprise_space_quota";
    public static final String EMAIL_DOMAIN_NAME = "email_domain_name";
    public static final String EMAIL_SENDER_NAME = "email_sender_name";
    public static final String KEYSTORE_PASS = "keystore_password";
    public static final String PUBLIC_TENANT_ADMIN = "public_tenant_admin";
    public static final String RMC_CUSTOM_URL = "rmc_custom_url";
    public static final String CACHING_MODE_SERVER = "caching.mode.server";
    public static final String CACHING_SERVER_HOSTNAME = "caching.server.hostname";
    public static final String CACHING_SERVER_CLIENT_PORT = "caching.server.client.port";
    public static final String RABBITMQ_API_URL = "rabbitmq.api.host";
    public static final String RABBITMQ_API_PORT = "rabbitmq.api.port";
    public static final String RABBITMQ_MQ_URL = "rabbitmq.mq.host";
    public static final String RABBITMQ_MQ_PORT = "rabbitmq.mq.port";
    public static final String RABBITMQ_USER = "rabbitmq.username";
    public static final String RABBITMQ_PASSWORD = "rabbitmq.password";
    public static final String RMX_UNIQUE_USER_ID = "rmx.unique_user_id";
    public static final String SDK_TOKENCACHE_MAXIMUMSIZE = "sdk.cache.config.maximumsize";
    public static final String SDK_TOKENCACHE_DEFAULT_MAXIMUMSIZE = "10000";
    public static final String SDK_TOKENCACHE_EXPIRY = "sdk.cache.config.expiry";
    public static final String SDK_TOKENCACHE_DEFAULT_EXPIRY = "10";
    public static final String RESTCLIENT_READ_TIMEOUT = "restclient.read.timeout";
    public static final String RESTCLIENT_CONNECTION_TIMEOUT = "restclient.connection.timeout";
    public static final String APP_LOGIN_NONCE_EXPIRY = "app.login.nonce.expiry";
    public static final String LOGGER_URL = "logger.url";
    private static final WebConfig INSTANCE = new WebConfig();

    private Properties prop;
    private File webBaseDir;
    private File configDir;
    private File tmpDir;
    private File commonSharedTempDir;
    private File rmsSharedTempDir;

    private WebConfig() {
        prop = new Properties();
    }

    public static WebConfig getInstance() {
        return INSTANCE;
    }

    public void setWebBaseDir(File webBaseDir) {
        this.webBaseDir = webBaseDir;
    }

    public File getWebBaseDir() {
        return webBaseDir;
    }

    public void setConfigDir(File configDir) {
        this.configDir = configDir;
    }

    public File getConfigDir() {
        return configDir;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public File getCommonSharedTempDir() {
        return commonSharedTempDir;
    }

    public void setCommonSharedTempDir(File commonSharedTempDir) {
        this.commonSharedTempDir = commonSharedTempDir;
    }

    public File getRmsSharedTempDir() {
        return rmsSharedTempDir;
    }

    public void setRmsSharedTempDir(File rmsSharedTempDir) {
        this.rmsSharedTempDir = rmsSharedTempDir;
    }

    public String getProperty(String key) {
        return prop.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }

    public boolean getBooleanProperty(String key) {
        String val = prop.getProperty(key);
        if (val == null) {
            return false;
        }
        return "true".equalsIgnoreCase(val.trim()) || "yes".equalsIgnoreCase(val.trim());
    }

    public int getIntProperty(String key) {
        int val = -1;
        try {
            String strVal = prop.getProperty(key);
            if (strVal != null && strVal.length() > 0) {
                val = Integer.parseInt(strVal.trim());
            }
            return val;
        } catch (Exception e) {
            return val;
        }
    }

    public void setProperty(String key, String value) {
        prop.setProperty(key, value);
    }
}
