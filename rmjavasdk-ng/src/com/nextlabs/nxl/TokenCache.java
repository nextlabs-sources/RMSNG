package com.nextlabs.nxl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum TokenCache {

    INSTANCE;

    private Logger logger = LogManager.getLogger("TokenCache");

    private Cache<String, byte[]> cache;

    private TokenCache() {
        FileInputStream fis = null;
        long maximumSize = Long.parseLong(WebConfig.SDK_TOKENCACHE_DEFAULT_MAXIMUMSIZE);
        long duration = Long.parseLong(WebConfig.SDK_TOKENCACHE_DEFAULT_EXPIRY);
        try {
            String sdkConfigFilePath = System.getProperty("sdk.cache.config");
            if (sdkConfigFilePath != null) {
                File sdkConfigFile = new File(sdkConfigFilePath);
                if (sdkConfigFile.exists() && sdkConfigFile.isFile() && sdkConfigFile.canRead()) {
                    Properties prop = new Properties();
                    fis = new FileInputStream(sdkConfigFile);
                    prop.load(fis);
                    for (String key : prop.stringPropertyNames()) {
                        if (key.equalsIgnoreCase(WebConfig.SDK_TOKENCACHE_MAXIMUMSIZE)) {
                            maximumSize = Long.parseLong(prop.getProperty(WebConfig.SDK_TOKENCACHE_MAXIMUMSIZE));
                        } else if (key.equalsIgnoreCase(WebConfig.SDK_TOKENCACHE_EXPIRY)) {
                            duration = Long.parseLong(prop.getProperty(WebConfig.SDK_TOKENCACHE_EXPIRY));
                        }
                    }
                    this.cache = CacheBuilder.newBuilder().maximumSize(maximumSize).expireAfterWrite(duration, TimeUnit.MINUTES).build();
                    logger.debug("TokenCache initialized with maximumSize = " + maximumSize + ", expiry (minutes) = " + duration);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            IOUtils.closeQuietly(fis);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            IOUtils.closeQuietly(fis);
        } finally {
            IOUtils.closeQuietly(fis);
        }

        if (this.cache == null) {
            this.cache = CacheBuilder.newBuilder().maximumSize(maximumSize).expireAfterWrite(duration, TimeUnit.MINUTES).build();
            logger.debug("TokenCache initialized with default values i.e. maximumSize = " + maximumSize + ", expiry (minutes) = " + duration);
        }
    }

    protected Cache<String, byte[]> getCache() {
        return this.cache;
    }

}
