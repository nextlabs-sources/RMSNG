package com.nextlabs.rms.cli;

import com.nextlabs.common.cli.CLI;
import com.nextlabs.common.cli.CommandParser;
import com.nextlabs.common.cli.Parameter;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.Config;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.TenantMgmt;
import com.nextlabs.rms.rs.UserMgmt;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.LogConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

public class AccessKeyMgmtAction implements Action {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void execute(Config config, String[] args) {
        CommandParser<AccessKeyMgmtConfig> p = new CommandParser<>(AccessKeyMgmtConfig.class);
        try {
            if (CommandParser.hasHelp(args)) {
                System.out.println("options:"); //NOPMD
                System.out.println("	--list: List all apps registered"); //NOPMD
                System.out.println("	--create --name=<appName>: Create a new app with name"); //NOPMD
                System.out.println("	--revoke --name=<appName>: Revoke app and set app to inactive state"); //NOPMD
                System.out.println("	--refresh --name=<appName>: Refresh app and create new AppKey"); //NOPMD
                System.out.println("	--joinTenant --name=<appName> --tenantName=<tenantName> : Join tenant in order to encrypt/decrypt files in a particular tenant"); //NOPMD
                return;
            }
            File file = new File(config.getConfDir(), "admin.properties");
            try (InputStream fis = new FileInputStream(file)) {
                Properties prop = new Properties();
                prop.load(fis);
                prop = PropertiesFileUtils.decryptPropertyValues(prop);
                WebConfig webConfig = WebConfig.getInstance();
                for (String key : prop.stringPropertyNames()) {
                    if (key.startsWith("web.")) {
                        webConfig.setProperty(key.substring(4), prop.getProperty(key));
                    }
                }
                webConfig.setConfigDir(config.getConfDir());
            } catch (IOException e) {
                logger.error("Error occurred while parsing admin.properties", e);
                System.exit(-1);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(-1);
            }

            AccessKeyMgmtConfig accessConfig = p.parse(args);
            String name = accessConfig.getName();
            if (accessConfig.isList()) {
                try (DbSession session = DbSession.newSession()) {
                    List<UserSession> usList = listSystemUserSession(session);
                    System.out.println("\nApps registered in system:"); //NOPMD
                    for (UserSession us : usList) {
                        System.out.println("AppId: " + us.getUser().getId()); //NOPMD
                        System.out.println("AppName: " + us.getUser().getDisplayName()); //NOPMD
                        System.out.println("AppKey: " + Hex.toHexString(us.getTicket())); //NOPMD
                        System.out.print("Enrolled in tenants: "); //NOPMD
                        List<JsonMembership> memberships = UserMgmt.getMemberships(session, us.getUser().getId(), null, us);
                        for (JsonMembership membership : memberships) {
                            Tenant tenant = session.get(Tenant.class, membership.getTenantId());
                            System.out.print(tenant.getName() + " "); //NOPMD
                        }
                        System.out.println("\n"); //NOPMD
                    }
                }
            } else if (accessConfig.isCreate()) {
                if (!StringUtils.hasText(name)) {
                    System.out.println("\nPlease specify AppName"); //NOPMD
                } else {
                    Tenant publicTenant = AbstractLogin.getDefaultTenant();
                    try (DbSession session = DbSession.newSession()) {
                        User user = getSystemUserByName(session, name);
                        if (user != null) {
                            if (user.getStatus() == User.Status.ACTIVE.ordinal()) {
                                System.out.println("\nThe app is already added and in active state."); //NOPMD
                                System.exit(0);
                            } else {
                                session.beginTransaction();
                                user.setStatus(User.Status.ACTIVE.ordinal());
                                session.save(user);
                            }
                        } else {
                            String admin = publicTenant.getAdmin().split(",")[0];
                            List<String> arrayList = Arrays.asList(name, Hex.toHexString(KeyManager.randomBytes(16)), name, "", UserMgmt.GROUP_NAME_PUBLIC, publicTenant.getDisplayName(), admin);
                            String line = StringUtils.join(arrayList, ",");
                            user = TenantMgmt.createLoginAccount(session, publicTenant, line);
                            if (user == null) {
                                System.out.println("Error occured while creating app."); //NOPMD
                                logger.error("Error occurred while creating app.");
                                System.exit(-1);
                            }
                            user.setType(User.Type.SYSTEM);
                            session.save(user);
                        }
                        UserSession userSession = createUserSession(user, publicTenant);
                        session.save(userSession);
                        session.commit();
                        System.out.println("\nApp Key created successfully."); //NOPMD
                        System.out.println("AppId: " + user.getId()); //NOPMD
                        System.out.println("AppName: " + user.getDisplayName()); //NOPMD
                        System.out.println("AppKey: " + Hex.toHexString(userSession.getTicket())); //NOPMD
                    } catch (GeneralSecurityException e) {
                        System.out.println("Error occurred while creating app."); //NOPMD
                        logger.error("Error occurred while creating app.", e);
                        System.exit(-1);
                    }
                }
            } else if (accessConfig.isRevoke()) {
                try (DbSession session = DbSession.newSession()) {
                    UserSession us = getSystemUserSessionByName(session, name);
                    if (us == null) {
                        System.out.println("\nApp " + name + " is not present or is in inactive state, cannot execute revoke operation."); //NOPMD
                    } else {
                        User user = us.getUser();
                        session.beginTransaction();
                        user.setStatus(User.Status.INACTIVE.ordinal());
                        session.save(user);
                        session.delete(us);
                        session.commit();
                        System.out.println("\nRevoked AppKey for app " + name + " and app is set to inactive state."); //NOPMD
                    }
                }

            } else if (accessConfig.isRefresh()) {
                try (DbSession session = DbSession.newSession()) {
                    UserSession us = getSystemUserSessionByName(session, name);
                    if (us == null) {
                        System.out.println("\nApp " + name + " is not present or in inactive state, cannot execute refresh operation."); //NOPMD
                    } else {
                        session.beginTransaction();
                        byte[] ticket = KeyManager.randomBytes(16);
                        us.setTicket(ticket);
                        session.save(us);
                        session.commit();
                        System.out.println("\nRefreshed AppKey for app " + name + " (AppId: " + us.getUser().getId() + ") new AppKey: " + Hex.toHexString(us.getTicket())); //NOPMD
                    }
                }
            } else if (accessConfig.isJoinTenant()) {
                String tenantName = accessConfig.getTenantName();
                if (!StringUtils.hasText(tenantName)) {
                    System.out.println("\nPlease specify tenantName"); //NOPMD
                } else {
                    try (DbSession session = DbSession.newSession()) {
                        UserSession us = getSystemUserSessionByName(session, name);
                        if (us == null) {
                            System.out.println("\nApp " + name + " is not present or is in inactive state, cannot execute join tenant operation."); //NOPMD
                        } else {
                            Criteria c = session.createCriteria(Tenant.class);
                            c.add(Restrictions.eq("name", tenantName));
                            Tenant tenant = (Tenant)c.uniqueResult();
                            if (tenant == null) {
                                System.out.println("\nThe tenantName you input is invalid."); //NOPMD
                                System.exit(0);
                            }
                            c = session.createCriteria(Project.class);
                            c.add(Restrictions.eq("tenant.id", tenant.getId()));
                            @SuppressWarnings("unchecked")
                            List<Project> projects = c.list();
                            if (projects == null || projects.isEmpty()) {
                                System.out.println("\nThe tenantName you input is invalid."); //NOPMD
                                System.exit(0);
                            } else {
                                Project project = projects.get(0);
                                if (ProjectService.checkUserProjectMembership(session, us, project.getId(), false)) {
                                    System.out.println("\nApp " + name + " is already a member of tenant " + tenant.getName()); //NOPMD
                                    System.exit(0);
                                }
                                Date now = new Date();
                                session.beginTransaction();
                                UserMgmt.addUserToProject(session, project, us.getUser(), null, now, us.getUser(), now);
                                session.commit();
                                System.out.println("\nEnrolled app " + name + " in tenant " + tenant.getName() + " successfully."); //NOPMD
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            System.out.println("Error occurred while parsing command"); //NOPMD
            logger.error("Error occurred while parsing command", e);
            System.exit(-1);
        } catch (TokenGroupException e) {
            System.out.println("Error occurred while running AccessKeyMgmt tool"); //NOPMD
            logger.error("Error occurred while running AccessKeyMgmt tool", e);
            System.exit(-1);
        }
    }

    private static User getSystemUserByName(DbSession session, String name) {
        Criteria criteria = session.createCriteria(User.class);
        criteria.add(Restrictions.eq("type", User.Type.SYSTEM));
        criteria.add(Restrictions.eq("displayName", name));
        return (User)criteria.uniqueResult();
    }

    private static UserSession getSystemUserSessionByName(DbSession session, String name) {
        DetachedCriteria dc = DetachedCriteria.forClass(UserSession.class, "us");
        DetachedCriteria userDc = dc.createCriteria("user", "u", JoinType.INNER_JOIN);
        userDc.add(Restrictions.eq("type", User.Type.SYSTEM));
        userDc.add(Restrictions.eq("displayName", name));
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        return (UserSession)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    private static List<UserSession> listSystemUserSession(DbSession session) {
        DetachedCriteria dc = DetachedCriteria.forClass(UserSession.class, "us");
        DetachedCriteria userDc = dc.createCriteria("user", "u", JoinType.INNER_JOIN);
        userDc.add(Restrictions.eq("type", User.Type.SYSTEM));
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        return criteria.list();
    }

    private static UserSession createUserSession(User user, Tenant loginTenant) {
        UserSession userSession = new UserSession();
        byte[] ticket = KeyManager.randomBytes(16);
        String clientId = AbstractLogin.generateClientId();
        long ttl = DateUtils.addDaysAsMilliseconds(99999);
        userSession.setClientId(clientId);
        userSession.setCreationTime(new Date());
        userSession.setTtl(ttl);
        userSession.setExpirationTime(new Date(ttl));
        userSession.setStatus(UserSession.Status.ACTIVE);
        userSession.setDeviceType(DeviceType.WEB.getLow());
        userSession.setTicket(ticket);
        userSession.setLoginType(LoginType.DB);
        userSession.setLoginTenant(loginTenant.getId());
        userSession.setUser(user);
        return userSession;
    }

    @CLI
    public static final class AccessKeyMgmtConfig {

        @Parameter(description = "List all apps", hasArgs = false, mandatory = false)
        private boolean list;

        @Parameter(description = "Create app", hasArgs = false, mandatory = false)
        private boolean create;

        @Parameter(description = "Join Tenant", hasArgs = false, mandatory = false)
        private boolean joinTenant;

        @Parameter(description = "Revoke app and set app to inactive state", hasArgs = false, mandatory = false)
        private boolean revoke;

        @Parameter(description = "Refresh app and create new AppKey", hasArgs = false, mandatory = false)
        private boolean refresh;

        @Parameter(description = "App name", hasArgs = true, mandatory = false)
        private String name;

        @Parameter(description = "tenant Name", hasArgs = true, mandatory = false)
        private String tenantName;

        public boolean isList() {
            return this.list;
        }

        public boolean isCreate() {
            return this.create;
        }

        public boolean isRevoke() {
            return revoke;
        }

        public boolean isRefresh() {
            return refresh;
        }

        public boolean isJoinTenant() {
            return joinTenant;
        }

        public String getName() {
            return this.name;
        }

        public String getTenantName() {
            return this.tenantName;
        }
    }
}
