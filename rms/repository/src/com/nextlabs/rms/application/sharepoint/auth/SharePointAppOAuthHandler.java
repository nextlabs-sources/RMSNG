package com.nextlabs.rms.application.sharepoint.auth;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.application.sharepoint.exception.InvalidHostNameException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointErrorResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointOAuthErrorResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointOAuthException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointServiceException;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.UnauthorizedApplicationRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.sharepoint.SharePointResponseHandler;
import com.nextlabs.rms.shared.IHTTPResponseHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SharePointAppOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger("com.nextlabs.rms.server");
    private static final String SHAREPOINT_APPLICATION_URL = "https://login.microsoftonline.com/";
    private static final String OAUTH_TOKEN_URL = "/oauth2/v2.0/token";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRANT_TYPE = "client_credentials";

    private SharePointAppOAuthHandler() {

    }

    public static SharePointAppTokenResponse getAccessToken(SharePointApplicationInfo info)
            throws ApplicationRepositoryException, RepositoryException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", info.clientId));
        params.add(new BasicNameValuePair("client_secret", info.clientSecret));
        params.add(new BasicNameValuePair("scope", SCOPE));
        params.add(new BasicNameValuePair("grant_type", GRANT_TYPE));

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(SHAREPOINT_APPLICATION_URL + info.tenantId + OAUTH_TOKEN_URL);
            post.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = client.execute(post)) {
                IHTTPResponseHandler<SharePointAppTokenResponse> handler = new SharePointResponseHandler<>(SharePointAppTokenResponse.class);
                return handler.handle(response);
            }
        } catch (SharePointServiceException | SharePointOAuthException e) {
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

    public static void handleException(Exception e) throws ApplicationRepositoryException, RepositoryException {
        if (e instanceof SharePointServiceException) {
            SharePointServiceException ex = (SharePointServiceException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            SharePointErrorResponse error = ex.getError();
            if (error != null) {
                SharePointResponse oneDriveError = error.getError();
                if (oneDriveError != null) {
                    errorMsg = oneDriveError.getMessage();
                }
            }
            LOGGER.info(" Error Message: " + errorMsg + " Status Code: " + statusCode);
            if (statusCode == 401) {
                throw new UnauthorizedApplicationRepositoryException(errorMsg);
            } else if (statusCode == 404) {
                throw new FileNotFoundException(errorMsg);
            } else if (statusCode == 507) {
                throw new InSufficientSpaceException(errorMsg);
            } else if (statusCode == 400 && errorMsg.startsWith("Invalid hostname ")) {
                throw new InvalidHostNameException(errorMsg);
            }

        } else if (e instanceof SharePointOAuthException) {
            SharePointOAuthException ex = (SharePointOAuthException)e;
            String errorMsg = ex.getMessage();
            int statusCode = ex.getStatusCode();
            SharePointOAuthErrorResponse error = ex.getError();
            if (error != null) {
                errorMsg = error.getError();
            }
            if (statusCode == 400 && StringUtils.equals("invalid_grant", errorMsg)) {
                throw new InvalidTokenException(errorMsg, e);
            }
            throw new UnauthorizedApplicationRepositoryException(errorMsg, e);
        } else if (e instanceof ApplicationRepositoryException) {
            throw (ApplicationRepositoryException)e;
        }
        throw new RepositoryException(e.getMessage(), e);
    }

    public static class SharePointApplicationInfo {

        String tenantId;
        String clientId;
        String clientSecret;

        public SharePointApplicationInfo(String tenantId, String clientId, String clientSecret) {
            this.tenantId = tenantId;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }
}
