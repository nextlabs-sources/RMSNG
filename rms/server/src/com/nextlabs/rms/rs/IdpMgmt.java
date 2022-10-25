package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.JsonAllowedIdentityProvider;
import com.nextlabs.common.shared.JsonIdentityProvider;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.IdentityProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.TenantUserAttribute;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.idp.AzureAdIdpAttributes;
import com.nextlabs.rms.idp.FacebookIdpAttributes;
import com.nextlabs.rms.idp.GoogleIdpAttributes;
import com.nextlabs.rms.idp.LdapIdpAttributes;
import com.nextlabs.rms.idp.SamlIdpAttributes;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/idp")
public class IdpMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final LoginType[] LOGIN_TYPE_VALUES = LoginType.values();

    private static int maxSelectNum = Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.USER_ATTRIBUTES_SELECT_MAXNUM, "5"));

    public static int getMaxSelectNum() {
        return maxSelectNum;
    }

    @Secured
    @GET
    @Path("/{tenant_id}/userAttrMap")
    @Produces(MediaType.APPLICATION_JSON)
    public String listIdpUserAttrMaps(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @PathParam("tenant_id") String tenantId) throws ServletException {
        boolean error = true;
        try {
            JsonResponse jsonResp = new JsonResponse("OK");
            jsonResp.putResult("idps", getConfiguredIDPs(null, tenantId));
            error = false;
            return jsonResp.toJson();
        } finally {
            Audit.audit(request, "API", "IdpMgmt", "listIdpUserAttrMaps", error ? 0 : 1);
        }

    }

    @SuppressWarnings("unchecked")
    public static List<JsonIdentityProvider> getConfiguredIDPs(String tenantName, String tenantId) {
        try (DbSession session = DbSession.newSession()) {
            if (!StringUtils.hasText(tenantName) && !StringUtils.hasText(tenantId)) {
                return null;
            }
            Tenant tenant;
            if (StringUtils.hasText(tenantName)) {
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", tenantName));
                tenant = (Tenant)criteria.uniqueResult();
            } else {
                tenant = session.get(Tenant.class, tenantId);
            }
            if (tenant == null) {
                return null;
            }
            List<JsonIdentityProvider> idps = (List<JsonIdentityProvider>)RMSCacheManager.getInstance().getIdpCache().get(tenant.getId());
            if (idps != null && idps.isEmpty()) {
                return idps;
            }
            idps = new ArrayList<JsonIdentityProvider>();
            Criteria criteria = session.createCriteria(IdentityProvider.class);
            criteria.add(Restrictions.eq("tenant.id", tenant.getId()));
            List<IdentityProvider> list = criteria.list();
            Gson gson = new Gson();
            if (list != null && !list.isEmpty()) {
                for (IdentityProvider idp : list) {
                    JsonIdentityProvider jsonIdp = new JsonIdentityProvider();
                    jsonIdp.setId(idp.getId());
                    jsonIdp.setType(idp.getType().ordinal());
                    Map<String, String> map = new HashMap<String, String>();
                    if (idp.getType() == Constants.LoginType.LDAP) {
                        try {
                            LdapIdpAttributes ldapAttrs = gson.fromJson(idp.getAttributes(), LdapIdpAttributes.class);
                            JsonObject json = new JsonObject();
                            json.addProperty("domain", ldapAttrs.getDomain());
                            jsonIdp.setAttributes(json.toString());
                            if (StringUtils.hasText(ldapAttrs.getName())) {
                                jsonIdp.setName(ldapAttrs.getName());
                            }
                            jsonIdp.setUserAttrMap(gson.fromJson(idp.getUserAttributeMap(), map.getClass()));
                            idps.add(jsonIdp);
                        } catch (JsonSyntaxException e) {
                            LOGGER.error("Failed to parse attributes of IDP: {}", idp.getId());
                            LOGGER.error(e.getMessage(), e);
                        }
                    } else if (idp.getType() == Constants.LoginType.SAML) {
                        try {
                            SamlIdpAttributes samlAttrs = gson.fromJson(idp.getAttributes(), SamlIdpAttributes.class);
                            JsonObject json = new JsonObject();
                            String btnText = StringUtils.hasText(samlAttrs.getButtonText()) ? samlAttrs.getButtonText() : RMSMessageHandler.getClientString("login.saml_btn") + " - " + samlAttrs.getName();
                            json.addProperty("buttonText", btnText);
                            jsonIdp.setAttributes(json.toString());
                            if (StringUtils.hasText(samlAttrs.getName())) {
                                jsonIdp.setName(samlAttrs.getName());
                            }
                            jsonIdp.setUserAttrMap(gson.fromJson(idp.getUserAttributeMap(), map.getClass()));
                            idps.add(jsonIdp);
                        } catch (JsonSyntaxException e) {
                            LOGGER.error("Failed to parse attributes of IDP: {}", idp.getId());
                            LOGGER.error(e.getMessage(), e);
                        }
                    } else if (idp.getType() == Constants.LoginType.AZUREAD) {
                        try {
                            jsonIdp.setUserAttrMap(gson.fromJson(idp.getUserAttributeMap(), map.getClass()));
                            idps.add(jsonIdp);
                        } catch (JsonSyntaxException e) {
                            LOGGER.error("Failed to parse attributes of IDP: {}", idp.getId());
                            LOGGER.error(e.getMessage(), e);
                        }

                    } else {
                        idps.add(jsonIdp);
                    }
                }
            }
            RMSCacheManager.getInstance().getIdpCache().put(tenant.getId(), idps);
            return idps;
        }
    }

    @Secured
    @POST
    @Path("/{tenant_id}/userAttrMap")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setIdpUserAttrMaps(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, String json)
            throws ServletException {
        boolean error = true;
        try {

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                if (tenant == null) {
                    return new JsonResponse(404, "Tenant not found").toJson();
                }
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }

                List<JsonWraper> attrMapList = req.getParameterAsList("attrMap");
                if (attrMapList != null) {
                    attrMapList = new ArrayList<>(new LinkedHashSet<>(attrMapList));
                    session.beginTransaction();
                    List<TenantUserAttribute> tenantUserAttrList = TenantMgmt.getTenantUserAttrList(session, tenantId, true);
                    List<String> userAttrList = new ArrayList<>();
                    for (TenantUserAttribute userAttr : tenantUserAttrList) {
                        userAttrList.add(userAttr.getName());
                    }
                    for (JsonWraper wraper : attrMapList) {
                        JsonIdentityProvider jsonIdp = wraper.getAsObject(JsonIdentityProvider.class);
                        IdentityProvider idp = session.get(IdentityProvider.class, jsonIdp.getId());
                        Map<String, String> userAttrMap = jsonIdp.getUserAttrMap();
                        if (userAttrMap != null) {
                            if (userAttrMap.size() > maxSelectNum) {
                                return new JsonResponse(5002, "Attribute list cannot contain more than " + maxSelectNum + " attributes").toJson();
                            }
                            for (Map.Entry<String, String> entry : userAttrMap.entrySet()) {
                                if (!TenantMgmt.validateAttributeName(entry.getValue()) || !TenantMgmt.validateAttributeName(entry.getKey())) {
                                    return new JsonResponse(4003, "Attribute name cannot contain special characters other than space, - and _.").toJson();
                                }
                            }
                            for (String key : userAttrMap.keySet()) {
                                if (!userAttrList.contains(key)) {
                                    return new JsonResponse(5003, "User attribute does not exist or not selected").toJson();
                                }
                            }
                            String jsonUserAttrMap = new Gson().toJson(userAttrMap);
                            if (jsonUserAttrMap.length() > 1200) {
                                return new JsonResponse(5001, "UserAttrMap exceeds length limit.").toJson();
                            }
                            idp.setUserAttributeMap(jsonUserAttrMap);
                            session.save(idp);
                        }
                    }
                    session.commit();
                    RMSCacheManager.getInstance().getIdpCache().remove(tenant.getId());
                    boolean invalidateSessions = Boolean.valueOf(req.getParameter("invalidateSessions"));
                    if (invalidateSessions) {
                        TenantMgmt tm = new TenantMgmt();
                        tm.invalidateSessionandCache(tenantId, session);
                    }

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
            Audit.audit(request, "API", "IdpMgmt", "setIdpUserAttrMaps", error ? 0 : 1);
        }
    }

    private String addOrEditIDP(String json, HttpServletRequest request, String tenantId, int userId, boolean isAdd) {
        boolean error = true;
        JsonIdentityProvider jsonIdp = null;
        try {
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                if (tenant == null) {
                    return new JsonResponse(404, "Tenant not found").toJson();
                }
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                if (!tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }

                JsonRequest req = JsonRequest.fromJson(json);
                if (req == null) {
                    return new JsonResponse(400, "Missing request").toJson();
                }

                jsonIdp = req.getParameter("idp", JsonIdentityProvider.class);
                if (jsonIdp == null || (!isAdd && jsonIdp.getId() <= 0) || (isAdd && jsonIdp.getType() == null)) {
                    return new JsonResponse(400, "Missing reqired parameters").toJson();
                }
                if (jsonIdp.getType() == null || jsonIdp.getType() >= LOGIN_TYPE_VALUES.length) {
                    return new JsonResponse(400, "Invalid IDP type.").toJson();
                }
                LoginType jsonIdpType = LOGIN_TYPE_VALUES[jsonIdp.getType()];

                Gson gson = GsonUtils.GSON;
                String jsonAttributes = "{}";
                switch (jsonIdpType) {
                    case FACEBOOK:
                        FacebookIdpAttributes fbAttrs = gson.fromJson(jsonIdp.getAttributes(), FacebookIdpAttributes.class);
                        if (!StringUtils.hasText(fbAttrs.getAppId()) || !StringUtils.hasText(fbAttrs.getAppSecret())) {
                            return new JsonResponse(400, "Missing required attributes for Facebook IDP.").toJson();
                        }
                        jsonAttributes = gson.toJson(fbAttrs);
                        break;
                    case GOOGLE:
                        GoogleIdpAttributes googleAttrs = gson.fromJson(jsonIdp.getAttributes(), GoogleIdpAttributes.class);
                        if (!StringUtils.hasText(googleAttrs.getAppId()) || !StringUtils.hasText(googleAttrs.getAppSecret())) {
                            return new JsonResponse(400, "Missing required attributes for Google IDP.").toJson();
                        }
                        jsonAttributes = gson.toJson(googleAttrs);
                        break;
                    case AZUREAD:
                        AzureAdIdpAttributes azureAttrs = gson.fromJson(jsonIdp.getAttributes(), AzureAdIdpAttributes.class);
                        if (!StringUtils.hasText(azureAttrs.getAppId()) || !StringUtils.hasText(azureAttrs.getAppSecret())) {
                            return new JsonResponse(400, "Missing required attributes for Azure IDP.").toJson();
                        }
                        jsonAttributes = gson.toJson(azureAttrs);
                        break;

                    case SAML:
                        SamlIdpAttributes samlAttrs = gson.fromJson(jsonIdp.getAttributes(), SamlIdpAttributes.class);
                        if (!StringUtils.hasText(samlAttrs.getIdpEntityId()) || !StringUtils.hasText(samlAttrs.getIdpSsoUrl()) || !StringUtils.hasText(samlAttrs.getIdpX509Cert()) || !StringUtils.hasText(samlAttrs.getSpEntityId()) || !StringUtils.hasText(samlAttrs.getSpAcsUrl())) {
                            return new JsonResponse(400, "Missing required attributes for SAML IDP.").toJson();
                        }
                        jsonAttributes = gson.toJson(samlAttrs);
                        break;
                    case LDAP:
                        LdapIdpAttributes ldapAttrs = gson.fromJson(jsonIdp.getAttributes(), LdapIdpAttributes.class);
                        if (!StringUtils.hasText(ldapAttrs.getLdapType()) || !StringUtils.hasText(ldapAttrs.getHostName()) || !StringUtils.hasText(ldapAttrs.getDomain()) || !StringUtils.hasText(ldapAttrs.getSearchBase()) || !StringUtils.hasText(ldapAttrs.getUserSearchQuery())) {
                            return new JsonResponse(400, "Missing required attributes for LDAP IDP.").toJson();
                        }
                        jsonAttributes = gson.toJson(ldapAttrs);
                        break;
                    default:
                        break;
                }

                // Only allow multiple LDP for SAML and LDAP
                if (isAdd && jsonIdpType != Constants.LoginType.SAML && jsonIdpType != Constants.LoginType.LDAP) {
                    Criteria criteria = session.createCriteria(IdentityProvider.class);
                    criteria.add(Restrictions.eq("tenant.id", tenant.getId()));
                    criteria.add(Restrictions.eq("type", jsonIdpType));
                    if (criteria.uniqueResult() != null) {
                        return new JsonResponse(304, "Multiple configurations of this IDP type is not allowed.").toJson();
                    }
                }

                IdentityProvider idp = isAdd ? new IdentityProvider() : session.get(IdentityProvider.class, jsonIdp.getId());

                idp.setTenant(tenant);
                idp.setType(jsonIdpType);
                idp.setAttributes(jsonAttributes);

                session.beginTransaction();
                session.saveOrUpdate(idp);
                session.commit();
                RMSCacheManager.getInstance().getIdpCache().remove(tenantId);
                JsonResponse jsonResp = new JsonResponse("OK");
                jsonResp.putResult("id", idp.getId());
                error = false;
                return jsonResp.toJson();
            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "IdpMgmt", isAdd ? "addIdp" : "editIdp", error ? 0 : 1, userId, jsonIdp != null ? jsonIdp.getType() : null);
        }
    }

    @Secured
    @POST
    @Path("/{tenant_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String editIdp(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, String json) {
        return addOrEditIDP(json, request, tenantId, userId, false);
    }

    @Secured
    @PUT
    @Path("/{tenant_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addIdp(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId, String json) {
        return addOrEditIDP(json, request, tenantId, userId, true);
    }

    @Secured
    @DELETE
    @Path("/{tenant_id}/{idp_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteIdp(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId,
        @PathParam("idp_id") Integer idpId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                IdentityProvider idp = session.get(IdentityProvider.class, idpId);
                if (tenant == null) {
                    return new JsonResponse(404, "Tenant not found.").toJson();
                }
                if (idp == null) {
                    return new JsonResponse(404, "IDP not found.").toJson();
                }
                if (!idp.getTenant().equals(tenant) || !tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }

                Criteria criteria = session.createCriteria(IdentityProvider.class).add(Restrictions.eq("tenant", tenant));
                List idpList = criteria.list();
                if (idpList.size() == 1) {
                    return new JsonResponse(4001, "The last identity provider cannot be deleted.").toJson();
                }

                session.beginTransaction();
                session.delete(idp);
                session.commit();

                RMSCacheManager.getInstance().getIdpCache().remove(tenantId);

                JsonResponse jsonResp = new JsonResponse("OK");
                error = false;
                return jsonResp.toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "IdpMgmt", "deleteIdp", error ? 0 : 1, userId, idpId);
        }
    }

    @Secured
    @GET
    @Path("/{tenant_id}/allow")
    public String getAllowedIDPs(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenant_id") String tenantId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            if (!UserService.checkTenantAdmin(session, tenantId, userId)) {
                return new JsonResponse(4001, "Wrong caller.").toJson();
            }
            Criteria criteria = session.createCriteria(IdentityProvider.class);
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            criteria.add(Restrictions.ne("type", LoginType.DB));
            List<IdentityProvider> idpList = criteria.list();
            boolean allowGoogle = true;
            boolean allowFacebook = true;
            boolean allowAzure = true;
            int countSAML = 1;
            int countLDAP = 1;
            for (IdentityProvider idp : idpList) {
                switch (idp.getType()) {
                    case GOOGLE:
                        allowGoogle = false;
                        break;
                    case FACEBOOK:
                        allowFacebook = false;
                        break;
                    case AZUREAD:
                        allowAzure = false;
                        break;
                    case LDAP:
                        ++countLDAP;
                        break;
                    case SAML:
                        ++countSAML;
                        break;
                    default:
                        LOGGER.error("Invalid identity provider type.");
                        break;
                }
            }
            List<JsonAllowedIdentityProvider> allowedIDPList = constructAllowedIDPList(allowGoogle, allowFacebook, allowAzure, countSAML, countLDAP);
            error = false;
            JsonResponse response = new JsonResponse("OK");
            response.putResult("IDPs", allowedIDPList);
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "IdpMgmt", "getAllowedIDPs", error ? 0 : 1, userId);
        }
    }

    private List<JsonAllowedIdentityProvider> constructAllowedIDPList(boolean allowGoogle, boolean allowFacebook,
        boolean allowAzure, int countSAML, int countLDAP) {
        List<JsonAllowedIdentityProvider> result = new ArrayList<>(2);
        result.add(new JsonAllowedIdentityProvider(LoginType.SAML.ordinal(), countSAML));
        result.add(new JsonAllowedIdentityProvider(LoginType.LDAP.ordinal(), countLDAP));
        if (allowAzure) {
            result.add(new JsonAllowedIdentityProvider(LoginType.AZUREAD.ordinal()));
        }
        if (allowGoogle) {
            result.add(new JsonAllowedIdentityProvider(LoginType.GOOGLE.ordinal()));
        }
        if (allowFacebook) {
            result.add(new JsonAllowedIdentityProvider(LoginType.FACEBOOK.ordinal()));
        }
        return result;
    }

}
