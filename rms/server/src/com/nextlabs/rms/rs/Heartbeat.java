package com.nextlabs.rms.rs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.HeartbeatItem;
import com.nextlabs.common.shared.JsonClassificationCategory;
import com.nextlabs.common.shared.JsonHeartbeatData;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.CRC32;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("")
public class Heartbeat {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final LoadingCache<ItemKey, HeartbeatItem> CACHE = CacheBuilder.newBuilder().maximumSize(10).build(new CacheLoader<ItemKey, HeartbeatItem>() {

        public HeartbeatItem load(ItemKey key) throws IOException {
            HeartbeatItem item = new HeartbeatItem();
            WebConfig config = WebConfig.getInstance();
            File configDir = config.getConfigDir();
            int platformId = key.getPlatformId();
            String fileName = "tenants/" + key.getTokenGroupName() + '/' + key.getName();
            File file = new File(configDir, fileName + '.' + platformId);
            if (!file.exists() || !file.isFile()) {
                file = new File(configDir, fileName);
            }
            if (file.exists() && file.isFile()) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(file);
                    byte[] buf = IOUtils.readFully(is);
                    CRC32 crc32 = new CRC32();
                    crc32.update(buf);
                    long checksum = crc32.getValue();

                    item.setContent(new String(buf, StandardCharsets.UTF_8.name()));
                    item.setSerialNumber(Long.toHexString(checksum));
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            return item;
        }
    });

    private static final String POLICY_CONFIG_DATA = "policyConfigData";
    private static final String TOKEN_GROUP_RESOURCE_TYPE_MAPPING = "tokenGroupResourceTypeMapping";

    private static final String HEARTBEAT_FREQUENCY = "heartbeatFrequency";
    private static final String CLIENT_HEARTBEAT_FREQUENCY = "CLIENT_HEARTBEAT_FREQUENCY";
    public static final String WATERMARK_CONFIG = "watermarkConfig";
    private static final Type CLIENTDATA_SET_TYPE = new TypeToken<Set<JsonHeartbeatData>>() {
    }.getType();

    @POST
    @Path("/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String heartbeat(@Context HttpServletRequest request, String json) {
        boolean error = true;
        int userId = -1;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            userId = req.getIntParameter("userId", -1);
            String ticket = req.getParameter("ticket");
            int platformId = req.getIntParameter("platformId", 0);
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                return new JsonResponse(401, "Missing Login parameters").toJson();
            }
            Tenant tenant = null;
            try (DbSession session = DbSession.newSession()) {
                session.beginTransaction();
                UserSession us = UserMgmt.authenticate(session, userId, ticket, null, platformId);
                if (us == null) {
                    return new JsonResponse(401, "Authentication failed").toJson();
                }
                tenant = AbstractLogin.getDefaultTenant();
                if (tenant == null) {
                    return new JsonResponse(400, "Invalid tenant").toJson();
                }
            }

            String tenantName = tenant.getName();

