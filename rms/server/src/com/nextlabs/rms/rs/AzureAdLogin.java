package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.microsoft.aad.msal4j.MsalInteractionRequiredException;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.idp.AzureAdIdpAttributes;
import com.nextlabs.rms.idp.IdpManager;
import com.nextlabs.rms.rs.exception.AccountCheckFailException;
import com.nextlabs.rms.rs.exception.AccountDisabledException;
import com.nextlabs.rms.rs.exception.AccountNotActivatedException;
import com.nextlabs.rms.rs.exception.AccountNotApprovedException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.SessionManagementUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/login/azuread")
public class AzureAdLogin extends AbstractLogin {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String AZURE_USER_SELECT = "?$select=" + "displayName,userPrincipalName,mail,givenName,surname,jobTitle,businessPhones,mobilePhone," + "usageLocation,birthdate,ageGroup,state,city,country,postalCode," + "streetAddress,companyName,department,officeLocation,otherMails,userType,id";

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpServletRequest request, @FormParam("token") String token,
        @FormParam("tenant") String tenantName) {
        boolean error = true;
        String clientId = null;
        int platformId = -1;
        try {
            if (token == null) {
                return Response.ok(new JsonResponse(400, "Missing required parameter").toJson(), MediaType.APPLICATION_JSON).build();
            }
            Cookie[] cookies = request.getCookies();
            platformId = DeviceType.WEB.getLow();
            boolean adminApp = false;
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (StringUtils.equals(cookie.getName(), PLATFORM_ID)) {
                        String value = cookie.getValue();
                        if (!org.apache.commons.lang3.StringUtils.isNumeric(value)) {
                            return Response.ok(new JsonResponse(400, "Invalid platform ID.").toJson(), MediaType.APPLICATION_JSON).build();
                        }
                        platformId = Integer.parseInt(value);
                    } else if (StringUtils.equals(cookie.getName(), CLIENT_ID)) {
                        String value = cookie.getValue();
                        if (value.length() > 32) {
                            return Response.ok(new JsonResponse(400, "Invalid client ID.").toJson(), MediaType.APPLICATION_JSON).build();
                        }
                        clientId = value;
                    } else if (StringUtils.equals(cookie.getName(), "adminApp")) {
                        adminApp = Boolean.parseBoolean(cookie.getValue());
                    }
                }
            }
            clientId = StringUtils.hasText(clientId) ? clientId : generateClientId(); //NOPMD

            String accessToken = SessionManagementUtil.getSessionAzureToken();

            // Get basic Azure user profile
            String jsonUserProfile = getAzureInfoFromGraph(accessToken, "/me" + AZURE_USER_SELECT);
            Gson gson = new Gson();
            @SuppressWarnings("unchecked")
            Map<String, Object> azureProfile = gson.fromJson(jsonUserProfile, Map.class);
            String displayName = getDataFromAzure(azureProfile, "displayName");
            String email = getDataFromAzure(azureProfile, "mail");
            String principalName = getDataFromAzure(azureProfile, "userPrincipalName");
            String loginName = getDataFromAzure(azureProfile, "id");
            //String adUserId = getDataFromAzure(azureProfile, "id");

            // Check account status and if username has been registered
            // Return error is account is disabled
            // Return error if approval is required
            Tenant tenant = getTenantByName(tenantName);
            AzureAdIdpAttributes attr = IdpManager.getAzureAttributes(tenant.getId());
            boolean enableApproval = false;
            String signupUrl = "";
            if (attr != null) {
                enableApproval = attr.isEnableApproval();
                signupUrl = attr.getSignupUrl();
            }

            try {
                validateUserAccount(loginName, Constants.LoginType.AZUREAD, enableApproval);
            } catch (AccountNotActivatedException e) {
                if (enableApproval) {
                    Map<String, String> userParam = new HashMap<String, String>();
                    userParam.put("username", loginName);
                    if (email.isEmpty()) {
                        userParam.put("email", getUserAccountEmail(loginName, Constants.LoginType.AZUREAD));
                    } else {
                        userParam.put("email", email);
                    }
                    userParam.put("display_name", displayName);
                    userParam.put("idp_type", "AZUREAD");
                    String jsonData = new Gson().toJson(userParam);

                    String redirectUrl = signupUrl + "?act=ac&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

                    return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();
                } else {
                    return Response.ok(new JsonResponse(303, "Your account is not activated. Please check your email or contact support for assistance.").toJson(), MediaType.APPLICATION_JSON).build();
                }
            } catch (AccountDisabledException e) {
                if (enableApproval) {
                    Map<String, String> userParam = new HashMap<String, String>();
                    userParam.put("username", loginName);
                    userParam.put("email", email);
                    userParam.put("display_name", displayName);
                    userParam.put("idp_type", "AZUREAD");
                    String jsonData = new Gson().toJson(userParam);

                    String redirectUrl = signupUrl + "?act=en&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

                    return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();
                } else {
                    return Response.ok(new JsonResponse(303, "Your account has been disabled. Please contact support for details.").toJson(), MediaType.APPLICATION_JSON).build();
                }
            } catch (AccountNotApprovedException e) {

                Map<String, String> userParam = new HashMap<String, String>();
                userParam.put("username", loginName);
                userParam.put("email", email);
                userParam.put("display_name", displayName);
                userParam.put("idp_type", "AZUREAD");
                String jsonData = new Gson().toJson(userParam);

                String redirectUrl = signupUrl + "?act=ap&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

                return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();

            } catch (AccountCheckFailException e) {
                return Response.ok(new JsonResponse(500, "Internal Server Error.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            // Policy User Attributes
            Map<String, List<String>> userAttributes = new HashMap<>();
            userAttributes.put(UserAttributeCacheItem.DISPLAYNAME, Arrays.asList(displayName));
            userAttributes.put(UserAttributeCacheItem.EMAIL, Arrays.asList(email));

            // IDP User Mapping
            Map<String, String> userAttributeMap = IdpManager.getUserAttributeMap(tenant.getId(), Constants.LoginType.AZUREAD);
            for (Map.Entry<String, String> entry : userAttributeMap.entrySet()) {

                // Skip these two attributes as they are already added by default
                if (entry.getKey().equals(UserAttributeCacheItem.DISPLAYNAME) || entry.getKey().equals(UserAttributeCacheItem.EMAIL)) {
                    continue;
                }

                String data = getDataFromAzure(azureProfile, entry.getValue());
                userAttributes.put(entry.getKey(), Arrays.asList(data));

            }

            String evalUserIdAttribute = attr.getEvalUserIdAttribute();
            if (StringUtils.hasText(evalUserIdAttribute)) {
                userAttributes.put(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE, Collections.singletonList(evalUserIdAttribute));
                userAttributes.putAll(IdpManager.getEAPAttributes(userAttributes.get(evalUserIdAttribute)));
            }

            try (DbSession session = DbSession.newSession()) {
                error = false;

                String userEmail = email.isEmpty() ? principalName : email;

                return loginSuccessed(request, session, adminApp, loginName, userEmail, displayName, userAttributes, tenantName, clientId, platformId, null);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(503, "Failed to connect to AzureAd").toJson(), MediaType.APPLICATION_JSON).build();
        } catch (MsalInteractionRequiredException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(503, "Failed to connect to AzureAd").toJson(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "AzureAdLogin", "login", error ? 0 : 1, clientId, platformId);
        }
    }

    private String getDataFromAzure(Map<String, Object> map, String key) {

        Object obj = map.get(key);
        if (obj == null) {
            return "";
        }

        if (obj instanceof String) {
            return (String)obj;
        }

        return "";
    }

    private String getAzureInfoFromGraph(String accessToken, String urlString) throws IOException {

        HttpURLConnection conn = null;
        try {

            //URL url = new URL(AzureAdIdpAttributes.GRAPH_ENDPOINT + "v1.0/me" + AZURE_USER_SELECT);        	        
            URL url = new URL(AzureAdIdpAttributes.GRAPH_ENDPOINT + "v1.0" + urlString);
            conn = (HttpURLConnection)url.openConnection();

            // Set the appropriate header fields in the request header.
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                String error = IOUtils.toString(conn.getErrorStream());
                throw new IOException(error);
            }

            String jsonData = IOUtils.toString(conn.getInputStream());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("AzureLogin>>>>getAzureInfoFromGraph: {}", jsonData);
            }
            return jsonData;
        } finally {
            IOUtils.close(conn);
        }
    }

    /*
    private ConfidentialClientApplication createClientApplication() throws MalformedURLException {
        return ConfidentialClientApplication.builder(appID, ClientCredentialFactory.createFromSecret(appSecret)).
                authority(authUrl).build();
    }
    */

    @Override
    protected Constants.LoginType getLoginType() {
        return Constants.LoginType.AZUREAD;
    }

}
