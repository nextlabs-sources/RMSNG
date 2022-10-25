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
import com.nextlabs.rms.idp.FacebookIdpAttributes;
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

@Path("/login/fb")
public class FacebookLogin extends AbstractLogin {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String PROFILE_URL = "https://graph.facebook.com/v2.6/me?fields=name,email,gender,verified&access_token=";

    private String authUrl;

    @PostConstruct
    public void init() {
        FacebookIdpAttributes attributes = (FacebookIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.FACEBOOK, null);
        authUrl = "https://graph.facebook.com/debug_token?access_token=" + attributes.getAppId() + '|' + attributes.getAppSecret() + "&input_token=";
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

            String ret = RestClient.get(authUrl + token);
            TokenData data = GsonUtils.GSON.fromJson(ret, AuthResult.class).getData();
            if (!data.isValid()) {
                return Response.ok(new JsonResponse(403, "Invalid token").toJson(), MediaType.APPLICATION_JSON).build();
            }

            ret = RestClient.get(PROFILE_URL + token);
            Profile profile = GsonUtils.GSON.fromJson(ret, Profile.class);
            // TODO: should we check profile.isVerified() ?
            String email = profile.getEmail();
            if (!StringUtils.hasText(email)) {
                // Currently we only support email address. If user verified with phone only, it will not return email address
                return Response.ok(new JsonResponse(403, "Please verify your email in Facebook to log in to SkyDRM.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            String loginName = profile.getId();
            String name = profile.getName();
            email = email.toLowerCase(Locale.getDefault()).trim();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Facebook Access Token for email {} will expire in {}", email, Long.parseLong(data.getExpiration()) * 1000);
            }

            if (!StringUtils.hasText(name)) {
                name = email;
            }

            // Check account status and if username has been registered
            // Return error is account is disabled
            // Return error if approval is required
            Tenant tenant = getTenantByName(tenantName);
            FacebookIdpAttributes attr = IdpManager.getFacebookAttributes(tenant.getId());
            boolean enableApproval = false;
            String signupUrl = "";
            if (attr != null) {
                enableApproval = attr.isEnableApproval();
                signupUrl = attr.getSignupUrl();
            }

            try {
                validateUserAccount(loginName, Constants.LoginType.FACEBOOK, enableApproval);
            } catch (AccountNotActivatedException e) {
                if (enableApproval) {
                    Map<String, String> userParam = new HashMap<String, String>();
                    userParam.put("username", loginName);
                    if (email.isEmpty()) {
                        userParam.put("email", getUserAccountEmail(loginName, Constants.LoginType.FACEBOOK));
                    } else {
                        userParam.put("email", email);
                    }
                    userParam.put("display_name", name);
                    userParam.put("idp_type", "FACEBOOK");
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
                    userParam.put("idp_type", "FACEBOOK");
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
                userParam.put("idp_type", "FACEBOOK");
                String jsonData = new Gson().toJson(userParam);

                String redirectUrl = signupUrl + "?act=ap&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));
                //LOGGER.debug("redirectUrl: " +  redirectUrl);
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
            return Response.ok(new JsonResponse(503, "Failed to connect to Facebook").toJson(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "FacebookLogin", "login", error ? 0 : 1, clientId, platformId);
        }
    }

    @Override
    protected Constants.LoginType getLoginType() {
        return Constants.LoginType.FACEBOOK;
    }

    public static final class AuthResult {

        private TokenData data;

        public AuthResult() {
        }

        public void setData(TokenData data) {
            this.data = data;
        }

        public TokenData getData() {
            return data;
        }
    }

    public static final class TokenData {

        @SerializedName("app_id")
        private String appId;
        private String application;
        @SerializedName("expires_at")
        private String expiration;
        @SerializedName("is_valid")
        private boolean valid;
        @SerializedName("user_id")
        private String userId;
        private List<String> scopes;

        public TokenData() {
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppId() {
            return appId;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getApplication() {
            return application;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() {
            return valid;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public List<String> getScopes() {
            return scopes;
        }
    }

    public static final class Profile {

        private String id;
        private String name;
        private String email;
        private String gender;
        private boolean verified;

        public Profile() {
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getGender() {
            return gender;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public boolean isVerified() {
            return verified;
        }
    }
}
