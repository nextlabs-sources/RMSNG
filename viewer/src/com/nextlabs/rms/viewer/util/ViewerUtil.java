package com.nextlabs.rms.viewer.util;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.client.Client;
import com.nextlabs.rms.viewer.json.ShowFileResponse;
import com.nextlabs.rms.viewer.manager.ViewFileManager;
import com.nextlabs.rms.viewer.repository.FileCacheId;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public final class ViewerUtil {

    private ViewerUtil() {
    }

    public static User extractUserFromRequest(HttpServletRequest request) {
        User user = new User.Builder().build();
        String userId = request.getHeader("userId");
        String ticket = request.getHeader("ticket");
        String clientId = request.getHeader("clientId");
        String platformId = request.getHeader("platformId");
        String deviceId = request.getHeader("deviceId");
        if (StringUtils.hasText(userId) && StringUtils.hasText(ticket) && StringUtils.hasText(clientId)) {
            user.setId(userId);
            user.setTicket(ticket);
            user.setClientId(clientId);
            if (StringUtils.hasText(platformId) && org.apache.commons.lang3.StringUtils.isNumeric(platformId)) {
                user.setPlatformId(Integer.parseInt(platformId));
            }
            if (StringUtils.hasText(deviceId)) {
                try {
                    user.setDeviceId(URLDecoder.decode(deviceId, "UTF-8"));
                } catch (UnsupportedEncodingException e) { //NOPMD
                }
            }
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if ("userId".equals(name)) {
                        user.setId(cookie.getValue());
                    } else if ("ticket".equals(name)) {
                        user.setTicket(cookie.getValue());
                    } else if ("tenantId".equals(name)) {
                        user.setTenantId(cookie.getValue());
                    } else if ("clientId".equals(name)) {
                        user.setClientId(cookie.getValue());
                    } else if ("platformId".equals(name)) {
                        String value = cookie.getValue();
                        boolean numeric = org.apache.commons.lang3.StringUtils.isNumeric(value);
                        if (numeric) {
                            user.setPlatformId(Integer.parseInt(value.trim()));
                        }
                    } else if ("deviceId".equals(name)) {
                        try {
                            user.setDeviceId(URLDecoder.decode(cookie.getValue(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) { //NOPMD
                        }
                    }
                }
            }
        }
        if (user.getId() == null || user.getTicket() == null || user.getClientId() == null) {
            return null;
        }
        return user;
    }

    public static void setCookie(String name, String value, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(request.getServletContext().getContextPath());
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    public static void sendErrorResponse(String cacheId, HttpServletRequest request, HttpServletResponse response,
        String viewingSessionId) {
        sendErrorResponseWithStatusCode(cacheId, request, response, viewingSessionId, 200);
    }

    public static void sendErrorResponse(HttpServletRequest request, HttpServletResponse response,
        String viewingSessionId, String code) {
        sendErrorResponseWithStatusCode(request, response, viewingSessionId, code, 200);
    }

    public static void sendErrorResponseWithStatusCode(String cacheId, HttpServletRequest request,
        HttpServletResponse response, String viewingSessionId, int statusCode) {
        ShowFileResponse result = new ShowFileResponse(HTTPUtil.getURI(request));
        String redirectURL = new StringBuilder("/ShowError.jsp?d=").append(cacheId).append("&s=").append(viewingSessionId).toString();
        result.setViewerUrl(redirectURL);
        result.setStatusCode(statusCode);
        JsonUtil.writeJsonToResponse(result, response);
    }

    public static void sendErrorResponseWithStatusCode(HttpServletRequest request,
        HttpServletResponse response, String viewingSessionId, String code, int statusCode) {
        ShowFileResponse result = new ShowFileResponse(HTTPUtil.getURI(request));
        String redirectURL = new StringBuilder("/ShowError.jsp?code=").append(code).append("&s=").append(viewingSessionId).toString();
        result.setViewerUrl(redirectURL);
        result.setStatusCode(statusCode);
        JsonUtil.writeJsonToResponse(result, response);
    }

    public static String setError(String sessionId, String errMsg) {
        Ehcache cache = ViewerCacheManager.getInstance().getCache(ViewerCacheManager.CACHEID_FILECONTENT);
        String cacheId = System.currentTimeMillis() + UUID.randomUUID().toString();
        FileCacheId fileCacheId = new FileCacheId(sessionId, cacheId);
        cache.put(new Element(fileCacheId, errMsg));
        return cacheId;
    }

    public static String getRMSInternalURL(String tenantName) throws IOException {
        if (ViewFileManager.RMS_INTERNAL_URL != null) {
            return ViewFileManager.RMS_INTERNAL_URL;
        }
        return Client.getRMSURL(tenantName);
    }

    public static String getRouterInternalURL() throws IOException {
        if (ViewFileManager.ROUTER_INTERNAL_URL != null) {
            return ViewFileManager.ROUTER_INTERNAL_URL;
        }
        return ViewFileManager.ROUTER_URL;
    }
}
