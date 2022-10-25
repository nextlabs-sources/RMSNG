package com.nextlabs.rms.repository.box;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxUser;
import com.box.sdk.BoxUser.Info;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.FIPSError;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class BoxOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private BoxOAuthHandler() {
    }

    public static String startAuth(HttpServletRequest request, RMSUserPrincipal user) throws URISyntaxException {
        String tenantId = user.getTenantId();
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        try (DbSession session = DbSession.newSession()) {
            ServiceProviderSetting setting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.BOX);
            String displayName = request.getParameter("name");
            String accountIdFrom = "";
            Criteria criteria = session.createCriteria(Repository.class);
            criteria.add(Restrictions.eq("userId", user.getUserId()));
            criteria.add(Restrictions.eq("name", displayName));
            Repository repo = (Repository)criteria.uniqueResult();
            if (repo != null) {
                accountIdFrom = repo.getAccountId();
            }

            return getAuthorizationUrl(setting, request, generateStateToken(displayName, accountIdFrom, user), authorizeType);
        }
    }

    private static String getAuthorizationUrl(ServiceProviderSetting setting, HttpServletRequest request, String state,
        RepoConstants.AUTHORIZE_TYPE authorizeType) throws URISyntaxException {
        String appKey = setting.getAttributes().get(ServiceProviderSetting.APP_ID);
        URL authorizationURL = BoxAPIConnection.getAuthorizationURL(appKey, getRedirectURL(setting, request, authorizeType), state, Collections.<String> emptyList());
        return authorizationURL.toString();
    }

    private static String generateStateToken(String displayName, String accountId, RMSUserPrincipal user) {

        try {
            SecureRandom sr1 = SecureRandom.getInstance("DEFAULT", "BCFIPS");
            JsonObject state = new JsonObject();
            state.addProperty("hash", "box;" + sr1.nextInt());
            state.addProperty("displayName", displayName);
            state.addProperty("accountId", accountId);
            state.addProperty("u", user.getUserId());
            state.addProperty("t", user.getTicket());
            state.addProperty("c", user.getClientId());
            return state.toString();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("DRBG algorithm or provider not available");
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }

    private static URI getRedirectURL(ServiceProviderSetting setting, HttpServletRequest request,
        RepoConstants.AUTHORIZE_TYPE authorizeType) throws URISyntaxException {
        String redirectURL = null;
        if (setting != null) {
            redirectURL = setting.getAttributes().get(ServiceProviderSetting.REDIRECT_URL);
        }
        if (!StringUtils.hasText(redirectURL)) {
            redirectURL = HTTPUtil.getURI(request);
        }
        if (redirectURL != null && redirectURL.endsWith("/")) {
            redirectURL = redirectURL.substring(0, redirectURL.length() - 1);
        }
        StringBuilder uriBuilder = new StringBuilder(redirectURL);
        uriBuilder.append('/').append(RepoConstants.BOX_AUTH_FINISH_URL);
        switch (authorizeType) {
            case AUTHORIZE_TYPE_CUSTOM:
                uriBuilder.append("?custom=true");
                break;
            case AUTHORIZE_TYPE_JSON:
                uriBuilder.append("?json=true");
                break;
            case AUTHORIZE_TYPE_WEB:
            default:
                break;
        }
        return new URI(uriBuilder.toString());
    }

    public static JsonResponse finishAuth(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal user) throws IOException, RepositoryException {
        String authCode = request.getParameter("code");
        String error = request.getParameter("error");
        String state = request.getParameter("state");
        String redirectUri = "";
        String displayName = null;
        String accountIdFrom = null;
        if (state != null) {
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(state, JsonElement.class);
            JsonObject jsonState = element.getAsJsonObject();
            displayName = jsonState.get("displayName").getAsString();
            accountIdFrom = jsonState.get("accountId").getAsString();
        }
        if (authCode == null || error != null) {
            LOGGER.warn("You are not authorized to access the Box account: {}", error);
            String msg = RMSMessageHandler.getClientString("repoUnauthorizedAccess", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.BOX.name()));
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(403, "Unauthorized access");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        }

        String tenantId = user.getTenantId();
        boolean reauthenticate = RepositoryManager.validateCookieRedirectParameters(request, response, accountIdFrom);
        try {
            ServiceProviderSetting setting = null;
            try (DbSession session = DbSession.newSession()) {
                setting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.BOX);
            }
            BoxAPIConnection connection = null;
            try {
                connection = getOAuthConnection(setting, authCode);
            } catch (BoxAPIException e) {
                try {
                    handleException(e);
                } catch (InvalidTokenException ex) {
                    Repository repo = OAuthHelper.getExistingRepo(user.getUserId(), setting, accountIdFrom, displayName);
                    if (repo != null) {
                        return OAuthHelper.sendRepoExistsResponse(repo, setting.getProviderType().name());
                    }
                    throw ex;
                }
            }

            BoxUser userInfo = BoxUser.getCurrentUser(connection);
            Info info = userInfo.getInfo();
            String repoName = displayName != null ? displayName : info.getName();
            String accountId = userInfo.getID();
            String accountName = info.getLogin();
            String token = connection.getRefreshToken();
            try (DbSession session = DbSession.newSession()) {
                JsonResponse jsonResp = OAuthHelper.addRecordToDB(session, user, repoName, accountId, accountIdFrom, accountName, token, setting, reauthenticate, false, request, response);
                JsonRepository jsonRepo = jsonResp.getResult("repository", JsonRepository.class);
                if (jsonRepo != null && StringUtils.hasText(jsonRepo.getRepoId())) {
                    updateState(jsonRepo.getRepoId(), user.getUserId(), connection.save(), null);
                }
                return jsonResp;
            }
        } catch (BoxAPIException e) {
            handleException(e);
            return null;
        } finally {
            RepositoryManager.clearCookieRedirectParameters(request, response);
        }
    }

    private static BoxAPIConnection getOAuthConnection(ServiceProviderSetting setting, String authCode)
            throws IOException {
        String appKey = setting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String appSecret = setting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        return new BoxAPIConnection(appKey, appSecret, authCode);
    }

    public static void updateState(String repoId, int userId, String state, String refreshToken) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Repository repo = session.get(Repository.class, repoId);
            if (repo != null && repo.getUserId() == userId) {
                repo.setState(state);
                if (StringUtils.hasText(refreshToken)) {
                    repo.setToken(refreshToken);
                }
            }
            session.commit();
        }
    }

    public static String getState(String repoId) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Repository repo = session.get(Repository.class, repoId);
            return repo != null ? repo.getState() : null;
        }
    }

    public static void handleException(Exception e) throws RepositoryException {
        if (e instanceof BoxAPIException) {
            int responseCode = ((BoxAPIException)e).getResponseCode();
            String error = ((BoxAPIException)e).getResponse();
            if (responseCode == 400) {
                if (error.contains("invalid_grant") || error.contains("unauthorized_client")) {
                    throw new InvalidTokenException(error, e);
                }
            } else if (responseCode == 404) {
                throw new com.nextlabs.rms.repository.exception.FileNotFoundException(error, e);
            } else if (responseCode == 403 || responseCode == 401) {
                throw new UnauthorizedRepositoryException(error, e);
            }
        }
        throw new RepositoryException(e.getMessage(), e);
    }
}
