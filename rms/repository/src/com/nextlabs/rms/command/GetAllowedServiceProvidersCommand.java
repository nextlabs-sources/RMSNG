package com.nextlabs.rms.command;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class GetAllowedServiceProvidersCommand extends AbstractCommand {

    @SuppressWarnings("unchecked")
    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try (DbSession session = DbSession.newSession()) {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UserSession userSession = AuthManager.getUserSession(session, userPrincipal);
            String tenantId = userPrincipal.getTenantId();
            Criteria criteria = session.createCriteria(StorageProvider.class);
            criteria.add(Restrictions.eq("tenantId", tenantId));
            List<StorageProvider> list = criteria.list();

            Map<ServiceProviderType, ServiceProviderSetting> allowedRepos = new HashMap<ServiceProviderType, ServiceProviderSetting>(list.size());
            for (StorageProvider sp : list) {
                ServiceProviderType spType = ServiceProviderType.getByOrdinal(sp.getType());
                if (StringUtils.equalsIgnoreCase(spType.name(), WebConfig.getInstance().getProperty(WebConfig.INBUILT_SERVICE_PROVIDER)) || spType == ServiceProviderType.SHAREPOINT_CROSSLAUNCH || spType == ServiceProviderType.SHAREPOINT_ONLINE_CROSSLAUNCH || RepositoryManager.isRepoHiddenFromUser(userSession, userPrincipal, spType)) {
                    continue;
                } else {
                    boolean personalRepoAllowed = true;
                    if (spType == ServiceProviderType.SHAREPOINT_ONPREMISE || spType == ServiceProviderType.SHAREPOINT_ONLINE) {
                        Map<String, String> attrs = GsonUtils.GSON.fromJson(sp.getAttributes(), GsonUtils.GENERIC_MAP_TYPE);
                        personalRepoAllowed = Boolean.parseBoolean(attrs.get(ServiceProviderSetting.ALLOW_PERSONAL_REPO));
                    }
                    if (!userPrincipal.isAdmin() && !personalRepoAllowed) {
                        continue;
                    }
                    ServiceProviderSetting setting = SettingManager.toServiceProviderSetting(sp);
                    allowedRepos.put(setting.getProviderType(), setting);
                }
            }
            JsonUtil.writeJsonToResponse(allowedRepos, response);
        }
    }
}
