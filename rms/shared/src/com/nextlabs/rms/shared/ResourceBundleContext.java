package com.nextlabs.rms.shared;

import com.nextlabs.common.util.StringUtils;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class ResourceBundleContext {

    private Set<String> resources = new HashSet<>(1);

    private ResourceBundle doGetBundle(String basename, Locale locale) {
        return ResourceBundle.getBundle(basename, locale);
    }

    public String getMessage(String code, Object[] args, Locale locale) {
        String msg = getMessageInternal(code, args, locale);
        return msg != null ? msg : code;
    }

    private MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale) {
        String msg = getStringOrNull(bundle, code);
        return msg != null ? new MessageFormat(msg, locale) : null;
    }

    private String getMessageInternal(String code, Object[] args, Locale locale) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (args == null || args.length == 0) {
            String message = resolveCodeWithoutArguments(code, locale);
            if (message != null) {
                return message;
            }
        } else {
            MessageFormat messageFormat = resolveCode(code, locale);
            if (messageFormat != null) {
                return messageFormat.format(args);
            }
        }
        return null;
    }

    private ResourceBundle getResourceBundle(String basename, Locale locale) {
        try {
            return doGetBundle(basename, locale);
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    private String getStringOrNull(ResourceBundle bundle, String code) {
        try {
            return bundle != null && bundle.containsKey(code) ? bundle.getString(code) : null;
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private MessageFormat resolveCode(String code, Locale locale) {
        for (String basename : resources) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null) {
                MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
                if (messageFormat != null) {
                    return messageFormat;
                }
            }
        }
        return null;
    }

    private String resolveCodeWithoutArguments(String code, Locale locale) {
        for (String basename : resources) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null && bundle.containsKey(code)) {
                return bundle.getString(code);
            }
        }
        return null;
    }

    public void setBasenameResourceBundles(String... basenames) {
        if (basenames != null && basenames.length > 0) {
            synchronized (resources) {
                resources.clear();
                for (String basename : basenames) {
                    if (!StringUtils.hasText(basename)) {
                        throw new IllegalArgumentException("Basename is empty");
                    }
                    resources.add(basename);
                }
            }
        }
    }
}
