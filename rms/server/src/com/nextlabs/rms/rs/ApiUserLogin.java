package com.nextlabs.rms.rs;

import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.RMSCacheManager.Cache;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ApiUserCert;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/login/trustedapp")
public class ApiUserLogin extends AbstractLogin {

    private static final int NONCE_LIFESPAN = Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.APP_LOGIN_NONCE_EXPIRY, "30"));
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Secured
    @GET
    @Path("/nonce")
    @Produces(MediaType.APPLICATION_JSON)
    public String getNonce(@Context HttpServletRequest request) {
        JsonResponse resp = new JsonResponse("OK");
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        int userId = us.getUser().getId();
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(UserSession.class);
            criteria.add(Restrictions.eq("clientId", UserService.API_USER_CLIENT_ID));
            criteria.add(Restrictions.eq("user.id", userId));
            UserSession userSession = (UserSession)criteria.uniqueResult();
            if (userSession == null) {
                return new JsonResponse(403, "Access denied.").toJson();
            }
        }
        Cache nonceCache = RMSCacheManager.getInstance().getAppLoginNonceCache();
        String nonce = Hex.toHexString(KeyManager.randomBytes(16));
        if (nonceCache.containsKey(nonce)) {
            nonce = Hex.toHexString(KeyManager.randomBytes(16));
            if (nonceCache.containsKey(nonce)) {
                return new JsonResponse(409, "Nonce is in conflict state, please try again.").toJson();
            }
        }
        nonceCache.put(nonce, nonce, NONCE_LIFESPAN, TimeUnit.MINUTES);
        resp.putResult("nonce", nonce);
        return resp.toJson();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpServletRequest request, String json) {
        boolean error = true;
        String clientId = null;
        int platformId = -1;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.ok(new JsonResponse(400, "Missing request").toJson(), MediaType.APPLICATION_JSON).build();
            }
            Cookie[] cookies = request.getCookies();
            platformId = DeviceType.RMX.getLow();
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
                    }
                }
            }
            clientId = StringUtils.hasText(clientId) ? clientId : generateClientId(); //NOPMD
            String email = req.getParameter("email");
            int appId = req.getIntParameter("appId", -1);
            long ttl = req.getLongParameter("ttl", -1);
            String nonce = req.getParameter("nonce");
            String userAttributes = req.getParameter("userAttributes");
            String signature = req.getParameter("signature");
            if (!StringUtils.hasText(email) || appId <= 0 || ttl < 0 || !StringUtils.hasText(userAttributes) || !StringUtils.hasText(signature) || !StringUtils.hasText(nonce)) {
                return Response.ok(new JsonResponse(400, "Invalid parameters.").toJson(), MediaType.APPLICATION_JSON).build();
            }
            Cache nonceCache = RMSCacheManager.getInstance().getAppLoginNonceCache();
            if (!nonceCache.containsKey(nonce)) {
                return Response.ok(new JsonResponse(400, "Invalid nonce for authentication.").toJson(), MediaType.APPLICATION_JSON).build();
            }
            Map<String, List<String>> attributes = (Map<String, List<String>>)GsonUtils.GSON.fromJson(userAttributes, GsonUtils.STRING_LIST_MAP_TYPE);
            String ticket = null;
            ApiUserCert userCert = null;
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(UserSession.class);
                criteria.add(Restrictions.eq("user.id", appId));
                criteria.add(Restrictions.eq("clientId", UserService.API_USER_CLIENT_ID));
                UserSession us = (UserSession)criteria.uniqueResult();
                if (us == null) {
                    return Response.ok(new JsonResponse(4001, "No valid login session for app.").toJson(), MediaType.APPLICATION_JSON).build();
                }
                if (!email.equalsIgnoreCase(us.getUser().getEmail())) {
                    return Response.ok(new JsonResponse(4003, "Email address does not match.").toJson(), MediaType.APPLICATION_JSON).build();
                }
                ticket = Hex.toHexString(us.getTicket());
                criteria = session.createCriteria(ApiUserCert.class);
                criteria.add(Restrictions.eq("apiUser.id", appId));
                criteria.add(Restrictions.isNull("certAlias"));
                userCert = (ApiUserCert)criteria.uniqueResult();
                if (userCert == null) {
                    return Response.ok(new JsonResponse(4002, "Missing certificate for app.").toJson(), MediaType.APPLICATION_JSON).build();
                }
            }
            byte[] data = userCert.getData();

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BCFIPS");
            InputStream in = new ByteArrayInputStream(data);
            X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance("BCFKS", "BCFIPS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("apiUserCert", cert);
            Signature sig = Signature.getInstance("SHA256WithRSA", "BCFIPS");
            sig.initVerify(cert.getPublicKey());
            boolean validSignature = false;
            try {
                // signature: SHA256WithRSA(appId.appKey.email.ttl.nonce.userattritues)
                StringBuilder sb = new StringBuilder().append(appId).append('.').append(ticket).append('.').append(email).append('.').append(ttl).append('.').append(nonce).append('.').append(userAttributes);
                sig.update(StringUtils.toBytesQuietly(sb.toString()));
                validSignature = sig.verify(Base64Codec.decode(signature));
            } catch (SignatureException e) {
                LOGGER.error("Error occurred while verifying digital signature.", e.getMessage(), e);
            }

            if (!validSignature) {
                return Response.ok(new JsonResponse(403, "Invalid digital signature, login failed.").toJson(), MediaType.APPLICATION_JSON).build();
            }

            nonceCache.remove(nonce);

            attributes.put(UserAttributeCacheItem.EMAIL, Collections.singletonList(email));
            String uniqueIdAttribute = UserAttributeCacheItem.EMAIL;
            String uniqueIdAttributeFromConfig = WebConfig.getInstance().getProperty(WebConfig.RMX_UNIQUE_USER_ID);
            if (StringUtils.hasText(uniqueIdAttributeFromConfig)) {
                uniqueIdAttribute = uniqueIdAttributeFromConfig;
            }
            attributes.put(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE, Collections.singletonList(uniqueIdAttribute));
            String name = attributes.containsKey(UserAttributeCacheItem.DISPLAYNAME) ? attributes.get(UserAttributeCacheItem.DISPLAYNAME).get(0) : email;
            attributes.put(UserAttributeCacheItem.DISPLAYNAME, Collections.singletonList(email));
            String tenant = req.getParameter("tenant");
            try (DbSession session = DbSession.newSession()) {
                error = false;
                ttl = ttl == 0 ? 3600 * 6 * 1000 : ttl;
                return loginSuccessed(request, session, false, email, email, name, attributes, tenant, clientId, platformId, ttl);
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.ok(new JsonResponse(500, "Internal Server Error").toJson(), MediaType.APPLICATION_JSON).build();
        } finally {
            Audit.audit(request, "API", "TrustedAppLogin", "login", error ? 0 : 1, clientId, platformId);
        }
    }

    @Override
    protected LoginType getLoginType() {
        return LoginType.TRUSTEDAPP;
    }
}
