package com.nextlabs.common.shared;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nextlabs.common.util.GsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonWraper {

    private JsonElement element;

    public JsonWraper(Object obj) {
        if (obj instanceof JsonElement) {
            this.element = (JsonElement)obj;
        } else if (obj instanceof Number) {
            element = new JsonPrimitive((Number)obj);
        } else if (obj instanceof Boolean) {
            element = new JsonPrimitive((Boolean)obj);
        } else if (obj instanceof String) {
            element = new JsonPrimitive((String)obj);
        } else {
            element = GsonUtils.GSON_SHALLOW.toJsonTree(obj);
        }
    }

    public String stringValue() {
        return element.getAsString();
    }

    public boolean booleanValue() {
        return element.getAsBoolean();
    }

    public int intValue() {
        return element.getAsInt();
    }

    public long longValue() {
        return element.getAsLong();
    }

    public double doubleValue() {
        return element.getAsDouble();
    }

    public List<JsonWraper> getAsList() {
        JsonArray arr = element.getAsJsonArray();
        int size = arr.size();
        List<JsonWraper> list = new ArrayList<JsonWraper>(size);
        for (int i = 0; i < size; ++i) {
            list.add(new JsonWraper(arr.get(i)));
        }
        return list;
    }

    public Map<String, JsonWraper> getAsMap() {
        Map<String, JsonWraper> map = new LinkedHashMap<String, JsonWraper>();
        JsonObject json = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), new JsonWraper(entry.getValue()));
        }
        return map;
    }

    public <T> T getAsObject(Class<T> clazz) {
        return GsonUtils.GSON_SHALLOW.fromJson(element, clazz);
    }

    public JsonElement getAsJsonTree() {
        return element;
    }
}
