package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.FIPSError;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.LoginAccount;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.rs.exception.AccountCheckFailException;
import com.nextlabs.rms.rs.exception.AccountDisabledException;
import com.nextlabs.rms.rs.exception.AccountNotActivatedException;
import com.nextlabs.rms.rs.exception.AccountNotApprovedException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public abstract class AbstractLogin {

    private static final Map<String, Tenant> TENANT_MAP = new ConcurrentHashMap<String, Tenant>();
    public static final String PLATFORM_ID = "platformId";
    public static final String CLIENT_ID = "clientId";
    public static final String TENANT_NAME = "tenant";
    public static final String LOGIN_APP = "loginApp";
    public static final String IDP_ID = "id";

    protected abstract Constants.LoginType getLoginType();

    protected Response loginSuccessed(HttpServletRequest req, DbSession session, boolean adminApp, String loginName,
        String email,
        String name, Map<String, List<String>> attributes, String tenantName, String clientId, Integer platformId,
        Long customTTL)
            throws GeneralSecurityException, UnsupportedEncodingException {
        platformId = platformId != null && platformId >= 0 ? platformId : DeviceType.WEB.getLow();
        DeviceType deviceType = DeviceType.getDeviceType(platformId);
        if (deviceType == null) {
            return Response.ok(new JsonResponse(400, "Unsupported platform ID.").toJson(), MediaType.APPLICATION_JSON).build();
        }
        Tenant tenant = null;
        if (StringUtils.hasText(tenantName)) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            tenant = (Tenant)criteria.uniqueResult();
        } else {
            tenant = getDefaultTenant();
        }
        if (tenant == null) {
            return Response.ok(new JsonResponse(503, "Service is not initialized yet.").toJson(), MediaType.APPLICATION_JSON).build();
        }
        if (adminApp && !tenant.isAdmin(email)) {
            return Response.ok(new JsonResponse(403, "Cannot login as admin.").toJson(), MediaType.APPLICATION_JSON).build();
        }

        Date now = new Date();

        session.beginTransaction();
        Criteria criteria = session.createCriteria(LoginAccount.class);
        criteria.add(Restrictions.eq("loginName", loginName));
        criteria.add(Restrictions.eq("type", getLoginType().ordinal()));
        LoginAccount account = (LoginAccount)criteria.uniqueResult();
        if (account == null) {
            account = new LoginAccount();
            account.setLoginName(loginName);
            account.setType(getLoginType().ordinal());
            account.setEmail(email);
            account.setCreationTime(now);
        } else {

            // Update email from Facebook, Google and Azuread
            if (account.getType() == LoginType.FACEBOOK.ordinal() || account.getType() == LoginType.GOOGLE.ordinal() || account.getType() == LoginType.AZUREAD.ordinal()) {
                account.setEmail(email);
            }
        }

        User user = UserMgmt.linkUser(session, account, tenant, name, false);
        if (user == null) {
            return Response.ok(new JsonResponse(403, "Auto provision is not allowed.").toJson(), MediaType.APPLICATION_JSON).build();
        }

        account.setLastLogin(now);
        session.commit();
        DefaultRepositoryManager.getInstance().createDefaultRepository(session, user.getId(), getDefaultTenant().getId());
        return UserMgmt.createResponse(req, session, user, attributes, tenant, account, false, clientId, platformId, getLoginType(), Boolean.FALSE, customTTL);
    }

    protected void validateUserAccount(String username, Constants.LoginType idpType, boolean approvalRequired)
            throws AccountNotActivatedException, AccountDisabledException, AccountNotApprovedException,
            AccountCheckFailException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(LoginAccount.class);
            criteria.add(Restrictions.eq("loginName", username));
            criteria.add(Restrictions.eq("type", idpType.ordinal()));
            LoginAccount account = (LoginAccount)criteria.uniqueResult();
            if (account == null) {
                if (approvalRequired) {
                    throw new AccountNotApprovedException("Account does not exist");
                }
            } else {
                if (account.getStatus() == Constants.Status.DISABLED.ordinal()) {
                    throw new AccountDisabledException("Account is disabled");
                } else if (account.getStatus() != Constants.Status.ACTIVE.ordinal()) {
                    throw new AccountNotActivatedException("Account is not activted");
                }
            }
        }

    }

    protected String getUserAccountEmail(String username, Constants.LoginType idpType) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(LoginAccount.class);
            criteria.add(Restrictions.eq("loginName", username));
            criteria.add(Restrictions.eq("type", idpType.ordinal()));
            LoginAccount account = (LoginAccount)criteria.uniqueResult();
            if (account != null) {
                return account.getEmail();
            }
        }

        return "";
    }

    protected Tenant getTenantByName(String tenantName) {

        try (DbSession session = DbSession.newSession()) {
            if (StringUtils.hasText(tenantName)) {
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", tenantName));
                return (Tenant)criteria.uniqueResult();
            } else {
                return AbstractLogin.getDefaultTenant();
            }
        }
    }

    protected static Cookie toServletCookie(NewCookie nc) {
        Cookie cookie = new Cookie(nc.getName(), nc.getValue());
        cookie.setDomain(nc.getDomain());
        cookie.setHttpOnly(nc.isHttpOnly());
        cookie.setMaxAge(nc.getMaxAge());
        cookie.setPath(nc.getPath());
        cookie.setSecure(nc.isSecure());
        cookie.setVersion(nc.getVersion());
        cookie.setComment(nc.getComment());
        return cookie;
    }

    public static void clearDefaultTenant() {
        String tenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        TENANT_MAP.remove(tenantName);
    }

    public static Tenant getDefaultTenant() {
        String tenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        Tenant defaultTenant = TENANT_MAP.get(tenantName);
        if (defaultTenant != null) {
            return defaultTenant;
        }
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            defaultTenant = (Tenant)criteria.uniqueResult();
            if (defaultTenant != null) {
                TENANT_MAP.put(tenantName, defaultTenant);
            }
            return defaultTenant;
        }
    }

    public static Tenant getTenantFromUrl(HttpServletRequest req) {
        Tenant tenant = null;
        String tenantName = req.getParameter("tenant");
        if (StringUtils.hasText(tenantName)) {
            tenant = TENANT_MAP.get(tenantName);
            if (tenant != null) {
                return tenant;
            }
        }
        try (DbSession session = DbSession.newSession()) {
            if (StringUtils.hasText(tenantName)) {
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", tenantName));
                tenant = (Tenant)criteria.uniqueResult();
                if (tenant != null) {
                    TENANT_MAP.put(tenantName, tenant);
                    return tenant;
                }
            }
            String host = URI.create(req.getRequestURL().toString()).getHost();
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("dnsName", host.toLowerCase(Locale.ENGLISH)));
            tenant = (Tenant)criteria.uniqueResult();
            if (tenant != null) {
                TENANT_MAP.put(tenant.getName(), tenant);
                return tenant;
            }
        }
        return null;
    }

    public static String generateClientId() {
        try {
            final byte[] b = new byte[16];
            SecureRandom random = SecureRandom.getInstance("DEFAULT", "BCFIPS");
            random.setSeed(System.currentTimeMillis());
            random.nextBytes(b);
            return Hex.toHexString(b, true);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }
}
