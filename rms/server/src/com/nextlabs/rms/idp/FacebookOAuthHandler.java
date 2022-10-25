package com.nextlabs.rms.idp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.servlet.HeaderFilter;
import com.nextlabs.rms.servlet.IdpManagerServlet;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FacebookOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String OAUTH_URL;

    private static final String TOKEN_URL;

    static {
        FacebookIdpAttributes attributes = (FacebookIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.FACEBOOK, null);
        OAUTH_URL = new StringBuilder("https://www.facebook.com/dialog/oauth?client_id=").append(attributes.getAppId()).append("&response_type=code&auth_type=reauthenticate&scope=public_profile,email&redirect_uri=").toString();
        TOKEN_URL = new StringBuilder("https://graph.facebook.com/v2.3/oauth/access_token?client_id=").append(attributes.getAppId()).append("&client_secret=").append(attributes.getAppSecret()).append("&redirect_uri=").toString();
    }

    private FacebookOAuthHandler() {
        // private constructor for utility class
    }

    public static String startAuth(HttpServletRequest request) throws UnsupportedEncodingException {
        String port = request.getServerPort() == 443 || request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
        String redirectUri = new StringBuilder(request.getScheme()).append("://").append(AbstractLogin.getDefaultTenant().getDnsName()).append(port).append(request.getContextPath()).append(IdpManagerServlet.FACEBOOK_AUTH_FINISH_URL).toString();
        StringBuilder sb = new StringBuilder(OAUTH_URL);
        sb.append(URLEncoder.encode(redirectUri, "UTF-8"));
        JsonObject state = new JsonObject();
        state.addProperty("tenant", request.getParameter(AbstractLogin.TENANT_NAME));
        if (request.getParameter(AbstractLogin.LOGIN_APP) != null) {
            state.addProperty("loginApp", request.getParameter(AbstractLogin.LOGIN_APP));
        }
        if (StringUtils.hasText(request.getParameter("r"))) {
            state.addProperty("redirectUrl", request.getParameter("r"));
        }
        sb.append("&state=").append(URLEncoder.encode(state.toString(), "UTF-8"));
        String url = sb.toString();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initializing FaceBook OAuth Request: {}", url);
        }
        return url;
    }

    public static String finishAuth(HttpServletRequest request) throws IOException {
        String state = request.getParameter("state");
        String redirectUrl = null;
        JsonElement element = new Gson().fromJson(state, JsonElement.class);
        JsonObject jsonState = element.getAsJsonObject();
        String tenant = jsonState.get("tenant").getAsString();
        if (jsonState.get("redirectUrl") != null) {
            redirectUrl = URLEncoder.encode(jsonState.get("redirectUrl").getAsString(), "UTF-8");
        }
        String error = request.getParameter("error");
        if (StringUtils.hasText(error)) {
            String code = "access_denied".equals(error) ? "403f" : "500f";
            StringBuilder url = new StringBuilder(HeaderFilter.LOGIN_ENDPOINT).append("?code=").append(code);
            if (StringUtils.hasText(redirectUrl)) {
                url.append("&r=").append(redirectUrl);
            }
            return url.toString();
        }
        String port = request.getServerPort() == 443 || request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
        String redirectUri = new StringBuilder(request.getScheme()).append("://").append(request.getServerName()).append(port).append(request.getContextPath()).append(IdpManagerServlet.FACEBOOK_AUTH_FINISH_URL).toString();
        String path = new StringBuilder(TOKEN_URL).append(URLEncoder.encode(redirectUri, "UTF-8")).append("&code=").append(request.getParameter("code")).toString();
        String ret = RestClient.get(path);
        FacebookAccessToken token = new Gson().fromJson(ret, FacebookAccessToken.class);
        String redirectEndpoint = HeaderFilter.LOGIN_ENDPOINT;
        if (jsonState.get("loginApp") != null && "admin".equals(jsonState.get("loginApp").getAsString())) {
            redirectEndpoint = HeaderFilter.LOGIN_ADMIN_ENDPOINT;
        }
        StringBuilder sb = new StringBuilder(redirectEndpoint).append("?i=").append(LoginType.FACEBOOK.ordinal()).append("&t=").append(token.getAccessToken()).append("&tenant=").append(tenant);
        if (StringUtils.hasText(redirectUrl)) {
            sb.append("&r=").append(redirectUrl);
        }
        return sb.toString();
    }
}
