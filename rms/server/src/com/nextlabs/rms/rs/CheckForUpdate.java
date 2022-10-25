package com.nextlabs.rms.rs;

import com.google.gson.JsonParseException;
import com.googlecode.flyway.core.util.StringUtils;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Client;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/upgrade")
public class CheckForUpdate {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String RMC_CURRENT_VERSION = "RMC_CURRENT_VERSION";
    public static final String RMC_FORCE_DOWNGRADE = "RMC_FORCE_DOWNGRADE";
    public static final String RMC_UPDATE_URL_32BITS = "RMC_UPDATE_URL_32BITS";
    public static final String RMC_SHA1_CHECKSUM_32BITS = "RMC_SHA1_CHECKSUM_32BITS";
    public static final String RMC_UPDATE_URL_64BITS = "RMC_UPDATE_URL_64BITS";
    public static final String RMC_SHA1_CHECKSUM_64BITS = "RMC_SHA1_CHECKSUM_64BITS";

    public static final String RMC_MAC_CURRENT_VERSION = "RMC_MAC_CURRENT_VERSION";
    public static final String RMD_MAC_DOWNLOAD_URL = "RMD_MAC_DOWNLOAD_URL";
    public static final String RMC_SHA1_CHECKSUM_MAC = "RMC_SHA1_CHECKSUM_MAC";

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String checkForUpdate(@Context HttpServletRequest request, String json) {
        DbSession session = DbSession.newSession();
        Tenant tenant = null;
        boolean error = false;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                error = true;
                return new JsonResponse(400, "Missing request").toJson();
            }

            String arch = req.getParameter("processorArch");
            int platformId = req.getIntParameter("platformId", -1);
            String version = req.getParameter("currentVersion");
            String deviceId = req.getParameter("deviceId");
            String appName = req.getParameter("appName");
            if (!StringUtils.hasText(version)) {
                error = true;
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            tenant = AbstractLogin.getTenantFromUrl(request);
            if (tenant == null) {
                error = true;
                return new JsonResponse(400, "Invalid tenant").toJson();
            }

            session.beginTransaction();

            String clientId = request.getHeader("clientId");
            if (StringUtils.hasText(clientId)) {
                Client client = session.get(Client.class, clientId);
                if (client == null) {
                    Date now = new Date();
                    client = new Client();
                    client.setClientId(clientId);
                    client.setDeviceId(deviceId == null ? clientId : deviceId);
                    client.setDeviceType(platformId);
                    client.setModel(arch);
                    client.setAppName(appName == null ? "RMC" : appName);
                    client.setAppVersion(version);
                    client.setCreationDate(now);
                    client.setLastModified(now);
                    session.save(client);
                } else if (!version.equals(client.getAppVersion())) {
                    client.setAppVersion(version);
                    client.setLastModified(new Date());
                }
            }

            session.commit();

            String preferences = tenant.getPreference();
            if (StringUtils.hasText(preferences)) {
                Map<String, Object> map = GsonUtils.GSON_SHALLOW.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);
                String url = null;
                String checksum = null;
                if (platformId >= DeviceType.MAC_OS.getLow() && platformId <= DeviceType.MAC_OS.getHigh()) {
                    url = (String)map.get(RMD_MAC_DOWNLOAD_URL);
                    checksum = (String)map.get(RMC_SHA1_CHECKSUM_MAC);
                    String newVersion = (String)map.get(RMC_MAC_CURRENT_VERSION);
                    if (StringUtils.hasText(newVersion) && isNewer(newVersion, version)) {
                        if (!StringUtils.hasText(url)) {
                            LOGGER.error("Missing url in check for update config: {}", tenant.getName());
                        } else {
                            JsonResponse resp = new JsonResponse("OK");
                            resp.putResult("newVersion", newVersion);
                            resp.putResult("downloadURL", url);
                            resp.putResult("sha1Checksum", checksum);
                            return resp.toJson();
                        }
                    }
                } else if (platformId >= DeviceType.WINDOWS_DESKTOP.getLow() && platformId <= DeviceType.WINDOWS_SERVER.getHigh()) {
                    Boolean force = (Boolean)map.get(RMC_FORCE_DOWNGRADE);
                    String newVersion = (String)map.get(RMC_CURRENT_VERSION);
                    if ("x86".equals(arch)) {
                        url = (String)map.get(RMC_UPDATE_URL_32BITS);
                        checksum = (String)map.get(RMC_SHA1_CHECKSUM_32BITS);
                    } else {
                        url = (String)map.get(RMC_UPDATE_URL_64BITS);
                        checksum = (String)map.get(RMC_SHA1_CHECKSUM_64BITS);
                    }
                    if (StringUtils.hasText(newVersion) && (force != null && force && !version.equals(newVersion) || isNewer(newVersion, version))) {
                        if (!StringUtils.hasText(url) || !StringUtils.hasText(checksum)) {
                            LOGGER.error("Missing url or checksum in check for update config: {}", tenant.getName());
                        } else {
                            JsonResponse resp = new JsonResponse("OK");
                            resp.putResult("newVersion", newVersion);
                            resp.putResult("downloadURL", url);
                            resp.putResult("sha1Checksum", checksum);
                            return resp.toJson();
                        }
                    }
                }
            }
            JsonResponse resp = new JsonResponse(304, "Not Modified");
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            error = true;
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            error = true;
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "CheckForUpdate", "checkForUpdate", error ? 0 : 1, tenant != null ? tenant.getName() : null);
            session.close();
        }
    }

    private static boolean isNewer(String targetVersion, String currentVersion) {
        String[] targetParts = targetVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");
        int length = Math.max(targetParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int target = i < targetParts.length ? Integer.parseInt(targetParts[i]) : 0;
            int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (target < current) {
                return false;
            }
            if (target > current) {
                return true;
            }
        }
        return false;
    }

    public static final class Update {

        private String newVersion;
        private String downloadURL;
        private String sha1Checksum;

        public Update() {
        }

        public void setNewVersion(String newVersion) {
            this.newVersion = newVersion;
        }

        public String getNewVersion() {
            return newVersion;
        }

        public void setDownloadURL(String downloadURL) {
            this.downloadURL = downloadURL;
        }

        public String getDownloadURL() {
            return downloadURL;
        }

        public void setSha1Checksum(String sha1Checksum) {
            this.sha1Checksum = sha1Checksum;
        }

        public String getSha1Checksum() {
            return sha1Checksum;
        }
    }
}
