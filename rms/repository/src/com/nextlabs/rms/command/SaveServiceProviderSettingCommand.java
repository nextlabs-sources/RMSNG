package com.nextlabs.rms.command;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class SaveServiceProviderSettingCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String spId = request.getParameter("id");
        ServiceProviderType type = ServiceProviderType.valueOf(request.getParameter("providerType"));

        OperationResult result = new OperationResult();
        Map<String, String> attrs = new HashMap<String, String>();

        boolean validRequest = false;
        if (ServiceProviderType.BOX == type || ServiceProviderType.DROPBOX == type || ServiceProviderType.GOOGLE_DRIVE == type || ServiceProviderType.ONE_DRIVE == type || ServiceProviderType.S3 == type) {
            validRequest = populateOAuthSettings(attrs, request);
        } else if (ServiceProviderType.SHAREPOINT_ONLINE == type) {
            populatePersonalRepoEnabledSetting(attrs, request);
            validRequest = populateOAuthSettings(attrs, request);
        } else if (ServiceProviderType.SHAREPOINT_ONPREMISE == type) {
            populatePersonalRepoEnabledSetting(attrs, request);
            validRequest = true;
        }

        if (!validRequest) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("error_bad_request"));
            return;
        }

        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            RMSUserPrincipal user = authenticate(session, request);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (!user.isAdmin()) {
                response.sendError(403, RMSMessageHandler.getClientString("userNotAdmin"));
                return;
            }
            String tenantId = user.getTenantId();
            StorageProvider sp = null;
            if (spId == null) {
                Criteria criteria = session.createCriteria(StorageProvider.class);
                criteria.add(Restrictions.eq("tenantId", tenantId));
                criteria.add(Restrictions.eq("type", type.ordinal()));
                sp = (StorageProvider)criteria.uniqueResult();
            } else {
                sp = session.get(StorageProvider.class, spId);
                if (sp == null || !sp.getTenantId().equals(tenantId)) {
                    result.setResult(false);
                    result.setMessage(RMSMessageHandler.getClientString("error_service_provider_not_found"));
                    return;
                }
            }

            if (sp == null) {
                sp = new StorageProvider();
                sp.setTenantId(tenantId);
                sp.setType(type.ordinal());
                sp.setCreationTime(new Date());
                sp.setName(ServiceProviderSetting.getProviderTypeDisplayName(type.toString()));
            } else {
                // delete personal repositories belonging to this service provider if required
                Map<String, String> existingAttrs = GsonUtils.GSON.fromJson(sp.getAttributes(), GsonUtils.GENERIC_MAP_TYPE);
                boolean existingAllowed = Boolean.parseBoolean(existingAttrs.get(ServiceProviderSetting.ALLOW_PERSONAL_REPO));
                boolean newAllowed = Boolean.parseBoolean(attrs.get(ServiceProviderSetting.ALLOW_PERSONAL_REPO));
                existingAttrs.keySet().removeAll(attrs.keySet());
                attrs.putAll(existingAttrs);
                if (existingAllowed && !newAllowed) {
                    session.createNamedQuery("deleteUnsharedRepoByProviderId").setParameter("providerId", sp.getId()).executeUpdate();
                }
            }

            sp.setAttributes(GsonUtils.GSON.toJson(attrs));
            session.save(sp);
            session.commit();

            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("success_save_service_provider_setting"));
            JsonUtil.writeJsonToResponse(result, response);
        }
    }

    private boolean populateOAuthSettings(Map<String, String> attrs, HttpServletRequest request) {
        String appId = request.getParameter(ServiceProviderSetting.APP_ID);
        String appSecret = request.getParameter(ServiceProviderSetting.APP_SECRET);
        String redirectUrl = request.getParameter(ServiceProviderSetting.REDIRECT_URL);
        String allowPR = request.getParameter(ServiceProviderSetting.ALLOW_PERSONAL_REPO);
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(redirectUrl)) {
            return false;
        }

        attrs.put(ServiceProviderSetting.APP_ID, appId);
        attrs.put(ServiceProviderSetting.APP_SECRET, appSecret);
        attrs.put(ServiceProviderSetting.REDIRECT_URL, redirectUrl);
        attrs.put(ServiceProviderSetting.ALLOW_PERSONAL_REPO, String.valueOf(Boolean.parseBoolean(allowPR)));
        return true;
    }

    private boolean populatePersonalRepoEnabledSetting(Map<String, String> attrs, HttpServletRequest request) {
        String allowPR = request.getParameter(ServiceProviderSetting.ALLOW_PERSONAL_REPO);
        attrs.put(ServiceProviderSetting.ALLOW_PERSONAL_REPO, String.valueOf(Boolean.valueOf(allowPR)));
        return true;
    }
}
