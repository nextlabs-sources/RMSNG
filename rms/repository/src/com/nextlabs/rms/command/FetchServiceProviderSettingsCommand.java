package com.nextlabs.rms.command;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.ServiceProviderSettings;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author nnallagatla
 */
public class FetchServiceProviderSettingsCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        DbSession session = DbSession.newSession();
        try {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!userPrincipal.isAdmin()) {
                response.sendError(403, RMSMessageHandler.getClientString("userNotAdmin"));
                return;
            }

            List<String> supportedProviders = new ArrayList<>();
            for (String provider : RepositoryFactory.ALLOWED_REPOSITORIES) {
                if (!StringUtils.equalsIgnoreCase(provider, WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER)) && !StringUtils.equalsIgnoreCase(provider, ServiceProviderType.ONEDRIVE_FORBUSINESS.name()) && !StringUtils.equalsIgnoreCase(provider, ServiceProviderType.S3.name())) { //hiding OD4B and S3 repositories in ServiceProvider settings
                    supportedProviders.add(provider);
                }
            }
            String redirectUrl = HTTPUtil.getURI(request);
            Map<String, String> supportedProvidersMap = new HashMap<String, String>();
            populateTypeDisplayNames(supportedProvidersMap, supportedProviders);

            List<ServiceProviderSetting> serviceProviderSettings = new ArrayList<>();
            for (ServiceProviderSetting serviceProviderSetting : SettingManager.getStorageProviderSettings(session, userPrincipal.getTenantId())) {
                if (!StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER)) && !StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), ServiceProviderType.ONEDRIVE_FORBUSINESS.name()) && !StringUtils.equalsIgnoreCase(serviceProviderSetting.getProviderType().name(), ServiceProviderType.S3.name())) {
                    serviceProviderSettings.add(serviceProviderSetting);
                }
            }

            JsonUtil.writeJsonToResponse(new ServiceProviderSettings(serviceProviderSettings, supportedProvidersMap, redirectUrl), response);
        } finally {
            session.close();
        }
    }

    private void populateTypeDisplayNames(Map<String, String> typetoNameMap, List<String> supportedProviders) {
        for (String type : supportedProviders) {
            typetoNameMap.put(type, RMSMessageHandler.getClientString(type + "_display_name"));
        }
    }
}
