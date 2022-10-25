package com.nextlabs.rms.viewer.locale;

import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ViewerMessageHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private ViewerMessageHandler() {
    }

    /**
     * Returns a localized string corresponding to the key if it is available based on the default locale
     * else returns the key
     * @param key
     * @return
     */
    public static String getString(String key) {
        try {
            ClassLoader cl = ViewerMessageHandler.class.getClassLoader();
            ResourceBundle bundle = ResourceBundle.getBundle("com.nextlabs.rms.viewer.locale.ViewerMessages", Locale.getDefault(), cl);
            return bundle.getString(key);
        } catch (Exception ex1) {
            LOGGER.error(ex1.getMessage(), ex1);
            return key;
        }
    }

    /**
     * Returns a localized string corresponding to the key if it is available based on the client locale
     * else returns key
     * @param key
     * @return
     */
    public static String getClientString(String key) {
        try {
            Locale loc = ViewerConfigManager.getInstance().getCurrentUserLocale();
            if (loc == null) {
                return key;
            }
            ClassLoader cl = ViewerMessageHandler.class.getClassLoader();
            ResourceBundle bundle = ResourceBundle.getBundle("com.nextlabs.rms.viewer.locale.ViewerMessages", loc, cl);
            return bundle.getString(key);
        } catch (Exception ex1) {
            LOGGER.error(ex1.getMessage(), ex1);
            return key;
        }
    }

    public static String getClientString(String key, Object... params) {
        return MessageFormat.format(getClientString(key), params);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }
}
