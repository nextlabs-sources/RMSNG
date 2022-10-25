package com.nextlabs.rms.application.onedrive.authentication;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.UnauthorizedApplicationRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.onedrive.OneDriveResponseHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveTokenResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;

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

/**
 * This class will handle authentication details of Application One Drive
 */
public final class ApplicationOneDriveOAuthHandler {

    private static final String ONE_DRIVE_APPLICATION_URL = "https://login.microsoftonline.com/";
    private static final String OAUTH_TOKEN_URL = "/oauth2/v2.0/token";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRANT_TYPE = "client_credentials";

    private ApplicationOneDriveOAuthHandler() {

    }

    public static OneDriveTokenResponse getAccessToken(OneDriveApplicationInfo info)
            throws ApplicationRepositoryException, InSufficientSpaceException, InvalidTokenException,
            FileNotFoundException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", info.clientId));
        params.add(new BasicNameValuePair("client_secret", info.clientSecret));
        params.add(new BasicNameValuePair("scope", SCOPE));
        params.add(new BasicNameValuePair("grant_type", GRANT_TYPE));

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(ONE_DRIVE_APPLICATION_URL + info.tenantId + OAUTH_TOKEN_URL);
            post.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = client.execute(post)) {
                OneDriveResponseHandler<OneDriveTokenResponse> handler = new OneDriveResponseHandler<>(OneDriveTokenResponse.class);
                return handler.handle(response);
            }
        } catch (OneDriveServiceException | OneDriveOAuthException e) {
            handleException(e);
        } catch (IOException e) {
            //            if (logger.isDebugEnabled()) {
            //                logger.debug(e.getMessage(), e);
            //            }
            handleException(e);
        } catch (Exception e) {
            //            logger.error(e.getMessage(), e);
            handleException(e);
        }
        return null;
    }

    public static void handleException(Exception e) throws ApplicationRepositoryException, InvalidTokenException,
            InSufficientSpaceException, FileNotFoundException {
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
                throw new UnauthorizedApplicationRepositoryException(errorMsg);
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
            throw new UnauthorizedApplicationRepositoryException(errorMsg, e);
        } else if (e instanceof ApplicationRepositoryException) {
            throw (ApplicationRepositoryException)e;
        }
        throw new ApplicationRepositoryException(e.getMessage(), e);
    }

    public static class OneDriveApplicationInfo {

        String tenantId;
        String clientId;
        String clientSecret;

        public OneDriveApplicationInfo(String tenantId, String clientId, String clientSecret) {
            this.tenantId = tenantId;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }
}
