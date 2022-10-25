package com.nextlabs.rms.entity.setting;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nnallagatla
 *
 */
public enum ServiceProviderType {

    /*
     * do not change the order of below enums. We are persisting ServiceProviderType.ordinal() as part of storage_provider table
     */

    DROPBOX,
    GOOGLE_DRIVE,
    ONE_DRIVE,
    SHAREPOINT_ONPREMISE,
    SHAREPOINT_ONLINE,
    SHAREPOINT_CROSSLAUNCH,
    SHAREPOINT_ONLINE_CROSSLAUNCH,
    S3,
    BOX,
    ONEDRIVE_FORBUSINESS,
    LOCAL_DRIVE,
    ONE_DRIVE_APPLICATION,
    SHAREPOINT;

    private static Map<Integer, ServiceProviderType> map = new HashMap<>();

    static {
        for (ServiceProviderType type : ServiceProviderType.values()) {
            map.put(type.ordinal(), type);
        }
    }

    public static ServiceProviderType getByOrdinal(int id) {
        return map.get(id);
    }
}
