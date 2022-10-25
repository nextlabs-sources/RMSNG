package com.nextlabs.rms.shared;

import com.google.gson.Gson;
import com.nextlabs.common.shared.HeartbeatItem;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.nxl.exception.JsonException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WatermarkConfigManager {

    private static Logger logger = LogManager.getLogger(WatermarkConfigManager.class);

    private static final Map<String, JsonWatermark> WATERMARK_CACHE = new HashMap<String, JsonWatermark>();

    public static final String WATERMARK_USERNAME = "$(User)";

    public static final String WATERMARK_LOCALTIME = "$(Time)";

    public static final String WATERMARK_LOCALDATE = "$(Date)";

    public static final String WATERMARK_LINEBREAK = "$(Break)";

    public static final String WATERMARK_HOST = "$(Host)";

    public static final String WATERMARK_GMTTIME = "%gmtTime";

    public static final String WATERMARK_DATE_FORMAT = "WATERMARK_DATE_FORMAT";

    public static final String WATERMARK_TEXT_KEY = "WATERMARK_TEXT_KEY";

    public static final int DEFAULT_PLATFORM_ID = -1;

    private WatermarkConfigManager() {
    }

    public static JsonWatermark getWaterMarkConfig(String rmsURL, String tokenGroupName, String ticket, int platform,
        String userId, String clientId) {
        JsonWatermark watermark = WATERMARK_CACHE.get(tokenGroupName + platform);
        if (watermark == null) {
            synchronized (WATERMARK_CACHE) {
                try {
                    watermark = sendHeartBeat(rmsURL, tokenGroupName, ticket, platform, userId, clientId);
                    WATERMARK_CACHE.put(tokenGroupName + platform, watermark);
                } catch (IOException e) {
                    logger.error("Failed to connect to rms: {}", e.getMessage(), e);
                } catch (JsonException e) {
                    logger.error("Error occurred while getting heartbeat: {}: {}", e.getStatusCode(), e.getMessage(), e);
                }
            }
        }
        return watermark;
    }

    public static String getWaterMarkText(String rmsURL, String tenantName, String ticket, int platform,
        String userId, String clientId) {
        JsonWatermark watermarkConfig = getWaterMarkConfig(rmsURL, tenantName, ticket, platform, userId, clientId);
        return watermarkConfig != null ? watermarkConfig.getText() : "";
    }

    private static JsonWatermark sendHeartBeat(String rmsBaseURL, String tokenGroupName, String ticket, int platform,
        String userId, String clientId) throws IOException, JsonException {
        String path = rmsBaseURL + "/rs/v2/heartbeat";
        JsonWatermark watermark = null;
        HeartbeatItem watermarkItem = new HeartbeatItem("watermarkConfig", "");
        HeartbeatItem[] array = new HeartbeatItem[1];
        array[0] = watermarkItem;
        Properties prop = new Properties();
        prop.setProperty("userId", userId);
        prop.setProperty("ticket", ticket);
        prop.setProperty("clientId", clientId);
        if (platform > 0) {
            prop.setProperty("platformId", "" + platform);
        }
        JsonRequest req = new JsonRequest();
        req.addParameter("tenant", tokenGroupName);
        req.addParameter("objects", array);
        String ret = RestClient.post(path, prop, req.toJson());
        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        if (resp.getStatusCode() == 200) {
            watermark = getConfigFromHeartBeat(resp);
        }
        return watermark;
    }

    public static JsonWatermark getConfigFromHeartBeat(JsonResponse resp) {
        Map<String, JsonWraper> resultAsMap = resp.getResultAsMap("watermarkConfig");
        JsonWraper jsonWraper = resultAsMap.get("content");
        String propertiesString = jsonWraper.getAsJsonTree().getAsString();
        Gson gson = new Gson();
        JsonWatermark watermark = gson.fromJson(propertiesString, JsonWatermark.class);
        watermark.setText(watermark.getText().replaceAll("\\n", "\\" + WATERMARK_LINEBREAK));
        return watermark;
    }

}
