package com.nextlabs.rms.auth;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * @author nnallagatla
 *
 */
public final class AuthManager {

    public static final String USER_ID = "userId";
    public static final String TICKET = "ticket";
    public static final String TENANT_ID = "tenantId";
    public static final String CLIENT_ID = "clientId";
    public static final String PLATFORM_ID = "platformId";
    public static final String DEVICE_ID = "deviceId";

    private AuthManager() {
    }

    public static RMSUserPrincipal authenticate(DbSession session, HttpServletRequest request) {
        RMSUserPrincipal principal = extractUser(request);
        if (principal == null) {
            return null;
        }
        UserSession userSession = getUserSession(session, principal);
        if (userSession == null) {
            return null;
        }
        long ttl = userSession.getTtl();
        if (ttl > 0 && System.currentTimeMillis() > ttl) {
            return null;
        }
        String tenantId = principal.getTenantId();
        if (tenantId == null) {
            tenantId = getDefaultTenant(session, request).getId();
        }
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("user.id", principal.getUserId()));
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("status", Membership.Status.ACTIVE));
        criteria.setProjection(Projections.rowCount());
        Number number = (Number)criteria.uniqueResult();
        if (number == null || number.intValue() == 0) {
            return null;
        }
        User user = userSession.getUser();
        Tenant tenant = session.get(Tenant.class, userSession.getLoginTenant());
        if (tenant.isAdmin(user.getEmail())) {
            principal.setAdmin(true);
        }
        principal.setTenantId(tenantId);
        principal.setName(user.getDisplayName());
        principal.setTenantName(tenant.getName());
        principal.setEmail(user.getEmail());
        principal.setTtl(userSession.getTtl());
        principal.setLoginTenant(userSession.getLoginTenant());
        return principal;
    }

    public static Tenant getDefaultTenant(DbSession session, HttpServletRequest request) {
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("displayName", "defaultTenant"));
        return (Tenant)criteria.uniqueResult();
    }

    public static Tenant getPublicTenant(DbSession session) {
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("name", WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, Constants.DEFAULT_TENANT)));
        return (Tenant)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public static UserSession getUserSession(DbSession session, RMSUserPrincipal principal) {
        byte[] bs = Hex.toByteArray(principal.getTicket());
        Criteria c = session.createCriteria(UserSession.class);
        c.add(Restrictions.eq("user.id", principal.getUserId()));
        c.add(Restrictions.eq("clientId", principal.getClientId()));
        c.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
        List<UserSession> list = c.list();
        UserSession userSession = null;
        for (UserSession us : list) {
            if (Arrays.equals(bs, us.getTicket())) {
                userSession = us;
                break;
            }
        }
        return userSession;
    }

    private static RMSUserPrincipal extractUser(HttpServletRequest request) {
        String id = request.getParameter(USER_ID);
        String tenantId = request.getParameter(TENANT_ID);
        String ticket = request.getParameter(TICKET);
        String clientId = request.getParameter(CLIENT_ID);
        String platform = request.getParameter(PLATFORM_ID);
        String deviceId = request.getParameter(DEVICE_ID);
        try {
            deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
        } catch (UnsupportedEncodingException e) { //NOPMD
        }

        if (id == null || ticket == null || clientId == null) {
            id = request.getHeader(USER_ID);
            ticket = request.getHeader(TICKET);
            tenantId = request.getHeader(TENANT_ID);
            clientId = request.getHeader(CLIENT_ID);
            platform = request.getHeader(PLATFORM_ID);
            deviceId = request.getHeader(DEVICE_ID);
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
        }

        if (id == null || ticket == null || clientId == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if (USER_ID.equals(name)) {
                        id = cookie.getValue();
                    } else if (TICKET.equals(name)) {
                        ticket = cookie.getValue();
                    } else if (TENANT_ID.equals(name)) {
                        tenantId = cookie.getValue();
                    } else if (CLIENT_ID.equals(name)) {
                        clientId = cookie.getValue();
                    } else if (PLATFORM_ID.equals(name)) {
                        platform = cookie.getValue();
                    } else if (DEVICE_ID.equals(name)) {
                        deviceId = cookie.getValue();
                        try {
                            deviceId = URLDecoder.decode(deviceId, "UTF-8");
                        } catch (UnsupportedEncodingException e) { //NOPMD
                        }
                    }
                }
            }
        }

        if (id == null || ticket == null || clientId == null) {
            id = (String)request.getAttribute(USER_ID);
            ticket = (String)request.getAttribute(TICKET);
            tenantId = (String)request.getAttribute(TENANT_ID);
            clientId = (String)request.getAttribute(CLIENT_ID);
            platform = (String)request.getAttribute(PLATFORM_ID);
            deviceId = (String)request.getAttribute(DEVICE_ID);
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            if (!StringUtils.hasText(id) || !StringUtils.hasText(ticket) || !StringUtils.hasText(clientId)) {
                return null;
            }
        }

        int userId = Integer.parseInt(id);
        Integer platformId = org.apache.commons.lang3.StringUtils.isNumeric(platform) ? Integer.parseInt(platform) : null;
        RMSUserPrincipal principal = new RMSUserPrincipal(userId, tenantId, ticket, clientId);
        principal.setPlatformId(platformId);
        principal.setDeviceId(deviceId);
        return principal;
    }
}
