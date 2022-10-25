package com.nextlabs.rms.repository.sharepoint;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.repository.sharepoint.online.SharePointOnlineOAuthException;
import com.nextlabs.rms.repository.sharepoint.response.SPRestErrorResponse;
import com.nextlabs.rms.repository.sharepoint.response.SPRestResult;
import com.nextlabs.rms.repository.sharepoint.response.SPRestResult.SPRestMessage;
import com.nextlabs.rms.repository.sharepoint.response.SharePointOAuthErrorResponse;
import com.nextlabs.rms.repository.sharepoint.response.SharePointTokenResponse;
import com.nextlabs.rms.repository.sharepoint.response.UserProfile;
import com.nextlabs.rms.shared.IHTTPResponseHandler;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.JWTVerifier;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SharePointRepoAuthHelper {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String IDENTIFIER = "00000003-0000-0ff1-ce00-000000000000";

    //TODO: Should this be here
    public static final String SP_ONLINE_APP_CONTEXT_ID = "SP_ONLINE_APP_CONTEXT_ID";

    private static final String SECURITY_TOKEN_SERVICE_URL = "https://accounts.accesscontrol.windows.net/tokens/OAuth/2";

    private SharePointRepoAuthHelper() {

    }

    public static UserProfile getUserProperties(String accessToken, String spServer)
            throws IOException, URISyntaxException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            StringBuilder builder = new StringBuilder(60);
            builder.append("https://").append(spServer).append("/_api/SP.UserProfiles.PeopleManager/GetMyProperties");
            URL url = convertToURLEscapingIllegalCharacters(builder.toString());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(url.toString());
            }
            HttpGet request = new HttpGet(url.toString());
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.addHeader("accept", "application/json;odata=verbose");
            response = client.execute(request);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Response code for getting user properties: {}", response.getStatusLine().getStatusCode());
            }
            IHTTPResponseHandler<UserProfile> handler = new SharePointResponseHandler<>(UserProfile.class);
            return handler.handle(response);
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }

    public static Map<String, String> getOAuthToken(HttpServletRequest request, String secret,
        RMSUserPrincipal userPrincipal) throws RMSException, RepositoryException {
        String accessToken = "";
        Map<String, String> tokens = null;
        try {
            tokens = new HashMap<>();
            String spAppToken = request.getParameter("SPAppToken");
            JWTVerifier verifier = new JWTVerifier(secret);
            Map<String, Object> decodedPayload = verifier.verify(spAppToken);
            String clientId = (String)decodedPayload.get("aud");
            String refreshToken = (String)decodedPayload.get("refreshtoken");
            String serverParameterName = request.getParameter("siteName");
            String spServer = serverParameterName;
            if (serverParameterName.indexOf('/') != -1) {
                spServer = serverParameterName.substring(0, serverParameterName.indexOf('/'));
            }

            if (userPrincipal == null) {
                //			    TODO: Why was this being done????
                //				SettingManager.saveSetting(userPrincipal.getTenantId(), SP_ONLINE_APP_CONTEXT_ID, clientId);
                //				ConfigManager.getInstance(userPrincipal.getTenantId()).loadConfigFromDB();
                LOGGER.error("user principal null, cannot get tenantId to save " + SP_ONLINE_APP_CONTEXT_ID);
            }
            SharePointTokenResponse tokenResponse = getAccessTokenFromRefreshToken(secret, clientId, refreshToken, spServer);
            accessToken = tokenResponse.getAccessToken();
            Long expiresOn = tokenResponse.getExpiresOn();
            tokens.put(RepositoryManager.ACCESS_TOKEN, accessToken);
            tokens.put(RepositoryManager.REFRESH_TOKEN, refreshToken);
            tokens.put(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME, String.valueOf(expiresOn));
            tokens.put(SP_ONLINE_APP_CONTEXT_ID, clientId);
            if (accessToken == null || accessToken.length() == 0) {
                LOGGER.error("oAuth Token is null");
            } else {
                LOGGER.debug("oAuth Token length is:" + accessToken.length());
            }
            return tokens;
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | IllegalStateException
                | SignatureException e) {
            LOGGER.error("Error occurred while getting oAuth Token", e);
            return tokens;
        }
    }

    public static SharePointTokenResponse getAccessTokenFromRefreshToken(String secret, String contextId,
        String refreshToken, String spServer)
            throws IOException, RepositoryException {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedRepositoryException("No refresh token");
        }
        String appId = contextId.split("@")[1];
        if (spServer.startsWith("https://")) {
            spServer = spServer.substring(8);
        }
        spServer = spServer.split("/")[0];
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
            HttpPost httppost = new HttpPost(SECURITY_TOKEN_SERVICE_URL);
            // Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
            params.add(new BasicNameValuePair("client_id", contextId));
            params.add(new BasicNameValuePair("client_secret", secret));
            params.add(new BasicNameValuePair("refresh_token", refreshToken));
            params.add(new BasicNameValuePair("resource", IDENTIFIER + "/" + spServer + "@" + appId));
            httppost.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse res = httpclient.execute(httppost)) {
                IHTTPResponseHandler<SharePointTokenResponse> handler = new SharePointResponseHandler<>(SharePointTokenResponse.class);
                return handler.handle(res);
            }
        } catch (SharePointOnlineOAuthException e) {
            handleException(e);
        } catch (IOException e) {
            handleException(e);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            handleException(e);
        }
        return null;
    }

    public static void handleException(Throwable e) throws RepositoryException {
        if (e instanceof SPRestServiceException) {
            SPRestServiceException ex = (SPRestServiceException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            SPRestErrorResponse error = ex.getError();
            if (error != null) {
                SPRestResult sperror = error.getError();
                if (sperror != null) {
                    SPRestMessage message = sperror.getMessage();
                    if (message != null) {
                        errorMsg = message.getValue();
                    }
                }
            }
            if (statusCode == 401 || statusCode == 403) {
                throw new UnauthorizedRepositoryException(errorMsg);
            } else if (statusCode == 404) {
                throw new com.nextlabs.rms.repository.exception.FileNotFoundException(errorMsg);
            } else if (statusCode == 400) {
                throw new com.nextlabs.rms.repository.exception.FolderCreationFailedException(errorMsg);
            }
            throw new RepositoryException(e.getMessage(), e);
        } else if (e instanceof SharePointOnlineOAuthException) {
            SharePointOnlineOAuthException ex = (SharePointOnlineOAuthException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            SharePointOAuthErrorResponse error = ex.getError();
            if (error != null) {
                errorMsg = error.getError();
            }
            if (statusCode == 400 && StringUtils.equals("invalid_grant", errorMsg)) {
                throw new InvalidTokenException(errorMsg, e);
            }
            throw new UnauthorizedRepositoryException(errorMsg, e);
        } else if (e instanceof RepositoryException) {
            throw (RepositoryException)e;
        } else {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    private static URL convertToURLEscapingIllegalCharacters(String string) throws URISyntaxException,
            MalformedURLException {
        //String decodedURL = URLDecoder.decode(string, "UTF-8");
        URL url = new URL(string);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        return uri.toURL();
    }
}
