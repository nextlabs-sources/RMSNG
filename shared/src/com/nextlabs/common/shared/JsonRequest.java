package com.nextlabs.common.shared;

import com.google.gson.JsonElement;
import com.nextlabs.common.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class JsonRequest {

    private HashMap<String, JsonWraper> parameters;

    private Client client;
    private Crash crash;

    public JsonRequest() {
    }

    public String getParameter(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }
        return wraper.stringValue();
    }

    public int getIntParameter(String key, int def) {
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

    public long getLongParameter(String key, long def) {
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

    public <T> T getParameter(String key, Class<T> cls) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        JsonElement element = wraper.getAsJsonTree();
        return GsonUtils.GSON_SHALLOW.fromJson(element, cls);
    }

    public <T> T getParameter(String key, Type type) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        JsonElement element = wraper.getAsJsonTree();
        return GsonUtils.GSON_SHALLOW.fromJson(element, type);
    }

    public List<JsonWraper> getParameterAsList(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return Collections.emptyList();
        }

        return wraper.getAsList();
    }

    public JsonElement getWrappedParameter(String key) {
        JsonWraper wraper = getWraper(key);
        if (wraper == null) {
            return null;
        }

        return wraper.getAsJsonTree();
    }

    public void addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<String, JsonWraper>();
        }
        parameters.put(key, new JsonWraper(value));
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public void setCrash(Crash crash) {
        this.crash = crash;
    }

    public Crash getCrash() {
        return crash;
    }

    public String toJson() {
        return GsonUtils.GSON_SHALLOW.toJson(this);
    }

    private JsonWraper getWraper(String key) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(key);
    }

    public static JsonRequest fromJson(String json) {
        return GsonUtils.GSON_SHALLOW.fromJson(json, JsonRequest.class);
    }
}
