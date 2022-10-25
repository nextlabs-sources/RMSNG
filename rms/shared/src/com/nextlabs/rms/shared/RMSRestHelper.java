package com.nextlabs.rms.shared;

import com.google.gson.JsonObject;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RMSRestHelper {

    private static final String RMS_ACTIVITY_LOG_URL_PATH = "/rs/log/v2/activity";
    private static final String RMS_ACTIVITY_LOG_APPLICATION = "RMS";
    private static final Logger LOGGER = LogManager.getLogger(RMSRestHelper.class);

    private RMSRestHelper() {
    }

    public static void sendActivityLogToRMS(String path, String ticket, String duid, String owner, int userId,
        String clientId, Operations operation, String repoId, String filePathId, String filePathDisplay,
        AccessResult accessResult, HttpServletRequest request, Properties prop, String activityData,
        AccountType logType) {
        sendActivityLogToRMS(path, ticket, duid, owner, userId, clientId, null, DeviceType.WEB.getLow(), operation, repoId, filePathId, filePathDisplay, accessResult, request, prop, activityData, logType);
    }

    public static void sendActivityLogToRMS(String path, String ticket, String duid, String owner, int userId,
        String clientId, String deviceId, Integer platformId, Operations operation, String repoId, String filePathId,
        String filePathDisplay, AccessResult accessResult, HttpServletRequest request, Properties prop,
        String activityData, AccountType logType) {
        CloseableHttpResponse httpResponse = null;
        CloseableHttpClient client = null;
        try {
            client = HTTPUtil.getHTTPClient();
            URL url = new URL(path + RMS_ACTIVITY_LOG_URL_PATH);
            String accessTime = String.valueOf(System.currentTimeMillis());
            HttpPut putRequest = new HttpPut(url.toString());
            if (prop != null) {
                for (String key : prop.stringPropertyNames()) {
                    putRequest.setHeader(key, prop.getProperty(key));
                }
            }
            if (activityData == null) {
                activityData = new JsonObject().toString();
            }
            deviceId = StringUtils.hasText(deviceId) ? deviceId : HTTPUtil.getRemoteAddress(request);
            platformId = platformId != null && platformId >= 0 ? platformId : DeviceType.WEB.getLow();

            putRequest.addHeader("Content-Type", "text/csv");
            putRequest.addHeader("Content-Encoding", "gzip");
            putRequest.addHeader("userId", String.valueOf(userId));
            putRequest.addHeader("ticket", ticket);
            putRequest.addHeader("clientId", clientId);
            putRequest.addHeader("platformId", String.valueOf(platformId));

            String[] fields = new String[17];
            fields[0] = StringEscapeUtils.escapeCsv(duid);
            fields[1] = StringEscapeUtils.escapeCsv(owner);
            fields[2] = StringEscapeUtils.escapeCsv(String.valueOf(userId));
            fields[3] = StringEscapeUtils.escapeCsv(String.valueOf(operation.getValue()));
            fields[4] = StringEscapeUtils.escapeCsv(deviceId);
            fields[5] = StringEscapeUtils.escapeCsv(String.valueOf(platformId));
            fields[6] = StringEscapeUtils.escapeCsv(repoId);
            fields[7] = StringEscapeUtils.escapeCsv(filePathId);
            if (filePathDisplay != null && !filePathDisplay.isEmpty()) {
                fields[8] = StringEscapeUtils.escapeCsv(filePathDisplay.substring(filePathDisplay.lastIndexOf('/') + 1));
            }
            fields[9] = StringEscapeUtils.escapeCsv(filePathDisplay);
            fields[10] = StringEscapeUtils.escapeCsv(RMS_ACTIVITY_LOG_APPLICATION);
            fields[13] = StringEscapeUtils.escapeCsv(String.valueOf(accessResult.ordinal()));
            fields[14] = StringEscapeUtils.escapeCsv(accessTime);
            fields[15] = StringEscapeUtils.escapeCsv(activityData);
            fields[16] = StringEscapeUtils.escapeCsv(String.valueOf(logType.ordinal()));

            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            PrintStream ps = new PrintStream(gzip, false, "UTF-8");
            ps.append(fields[0]);
            for (int i = 1; i < fields.length; ++i) {
                ps.append(',');
                if (fields[i] != null) {
                    ps.append(fields[i]);
                }
            }
            ps.println();
            ps.close();
            gzip.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(obj.toByteArray());
            InputStreamEntity reqEntity = new InputStreamEntity(bais, -1);
            putRequest.setEntity(reqEntity);
            httpResponse = client.execute(putRequest);
        } catch (IOException e) {
            LOGGER.error("Error While adding activity log", e);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error While adding activity log", e);
        } finally {
            IOUtils.closeQuietly(httpResponse);
            IOUtils.closeQuietly(client);
        }
    }

}
