package com.nextlabs.rms.idp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.servlet.HeaderFilter;
import com.nextlabs.rms.servlet.IdpManagerServlet;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GoogleOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String OAUTH_URL;

    private static final String APP_ID;

    private static final String APP_SECRET;

    static {
        GoogleIdpAttributes attributes = (GoogleIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.GOOGLE, null);
        OAUTH_URL = new StringBuilder("https://accounts.google.com/o/oauth2/v2/auth?client_id=").append(attributes.getAppId()).append("&response_type=code&prompt=select_account&scope=profile%20email&redirect_uri=").toString();
        APP_ID = attributes.getAppId();
        APP_SECRET = attributes.getAppSecret();
    }

    private GoogleOAuthHandler() {
        // private constructor for utility class
    }

    public static String startAuth(HttpServletRequest request) throws UnsupportedEncodingException {
        String port = request.getServerPort() == 443 || request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
        String redirectUri = new StringBuilder(request.getScheme()).append("://").append(AbstractLogin.getDefaultTenant().getDnsName()).append(port).append(request.getContextPath()).append(IdpManagerServlet.GOOGLE_AUTH_FINISH_URL).toString();
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
            LOGGER.trace("Initializing Google OAuth Request: {}", url);
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
            String code = "access_denied".equals(error) ? "403g" : "500g";
            StringBuilder url = new StringBuilder(HeaderFilter.LOGIN_ENDPOINT).append("?code=").append(code);
            if (StringUtils.hasText(redirectUrl)) {
                url.append("&r=").append(redirectUrl);
            }
            return url.toString();
        }
        String port = request.getServerPort() == 443 || request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
        String redirectUri = new StringBuilder(request.getScheme()).append("://").append(request.getServerName()).append(port).append(request.getContextPath()).append(IdpManagerServlet.GOOGLE_AUTH_FINISH_URL).toString();
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost("https://www.googleapis.com/oauth2/v3/token");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("client_id", APP_ID));
            nvps.add(new BasicNameValuePair("client_secret", APP_SECRET));
            nvps.add(new BasicNameValuePair("redirect_uri", redirectUri));
            nvps.add(new BasicNameValuePair("code", request.getParameter("code")));
            nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
            post.setEntity(new UrlEncodedFormEntity(nvps));
            response = client.execute(post);
            String responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOGGER.error("Error occured while exchanging code for token with Google Servers: {}", responseBody);
                throw new IOException("Error occured while exchanging code for token with Google Servers");
            }
            GoogleAccessToken token = new Gson().fromJson(responseBody, GoogleAccessToken.class);
            String redirectEndpoint = HeaderFilter.LOGIN_ENDPOINT;
            if (jsonState.get("loginApp") != null && "admin".equals(jsonState.get("loginApp").getAsString())) {
                redirectEndpoint = HeaderFilter.LOGIN_ADMIN_ENDPOINT;
            }
            StringBuilder sb = new StringBuilder(redirectEndpoint).append("?i=").append(LoginType.GOOGLE.ordinal()).append("&t=").append(token.getAccessToken()).append("&tenant=").append(tenant);
            if (StringUtils.hasText(redirectUrl)) {
                sb.append("&r=").append(redirectUrl);
            }
            return sb.toString();
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }
}
