package com.nextlabs.rms.command;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SaveConfigurationCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!userPrincipal.isAdmin()) {
                response.sendError(403, RMSMessageHandler.getClientString("userNotAdmin"));
                return;
            }

            // TODO: Add more per tenant configurations
            String currentVersion = request.getParameter(Constants.RMC_CURRENT_VERSION);
            String currentMacVersion = request.getParameter(Constants.RMC_MAC_CURRENT_VERSION);
            String downloadURL32Bit = request.getParameter(Constants.RMC_UPDATE_URL_32BITS);
            String crcChecksum32Bit = request.getParameter(Constants.RMC_CRC_CHECKSUM_32BITS);
            String sha1Checksum32Bit = request.getParameter(Constants.RMC_SHA1_CHECKSUM_32BITS);
            String downloadURL64Bit = request.getParameter(Constants.RMC_UPDATE_URL_64BITS);
            String crcChecksum64Bit = request.getParameter(Constants.RMC_CRC_CHECKSUM_64BITS);
            String sha1Chceksum64Bit = request.getParameter(Constants.RMC_SHA1_CHECKSUM_64BITS);
            String crcChecksumMac = request.getParameter(Constants.RMC_CRC_CHECKSUM_MAC);
            String sha1ChecksumMac = request.getParameter(Constants.RMC_SHA1_CHECKSUM_MAC);
            String forceDowngrade = request.getParameter(Constants.RMC_FORCE_DOWNGRADE);
            String rmdWin32Url = request.getParameter(Constants.RMD_WIN_32_DOWNLOAD_URL);
            String rmdWin64Url = request.getParameter(Constants.RMD_WIN_64_DOWNLOAD_URL);
            String rmdMacUrl = request.getParameter(Constants.RMD_MAC_DOWNLOAD_URL);
            String rmcIOSUrl = request.getParameter(Constants.RMC_IOS_DOWNLOAD_URL);
            String rmcAndroidUrl = request.getParameter(Constants.RMC_ANDROID_DOWNLOAD_URL);
            String clientHeartBeatFrequency = request.getParameter(Constants.CLIENT_HEARTBEAT_FREQUENCY);
            if (!validateParams(clientHeartBeatFrequency)) {
                response.sendError(400, RMSMessageHandler.getClientString("error_invalid_config"));
                return;
            }

            OperationResult result = new OperationResult();

            if (userPrincipal.getTenantId().equals(userPrincipal.getLoginTenant())) {
                Tenant tenant = session.get(Tenant.class, userPrincipal.getTenantId());
                String preferences = tenant.getPreference();
                Map<String, Object> settings = GsonUtils.GSON.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);
                settings.put(Constants.RMC_CURRENT_VERSION, currentVersion);
                settings.put(Constants.RMC_UPDATE_URL_32BITS, downloadURL32Bit);
                settings.put(Constants.RMC_CRC_CHECKSUM_32BITS, crcChecksum32Bit);
                settings.put(Constants.RMC_SHA1_CHECKSUM_32BITS, sha1Checksum32Bit);
                settings.put(Constants.RMC_UPDATE_URL_64BITS, downloadURL64Bit);
                settings.put(Constants.RMC_CRC_CHECKSUM_64BITS, crcChecksum64Bit);
                settings.put(Constants.RMC_SHA1_CHECKSUM_64BITS, sha1Chceksum64Bit);
                settings.put(Constants.RMC_MAC_CURRENT_VERSION, currentMacVersion);
                settings.put(Constants.RMC_CRC_CHECKSUM_MAC, crcChecksumMac);
                settings.put(Constants.RMC_SHA1_CHECKSUM_MAC, sha1ChecksumMac);
                settings.put(Constants.RMC_FORCE_DOWNGRADE, Boolean.parseBoolean(forceDowngrade));
                settings.put(Constants.RMD_WIN_32_DOWNLOAD_URL, rmdWin32Url);
                settings.put(Constants.RMD_WIN_64_DOWNLOAD_URL, rmdWin64Url);
                settings.put(Constants.RMD_MAC_DOWNLOAD_URL, rmdMacUrl);
                settings.put(Constants.RMC_IOS_DOWNLOAD_URL, rmcIOSUrl);
                settings.put(Constants.RMC_ANDROID_DOWNLOAD_URL, rmcAndroidUrl);
                settings.put(Constants.CLIENT_HEARTBEAT_FREQUENCY, clientHeartBeatFrequency);
                tenant.setPreference(GsonUtils.GSON.toJson(settings));
            } else {
                Tenant loginTenant = session.get(Tenant.class, userPrincipal.getLoginTenant());
                String tenantPreferences = loginTenant.getPreference();
                Map<String, Object> tenantSettings = GsonUtils.GSON.fromJson(tenantPreferences, GsonUtils.GENERIC_MAP_TYPE);
                tenantSettings.put(Constants.CLIENT_HEARTBEAT_FREQUENCY, clientHeartBeatFrequency);
                loginTenant.setPreference(GsonUtils.GSON.toJson(tenantSettings));
            }
            session.commit();
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("settings.saved.successfully"));
            JsonUtil.writeJsonToResponse(result, response);
        } finally {
            session.close();
        }
    }

    private boolean validateParams(String clientHeartBeatFrequency) {

        int chbFreq;
        try {
            chbFreq = Integer.parseInt(clientHeartBeatFrequency);
        } catch (NumberFormatException e) {
            return false;
        }
        return chbFreq >= 0;
    }
}
