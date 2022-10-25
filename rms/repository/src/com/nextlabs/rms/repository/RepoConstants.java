package com.nextlabs.rms.repository;

public class RepoConstants {

    public static final String RMS_CLIENT_IDENTIFIER = "RMS";

    public static final String DROPBOX_AUTH_START_URL = "OAuthManager/DBAuth/DBAuthStart";

    public static final String DROPBOX_AUTH_FINSIH_URL = "OAuthManager/DBAuth/dbAuthFinish";

    public static final String GOOGLE_DRIVE_AUTH_START_URL = "OAuthManager/GDAuth/GDAuthStart";

    public static final String GOOGLE_DRIVE_AUTH_FINISH_URL = "OAuthManager/GDAuth/gdAuthFinish";

    public static final String ONE_DRIVE_AUTH_START_URL = "OAuthManager/ODAuth/ODAuthStart";

    public static final String ONE_DRIVE_AUTH_FINISH_URL = "OAuthManager/ODAuth/odAuthFinish";

    public static final String SHAREPOINT_ONLINE_AUTH_START_URL = "OAuthManager/SPOnlineAuth/SPOnlineAuthStart";

    public static final String SHAREPOINT_ONLINE_AUTH_FINISH_URL = "OAuthManager/SPOnlineAuth/SPOnlineAuthFinish";

    public static final String BOX_AUTH_START_URL = "OAuthManager/BoxAuth/BoxAuthStart";

    public static final String BOX_AUTH_FINISH_URL = "OAuthManager/BoxAuth/BoxAuthFinish";

    public static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;

    public static final int READ_TIMEOUT = 5 * 60 * 1000;

    public static final String MY_VAULT_NAME = "nxl_myvault_nxl";

    public static final String MY_VAULT_FOLDER_PATH_ID = "/" + MY_VAULT_NAME + "/";

    public static final String MY_VAULT_FOLDER_PATH_DISPLAY = "/" + MY_VAULT_NAME;

    public static final Long USER_MYSPACE_QUOTA = 1073741824L;

    public static final Long USER_MYSPACE_GRACE_STORAGE = 2147483648L;

    public static final String DB_STORAGE_USED = "size";

    public static final String STORAGE_USED = "usage";

    public static final String DB_MYVAULT_STORAGE_USED = "my_vault_size";

    public static final String MY_VAULT_STORAGE_USED = "myVaultUsage";

    public static final String USER_QUOTA = "quota";

    public static final String USER_VAULT_QUOTA = "vaultQuota";

    public static final String KEY_USE_JSON_ENDPOINT = "useJsonEndPoint";

    public static final String KEY_AUTHORIZE_TYPE = "authorizeType";

    public static final String KEY_REDIRECT_URL = "redirectURL";

    public static final int MAX_FOLDERNAME_LENGTH = 127;

    public enum AUTHORIZE_TYPE {
        AUTHORIZE_TYPE_WEB,
        AUTHORIZE_TYPE_JSON,
        AUTHORIZE_TYPE_CUSTOM
    }

}
