package com.nextlabs.rms.shared;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JsonUtil {

    private static final Logger LOGGER = LogManager.getLogger(JsonUtil.class);

    private JsonUtil() {

    }

    public static void writeJsonToResponse(Object obj, HttpServletResponse response) {
        final Gson gson = GsonUtils.GSON;
        String jsonStr = gson.toJson(obj);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
        try {
            response.getWriter().write(jsonStr);
        } catch (IOException e) {
            LOGGER.debug("Error occurred while writing response to the stream", e);
        }
    }

    public static void writeJsonToResponse(JsonResponse obj, HttpServletResponse response) {
        String jsonStr = obj.toJson();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
        try {
            response.getWriter().write(jsonStr);
        } catch (IOException e) {
            LOGGER.debug("Error occurred while writing response to the stream", e);
        }
    }

    public static JsonArray getJsonArray(Object arrayObj) {
        Gson gson = GsonUtils.GSON;
        JsonElement element = gson.toJsonTree(arrayObj);
        return element.getAsJsonArray();
    }

    public static JsonObject getJsonObject(Object obj) {
        Gson gson = GsonUtils.GSON;
        JsonElement element = gson.toJsonTree(obj);
        return element.getAsJsonObject();
    }
}
