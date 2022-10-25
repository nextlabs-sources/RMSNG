package com.nextlabs.rms.locale;

import com.nextlabs.rms.shared.LogConstants;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RMSMessageHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private RMSMessageHandler() {
    }

    /**
     * Returns a localized string corresponding to the key if it is available based on the default locale
     * else returns the key
     * @param key
     * @return
     */
    public static String getString(String key) {
        try {
            ClassLoader cl = RMSMessageHandler.class.getClassLoader();
            ResourceBundle bundle = ResourceBundle.getBundle("com.nextlabs.rms.locale.RMSMessages", Locale.getDefault(), cl);
            return bundle.getString(key);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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
        Locale loc = Locale.getDefault();
        ClassLoader cl = RMSMessageHandler.class.getClassLoader();
        ResourceBundle bundle = ResourceBundle.getBundle("com.nextlabs.rms.locale.RMSMessages", loc, cl);
        return bundle.getString(key);
    }

    public static String getClientString(String key, Object... params) {
        return MessageFormat.format(getClientString(key), params);
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }
}
