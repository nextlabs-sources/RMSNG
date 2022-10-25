package com.nextlabs.rms.util;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.util.StringUtils;

public final class MembershipUtil {

    private MembershipUtil() {
    }

    public static String getTokenGroup(String fileOwner) {
        if (fileOwner.endsWith(Constants.SYSTEM_BUCKET_NAME_SUFFIX)) {
            return StringUtils.substringAfter(StringUtils.substringBefore(fileOwner, Constants.SYSTEM_BUCKET_NAME_SUFFIX), "@");
        } else {
            return StringUtils.substringAfter(fileOwner, "@");
        }
    }
}
