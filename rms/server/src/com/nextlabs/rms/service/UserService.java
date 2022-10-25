package com.nextlabs.rms.service;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.rs.AbstractLogin;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class UserService {

    public static final String API_USER_CLIENT_ID = "RMJavaSDK_CLIENT";

    private UserService() {
    }

    public static boolean checkSuperAdmin(DbSession session, int userId) {
        Criteria criteria = session.createCriteria(User.class);
        criteria.add(Restrictions.eq("id", userId));
        return isAdmin(AbstractLogin.getDefaultTenant(), (User)criteria.uniqueResult());
    }

    public static boolean checkTenantAdmin(DbSession session, String tenantId, int userId) {
        Criteria criteria = session.createCriteria(User.class);
        criteria.add(Restrictions.eq("id", userId));
        User user = (User)criteria.uniqueResult();
        criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("id", tenantId));
        Tenant tenant = (Tenant)criteria.uniqueResult();
        return isAdmin(tenant, user);
    }

    private static boolean isAdmin(Tenant tenant, User user) {
        if (tenant == null || user == null) {
            return false;
        }
        String[] admins = tenant.getAdmin().split(",");
        for (String admin : admins) {
            if (admin.equalsIgnoreCase(user.getEmail())) {
                return true;
            }
        }
        return false;
    }

    public static UserSession createUserSession(User user, Tenant loginTenant) {
        UserSession userSession = new UserSession();
        byte[] ticket = KeyManager.randomBytes(16);
        long ttl = DateUtils.addDaysAsMilliseconds(99999);
        userSession.setClientId(API_USER_CLIENT_ID);
        userSession.setCreationTime(new Date());
        userSession.setTtl(ttl);
        userSession.setExpirationTime(new Date(ttl));
        userSession.setStatus(UserSession.Status.ACTIVE);
        userSession.setDeviceType(DeviceType.WEB.getLow());
        userSession.setTicket(ticket);
        userSession.setLoginType(Constants.LoginType.DB);
        userSession.setLoginTenant(loginTenant.getId());
        userSession.setUser(user);
        return userSession;
    }

    public static boolean isAPIUserSession(UserSession us) {
        return us.getUser().getType() == User.Type.SYSTEM && us.getClientId().equals(UserService.API_USER_CLIENT_ID);
    }

}
