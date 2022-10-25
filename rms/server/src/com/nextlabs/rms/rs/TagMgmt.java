package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonTag;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.exception.TagException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tag;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.TagService;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/tags")
public class TagMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Secured
    @GET
    @Path("/tenant/{tenantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTenantTags(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenantId") String tenantId,
        @QueryParam("type") int tagType) {
        boolean error = true;
        try {
            try (DbSession session = DbSession.newSession()) {
                Tenant tenant = session.get(Tenant.class, tenantId);
                if (tenant == null) {
                    return new JsonResponse(400, "Invalid tenant.").toJson();
                }
                Membership membership = TenantMgmt.getMembership(session, userId, tenantId);
                if (membership == null) {
                    return new JsonResponse(401, "User not part of tenant").toJson();
                }

                if (tagType >= Tag.TagType.values().length) {
                    return new JsonResponse(402, "Invalid tag type").toJson();
                }

                List<JsonTag> jsonTags = TagService.getTenantTags(session, tenantId, tagType);
                JsonResponse response = new JsonResponse("OK");
                error = false;
                response.putResult("tags", jsonTags);
                return response.toJson();
            }

        } catch (Throwable e) {
            LOGGER.error("Error occurred during fetching tags for tenant", e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "TagMgmt", "getTags", !error ? 1 : 0, userId);
        }

    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tenant/{tenantId}")
    public String updateTenantTags(@Context HttpServletRequest request, String json, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tenantId") String tenantId) {
        Tenant tenant = null;
        boolean error = true;

        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            tenant = session.get(Tenant.class, tenantId);
            if (tenant == null || StringUtils.hasText(tenant.getParentId())) {
                return new JsonResponse(403, "Invalid tenant").toJson();
            }
            User user = us.getUser();
            boolean isSaasMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.SAAS, "false"));
            if (!isSaasMode && !(tenant.isAdmin(user.getEmail()))) {
                return new JsonResponse(401, "Access denied").toJson();
            }
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            int tagType = req.getIntParameter("tagType", 99);
            if (tagType >= Tag.TagType.values().length) {
                return new JsonResponse(402, "Invalid tag type").toJson();
            }
            List<JsonWraper> tagMapList = req.getParameterAsList("tags");
            List<JsonTag> tagList = new ArrayList<>();
            if (tagMapList == null) {
                return new JsonResponse(405, "Missing tag values").toJson();
            }
            if (tagMapList.isEmpty()) {
                return new JsonResponse(405, "Tag List cannot be empty").toJson();
            }

            for (JsonWraper wraper : tagMapList) {
                JsonTag jsonTag = wraper.getAsObject(JsonTag.class);
                tagList.add(jsonTag);
            }
            TagService.persistTenantTags(session, tenant, tagList, tagType);
            JsonResponse response = new JsonResponse("OK");
            error = false;
            return response.toJson();
        } catch (TagException e) {
            LOGGER.error("Failed to update project tags (tenantName: {}): {}", tenant.getName(), e.getMessage());
            return new JsonResponse(500, "Error occured while updating tenant tags").toJson();
        } catch (Throwable e) {
            LOGGER.error("Failed to update project tags (tenantName: {}): {}", tenant.getName(), e.getMessage());
            return new JsonResponse(500, "Error occured while updating tenant tags").toJson();
        } finally {
            Audit.audit(request, "API", "TagMgmt", "updateTenantTags", !error ? 1 : 0, userId);
        }

    }

    @Secured
    @GET
    @Path("/project/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProjectTags(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId) {
        boolean error = true;
        String projectName = null;
        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed.").toJson();
            }
            Project project = ProjectService.getProject(session, us, projectId);

            if (project == null) {
                return new JsonResponse(400, "Invalid Project").toJson();
            }
            projectName = project.getName();
            List<JsonTag> jsonTags = TagService.getProjectTags(session, project.getId());
            JsonResponse response = new JsonResponse("OK");
            error = false;
            response.putResult("tags", jsonTags);
            return response.toJson();

        } catch (Throwable e) {
            LOGGER.error("Error occurred during fetching tags (projectName: {}): {}", projectName, e.getMessage());
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "TagMgmt", "getProjectTags", !error ? 1 : 0, userId);
        }

    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/project/{projectId}")
    public String updateProjectTags(@Context HttpServletRequest request, String json, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId) {
        String projectName = null;
        boolean error = true;

        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (us == null) {
                return new JsonResponse(400, "Authentication failed.").toJson();
            }
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(402, "Missing request").toJson();
            }
            if (projectId == 0) {
                return new JsonResponse(403, "Invalid project id").toJson();
            }
            Project project = ProjectService.getProject(session, us, projectId);
            if (project == null) {
                return new JsonResponse(404, "Invalid Project").toJson();
            }
            User user = us.getUser();
            if (!project.getOwner().equalsIgnoreCase(user.getEmail())) {
                return new JsonResponse(401, "Access denied").toJson();
            }

            projectName = project.getName();
            List<JsonWraper> tagMapList = req.getParameterAsList("projectTags");
            TagService.persistProjectTags(session, tagMapList, project);
            JsonResponse response = new JsonResponse("OK");
            error = false;
            return response.toJson();
        } catch (TagException e) {
            LOGGER.error("Failed to update project tags (projectName: {}): {}", projectName, e.getMessage());
            return new JsonResponse(500, "Error occured while updating project tags").toJson();
        } catch (Throwable e) {
            LOGGER.error("Failed to update project tags (projectName: {}): {}", projectName, e.getMessage());
            return new JsonResponse(500, "Error occured while updating project tags").toJson();
        } finally {
            Audit.audit(request, "API", "TagMgmt", "updateProjectTags", !error ? 1 : 0, userId);
        }

    }

}
