package com.nextlabs.rms.cc.service;

import com.google.gson.JsonElement;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.cc.pojos.ControlCenterUser;
import com.nextlabs.rms.security.KeyStoreManagerImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public final class ControlCenterUserService extends ControlCenterRestClient {

    private static final String CREATE_ENDPOINT = "/console/api/v1/profile/user/add";

    ControlCenterUserService(ControlCenterRestClient rs) {
        super(rs);
    }

    String createUser(String tokenGroup) throws ControlCenterRestClientException, ControlCenterServiceException {

        String name;
        if (tokenGroup.endsWith(Constants.MEMBERSHIP_MODEL_SUFFIX)) {
            String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroup.substring(0, tokenGroup.length() - Constants.MEMBERSHIP_MODEL_SUFFIX.length())).getId();
            name = ControlCenterManager.escapeIllegalChars(keyStoreId) + Constants.MEMBERSHIP_MODEL_SUFFIX;
        } else {
            name = ControlCenterManager.escapeIllegalChars(new KeyStoreManagerImpl().getKeyStore(tokenGroup).getId());
        }
        //To create an old password
        ControlCenterUser user = new ControlCenterUser(name, getPassword((new StringBuilder(tokenGroup).reverse().toString()).concat(tokenGroup)), tokenGroup.toUpperCase(), "ADMIN");
        String serviceUrl = getConsoleUrl() + CREATE_ENDPOINT;
        ControlCenterResponse psResponse = doPost(serviceUrl, user, ControlCenterResponse.class);
        if (!CODE_1000.equals(psResponse.getStatusCode())) {
            if (CODE_6006.equals(psResponse.getStatusCode())) {
                LOGGER.debug("User already exists for tokenGroup: {} ", tokenGroup);
                return null;
            } else {
                throw new ControlCenterServiceException("Error occurred while creating user: " + psResponse.getStatusCode() + " - " + psResponse.getMessage());
            }
        }
        JsonElement data = psResponse.getData();
        LOGGER.debug("Created admin user for tokenGroup: {}", tokenGroup);
        return data.getAsString();
    }

    public void createUsers(List<String> tokenGroups)
            throws ControlCenterRestClientException, ControlCenterServiceException, ExecutionException,
            InterruptedException {
        List<ControlCenterUser> users = new ArrayList<>();
        for (String tokenGroup : tokenGroups) {
            String name;
            if (tokenGroup.endsWith(Constants.MEMBERSHIP_MODEL_SUFFIX)) {
                String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroup.substring(0, tokenGroup.length() - Constants.MEMBERSHIP_MODEL_SUFFIX.length())).getId();
                name = ControlCenterManager.escapeIllegalChars(keyStoreId) + Constants.MEMBERSHIP_MODEL_SUFFIX;
            } else {
                name = ControlCenterManager.escapeIllegalChars(new KeyStoreManagerImpl().getKeyStore(tokenGroup).getId());
            }
            ControlCenterUser user = new ControlCenterUser(name, getPassword(tokenGroup), tokenGroup.toUpperCase(), "ADMIN");
            users.add(user);
        }
        String serviceUrl = getConsoleUrl() + CREATE_ENDPOINT;
        List<ControlCenterAsyncResponse<ControlCenterResponse>> psResponses = doPostAsync(serviceUrl, users);
        for (ControlCenterAsyncResponse<ControlCenterResponse> async : psResponses) {
            ControlCenterUser user = (ControlCenterUser)async.getRequest();
            if (async.getResponse() == null) {
                LOGGER.error("Error occurred while creating user {0}: {1} {2}", user.getUsername(), async.getException().getMessage(), async.getException());
            } else {
                ControlCenterResponse psResponse = async.getResponse();
                if (!CODE_1000.equals(psResponse.getStatusCode())) {
                    if (CODE_6006.equals(psResponse.getStatusCode())) {
                        LOGGER.debug("User already exists: {} ", user.getUsername());
                    } else {
                        LOGGER.error("Error occurred while creating user: " + psResponse.getStatusCode() + " - " + psResponse.getMessage());
                    }
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created user: {}", user.getUsername());
                }
            }
        }
    }

    public void activateUser(String tokenGroup) throws ControlCenterRestClientException, ControlCenterServiceException {
        String name;
        if (tokenGroup.endsWith(Constants.MEMBERSHIP_MODEL_SUFFIX)) {
            String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroup.substring(0, tokenGroup.length() - Constants.MEMBERSHIP_MODEL_SUFFIX.length())).getId();
            name = ControlCenterManager.escapeIllegalChars(keyStoreId) + Constants.MEMBERSHIP_MODEL_SUFFIX;
        } else {
            name = ControlCenterManager.escapeIllegalChars(new KeyStoreManagerImpl().getKeyStore(tokenGroup).getId());
        }
        String url = getConsoleUrl() + "/cas/account/modifyPassword";
        HttpPost post = new HttpPost(url);

        //user name needs to be sent in post parameter
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("username", name));
        String pwd = getPassword((new StringBuilder(tokenGroup).reverse().toString()).concat(tokenGroup));
        String newPwd = getPassword(tokenGroup);
        //Old password can not be same as new password from cc 9.1 onwards
        urlParameters.add(new BasicNameValuePair("password", pwd));
        urlParameters.add(new BasicNameValuePair("newPassword", newPwd));
        urlParameters.add(new BasicNameValuePair("confirmPassword", newPwd));

        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters, "UTF-8"));
            HttpResponse response = HTTPCLIENT.execute(post, HttpClientContext.create());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new ControlCenterRestClientException(statusCode, EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }

    public void activateUsers(List<String> tokenGroups)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        for (String tokenGroup : tokenGroups) {
            activateUser(tokenGroup);
        }
    }
}
