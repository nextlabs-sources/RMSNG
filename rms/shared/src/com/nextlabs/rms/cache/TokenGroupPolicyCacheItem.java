package com.nextlabs.rms.cache;

import java.io.Serializable;

/**
 * @author RMS-DEV-TEAM@nextlabs.com
 * @version 1.0.0
 */

public final class TokenGroupPolicyCacheItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String tokenGroupName;
    private final String tokenGroupPolicies;
    private final long timeStamp;

    /**
     * @param tokenGroupName
     * @param tokenGroupPolicies
     * @param timeStamp
     */
    public TokenGroupPolicyCacheItem(String tokenGroupName, String tokenGroupPolicies, long timeStamp) {

        this.tokenGroupName = tokenGroupName;
        this.tokenGroupPolicies = tokenGroupPolicies;
        this.timeStamp = timeStamp;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getTokenGroupPolicies() {
        return tokenGroupPolicies;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
