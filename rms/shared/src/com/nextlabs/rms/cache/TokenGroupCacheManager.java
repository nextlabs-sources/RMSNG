package com.nextlabs.rms.cache;

import com.nextlabs.common.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TokenGroupCacheManager {

    private final RMSCacheManager.Cache<String, String> tokenGroupCache;
    private static TokenGroupCacheManager instance;

    private static final String MEMBERSHIP_MODEL_SUFFIX = "_abac_membership";

    private TokenGroupCacheManager() {
        this.tokenGroupCache = RMSCacheManager.getInstance().getTokenGroupCache();
    }

    public static synchronized void init(List<Map<String, String>> tokenGroups) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Token Group Cache Manager is already initialized.");
        }
        instance = new TokenGroupCacheManager();
        if (!tokenGroups.isEmpty()) {
            instance.clearAll();
            for (Map<String, String> entry : tokenGroups) {
                instance.putResourceType(entry.get("tokenGroupName"), entry.get("keystoreId"));
            }
        }
    }

    public String getResourceType(String tokenGroupName) {
        if (tokenGroupName.endsWith(MEMBERSHIP_MODEL_SUFFIX)) {
            tokenGroupName = StringUtils.substringBefore(tokenGroupName, MEMBERSHIP_MODEL_SUFFIX);
            String keystoreId = tokenGroupCache.get(tokenGroupName);
            if (keystoreId != null) {
                return keystoreId + MEMBERSHIP_MODEL_SUFFIX;
            }
        } else {
            String keystoreId = tokenGroupCache.get(tokenGroupName);
            if (keystoreId != null) {
                return keystoreId;
            }
        }
        return "";
    }

    public void putResourceType(String tokenGroupName, String keystoreId) {
        tokenGroupCache.put(tokenGroupName, keystoreId);
    }

    private void clearAll() {
        for (String key : tokenGroupCache.keySet()) {
            tokenGroupCache.remove(key);
        }
    }

    public Set<Map.Entry<String, String>> getAllMappings() {
        return tokenGroupCache.entrySet();
    }

    public static TokenGroupCacheManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Token Group Cache Manager is not initialized.");
        }
        return instance;
    }

}
