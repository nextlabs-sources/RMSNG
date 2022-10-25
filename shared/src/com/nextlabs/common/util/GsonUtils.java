package com.nextlabs.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nextlabs.common.shared.JsonWraper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class GsonUtils {

    private static final String FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssX";

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setDateFormat(FORMAT_ISO_8601).create();
    public static final Gson GSON_SHALLOW = new GsonBuilder().disableHtmlEscaping().setDateFormat(FORMAT_ISO_8601).registerTypeAdapter(JsonWraper.class, new WraperAdapter()).create();
    public static final Type GENERIC_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    public static final Type STRING_ARRAY_MAP_TYPE = new TypeToken<Map<String, String[]>>() {
    }.getType();
    public static final Type STRING_LIST_MAP_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();
    public static final Type WRAPER_MAP_TYPE = new TypeToken<Map<String, JsonWraper>>() {
    }.getType();
    public static final Type GENERIC_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    public static final Type STRING_ARRAY_TYPE = new TypeToken<String[]>() {
    }.getType();

    public GsonUtils() {
    }

    public static final class WraperAdapter extends TypeAdapter<JsonWraper> {

        public WraperAdapter() {
        }

        @Override
        public void write(JsonWriter writer, JsonWraper wraper) throws IOException {
            if (wraper == null) {
                writer.nullValue();
                return;
            }

            TypeAdapter<JsonElement> adapter = GSON_SHALLOW.getAdapter(JsonElement.class);
            adapter.write(writer, wraper.getAsJsonTree());
        }

        @Override
        public JsonWraper read(JsonReader reader) throws IOException {
            TypeAdapter<JsonElement> adapter = GSON_SHALLOW.getAdapter(JsonElement.class);
            JsonElement element = adapter.read(reader);
            if (element == null) {
                return null;
            }
            return new JsonWraper(element);
        }
    }
}
