package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.idp.GoogleIdpAttributes;
import com.nextlabs.rms.idp.IdpManager;
import com.nextlabs.rms.rs.exception.AccountCheckFailException;
import com.nextlabs.rms.rs.exception.AccountDisabledException;
import com.nextlabs.rms.rs.exception.AccountNotActivatedException;
import com.nextlabs.rms.rs.exception.AccountNotApprovedException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
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

@Path("/login/google")
public class GoogleLogin extends AbstractLogin {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String AUTH_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final String USER_PROFILE_URL = "https://www.googleapis.com/oauth2/v3/userinfo?alt=json&access_token=";
    private String client;

    @PostConstruct
    public void init() {
        GoogleIdpAttributes attributes = (GoogleIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.GOOGLE, null);
        client = attributes.getAppId();
    }

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

            String ret = RestClient.get(AUTH_URL + token);
            GoogleIdToken idToken = GsonUtils.GSON.fromJson(ret, GoogleIdToken.class);
            if (!client.equals(idToken.getAud())) {
                return Response.ok(new JsonResponse(403, "Invalid token").toJson(), MediaType.APPLICATION_JSON).build();
            }
            if (!"true".equals(idToken.getEmailVerified())) {
                return Response.ok(new JsonResponse(403, "Please verify your email in Google to log in to SkyDRM.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            String loginName = idToken.getSub();
            String email = idToken.getEmail().toLowerCase(Locale.getDefault()).trim();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Google Access Token for email {} will expire in {}", email, Long.parseLong(idToken.getExp()) * 1000);
            }

            String response = RestClient.get(USER_PROFILE_URL + token);
            GoogleUserInfo userInfo = GsonUtils.GSON.fromJson(response, GoogleUserInfo.class);
            String name = userInfo.getName();
            if (!StringUtils.hasText(name)) {
                name = email;
            }

            // Check account status and if username has been registered
            // Return error is account is disabled
            // Return error if approval is required
            Tenant tenant = getTenantByName(tenantName);
            GoogleIdpAttributes attr = IdpManager.getGoogleAttributes(tenant.getId());
            String signupUrl = "";
            boolean enableApproval = false;
            if (attr != null) {
                enableApproval = attr.isEnableApproval();
                signupUrl = attr.getSignupUrl();
            }

            try {
                validateUserAccount(loginName, Constants.LoginType.GOOGLE, enableApproval);
            } catch (AccountNotActivatedException e) {
                if (enableApproval) {
                    Map<String, String> userParam = new HashMap<String, String>();
                    userParam.put("username", loginName);
                    if (email.isEmpty()) {
                        userParam.put("email", getUserAccountEmail(loginName, Constants.LoginType.GOOGLE));
                    } else {
                        userParam.put("email", email);
                    }
                    userParam.put("display_name", name);
                    userParam.put("idp_type", "GOOGLE");
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
                    userParam.put("display_name", name);
                    userParam.put("idp_type", "GOOGLE");
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
                userParam.put("display_name", name);
                userParam.put("idp_type", "GOOGLE");
                String jsonData = new Gson().toJson(userParam);

                String redirectUrl = signupUrl + "?act=ap&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));
                return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();

            } catch (AccountCheckFailException e) {
                return Response.ok(new JsonResponse(500, "Internal Server Error.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put(UserAttributeCacheItem.DISPLAYNAME, Collections.singletonList(name));
            attributes.put(UserAttributeCacheItem.EMAIL, Collections.singletonList(email));
            attributes.put(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE, Collections.singletonList(UserAttributeCacheItem.EMAIL));
            try (DbSession session = DbSession.newSession()) {
                error = false;
                return loginSuccessed(request, session, adminApp, loginName, email, name, attributes, tenantName, clientId, platformId, null);
            }
        } catch (IOException e) {
            return Response.ok(new JsonResponse(503, "Failed to connect to Google").toJson(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "GoogleLogin", "login", error ? 0 : 1, clientId, platformId);
        }
    }

    @Override
    protected Constants.LoginType getLoginType() {
        return Constants.LoginType.GOOGLE;
    }

    public static final class GoogleUserInfo {

        private String sub;
        private String name;
        @SerializedName("given_name")
        private String givenName;
        @SerializedName("family_name")
        private String familyName;
        private String profile;
        private String picture;
        private String email;
        @SerializedName("email_verified")
        private String emailVerified;
        private String gender;
        private String locale;

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getPicture() {
            return picture;
        }

        public void setPicture(String picture) {
            this.picture = picture;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(String emailVerified) {
            this.emailVerified = emailVerified;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }
    }

    public static final class GoogleIdToken {

        private String azp;
        private String aud;
        private String sub;
        private String scope;
        private String exp;
        @SerializedName("expires_in")
        private String expiresIn;
        private String email;
        @SerializedName("email_verified")
        private String emailVerified;
        @SerializedName("access_type")
        private String accessType;

        public String getAccessType() {
            return accessType;
        }

        public void setAccessType(String accessType) {
            this.accessType = accessType;
        }

        public String getAzp() {
            return azp;
        }

        public void setAzp(String azp) {
            this.azp = azp;
        }

        public String getAud() {
            return aud;
        }

        public void setAud(String aud) {
            this.aud = aud;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getExp() {
            return exp;
        }

        public void setExp(String exp) {
            this.exp = exp;
        }

        public String getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(String expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(String emailVerified) {
            this.emailVerified = emailVerified;
        }
    }
}
