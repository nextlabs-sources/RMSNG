/**
 * 
 */
package com.nextlabs.nxl;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * @author RMS-DEV-TEAM@nextlabs.com
 *
 */
public interface Constants {

    public static final String NXL_FILE_EXTN = ".nxl";
    public static final String RMS_URL = "rmsURL";
    public static final String TENANT_NAME = "tenant";
    public static final String DEFAULT_TENANT = "defaultTenant";
    public static final String MEMBERSHIP_ID = "membershipID";
    public static final Type FILEINFO_TYPE = new TypeToken<FileInfo>() {
    }.getType();
}
