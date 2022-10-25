package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.Environment;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyStoreManager;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.Status;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.JsonIdentityProvider;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonTag;
import com.nextlabs.common.shared.JsonTenant;
import com.nextlabs.common.shared.JsonTenantList;
import com.nextlabs.common.shared.JsonTenantUserAttr;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.exception.SystemBucketException;
import com.nextlabs.rms.exception.TagException;
import com.nextlabs.rms.exception.TenantException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.IdentityProvider;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.LoginAccount;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tag;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.TenantUserAttribute;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TagService;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.servlet.HeaderFilter;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.task.BackupKeyStore;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.CookieUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

@Path("/tenant")
public class TenantMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String[] DEFAULT_USER_ATTRS = { "Company", "Department", "Location", "Nationality",
        "Project", "Job Function", "Cost Center", "Job Level", "Age Group", "Description",
        "Assignment" };
    private static final String[] DEFAULT_PROJECT_TAGS = { "Internal", "External", "HR", "IT" };

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String register(@Context HttpServletRequest request, String json) {
        String name = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            name = req.getParameter("name");
            String otp = req.getParameter("otp");
            if (!StringUtils.hasText(name) || !StringUtils.hasText(otp)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            error = false;
            return addTenant(name, otp, req.getParameter("displayName"), req.getParameter("admin"), req.getParameter("preference"), req.getParameter("dns"), req.getParameter("icon"), req.getIntParameter("securityMode", 0), req.getParameter("parentId"), req.getParameter("description"));
        } catch (IllegalArgumentException | JsonParseException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error("Unable to register tenant (tenantName: {}): {}", name, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "register", error ? 0 : 1, 0);
        }
    }

    private String addTenant(String name, String otp, String displayName, String admin, String preferences, String dns,
        String icon, int securityMode, String parentId)
            throws GeneralSecurityException, IOException {
        return addTenant(name, otp, displayName, admin, preferences, dns, icon, securityMode, parentId, null);
    }

    private boolean validateTenantName(String tenantName) throws UnsupportedEncodingException {
        if (tenantName.length() == tenantName.getBytes(StandardCharsets.UTF_8.name()).length) {
            Matcher matcher = RegularExpressions.CC_TENANT_SUPPORT_CHAR_PATTERN.matcher(tenantName);
            return matcher.matches();
        }
        return false;
    }

    protected String addTenant(String name, String otp, String displayName, String admin, String preferences,
        String dns,
        String icon, int securityMode, String parentId, String description)
            throws GeneralSecurityException, IOException {
        KeyStoreEntry keystore;
        try {
            TokenGroupManager tgm = new TokenGroupManager(name, TokenGroupType.TOKENGROUP_TENANT);
            keystore = tgm.createKeyStore(otp);
        } catch (TokenGroupException e) {
            LOGGER.error("Failed to create key store for token group {} ", name, e.getMessage(), e);
            return new JsonResponse(500, "Failed to create key store").toJson();
        }
        Tenant tenant = null;
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", name));
            tenant = (Tenant)criteria.uniqueResult();
            if (tenant == null) {
                tenant = new Tenant();
                tenant.setName(name);
                tenant.setKeystore(keystore);
                if (tenant.getParentId() == null) {
                    Map<String, String> defaultPreferences = new HashMap<>();
                    defaultPreferences.put(com.nextlabs.rms.config.Constants.CLIENT_HEARTBEAT_FREQUENCY, com.nextlabs.rms.config.Constants.DEFAULT_CLIENT_HEARTBEAT_FREQUENCY);
                    tenant.setPreference(GsonUtils.GSON.toJson(defaultPreferences));
                } else {
                    tenant.setPreference("{}");
                }
                tenant.setCreationTime(new Date());
                tenant.setConfigurationModified(new Date());
            }
            if (StringUtils.hasText(displayName)) {
                tenant.setDisplayName(displayName);
            }
            if (admin != null) {
                admin = admin.toLowerCase(Locale.getDefault());
            }
            tenant.setAdmin(admin);
            tenant.setDnsName(dns);
            tenant.setSecurityMode(securityMode);
            tenant.setConfigurationModified(new Date());
            tenant.setDescription(description);
            if (StringUtils.hasText(preferences)) {
                tenant.setPreference(preferences);
            }
            if (StringUtils.hasText(icon)) {
                tenant.setLoginIcon(Base64Codec.decode(icon));
            }
            if (StringUtils.hasText(parentId)) {
                tenant.setParentId(parentId);
            }
            tenant.setEwsSizeUsed(0L);
            session.saveOrUpdate(tenant);
            session.commit();
            //create system bucket for both SaaS and on premises mode
            createSystemBucket(tenant, false);
            List<JsonTag> tagList = new ArrayList<>();
            for (String defaultProjectTag : DEFAULT_PROJECT_TAGS) {
                JsonTag jsonTag = new JsonTag(defaultProjectTag, Tag.TagType.PROJECT.ordinal());
                tagList.add(jsonTag);
            }
            TagService.persistTenantTags(session, tenant, tagList, Tag.TagType.PROJECT.ordinal());
            String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
            if (defaultTenant != null && !name.equals(defaultTenant)) {
                ControlCenterManager.updateResourceModel(tenant.getName(), ClassificationMgmt.getTenantClassification(session, tenant.getName()), TokenGroupType.TOKENGROUP_TENANT);
                ControlCenterManager.updateABACPolicyModel(tenant);
            }

        } catch (TagException e) {
            LOGGER.error("Failed to create project tags (tenantName: {}): {}", name, e.getMessage());
        } catch (ControlCenterServiceException | ControlCenterRestClientException e) {
            LOGGER.error("Failed to create abac policy model (tenantName: {}): {}", name, e.getMessage());
        } catch (SystemBucketException e) {
            LOGGER.error("Error occurred while creating system bucket" + e.getMessage(), e);
        }

        LOGGER.info("Tenant: {} registered.", name);
        JsonResponse jsonResp = new JsonResponse("Tenant registered");
        jsonResp.putResult("id", tenant.getId());
        jsonResp.putResult("name", tenant.getName());
        return jsonResp.toJson();
    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String name = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            name = req.getParameter("name");
            if (!StringUtils.hasText(name)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", name));
                Tenant tenant = (Tenant)criteria.uniqueResult();
                if (tenant == null) {
                    return new JsonResponse(404, "Invalid tenant").toJson();
                }
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied").toJson();
                }

                String admin = req.getParameter("admin");
                String dns = req.getParameter("dns");
                String icon = req.getParameter("icon");
                int securityMode = req.getIntParameter("securityMode", -1);
                String preference = req.getParameter("preference");

                session.beginTransaction();
                if (StringUtils.hasText(admin)) {
                    tenant.setAdmin(admin);
                }
                if (StringUtils.hasText(dns)) {
                    tenant.setDnsName(dns);
                }
                if (securityMode != -1) {
                    tenant.setSecurityMode(securityMode);
                }
                if (StringUtils.hasText(preference)) {
                    tenant.setPreference(preference);
                }
                if (StringUtils.hasText(icon)) {
                    tenant.setLoginIcon(Base64Codec.decode(icon));
                }

                session.saveOrUpdate(tenant);
                session.commit();
            }
            error = false;
            return new JsonResponse("Tenant updated").toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "update", error ? 0 : 1, userId, name);
        }
    }

    @POST
    @Path("/imp/{tenant_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String importUsers(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, InputStream input) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                if (tenant == null) {
                    return new JsonResponse(404, "Invalid tenant").toJson();
                }
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied").toJson();
                }

                session.beginTransaction();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                    int count = 0;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            continue;
                        }

                        createLoginAccount(session, tenant, line);
                        if (count % 100 == 99) {
                            session.flush();
                            session.clear();
                        }
                        ++count;
                    }
                }
                session.commit();
            }
            error = false;
            return new JsonResponse("Tenant updated").toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "importUsers", error ? 0 : 1, userId);
        }
    }

    @GET
    public String getSsoPath(@Context HttpServletRequest req, @Context HttpServletResponse resp) {
        boolean error = true;
        try {
            Tenant tenant = AbstractLogin.getTenantFromUrl(req);
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }

            String dnsName = tenant.getDnsName();
            StringBuffer sb = req.getRequestURL();
            URI uri = URI.create(sb.toString());
            sb.setLength(0);
            sb.append(uri.getScheme()).append("://");
            if (!StringUtils.hasText(dnsName)) {
                sb.append(uri.getAuthority());
            } else {
                sb.append(dnsName);
                if (uri.getPort() != 443 && uri.getPort() != -1) {
                    sb.append(':').append(uri.getPort());
                }
            }
            sb.append(req.getContextPath()).append(HeaderFilter.LOGIN_ENDPOINT).append("?tenant=").append(URLEncoder.encode(tenant.getName(), StandardCharsets.UTF_8.name()));
            resp.sendRedirect(sb.toString());
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (IOException e) {
            return new JsonResponse(500, "Client connection abort").toJson();
        } finally {
            Audit.audit(req, "API", "TenantMgmt", "getSsoPath", error ? 0 : 1, 0);
        }
    }

    public static Tenant getTenantByName(DbSession session, String tenantName) {
        if (StringUtils.hasText(tenantName)) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            return (Tenant)criteria.uniqueResult();
        } else {
            return AbstractLogin.getDefaultTenant();
        }
    }

    public static Membership getMembership(DbSession session, int userId, String tenantId) {
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("user.id", userId));
        criteria.add(Restrictions.eq("status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("type", TokenGroupType.TOKENGROUP_TENANT));
        return (Membership)criteria.uniqueResult();
    }

    @POST
    @Path("/preference/{tenant_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String setPreferences(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @PathParam("tenant_id") String tenantId, String json) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            JsonRequest req = JsonRequest.fromJson(json);
            boolean adhocEnabled = req.getParameter("adhocEnabled", Boolean.class);
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    ticket = params[1];
                }
                if (userId < 0 || !StringUtils.hasText(ticket)) {
                    return new JsonResponse(401, "Missing login parameters").toJson();
                }
            }
            UserSession us = UserMgmt.authenticate(session, userId, ticket, null, null);
            if (us == null) {
                return new JsonResponse(401, "Access denied").toJson();
            }
            Tenant tenant = session.get(Tenant.class, tenantId);
            if (tenant == null) {
                return new JsonResponse(404, "Tenant not found").toJson();
            }
            User user = us.getUser();
            if (!tenant.isAdmin(user.getEmail())) {
                return new JsonResponse(403, "Access denied").toJson();
            }

            Criteria criteria = session.createCriteria(Membership.class);
            criteria.add(Restrictions.eq("user.id", userId));
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            criteria.setProjection(Projections.rowCount());
            Number totalRecord = (Number)criteria.uniqueResult();
            if (totalRecord.intValue() == 0) {
                return new JsonResponse(403, "Access denied").toJson();
            }

            JsonResponse resp = new JsonResponse("OK");
            String preferences = tenant.getPreference();
            if (StringUtils.hasText(preferences)) {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(preferences);
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                jsonObject.addProperty("ADHOC_ENABLED", adhocEnabled);
                tenant.setPreference(jsonObject.toString());
                session.beginTransaction();
                session.saveOrUpdate(tenant);
                session.commit();
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "setPreferences", error ? 0 : 1, userId);
        }
    }

    @Secured
    @GET
    @Path("/{tenant_id}/idp/details")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDetailsIdp(@Context HttpServletRequest request, @PathParam("tenant_id") String tenantId,
        @HeaderParam("userId") int userId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("clientId") String clientId, @HeaderParam("ticket") String ticket) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            Tenant tenant = session.get(Tenant.class, tenantId);
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            if (!tenant.isAdmin(user.getEmail())) {
                return new JsonResponse(403, "Access denied").toJson();
            }
            List<JsonIdentityProvider> idps = getTypeAndAttributeOfIdp(session, tenantId);
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("idps", idps);
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "getDetailsIdp", error ? 0 : 1, tenantId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<JsonIdentityProvider> getTypeAndAttributeOfIdp(DbSession session, String tenantId) {
        ArrayList<JsonIdentityProvider> idps = new ArrayList<>();
        Criteria criteria = session.createCriteria(IdentityProvider.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.ne("type", Constants.LoginType.DB));
        List<IdentityProvider> list = criteria.list();
        if (list != null && !list.isEmpty()) {
            for (IdentityProvider idp : list) {
                JsonIdentityProvider jsonIdp = new JsonIdentityProvider();
                jsonIdp.setId(idp.getId());
                jsonIdp.setType(idp.getType().ordinal());
                jsonIdp.setAttributes(idp.getAttributes());
                idps.add(jsonIdp);
            }
        }
        return idps;
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String listTenants(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("searchString") String searchString) {
        boolean error = true;
        try {
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    ticket = params[1];
                }
                if (userId < 0 || !StringUtils.hasText(ticket)) {
                    return new JsonResponse(401, "Missing login parameters").toJson();
                }
            }

            JsonTenantList tenantList = getJsonTenantList(getTenantList());
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("details", tenantList);
            return resp.toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "getPreferences", error ? 0 : 1, userId);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Tenant> getTenantList() {
        try (DbSession session = DbSession.newSession()) {

            Criteria criteria = session.createCriteria(Tenant.class);

            return criteria.list();
        }

    }

    private JsonTenantList getJsonTenantList(List<Tenant> tenantList) {
        JsonTenantList jsonTenantListObj = new JsonTenantList();
        if (tenantList == null || tenantList.isEmpty()) {
            jsonTenantListObj.setTotalTenants(0L);
            return null;
        }

        Tenant defaultTenant = AbstractLogin.getDefaultTenant();

        List<JsonTenant> jsonTenantList = new ArrayList<>(tenantList.size());
        for (Tenant tenant : tenantList) {
            JsonTenant jsonTenant = new JsonTenant();
            jsonTenant.setId(tenant.getId());
            jsonTenant.setName(tenant.getName());
            jsonTenant.setDescription(tenant.getDescription());
            jsonTenant.setDefault(defaultTenant.getId().equals(tenant.getId()));
            jsonTenant.setLastModified(tenant.getConfigurationModified().getTime());
            jsonTenant.setDnsName(tenant.getDnsName());
            jsonTenant.setAdminList(Arrays.stream(tenant.getAdmin().split(",")).distinct().collect(Collectors.toList()));
            jsonTenantList.add(jsonTenant);
        }
        jsonTenantListObj.setTenantList(jsonTenantList);
        jsonTenantListObj.setTotalTenants((long)jsonTenantList.size());
        return jsonTenantListObj;
    }

    @GET
    @Path("/v2/{tenant_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPreferencesV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @PathParam("tenant_id") String tenantId) {
        return getPreferences(request, userId, ticket, tenantId);
    }

    @GET
    @Path("/{user_id}/{ticket}/{tenant_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPreferences(@Context HttpServletRequest request, @PathParam("user_id") int userId,
        @PathParam("ticket") String ticket, @PathParam("tenant_id") String tenantId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            boolean isSaasMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.SAAS, "false"));
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                String[] params = CookieUtil.getParamsFromCookies(request, "userId", "ticket");
                if (params != null) {
                    userId = Integer.parseInt(params[0]);
                    ticket = params[1];
                }
                if (userId < 0 || !StringUtils.hasText(ticket)) {
                    return new JsonResponse(401, "Missing login parameters").toJson();
                }
            }
            UserSession us = UserMgmt.authenticate(session, userId, ticket, null, null);
            if (us == null) {
                return new JsonResponse(401, "Access denied").toJson();
            }

            Tenant tenant = session.get(Tenant.class, tenantId);
            if (tenant == null) {
                return new JsonResponse(404, "Tenant not found").toJson();
            }

            Criteria criteria = session.createCriteria(Membership.class);
            criteria.add(Restrictions.eq("user.id", userId));
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            criteria.setProjection(Projections.rowCount());
            Number totalRecord = (Number)criteria.uniqueResult();
            if (totalRecord.intValue() == 0) {
                return new JsonResponse(403, "Access denied").toJson();
            }

            JsonResponse resp = new JsonResponse("OK");
            String preferences = tenant.getPreference();
            if (StringUtils.hasText(preferences)) {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(preferences);
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                boolean updateInDb = false;
                if (!jsonObject.has("SYSTEM_BUCKET_NAME")) {
                    KeyStoreManagerImpl km = new KeyStoreManagerImpl();
                    SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();
                    KeyStoreEntry keyStoreEntry = km.getKeyStore(sbm.constructSystemBucketName(tenant.getName()));
                    if (keyStoreEntry != null) {
                        jsonObject.addProperty("SYSTEM_BUCKET_NAME", keyStoreEntry.getTokenGroupName());
                        updateInDb = true;
                    }
                }
                if (!jsonObject.has("ADHOC_ENABLED")) {
                    jsonObject.addProperty("ADHOC_ENABLED", isSaasMode);
                }
                boolean isHideWorkspace = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.HIDE_WORKSPACE, "false"));
                jsonObject.addProperty("WORKSPACE_ENABLED", !isHideWorkspace);
                boolean isDAPServerEnabled = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.DAP_SERVER_ENABLED, "false"));
                jsonObject.addProperty("DAPSERVER_ENABLED", isDAPServerEnabled);
                String icenetUrl = WebConfig.getInstance().getProperty(WebConfig.ICENET_URL, "");
                if (!icenetUrl.isEmpty()) {
                    jsonObject.addProperty("ICENET_URL", icenetUrl);
                }
                if (updateInDb) {
                    tenant.setPreference(jsonObject.toString());
                    session.beginTransaction();
                    session.saveOrUpdate(tenant);
                    session.commit();
                }
                resp.setExtra(parser.parse(jsonObject.toString()));
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "getPreferences", error ? 0 : 1, userId);
        }
    }

    public static User createLoginAccount(DbSession session, Tenant tenant, String line)
            throws GeneralSecurityException {
        // email,otp,full_name,member_id,group_name,group_full_name,group_admin
        String[] fields = line.split(",");
        if (fields.length != 7) {
            return null;
        }
        String email = fields[0];
        String otp = fields[1];
        String fullName = fields[2];
        String membershipId = fields[3];
        String groupName = fields[4];
        String groupDisplayName = fields[5];
        String groupOwner = fields[6];

        if (email.length() > 150 || otp.length() > 32 || fullName.length() > 150 || membershipId.length() > 150 || groupName.length() > 50 || groupDisplayName.length() > 150 || groupOwner.length() > 150) {
            throw new IllegalArgumentException("Invalid data length");
        }
        email = email.toLowerCase(Locale.getDefault()).trim();

        session.beginTransaction();
        Criteria criteria = session.createCriteria(LoginAccount.class);
        criteria.add(Restrictions.eq("loginName", email));
        criteria.add(Restrictions.eq("type", Constants.LoginType.DB.ordinal()));
        LoginAccount account = (LoginAccount)criteria.uniqueResult();
        if (account != null) {
            return null;
        }

        Date now = new Date();
        account = new LoginAccount();
        account.setLoginName(email);
        account.setEmail(email);
        account.setOtp(Hex.toByteArray(otp));
        account.setCreationTime(now);
        account.setStatus(Status.PENDING.ordinal());

        if (!StringUtils.hasText(fullName)) {
            fullName = email;
        }

        return UserMgmt.linkUser(session, account, tenant, fullName, true);
    }

    @Secured
    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createTenant(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        String tenantName = null;
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing parameters.").toJson();
            }
            tenantName = req.getParameter("tenantName");
            if (!validateTenantName(tenantName)) {
                return new JsonResponse(4003, "Tenant Name contains illegal special characters").toJson();
            }
            if (hasTenant(session, tenantName)) {
                return new JsonResponse(4004, "Tenant already exists.").toJson();
            }
            String adminEmail = req.getParameter("admin");
            String server = req.getParameter("server");
            if (!StringUtils.hasText(tenantName) || !StringUtils.hasText(adminEmail) || !StringUtils.hasText(server)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            } else if (tenantName.length() > 250) {
                return new JsonResponse(4001, "Tenant name is too long.").toJson();
            }
            String preference = req.getParameter("preference");
            if (StringUtils.hasText(preference)) {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(preference);
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (!jsonObject.has("ADHOC_ENABLED")) {
                    jsonObject.addProperty("ADHOC_ENABLED", false);
                }
                if (!jsonObject.has("heartbeat")) {
                    jsonObject.addProperty("heartbeat", 1000.0);
                }
                preference = jsonObject.toString();
            } else {
                preference = "{\"heartbeat\":1000, \"ADHOC_ENABLED\":false}";
            }
            String dns = null;
            if (!StringUtils.hasText(req.getParameter("parentId"))) {
                dns = StringUtils.hasText(req.getParameter("dns")) ? req.getParameter("dns") : request.getServerName();
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("dnsName", dns));
                Tenant result = (Tenant)criteria.uniqueResult();
                if (result != null) {
                    return new JsonResponse(4002, "The server address is occupied.").toJson();
                }
            }

            String publicTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
            JsonResponse resp = createTenantInRouter(publicTenant, tenantName, server);
            if (resp.hasError()) {
                LOGGER.warn("Error occurred when creating tenant in central server (tenantName: {}): {}", tenantName, resp.toJson());
                return new JsonResponse(503, resp.getMessage()).toJson();
            }

            JsonRequest registerTenantRequest = new JsonRequest();
            registerTenantRequest.addParameter("name", tenantName);
            registerTenantRequest.addParameter("otp", resp.getResultAsString("otp"));
            registerTenantRequest.addParameter("admin", adminEmail);
            registerTenantRequest.addParameter("server", req.getParameter("server"));
            registerTenantRequest.addParameter("preference", preference);
            registerTenantRequest.addParameter("parentId", req.getParameter("parentId"));
            registerTenantRequest.addParameter("dns", dns);
            registerTenantRequest.addParameter("description", req.getParameter("description"));

            String path = HTTPUtil.getInternalURI(request) + "/rs/tenant";
            error = false;
            String response = RestClient.put(path, registerTenantRequest.toJson(), RestClient.getConnectionTimeout(), (int)TimeUnit.MINUTES.toMillis(2));
            JsonResponse jsonResponse = JsonResponse.fromJson(response);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            TokenGroupManager tgm = new TokenGroupManager(tenantName, TokenGroupType.TOKENGROUP_TENANT);
            Runnable worker = new BackupKeyStore(tgm.getTokenGroupName());
            executor.execute(worker);
            executor.shutdown();
            String tenantId = jsonResponse.getResult("id", String.class);

            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("id", tenantId));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            criteria = session.createCriteria(IdentityProvider.class);
            criteria.add(Restrictions.eq("tenant.id", AbstractLogin.getDefaultTenant().getId()));
            List<IdentityProvider> idpList = criteria.list();
            session.beginTransaction();
            for (IdentityProvider idp : idpList) {
                IdentityProvider newIdp = new IdentityProvider();
                newIdp.setAttributes(idp.getAttributes());
                newIdp.setType(idp.getType());
                newIdp.setUserAttributeMap(idp.getUserAttributeMap());
                newIdp.setTenant(tenant);
                session.save(newIdp);
            }
            session.commit();

            return response;
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Timeout occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            }
            return new JsonResponse(500, "Timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (GeneralSecurityException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Failed to create tenant").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "createTenant", error ? 0 : 1, userId);
        }
    }

    public void createDefaultTenant(String tenantName, String admin) throws TenantException {
        WebConfig webConfig = WebConfig.getInstance();
        String routerUrl = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL, webConfig.getProperty(WebConfig.ROUTER_URL, "https://r.skydrm.com"));
        final String restUrl = routerUrl + "/rs/tenant";
        try {
            KeyStoreManager ksm = new KeyStoreManager(new File(Environment.getInstance().getDataDir() + File.separator + KeyStoreManager.RMS_KEYSTORE_FILE_SECURE), KeyStoreManager.RMS_KEYSTORE_FILE_PASSWORD.toCharArray(), KeyStoreManager.KeyStoreType.BCFKS.name());
            Certificate cert = ksm.getCertificate(KeyStoreManager.RMS_KEYSTORE_SECURE_ALIAS);
            JsonRequest createTenantRequest = new JsonRequest();
            createTenantRequest.addParameter("name", tenantName);
            createTenantRequest.addParameter("defaultTenant", true);
            Properties prop = new Properties();
            prop.setProperty("cert", Base64Codec.encodeAsString(cert.getEncoded()));
            String ret = RestClient.put(restUrl, prop, createTenantRequest.toJson(), RestClient.getConnectionTimeout(), (int)TimeUnit.SECONDS.toMillis(90));
            JsonResponse resp = JsonResponse.fromJson(ret);
            if (resp.hasError()) {
                LOGGER.error("Error occurred when creating tenant in central server (tenantName: {}): {}", tenantName, resp.toJson());
                throw new TenantException("Error occurred when creating default tenant");
            }
            addTenant(tenantName, resp.getResultAsString("otp"), "defaultTenant", admin, null, URI.create(resp.getResultAsString("server")).getHost(), "", 0, null);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            TokenGroupManager tgm = new TokenGroupManager(tenantName, TokenGroupType.TOKENGROUP_TENANT);
            Runnable worker = new BackupKeyStore(tgm.getTokenGroupName());
            executor.execute(worker);
            executor.shutdown();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Timeout occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            }
            throw new TenantException("Error occurred when creating default tenant", e);
        } catch (IOException | GeneralSecurityException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            }
            throw new TenantException("Error occurred when creating default tenant", e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred during tenant creation (tenantName: {}): {}", tenantName, e.getMessage(), e);
            throw new TenantException("Error occurred when creating default tenant", e);
        }

    }

    public void createSystemBucket(Tenant tenant, boolean updateAll) throws SystemBucketException {
        SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();
        String systemBucketName = sbm.constructSystemBucketName(tenant.getName());
        boolean systemBucketExists = false;
        try {
            if (updateAll) {
                KeyStoreManagerImpl km = new KeyStoreManagerImpl();
                if (km.getKeyStore(systemBucketName) != null) {
                    systemBucketExists = true;
                }
            }
            if (!systemBucketExists) {
                TokenGroupManager tgm = new TokenGroupManager(tenant.getName(), TokenGroupType.TOKENGROUP_SYSTEMBUCKET);
                tgm.createKeyStore();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Runnable worker = new BackupKeyStore(systemBucketName);
                executor.execute(worker);
                executor.shutdown();
            }
        } catch (SocketTimeoutException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Timeout occurred during system bucket ({}): {}", systemBucketName, e.getMessage(), e);
            }
            throw new SystemBucketException("Error occurred when creating system bucket", e);
        } catch (IOException | GeneralSecurityException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred during system bucket ({}): {}", systemBucketName, e.getMessage(), e);
            }
            throw new SystemBucketException("Error occurred when creating system bucket", e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred during system bucket ({}): {}", systemBucketName, e.getMessage(), e);
            throw new SystemBucketException("Error occurred when creating system bucket", e);
        }
    }

    private JsonResponse createTenantInRouter(String parentName, String name, String server)
            throws GeneralSecurityException, IOException {
        WebConfig webConfig = WebConfig.getInstance();
        String routerUrl = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL);
        if (!StringUtils.hasText(routerUrl)) {
            routerUrl = webConfig.getProperty(WebConfig.ROUTER_URL, "https://r.skydrm.com");
        }
        final String restUrl = routerUrl + "/rs/tenant";

        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
        String signature = KeyManager.signData((PrivateKey)km.getKey(parentName, IKeyStoreManager.PREFIX_ICA + parentName), name);
        Certificate cert = km.getCertificate(parentName, IKeyStoreManager.PREFIX_ICA + parentName);

        JsonRequest createTenantRequest = new JsonRequest();
        createTenantRequest.addParameter("certificate", Base64Codec.encodeAsString(cert.getEncoded()));
        createTenantRequest.addParameter("signature", signature);
        createTenantRequest.addParameter("name", name);
        if (StringUtils.hasText(server)) {
            createTenantRequest.addParameter("server", server);
        }

        String ret = RestClient.put(restUrl, createTenantRequest.toJson(), RestClient.getConnectionTimeout(), (int)TimeUnit.SECONDS.toMillis(90));
        return JsonResponse.fromJson(ret);
    }

    public static boolean hasTenant(DbSession session, String tenantName) {
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("name", tenantName).ignoreCase());
        Tenant tenant = (Tenant)criteria.uniqueResult();
        return tenant != null;
    }

    @Secured
    @POST
    @Path("/{tenantName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenantName") String tenantName, String json) {
        boolean error = true;
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request").toJson();
        }
        String adminListStr = req.getParameter("admin");
        String description = req.getParameter("description");
        if (!StringUtils.hasText(adminListStr) || !StringUtils.hasText(tenantName)) {
            return new JsonResponse(400, "Missing required parameter").toJson();
        }
        adminListStr = cleanAdminList(adminListStr);
        try (DbSession session = DbSession.newSession()) {

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!UserService.checkTenantAdmin(session, us.getLoginTenant(), us.getUser().getId())) {
                return new JsonResponse(401, "Unauthorised").toJson();
            }

            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            tenant.setAdmin(adminListStr);
            tenant.setDescription(description);
            session.beginTransaction();
            session.update(tenant);
            session.commit();
            error = false;
            if (WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT).equals(tenantName)) {
                AbstractLogin.clearDefaultTenant();
            }
            return new JsonResponse(200, "OK").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred during tenant update (tenantName: {}): {}", tenantName, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "updateTenant", error ? 0 : 1, userId);
        }
    }

    private String cleanAdminList(String adminListStr) {
        adminListStr = adminListStr.toLowerCase(Locale.getDefault());
        HashSet<String> set = new HashSet<>(Arrays.asList(adminListStr.split(",")));
        return String.join(",", set);
    }

    @Secured
    @DELETE
    @Path("/{tenantName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenantName") String tenantName) {
        boolean error = true;
        if (!StringUtils.hasText(tenantName)) {
            return new JsonResponse(400, "Missing required parameter").toJson();
        }
        try (DbSession session = DbSession.newSession()) {
            Tenant defaultTenant = AbstractLogin.getDefaultTenant();
            if (tenantName.equals(defaultTenant.getName())) {
                return new JsonResponse(4001, "Cannot delete default tenant").toJson();
            }
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (StringUtils.hasText(tenant.getParentId())) {
                return new JsonResponse(4002, "Invalid tenant").toJson();
            }
            deleteTenant(tenantName);
            error = false;
            return new JsonResponse(204, "OK").toJson();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred during tenant delete (tenantName: {}): {}", tenantName, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (GeneralSecurityException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Failed to delete tenant").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred during tenant delete (tenantName: {}): {}", tenantName, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "deleteTenant", error ? 0 : 1, userId);
        }
    }

    public static void deleteTenant(String tenantName) throws GeneralSecurityException, IOException,
            TenantException {
        WebConfig webConfig = WebConfig.getInstance();
        String routerUrl = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL);
        if (!StringUtils.hasText(routerUrl)) {
            routerUrl = webConfig.getProperty(WebConfig.ROUTER_URL, "https://r.skydrm.com");
        }
        String publicTenant = webConfig.getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        String restUrl = routerUrl + "/rs/tenant";

        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
        Certificate cert = km.getCertificate(publicTenant, IKeyStoreManager.PREFIX_ICA + publicTenant);
        String signedRequest = KeyManager.signData((PrivateKey)km.getKey(publicTenant, IKeyStoreManager.PREFIX_ICA + publicTenant), "");
        JsonRequest deleteTenantRequest = new JsonRequest();
        deleteTenantRequest.addParameter("certificate", Base64Codec.encodeAsString(cert.getEncoded()));
        deleteTenantRequest.addParameter("signature", signedRequest);
        deleteTenantRequest.addParameter("name", tenantName);
        String ret = RestClient.delete(restUrl, deleteTenantRequest.toJson());
        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            LOGGER.error("Remote failed: {}", resp.toJson());
            throw new TenantException("Error occurred while deleting tenant in router: " + resp.toJson());
        }
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant != null) {
                session.beginTransaction();
                session.delete(tenant);
                session.commit();
            }
            km.deleteKeyStore(tenantName);
        }
        // FIXME: system bucket and project tokenGroupNames are not deleted from key_store_entry
    }

    public void updateTenantAdmin(String publicTenant, String adminName) throws TenantException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", publicTenant));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Error occurred while updating tenant admin for (tenantName: {})", publicTenant);
                }
                throw new TenantException("Invalid Tenant.");
            }
            session.beginTransaction();
            if (StringUtils.hasText(adminName)) {
                tenant.setAdmin(adminName.toLowerCase(Locale.getDefault()));
            }
            session.saveOrUpdate(tenant);
            session.commit();
        }

    }

    @SuppressWarnings("unchecked")
    public static List<TenantUserAttribute> getTenantUserAttrList(DbSession session, String tenantId,
        boolean filterSelected) {
        Criteria criteria = session.createCriteria(TenantUserAttribute.class);
        if (filterSelected) {
            criteria.add(Restrictions.eq("selected", filterSelected));
        }
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        return (List<TenantUserAttribute>)criteria.list();
    }

    @Secured
    @GET
    @Path("/{tenant_id}/userAttr")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserAttr(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @PathParam("tenant_id") String tenantId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            Tenant tenant = session.get(Tenant.class, tenantId);
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            if (!tenant.isAdmin(user.getEmail())) {
                return new JsonResponse(403, "Access denied").toJson();
            }
            List<JsonTenantUserAttr> jsonAttrList = getUserAttrList(session, tenantId, false);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("maxSelectNum", IdpMgmt.getMaxSelectNum());
            resp.putResult("attrList", jsonAttrList);
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "getUserAttr", error ? 0 : 1);
        }
    }

    public static List<JsonTenantUserAttr> getUserAttrList(DbSession session, String tenantId, boolean filterSelected) {
        List<TenantUserAttribute> userAttrList = getTenantUserAttrList(session, tenantId, filterSelected);
        return constructJsonUserAttrList(userAttrList, filterSelected);
    }

    @Secured
    @GET
    @Path("/{tenant_id}/projectAdmin")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProjectAdmin(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @PathParam("tenant_id") String tenantId) {
        boolean error = true;
        try {
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                if (tenant == null || tenant.getParentId() != null) {
                    return new JsonResponse(401, "Invalid parameter.").toJson();
                }

                String preferences = tenant.getPreference();
                Map<String, Object> tenantPreferences = GsonUtils.GSON.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);

                List<JsonWraper> projectAdminJsonList = new ArrayList<>();
                Map<String, Object> projectAdminMap;
                for (String tenantAdmin : tenant.getAdmin().split(",")) {
                    projectAdminMap = new TreeMap<>();
                    projectAdminMap.put("email", tenantAdmin);
                    projectAdminMap.put("tenantAdmin", true);
                    projectAdminJsonList.add(new JsonWraper(projectAdminMap));
                }

                @SuppressWarnings("unchecked")
                List<String> projectAdminList = (List<String>)tenantPreferences.get(Project.TENANT_PREF_PROJECT_ADMIN);
                if (projectAdminList != null) {
                    for (String email : projectAdminList) {
                        if (tenant.getAdmin().contains(email)) {
                            continue;
                        }
                        projectAdminMap = new HashMap<>();
                        projectAdminMap.put("email", email);
                        projectAdminJsonList.add(new JsonWraper(projectAdminMap));
                    }
                }

                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("projectAdmin", projectAdminJsonList);
                error = false;
                return resp.toJson();
            }

        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getRequestURI(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "getProjectAdmin", error ? 0 : 1);
        }
    }

    @Secured
    @PUT
    @Path("/{tenant_id}/projectAdmin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setProjectAdmin(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }

                List<JsonWraper> projectAdminJsonList = req.getParameterAsList("projectAdmin");
                if (projectAdminJsonList != null) {
                    projectAdminJsonList = new ArrayList<>(new LinkedHashSet<>(projectAdminJsonList));
                    List<String> projectAdminList = new ArrayList<>();
                    for (JsonWraper emailJson : projectAdminJsonList) {
                        String email = emailJson.stringValue().toLowerCase().trim();
                        if (!EmailUtils.validateEmail(email)) {
                            return new JsonResponse(400, "One or more emails have an invalid format.").toJson();
                        }
                        if (!tenant.isAdmin(email)) {
                            projectAdminList.add(email);
                        }
                    }
                    String preferences = tenant.getPreference();
                    Map<String, Object> tenantPreferences = GsonUtils.GSON.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);
                    tenantPreferences.put(Project.TENANT_PREF_PROJECT_ADMIN, projectAdminList);

                    session.beginTransaction();
                    tenant.setPreference(GsonUtils.GSON.toJson(tenantPreferences));
                    session.commit();
                }
            }

            JsonResponse jsonResp = new JsonResponse("OK");
            error = false;
            return jsonResp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "setProjectAdmin", error ? 0 : 1);
        }
    }

    @Secured
    @POST
    @Path("/{tenant_id}/userAttr")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setUserAttr(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }

                List<JsonWraper> attrList = req.getParameterAsList("attributes");
                if (attrList != null) {
                    attrList = new ArrayList<>(new LinkedHashSet<>(attrList));
                    session.beginTransaction();
                    String queryString = "delete from TenantUserAttribute where tenant.id = :tenantId";
                    Query query = session.createQuery(queryString);
                    query.setParameter("tenantId", tenantId);
                    query.executeUpdate();
                    for (JsonWraper wraper : attrList) {
                        JsonTenantUserAttr jsonAttr = wraper.getAsObject(JsonTenantUserAttr.class);
                        if (jsonAttr.getName().length() > 50) {
                            return new JsonResponse(4001, "User attribute name cannot exceed 50 characters.").toJson();
                        }
                        if (!validateAttributeName(jsonAttr.getName())) {
                            return new JsonResponse(4003, "Attribute name cannot contain special characters other than space, - and _.").toJson();
                        }
                    }
                    for (JsonWraper wraper : attrList) {
                        JsonTenantUserAttr jsonAttr = wraper.getAsObject(JsonTenantUserAttr.class);
                        TenantUserAttribute userAttr = new TenantUserAttribute();
                        userAttr.setTenant(tenant);
                        userAttr.setName(jsonAttr.getName());
                        userAttr.setCustom(jsonAttr.getCustom());
                        userAttr.setSelected(jsonAttr.getSelected());
                        session.save(userAttr);
                    }
                    session.commit();
                }
            }

            JsonResponse jsonResp = new JsonResponse("OK");
            error = false;
            return jsonResp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "setUserAttr", error ? 0 : 1);
        }
    }

    public static boolean validateAttributeName(String name) {
        Matcher matcher = RegularExpressions.ATTRIBUTE_NAME_PATTERN.matcher(name);
        return matcher.matches();
    }

    @SuppressWarnings("unchecked")
    private static List<JsonTenantUserAttr> constructJsonUserAttrList(List<TenantUserAttribute> userAttrList,
        boolean filterSelected) {
        List<JsonTenantUserAttr> jsonAttrList = new ArrayList<>();
        if (userAttrList.isEmpty() && !filterSelected) {
            String attrsInConfig = WebConfig.getInstance().getProperty(WebConfig.DEFAULT_USER_ATTRIBUTES);
            if (StringUtils.hasText(attrsInConfig)) {
                Gson gson = new Gson();
                ArrayList<String> attrs = gson.fromJson(attrsInConfig, ArrayList.class);
                for (String name : attrs) {
                    JsonTenantUserAttr jsonAttr = new JsonTenantUserAttr(name, false, false);
                    jsonAttrList.add(jsonAttr);
                }
            } else {
                for (String name : DEFAULT_USER_ATTRS) {
                    JsonTenantUserAttr jsonAttr = new JsonTenantUserAttr(name, false, false);
                    jsonAttrList.add(jsonAttr);
                }
            }
        } else {
            for (TenantUserAttribute userAttr : userAttrList) {
                JsonTenantUserAttr jsonAttr = new JsonTenantUserAttr(userAttr.getName(), userAttr.isCustom(), userAttr.isSelected());
                jsonAttrList.add(jsonAttr);
            }
        }
        return jsonAttrList;
    }

    @Secured
    @DELETE
    @Path("/{tenant_id}/invalidateSessions")
    public String invalidateSessions(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                User user = us.getUser();
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }
                invalidateSessionandCache(tenantId, session);
            }
            LOGGER.info("Session invalidated for all users for tenatId: {}", tenantId);
            JsonResponse jsonResp = new JsonResponse("OK");
            error = false;
            return jsonResp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "TenantMgmt", "invalidateSessions", error ? 0 : 1);
        }
    }

    public void invalidateSessionandCache(String tenantId, DbSession session) {
        session.beginTransaction();
        Criteria criteria = session.createCriteria(UserSession.class);
        criteria.add(Restrictions.eq("loginTenant", tenantId));
        @SuppressWarnings("unchecked")
        List<UserSession> userSessionList = criteria.list();
        for (UserSession userSession : userSessionList) {
            String cacheKey = UserAttributeCacheItem.getKey(userSession.getUser().getId(), userSession.getClientId());
            RMSCacheManager.getInstance().getUserAttributeCache().remove(cacheKey);
        }
        Query query = session.createNamedQuery("revoke.session");
        query.setParameter("tenantId", tenantId);
        query.setParameter("status", UserSession.Status.REVOKED);
        query.setParameter("userType", User.Type.SYSTEM);
        query.executeUpdate();
        session.commit();
    }

    public static Tenant getTenantByTokenGroupName(DbSession session, String tokenGroupName) {
        // TODO replace with suitable methods in TokenGroupManager
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("name", tokenGroupName));
        Tenant tenant = (Tenant)criteria.uniqueResult();
        if (tenant == null) {
            SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();
            if (sbm.isSystemBucket(tokenGroupName)) {
                return sbm.getParentTenant(tokenGroupName, session);
            }
            criteria = session.createCriteria(Project.class);
            criteria.createCriteria("keystore", "k");
            criteria.add(Restrictions.eq("k.tokenGroupName", tokenGroupName));
            Project project = (Project)criteria.uniqueResult();
            tenant = project.getParentTenant();
        }
        return tenant;
    }
}
