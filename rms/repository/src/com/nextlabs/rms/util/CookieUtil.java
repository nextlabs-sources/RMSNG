package com.nextlabs.rms.util;

import com.nextlabs.common.shared.WebConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static void clearCookies(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            final String domain = getCookieDomainName(request);
            for (Cookie cookie : cookies) {
                cookie.setDomain(domain);
                cookie.setValue(null);
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
    }

    public static String getCookieDomainName(HttpServletRequest request) {
        return WebConfig.getInstance().getProperty(WebConfig.COOKIE_DOMAIN, "." + request.getServerName());
    }

    public static String[] getParamsFromCookies(HttpServletRequest request, String... params) {
        if (params != null && params.length > 0) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                String[] values = new String[params.length];
                Map<String, Integer> paramMap = new HashMap<String, Integer>((int)Math.ceil(params.length / 0.7));
                for (int i = 0; i < params.length; i++) {
                    paramMap.put(params[i], i);
                }
                int count = 0;
                String cookieName = null;
                for (int i = 0; i < cookies.length; i++) {
                    cookieName = cookies[i].getName();
                    if (paramMap.containsKey(cookieName)) {
                        values[paramMap.get(cookieName)] = cookies[i].getValue();
                        if (++count == params.length) {
                            break;
                        }
                    }
                }
                return values;
            }
        }
        return null;
    }

}
