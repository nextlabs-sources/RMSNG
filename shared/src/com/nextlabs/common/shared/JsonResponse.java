package com.nextlabs.common.shared;

import com.google.gson.JsonElement;
import com.nextlabs.common.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JsonResponse {

    private int statusCode;
    private String message;
    private long serverTime = System.currentTimeMillis();
    private Map<String, JsonWraper> results;
    private JsonWraper extra;

    public JsonResponse() {
    }

    public JsonResponse(boolean sort) {
        if (sort) {
            results = new TreeMap<String, JsonWraper>();
        }
    }

    public JsonResponse(String message) {
        this.message = message;
        statusCode = 200;
    }

    public JsonResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public boolean hasError() {
        return statusCode >= 400;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public long getServerTime() {
        return serverTime;
    }

    public String getResultAsString(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }
        return wraper.stringValue();
    }

    public int getResultAsInt(String key, int def) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return def;
        }

        try {
            return wraper.intValue();
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public long getResultAsLong(String key, long def) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return def;
        }

        try {
            return wraper.longValue();
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public List<JsonWraper> getResultAsList(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return Collections.emptyList();
        }

        return wraper.getAsList();
    }

    public Map<String, JsonWraper> getResultAsMap(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return Collections.emptyMap();
        }

        return wraper.getAsMap();
    }

    public <T> T getResult(String key, Class<T> cls) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        JsonElement element = wraper.getAsJsonTree();
        return GsonUtils.GSON_SHALLOW.fromJson(element, cls);
    }

    public <T> T getResult(String key, Type type) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        JsonElement element = wraper.getAsJsonTree();
        return GsonUtils.GSON_SHALLOW.fromJson(element, type);
    }

    public JsonElement getWrappedParameter(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        return wraper.getAsJsonTree();
    }

    public void putResult(String key, Object value) {
        if (results == null) {
            results = new HashMap<String, JsonWraper>();
        }
        results.put(key, new JsonWraper(value));
    }

    public void setExtra(Object extra) {
        this.extra = new JsonWraper(extra);
    }

    public <T> T getExtra(Class<T> clazz) {
        if (extra == null) {
            return null;
        }
        return GsonUtils.GSON_SHALLOW.fromJson(extra.getAsJsonTree(), clazz);
    }

    private JsonWraper getWraper(String key) {
        if (results == null) {
            return null;
        }
        return results.get(key);
    }

    public String toJson() {
        return GsonUtils.GSON_SHALLOW.toJson(this);
    }

    public static JsonResponse fromJson(String json) {
        return GsonUtils.GSON_SHALLOW.fromJson(json, JsonResponse.class);
    }
}
