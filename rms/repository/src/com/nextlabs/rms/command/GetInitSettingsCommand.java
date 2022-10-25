package com.nextlabs.rms.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.Roles;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserPreferences;
import com.nextlabs.rms.json.InitSettings;
import com.nextlabs.rms.json.RMDownloadUrls;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.CookieUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class GetInitSettingsCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        JsonObject settings = getInitSettingsJSON(request, response);
        JsonUtil.writeJsonToResponse(settings, response);
    }

    public JsonObject getInitSettingsJSON(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RMSUserPrincipal userPrincipal = null;
        try {
            userPrincipal = authenticate(request);
            if (userPrincipal == null) {
                CookieUtil.clearCookies(request, response);
                logger.trace("Cookies cleared since authentication failed.");
                return null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            CookieUtil.clearCookies(request, response);
            logger.trace("Cookies cleared due to exception.");
            return null;
        }

        Cookie[] cookies = request.getCookies();
        int idp = com.nextlabs.common.shared.Constants.LoginType.DB.ordinal();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("idp".equals(cookie.getName())) {
                    try {
                        idp = Integer.parseInt(cookie.getValue());
                    } catch (NumberFormatException e) {
                        idp = com.nextlabs.common.shared.Constants.LoginType.DB.ordinal();
                    }
                    break;
                }
            }
        }

        String loginAccountType = com.nextlabs.common.shared.Constants.LoginType.values()[idp].toString();

        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();

            Tenant tenant = session.get(Tenant.class, userPrincipal.getLoginTenant());
            Map<String, Object> tenantPreferences = GsonUtils.GSON.fromJson(tenant.getPreference(), GsonUtils.GENERIC_MAP_TYPE);

            InitSettings settings = new InitSettings();
            settings.setLoginAccountType(loginAccountType);
            settings.setTenantId(userPrincipal.getTenantId());
            settings.setTenantName(userPrincipal.getTenantName());
            settings.setUserName(userPrincipal.getEmail());
            settings.setUserDisplayName(userPrincipal.getName());
            settings.setRmsVersion(BuildConfig.VERSION);
            settings.setAdmin(userPrincipal.isAdmin());
            settings.setPersonalRepoEnabled(true);
            settings.setManageProfileAllowed(idp == com.nextlabs.common.shared.Constants.LoginType.DB.ordinal());

            UserPreferences userPreferences = session.get(UserPreferences.class, userPrincipal.getUserId());
            String userPrefs = userPreferences.getPreferences();
            String landingPage = Constants.RMS_WELCOME_PAGE;
            String redirectPageFromWelcome = getLandingPage(userPrincipal, session);
            if (userPrefs != null && !userPrefs.isEmpty()) {
                Map<String, Object> userPrefsMap = GsonUtils.GSON.fromJson(userPrefs, GsonUtils.GENERIC_MAP_TYPE);
                settings.setUserPreferences(new JsonParser().parse(userPrefs));
                landingPage = (String)userPrefsMap.get(Constants.USER_PREF_LANDING_PAGE);
                if (!Constants.RMS_WELCOME_PAGE.equals(landingPage)) {
                    landingPage = redirectPageFromWelcome;
                }
            } else {
                settings.setUserPreferences(new JsonObject());
            }
            settings.setLandingPage(landingPage);
            if (tenantPreferences != null) {
                settings.setRoles(getRoles(userPrincipal, session, tenantPreferences));
                RMDownloadUrls rmDownloadUrls = new RMDownloadUrls();
                rmDownloadUrls.setRmdWin32Url((String)tenantPreferences.get(Constants.RMD_WIN_32_DOWNLOAD_URL));
                rmDownloadUrls.setRmdWin64Url((String)tenantPreferences.get(Constants.RMD_WIN_64_DOWNLOAD_URL));
                rmDownloadUrls.setRmdMacUrl((String)tenantPreferences.get(Constants.RMD_MAC_DOWNLOAD_URL));
                rmDownloadUrls.setRmciOSURL((String)tenantPreferences.get(Constants.RMC_IOS_DOWNLOAD_URL));
                rmDownloadUrls.setRmcAndroidURL((String)tenantPreferences.get(Constants.RMC_ANDROID_DOWNLOAD_URL));
                settings.setRmDownloadUrls(rmDownloadUrls);
            }

            WebConfig webConfig = WebConfig.getInstance();
            settings.setDefaultServiceProvider(webConfig.getProperty(WebConfig.INBUILT_SERVICE_PROVIDER));
            if (webConfig.getProperty(WebConfig.INBUILT_SERVICE_PROVIDER).equals(ServiceProviderType.ONEDRIVE_FORBUSINESS.name())) {
                char[] invalidChars = { '#', '%', '*', ':', '<', '>', '?', '/', '|', '"' };
                settings.setInvalidCharactersInFilename(invalidChars);
            } else {
                char[] invalidChars = {};
                settings.setInvalidCharactersInFilename(invalidChars);
            }
            settings.setCookieDomain(CookieUtil.getCookieDomainName(request));
            settings.setViewerURL(webConfig.getProperty(WebConfig.VIEWER_URL));
            settings.setWelcomePageVideoURL(webConfig.getProperty(WebConfig.WELCOME_PAGE_VIDEO_URL));
            settings.setRedirectPageFromWelcome(redirectPageFromWelcome);
            settings.setSaasMode(Boolean.parseBoolean(webConfig.getProperty(WebConfig.SAAS, "false")));
            settings.setHideWorkspace(Boolean.parseBoolean(webConfig.getProperty(WebConfig.HIDE_WORKSPACE, "false")));
            return GsonUtils.GSON.toJsonTree(settings).getAsJsonObject();
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    private String[] getRoles(RMSUserPrincipal userPrincipal, DbSession session,
        Map<String, Object> tenantPreferences) {
        ArrayList<String> roles = new ArrayList<>();
        if (AuthManager.getPublicTenant(session).isAdmin(userPrincipal.getEmail())) {
            roles.add(Roles.SYSTEM_ADMIN.name());
        }
        if (userPrincipal.isAdmin()) {
            roles.add(Roles.TENANT_ADMIN.name());
        }
        List<String> projectAdmins = ((List<String>)tenantPreferences.get(Project.TENANT_PREF_PROJECT_ADMIN));
        if (StringUtils.containsElement(projectAdmins, userPrincipal.getEmail(), true)) {
            roles.add(Roles.PROJECT_ADMIN.name());
        }
        return roles.toArray(new String[roles.size()]);
    }

    @SuppressWarnings("unchecked")
    public static String getLandingPage(RMSUserPrincipal userPrincipal, DbSession session) {
        String landingPage = Constants.RMS_HOME_PAGE;
        boolean isOnlyPersonalRepo = userPrincipal.isAdmin();
        List<IRepository> repoList = RepositoryManager.getRepositoryList(session, userPrincipal, isOnlyPersonalRepo);
        if (repoList.isEmpty()) {
            if (userPrincipal.isAdmin()) {
                String tenantId = userPrincipal.getTenantId();
                Criteria criteria = session.createCriteria(StorageProvider.class);
                criteria.add(Restrictions.eq("tenantId", tenantId));
                List<StorageProvider> list = criteria.list();
                landingPage = list.isEmpty() ? Constants.RMS_SP_PAGE : Constants.RMS_MANAGE_REPO_PAGE;
            } else {
                landingPage = Constants.RMS_MANAGE_REPO_PAGE;
            }
        } else {
            for (IRepository repository : repoList) {
                if (repository instanceof Closeable) {
                    IOUtils.closeQuietly(Closeable.class.cast(repository));
                }
            }
        }
        return landingPage;
    }
}