            JsonResponse resp = new JsonResponse("OK");
            List<JsonWraper> list = req.getParameterAsList("objects");
            for (JsonWraper wraper : list) {
                HeartbeatItem item = wraper.getAsObject(HeartbeatItem.class);
                String name = item.getName();
                String serialNumber = item.getSerialNumber();
                ItemKey key = new ItemKey(tenantName, name, platformId);
                HeartbeatItem cachedItem = CACHE.get(key);
                if (cachedItem.hasNewContent(serialNumber)) {
                    resp.putResult(name, cachedItem);
                }
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "Heartbeat", "heartbeat", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/v2/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String heartbeatV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            if (platformId == null) {
                platformId = req.getIntParameter("platformId", -1);
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Set<JsonHeartbeatData> clientDataSet = req.getParameter("clientData", CLIENTDATA_SET_TYPE);
            if (clientDataSet == null) {
                clientDataSet = new HashSet<>();
            }
            Map<String, JsonHeartbeatData> clientDataMap = new HashMap<>();
            for (JsonHeartbeatData clientData : clientDataSet) {
                if (StringUtils.hasText(clientData.getTokenGroupName())) {
                    clientDataMap.put(clientData.getTokenGroupName(), clientData);
                }
            }

            try (DbSession session = DbSession.newSession()) {

                Tenant defaultTenant = AbstractLogin.getDefaultTenant();
                if (defaultTenant == null) {
                    return new JsonResponse(400, "Invalid tenant").toJson();
                }

                String defaultTenantName = defaultTenant.getName();
                JsonResponse resp = new JsonResponse("OK");

                Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
                String preferences = loginTenant.getPreference();
                Map<String, JsonWraper> preferencesMap = GsonUtils.GSON_SHALLOW.fromJson(preferences, GsonUtils.WRAPER_MAP_TYPE);
                JsonWraper clientHeartbeatFrequency = preferencesMap.get(CLIENT_HEARTBEAT_FREQUENCY);
                resp.putResult(HEARTBEAT_FREQUENCY, clientHeartbeatFrequency == null ? Integer.parseInt(com.nextlabs.rms.config.Constants.DEFAULT_CLIENT_HEARTBEAT_FREQUENCY) : clientHeartbeatFrequency.intValue());

                if (EvaluationAdapterFactory.isInitialized() && !(platformId >= DeviceType.WINDOWS_PHONE.getLow() && platformId <= DeviceType.ANDROID_TABLET.getHigh())) {
                    List<JsonHeartbeatData> policyConfigDataList = new ArrayList<>();
                    List<JsonMembership> memberships = UserMgmt.getMemberships(session, userId, null, us);
                    Set<String> seenTokenGroupNames = new HashSet<>();
                    for (JsonMembership membership : memberships) {

                        String memberTokenGroupName = StringUtils.substringAfter(membership.getId(), "@");
                        if (seenTokenGroupNames.contains(memberTokenGroupName)) {
                            continue;
                        } else {
                            seenTokenGroupNames.add(memberTokenGroupName);
                        }

                        long clientConfigurationModifiedTimeStamp = 0L;
                        boolean foundTokenGroupMember = false;

                        if (clientDataMap.containsKey(memberTokenGroupName)) {
                            foundTokenGroupMember = true;
                            if (clientDataMap.get(memberTokenGroupName).getConfigurationModifiedTimeStamp() != null) {
                                clientConfigurationModifiedTimeStamp = clientDataMap.get(memberTokenGroupName).getConfigurationModifiedTimeStamp();
                            }
                        }

                        long tgConfigurationLastModified;
                        List<JsonClassificationCategory> classificationCategories;
                        HeartbeatItem heartbeatItem;
                        switch (TokenGroupType.values()[membership.getType()]) {
                            case TOKENGROUP_SYSTEMBUCKET:
                                Tenant sbTenant = new SystemBucketManagerImpl().getParentTenant(memberTokenGroupName, session);
                                tgConfigurationLastModified = sbTenant.getConfigurationModified().getTime();
                                classificationCategories = ClassificationMgmt.getTenantClassification(session, sbTenant.getName());
                                heartbeatItem = CACHE.get(new ItemKey(sbTenant.getName(), WATERMARK_CONFIG, platformId));
                                break;
                            case TOKENGROUP_PROJECT:
                                Project tgProject = session.get(Project.class, membership.getProjectId());
                                tgConfigurationLastModified = tgProject.getConfigurationModified().getTime();
                                classificationCategories = ClassificationMgmt.getProjectClassification(session, memberTokenGroupName);
                                heartbeatItem = new HeartbeatItem("watermarkConfig", "");
                                heartbeatItem.setContent(tgProject.getWatermark());
                                break;
                            case TOKENGROUP_TENANT:
                                Tenant tgTenant = session.get(Tenant.class, membership.getTenantId());
                                tgConfigurationLastModified = tgTenant.getConfigurationModified().getTime();
                                classificationCategories = ClassificationMgmt.getTenantClassification(session, tgTenant.getName());
                                heartbeatItem = CACHE.get(new ItemKey(memberTokenGroupName, WATERMARK_CONFIG, platformId));
                                break;
                            default:
                                return new JsonResponse(500, "Inconsistent token group type detected in server").toJson();
                        }
                        JsonHeartbeatData configDataElement = null;
                        boolean addConfigData = false;

                        if (!foundTokenGroupMember || tgConfigurationLastModified > clientConfigurationModifiedTimeStamp) {
                            addConfigData = true;
                            configDataElement = new JsonHeartbeatData();
                            configDataElement.setConfigurationModifiedTimeStamp(tgConfigurationLastModified);
                            configDataElement.setWatermarkConfig(heartbeatItem);
                            configDataElement.setClassificationCategories(classificationCategories);
                        }
                        if (addConfigData) {
                            configDataElement.setTokenGroupName(memberTokenGroupName);
                            policyConfigDataList.add(configDataElement);
                        }
                    }
                    resp.putResult(POLICY_CONFIG_DATA, policyConfigDataList);
                }

                // For default tenant watermarkConfig
                ItemKey itemKey = new ItemKey(defaultTenantName, WATERMARK_CONFIG, platformId);
                HeartbeatItem heartbeatItem = CACHE.get(itemKey);
                resp.putResult(WATERMARK_CONFIG, heartbeatItem);
                Map<String, String> flattenedTokenGroupMap = new HashMap<>();
                TokenGroupManager.getTokenGroupResourceTypeMappings().forEach(map -> flattenedTokenGroupMap.put(map.get("tokenGroupName"), map.get("keystoreId")));
                resp.putResult(TOKEN_GROUP_RESOURCE_TYPE_MAPPING, flattenedTokenGroupMap);

                error = false;
                return resp.toJson();
            }
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "Heartbeat", "heartbeat", error ? 0 : 1, userId);
        }
    }

    public static JsonResponse getWaterMarkHeartBeatItem(String tenantName, Integer platformId)
            throws ExecutionException {
        JsonResponse resp = new JsonResponse();
        ItemKey itemKey = new ItemKey(tenantName, WATERMARK_CONFIG, platformId);
        HeartbeatItem heartbeatItem = CACHE.get(itemKey);
        resp.putResult(WATERMARK_CONFIG, heartbeatItem);
        return resp;
    }

    private static final class ItemKey {

        private final String tokenGroupName;
        private final String name;
        private final int platformId;

        public ItemKey(String tokenGroupName, String name, int platformId) {
            this.tokenGroupName = tokenGroupName;
            this.name = name;
            this.platformId = platformId;
        }

        private String getTokenGroupName() {
            return tokenGroupName;
        }

        private String getName() {
            return name;
        }

        private int getPlatformId() {
            return platformId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Heartbeat.ItemKey)) {
                return false;
            }
            return hashCode() == object.hashCode();
        }

        @Override
        public int hashCode() {
            return tokenGroupName.hashCode() + name.hashCode() + platformId;
        }
    }
}
