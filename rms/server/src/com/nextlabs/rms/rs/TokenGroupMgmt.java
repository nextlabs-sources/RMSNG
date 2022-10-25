package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/tokenGroup")
public class TokenGroupMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Secured
    @GET
    @Path("/details")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTokenGroupDetailsByName(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("tokenGroupName") String tokenGroupName)
            throws ServletException {
        boolean error = true;
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            if (!StringUtils.hasText(tokenGroupName)) {
                tokenGroupName = AbstractLogin.getDefaultTenant().getName();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            return getTokenGroupDetailsResponse(us, userId, tokenGroupName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TokenGroupMgmt", "getTokenGroupDetailsByName", error ? 0 : 1, userId);
        }
    }

    @Secured
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTokenGroupDetails(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("tokenGroupType") Integer tgTypeOrdinal,
        @QueryParam("tenantName") String tenantName, @QueryParam("projectName") String projectName)
            throws ServletException {
        boolean error = true;
        String tokenGroupName = null;
        if (tgTypeOrdinal != null) {
            try (DbSession session = DbSession.newSession()) {
                TokenGroupType[] tgTypeValues = TokenGroupType.values();
                if (tgTypeOrdinal < 0 || tgTypeOrdinal >= tgTypeValues.length) {
                    return new JsonResponse(503, "Invalid Token Group Type").toJson();
                }
                TokenGroupType tgType = tgTypeValues[tgTypeOrdinal];
                if (!StringUtils.hasText(tenantName)) {
                    tenantName = AbstractLogin.getDefaultTenant().getName();
                } else {
                    Criteria criteria = session.createCriteria(Tenant.class);
                    criteria.add(Restrictions.eq("name", tenantName));
                    Tenant tenant = (Tenant)criteria.uniqueResult();
                    if (tenant == null) {
                        return new JsonResponse(504, "Invalid tenant name.").toJson();
                    }
                }
                switch (tgType) {
                    case TOKENGROUP_TENANT:
                        tokenGroupName = tenantName;
                        break;
                    case TOKENGROUP_PROJECT:
                        Criteria criteria = session.createCriteria(Project.class);
                        criteria.createCriteria("parentTenant", "pt");
                        criteria.add(Restrictions.eq("pt.name", tenantName));
                        criteria.add(Restrictions.eq("name", projectName));
                        Project project = (Project)criteria.uniqueResult();
                        if (project == null) {
                            return new JsonResponse(505, "Project name \"" + projectName + "\" not found under tenant " + tenantName).toJson();
                        }
                        tokenGroupName = project.getKeystore().getTokenGroupName();
                        break;
                    case TOKENGROUP_SYSTEMBUCKET:
                        tokenGroupName = tenantName + com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX;
                        break;
                    default:
                        break;
                }
            }
        } else {
            return new JsonResponse(505, "Missing valid Token Group type").toJson();
        }
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            return getTokenGroupDetailsResponse(us, userId, tokenGroupName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TokenGroupMgmt", "tokenGroupDetails", error ? 0 : 1, userId);
        }
    }

    private String getTokenGroupDetailsResponse(UserSession us, int userId, String tokenGroupName) {
        try (DbSession session = DbSession.newSession()) {
            TokenGroupManager tokenGroupMgr = TokenGroupManager.newInstance(session, tokenGroupName, us.getLoginTenant());
            Membership membership = tokenGroupMgr.getStaticMembership(session, userId);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("tokenGroupName", tokenGroupName);
            if (null != membership) {
                resp.putResult("membershipName", membership.getName());
                resp.putResult("tokenGroupType", membership.getType().ordinal());
                if (membership.getType() == TokenGroupType.TOKENGROUP_TENANT) {
                    resp.putResult("tenantId", membership.getTenant().getId());
                    resp.putResult("tenantName", membership.getTenant().getName());
                }
                if (membership.getType() == TokenGroupType.TOKENGROUP_PROJECT) {
                    resp.putResult("projectId", membership.getProject().getId());
                    Tenant parentTenant = membership.getProject().getParentTenant();
                    resp.putResult("parentTenantId", parentTenant.getId());
                    resp.putResult("parentTenantName", parentTenant.getName());
                }
                return resp.toJson();
            } else {
                Tenant tenant = getTenantById(session, us.getLoginTenant());
                if (new SystemBucketManagerImpl().isSystemBucket(tokenGroupName, tenant.getName())) {
                    resp.putResult("membershipName", UserMgmt.generateDynamicMemberName(userId, tokenGroupName));
                    resp.putResult("tokenGroupType", TokenGroupType.TOKENGROUP_SYSTEMBUCKET.ordinal());
                    resp.putResult("parentTenantId", tenant.getId());
                    resp.putResult("parentTenantName", tenant.getName());
                    return resp.toJson();
                } else {
                    Project dynamicProject = getDynamicProjectIfAccessible(session, us, tokenGroupName);
                    if (dynamicProject != null) {
                        resp.putResult("membershipName", UserMgmt.generateDynamicMemberName(userId, tokenGroupName));
                        resp.putResult("tokenGroupType", TokenGroupType.TOKENGROUP_PROJECT.ordinal());
                        resp.putResult("projectId", dynamicProject.getId());
                        Tenant parentTenant = dynamicProject.getParentTenant();
                        resp.putResult("parentTenantId", parentTenant.getId());
                        resp.putResult("parentTenantName", parentTenant.getName());
                        return resp.toJson();
                    } else {
                        return new JsonResponse(502, "Invalid Token Group Name").toJson();
                    }
                }
            }
        } catch (TokenGroupException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(502, e.getMessage()).toJson();
        }
    }

    private static Tenant getTenantById(DbSession session, String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            return session.get(Tenant.class, tenantId);
        } else {
            return AbstractLogin.getDefaultTenant();
        }
    }

    private static Project getDynamicProjectIfAccessible(DbSession session, UserSession us, String tokenGroupName) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.createCriteria("keystore", "ks");
        criteria.add(Restrictions.eq("ks.tokenGroupName", tokenGroupName));
        Project dynamicProject = (Project)criteria.uniqueResult();

        if (dynamicProject != null && MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, dynamicProject.getId())) {
            return dynamicProject;
        }
        return null;
    }
}
