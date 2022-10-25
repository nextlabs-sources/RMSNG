package com.nextlabs.rms.repository.sharepoint.online;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.sharepoint.SharePointRepoAuthHelper;
import com.nextlabs.rms.repository.sharepoint.response.UserProfile;
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class SharePointOnlineOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private SharePointOnlineOAuthHandler() {

    }

    public static String startAuthRequest(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal userPrincipal) throws IOException {

        String name = request.getParameter("name");
        String siteName = request.getParameter("siteName");
        String isShared = request.getParameter("isShared");
        String repoId = request.getParameter("repoId");
        String redirectCode = request.getParameter("redirectCode");
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);

        if (!siteName.endsWith("/")) {
            StringBuilder siteNameBuilder = new StringBuilder(siteName);
            siteName = siteNameBuilder.append('/').toString();
        }
        DbSession session = DbSession.newSession();
        try {
            ServiceProviderSetting storageProviderSettings = SettingManager.getStorageProviderSettings(session, userPrincipal.getTenantId(), ServiceProviderType.SHAREPOINT_ONLINE);
            String accountIdFrom = "";
            Criteria criteria = session.createCriteria(Repository.class);
            criteria.add(Restrictions.eq("userId", userPrincipal.getUserId()));
            criteria.add(Restrictions.eq("name", name));
            Repository repo = (Repository)criteria.uniqueResult();
            if (repo != null) {
                accountIdFrom = repo.getAccountId();
            }

            Map<String, String> attributesMap = storageProviderSettings.getAttributes();
            String appId = attributesMap.get(ServiceProviderSetting.APP_ID);
            StringBuilder uriBuilder = new StringBuilder(attributesMap.get(ServiceProviderSetting.REDIRECT_URL));

            uriBuilder.append('/').append(RepoConstants.SHAREPOINT_ONLINE_AUTH_FINISH_URL);
            String serverName = siteName.toLowerCase();
            if (serverName.startsWith("https://")) {
                serverName = serverName.substring(8);
            }
            String redirectUri = uriBuilder.toString();
            StringBuilder encodedURLBuilder = new StringBuilder(redirectUri);
            encodedURLBuilder.append("?clientID=").append(appId).append("&siteName=").append(serverName).append("&name=").append(name).append("&accountId=").append(accountIdFrom).append("&repoType=").append(ServiceProviderType.SHAREPOINT_ONLINE.name()).append("&isShared=").append(isShared);
            switch (authorizeType) {
                case AUTHORIZE_TYPE_CUSTOM:
                    encodedURLBuilder.append("&custom=true");
                    break;
                case AUTHORIZE_TYPE_JSON:
                    encodedURLBuilder.append("&json=true");
                    break;
                case AUTHORIZE_TYPE_WEB:
                default:
                    break;
            }
            encodedURLBuilder.append("&u=").append(userPrincipal.getUserId());
            encodedURLBuilder.append("&t=").append(userPrincipal.getTicket());
            encodedURLBuilder.append("&c=").append(userPrincipal.getClientId());
            if (redirectCode != null) {
                encodedURLBuilder.append("&redirectCode=").append(redirectCode);
            }
            if (repoId != null) {
                encodedURLBuilder.append("&repoId=").append(repoId);
            }
            StringBuilder redirectURLBuilder = new StringBuilder(siteName).append("_layouts/15/appredirect.aspx?client_id=").append(appId).append("&redirect_uri=").append(URLEncoder.encode(encodedURLBuilder.toString(), "UTF-8"));
            return redirectURLBuilder.toString();
        } finally {
            session.close();
        }
    }

    public static JsonResponse finishAuthRequest(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal userPrincipal) throws UnsupportedEncodingException {
        String redirectURI = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + RMSMessageHandler.getClientString("appNotFound");
        String repoName = request.getParameter("name");
        String accountIdFrom = request.getParameter("accountId");

        JsonResponse error = new JsonResponse(400, "App not found");
        error.putResult(RepoConstants.KEY_REDIRECT_URL, redirectURI);

        if (!StringUtils.hasText(request.getParameter("siteName"))) {
            return error;
        }
        String spServer = URLDecoder.decode(request.getParameter("siteName"), "UTF-8");
        try {
            ServiceProviderSetting storageProviderSettings = null;
            try (DbSession session = DbSession.newSession()) {
                storageProviderSettings = SettingManager.getStorageProviderSettings(session, userPrincipal.getTenantId(), ServiceProviderType.SHAREPOINT_ONLINE);
            }
            Map<String, String> attributesMap = storageProviderSettings.getAttributes();
            Map<String, String> tokens = SharePointRepoAuthHelper.getOAuthToken(request, attributesMap.get(ServiceProviderSetting.APP_SECRET), userPrincipal);
            if (tokens == null) {
                return error;
            }
            String refreshToken = tokens.get(RepositoryManager.REFRESH_TOKEN);
            UserProfile userProfile = SharePointRepoAuthHelper.getUserProperties(tokens.get(RepositoryManager.ACCESS_TOKEN), spServer);
            String username = userProfile.getDetail().getEmail();
            try (DbSession session = DbSession.newSession()) {
                StorageProvider storageProvider = SettingManager.getStorageProvider(session, userPrincipal.getTenantId(), ServiceProviderType.SHAREPOINT_ONLINE);
                Map<String, Object> map = GsonUtils.GSON.fromJson(storageProvider.getAttributes(), GsonUtils.GENERIC_MAP_TYPE);
                map.put(SharePointRepoAuthHelper.SP_ONLINE_APP_CONTEXT_ID, tokens.get(SharePointRepoAuthHelper.SP_ONLINE_APP_CONTEXT_ID));
                session.beginTransaction();
                storageProvider.setAttributes(GsonUtils.GSON.toJson(map));
                session.update(storageProvider);
                session.commit();

                if (!spServer.endsWith("/")) {
                    StringBuilder spServerBuilder = new StringBuilder(spServer);
                    spServer = spServerBuilder.append('/').toString();
                }

                String accName = spServer;
                if (!accName.startsWith("https://")) {
                    accName = new StringBuilder("https://").append(accName).toString();
                }
                boolean isShared = Boolean.valueOf(request.getParameter("isShared"));
                boolean reauthenticate = RepositoryManager.validateCookieRedirectParameters(request, response, accountIdFrom);
                return OAuthHelper.addRecordToDB(session, userPrincipal, repoName, username, accountIdFrom, accName, refreshToken, storageProviderSettings, reauthenticate, isShared, request, response);
            }
        } catch (RMSException e) {
            LOGGER.debug("Error in AuthFinish", e);
        } catch (Exception e) {
            LOGGER.error("Error in finishAuth: {}", e.getMessage(), e);
        } finally {
            RepositoryManager.clearCookieRedirectParameters(request, response);
        }
        return error;
    }
}
