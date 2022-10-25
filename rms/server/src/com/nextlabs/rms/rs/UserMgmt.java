package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nextlabs.captcha.Captcha;
import com.nextlabs.captcha.CaptchaConfig;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.Constants.Status;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonUser;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.AuthUtils;
import com.nextlabs.common.util.ByteUtils;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCache;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.command.GetInitSettingsCommand;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.ApiUserCert;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.LoginAccount;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserPreferences;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.idp.IdpManager;
import com.nextlabs.rms.idp.LocalIdpAttributes;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.mail.Mail;
import com.nextlabs.rms.mail.Sender;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.CookieUtil;
import com.nextlabs.rms.util.RestUploadUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;

@Path("/usr")
public class UserMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int MAX_ATTEMPT = 3;
    public static final String GROUP_NAME_PUBLIC = "Public";
    public static final String PROFILE_PICTURE = "profile_picture";

    private boolean debugMode;

    @PostConstruct
    public void init() {
        WebConfig config = WebConfig.getInstance();
        debugMode = Boolean.parseBoolean(config.getProperty(WebConfig.DEBUG, "false"));
    }

    @GET
    @Path("/captcha")
    @Produces(MediaType.APPLICATION_JSON)
    public String captcha(@Context HttpServletRequest request) {
        boolean error = true;
        try {
            CaptchaConfig config = CaptchaConfig.getInstance();

            Captcha captcha = new Captcha();
            String text = captcha.randomText(5);
            BufferedImage img = captcha.createCaptcha(text);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            bos.close();
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("captcha", Base64Codec.encodeAsString(bos.toByteArray()));

            int time = (int)(System.currentTimeMillis() / 1000);
            byte[] timeBuf = new byte[4];
            ByteUtils.writeInt(timeBuf, time);
            byte[] captchaKey = config.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] textBuf = StringUtils.toBytesQuietly(text);
            byte[] hmac = AuthUtils.hmac(IKeyStoreManager.ALG_HMAC_SHA256, textBuf, timeBuf, captchaKey);
            byte[] nonce = new byte[AuthUtils.HMAC_SHA256_LENGTH + 4];
            System.arraycopy(hmac, 0, nonce, 0, AuthUtils.HMAC_SHA256_LENGTH);
            System.arraycopy(timeBuf, 0, nonce, AuthUtils.HMAC_SHA256_LENGTH, 4);
            resp.putResult("nonce", Hex.toHexString(nonce));
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "captcha", error ? 0 : 1);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpServletRequest request, @FormParam("email") String email,
        @FormParam("password") String password, @FormParam("rememberMe") Boolean rememberMe,
        @FormParam("tenant") String tenant) {
        boolean error = true;
        boolean adminApp = false;
        String clientId = null;
        int platformId = -1;
        try {
            if (tenant != null) {
                tenant = URLDecoder.decode(tenant, StandardCharsets.UTF_8.name());
            }
            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                return Response.ok(new JsonResponse(400, "Missing required parameters.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            email = email.toLowerCase(Locale.getDefault()).trim();
            Cookie[] cookies = request.getCookies();
            platformId = DeviceType.WEB.getLow();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (StringUtils.equals(cookie.getName(), AbstractLogin.PLATFORM_ID)) {
                        String value = cookie.getValue();
                        if (!org.apache.commons.lang3.StringUtils.isNumeric(value)) {
                            return Response.ok(new JsonResponse(400, "Invalid platform ID.").toJson(), MediaType.APPLICATION_JSON).build();
                        }
                        platformId = Integer.parseInt(value);
                    } else if (StringUtils.equals(cookie.getName(), AbstractLogin.CLIENT_ID)) {
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
            clientId = StringUtils.hasText(clientId) ? clientId : AbstractLogin.generateClientId(); //NOPMD

            try (DbSession session = DbSession.newSession()) {

                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("loginName", email));
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
                LoginAccount account = (LoginAccount)criteria.uniqueResult();
                if (account == null) {
                    return Response.ok(new JsonResponse(404, "The account does not exist.").toJson(), MediaType.APPLICATION_JSON).build();
                }
                int attempt = account.getAttempt();
                if (attempt > MAX_ATTEMPT) {
                    Date lastAttempt = account.getLastAttempt();
                    long blockTime = (long)(DateUtils.MILLIS_PER_MINUTE * Math.pow(2, (double)attempt - MAX_ATTEMPT));
                    long remainingBlockTime = blockTime - (System.currentTimeMillis() - lastAttempt.getTime());
                    if (remainingBlockTime > 0) {
                        String sb = "Your account has been temporarily locked due to repeated failed login attempts. Try logging in again after " + getDurationBreakdown(remainingBlockTime) + " or contact SkyDRM support.";
                        return Response.ok(new JsonResponse(401, sb).toJson(), MediaType.APPLICATION_JSON).build();
                    }
                }
                if (!Arrays.equals(account.getPassword(), AuthUtils.hmac(email, password))) {
                    session.beginTransaction();
                    account.setAttempt(attempt + 1);
                    account.setLastAttempt(new Date());
                    session.commit();
                    return Response.ok(new JsonResponse(401, "You've entered an incorrect username/password.").toJson(), MediaType.APPLICATION_JSON).build();
                }

                Tenant loginTenant = TenantMgmt.getTenantByName(session, tenant);
                LocalIdpAttributes attr = IdpManager.getLocalAttributes(loginTenant.getId());
                boolean enableApproval = false;
                String signupUrl = "";
                if (attr != null) {
                    enableApproval = attr.isEnableApproval();
                    signupUrl = attr.getSignupUrl();
                }

                if (account.getStatus() == Status.DISABLED.ordinal() && !debugMode) {
                    if (enableApproval) {
                        Map<String, String> userParam = new HashMap<String, String>();
                        userParam.put("username", account.getLoginName());
                        userParam.put("email", email);
                        if (account.getUser() != null) {
                            userParam.put("display_name", account.getUser().getDisplayName());
                        } else {
                            userParam.put("display_name", email);
                        }
                        userParam.put("idp_type", "DB");
                        String jsonData = new Gson().toJson(userParam);

                        String redirectUrl = signupUrl + "?act=en&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

                        return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();
                    } else {
                        return Response.ok(new JsonResponse(303, "Your account has been disabled. Please contact support for assistance.").toJson(), MediaType.APPLICATION_JSON).build();
                    }

                }

                if (account.getStatus() != Status.ACTIVE.ordinal() && !debugMode) {

                    if (enableApproval) {
                        Map<String, String> userParam = new HashMap<String, String>();
                        userParam.put("username", account.getLoginName());
                        userParam.put("email", email);
                        if (account.getUser() != null) {
                            userParam.put("display_name", account.getUser().getDisplayName());
                        } else {
                            userParam.put("display_name", email);
                        }
                        userParam.put("idp_type", "DB");
                        String jsonData = new Gson().toJson(userParam);

                        String redirectUrl = signupUrl + "?act=ac&param=" + Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

                        return Response.ok(new JsonResponse(307, redirectUrl).toJson(), MediaType.APPLICATION_JSON).build();
                    } else {
                        return Response.ok(new JsonResponse(303, "Your account is not activated.").toJson(), MediaType.APPLICATION_JSON).build();
                    }
                }

                session.beginTransaction();
                User user = UserMgmt.linkUser(session, account, loginTenant, null, false);
                if (user == null) {
                    return Response.ok(new JsonResponse(403, "Auto provision is not allowed.").toJson(), MediaType.APPLICATION_JSON).build();
                }

                account.setAttempt(0);
                account.setLastLogin(new Date());
                session.commit(true);

                DefaultRepositoryManager.getInstance().createDefaultRepository(session, user.getId(), AbstractLogin.getDefaultTenant().getId());
                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put(UserAttributeCacheItem.DISPLAYNAME, Collections.singletonList(user.getDisplayName()));
                attributes.put(UserAttributeCacheItem.EMAIL, Collections.singletonList(email));
                if (adminApp && !loginTenant.isAdmin(user.getEmail())) {
                    return Response.ok(new JsonResponse(403, "Cannot login as admin.").toJson(), MediaType.APPLICATION_JSON).build();
                }
                error = false;
                return createResponse(request, session, user, attributes, loginTenant, account, debugMode, clientId, platformId, LoginType.DB, rememberMe, null);
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error.").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "login", error ? 0 : 1, clientId, platformId);
        }
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("forceLogout") Boolean forceLogout) {
        boolean error = true;
        try {
            try (DbSession session = DbSession.newSession()) {
                UserSession userSession = null;
                if (userId >= 0 && StringUtils.hasText(ticket)) {
                    userSession = getUserSession(session, userId, ticket, clientId, platformId);
                } else {
                    String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket", "clientId", "platformId");
                    if (params != null) {
                        userId = Integer.parseInt(params[0]);
                        ticket = params[1];
                        clientId = params[2];
                        platformId = Integer.parseInt(params[3]);
                    }
                    if (userId >= 0 && StringUtils.hasText(ticket)) {
                        userSession = getUserSession(session, userId, ticket, clientId, platformId);
                    }
                }
                if (userSession != null) {
                    session.beginTransaction();
                    if (Boolean.TRUE.equals(forceLogout)) {
                        Query query = session.createNamedQuery("deleteByUser");
                        query.setParameter("userId", userId);
                        query.setParameter("userType", User.Type.SYSTEM);
                        query.executeUpdate();
                    } else {
                        session.delete(userSession);
                    }
                    session.commit();
                    String cacheKey = UserAttributeCacheItem.getKey(userSession.getUser().getId(), userSession.getClientId());
                    RMSCacheManager.getInstance().getUserAttributeCache().remove(cacheKey);
                }
                Cookie[] cookies = request.getCookies();
                NewCookie[] newCookies = null;
                if (cookies != null) {
                    final String domain = CookieUtil.getCookieDomainName(request);
                    newCookies = new NewCookie[cookies.length];
                    int i = 0;
                    for (Cookie cookie : cookies) {
                        String name = cookie.getName();
                        newCookies[i] = new NewCookie(name, null, "/", domain, null, 0, cookie.getSecure());
                        ++i;
                    }
                }
                error = false;
                return Response.ok(new JsonResponse("OK").toJson(), MediaType.APPLICATION_JSON).cookie(newCookies).build();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error.").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "logout", !error ? 1 : 0, userId, platformId);
        }
    }

    @Secured
    @GET
    @Path("/init")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInitData(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        try {
            GetInitSettingsCommand command = new GetInitSettingsCommand();
            JsonObject jsonObj = command.getInitSettingsJSON(request, response);
            JsonResponse jsonResponse = new JsonResponse("OK");
            jsonResponse.putResult("initData", jsonObj);
            return jsonResponse.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        }
    }

    @GET
    @Path("/basic/{user_id}/{ticket}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getBasicProfile(@Context HttpServletRequest request, @PathParam("user_id") int userId,
        @PathParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        UserSession userSession;
        if (userId < 0 || !StringUtils.hasText(ticket)) {
            String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket", "clientId", "platformId");
            if (params != null) {
                userId = Integer.parseInt(params[0]);
                ticket = params[1];
                clientId = params[2];
                platformId = Integer.parseInt(params[3]);
            }
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                return new JsonResponse(401, "Missing login parameters").toJson();
            }
        }
        try (DbSession session = DbSession.newSession()) {
            userSession = authenticate(session, userId, ticket, clientId, platformId);
            if (userSession == null) {
                return new JsonResponse(401, "Authentication failed.").toJson();
            }
        }
        return getBasicProfileUser(request, userSession, userId, 1);

    }

    @Secured
    @GET
    @Path("/v2/basic")
    @Produces(MediaType.APPLICATION_JSON)
    public String getBasicProfileV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        return getBasicProfileUser(request, us, userId, 2);
    }

    public String getBasicProfileUser(HttpServletRequest request, UserSession userSession, int userId, int version) {
        boolean error = true;
        try {
            Set<String> emails;
            String displayName;
            try (DbSession session = DbSession.newSession()) {
                User user = userSession.getUser();
                emails = getUserEmails(session, user);
                displayName = user.getDisplayName();
            }
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("displayName", displayName);
            resp.putResult("emails", emails);
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "getBasicProfile" + version, error ? 0 : 1, userId);
        }
    }

    @GET
    @Path("/profile/{user_id}/{ticket}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProfile(@Context HttpServletRequest request, @PathParam("user_id") int userId,
        @PathParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        UserSession us;
        if (userId < 0 || !StringUtils.hasText(ticket)) {
            String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket", "clientId", "platformId");
            if (params != null) {
                userId = Integer.parseInt(params[0]);
                ticket = params[1];
                clientId = params[2];
                platformId = Integer.parseInt(params[3]);
            }
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                return new JsonResponse(401, "Missing login parameters").toJson();
            }
        }
        try (DbSession session = DbSession.newSession()) {
            us = authenticate(session, userId, ticket, clientId, platformId);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed.").toJson();
            }
        }
        return getProfileUser(request, us, userId, 2);
    }

    @Secured
    @GET
    @Path("/v2/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProfileV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        return getProfileUser(request, us, userId, 2);
    }

    public String getProfileUser(HttpServletRequest request, UserSession userSession, int userId, int version) {
        boolean error = true;
        try {
            try (DbSession session = DbSession.newSession()) {
                error = false;
                return createResponse(session, userSession, userSession.getLoginType().ordinal()).toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "getProfile" + version, error ? 0 : 1, userId);
        }
    }

    @GET
    @Path("/memberships")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMemberships(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("q") String projectIds,
        @QueryParam("tokenGroupName") String tokenGroupName) {
        boolean error = true;
        try {
            boolean remote = false;
            if (StringUtils.hasText(tokenGroupName)) {
                KeyManager km = new KeyManager(new KeyStoreManagerImpl());
                Key key = km.getKey(tokenGroupName, IKeyStoreManager.PREFIX_DH + tokenGroupName);
                remote = key == null;
            } else {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket", "clientId", "platformId", "tenant");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    ticket = params[1];
                    clientId = params[2];
                    platformId = Integer.parseInt(params[3]);
                    tokenGroupName = params[4];
                }
                if (StringUtils.hasText(tokenGroupName)) {
                    KeyManager km = new KeyManager(new KeyStoreManagerImpl());
                    Key key = km.getKey(tokenGroupName, IKeyStoreManager.PREFIX_DH + tokenGroupName);
                    remote = key == null;
                }
            }
            List<JsonMembership> memberships;
            if (remote) {
                JsonResponse resp = UserMgmt.getRemoteUserProfile(userId, ticket, tokenGroupName, clientId, platformId);
                if (resp.hasError()) {
                    return resp.toJson();
                }
                List<String> emails = resp.getResult("emails", GsonUtils.GENERIC_LIST_TYPE);
                try (DbSession session = DbSession.newSession()) {
                    memberships = getMemberships(session, emails);
                }
            } else {
                try (DbSession session = DbSession.newSession()) {
                    Tenant defaultTenant = AbstractLogin.getDefaultTenant();
                    if (defaultTenant == null) {
                        return new JsonResponse(503, "Service is not initialized yet.").toJson();
                    }
                    UserSession us = authenticate(session, userId, ticket, clientId, platformId);
                    if (!StringUtils.hasText(projectIds)) {
                        memberships = getMemberships(session, userId, null, us);
                    } else {
                        List<String> projectIdList = StringUtils.tokenize(projectIds, ",");
                        List<Integer> intList = new ArrayList<>(projectIdList.size());
                        for (String s : projectIdList) {
                            intList.add(Integer.valueOf(s));
                        }
                        memberships = getMemberships(session, userId, intList, us);
                    }
                }
            }

            JsonResponse resp = new JsonResponse("Ok");
            resp.putResult("memberships", memberships);
            error = false;
            return resp.toJson();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Timeout occurred when retrieving membership (user ID: {}, platform: {}, tokenGroup: {}): {}", userId, platformId, tokenGroupName, e.getMessage(), e);
            }
            return new JsonResponse(500, "Timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO error occurred when retrieving membership (user ID: {}, platform: {}, tokenGroup: {}): {}", userId, platformId, tokenGroupName, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "getMemberships", error ? 0 : 1, userId, projectIds);
        }
    }

    @Secured
    @POST
    @Path("/tour/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateTour(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("type") String tourType, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            if (req.getParameter(tourType) == null) {
                return new JsonResponse(401, "Missing required parameters.").toJson();
            }
            boolean finishTour = Boolean.parseBoolean(req.getParameter(tourType));
            try (DbSession session = DbSession.newSession()) {
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                Map<String, Object> prefsSavedMap;
                UserPreferences userPreferences = session.get(UserPreferences.class, user.getId());
                String prefsSaved = userPreferences.getPreferences();
                if (StringUtils.hasText(prefsSaved)) {
                    prefsSavedMap = GsonUtils.GSON.fromJson(prefsSaved, GsonUtils.GENERIC_MAP_TYPE);
                } else {
                    prefsSavedMap = new HashMap<>();
                }
                session.beginTransaction();
                prefsSavedMap.put(tourType, finishTour);
                userPreferences.setPreferences(GsonUtils.GSON.toJson(prefsSavedMap));
                session.update(userPreferences);
                session.commit();
            }

            error = false;
            return new JsonResponse("User updated.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "updateTour", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateProfile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            String displayName = req.getParameter("displayName");
            if (StringUtils.hasText(displayName)) {
                if (displayName.length() > 150) {
                    return new JsonResponse(5001, "Display name is too long.").toJson();
                } else if (!RegularExpressions.PROFILE_DISPLAY_NAME_PATTERN.matcher(displayName).matches()) {
                    return new JsonResponse(5002, "Display name contains illegal special characters").toJson();
                }
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                session.beginTransaction();
                if (StringUtils.hasText(displayName)) {
                    user.setDisplayName(displayName);
                    session.update(user);
                }
                JsonElement prefsIn = req.getWrappedParameter("preferences");
                if (prefsIn != null) {
                    JsonObject o = new JsonParser().parse(prefsIn.toString()).getAsJsonObject();

                    Map<String, Object> prefsSavedMap;
                    UserPreferences userPreferences = session.get(UserPreferences.class, user.getId());
                    String prefsSaved = userPreferences.getPreferences();
                    if (StringUtils.hasText(prefsSaved)) {
                        prefsSavedMap = GsonUtils.GSON.fromJson(prefsSaved, GsonUtils.GENERIC_MAP_TYPE);
                    } else {
                        prefsSavedMap = new HashMap<>();
                    }

                    for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
                        prefsSavedMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                    userPreferences.setPreferences(GsonUtils.GSON.toJson(prefsSavedMap));
                    session.update(userPreferences);
                }

                session.commit();
            }
            error = false;
            return new JsonResponse("User updated.").toJson();
        } catch (IllegalArgumentException | JsonParseException | DataException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "updateProfile", error ? 0 : 1, userId);
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String register(@Context HttpServletRequest request, @FormParam("email") String email,
        @FormParam("password") String password, @FormParam("displayName") String displayName,
        @FormParam("nonce") String nonce, @FormParam("captcha") String captcha,
        @FormParam("id") String invitationId, @FormParam("code") String invitationCode,
        @FormParam("tenant") String tenantName) {
        boolean error = true;
        try {
            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }

            JsonResponse resp = validateCaptcha(nonce, captcha);
            if (resp.getStatusCode() != 200) {
                return resp.toJson();
            }

            LOGGER.warn("[Registration][v1] " + email + " received from " + request.getRemoteAddr());

            email = email.toLowerCase(Locale.getDefault()).trim();
            if (email.length() > 150) {
                return new JsonResponse(5004, "Email is too long.").toJson();
            }
            if (!RegularExpressions.EMAIL_PATTERN.matcher(email).matches()) {
                return new JsonResponse(5003, "Invalid Email").toJson();
            }
            if (StringUtils.hasText(displayName)) {
                if (displayName.length() > 150) {
                    return new JsonResponse(5001, "Display name is too long.").toJson();
                    //} else if (!RegularExpressions.PROFILE_DISPLAY_NAME_PATTERN.matcher(displayName).matches()) {
                    //    return new JsonResponse(5002, "Display name contains illegal special characters").toJson();
                }
            } else {
                displayName = email;
            }
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("loginName", email));
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
                LoginAccount account = (LoginAccount)criteria.uniqueResult();
                if (account != null) {
                    if (account.getStatus() == Status.PENDING.ordinal()) {
                        return new JsonResponse(303, "Your account is not activated.").toJson();
                    }
                    return new JsonResponse(304, "User already exists.").toJson();
                }

                session.beginTransaction();
                Date now = new Date();
                account = new LoginAccount();
                account.setLoginName(email);
                account.setPassword(AuthUtils.hmac(email, password));
                account.setOtp(KeyManager.randomBytes(16));
                account.setEmail(email);
                account.setCreationTime(now);
                account.setStatus(Status.PENDING.ordinal());

                Tenant tenant;
                if (StringUtils.hasText(tenantName)) {
                    Criteria criteria1 = session.createCriteria(Tenant.class);
                    criteria1.add(Restrictions.eq("name", tenantName));
                    tenant = (Tenant)criteria1.uniqueResult();
                } else {
                    tenant = AbstractLogin.getDefaultTenant();
                }
                if (tenant == null) {
                    return new JsonResponse(503, "Service is not initialized yet.").toJson();
                }
                User user = linkUser(session, account, tenant, displayName, false);
                if (user == null) {
                    return new JsonResponse(403, "Auto provision is not allowed.").toJson();
                }
                session.commit();

                if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                    sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
                } else {
                    String[] params = CookieUtil.getParamsFromCookies(request, "id", "code");
                    if (params != null) {
                        invitationId = params[0];
                        invitationCode = params[1];
                    }
                    if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                        sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
                    } else {
                        sendEmail("register", request, user, account, invitationId, invitationCode);
                    }
                }
            }
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "register", error ? 0 : 1);
        }
    }

    @POST
    @Path("/v2/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String register(@Context HttpServletRequest request, @FormParam("email") String email,
        @FormParam("password") String password, @FormParam("displayName") String displayName,
        @FormParam("nonce") String nonce, @FormParam("captcha") String captcha,
        @FormParam("id") String invitationId, @FormParam("code") String invitationCode,
        @FormParam("tenant") String tenantName, @FormParam("idp_type") String idpType,
        @FormParam("login_name") String loginName) {
        boolean error = true;
        try {

            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }

            JsonResponse resp = validateCaptcha(nonce, captcha);
            if (resp.getStatusCode() != 200) {
                return resp.toJson();
            }

            LOGGER.warn("[New Registration][v2] " + email + " received from " + request.getRemoteAddr());

            email = email.toLowerCase(Locale.getDefault()).trim();
            if (email.length() > 150) {
                return new JsonResponse(5004, "Email is too long.").toJson();
            }
            if (!RegularExpressions.EMAIL_PATTERN.matcher(email).matches()) {
                return new JsonResponse(5003, "Invalid Email").toJson();
            }
            if (StringUtils.hasText(displayName)) {
                if (displayName.length() > 150) {
                    return new JsonResponse(5001, "Display name is too long.").toJson();
                    //} else if (!RegularExpressions.PROFILE_DISPLAY_NAME_PATTERN.matcher(displayName).matches()) {
                    //    return new JsonResponse(5002, "Display name contains illegal special characters").toJson();
                }
            } else {
                displayName = email;
            }
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("loginName", loginName));
                criteria.add(Restrictions.eq("type", getIdpType(idpType)));
                LoginAccount account = (LoginAccount)criteria.uniqueResult();
                if (account != null) {
                    if (account.getStatus() == Status.PENDING.ordinal()) {
                        return new JsonResponse(303, "Your account is not activated.").toJson();
                    }
                    return new JsonResponse(304, "User already exists.").toJson();
                }

                session.beginTransaction();
                Date now = new Date();
                account = new LoginAccount();
                account.setLoginName(loginName);
                account.setPassword(AuthUtils.hmac(email, password));
                account.setOtp(KeyManager.randomBytes(16));
                account.setEmail(email);
                account.setCreationTime(now);
                account.setType(getIdpType(idpType));
                account.setStatus(Status.PENDING.ordinal());

                Tenant tenant;
                if (StringUtils.hasText(tenantName)) {
                    Criteria criteria1 = session.createCriteria(Tenant.class);
                    criteria1.add(Restrictions.eq("name", tenantName));
                    tenant = (Tenant)criteria1.uniqueResult();
                } else {
                    tenant = AbstractLogin.getDefaultTenant();
                }
                if (tenant == null) {
                    return new JsonResponse(503, "Service is not initialized yet.").toJson();
                }
                User user = linkUser(session, account, tenant, displayName, false);
                if (user == null) {
                    return new JsonResponse(403, "Auto provision is not allowed.").toJson();
                }
                session.commit();

                if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                    sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
                } else {
                    String[] params = CookieUtil.getParamsFromCookies(request, "id", "code");
                    if (params != null) {
                        invitationId = params[0];
                        invitationCode = params[1];
                    }
                    if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                        sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
                    } else {
                        sendEmail("register", request, user, account, invitationId, invitationCode);
                    }
                }
            }
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "register", error ? 0 : 1);
        }
    }

    private int getIdpType(String idpType) {
        if (idpType == null || idpType.isEmpty()) {
            return Constants.LoginType.DB.ordinal();
        }
        switch (idpType) {
            case "GOOGLE":
                return Constants.LoginType.GOOGLE.ordinal();
            case "FACEBOOK":
                return Constants.LoginType.FACEBOOK.ordinal();
            case "AZUREAD":
                return Constants.LoginType.AZUREAD.ordinal();
            default:
                return Constants.LoginType.DB.ordinal();
        }
    }

    @POST
    @Path("/unregister")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(@Context HttpServletRequest req, @FormParam("account_id") int accountId,
        @FormParam("otp") String otp, @FormParam("suc_url") String redirUrl) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            LoginAccount account = session.get(LoginAccount.class, accountId);
            if (account == null) {
                URI uri = createURI(req, redirUrl, "?code=404");
                return Response.seeOther(uri).build();
            }

            byte[] accountOtp = account.getOtp();

            account.setOtp(null);
            session.commit(true);

            if (!Arrays.equals(accountOtp, Hex.toByteArray(otp))) {
                URI uri = createURI(req, redirUrl, "?code=403");
                return Response.seeOther(uri).build();
            }

            if (account.getStatus() != Status.PENDING.ordinal()) {
                URI uri = createURI(req, redirUrl, "?code=303");
                return Response.seeOther(uri).build();
            }

            User user = session.get(User.class, account.getUserId());
            session.delete(user);

            session.commit();
            URI uri = createURI(req, redirUrl, "?code=204");
            error = false;
            return Response.seeOther(uri).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(500).entity(new JsonResponse(400, "Internal Server Error").toJson()).build();
        } finally {
            Audit.audit(req, "API", "UserMgmt", "unregister", error ? 0 : 1);
        }
    }

    @POST
    @Path("/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activate(@Context HttpServletRequest req, @FormParam("account_id") int accountId,
        @FormParam("otp") String otp, @FormParam("suc_url") String redirUrl) {
        boolean error = true;
        try {
            JsonResponse validateResp = validateLoggedInUserForActivation(req, accountId);
            if (validateResp.getStatusCode() == 4001) {
                URI uri = createURI(req, "/activate?account_id=" + accountId + "&otp=" + otp + "&suc_url=" + HTTPUtil.encode(redirUrl), "&code=4001");
                return Response.seeOther(uri).build();
            }

            if (!StringUtils.hasText(otp)) {
                return Response.status(400).entity("Malformed request.").build();
            }
            User user;
            try (DbSession session = DbSession.newSession()) {
                LoginAccount account = session.get(LoginAccount.class, accountId);
                if (account == null) {
                    URI uri = createURI(req, redirUrl, "?code=404");
                    return Response.seeOther(uri).build();
                }
                byte[] accountOtp = account.getOtp();

                if (!Arrays.equals(accountOtp, Hex.toByteArray(otp))) {
                    URI uri = createURI(req, redirUrl, "?code=403");
                    return Response.seeOther(uri).build();
                }

                session.beginTransaction();
                account.setOtp(null);
                user = session.get(User.class, account.getUserId());
                account.setStatus(Status.ACTIVE.ordinal());
                session.commit();
            }

            long ttl = DateUtils.addDaysAsMilliseconds(1);
            int maxAge = (int)TimeUnit.MILLISECONDS.toSeconds(ttl);
            NewCookie[] cookies = new NewCookie[1];
            final String domainName = CookieUtil.getCookieDomainName(req);
            cookies[0] = new NewCookie("userEmail", String.valueOf(user.getEmail()), "/", domainName, "", maxAge, !debugMode);
            URI uri = createURI(req, redirUrl, "?code=200");
            error = false;
            return Response.seeOther(uri).cookie(cookies).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(500).entity(new JsonResponse(400, "Internal Server Error.").toJson()).build();
        } finally {
            Audit.audit(req, "API", "UserMgmt", "activate", error ? 0 : 1);
        }
    }

    @GET
    @Path("/resetPassword")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleGetForResetPassword(@Context HttpServletRequest req, @QueryParam("account_id") int accountId,
        @QueryParam("otp") String otp, @QueryParam("suc_url") String redirUrl) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            if (!StringUtils.hasText(otp)) {
                return Response.status(400).entity("Malformed request.").build();
            }
            LoginAccount account = session.get(LoginAccount.class, accountId);
            if (account == null) {
                URI uri = createURI(req, redirUrl, "?code=404");
                return Response.seeOther(uri).build();
            }
            byte[] accountOtp = account.getOtp();

            if (!Arrays.equals(accountOtp, Hex.toByteArray(otp))) {
                URI uri = createURI(req, redirUrl, "?code=403");
                return Response.seeOther(uri).build();
            }

            User user = session.get(User.class, account.getUserId());
            // FIXME better name for resetPasswordEmailLink

            Tenant tenant = AbstractLogin.getDefaultTenant();

            int maxAge = -1;
            final String domainName = CookieUtil.getCookieDomainName(req);
            NewCookie[] cookies = new NewCookie[4];
            cookies[0] = new NewCookie("userId", String.valueOf(user.getId()), "/", domainName, "", maxAge, !debugMode);
            cookies[1] = new NewCookie("tenantId", tenant.getId(), "/", domainName, "", maxAge, !debugMode);
            cookies[2] = new NewCookie("userEmail", user.getEmail(), "/", domainName, "", maxAge, !debugMode);
            cookies[3] = new NewCookie("otp", otp, "/", domainName, "", maxAge, !debugMode);
            URI uri = createURI(req, redirUrl, "?code=200");
            error = false;
            return Response.seeOther(uri).cookie(cookies).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(500).entity(new JsonResponse(400, "Internal Server Error.").toJson()).build();
        } finally {
            Audit.audit(req, "API", "UserMgmt", "resetPasswordEmailLink", error ? 0 : 1);
        }
    }

    @POST
    @Path("/forgotPassword")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String forgotPassword(@Context HttpServletRequest req, @FormParam("email") String email,
        @FormParam("nonce") String nonce, @FormParam("captcha") String captcha) {
        boolean error = true;
        try {
            if (!StringUtils.hasText(email)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }

            if (!debugMode) {
                JsonResponse resp = validateCaptcha(nonce, captcha);
                if (resp.getStatusCode() != 200) {
                    return resp.toJson();
                }
            }

            email = email.toLowerCase(Locale.getDefault()).trim();
            User user;
            LoginAccount account;
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("loginName", email));
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
                criteria.setFetchMode("user", FetchMode.JOIN);
                account = (LoginAccount)criteria.uniqueResult();
                if (account == null) {
                    return new JsonResponse(404, "The account does not exist.").toJson();
                } else if (account.getStatus() == Status.PENDING.ordinal()) {
                    return new JsonResponse(303, "Your account is not activated.").toJson();
                }

                session.beginTransaction();
                user = account.getUser();
                account.setOtp(KeyManager.randomBytes(16));
                session.commit();
            }
            sendEmail("resetPwd", req, user, account, null, null);
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(req, "API", "UserMgmt", "forgotPassword", error ? 0 : 1);
        }
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/resetPassword")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String resetPassword(@Context HttpServletRequest request, @FormParam("userId") int userId,
        @FormParam("newPassword") String newPassword, @FormParam("otp") String otp) {
        boolean error = true;
        try {
            if (userId < 0 || !StringUtils.hasText(newPassword) || !StringUtils.hasText(otp)) {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "otp");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    otp = params[1];
                }
                if (userId < 0 || !StringUtils.hasText(newPassword) || !StringUtils.hasText(otp)) {
                    return new JsonResponse(400, "Missing required parameters.").toJson();
                }
            }
            LoginAccount account;
            User user;
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("userId", userId));
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
                criteria.setFetchMode("user", FetchMode.JOIN);
                account = (LoginAccount)criteria.uniqueResult();
                if (account == null) {
                    return new JsonResponse(404, "The account does not exist.").toJson();
                }
                user = account.getUser();
                byte[] accountOtp = account.getOtp();

                if (!Arrays.equals(accountOtp, Hex.toByteArray(otp))) {
                    return new JsonResponse(401, "This link has expired. Please request a new one.").toJson();
                }
                session.beginTransaction();
                account.setOtp(null);

                String email = account.getEmail();
                account.setAttempt(0);
                account.setPassword(AuthUtils.hmac(email, newPassword));
                account.setOtp(null);

                Criteria c = session.createCriteria(UserSession.class);
                c.add(Restrictions.eq("user.id", userId));
                c.add(Restrictions.eq("loginType", LoginType.DB));
                List<UserSession> list = c.list();
                if (!list.isEmpty()) {
                    for (UserSession userSession : list) {
                        session.delete(userSession);
                    }
                }
                session.commit();
            }
            sendEmail("pwdUpdated", request, user, account, null, null);
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "resetPassword", error ? 0 : 1, userId);
        }
    }

    @POST
    @Path("/changePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                String jsonResp = new JsonResponse(400, "Missing request.").toJson();
                return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
            }

            String oldPassword = req.getParameter("oldPassword");
            String newPassword = req.getParameter("newPassword");
            if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
                String jsonResp = new JsonResponse(400, "Missing required parameters.").toJson();
                return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
            }
            if (userId < 0 || !StringUtils.hasText(ticket) || !StringUtils.hasText(clientId)) {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket", "clientId", "platformId");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    ticket = params[1];
                    clientId = params[2];
                    platformId = Integer.parseInt(params[3]);
                }
                if (userId < 0 || !StringUtils.hasText(ticket) || !StringUtils.hasText(clientId)) {
                    String jsonResp = new JsonResponse(401, "Missing login parameters.").toJson();
                    return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
                }
            }
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(LoginAccount.class);
                criteria.add(Restrictions.eq("userId", userId));
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
                LoginAccount account = (LoginAccount)criteria.uniqueResult();
                if (account == null) {
                    String jsonResp = new JsonResponse(404, "The account does not exist.").toJson();
                    return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
                }
                final String domainName = CookieUtil.getCookieDomainName(request);
                int attempt = account.getAttempt();
                if (attempt > MAX_ATTEMPT) {
                    Date lastAttempt = account.getLastAttempt();
                    long blockTime = (long)(DateUtils.MILLIS_PER_MINUTE * Math.pow(2, (double)attempt - 3));
                    if (System.currentTimeMillis() - lastAttempt.getTime() < blockTime) {
                        NewCookie[] cookies = new NewCookie[2];
                        cookies[0] = new NewCookie("userId", null, "/", domainName, "", 0, !debugMode);
                        cookies[1] = new NewCookie("ticket", null, "/", domainName, "", 0, !debugMode);
                        String jsonResp = new JsonResponse(4002, "Too many attempts.").toJson();
                        return Response.ok(jsonResp, MediaType.APPLICATION_JSON).cookie(cookies).build();
                    }
                }
                // authentication happens after checking for MAX_ATTEMPT so that a user with expired TTL will not be redirected to /timeout
                UserSession us = authenticate(session, userId, ticket, clientId, platformId);
                if (us == null) {
                    String jsonResp = new JsonResponse(401, "Authentication failed.").toJson();
                    return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
                }
                if (!LoginType.DB.equals(us.getLoginType())) {
                    String jsonResp = new JsonResponse(403, "Access denied.").toJson();
                    return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
                }
                User user = us.getUser();
                if (!Arrays.equals(account.getPassword(), AuthUtils.hmac(user.getEmail(), oldPassword))) {
                    session.beginTransaction();
                    account.setAttempt(attempt + 1);
                    account.setLastAttempt(new Date());
                    if (attempt > MAX_ATTEMPT) {
                        revokeUserSession(session, user, LoginType.DB);
                    }
                    session.commit();
                    String jsonResp = new JsonResponse(4001, "Incorrect password.").toJson();
                    return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
                }

                session.beginTransaction();

                Criteria c = session.createCriteria(UserSession.class);
                c.add(Restrictions.eq("user.id", userId));
                c.add(Restrictions.eq("loginType", LoginType.DB));
                @SuppressWarnings("unchecked")
                List<UserSession> list = c.list();
                if (!list.isEmpty()) {
                    for (UserSession userSession : list) {
                        if (!userSession.getId().equals(us.getId())) {
                            session.delete(userSession);
                        }
                    }
                }

                String email = account.getEmail();
                account.setAttempt(0);
                account.setPassword(AuthUtils.hmac(email, newPassword));
                account.setOtp(null);
                us.setTicket(KeyManager.randomBytes(16));
                session.update(us);
                session.commit();

                sendEmail("pwdUpdated", request, user, account, null, null);
                final String newTicket = Hex.toHexString(us.getTicket());
                long ttl = us.getTtl();
                int maxAge = (int)(ttl >= 0 ? TimeUnit.MILLISECONDS.toSeconds(ttl - System.currentTimeMillis()) : -1);
                NewCookie[] cookies = new NewCookie[1];
                cookies[0] = new NewCookie("ticket", newTicket, "/", domainName, "", maxAge, !debugMode);
                JsonResponse resp = new JsonResponse("OK");
                JsonUser ju = new JsonUser();
                ju.setTicket(newTicket);
                ju.setTtl(ttl);

                String cacheKey = UserAttributeCacheItem.getKey(us.getUser().getId(), us.getClientId());
                UserAttributeCacheItem item = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
                if (item != null) {
                    Map<String, List<String>> attributes = item.getUserAttributes();
                    RMSCacheManager.getInstance().getUserAttributeCache().remove(cacheKey);
                    if (attributes != null && !attributes.isEmpty()) {
                        RMSCacheManager.getInstance().getUserAttributeCache().put(cacheKey, item, ttl, TimeUnit.MILLISECONDS);
                        ju.setAttributes(attributes);
                    }
                }

                resp.setExtra(ju);
                error = false;
                return Response.ok(resp.toJson(), MediaType.APPLICATION_JSON).cookie(cookies).build();

            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            String jsonResp = new JsonResponse(400, "Malformed request.").toJson();
            return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            String jsonResp = new JsonResponse(500, "Internal Server Error.").toJson();
            return Response.ok(jsonResp, MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "changePassword", error ? 0 : 1, userId);
        }
    }

    @POST
    @Path("/resendEmail")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String resendEmail(@Context HttpServletRequest request, @FormParam("email") String email,
        @FormParam("type") String type, @FormParam("id") String invitationId,
        @FormParam("code") String invitationCode, @FormParam("idp_type") String idpType,
        @FormParam("login_name") String loginName) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            if (!StringUtils.hasText(email) || !StringUtils.hasText(type)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }

            email = email.toLowerCase(Locale.getDefault()).trim();

            Criteria criteria = session.createCriteria(LoginAccount.class);
            if (StringUtils.hasText(loginName)) {
                criteria.add(Restrictions.eq("loginName", loginName));
            } else {
                criteria.add(Restrictions.eq("loginName", email));
            }
            if (StringUtils.hasText(idpType)) {
                criteria.add(Restrictions.eq("type", getIdpType(idpType)));
            } else {
                criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
            }
            criteria.setFetchMode("user", FetchMode.JOIN);
            LoginAccount account = (LoginAccount)criteria.uniqueResult();
            if (account == null) {
                return new JsonResponse(404, "The account does not exist.").toJson();
            }

            if (account.getStatus() == Status.ACTIVE.ordinal() && "register".equals(type)) {
                return new JsonResponse(4000, "The account has already been activated.").toJson();
            }

            session.beginTransaction();
            // update otp for every mail
            account.setOtp(KeyManager.randomBytes(16));
            User user = account.getUser();
            session.commit();

            if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
            } else {
                String[] params = CookieUtil.getParamsFromCookies(request, "id", "code");
                if (params != null) {
                    invitationId = params[0];
                    invitationCode = params[1];
                }
                if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
                    sendEmail("registerInvitation", request, user, account, invitationId, invitationCode);
                } else {
                    sendEmail(type, request, user, account, invitationId, invitationCode);
                }
            }
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "resendEmail", error ? 0 : 1);
        }
    }

    @Secured
    @GET
    @Path("/preference")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPreference(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            UserPreferences userPreferences = session.get(UserPreferences.class, userId);
            JsonResponse resp = new JsonResponse("OK");
            JsonExpiry expiry = new Gson().fromJson(userPreferences.getExpiry(), JsonExpiry.class);
            resp.putResult("expiry", expiry);
            resp.putResult("watermark", userPreferences.getWatermark());
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "getPreference", error ? 0 : 1, userId);
        }
    }

    @Secured
    @PUT
    @Path("/preference")
    @Consumes(MediaType.APPLICATION_JSON)
    public String updatePreference(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request.").toJson();
        }
        try (DbSession session = DbSession.newSession()) {
            JsonObject expiryJson = (JsonObject)req.getWrappedParameter(com.nextlabs.rms.Constants.PARAM_EXPIRY);
            String watermark = req.getParameter(com.nextlabs.rms.Constants.PARAM_WATERMARK);
            if (expiryJson == null && !StringUtils.hasText(watermark)) {
                return new JsonResponse(401, "Missing required parameters.").toJson();
            }
            if (watermark != null) {
                if (watermark.length() > 50) {
                    return new JsonResponse(4001, "Watermark too long").toJson();
                } else if ("".equals(watermark)) {
                    return new JsonResponse(4001, "Watermark empty").toJson();
                }
            }
            Map<String, Object> prefsSavedMap = null;
            if (expiryJson != null) {
                try {
                    prefsSavedMap = ExpiryUtil.validateExpiry(expiryJson);
                    if (prefsSavedMap == null) {
                        return new JsonResponse(4003, "Invalid expiry.").toJson();
                    }
                } catch (NumberFormatException | IllegalStateException e) {
                    return new JsonResponse(4002, "Invalid parameters.").toJson();
                }
            }
            UserPreferences userPreferences = session.get(UserPreferences.class, userId);
            if (prefsSavedMap != null) {
                userPreferences.setExpiry(GsonUtils.GSON.toJson(prefsSavedMap));
            }
            userPreferences.setWatermark(watermark);
            session.beginTransaction();
            session.update(userPreferences);
            session.commit();
            error = false;
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "updatePreference", error ? 0 : 1, userId);
        }
        return new JsonResponse(200, "OK").toJson();
    }

    public static int getLoggedInUser(HttpServletRequest req) {
        int userId = -1;
        String ticket = "";
        Cookie[] cookies = req.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if ("userId".equals(cookie.getName())) {
                    userId = Integer.parseInt(cookie.getValue());
                } else if ("ticket".equals(cookie.getName())) {
                    ticket = cookie.getValue();
                }
                if (userId >= 0 && StringUtils.hasText(ticket)) {
                    return userId;
                }
            }
        }
        return userId;
    }

    public static JsonResponse validateLoggedInUserForActivation(HttpServletRequest req, int accountId) {
        int userId = getLoggedInUser(req);
        if (userId >= 0) {
            try (DbSession session = DbSession.newSession()) {
                LoginAccount account = session.get(LoginAccount.class, accountId);
                User user = session.get(User.class, userId);
                if (account == null || user == null) {
                    return new JsonResponse(404, "The account does not exist.");
                }
                if (userId != account.getUserId()) {
                    String activationUserEmail = account.getEmail();
                    String loggedInUserEmail = user.getEmail();
                    return new JsonResponse(4001, RMSMessageHandler.getClientString("activationAlertMsg", loggedInUserEmail, activationUserEmail));
                }
            }
        }
        return new JsonResponse("OK");
    }

    public static User linkUser(DbSession session, LoginAccount account, Tenant tenant, String displayName,
        boolean forceAdd) {
        int userId = account.getUserId();
        if (userId > 0) {
            // Account already exist
            User user = session.get(User.class, account.getUserId());
            Criteria criteria = session.createCriteria(Membership.class);
            criteria.add(Restrictions.eq("user.id", userId));
            criteria.add(Restrictions.eq("tenant.id", tenant.getId()));
            List<?> memberships = criteria.list();
            if (!memberships.isEmpty()) {
                return user;
            }

            if (!allowAutoProvision(tenant)) {
                LOGGER.warn("Auto provision is not allowed: {}", tenant.getName());
                return null;
            }

            Date now = new Date();
            String tenantName = tenant.getName();
            Membership membership = new Membership();
            membership.setName(generateMemberName(session, tenantName));
            membership.setUser(user);
            membership.setTenant(tenant);
            membership.setCreationTime(now);
            membership.setLastModified(now);
            membership.setProjectActionTime(now);
            membership.setStatus(com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE);
            membership.setType(TokenGroupType.TOKENGROUP_TENANT);
            session.save(membership);

            return user;
        }

        if (!forceAdd && !allowAutoProvision(tenant)) {
            LOGGER.warn("Auto provision is not allowed: {}", tenant.getName());
            return null;
        }

        User user = searchLinkedUser(session, account);
        if (user == null) {
            user = createNewUser(session, tenant, displayName, account.getEmail());
            createNewUserPref(session, user);
        }
        account.setUser(user);
        session.save(account);
        return user;
    }

    public static JsonResponse createResponse(DbSession session, UserSession us, Integer idpType)
            throws UnsupportedEncodingException {
        User user = us.getUser();
        int userId = user.getId();
        String ticket = Hex.toHexString(us.getTicket());
        JsonResponse resp = new JsonResponse("Authorized");
        JsonUser ju = new JsonUser();
        ju.setTicket(ticket);
        ju.setUserId(userId);
        ju.setTenantId(AbstractLogin.getDefaultTenant().getId());
        long ttl = us.getTtl();
        if (ttl < 0) {
            ttl = DateUtils.addDaysAsMilliseconds(7);
        }
        ju.setTtl(ttl);
        ju.setName(user.getDisplayName());
        ju.setEmail(user.getEmail());
        if (idpType != null) {
            ju.setIdpType(idpType);
        }

        Tenant defaultTenant = AbstractLogin.getDefaultTenant();
        String defaultTenantUrl = TokenGroupCache.lookupTokenGroup(defaultTenant.getName());
        ju.setDefaultTenant(defaultTenant.getName());
        ju.setDefaultTenantUrl(defaultTenantUrl);
        ju.setTokenGroupName(session.get(KeyStoreEntry.class, defaultTenant.getKeystore().getId()).getTokenGroupName());
        UserPreferences userPreferences = session.get(UserPreferences.class, userId);
        String preferences = userPreferences.getPreferences();
        if (StringUtils.hasText(preferences)) {
            JsonParser parser = new JsonParser();
            ju.setPreferences(parser.parse(preferences));
        }
        String cacheKey = UserAttributeCacheItem.getKey(us.getUser().getId(), us.getClientId());
        UserAttributeCacheItem item = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
        if (item != null) {
            Map<String, List<String>> attributes = item.getUserAttributes();
            if (attributes != null && !attributes.isEmpty()) {
                attributes.remove(UserAttributeCacheItem.ADPASS);
                ju.setAttributes(attributes);
            }
        }
        List<JsonMembership> memberships = getMemberships(session, userId, null, us);
        ju.setMemberships(memberships);
        resp.setExtra(ju);
        return resp;
    }

    @SuppressWarnings("unchecked")
    public static Response createResponse(HttpServletRequest request, DbSession session, User user,
        Map<String, List<String>> attributes, Tenant loginTenant, LoginAccount account, boolean debugMode,
        String clientId, int platformId, LoginType loginType, Boolean rememberMe, Long customTTL)
            throws UnsupportedEncodingException {
        int userId = user.getId();
        long ttl;
        DeviceType deviceType = DeviceType.getDeviceType(platformId);
        UserSession userSession = null;
        if (deviceType != DeviceType.WEB) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date time = cal.getTime();
            Criteria criteria = session.createCriteria(UserSession.class);
            criteria.add(Restrictions.eq("user.id", user.getId()));
            criteria.add(Restrictions.eq("clientId", clientId));
            criteria.add(Restrictions.eq("deviceType", platformId));
            criteria.add(Restrictions.eq("loginType", loginType));
            criteria.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
            criteria.add(Restrictions.ge("expirationTime", time));
            criteria.addOrder(Order.desc("expirationTime"));
            criteria.setMaxResults(1);
            List<UserSession> list = criteria.list();
            if (!list.isEmpty()) {
                userSession = list.get(0);
            }
        }
        if (userSession == null) {
            if (customTTL != null) {
                ttl = new Date().getTime() + customTTL;
            } else {
                if (loginType == LoginType.DB) {
                    if (deviceType == DeviceType.WEB) {
                        ttl = Boolean.TRUE.equals(rememberMe) ? DateUtils.addDaysAsMilliseconds(14) : DateUtils.addDaysAsMilliseconds(1);
                    } else {
                        ttl = DateUtils.addDaysAsMilliseconds(30);
                    }
                } else {
                    if (deviceType == DeviceType.WEB) {
                        ttl = DateUtils.addDaysAsMilliseconds(1);
                    } else {
                        ttl = DateUtils.addDaysAsMilliseconds(30);
                    }
                }
            }
            byte[] ticket = KeyManager.randomBytes(16);
            userSession = new UserSession();
            userSession.setClientId(clientId);
            userSession.setCreationTime(new Date());
            userSession.setTtl(ttl);
            userSession.setExpirationTime(new Date(ttl));
            userSession.setStatus(UserSession.Status.ACTIVE);
            userSession.setDeviceType(platformId);
            userSession.setTicket(ticket);
            userSession.setLoginType(loginType);
            userSession.setUser(user);
            userSession.setLoginTenant(loginTenant.getId());
            session.beginTransaction();
            session.save(userSession);
            session.commit();
        } else {
            ttl = userSession.getTtl();
        }
        String ticketHex = Hex.toHexString(userSession.getTicket());

        Tenant defaultTenant = AbstractLogin.getDefaultTenant();
        String defaultTenantUrl = TokenGroupCache.lookupTokenGroup(defaultTenant.getName());

        JsonResponse resp = new JsonResponse("Authorized");
        JsonUser ju = new JsonUser();
        ju.setTicket(ticketHex);
        ju.setUserId(userId);
        ju.setTenantId(defaultTenant.getId());
        ju.setLt(loginTenant.getName());
        ju.setLtId(loginTenant.getId());
        ju.setTokenGroupName(session.get(KeyStoreEntry.class, defaultTenant.getKeystore().getId()).getTokenGroupName());
        ju.setTtl(ttl);
        ju.setName(user.getDisplayName());
        ju.setEmail(user.getEmail());
        ju.setIdpType(account.getType());
        ju.setDefaultTenant(defaultTenant.getName());
        ju.setDefaultTenantUrl(defaultTenantUrl);
        UserPreferences userPreferences = session.get(UserPreferences.class, userId);
        String preferences = userPreferences.getPreferences();
        if (StringUtils.hasText(preferences)) {
            JsonParser parser = new JsonParser();
            ju.setPreferences(parser.parse(preferences));
        }
        if (attributes != null && !attributes.isEmpty()) {
            String cacheKey = UserAttributeCacheItem.getKey(userId, clientId);
            UserAttributeCacheItem item = new UserAttributeCacheItem();
            item.setUserAttributes(attributes);
            RMSCacheManager.getInstance().getUserAttributeCache().put(cacheKey, item, ttl, TimeUnit.MILLISECONDS);
            ju.setAttributes(attributes);
        }
        List<JsonMembership> memberships = getMemberships(session, userId, null, userSession);
        ju.setMemberships(memberships);
        resp.setExtra(ju);
        final String domainName = CookieUtil.getCookieDomainName(request);
        int maxAge = (int)(ttl >= 0 ? TimeUnit.MILLISECONDS.toSeconds(ttl - System.currentTimeMillis()) : -1);
        NewCookie[] cookies = new NewCookie[8];
        cookies[0] = new NewCookie("userId", String.valueOf(user.getId()), "/", domainName, "", maxAge, !debugMode);
        cookies[1] = new NewCookie("ticket", ticketHex, "/", domainName, "", maxAge, !debugMode);
        cookies[2] = new NewCookie("clientId", clientId, "/", domainName, "", maxAge, !debugMode);
        cookies[3] = new NewCookie("tenantId", defaultTenant.getId(), "/", domainName, "", maxAge, !debugMode);
        cookies[4] = new NewCookie("idp", String.valueOf(account.getType()), "/", domainName, "", maxAge, !debugMode);
        cookies[5] = new NewCookie("lt", URLEncoder.encode(loginTenant.getName(), StandardCharsets.UTF_8.name()), "/", domainName, "", (int)TimeUnit.MINUTES.toSeconds(5) + maxAge, !debugMode);
        cookies[6] = new NewCookie("ltId", loginTenant.getId(), "/", domainName, "", (int)TimeUnit.MINUTES.toSeconds(5) + maxAge, !debugMode);
        cookies[7] = new NewCookie("platformId", String.valueOf(platformId), "/", domainName, "", maxAge, !debugMode);
        return Response.ok(resp.toJson(), MediaType.APPLICATION_JSON).cookie(cookies).build();
    }

    public static List<JsonMembership> getMemberships(DbSession session, int userId, List<Integer> projectIdList,
        UserSession us) {
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("user.id", userId));
        criteria.add(Restrictions.eq("status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
        if (projectIdList != null && !projectIdList.isEmpty()) {
            criteria.add(Restrictions.in("project.id", projectIdList));
        }
        List<?> list = criteria.list();
        List<JsonMembership> memberships = new ArrayList<>(list.size());
        for (Object obj : list) {
            Membership membership = (Membership)obj;
            Optional<JsonMembership> jsonMembershipOptional = createJsonMembership(session, membership);
            jsonMembershipOptional.ifPresent(memberships::add);
        }
        if (us != null) {
            Set<Integer> abacProjects = ProjectService.getABACProjectIDs(session, us);
            if (projectIdList != null && !projectIdList.isEmpty()) {
                abacProjects.retainAll(new HashSet<>(projectIdList));
            }
            for (Integer abacProjectId : abacProjects) {
                Project abacProject = session.get(Project.class, abacProjectId);
                memberships.add(new JsonMembership(generateDynamicMemberName(userId, abacProject.getKeystore().getTokenGroupName()), TokenGroupType.TOKENGROUP_PROJECT.ordinal(), null, abacProject.getId(), abacProject.getKeystore().getTokenGroupName()));
            }
            KeyStoreManagerImpl km = new KeyStoreManagerImpl();
            SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            KeyStoreEntry keyStoreEntry = km.getKeyStore(sbm.constructSystemBucketName(loginTenant.getName()));
            if (keyStoreEntry != null) {
                memberships.add(new JsonMembership(generateDynamicMemberName(userId, keyStoreEntry.getTokenGroupName()), TokenGroupType.TOKENGROUP_SYSTEMBUCKET.ordinal(), null, null, keyStoreEntry.getTokenGroupName()));
            }
        }
        return memberships;
    }

    @SuppressWarnings("unchecked")
    private static List<JsonMembership> getMemberships(DbSession session, Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Collections.emptyList();
        }
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.createAlias("user", "u");
        criteria.add(Restrictions.in("u.email", emails));
        criteria.add(Restrictions.eq("status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
        List<Membership> list = criteria.list();
        List<JsonMembership> memberships = new ArrayList<>(list.size());
        for (Membership membership : list) {
            Optional<JsonMembership> jsonMembershipOptional = createJsonMembership(session, membership);
            jsonMembershipOptional.ifPresent(memberships::add);
        }
        return memberships;
    }

    private static Optional<JsonMembership> createJsonMembership(DbSession session, Membership membership) { //NOPMD
        if (membership.getType() == TokenGroupType.TOKENGROUP_TENANT) {
            KeyStoreEntry tenantKeyStore = session.get(KeyStoreEntry.class, membership.getTenant().getKeystore().getId());
            return Optional.of(new JsonMembership(membership.getName(), membership.getType().ordinal(), membership.getTenant().getId(), null, tenantKeyStore.getTokenGroupName()));
        } else if (membership.getType() == TokenGroupType.TOKENGROUP_PROJECT) {
            KeyStoreEntry projectKeyStore = session.get(KeyStoreEntry.class, membership.getProject().getKeystore().getId());
            return Optional.of(new JsonMembership(membership.getName(), membership.getType().ordinal(), null, membership.getProject().getId(), projectKeyStore.getTokenGroupName()));
        }
        return Optional.empty();
    }

    private static User createNewUser(DbSession session, Tenant tenant, String displayName, String email) {
        Date now = new Date();
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setCreationTime(now);
        session.save(user);
        session.flush();
        session.refresh(user);

        String tenantName = tenant.getName();
        Membership membership = new Membership();
        membership.setName(generateMemberName(session, tenantName));
        membership.setUser(user);
        membership.setTenant(tenant);
        membership.setCreationTime(now);
        membership.setLastModified(now);
        membership.setProjectActionTime(now);
        membership.setStatus(com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE);
        membership.setProjectActionTime(now);
        membership.setType(TokenGroupType.TOKENGROUP_TENANT);
        session.save(membership);
        final String publicTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        if (!StringUtils.equals(tenantName, publicTenantName)) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", publicTenantName));
            Tenant publicTenant = (Tenant)criteria.uniqueResult();
            membership = new Membership();
            membership.setName(generateMemberName(session, publicTenant.getName()));
            membership.setUser(user);
            membership.setTenant(publicTenant);
            membership.setCreationTime(now);
            membership.setLastModified(now);
            membership.setProjectActionTime(now);
            membership.setStatus(com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE);
            membership.setType(TokenGroupType.TOKENGROUP_TENANT);
            session.save(membership);
        }
        return user;
    }

    private static void createNewUserPref(DbSession session, User user) {
        String watermark = "";
        final String publicTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        JsonResponse resp;
        try {
            resp = Heartbeat.getWaterMarkHeartBeatItem(publicTenantName, DeviceType.WEB.getLow());
            JsonWatermark item = WatermarkConfigManager.getConfigFromHeartBeat(resp);
            watermark = item.getText();
        } catch (ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
        }

        JsonExpiry defaultExpiry = new JsonExpiry();
        defaultExpiry.setOption(0);

        UserPreferences prefs = new UserPreferences();
        prefs.setExpiry(new Gson().toJson(defaultExpiry));
        prefs.setWatermark(watermark);
        prefs.setUser(user);
        session.save(prefs);
        session.flush();
    }

    private static User searchLinkedUser(DbSession session, LoginAccount account) {
        Criteria criteria = session.createCriteria(LoginAccount.class);
        criteria.add(Restrictions.eq("email", account.getEmail()));
        criteria.setFetchMode("user", FetchMode.JOIN);
        criteria.setMaxResults(1);
        LoginAccount result = (LoginAccount)criteria.uniqueResult();
        if (result != null) {
            return result.getUser();
        }
        return null;
    }

    private static String generateMemberName(DbSession session, String groupName) {
        try {
            return "m" + session.nextCsn() + '@' + groupName;
        } catch (UnsupportedOperationException e) {
            return "m" + UUID.randomUUID() + '@' + groupName;
        }
    }

    public static String generateDynamicMemberName(int userId, String groupName) {
        return "user" + userId + '@' + groupName;
    }

    public static boolean validateDynamicMembership(String membership) {
        return RegularExpressions.DYNAMIC_MEMBERSHIP_PATTERN.matcher(membership).matches();
    }

    @SuppressWarnings("unchecked")
    public static UserSession getUserSession(DbSession session, int userId, String ticket, String clientId,
        Integer deviceType) {
        byte[] bs = Hex.toByteArray(ticket);
        Criteria c = session.createCriteria(UserSession.class);
        c.add(Restrictions.eq("user.id", userId));
        if (StringUtils.hasText(clientId)) {
            c.add(Restrictions.eq("clientId", clientId));
        }
        if (deviceType != null && deviceType >= 0) {
            c.add(Restrictions.eq("deviceType", deviceType));
        }
        List<UserSession> list = c.list();
        if (!list.isEmpty()) {
            for (UserSession us : list) {
                if (Arrays.equals(bs, us.getTicket())) {
                    return us;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static UserSession authenticate(DbSession session, int userId, String ticket, String clientId,
        Integer deviceType) {
        byte[] bs = Hex.toByteArray(ticket);
        User user = session.get(User.class, userId);
        boolean systemUser = user != null && user.getType() == User.Type.SYSTEM;
        Criteria c = session.createCriteria(UserSession.class);
        c.add(Restrictions.eq("user.id", userId));
        if (!systemUser && StringUtils.hasText(clientId)) {
            c.add(Restrictions.eq("clientId", clientId));
        }
        if (deviceType != null && deviceType >= 0) {
            c.add(Restrictions.eq("deviceType", deviceType));
        }
        c.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
        List<UserSession> list = c.list();
        if (!list.isEmpty()) {
            final Date now = new Date();
            for (UserSession us : list) {
                if (Arrays.equals(bs, us.getTicket())) {
                    if (us.getExpirationTime().after(now)) {
                        return us;
                    } else {
                        session.beginTransaction();
                        us.setStatus(UserSession.Status.EXPIRED);
                        session.update(us);
                        session.commit();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static Membership addUserToProject(DbSession session, Project project, User user, Tenant tenant, Date now,
        User inviter, Date invitedOn) throws TokenGroupException {
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("project.id", project.getId()));
        criteria.add(Restrictions.eq("user.id", user.getId()));
        Membership membership = (Membership)criteria.uniqueResult();
        if (membership != null) {
            membership.setStatus(com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE);
            membership.setLastModified(now);
            membership.setCreationTime(now);
            membership.setInviter(inviter);
            membership.setInvitedOn(invitedOn);
            membership.setProjectActionTime(now);
        } else {
            membership = new Membership();
            TokenGroupManager tgm = new TokenGroupManager(tenant.getName(), project.getName(), getUserIdFromEmail(session, project.getOwner()));
            membership.setName(generateMemberName(session, tgm.getTokenGroupName()));
            membership.setUser(user);
            membership.setProject(project);
            membership.setCreationTime(now);
            membership.setLastModified(now);
            membership.setInviter(inviter);
            membership.setInvitedOn(invitedOn);
            membership.setProjectActionTime(now);
            membership.setStatus(com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE);
        }
        membership.setType(TokenGroupType.TOKENGROUP_PROJECT);
        session.save(membership);
        return membership;
    }

    private static int getUserIdFromEmail(DbSession session, String email) {
        Criteria criteria = session.createCriteria(User.class);
        criteria.setProjection(Projections.property("id"));
        criteria.add(Restrictions.eq("email", email));
        return (int)criteria.uniqueResult();
    }

    public static Set<String> getUserEmails(DbSession session, User user) {
        HashSet<String> set = new HashSet<>();
        Criteria criteria = session.createCriteria(LoginAccount.class);
        criteria.add(Restrictions.eq("userId", user.getId()));
        boolean debugMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.DEBUG, "false"));
        if (!debugMode) {
            criteria.add(Restrictions.eq("status", Constants.Status.ACTIVE.ordinal()));
        }
        List<?> list = criteria.list();
        for (Object obj : list) {
            LoginAccount account = (LoginAccount)obj;
            String email = account.getEmail();
            if (email != null) {
                set.add(email);
            }
        }
        return set;
    }

    private static boolean allowAutoProvision(Tenant tenant) {
        String preferences = tenant.getPreference();
        Map<String, JsonWraper> map = GsonUtils.GSON_SHALLOW.fromJson(preferences, GsonUtils.WRAPER_MAP_TYPE);
        JsonWraper autoProvision = map.get("autoProvision");
        return autoProvision == null || autoProvision.booleanValue();
    }

    private void sendEmail(String template, HttpServletRequest req, User user, LoginAccount account,
        String invitationId, String invitationCode) throws UnsupportedEncodingException {

        Locale locale = req.getLocale();
        String userName = user.getDisplayName();
        if (!StringUtils.hasText(userName)) {
            userName = user.getEmail();
        }
        Properties prop = new Properties();
        prop.setProperty(Mail.KEY_RECIPIENT, user.getEmail());
        prop.setProperty(Mail.KEY_RECIPIENT_ENCODED, URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8.name()));
        prop.setProperty(Mail.KEY_FULL_NAME, userName);
        prop.setProperty(Mail.KEY_ACCOUNT, String.valueOf(account.getId()));
        prop.setProperty(Mail.KEY_WEB_URL, getBaseUrl(req).toString());
        prop.setProperty(Mail.BASE_URL, HTTPUtil.getURI(req));
        if (account.getOtp() != null) {
            prop.setProperty(Mail.KEY_OTP, Hex.toHexString(account.getOtp()));
        }
        if (StringUtils.hasText(invitationId) && StringUtils.hasText(invitationCode)) {
            prop.setProperty(Mail.INVITATION_ID, invitationId);
            prop.setProperty(Mail.INVITATION_CODE, invitationCode);
        }
        Sender.send(prop, template, locale);
    }

    private URI createURI(HttpServletRequest req, String redirUrl, String code) {
        StringBuffer sb = getBaseUrl(req).append(redirUrl).append(code);
        return URI.create(sb.toString());
    }

    private StringBuffer getBaseUrl(HttpServletRequest req) {
        StringBuffer sb = req.getRequestURL();
        URI uri = URI.create(sb.toString());
        sb.setLength(0);
        sb.append(uri.getScheme()).append("://").append(uri.getAuthority());
        sb.append(req.getContextPath());
        return sb;
    }

    private JsonResponse validateCaptcha(String nonce, String captcha) throws GeneralSecurityException {
        if (!StringUtils.hasText(nonce) || !StringUtils.hasText(captcha)) {
            return new JsonResponse(400, "Missing required parameters.");
        }

        CaptchaConfig config = CaptchaConfig.getInstance();
        byte[] buf = Hex.toByteArray(nonce);
        byte[] timeBuf = new byte[4];
        byte[] hmac = new byte[AuthUtils.HMAC_SHA256_LENGTH];
        System.arraycopy(buf, 0, hmac, 0, AuthUtils.HMAC_SHA256_LENGTH);
        System.arraycopy(buf, AuthUtils.HMAC_SHA256_LENGTH, timeBuf, 0, 4);
        byte[] captchaBuf = StringUtils.toBytesQuietly(captcha);
        byte[] captchaKey = StringUtils.toBytesQuietly(config.getKey());
        byte[] result = AuthUtils.hmac(IKeyStoreManager.ALG_HMAC_SHA256, captchaBuf, timeBuf, captchaKey);
        if (!Arrays.equals(hmac, result)) {
            return new JsonResponse(406, "Incorrect captcha.");
        }

        long time = ByteUtils.readInt(timeBuf, 0) * 1000L;
        if (System.currentTimeMillis() - time > DateUtils.MILLIS_PER_MINUTE * 10) {
            return new JsonResponse(406, "Captcha expired.");
        }
        return new JsonResponse(200, "Valid captcha.");
    }

    private static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days != 0) {
            sb.append(days);
            sb.append(days == 1 ? " day " : " days ");
        }
        if (hours != 0) {
            sb.append(hours);
            sb.append(hours == 1 ? " hour " : " hours ");
        }
        if (minutes != 0) {
            sb.append(minutes);
            sb.append(minutes == 1 ? " minute " : " minutes ");
        }
        if (seconds != 0) {
            sb.append(seconds);
            sb.append(seconds == 1 ? " second" : " seconds");
        } else {
            sb.append("1 second");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static void revokeUserSession(DbSession session, User user, LoginType loginType) {
        Criteria c = session.createCriteria(UserSession.class);
        c.add(Restrictions.eq("user.id", user.getId()));
        c.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
        if (loginType != null) {
            c.add(Restrictions.eq("loginType", loginType));
        }
        List<UserSession> list = c.list();
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                UserSession us = list.get(0);
                us.setStatus(UserSession.Status.REVOKED);
                session.update(us);
                if (i > 0 && i % 100 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            session.flush();
            session.clear();
        }
    }

    public static JsonResponse getRemoteUserProfile(int userId, String ticket, String userTokenGroupName,
        String clientId,
        Integer platformId) throws IOException {
        String rms = TokenGroupCache.lookupTokenGroup(userTokenGroupName);
        if (rms == null) {
            return new JsonResponse(400, "Invalid user tokenGroup");
        }
        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(userId));
        prop.setProperty("ticket", ticket);
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null && platformId >= 0) {
            prop.setProperty("platformId", String.valueOf(platformId));
        }
        String path = TokenGroupCache.lookupTokenGroup(userTokenGroupName) + "/rs/usr/v2/basic";
        return JsonResponse.fromJson(RestClient.get(path, prop, false));
    }

    @Secured
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserList(@Context HttpServletRequest request, @QueryParam("searchString") String searchString) {
        boolean error = true;
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        int userId = us.getUser().getId();
        try (DbSession session = DbSession.newSession()) {
            if (!UserService.checkSuperAdmin(session, userId)) {
                return new JsonResponse(4001, "Wrong caller.").toJson();
            }
            String processedSearchString = StringUtils.hasText(searchString) ? searchString.replace("^\\.+", "").replaceAll("[\\\\/:*?\"\'<>|]", "") : "";
            Criteria laCriteria = session.createCriteria(LoginAccount.class);
            Criteria uCriteria = laCriteria.createCriteria("user");
            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add(EscapedLikeRestrictions.ilike("displayName", processedSearchString.toLowerCase(), MatchMode.ANYWHERE));
            disjunction.add(EscapedLikeRestrictions.ilike("email", processedSearchString.toLowerCase(), MatchMode.ANYWHERE));
            uCriteria.addOrder(Order.asc("creationTime"));
            uCriteria.add(disjunction);
            List<LoginAccount> loginAccountList = uCriteria.list();
            JsonResponse response = new JsonResponse(200, "OK");
            response.putResult("totalUsers", loginAccountList.size());
            response.putResult("users", constructUserList(session, loginAccountList));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "listAPIUser", error ? 0 : 1, userId);
        }
    }

    private List<JsonUser> constructUserList(DbSession session, List<LoginAccount> loginAccountList) {
        Map<Integer, JsonUser> users = new HashMap<>(loginAccountList.size());
        Tenant defaultTenant = AbstractLogin.getDefaultTenant();
        String adminStr = defaultTenant.getAdmin();
        Set<String> adminSet = new HashSet<>(Arrays.asList(adminStr.split(",")));
        for (LoginAccount la : loginAccountList) {
            User user = la.getUser();
            if (users.containsKey(user.getId()) && (la.getStatus() == User.Status.INACTIVE.ordinal() || users.get(user.getId()).getStatus() == User.Status.ACTIVE.ordinal())) {
                continue;
            }
            JsonUser jsonUser = new JsonUser();
            jsonUser.setEmail(user.getEmail());
            if (adminSet.contains(user.getEmail().toLowerCase(Locale.getDefault()))) {
                jsonUser.setSuperAdmin(true);
            }
            jsonUser.setName(user.getDisplayName());
            jsonUser.setUserId(user.getId());
            jsonUser.setCreationTime(user.getCreationTime().getTime());
            jsonUser.setStatus(la.getStatus());
            users.put(jsonUser.getUserId(), jsonUser);
        }

        Criteria criteria = session.createCriteria(UserSession.class);
        criteria.add(Restrictions.eq("clientId", UserService.API_USER_CLIENT_ID));
        List<UserSession> usList = criteria.list();
        for (UserSession us : usList) {
            if (users.containsKey(us.getUser().getId())) {
                users.get(us.getUser().getId()).setTicket(Hex.toHexString(us.getTicket()));
            }
        }

        criteria = session.createCriteria(ApiUserCert.class);
        criteria.add(Restrictions.isNull("certAlias"));
        List<ApiUserCert> certList = criteria.list();
        for (ApiUserCert cert : certList) {
            if (users.containsKey(cert.getApiUser().getId())) {
                users.get(cert.getApiUser().getId()).setAppCertImported(true);
            }
        }

        return new ArrayList<>(users.values());
    }

    @Secured
    @PUT
    @Path("/apiUser")
    @Produces(MediaType.APPLICATION_JSON)
    public String createAPIUser(@Context HttpServletRequest request, String json) {
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request.").toJson();
        }
        if (req.getParameter("apiUserId") == null) {
            return new JsonResponse(401, "Missing required parameters.").toJson();
        }
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        int userId = us.getUser().getId();
        int apiUserId = Integer.parseInt(req.getParameter("apiUserId"));
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            if (!UserService.checkSuperAdmin(session, userId)) {
                return new JsonResponse(4001, "Wrong caller.").toJson();
            }
            Criteria criteria = session.createCriteria(UserSession.class);
            criteria.add(Restrictions.eq("clientId", UserService.API_USER_CLIENT_ID));
            criteria.add(Restrictions.eq("user.id", apiUserId));
            UserSession userSession = (UserSession)criteria.uniqueResult();
            if (userSession == null) {
                criteria = session.createCriteria(User.class);
                criteria.add(Restrictions.eq("id", apiUserId));
                User user = (User)criteria.uniqueResult();
                Tenant defaultTenant = AbstractLogin.getDefaultTenant();
                session.beginTransaction();
                userSession = UserService.createUserSession(user, defaultTenant);
                user.setType(User.Type.SYSTEM);
                session.save(userSession);
                session.update(user);
                session.commit();
            }
            error = false;
            JsonResponse response = new JsonResponse(200, "OK");
            response.putResult("ticket", Hex.toHexString(userSession.getTicket()));
            response.putResult("appId", apiUserId);
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "createAPIUser", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/apiUser/cert")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String importAPIUserCert(@Context HttpServletRequest request) {
        RestUploadRequest uploadReq;
        boolean error = true;
        int apiUserId = 0;
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        int userId = us.getUser().getId();
        try {
            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            if (uploadReq.getFileStream() == null || !StringUtils.hasText(uploadReq.getJson())) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            JsonRequest req = JsonRequest.fromJson(uploadReq.getJson());
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            apiUserId = Integer.parseInt(req.getParameter("apiUserId"));

            try (DbSession session = DbSession.newSession()) {
                if (!UserService.checkSuperAdmin(session, userId)) {
                    return new JsonResponse(4001, "Wrong caller.").toJson();
                }
                Criteria criteria = session.createCriteria(UserSession.class);
                criteria.add(Restrictions.eq("clientId", UserService.API_USER_CLIENT_ID));
                criteria.add(Restrictions.eq("user.id", apiUserId));
                UserSession userSession = (UserSession)criteria.uniqueResult();
                if (userSession == null) {
                    return new JsonResponse(4002, "API user does not exist").toJson();
                }

                byte[] bytes = IOUtils.toByteArray(uploadReq.getFileStream());
                ApiUserCert userCert;
                criteria = session.createCriteria(ApiUserCert.class);
                criteria.add(Restrictions.eq("apiUser.id", apiUserId));
                criteria.add(Restrictions.isNull("certAlias"));
                userCert = (ApiUserCert)criteria.uniqueResult();
                session.beginTransaction();
                Date now = new Date();
                if (userCert == null) {
                    userCert = new ApiUserCert();
                    userCert.setApiUser(session.get(User.class, apiUserId));
                    userCert.setCreationTime(now);
                }
                userCert.setLastModified(now);
                userCert.setData(bytes);
                session.saveOrUpdate(userCert);
                session.commit();
                error = false;
                return new JsonResponse(200, "OK").toJson();
            }
        } catch (FileUploadException e) {
            LOGGER.error("File upload error occurred when uploading cert for api user (user ID: {}", apiUserId);
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (IOException e) {
            LOGGER.error("IO error occurred when uploading cert for api user (user ID: {}", apiUserId);
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "importAPIUserCert", error ? 0 : 1, userId);
        }
    }

    @Secured
    @DELETE
    @Path("/apiUser/cert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteAPIUserCert(@Context HttpServletRequest request, String json) {
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request.").toJson();
        }
        if (req.getParameter("apiUserId") == null) {
            return new JsonResponse(401, "Missing required parameters.").toJson();
        }
        int apiUserId = Integer.parseInt(req.getParameter("apiUserId"));
        boolean error = false;
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        int userId = us.getUser().getId();

        try (DbSession session = DbSession.newSession()) {
            if (!UserService.checkSuperAdmin(session, userId)) {
                return new JsonResponse(4001, "Wrong caller.").toJson();
            }
            session.beginTransaction();
            Query query = session.createQuery("DELETE FROM ApiUserCert where apiUser.id = :apiUserId AND certAlias is NULL");
            query.setParameter("apiUserId", apiUserId);
            query.executeUpdate();
            session.commit();
            return new JsonResponse(200, "OK").toJson();
        } catch (Throwable e) {
            error = true;
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "UserMgmt", "deleteAPIUserCert", error ? 0 : 1, userId);
        }
    }
}
