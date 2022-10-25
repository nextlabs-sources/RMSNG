package com.nextlabs.rms.viewer.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class Client {

    private static final Map<String, String> RMS_CACHE = new ConcurrentHashMap<>();

    private Client() {
    }

    public static String getRMSURL(String tokenGroupName) throws IOException {
        String rms = RMS_CACHE.get(tokenGroupName);
        if (StringUtils.hasText(rms)) {
            return rms;
        }
        String path = ViewerUtil.getRouterInternalURL() + "/rs/q/tokenGroupName/" + URLEncoder.encode(tokenGroupName, StandardCharsets.UTF_8.name());
        Properties prop = new Properties();
        String ret = RestClient.get(path, prop, false);
        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new IOException(resp.getMessage());
        }
        rms = resp.getResultAsString("server");
        RMS_CACHE.put(tokenGroupName, rms);
        return rms;
    }

    public static String getSystemBucketName(String tenantName, String tenantId, User user)
            throws IOException, JsonException {
        Properties prop = new Properties();
        prop.setProperty("userId", user.getId());
        prop.setProperty("ticket", user.getTicket());
        String clientId = user.getClientId();
        Integer platformId = user.getPlatformId();
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null) {
            prop.setProperty("platformId", platformId.toString());
        }
        String respString = RestClient.get(ViewerUtil.getRMSInternalURL(tenantName) + "/rs/tenant/v2/" + tenantId, prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        JsonObject jsonExtra = resp.getExtra(JsonObject.class);
        JsonElement element = jsonExtra.get("SYSTEM_DEFAULT_PROJECT_TENANTID");
        if (element != null) {
            return element.getAsString();
        }
        return null;
    }

    public static JsonResponse getTenantDetails(String tokenGroupName, User user) throws IOException, JsonException {
        Properties prop = new Properties();
        prop.setProperty("userId", user.getId());
        prop.setProperty("ticket", user.getTicket());
        String clientId = user.getClientId();
        Integer platformId = user.getPlatformId();
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null) {
            prop.setProperty("platformId", platformId.toString());
        }
        String respString = RestClient.get(ViewerUtil.getRMSInternalURL(tokenGroupName) + "/rs/tokenGroup/details?tokenGroupName=" + URLEncoder.encode(tokenGroupName, StandardCharsets.UTF_8.name()), prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        return resp;
    }

    public static String getParentTenantName(String tokenGroupName, User user) throws IOException, JsonException {
        JsonResponse resp = getTenantDetails(tokenGroupName, user);
        return resp.getResultAsString("parentTenantName");
    }
}
