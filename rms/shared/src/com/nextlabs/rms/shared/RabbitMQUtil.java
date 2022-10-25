package com.nextlabs.rms.shared;

import com.google.gson.JsonObject;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RabbitMQUtil {

    public static final String MYDRIVE_UPLOAD_EXCHANGE_NAME = "mydrive-upload";
    public static final String MYVAULT_UPLOAD_EXCHANGE_NAME = "myvault-upload";
    public static final String PROJECT_UPLOAD_EXCHANGE_NAME = "project-upload";
    public static final String ENTERPRISE_SPACE_UPLOAD_EXCHANGE_NAME = "enterprisews-upload";
    public static final String API_BASE = "http://" + WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_API_URL) + ":" + WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_API_PORT) + "/api/";
    private static final String ENDPOINT_USERS = "users/";
    private static final String ENDPOINT_PERMISSION = "permissions/%2F/";
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static ConnectionFactory factory;

    private RabbitMQUtil() {

    }

    public static String generateRoutingKey(int userId) {
        return userId + "-" + UUID.randomUUID().toString();
    }

    public static void sendDirectMessage(String routingKey, String exchangeName, String message) {
        factory = getFactory();
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(exchangeName, "direct");
            channel.basicPublish(exchangeName, routingKey, null, message.getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException | TimeoutException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static synchronized ConnectionFactory getFactory() {
        if (factory == null) {
            factory = new ConnectionFactory();
            factory.setHost(WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_MQ_URL));
            if (StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_MQ_PORT))) {
                factory.setPort(Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_MQ_PORT)));
            }
            factory.setUsername(WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_USER));
            factory.setPassword(WebConfig.getInstance().getProperty(WebConfig.RABBITMQ_PASSWORD));
        }
        return factory;
    }

    public static boolean checkUserExists(String username) throws GeneralSecurityException, IOException {
        StringBuilder urlBuilder = new StringBuilder(API_BASE);
        urlBuilder.append(ENDPOINT_USERS).append(username);
        URL url = new URL(urlBuilder.toString());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("URL to get rabbitmq user info", url);
        }
        HttpGet getRequest = new HttpGet(url.toString());
        String auth = "guest" + ":" + "guest";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.ISO_8859_1);
        getRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        try (CloseableHttpClient client = HTTPUtil.getHTTPClient();
                CloseableHttpResponse getResponse = client.execute(getRequest)) {
            return getResponse.getStatusLine().getStatusCode() == 200;
        }
    }

    public static boolean addAUser(String username, String password) throws GeneralSecurityException, IOException {
        StringBuilder urlBuilder = new StringBuilder(API_BASE);
        urlBuilder.append(ENDPOINT_USERS).append(username);
        URL url = new URL(urlBuilder.toString());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("URL to set permission", url);
        }
        HttpPut putRequest = new HttpPut(url.toString());
        String auth = "guest" + ":" + "guest";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.ISO_8859_1);
        putRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        JsonObject object = new JsonObject();
        object.addProperty("password", password);
        object.addProperty("tags", "administrator");
        String json = object.toString();
        putRequest.addHeader("content-type", "application/json");
        putRequest.setEntity(new StringEntity(json, "UTF-8"));
        try (CloseableHttpClient client = HTTPUtil.getHTTPClient();
                CloseableHttpResponse getResponse = client.execute(putRequest)) {
            return getResponse.getStatusLine().getStatusCode() == 201 && setPermission(username);
        }
    }

    private static boolean setPermission(String username) throws GeneralSecurityException, IOException {
        StringBuilder urlBuilder = new StringBuilder(API_BASE);
        urlBuilder.append(ENDPOINT_PERMISSION).append(username);
        URL url = new URL(urlBuilder.toString());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("URL to create a rabbitmq user", url);
        }
        HttpPut putRequest = new HttpPut(url.toString());
        String auth = "guest" + ":" + "guest";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.ISO_8859_1);
        putRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        JsonObject object = new JsonObject();
        object.addProperty("configure", ".*");
        object.addProperty("write", ".*");
        object.addProperty("read", ".*");
        String json = object.toString();
        putRequest.addHeader("content-type", "application/json");
        putRequest.setEntity(new StringEntity(json, "UTF-8"));
        try (CloseableHttpClient client = HTTPUtil.getHTTPClient();
                CloseableHttpResponse getResponse = client.execute(putRequest)) {
            return getResponse.getStatusLine().getStatusCode() == 201;
        }
    }
}
