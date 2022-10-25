package com.nextlabs.rms.shared;

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public final class LocalizationUtil {

    public static final String RESOURCE_BUNDLE = ".resource_bundle";

    public static String getMessage(HttpServletRequest request, String code, Object[] args, Locale locale) {
        return getMessage(request.getServletContext(), code, args, locale);
    }

    public static String getMessage(ServletContext servletContext, String code, Object[] args, Locale locale) {
        ResourceBundleContext ctx = (ResourceBundleContext)servletContext.getAttribute(RESOURCE_BUNDLE);
        if (ctx == null) {
            throw new IllegalArgumentException("Resource bundle is not initialized");
        }
        return ctx.getMessage(code, args, locale);
    }

    public static void setBasenameResourceBundles(ServletContext servletContext, String... basenames) {
        ResourceBundleContext ctx = new ResourceBundleContext();
        ctx.setBasenameResourceBundles(basenames);
        servletContext.setAttribute(RESOURCE_BUNDLE, ctx);
    }

    private LocalizationUtil() {
    }

}
