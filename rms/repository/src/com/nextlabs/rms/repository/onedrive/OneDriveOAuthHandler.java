package com.nextlabs.rms.repository.onedrive;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.repository.onedrive.OneDriveOwner.OneDriveUser;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.IHTTPResponseHandler;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class OneDriveOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String SCOPES = "wl.signin wl.offline_access onedrive.readwrite";

    private static final String ONE_DRIVE_CODE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String ONE_DRIVE_TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String ONE_DRIVE_API_URL = "https://api.onedrive.com/v1.0";

    private OneDriveOAuthHandler() {
    }

    private static OneDriveAppInfo initOneDriveAppInfo(ServiceProviderSetting oneDriveSetting,
        RepoConstants.AUTHORIZE_TYPE authorizeType) {
        String clientId = oneDriveSetting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String clientSecret = oneDriveSetting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        String redirectUrl = oneDriveSetting.getAttributes().get(ServiceProviderSetting.REDIRECT_URL);
        if (!StringUtils.hasText(redirectUrl)) {
            throw new IllegalArgumentException("No redirect URL is configured");
        }
        if (redirectUrl.endsWith("/")) {
            redirectUrl = redirectUrl.substring(0, redirectUrl.length() - 1);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(redirectUrl);
        switch (authorizeType) {
            case AUTHORIZE_TYPE_CUSTOM:
                builder.append("/custom");
                break;
            case AUTHORIZE_TYPE_JSON:
                builder.append("/json");
                break;
            case AUTHORIZE_TYPE_WEB:
            default:
                break;
        }
        builder.append('/');
        builder.append(RepoConstants.ONE_DRIVE_AUTH_FINISH_URL);
        redirectUrl = builder.toString();
        return new OneDriveAppInfo(clientId, clientSecret, redirectUrl);
    }

    public static String startAuth(HttpServletRequest request, RMSUserPrincipal userPrincipal) {
        String tenantId = userPrincipal.getTenantId();
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        DbSession session = DbSession.newSession();
        try {
            ServiceProviderSetting oneDriveSetting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.ONE_DRIVE);

            OneDriveAppInfo info = initOneDriveAppInfo(oneDriveSetting, authorizeType);
            String repoName = request.getParameter("name");
            String accountIdFrom = "";
            Criteria criteria = session.createCriteria(Repository.class);
            criteria.add(Restrictions.eq("userId", userPrincipal.getUserId()));
            criteria.add(Restrictions.eq("name", repoName));
            Repository repo = (Repository)criteria.uniqueResult();
            if (repo != null) {
                accountIdFrom = repo.getAccountId();
            }
            String state = generateStateToken(repoName, accountIdFrom, userPrincipal);

            List<NameValuePair> params = new ArrayList<>(5);
            params.add(new BasicNameValuePair("client_id", info.clientId));
            params.add(new BasicNameValuePair("scope", SCOPES));
            params.add(new BasicNameValuePair("response_type", "code"));
            params.add(new BasicNameValuePair("redirect_uri", info.redirectUrl));
            params.add(new BasicNameValuePair("state", state));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            return ONE_DRIVE_CODE_URL + "?" + queryParams;
        } finally {
            session.close();
        }
    }

    public static JsonResponse finishAuth(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal userPrincipal)
            throws RepositoryException, UnsupportedEncodingException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");

        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);

        String repoName = null;
        String accountIdFrom = null;

        if (state != null) {
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(state, JsonElement.class);
            JsonObject jsonState = element.getAsJsonObject();
            repoName = jsonState.get("displayName").getAsString();
            accountIdFrom = jsonState.get("accountId").getAsString();
        }

        if (!StringUtils.hasText(code) || error != null) {
            String errDetails = request.getParameter("error_description");
            LOGGER.error("You are not authorized to access the One Drive account. {}: {}", error, errDetails);
            JsonResponse res = new JsonResponse(403, "Unauthorized access to service provider account");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("repoUnauthorizedAccess", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.ONE_DRIVE.name())), StandardCharsets.UTF_8.name()));
            return res;
        }

        String tenantId = userPrincipal.getTenantId();

        boolean reauthenticate = RepositoryManager.validateCookieRedirectParameters(request, response, accountIdFrom);
        try {
            ServiceProviderSetting oneDriveSetting = null;
            try (DbSession session = DbSession.newSession()) {
                oneDriveSetting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.ONE_DRIVE);
            }
            OneDriveAppInfo info = initOneDriveAppInfo(oneDriveSetting, authorizeType);
            OneDriveTokenResponse tokenResponse = null;
            try {
                tokenResponse = getAccessToken(info, code, GrantType.AUTHORIZATION_CODE);
            } catch (InvalidTokenException e) {
                Repository repo = OAuthHelper.getExistingRepo(userPrincipal.getUserId(), oneDriveSetting, accountIdFrom, repoName);
                if (repo != null) {
                    return OAuthHelper.sendRepoExistsResponse(repo, oneDriveSetting.getProviderType().name());
                }
                throw e;
            }

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            OneDriveUser user = getUser(accessToken);
            LOGGER.trace("User info received from OneDrive ...");

            String accountId = user.getId();
            String accountName = user.getDisplayName();
            try (DbSession session = DbSession.newSession()) {
                return OAuthHelper.addRecordToDB(session, userPrincipal, repoName, accountId, accountIdFrom, accountName, refreshToken, oneDriveSetting, reauthenticate, false, request, response);
            }
        } finally {
            RepositoryManager.clearCookieRedirectParameters(request, response);
        }
    }

    public static OneDriveTokenResponse getAccessToken(OneDriveAppInfo info, String code, GrantType grantType)
            throws RepositoryException {
        List<NameValuePair> params = new ArrayList<>(5);
        params.add(new BasicNameValuePair("client_id", info.clientId));
        params.add(new BasicNameValuePair("client_secret", info.clientSecret));
        params.add(new BasicNameValuePair("redirect_uri", info.redirectUrl));

        if (grantType == GrantType.AUTHORIZATION_CODE) {
            params.add(new BasicNameValuePair("code", code));
        } else if (grantType == GrantType.REFRESH_TOKEN) {
            if (!StringUtils.hasText(code)) {
                throw new UnauthorizedRepositoryException("No refresh token");
            }
            params.add(new BasicNameValuePair("refresh_token", code));
        }
        params.add(new BasicNameValuePair("grant_type", grantType.getType()));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(ONE_DRIVE_TOKEN_URL);
            post.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = client.execute(post)) {
                IHTTPResponseHandler<OneDriveTokenResponse> handler = new OneDriveResponseHandler<>(OneDriveTokenResponse.class);
                return handler.handle(response);
            }
        } catch (OneDriveServiceException | OneDriveOAuthException e) {
            handleException(e);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            handleException(e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            handleException(e);
        }
        return null;
    }

    private static OneDriveUser getUser(String accessToken) throws RepositoryException {
        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new BasicNameValuePair("access_token", accessToken));
        String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
        String uri = ONE_DRIVE_API_URL + "/drive?" + queryParams;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(uri);
            try (CloseableHttpResponse response = client.execute(get)) {
                IHTTPResponseHandler<OneDriveDriveResponse> handler = new OneDriveResponseHandler<>(OneDriveDriveResponse.class);
                OneDriveDriveResponse result = handler.handle(response);
                return result.getOwner().getUser();
            }
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            handleException(e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            handleException(e);
        }
        return null;
    }

    private static String generateStateToken(String repoName, String accountId, RMSUserPrincipal user) {
        try {
            JsonObject state = new JsonObject();
            SecureRandom sr1 = SecureRandom.getInstance("DEFAULT", "BCFIPS");
            state.addProperty("hash", "rms_onedrive;" + sr1.nextInt());
            state.addProperty("displayName", repoName);
            state.addProperty("accountId", accountId);
            state.addProperty("u", user.getUserId());
            state.addProperty("t", user.getTicket());
            state.addProperty("c", user.getClientId());
            return state.toString();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }

    public static void handleException(Exception e) throws RepositoryException {
        if (e instanceof OneDriveServiceException) {
            OneDriveServiceException ex = (OneDriveServiceException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            OneDriveErrorResponse error = ex.getError();
            if (error != null) {
                OneDriveOAuthException.OneDriveResponse oneDriveError = error.getError();
                if (oneDriveError != null) {
                    errorMsg = oneDriveError.getMessage();
                }
            }
            if (statusCode == 401) {
                throw new UnauthorizedRepositoryException(errorMsg);
            } else if (statusCode == 404) {
                throw new FileNotFoundException(errorMsg);
            } else if (statusCode == 507) {
                throw new InSufficientSpaceException(errorMsg);
            }
        } else if (e instanceof OneDriveOAuthException) {
            OneDriveOAuthException ex = (OneDriveOAuthException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            OneDriveOAuthErrorResponse error = ex.getError();
            if (error != null) {
                errorMsg = error.getError();
            }
            if (statusCode == 400 && StringUtils.equals("invalid_grant", errorMsg)) {
                throw new InvalidTokenException(errorMsg, e);
            }
            throw new UnauthorizedRepositoryException(errorMsg, e);
        } else if (e instanceof RepositoryException) {
            throw (RepositoryException)e;
        }
        throw new RepositoryException(e.getMessage(), e);
    }

    public static enum GrantType {
        AUTHORIZATION_CODE("authorization_code"),
        REFRESH_TOKEN("refresh_token");

        private String type;

        private GrantType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class OneDriveAppInfo {

        String clientId;
        String clientSecret;
        String redirectUrl;

        public OneDriveAppInfo(String clientId, String clientSecret, String redirectUrl) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUrl = redirectUrl;
        }

    }
}
