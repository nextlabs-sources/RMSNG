package com.nextlabs.rms.config;

import com.nextlabs.rms.shared.UploadUtil;

/**
 * Created by IntelliJ IDEA.
 * User: tbiegeleisen
 * Date: 6/28/16
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Constants {

    public static final String TEMPDIR_NAME = "temp";

    public static final boolean RETAIN_UPLOADED_FILE = false;

    public static final int FILE_UPLD_THRESHOLD_SIZE = UploadUtil.THRESHOLD_SIZE;

    public static final long FILE_UPLD_MAX_REQUEST_SIZE = UploadUtil.REQUEST_SIZE;

    public static final String RMC_CURRENT_VERSION = "RMC_CURRENT_VERSION";

    public static final String RMC_MAC_CURRENT_VERSION = "RMC_MAC_CURRENT_VERSION";

    public static final String RMC_FORCE_DOWNGRADE = "RMC_FORCE_DOWNGRADE";

    public static final String RMC_UPDATE_URL_32BITS = "RMC_UPDATE_URL_32BITS";

    public static final String RMC_CRC_CHECKSUM_32BITS = "RMC_CRC_CHECKSUM_32BITS";

    public static final String RMC_SHA1_CHECKSUM_32BITS = "RMC_SHA1_CHECKSUM_32BITS";

    public static final String RMC_UPDATE_URL_64BITS = "RMC_UPDATE_URL_64BITS";

    public static final String RMC_CRC_CHECKSUM_64BITS = "RMC_CRC_CHECKSUM_64BITS";

    public static final String RMC_SHA1_CHECKSUM_64BITS = "RMC_SHA1_CHECKSUM_64BITS";

    public static final String RMC_CRC_CHECKSUM_MAC = "RMC_CRC_CHECKSUM_MAC";

    public static final String RMC_SHA1_CHECKSUM_MAC = "RMC_SHA1_CHECKSUM_MAC";

    public static final String RMD_WIN_32_DOWNLOAD_URL = "RMD_WIN_32_DOWNLOAD_URL";

    public static final String RMD_WIN_64_DOWNLOAD_URL = "RMD_WIN_64_DOWNLOAD_URL";

    public static final String RMD_MAC_DOWNLOAD_URL = "RMD_MAC_DOWNLOAD_URL";

    public static final String RMC_IOS_DOWNLOAD_URL = "RMC_IOS_DOWNLOAD_URL";

    public static final String RMC_ANDROID_DOWNLOAD_URL = "RMC_ANDROID_DOWNLOAD_URL";

    public static final String CLIENT_HEARTBEAT_FREQUENCY = "CLIENT_HEARTBEAT_FREQUENCY";

    public static final String DEFAULT_CLIENT_HEARTBEAT_FREQUENCY = "60";

    public static final String RMS_WELCOME_PAGE = "WELCOME";

    public static final String RMS_HOME_PAGE = "HOME";

    public static final String RMS_SP_PAGE = "SP";

    public static final String RMS_MANAGE_REPO_PAGE = "MANAGE_REPO";

    public static final String USER_PREF_LANDING_PAGE = "lPage";

    public static final String DEFAULT_TENANT = "skydrm.com";

    public static final int DEFAULT_CC_SESSION_DURATION = 20 * 60 * 1000;

}
