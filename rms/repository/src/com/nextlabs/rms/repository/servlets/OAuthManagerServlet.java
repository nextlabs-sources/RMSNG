package com.nextlabs.rms.repository.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.box.BoxOAuthHandler;
import com.nextlabs.rms.repository.dropbox.DropBoxOAuthHandler;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.googledrive.GoogleDriveOAuthHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler;
import com.nextlabs.rms.repository.sharepoint.online.SharePointOnlineOAuthHandler;
import com.nextlabs.rms.servlets.OAuthHelper;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OAuthManagerServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final long serialVersionUID = 3092566310043076819L;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        RMSUserPrincipal user = null;
        String repoType = getRepoType(uri);
        boolean useJsonEndpoint = false;
        boolean useCustomURL = false;
        if ("true".equals(request.getParameter("custom")) || uri.contains("/custom/")) {
            useCustomURL = true;
            request.setAttribute(RepoConstants.KEY_AUTHORIZE_TYPE, RepoConstants.AUTHORIZE_TYPE.AUTHORIZE_TYPE_CUSTOM);
        } else if ("true".equals(request.getParameter("json")) || uri.contains("/json/") || uri.endsWith(RepoConstants.SHAREPOINT_ONLINE_AUTH_FINISH_URL) && request.getParameter(RepoConstants.KEY_USE_JSON_ENDPOINT) != null) {
            useJsonEndpoint = true;
            request.setAttribute(RepoConstants.KEY_AUTHORIZE_TYPE, RepoConstants.AUTHORIZE_TYPE.AUTHORIZE_TYPE_JSON);
        } else {
            request.setAttribute(RepoConstants.KEY_AUTHORIZE_TYPE, RepoConstants.AUTHORIZE_TYPE.AUTHORIZE_TYPE_WEB);
        }
        if (uri.toLowerCase().endsWith("finish") && useCustomURL) {
            if (ServiceProviderType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(repoType)) {
                request.setAttribute(AuthManager.USER_ID, request.getParameter("u"));
                request.setAttribute(AuthManager.TICKET, request.getParameter("t"));
                request.setAttribute(AuthManager.CLIENT_ID, request.getParameter("c"));
            } else {
                String state = request.getParameter("state");
                if (state != null) {
                    state = state.substring(state.indexOf('{'));
                    Gson gson = new Gson();
                    JsonElement element = gson.fromJson(state, JsonElement.class);
                    JsonObject jsonState = element.getAsJsonObject();
                    request.setAttribute(AuthManager.USER_ID, jsonState.get("u").getAsString());
                    request.setAttribute(AuthManager.TICKET, jsonState.get("t").getAsString());
                    request.setAttribute(AuthManager.CLIENT_ID, jsonState.get("c").getAsString());
                }
            }
        }
        try (DbSession session = DbSession.newSession()) {
            user = AuthManager.authenticate(session, request);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        if (uri.toLowerCase().endsWith("start")) {
            try {
                String authRepoResponse = "";
                authRepoResponse = handleAuthStart(request, response, user, repoType);
                response.sendRedirect(getAbsoluteURL(authRepoResponse, request));
                return;
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e.getCause() != null ? e.getCause() : e);
                if (useJsonEndpoint) {
                    JsonUtil.writeJsonToResponse(new JsonResponse(500, "Authorization failed"), response);
                    return;
                }
                response.sendRedirect(getAbsoluteURL(OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("repoRedirectURLErr", ServiceProviderSetting.getProviderTypeDisplayName(repoType)), StandardCharsets.UTF_8.name()), request));
                return;
            }
        }

        if (uri.toLowerCase().endsWith("finish")) {
            try {
                JsonResponse res = handleAuthFinish(request, response, user, repoType);
                if (useCustomURL) {
                    StringBuilder stringBuilder = new StringBuilder("/customURL?url=");
                    stringBuilder.append(URLEncoder.encode(constructCustomURL(res), "UTF-8"));
                    response.sendRedirect(getAbsoluteURL(stringBuilder.toString(), request));
                    return;
                } else if (useJsonEndpoint) {
                    JsonUtil.writeJsonToResponse(res, response);
                    return;
                }
                response.sendRedirect(getAbsoluteURL(res.getResultAsString(RepoConstants.KEY_REDIRECT_URL), request));
                return;
            } catch (RepositoryException e) {
                JsonResponse json;
                if (e instanceof InvalidTokenException) {
                    json = new JsonResponse(410, "Code Expired");
                } else {
                    json = new JsonResponse(500, "Authorization failed");
                    LOGGER.error(e.getMessage(), e.getCause() != null ? e.getCause() : e);
                }
                if (useCustomURL) {
                    StringBuilder stringBuilder = new StringBuilder("/customURL?url=");
                    String customURL = WebConfig.getInstance().getProperty(WebConfig.RMC_CUSTOM_URL, "com.skydrm.rmc://repo.auth.result") + "?statusCode=" + json.getStatusCode();
                    stringBuilder.append(URLEncoder.encode(customURL, "UTF-8"));
                    response.sendRedirect(getAbsoluteURL(stringBuilder.toString(), request));
                    return;
                } else if (useJsonEndpoint) {
                    JsonUtil.writeJsonToResponse(json, response);
                    return;
                }
                response.sendRedirect(getAbsoluteURL(OAuthHelper.REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(RMSMessageHandler.getClientString("errAddRepo", ServiceProviderSetting.getProviderTypeDisplayName(repoType)), StandardCharsets.UTF_8.name()), request));
                return;
            }
        }
    }

    private static String constructCustomURL(JsonResponse response) {
        JsonRepository jsonRepo = response.getResult("repository", JsonRepository.class);
        String customURL = WebConfig.getInstance().getProperty(WebConfig.RMC_CUSTOM_URL, "com.skydrm.rmc://repo.auth.result");
        StringBuilder builder = new StringBuilder(customURL);
        if (jsonRepo != null && StringUtils.hasText(jsonRepo.getRepoId())) {
            builder.append("?statusCode=").append(response.getStatusCode());
            builder.append("&repoId=").append(jsonRepo.getRepoId());
            builder.append("&name=").append(jsonRepo.getName());
            builder.append("&type=").append(jsonRepo.getType());
            builder.append("&accountName=").append(jsonRepo.getAccountName());
            builder.append("&accountId=").append(jsonRepo.getAccountId());
        } else {
            builder.append("?statusCode=").append(response.getStatusCode());
            builder.append("&message=").append(response.getMessage());
        }
        return builder.toString();
    }

    private static String getAbsoluteURL(String redirectUri, HttpServletRequest request) {
        if (!redirectUri.startsWith("http") && !redirectUri.startsWith(request.getServletContext().getServletContextName())) {
            StringBuilder builder = new StringBuilder();
            builder.append(request.getContextPath());
            builder.append(redirectUri);
            redirectUri = builder.toString();
        }
        return redirectUri;
    }

    private String handleAuthStart(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal user, String repoType) throws IOException, RepositoryException, URISyntaxException {
        if (repoType.equalsIgnoreCase(ServiceProviderType.SHAREPOINT_ONLINE.name())) {
            return SharePointOnlineOAuthHandler.startAuthRequest(request, response, user);
        } else if (repoType.equalsIgnoreCase(ServiceProviderType.DROPBOX.name())) {
            return DropBoxOAuthHandler.startDBAuth(request, response, user);
        } else if (repoType.equalsIgnoreCase(ServiceProviderType.GOOGLE_DRIVE.name())) {
            return GoogleDriveOAuthHandler.startGDAuth(request, user);
        } else if (repoType.equalsIgnoreCase(ServiceProviderType.ONE_DRIVE.name())) {
            return OneDriveOAuthHandler.startAuth(request, user);
        } else if (StringUtils.equalsIgnoreCase(repoType, ServiceProviderType.BOX.name())) {
            return BoxOAuthHandler.startAuth(request, user);
        }
        throw new RepositoryException("Invalid ServiceProvider: " + repoType);
    }

    private JsonResponse handleAuthFinish(HttpServletRequest request, HttpServletResponse response,
        RMSUserPrincipal user, String repoType) throws IOException, RepositoryException {
        if (repoType != null) {
            if (repoType.equalsIgnoreCase(ServiceProviderType.SHAREPOINT_ONLINE.name())) {
                return SharePointOnlineOAuthHandler.finishAuthRequest(request, response, user);
            } else if (repoType.equalsIgnoreCase(ServiceProviderType.DROPBOX.name())) {
                return DropBoxOAuthHandler.finishDBAuth(request, response, user);
            } else if (repoType.equalsIgnoreCase(ServiceProviderType.GOOGLE_DRIVE.name())) {
                return GoogleDriveOAuthHandler.finishGDAuth(request, response, user);
            } else if (repoType.equalsIgnoreCase(ServiceProviderType.ONE_DRIVE.name())) {
                return OneDriveOAuthHandler.finishAuth(request, response, user);
            } else if (StringUtils.equalsIgnoreCase(repoType, ServiceProviderType.BOX.name())) {
                return BoxOAuthHandler.finishAuth(request, response, user);
            }
        }
        throw new RepositoryException("Invalid ServiceProvider: " + repoType);
    }

    private String getRepoType(String uri) {
        if (uri.endsWith(RepoConstants.DROPBOX_AUTH_START_URL) || uri.endsWith(RepoConstants.DROPBOX_AUTH_FINSIH_URL)) {
            return ServiceProviderType.DROPBOX.name();
        } else if (uri.endsWith(RepoConstants.SHAREPOINT_ONLINE_AUTH_START_URL) || uri.endsWith(RepoConstants.SHAREPOINT_ONLINE_AUTH_FINISH_URL)) {
            return ServiceProviderType.SHAREPOINT_ONLINE.name();
        } else if (uri.endsWith(RepoConstants.GOOGLE_DRIVE_AUTH_START_URL) || uri.endsWith(RepoConstants.GOOGLE_DRIVE_AUTH_FINISH_URL)) {
            return ServiceProviderType.GOOGLE_DRIVE.name();
        } else if (uri.endsWith(RepoConstants.ONE_DRIVE_AUTH_START_URL) || uri.endsWith(RepoConstants.ONE_DRIVE_AUTH_FINISH_URL)) {
            return ServiceProviderType.ONE_DRIVE.name();
        } else if (uri.endsWith(RepoConstants.BOX_AUTH_START_URL) || uri.endsWith(RepoConstants.BOX_AUTH_FINISH_URL)) {
            return ServiceProviderType.BOX.name();
        }
        return null;
    }
}
