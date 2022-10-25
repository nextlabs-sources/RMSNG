package com.nextlabs.rms.cache;

import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TokenGroupCache {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final Map<String, String> TOKEN_GROUP_CACHE = new HashMap<String, String>();

    private TokenGroupCache() {
    }

    public static String lookupTokenGroup(String tokenGroupName) throws UnsupportedEncodingException {
        String rms = TOKEN_GROUP_CACHE.get(tokenGroupName);
        if (rms == null) {
            synchronized (TOKEN_GROUP_CACHE) {
                WebConfig webConfig = WebConfig.getInstance();
                String routerURL = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL, webConfig.getProperty(WebConfig.ROUTER_URL));
                String path = routerURL + "/rs/q/tokenGroupName/" + URLEncoder.encode(tokenGroupName, "UTF-8");
                try {
                    String ret = RestClient.get(path);
                    JsonResponse resp = JsonResponse.fromJson(ret);
                    if (!resp.hasError()) {
                        rms = resp.getResultAsString("server");
                        TOKEN_GROUP_CACHE.put(tokenGroupName, rms);
                    } else {
                        LOGGER.warn(resp.getMessage());
                        if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Response for request '{}': {}", path, ret);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to connect to router: {}", e.getMessage(), e);
                }
            }
        }
        return rms;
    }
}
