package com.nextlabs.rms.repository.googledrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
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
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class GoogleDriveOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE, "email", "profile");

    private GoogleDriveOAuthHandler() {
    }

    /**
     * Build an authorization flow and store it as a static class attribute.
     * @param setting
     * @return GoogleAuthorizationCodeFlow instance.
     */
    private static GoogleAuthorizationCodeFlow getFlow(ServiceProviderSetting setting) {
        return new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, getAppKey(setting), getAppSecret(setting), SCOPES).setAccessType("offline").setApprovalPrompt("auto").build();
    }

    /**
     * Retrieve the authorization URL.
     * @param setting
     * @param state State for the authorization URL.
     * @return Authorization URL to redirect the user to.
     * @throws IOException
     */
    private static String getAuthorizationUrl(ServiceProviderSetting setting, String state,
        RepoConstants.AUTHORIZE_TYPE authorizeType)
            throws IOException {
        String redirectURL = getRedirectURL(setting, authorizeType);
        GoogleAuthorizationCodeRequestUrl urlBuilder = getFlow(setting).newAuthorizationUrl().setRedirectUri(redirectURL).setApprovalPrompt("force").setState(state);
        return urlBuilder.build();
    }

    public static String startGDAuth(HttpServletRequest request, RMSUserPrincipal userPrincipal) throws IOException {
        String tenantId = userPrincipal.getTenantId();
        DbSession session = DbSession.newSession();
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        try {
            ServiceProviderSetting setting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.GOOGLE_DRIVE);
            String displayName = request.getParameter("name");
            String accountIdFrom = "";
            Criteria criteria = session.createCriteria(Repository.class);
            criteria.add(Restrictions.eq("userId", userPrincipal.getUserId()));
            criteria.add(Restrictions.eq("name", displayName));
            Repository repo = (Repository)criteria.uniqueResult();
            if (repo != null) {
                accountIdFrom = repo.getAccountId();
            }

            return getAuthorizationUrl(setting, generateStateToken(displayName, accountIdFrom, userPrincipal), authorizeType);
        } finally {
            session.close();
        }
    }

    public static JsonResponse finishGDAuth(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal user)
            throws IOException, RepositoryException {
        String authCode = request.getParameter("code");
        String error = request.getParameter("error");
        String state = request.getParameter("state");
        String redirectUri = "";
        String displayName = null;
        String accountIdFrom = null;
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        if (state != null) {
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(state, JsonElement.class);
            JsonObject jsonState = element.getAsJsonObject();
            displayName = jsonState.get("displayName").getAsString();
            accountIdFrom = jsonState.get("accountId").getAsString();
        }
        if (authCode == null || error != null) {
            LOGGER.warn("You are not authorized to access the Google Drive account: {}", error);
            String msg = RMSMessageHandler.getClientString("repoUnauthorizedAccess", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.GOOGLE_DRIVE.name()));
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
                setting = SettingManager.getStorageProviderSettings(session, tenantId, ServiceProviderType.GOOGLE_DRIVE);
            }
            Credential credential = null;
            try {
                credential = getOAuthTokens(setting, authCode, authorizeType);
            } catch (IOException e) {
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

            Oauth2 oauth2 = new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(RepoConstants.RMS_CLIENT_IDENTIFIER).build();
            Userinfoplus userInfo = oauth2.userinfo().get().execute();

            String repoName = displayName != null ? displayName : userInfo.getName();
            String accountId = userInfo.getId();
            String accountName = userInfo.getEmail();
            String token = credential.getRefreshToken();
            try (DbSession session = DbSession.newSession()) {
                return OAuthHelper.addRecordToDB(session, user, repoName, accountId, accountIdFrom, accountName, token, setting, reauthenticate, false, request, response);
            }
        } finally {
            RepositoryManager.clearCookieRedirectParameters(request, response);
        }
    }

    private static Credential getOAuthTokens(ServiceProviderSetting setting, String authCode,
        RepoConstants.AUTHORIZE_TYPE authorizeType)
            throws IOException {
        String redirectURL = getRedirectURL(setting, authorizeType);
        GoogleTokenResponse response = getFlow(setting).newTokenRequest(authCode).setRedirectUri(redirectURL).execute();
        return buildCredential(setting).setFromTokenResponse(response);
    }

    public static Credential getAccessTokenFromRefreshToken(ServiceProviderSetting setting, String refreshToken)
            throws RepositoryException {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedRepositoryException("No refresh token");
        }
        Credential credential = GoogleDriveOAuthHandler.buildCredential(setting);
        credential.setRefreshToken(refreshToken);
        try {
            credential.refreshToken();
        } catch (IOException e) {
            handleException(e);
        }
        return credential;
    }

    private static String getRedirectURL(ServiceProviderSetting googleDriveSetting,
        RepoConstants.AUTHORIZE_TYPE authorizeType) {
        String url = googleDriveSetting.getAttributes().get(ServiceProviderSetting.REDIRECT_URL);
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("No redirect URL is configured");
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        StringBuilder uriBuilder = new StringBuilder(url);
        switch (authorizeType) {
            case AUTHORIZE_TYPE_CUSTOM:
                uriBuilder.append("/custom");
                break;
            case AUTHORIZE_TYPE_JSON:
                uriBuilder.append("/json");
                break;
            case AUTHORIZE_TYPE_WEB:
            default:
                break;
        }
        uriBuilder.append('/').append(RepoConstants.GOOGLE_DRIVE_AUTH_FINISH_URL);
        return uriBuilder.toString();
    }

    private static String getAppKey(ServiceProviderSetting setting) {
        return setting.getAttributes().get(ServiceProviderSetting.APP_ID);
    }

    private static String getAppSecret(ServiceProviderSetting setting) {
        return setting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
    }

    public static Credential buildCredential(ServiceProviderSetting setting) {
        return new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY).setClientSecrets(getAppKey(setting), getAppSecret(setting)).build();
    }

    public static Drive getDrive(Credential credential) {
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(credential)).setApplicationName(RepoConstants.RMS_CLIENT_IDENTIFIER).build();
    }

    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {

            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(RepoConstants.CONNECTION_TIMEOUT);
                httpRequest.setReadTimeout(RepoConstants.READ_TIMEOUT);
            }
        };
    }

    public static void handleException(Exception e) throws RepositoryException {
        if (e instanceof IOException) {
            if (e instanceof HttpResponseException) {
                HttpResponseException ex = (HttpResponseException)e;
                int statusCode = ex.getStatusCode();
                if (statusCode == 400) {
                    if (e instanceof TokenResponseException) {
                        TokenResponseException ex1 = (TokenResponseException)e;
                        TokenErrorResponse details = ex1.getDetails();
                        if (details != null) {
                            String error = details.getError();
                            String errorDescription = details.getErrorDescription();
                            if (error.contains("invalid_grant") || error.contains("unauthorized_client")) {
                                throw new InvalidTokenException(errorDescription != null ? errorDescription : error, e);
                            }
                        }
                    }
                } else if (statusCode == 401) {
                    throw new UnauthorizedRepositoryException(e.getMessage(), ex);
                } else if (statusCode == 403 && e.getMessage().contains("The user has exceeded their Drive storage quota")) {
                    throw new InSufficientSpaceException(e.getMessage(), ex);
                } else if (statusCode == 404) {
                    String msg = e.getMessage();
                    if (e instanceof GoogleJsonResponseException) {
                        GoogleJsonResponseException ex1 = (GoogleJsonResponseException)e;
                        GoogleJsonError details = ex1.getDetails();
                        if (details != null) {
                            msg = details.getMessage();
                        }
                    }
                    throw new FileNotFoundException(msg, e);
                }
            }
            throw new RepositoryException(e.getMessage(), e);
        } else if (e instanceof RepositoryException) {
            throw (RepositoryException)e;
        }
        throw new RepositoryException(e.getMessage(), e);
    }

    /**
     * Generates a secure state token
     */
    private static String generateStateToken(String displayName, String accountId, RMSUserPrincipal user) {
        try {
            JsonObject state = new JsonObject();
            SecureRandom sr1 = SecureRandom.getInstance("DEFAULT", "BCFIPS");
            state.addProperty("hash", "google;" + sr1.nextInt());
            state.addProperty("displayName", displayName);
            state.addProperty("accountId", accountId);
            state.addProperty("u", user.getUserId());
            state.addProperty("t", user.getTicket());
            state.addProperty("c", user.getClientId());
            return state.toString();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }
}
