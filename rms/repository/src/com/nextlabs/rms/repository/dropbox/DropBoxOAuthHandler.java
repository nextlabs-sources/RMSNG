package com.nextlabs.rms.repository.dropbox;

import com.dropbox.core.BadRequestException;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxRequestConfig.Builder;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxWebAuth.Request;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.util.IOUtil.ReadException;
import com.dropbox.core.util.IOUtil.WrappedException;
import com.dropbox.core.util.IOUtil.WriteException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.UploadError;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadWriteFailed;
import com.dropbox.core.v2.files.WriteError;
import com.dropbox.core.v2.files.WriteError.Tag;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.SpaceUsage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class DropBoxOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String DB_AUTH_CSRF_TOKEN_KEY = "dropbox-auth-csrf-token";

    private static final String COOKIE_AUTH_CSRF_TOKEN_KEY = "key";

    private DropBoxOAuthHandler() {
    }

    private static DbxWebAuth getWebAuth(HttpServletRequest request, RMSUserPrincipal user) {
        // After we redirect the user to the Dropbox website for authorization,
        // Dropbox will redirect them back here.
        DbxAppInfo dbxAppInfo = null;
        try (DbSession db = DbSession.newSession()) {
            ServiceProviderSetting dropboxSetting = SettingManager.getStorageProviderSettings(db, user.getTenantId(), ServiceProviderType.DROPBOX);
            if (dropboxSetting == null) {
                throw new IllegalArgumentException("No service provider configured");
            }
            String appKey = dropboxSetting.getAttributes().get(ServiceProviderSetting.APP_ID);
            String appSecret = dropboxSetting.getAttributes().get(ServiceProviderSetting.APP_SECRET);

            StringBuilder sb = new StringBuilder(50);
            sb.append("{\"key\":\"");
            sb.append(appKey);
            sb.append("\",\"secret\":\"");
            sb.append(appSecret);
            sb.append("\"}");
            dbxAppInfo = DbxAppInfo.Reader.readFully(sb.toString());
        } catch (JsonReadException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new DbxWebAuth(getRequestConfig(request), dbxAppInfo);
    }

    private static DbxRequestConfig getRequestConfig(HttpServletRequest request) {
        Builder builder = DbxRequestConfig.newBuilder(RepoConstants.RMS_CLIENT_IDENTIFIER);
        builder.withUserLocaleFrom(request.getLocale());
        return builder.build();
    }

    public static String startDBAuth(HttpServletRequest request, HttpServletResponse response, RMSUserPrincipal user)
            throws UnsupportedEncodingException {
        String tenantId = user.getTenantId();
        DbSession db = DbSession.newSession();
        ServiceProviderSetting dropboxSetting = null;
        String name = request.getParameter("name");
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        String accountIdFrom = "";
        try {
            dropboxSetting = SettingManager.getStorageProviderSettings(db, tenantId, ServiceProviderType.DROPBOX);
            Criteria criteria = db.createCriteria(Repository.class);
            criteria.add(Restrictions.eq("userId", user.getUserId()));
            criteria.add(Restrictions.eq("name", name));
            Repository repo = (Repository)criteria.uniqueResult();
            if (repo != null) {
                accountIdFrom = repo.getAccountId();
            }
        } finally {
            db.close();
        }
        // Start the authorization process with Dropbox.
        // Select a spot in the session for DbxWebAuth to store the CSRF token.
        HttpSession session = request.getSession(true);
        DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, DB_AUTH_CSRF_TOKEN_KEY);
        DbxWebAuth auth = getWebAuth(request, user);
        JsonObject state = new JsonObject();
        state.addProperty("displayName", name);
        state.addProperty("accountId", accountIdFrom);
        state.addProperty("u", user.getUserId());
        state.addProperty("t", user.getTicket());
        state.addProperty("c", user.getClientId());
        Request.Builder builder = Request.newBuilder();
        builder.withState(state.toString());
        builder.withRedirectUri(getRedirectURL(dropboxSetting, request, authorizeType), csrfTokenStore);
        Request dropboxRequest = builder.build();
        String authorizeUrl = auth.authorize(dropboxRequest);
        /*
         * getWebAuth.start above will set csrf token in session using the key we provided.
         * Get it and send it as part of cookie. We will use the same token from cookie
         * in finishDBAuth. Even though we are using HttpSession this change will make it stateless
         */
        String csrfToken = (String)session.getAttribute(DB_AUTH_CSRF_TOKEN_KEY);
        response.addCookie(new Cookie(COOKIE_AUTH_CSRF_TOKEN_KEY, csrfToken));
        session.invalidate();
        return addCsrfTokenToAuthUrl(authorizeUrl, csrfToken);
    }

    private static String addCsrfTokenToAuthUrl(String authURL, String csrfToken) throws UnsupportedEncodingException {
        String decoded = URLDecoder.decode(authURL, "UTF-8");
        StringBuilder str = new StringBuilder(decoded);
        str.insert(decoded.indexOf("\"accountId\""), "\"csrf_token\":\"" + csrfToken + "\",");
        return str.toString();
    }

    /**
     *
     * @param request
     * @param response
     * @throws UnsupportedEncodingException 
     */
    private static void addTokenToSessionFromCookie(HttpServletRequest request,
        HttpServletResponse response) throws UnsupportedEncodingException {
        String token = null;
        Cookie[] cookies = request.getCookies();
        Cookie csrfCookie = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(COOKIE_AUTH_CSRF_TOKEN_KEY)) {
                    token = cookies[i].getValue();
                    csrfCookie = cookies[i];
                    csrfCookie.setMaxAge(0);
                    response.addCookie(csrfCookie);
                    break;
                }
            }
        }
        if (token == null) {
            String state = URLDecoder.decode(request.getParameter("state"), "UTF-8");
            token = state.substring(state.indexOf("csrf_token\":\"") + "csrf_token\":\"".length());
            token = token.substring(0, token.indexOf('\"'));
        }
        /*
         * Dropbox lib class will read this attribute from session to verify that csrf token
         * generated is same at the start and end of authorization
         */
        request.getSession(true).setAttribute(DB_AUTH_CSRF_TOKEN_KEY, token);
    }

    private static String getRedirectURL(ServiceProviderSetting setting, HttpServletRequest request,
        RepoConstants.AUTHORIZE_TYPE authorizeType) {
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
        uriBuilder.append('/').append(RepoConstants.DROPBOX_AUTH_FINSIH_URL);
        return uriBuilder.toString();
    }

    public static JsonResponse finishDBAuth(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal user) throws UnsupportedEncodingException, InvalidTokenException {
        String repoName = "";
        String accountIdFrom = "";
        String redirectUri = "";
        RepoConstants.AUTHORIZE_TYPE authorizeType = (RepoConstants.AUTHORIZE_TYPE)request.getAttribute(RepoConstants.KEY_AUTHORIZE_TYPE);
        if (!StringUtils.equals(request.getMethod(), "GET")) {
            JsonResponse res = new JsonResponse(405, "Invalid method");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, "");
            return res;
        }
        DbxAuthFinish authFinish;
        ServiceProviderSetting dropboxSetting = null;
        try {
            addTokenToSessionFromCookie(request, response);
            DbxWebAuth auth = getWebAuth(request, user);
            HttpSession session = request.getSession();
            DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, DB_AUTH_CSRF_TOKEN_KEY);
            String tenantId = user.getTenantId();
            try (DbSession db = DbSession.newSession()) {
                dropboxSetting = SettingManager.getStorageProviderSettings(db, tenantId, ServiceProviderType.DROPBOX);
            }
            authFinish = auth.finishFromRedirect(getRedirectURL(dropboxSetting, request, authorizeType), csrfTokenStore, request.getParameterMap());
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(authFinish.getUrlState(), JsonElement.class);
            JsonObject jsonState = element.getAsJsonObject();
            repoName = jsonState.get("displayName").getAsString();
            accountIdFrom = jsonState.get("accountId").getAsString();
            session.invalidate();
        } catch (DbxWebAuth.BadRequestException e) {
            LOGGER.error("On /dropbox-auth-finish: Bad request: {}", e.getMessage());
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } catch (DbxWebAuth.BadStateException e) {
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } catch (DbxWebAuth.CsrfException e) {
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } catch (DbxWebAuth.NotApprovedException e) {
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("repoUnauthorizedAccess", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(403, "unauthorized access");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } catch (DbxWebAuth.ProviderException e) {
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } catch (DbxException e) {
            if (e instanceof BadRequestException) {
                try {
                    DropboxOAuthErrorResponse err = new Gson().fromJson(e.getMessage(), DropboxOAuthErrorResponse.class);
                    String error = err.getError();
                    String errorDescription = err.getDescription();
                    if (error.contains("invalid_grant")) {
                        Repository repo = OAuthHelper.getExistingRepo(user.getUserId(), dropboxSetting, accountIdFrom, repoName);
                        if (repo != null && dropboxSetting != null) {
                            return OAuthHelper.sendRepoExistsResponse(repo, dropboxSetting.getProviderType().name());
                        }
                        throw new InvalidTokenException(errorDescription != null ? errorDescription : error, e);
                    }
                } catch (JsonSyntaxException ex) {
                }
            }
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        }
        // We have an Dropbox API access token now.  This is what will let us make Dropbox API
        // calls.  Save it in the database entry for the current user.

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Access Token received from DropBox...");
        }

        String accountIdTo = String.valueOf(authFinish.getUserId());
        String token = authFinish.getAccessToken();
        boolean reauthenticate = RepositoryManager.validateCookieRedirectParameters(request, response, accountIdFrom);
        try {
            if (dropboxSetting == null) {
                return null;
            }
            Builder builder = DbxRequestConfig.newBuilder(RepoConstants.RMS_CLIENT_IDENTIFIER);
            builder.withUserLocaleFrom(Locale.getDefault());
            DbxRequestConfig config = builder.build();
            DbxClientV2 client = new DbxClientV2(config, token);
            FullAccount account = client.users().getCurrentAccount();
            String accountName = account.getEmail();
            try (DbSession session = DbSession.newSession()) {
                return OAuthHelper.addRecordToDB(session, user, repoName, accountIdTo, accountIdFrom, accountName, token, dropboxSetting, reauthenticate, false, request, response);
            }
        } catch (DbxException e) {
            LOGGER.error(e.getMessage(), e);
            redirectUri = OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(ServiceProviderType.DROPBOX.name())), StandardCharsets.UTF_8.name());
            JsonResponse res = new JsonResponse(500, "Error finishing oAuth");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, redirectUri);
            return res;
        } finally {
            RepositoryManager.clearCookieRedirectParameters(request, response);
        }
    }

    public static SpaceUsage getSpaceUsage(String accessToken) throws RepositoryException {
        if (!StringUtils.hasText(accessToken)) {
            throw new UnauthorizedRepositoryException("No access token");
        }
        Builder builder = DbxRequestConfig.newBuilder(RepoConstants.RMS_CLIENT_IDENTIFIER);
        builder.withUserLocaleFrom(Locale.getDefault());
        DbxRequestConfig config = builder.build();
        DbxClientV2 client = new DbxClientV2(config, accessToken);
        try {
            return client.users().getSpaceUsage();
        } catch (DbxException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public static void handleException(Exception e) throws RepositoryException {
        if (e instanceof DownloadErrorException) {
            throw new FileNotFoundException(e.getMessage(), e);
        } else if (e instanceof UploadErrorException) {
            UploadErrorException ex = (UploadErrorException)e;
            UploadError uploadError = ex.errorValue;
            UploadWriteFailed uploadWriteFailed = uploadError.getPathValue();
            WriteError writeError = uploadWriteFailed.getReason();
            Tag tag = writeError.tag();
            if (tag == Tag.INSUFFICIENT_SPACE) {
                throw new InSufficientSpaceException(e.getMessage(), e);
            } else {
                throw new RepositoryException(e.getMessage(), e);
            }
        } else if (e instanceof DbxException) {
            if (e instanceof InvalidAccessTokenException) {
                throw new InvalidTokenException(e.getMessage(), e);
            } else if (e instanceof NetworkIOException) {
                throw new com.nextlabs.rms.repository.exception.IOException(e.getMessage(), e);
            } else if (e instanceof BadRequestException) {
                throw new UnauthorizedRepositoryException(e.getMessage(), e);
            }
            throw new RepositoryException(e.getMessage(), e);
        } else if (e instanceof WrappedException) {
            if (e instanceof ReadException || e instanceof WriteException) {
                throw new com.nextlabs.rms.repository.exception.IOException(e.getMessage(), e);
            }
            throw new RepositoryException(e.getMessage(), e);
        } else if (e instanceof RepositoryException) {
            throw (RepositoryException)e;
        }
        throw new RepositoryException(e.getMessage(), e);
    }

    public static void handleException(Exception e, String folderName) throws RepositoryException {
        if (e instanceof CreateFolderErrorException) {
            CreateFolderErrorException ex = (CreateFolderErrorException)e;
            if (ex.errorValue.isPath() && ex.errorValue.getPathValue().isConflict()) {
                throw new FileConflictException(e.getMessage(), e, folderName);
            }
        }
    }
}
