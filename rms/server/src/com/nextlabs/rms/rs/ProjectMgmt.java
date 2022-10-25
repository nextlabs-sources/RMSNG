package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.ProjectUploadType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonProject;
import com.nextlabs.common.shared.JsonProjectFileInfo;
import com.nextlabs.common.shared.JsonProjectFileList;
import com.nextlabs.common.shared.JsonProjectInvitation;
import com.nextlabs.common.shared.JsonProjectInvitationList;
import com.nextlabs.common.shared.JsonProjectMemberDetails;
import com.nextlabs.common.shared.JsonProjectMemberList;
import com.nextlabs.common.shared.JsonRepositoryFileEntry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.UnsupportedNxlVersionException;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.cache.TokenGroupCache;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.ProjectStorageExceededException;
import com.nextlabs.rms.exception.TagException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ActivityLog;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectInvitation;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.Tag;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.InvalidFileOverwriteException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.TagService;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.RabbitMQUtil;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.ProjectDownloadUtil;
import com.nextlabs.rms.util.ProjectUploadUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;
import com.nextlabs.rms.util.RestUploadUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@Path("/project")
public class ProjectMgmt {

    public static final int MAX_PROJECT_NAME_LENGTH = 50;
    public static final int MAX_PROJECT_DESCRIPTION_LENGTH = 250;
    public static final int MAX_PROJECT_INVITATION_MESSAGE_LENGTH = 250;
    public static final Type FILEINFO_TYPE = new TypeToken<FileInfo>() {
    }.getType();
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    ProjectDownloadUtil util = new ProjectDownloadUtil();

    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getProjects(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("ownedByMe") Boolean ownedByMe,
        @QueryParam("page") Integer page,
        @QueryParam("size") Integer size,
        @QueryParam("orderBy") String orderBy) {
        List<JsonProject> jsonProjects = new ArrayList<>();
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            page = page != null && page > 0 ? page : 1;
            size = size != null && size > 0 ? size : -1;
            try (DbSession session = DbSession.newSession()) {
                long projectNum = ProjectService.getTotalProjects(session, us, ownedByMe);
                List<Project> projects;
                List<Order> orders = Collections.emptyList();
                if (StringUtils.hasText(orderBy)) {
                    Map<String, String> supportedFields = new HashMap<>(2);
                    supportedFields.put("lastActionTime", "projectActionTime");
                    supportedFields.put("name", "project.name");
                    OrderBuilder builder = new OrderBuilder(supportedFields);
                    List<String> list = StringUtils.tokenize(orderBy, ",");
                    for (String s : list) {
                        builder.add(s);
                    }
                    orders = builder.build();
                }
                projects = ProjectService.getProjects(session, us, orders, ownedByMe, size, page);
                for (Project project : projects) {
                    JsonProject jsonProject = ProjectService.getProjectMetadata(session, us, project, true);
                    jsonProjects.add(jsonProject);
                }
                JsonResponse response = new JsonResponse("OK");
                response.putResult("detail", jsonProjects);
                response.putResult("totalProjects", projectNum);
                error = false;
                return response.toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getProjects", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProject(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId) {
        JsonProject jsonProject;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed.").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                jsonProject = ProjectService.getProjectMetadata(session, us, project, false);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", jsonProject);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getProject", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @GET
    @Path("/tokenGroupName")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProjectTokenGroupName(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("parentTenantName") String parentTenantName,
        @QueryParam("projectName") String projectName) {
        String tokenGroupName;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed.").toJson();
            }
            if (!StringUtils.hasText(parentTenantName)) {
                parentTenantName = AbstractLogin.getDefaultTenant().getName();
            }
            if (!StringUtils.hasText(projectName)) {
                return new JsonResponse(402, "Missing required query parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(Project.class);
                criteria.createCriteria("parentTenant", "pt");
                criteria.add(Restrictions.eq("pt.name", parentTenantName));
                criteria.add(Restrictions.eq("name", projectName));
                Project project = (Project)criteria.uniqueResult();
                if (project == null) {
                    return new JsonResponse(400, "Project " + projectName + " not found under tenant " + parentTenantName).toJson();
                }
                tokenGroupName = project.getKeystore().getTokenGroupName();
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("tokenGroupName", tokenGroupName);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getProjectTokenGroupName", !error ? 1 : 0, userId, projectName, parentTenantName);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/members")
    @Produces(MediaType.APPLICATION_JSON)
    public String listMembers(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("picture") Boolean showPicture, @QueryParam("q") String searchFields,
        @QueryParam("searchString") String searchString) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonProjectMemberList members;
            try (DbSession session = DbSession.newSession()) {
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = Collections.emptyList();
                if (StringUtils.hasText(orderBy)) {
                    Map<String, String> supportedFields = new HashMap<>(2);
                    supportedFields.put("displayName", "u.displayName");
                    supportedFields.put("creationTime", "m.creationTime");
                    OrderBuilder builder = new OrderBuilder(supportedFields);
                    List<String> list = StringUtils.tokenize(orderBy, ",");
                    for (String s : list) {
                        builder.add(s);
                    }
                    orders = builder.build();
                }
                List<String> searchFieldList = StringUtils.tokenize(searchFields, ",");
                members = ProjectService.getJsonActiveMembers(session, us, projectId, page, size, orders, showPicture, searchFieldList, searchString);
                if (members == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                util.updateProjectActionTime(userId, projectId);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", members);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "listMembers", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/membership")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMembership(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId) {
        boolean error = true;
        Membership membership;
        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            membership = ProjectService.getActiveMembership(session, user.getId(), projectId);
            if (membership == null) {
                boolean isDynamicMember = MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, projectId);
                if (isDynamicMember) {
                    Project abacProject = session.get(Project.class, projectId);
                    membership = new Membership();
                    membership.setName(UserMgmt.generateDynamicMemberName(userId, abacProject.getKeystore().getTokenGroupName()));
                    membership.setType(TokenGroupType.TOKENGROUP_PROJECT);
                    membership.setTenant(abacProject.getParentTenant());
                    membership.setProject(abacProject);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Unable to find active membership for user: {}, requester: {}, project ID: {}", user.getId(), user.getId(), projectId);
                    }
                    return new JsonResponse(404, "Not Found.").toJson();
                }
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("membership", new JsonMembership(membership.getName(), TokenGroupType.TOKENGROUP_PROJECT.ordinal(), null, membership.getProject().getId(), membership.getProject().getKeystore().getTokenGroupName()));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getMembership", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/member/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMemberInfo(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId,
        @PathParam("memberId") int memberId, @QueryParam("picture") Boolean showPicture) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonProjectMemberDetails memberDetail;
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }

                memberDetail = ProjectService.getJsonMemberDetails(session, memberId, project, showPicture);
                if (memberDetail == null) {
                    return new JsonResponse(400, "Invalid member.").toJson();
                }
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", memberDetail);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getMemberInfo", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @GET
    @Path("/user/invitation/pending")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPendingInvitationsForUser(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                List<JsonProjectInvitation> pendingInvitations = ProjectService.getJsonPendingInvitationsForUser(session, user.getEmail());
                JsonResponse resp = new JsonResponse("OK");
                long totalPending = ProjectService.getTotalPendingInvitationsForUser(session, user.getEmail());
                resp.putResult("totalPendingInvitations", totalPending);
                resp.putResult("pendingInvitations", pendingInvitations);
                error = false;
                return resp.toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "pendingInvitationsForUsers", error ? 0 : 1, userId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/invitation/pending")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPendingInvitations(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("q") String searchField, @QueryParam("searchString") String searchString) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonProjectInvitationList pendingList;
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project id.").toJson();
                }
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = Collections.emptyList();
                if (StringUtils.hasText(orderBy)) {
                    Map<String, String> supportedFields = new HashMap<>(2);
                    supportedFields.put("displayName", "inviteeEmail");
                    supportedFields.put("inviteTime", "inviteTime");
                    OrderBuilder builder = new OrderBuilder(supportedFields);
                    List<String> list = StringUtils.tokenize(orderBy, ",");
                    for (String s : list) {
                        builder.add(s);
                    }
                    orders = builder.build();
                }
                pendingList = ProjectService.getJsonPendingInvitations(session, projectId, page, size, orders, searchField, searchString);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("pendingList", pendingList);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "pendingInvitations", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/members/remove")
    @Produces(MediaType.APPLICATION_JSON)
    public String removeMember(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        Membership membership = null;
        boolean error = true;
        int memberId = -1;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            memberId = req.getIntParameter("memberId", -1);
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project id.").toJson();
                }
                User owner = ProjectService.getOwner(session, project);
                if (owner != null && user.getId() != owner.getId()) {
                    return new JsonResponse(5001, "Only Project owner can remove a member.").toJson();
                }
                if (owner != null && memberId == owner.getId()) {
                    return new JsonResponse(5002, "Project owner cannot be removed .").toJson();
                }
                membership = ProjectService.getActiveMembership(session, memberId, projectId);
                if (membership == null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Unable to find active membership for user: {}, requester: {}, project ID: {}", memberId, userId, projectId);
                    }
                    return new JsonResponse(404, "Not Found.").toJson();
                }
                membership.setStatus(Membership.Status.REMOVED);
                membership.setLastModified(new Date());
                session.beginTransaction();
                session.update(membership);
                session.commit();
                util.updateProjectActionTime(userId, projectId);
            }
            JsonResponse resp = new JsonResponse(204, "Member successfully removed");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Member (ID: {}, membership: {}, project ID: {}) successfully removed by user ID: {}", memberId, membership.getName(), projectId, userId);
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "removeMember", !error ? 1 : 0, userId, memberId, membership != null ? membership.getName() : "<none>", projectId);
        }
    }

    @Secured
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createProject(@Context HttpServletRequest request, String json, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        String projectName = null;
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            User user = us.getUser();
            boolean isSaasMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.SAAS, "false"));
            if (!isSaasMode && !(loginTenant.isAdmin(user.getEmail()) || loginTenant.isProjectAdmin(user.getEmail())) && !UserService.isAPIUserSession(us)) {
                return new JsonResponse(403, "Access denied").toJson();
            }
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            projectName = req.getParameter("projectName");

            String projectDescription = req.getParameter("projectDescription");
            String projectInvitationMessage = req.getParameter("invitationMsg");
            if (!StringUtils.hasText(projectName) || !StringUtils.hasText(projectDescription)) {
                return new JsonResponse(400, "Missing Parameters").toJson();
            }
            if (projectName.length() > MAX_PROJECT_NAME_LENGTH) {
                return new JsonResponse(4001, "Project Name Too Long").toJson();
            }
            if (!Validator.validateProjectName(projectName)) {
                return new JsonResponse(4003, "Project Name containing illegal special characters").toJson();
            }
            if (UserMgmt.GROUP_NAME_PUBLIC.equals(projectName)) {
                return new JsonResponse(4010, "'Public' is a reserved name and cannot be used as a project name.").toJson();
            }
            if (projectDescription.length() > MAX_PROJECT_DESCRIPTION_LENGTH) {
                return new JsonResponse(4002, "Project Description Too Long").toJson();
            }
            if (StringUtils.hasText(projectInvitationMessage) && projectInvitationMessage.length() > MAX_PROJECT_INVITATION_MESSAGE_LENGTH) {
                return new JsonResponse(4008, "Invitation Message Too Long").toJson();
            }
            Set<String> emails = util.extractEmails(req);
            if (!EmailUtils.validateEmails(emails)) {
                return new JsonResponse(4009, "Invalid emails").toJson();
            }
            req.addParameter("parentId", us.getLoginTenant());
            //forbid to create project when a same projectName has been created with same owner and under same parentTenant
            if (ProjectService.hasProject(session, projectName, us.getLoginTenant(), us.getUser().getEmail())) {
                LOGGER.warn("Duplicated project name: ", projectName);
                return new JsonResponse(4010, "Duplicated project name.").toJson();
            }
            List<JsonWraper> tagMapList = req.getParameterAsList("projectTags");
            if (tagMapList != null) {
                Set<Integer> tagIds = TagService.convertToTagIds(tagMapList);
                if (!tagIds.isEmpty() && !TagService.areTagsOwnedByTenant(session, us.getLoginTenant(), tagIds, Tag.TagType.PROJECT.ordinal())) {
                    LOGGER.warn("Tenant does not own one or more of TagIds specified");
                    return new JsonResponse(4012, "Invalid tag ID.").toJson();
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request to create project (tenantName: {}, user ID: {}) for project name: {}", loginTenant.getName(), userId, projectName);
            }
            KeyStoreEntry keyStoreEntry;
            try {
                keyStoreEntry = ProjectService.createProjectKeyStore(loginTenant.getName(), projectName, userId);
                ControlCenterManager.create(keyStoreEntry.getTokenGroupName(), com.nextlabs.common.shared.Constants.PolicyModelType.RESOURCE);
            } catch (TokenGroupException | GeneralSecurityException | IOException e) {
                LOGGER.warn("Error occured while creating the project (projectName: {}): {}", projectName);
                LOGGER.error(e.getMessage(), e);
                return new JsonResponse(500, "Error occured while creating the project.").toJson();
            }

            final Date now = new Date();
            session.beginTransaction();
            Project project = ProjectService.createProject(session, projectName, projectDescription, projectInvitationMessage, loginTenant, user, com.nextlabs.rms.hibernate.model.CustomerAccount.AccountType.PROJECT_TRIAL, keyStoreEntry);
            Membership membership = UserMgmt.addUserToProject(session, project, user, loginTenant, now, null, now);
            session.commit();

            TagService.persistProjectTags(session, tagMapList, project);

            String parentTenantId = project.getParentTenant().getId();
            String baseURL = HTTPUtil.getURI(request);
            if (parentTenantId != null) {
                Tenant parentTenant = session.get(Tenant.class, parentTenantId);
                baseURL = TokenGroupCache.lookupTokenGroup(parentTenant.getName());
            }
            if (!emails.isEmpty()) {
                List<String> nowInvited = new ArrayList<>(emails.size());
                List<String> alreadyMembers = new ArrayList<>(emails.size());
                List<String> alreadyInvited = new ArrayList<>(emails.size());
                ProjectService.persistAndSendInvitations(session, project, user, emails, nowInvited, alreadyMembers, alreadyInvited, baseURL, projectInvitationMessage);
            }
            util.updateProjectActionTime(userId, project.getId());
            ProjectService.addRootPath(userId, project.getId());

            JsonResponse response = new JsonResponse("OK");
            response.putResult("projectId", project.getId());
            response.putResult("membership", new JsonMembership(membership.getName(), TokenGroupType.TOKENGROUP_PROJECT.ordinal(), null, membership.getProject().getId(), membership.getProject().getKeystore().getTokenGroupName()));
            error = false;
            return response.toJson();
        } catch (TagException e) {
            LOGGER.error("Error occurred while creating project tags (user ID: {}): {}", userId, e.getMessage(), e);
            return new JsonResponse(500, "Error occured while creating the project tags.").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred while creating project (user ID: {}): {}", userId, e.getMessage(), e);
            return new JsonResponse(500, "Error occured while creating the project.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "createProject", !error ? 1 : 0, userId, projectName);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateProject(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        boolean fromListProjects = false;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            String projectDescription = req.getParameter("projectDescription");
            String projectInvitationMessage = req.getParameter("invitationMsg");
            if (!StringUtils.hasText(projectDescription)) {
                return new JsonResponse(400, "Missing Parameters").toJson();
            }
            if (StringUtils.hasText(projectDescription) && projectDescription.length() > MAX_PROJECT_DESCRIPTION_LENGTH) {
                return new JsonResponse(4002, "Project Description Too Long").toJson();
            }
            if (StringUtils.hasText(projectInvitationMessage) && projectInvitationMessage.length() > MAX_PROJECT_INVITATION_MESSAGE_LENGTH) {
                return new JsonResponse(4008, "Invitation Message Too Long").toJson();
            }
            JsonProject jsonProject;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project id.").toJson();
                }
                User owner = ProjectService.getOwner(session, project);
                if (owner == null || user.getId() != owner.getId()) {
                    return new JsonResponse(5001, "Only Project owner can edit project details.").toJson();
                }
                session.beginTransaction();
                if (StringUtils.hasText(projectDescription)) {
                    project.setDescription(projectDescription);
                }
                project.setDefaultInvitationMsg(projectInvitationMessage);
                project.setConfigurationModified(new Date());
                session.update(project);
                session.commit();

                jsonProject = ProjectService.getProjectMetadata(session, us, project, fromListProjects);
                util.updateProjectActionTime(userId, projectId);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", jsonProject);
            error = false;
            return response.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred while updating project details (user ID: {}, project ID: {}): {}", userId, projectId, e.getMessage(), e);
            return new JsonResponse(500, "Error occured while updating the project details.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "updateProject", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/invite")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String inviteUsersToProject(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            Set<String> emails = util.extractEmails(req);
            String projectInvitationMessage = req.getParameter("invitationMsg");

            if (emails.isEmpty() || !EmailUtils.validateEmails(emails)) {
                return new JsonResponse(400, "Bad request.").toJson();
            }
            if (StringUtils.hasText(projectInvitationMessage) && projectInvitationMessage.length() > MAX_PROJECT_INVITATION_MESSAGE_LENGTH) {
                return new JsonResponse(4008, "Invitation Message Too Long").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                util.updateProjectActionTime(userId, projectId);
                String baseURL;
                String parentTenantId = project.getParentTenant().getId();
                if (parentTenantId != null) {
                    Tenant parentTenant = session.get(Tenant.class, project.getParentTenant().getId());
                    baseURL = TokenGroupCache.lookupTokenGroup(parentTenant.getName());
                } else {
                    baseURL = HTTPUtil.getURI(request);
                }
                List<String> alreadyMembers = new ArrayList<>();
                List<String> alreadyInvited = new ArrayList<>();
                List<String> nowInvited = new ArrayList<>();
                ProjectService.persistAndSendInvitations(session, project, user, emails, nowInvited, alreadyMembers, alreadyInvited, baseURL, projectInvitationMessage);

                JsonResponse response = new JsonResponse("OK");
                response.putResult("alreadyMembers", alreadyMembers);
                response.putResult("alreadyInvited", alreadyInvited);
                response.putResult("nowInvited", nowInvited);
                error = false;
                return response.toJson();
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
            Audit.audit(request, "API", "ProjectMgmt", "inviteUsersToProject", !error ? 1 : 0, userId, projectId);
        }
    }

    @Secured
    @GET
    @Path("/accept")
    @Produces(MediaType.APPLICATION_JSON)
    public String acceptInvitation(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("ticket") String ticket) {
        boolean error = true;
        String invitationId = null;
        try {
            invitationId = request.getParameter("id");
            String hmac = request.getParameter("code");
            JsonResponse resp = new JsonResponse("OK");
            if (ProjectService.isValidInvitation(invitationId, hmac)) {
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                User user = us.getUser();
                try (DbSession session = DbSession.newSession()) {
                    String queryString = "select p from ProjectInvitation p inner join fetch p.project pp inner join fetch p.inviter pi inner join fetch pp.keystore where p.id = :id";
                    Query query = session.createQuery(queryString);
                    query.setParameter("id", Long.valueOf(invitationId));
                    ProjectInvitation invitation = (ProjectInvitation)query.uniqueResult();
                    if (invitation == null) {
                        return new JsonResponse(404, "Not found.").toJson();
                    }
                    queryString = "select id from User u where lower(u.email) = lower(:email)";
                    query = session.createQuery(queryString);
                    query.setParameter("email", invitation.getInviteeEmail());
                    Integer desiredUId = (Integer)query.uniqueResult();
                    if (desiredUId == null || desiredUId != userId) {
                        return new JsonResponse(4003, "Login email mismatch!").toJson();
                    }

                    java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                    Date now = new Date();
                    if (invitation.getExpireDate().before(today)) {
                        if (invitation.getStatus() != ProjectInvitation.Status.EXPIRED) {
                            session.beginTransaction();
                            invitation.setActionTime(now);
                            invitation.setStatus(ProjectInvitation.Status.EXPIRED);
                            session.update(invitation);
                            session.commit();
                        }
                        return new JsonResponse(4001, "Invitation expired").toJson();
                    }
                    if (invitation.getStatus() == ProjectInvitation.Status.DECLINED) {
                        return new JsonResponse(4002, "Invitation already declined").toJson();
                    }
                    if (invitation.getStatus() == ProjectInvitation.Status.REVOKED) {
                        return new JsonResponse(4006, "Invitation already revoked").toJson();
                    }
                    Project project = invitation.getProject();
                    Tenant parentTenant = project.getParentTenant();
                    boolean checkUserProjectMembershipStatus = ProjectService.checkUserProjectMembership(session, us, project.getId(), false);
                    if (invitation.getStatus() == ProjectInvitation.Status.ACCEPTED) {
                        if (!checkUserProjectMembershipStatus) {
                            return new JsonResponse(4006, "You cannot join the project because the membership has been removed").toJson();
                        } else {
                            return new JsonResponse(4005, "Invitation already accepted").toJson();
                        }
                    }
                    if (checkUserProjectMembershipStatus) {
                        return new JsonResponse(4004, "Already member!").toJson();
                    }

                    session.beginTransaction();
                    Membership membership = UserMgmt.addUserToProject(session, project, user, parentTenant, now, invitation.getInviter(), invitation.getInviteTime());
                    invitation.setActionTime(now);
                    invitation.setStatus(ProjectInvitation.Status.ACCEPTED);
                    session.update(invitation);
                    session.commit();
                    resp.putResult("projectId", project.getId());
                    resp.putResult("membership", new JsonMembership(membership.getName(), TokenGroupType.TOKENGROUP_PROJECT.ordinal(), null, membership.getProject().getId(), membership.getProject().getKeystore().getTokenGroupName()));
                    util.sendInvitationAcceptedMail(request, invitation, user);
                    util.updateProjectActionTime(userId, project.getId());
                }
                error = false;
                return resp.toJson();
            } else {
                return new JsonResponse(400, "Invalid request parameters").toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "acceptInvitation", !error ? 1 : 0, userId, invitationId);
        }
    }

    @POST
    @Path("/decline")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String declineInvitation(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @FormParam("id") String invitationId, @FormParam("code") String hmac,
        @FormParam("declineReason") String reason) {
        boolean error = true;
        try {
            if (ProjectService.isValidInvitation(invitationId, hmac)) {
                if (StringUtils.hasText(reason) && reason.length() > 250) {
                    // our column size is 250
                    return new JsonResponse(4007, "Decline reason too long").toJson();
                }
                ProjectInvitation invitation;
                User inviter;
                Project project;
                try (DbSession session = DbSession.newSession()) {
                    String queryString = "select p from ProjectInvitation p inner join fetch p.project pp inner join fetch p.inviter pi where p.id = :id";
                    Query query = session.createQuery(queryString);
                    query.setParameter("id", Long.valueOf(invitationId));
                    invitation = (ProjectInvitation)query.uniqueResult();
                    if (invitation == null) {
                        return new JsonResponse(404, "Not found.").toJson();
                    }

                    java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                    Date now = new Date();
                    if (invitation.getExpireDate().before(today)) {
                        if (invitation.getStatus() != ProjectInvitation.Status.EXPIRED) {
                            session.beginTransaction();
                            invitation.setActionTime(now);
                            invitation.setStatus(ProjectInvitation.Status.EXPIRED);
                            session.update(invitation);
                            session.commit();
                        }
                        return new JsonResponse(4001, "Invitation expired").toJson();
                    }
                    if (invitation.getStatus() == ProjectInvitation.Status.ACCEPTED) {
                        return new JsonResponse(4005, "Invitation already accepted").toJson();
                    }
                    if (invitation.getStatus() == ProjectInvitation.Status.DECLINED) {
                        return new JsonResponse(4002, "Invitation already declined").toJson();
                    }
                    if (invitation.getStatus() == ProjectInvitation.Status.REVOKED) {
                        return new JsonResponse(4006, "Invitation already revoked").toJson();
                    }
                    if (StringUtils.hasText(reason)) {
                        invitation.setComment(reason);
                    }
                    project = invitation.getProject();
                    inviter = invitation.getInviter();
                    session.beginTransaction();
                    invitation.setActionTime(now);
                    invitation.setStatus(ProjectInvitation.Status.DECLINED);
                    session.update(invitation);
                    session.commit();
                }
                util.sendInvitationDeclinedMail(request, inviter, invitation.getInviteeEmail(), reason, project);
                JsonResponse resp = new JsonResponse("OK");
                error = false;
                return resp.toJson();
            } else {
                return new JsonResponse(400, "Invalid request parameters").toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "declineInvitation", !error ? 1 : 0, invitationId);
        }
    }

    @Secured
    @POST
    @Path("/sendReminder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendReminder(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        long invitationId = -1;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            invitationId = req.getLongParameter("invitationId", -1);
            ProjectInvitation invitation;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                String queryString = "select p from ProjectInvitation p inner join fetch p.project pp inner join fetch p.inviter pi where p.id = :id";
                Query query = session.createQuery(queryString);
                query.setParameter("id", invitationId);
                invitation = (ProjectInvitation)query.uniqueResult();
                if (invitation == null) {
                    return new JsonResponse(404, "Invitation not found.").toJson();
                }
                if (!StringUtils.equalsIgnoreCase(user.getEmail(), invitation.getProject().getOwner())) {
                    return new JsonResponse(403, "Unauthorized to perform operation").toJson();
                }
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                Date now = new Date();
                if (invitation.getExpireDate().before(today)) {
                    if (invitation.getStatus() != ProjectInvitation.Status.EXPIRED) {
                        session.beginTransaction();
                        invitation.setActionTime(now);
                        invitation.setStatus(ProjectInvitation.Status.EXPIRED);
                        session.update(invitation);
                        session.commit();
                    }
                    return new JsonResponse(4001, "Invitation expired").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.ACCEPTED) {
                    return new JsonResponse(4005, "Invitation already accepted").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.DECLINED) {
                    return new JsonResponse(4002, "Invitation already declined").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.REVOKED) {
                    return new JsonResponse(4006, "Invitation already revoked").toJson();
                }
                util.updateProjectActionTime(userId, invitation.getProject().getId());
            }
            util.sendInvitationReminderMail(request, invitation);
            JsonResponse resp = new JsonResponse("OK");
            error = false;
            return resp.toJson();

        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "sendReminder", !error ? 1 : 0, userId, invitationId);
        }
    }

    @Secured
    @POST
    @Path("/revokeInvite")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String revokePendingInvite(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        long invitationId = -1;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            invitationId = req.getLongParameter("invitationId", -1);
            ProjectInvitation invitation;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                String queryString = "select p from ProjectInvitation p inner join fetch p.project pp inner join fetch p.inviter pi where p.id = :id";
                Query query = session.createQuery(queryString);
                query.setParameter("id", invitationId);
                invitation = (ProjectInvitation)query.uniqueResult();
                if (invitation == null) {
                    return new JsonResponse(404, "Invitation not found").toJson();
                }
                if (!StringUtils.equalsIgnoreCase(user.getEmail(), invitation.getProject().getOwner())) {
                    return new JsonResponse(403, "Unauthorized to perform operation").toJson();
                }

                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                Date now = new Date();
                if (invitation.getExpireDate().before(today)) {
                    if (invitation.getStatus() != ProjectInvitation.Status.EXPIRED) {
                        session.beginTransaction();
                        invitation.setActionTime(now);
                        invitation.setStatus(ProjectInvitation.Status.EXPIRED);
                        session.update(invitation);
                        session.commit();
                    }
                    return new JsonResponse(4001, "Invitation expired").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.ACCEPTED) {
                    return new JsonResponse(4005, "Invitation already accepted").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.DECLINED) {
                    return new JsonResponse(4002, "Invitation already declined").toJson();
                }
                if (invitation.getStatus() == ProjectInvitation.Status.REVOKED) {
                    return new JsonResponse(4006, "Invitation already revoked").toJson();
                }
                session.beginTransaction();
                invitation.setActionTime(now);
                invitation.setStatus(ProjectInvitation.Status.REVOKED);
                session.update(invitation);
                session.commit();
                util.updateProjectActionTime(userId, invitation.getProject().getId());
            }
            JsonResponse resp = new JsonResponse("OK");
            error = false;
            return resp.toJson();

        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "revokePendingInvite", !error ? 1 : 0, userId, invitationId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/preference")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProjectPreference(@Context HttpServletRequest request,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @PathParam("projectId") int projectId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Project project = ProjectService.getProject(session, us, projectId);
            JsonExpiry expiry = new Gson().fromJson(project.getExpiry(), JsonExpiry.class);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("expiry", expiry);
            resp.putResult("watermark", project.getWatermark());
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getProjectPreference", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @PUT
    @Path("/{projectId}/preference")
    @Produces(MediaType.APPLICATION_JSON)
    public String setProjectPreference(@Context HttpServletRequest request,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @PathParam("projectId") int projectId, String json) {
        boolean error = true;
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request.").toJson();
        }
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        try (DbSession session = DbSession.newSession()) {
            JsonObject expiryJson = (JsonObject)req.getWrappedParameter(Constants.PARAM_EXPIRY);
            String watermark = req.getParameter(Constants.PARAM_WATERMARK);
            if (expiryJson == null && !StringUtils.hasText(watermark)) {
                return new JsonResponse(401, "Missing required parameters.").toJson();
            }
            if (watermark != null) {
                if (watermark.length() > 50) {
                    return new JsonResponse(4001, "Watermark too long").toJson();
                } else if ("".equals(watermark)) {
                    return new JsonResponse(4001, "Watermark empty").toJson();
                }
            }
            Map<String, Object> prefsSavedMap = null;
            if (expiryJson != null) {
                try {
                    prefsSavedMap = ExpiryUtil.validateExpiry(expiryJson);
                    if (prefsSavedMap == null) {
                        return new JsonResponse(4003, "Invalid expiry.").toJson();
                    }
                } catch (NumberFormatException | IllegalStateException e) {
                    return new JsonResponse(4002, "Invalid parameters.").toJson();
                }
            }
            Project project = ProjectService.getProject(session, us, projectId);
            if (project == null) {
                return new JsonResponse(400, "Invalid project id.").toJson();
            }
            if (prefsSavedMap != null) {
                project.setExpiry(GsonUtils.GSON.toJson(prefsSavedMap));
            }
            project.setWatermark(watermark);
            session.beginTransaction();
            session.update(project);
            session.commit();
            error = false;
            return new JsonResponse(200, "OK").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "setProjectPreference", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/files")
    @Produces(MediaType.APPLICATION_JSON)
    public String listFiles(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("pathId") String parentPath, @QueryParam("q") String searchFields,
        @QueryParam("searchString") String searchString,
        @QueryParam("filter") String filter) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!"/".equals(Nvl.nvl(parentPath, "/")) && !ProjectService.pathExists(String.valueOf(projectId), parentPath)) {
                return new JsonResponse(404, "Project folder not found.").toJson();
            }
            JsonProjectFileList fileList;
            Map<String, Long> myProjectStatus;
            Long folderLastUpdatedTime = null;
            try (DbSession session = DbSession.newSession()) {
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = util.getOrdersList(orderBy);
                List<String> searchFieldList = StringUtils.tokenize(searchFields, ",");
                String processedSearchString = StringUtils.hasText(searchString) ? searchString.replace("^\\.+", "").replaceAll("[\\\\/:*?\"'<>|]", "") : "";
                fileList = ProjectService.getJsonFileList(session, us, projectId, page, size, orders, parentPath, searchFieldList, processedSearchString, filter);
                if (fileList == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                myProjectStatus = ProjectService.getMyProjectStatus(session, projectId);
                util.updateProjectActionTime(userId, projectId);
                if (parentPath != null) {
                    folderLastUpdatedTime = ProjectService.getFolderLastUpdatedTime(session, projectId, parentPath);
                }
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", fileList);
            if (parentPath != null) {
                response.putResult("folderLastUpdatedTime", folderLastUpdatedTime);
            }
            response.putResult("usage", myProjectStatus.get(RepoConstants.STORAGE_USED));
            response.putResult("quota", myProjectStatus.get(ProjectService.PROJECT_QUOTA));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "listFiles", !error ? 1 : 0, userId, projectId, parentPath);
        }
    }

    @Secured
    @GET
    @Path("/{projectId}/folderMetadata")
    @Produces(MediaType.APPLICATION_JSON)
    public String folderMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("pathId") String parentPath, @QueryParam("lastModified") long lastModifiedTime) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (lastModifiedTime == 0L || !StringUtils.hasText(parentPath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (!util.checkProjectExist(projectId)) {
                return new JsonResponse(400, "Invalid project").toJson();
            }
            if (!"/".equals(Nvl.nvl(parentPath, "/")) && !ProjectService.pathExists(String.valueOf(projectId), parentPath)) {
                return new JsonResponse(404, "Project folder not found.").toJson();
            }
            JsonProjectFileList fileList;
            Map<String, Long> myProjectStatus;
            Long folderLastUpdatedTime;
            try (DbSession session = DbSession.newSession()) {
                boolean folderUpdated = ProjectService.checkFolderUpdated(session, userId, projectId, parentPath, lastModifiedTime);
                if (!folderUpdated) {
                    return new JsonResponse(304, "Folder not modified.").toJson();
                }
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = util.getOrdersList(orderBy);
                fileList = ProjectService.getJsonFileList(session, us, projectId, page, size, orders, parentPath, null, null, null);
                if (fileList == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                myProjectStatus = ProjectService.getMyProjectStatus(session, projectId);
                folderLastUpdatedTime = ProjectService.getFolderLastUpdatedTime(session, projectId, parentPath);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", fileList);
            response.putResult("folderLastUpdatedTime", folderLastUpdatedTime);
            response.putResult("usage", myProjectStatus.get(RepoConstants.STORAGE_USED));
            response.putResult("quota", myProjectStatus.get(ProjectService.PROJECT_QUOTA));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "folderMetadata", !error ? 1 : 0, userId, parentPath, projectId);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/file/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getFileInfo(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        boolean error = true;
        String filePath = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            filePath = req.getParameter("pathId");
            if (!StringUtils.hasText(filePath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();

            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                RMSUserPrincipal principal = new RMSUserPrincipal(us, project.getParentTenant());
                String deviceId = request.getHeader("deviceId");
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(userId)).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();

                JsonProjectFileInfo fileInfo = ProjectService.getProjectFileInfo(session, principal, userEval, projectId, filePath);
                if (fileInfo == null) {
                    return new JsonResponse(404, "Invalid file.").toJson();
                }
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("fileInfo", fileInfo);
                error = false;
                return resp.toJson();
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
            Audit.audit(request, "API", "ProjectMgmt", "getFileInfo", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/file/checkIfExists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String checkFileExists(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        boolean error = true;
        String filePath = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            filePath = req.getParameter("pathId");
            if (!StringUtils.hasText(filePath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);

            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
            }

            boolean fileExists = ProjectService.checkFileExists(us, us.getLoginTenant(), projectId, filePath);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("fileExists", fileExists);
            error = false;
            return resp.toJson();

        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "checkFileExists", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/file/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String validate(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId) {
        RestUploadRequest uploadReq = null;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant parentTenant;
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                parentTenant = project.getParentTenant();
            }
            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            if (uploadReq.getFileStream() == null) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }

            try (NxlFile nxlMetadata = NxlFile.parse(uploadReq.getFileStream())) {
                String duid = nxlMetadata.getDuid();
                String owner = nxlMetadata.getOwner();
                Rights[] rights;
                if (!StringUtils.hasText(duid) || !StringUtils.hasText(owner)) {
                    return new JsonResponse(5001, "Invalid Nxl file").toJson();
                } else if (Validator.validateMembership(nxlMetadata, projectId)) {
                    String membership = nxlMetadata.getOwner();
                    Map<String, String[]> tags = DecryptUtil.getTags(nxlMetadata, null);
                    EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxlMetadata);
                    rights = evalResponse.getRights();
                    if (rights.length == 0) {
                        File tmpFile = new File(uploadReq.getUploadDir(), uploadReq.getFileName());
                        Files.copy(uploadReq.getFileStream(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        User user = us.getUser();
                        com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(request.getHeader("deviceId")).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                        List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(uploadReq.getFileName(), membership, parentTenant.getName(), userEval, tags);
                        rights = PolicyEvalUtil.getUnionRights(responses);
                    }
                    JsonResponse resp = new JsonResponse("OK");
                    resp.putResult("rights", rights);
                    resp.putResult("protectionType", ProtectionType.ADHOC.ordinal());
                    error = false;
                    return resp.toJson();
                }
            } catch (NxlException e) {
                return new JsonResponse(5001, "Invalid NXL format.").toJson();
            }
            return new JsonResponse(5002, "NXL file is not from this project.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "ProjectMgmt", "validate", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @GET
    @Path("/uploadTicket")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTicket(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        JsonResponse resp = new JsonResponse("OK");
        resp.putResult("exchange", RabbitMQUtil.PROJECT_UPLOAD_EXCHANGE_NAME);
        resp.putResult("routing_key", RabbitMQUtil.generateRoutingKey(userId));
        return resp.toJson();
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{projectId}/uploadLargeFile/{routingKey}")
    public String uploadLargeFile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @PathParam("projectId") int projectId, @PathParam("routingKey") String routingKey) {
        if (!StringUtils.hasText(routingKey)) {
            new JsonResponse(400, "Missing routing key").toJson();
        }
        String responseMessage = upload(request, userId, ticket, platformId, deviceId, clientId, projectId);
        RabbitMQUtil.sendDirectMessage(routingKey, RabbitMQUtil.PROJECT_UPLOAD_EXCHANGE_NAME, responseMessage);
        return responseMessage;
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{projectId}/upload")
    public String upload(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @PathParam("projectId") int projectId) {
        DefaultRepositoryTemplate repository = null;
        RestUploadRequest uploadReq = null;
        String fileParentPathId = null;
        String fileName = null;
        String duid = null;
        String uniqueResourceId = null;
        boolean allowOverwrite = false;
        boolean userConfirmedFileOverwrite = false;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, StandardCharsets.UTF_8.name()) : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (deviceType == null) {
                return new JsonResponse(400, "Unknown platform.").toJson();
            }

            Project project;
            Tenant parentTenant;
            RMSUserPrincipal principal;
            String baseURL;
            try (DbSession session = DbSession.newSession()) {
                project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                principal = new RMSUserPrincipal(us, project.getParentTenant());
                repository = ProjectService.getProjectRepository(session, principal, projectId);
                util.updateProjectActionTime(userId, projectId);
                ProjectService.checkProjectStorageExceeded(session, projectId);
                parentTenant = session.get(Tenant.class, project.getParentTenant().getId());
                baseURL = TokenGroupCache.lookupTokenGroup(parentTenant.getName());
            }

            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            JsonRequest req = JsonRequest.fromJson(uploadReq.getJson());
            if (req == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            fileName = req.getParameter("name");
            fileParentPathId = req.getParameter("parentPathId");
            String userConfirmedFileOverwriteReq = req.getParameter("userConfirmedFileOverwrite");
            ProjectUploadType uploadType = ProjectUploadUtil.validateUploadType(req.getIntParameter("type", ProjectUploadType.UPLOAD_PROJECT_SYSBUCKET.ordinal()));
            userConfirmedFileOverwrite = Boolean.parseBoolean(userConfirmedFileOverwriteReq);
            if (!"/".equals(Nvl.nvl(fileParentPathId, "/")) && !ProjectService.pathExists(String.valueOf(projectId), fileParentPathId)) {
                return new JsonResponse(404, "Project folder not found.").toJson();
            }
            if (!StringUtils.hasText(fileName) || !StringUtils.hasText(fileParentPathId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (fileName.length() > Constants.MAX_FILE_NAME_LENGTH) {
                return new JsonResponse(4005, "File name cannot exceed " + Constants.MAX_FILE_NAME_LENGTH + " characters").toJson();
            }

            File tmpFile;
            @SuppressWarnings("unchecked")
            Map<String, String> source = req.getParameter("source", Map.class);
            if (source != null) {
                String repoId = source.get("repoId");
                String pathId = source.get("pathId");
                String pathDisplay = source.get("pathDisplay");
                if (!StringUtils.hasText(repoId) || (!StringUtils.hasText(pathId) && !StringUtils.hasText(pathDisplay))) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                }
                IRepository personalRepo;
                try (DbSession session = DbSession.newSession()) {
                    personalRepo = RepositoryFactory.getInstance().getRepository(session, principal, repoId);
                }
                tmpFile = RepositoryFileUtil.downloadFileFromRepo(personalRepo, pathId, pathDisplay, uploadReq.getUploadDir());
            } else {
                if (uploadReq.getFileStream() == null) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                }
                tmpFile = new File(uploadReq.getUploadDir(), fileName);
                Files.copy(uploadReq.getFileStream(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (tmpFile.length() == 0) {
                return new JsonResponse(5005, "Empty files are not allowed to be uploaded.").toJson();
            }

            boolean isNxl = false;
            boolean isAdhocPolicy = false;
            String owner = null;
            Rights[] rights = null;
            Long lastModified = null;
            Long creationTime = null;
            String createdBy = null;
            String lastModifiedBy = null;
            ProjectUploadUtil projectUploadUtil = new ProjectUploadUtil();
            String path = HTTPUtil.getInternalURI(request);

            principal.setDeviceId(deviceId);
            tmpFile = projectUploadUtil.transformNxlFileForUpload(tmpFile, uploadType, principal, path, project, us, parentTenant, fileName);

            try (InputStream is = new FileInputStream(tmpFile);
                    NxlFile nxlMetadata = NxlFile.parse(is)) {
                if (nxlMetadata.getContentLength() == 0) {
                    return new JsonResponse(5005, "Empty files are not allowed to be uploaded.").toJson();
                }
                owner = nxlMetadata.getOwner();
                duid = nxlMetadata.getDuid();
                if (StringUtils.hasText(owner)) {
                    isNxl = true;
                    createdBy = util.getUserId(owner);
                    FileInfo fileInfo = DecryptUtil.getInfo(nxlMetadata, null);
                    lastModified = fileInfo.getLastModified();
                    creationTime = fileInfo.getCreationTime();
                    Map<String, String[]> tagMap = DecryptUtil.getTags(nxlMetadata, null);
                    lastModifiedBy = util.getUserId(fileInfo.getModifiedBy());
                    if (!Validator.validateMembership(nxlMetadata, projectId)) {
                        return new JsonResponse(5003, "The nxl file does not belong to this project.").toJson();
                    }
                    if (!TokenMgmt.checkNxlMetadata(duid, DecryptUtil.getFilePolicyStr(nxlMetadata, null), GsonUtils.GSON.toJson(tagMap), nxlMetadata.getProtectionType())) {
                        return new JsonResponse(5008, "The nxl file does not have valid metadata.").toJson();
                    }
                    try (DbSession session = DbSession.newSession()) {
                        ProjectSpaceItem item = ProjectService.getProjectFileByDUID(session, duid);
                        if (item != null && item.getProject() != null && item.getFilePath() != null && item.getProject().getId() == projectId && !item.getFilePath().equals(fileParentPathId + fileName.toLowerCase())) {
                            return new JsonResponse(5009, "The nxl file already exists in another location in this project.").toJson();
                        }
                    }

                    EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxlMetadata);
                    rights = evalResponse.getRights();
                    if (rights.length == 0) {
                        User user = us.getUser();
                        com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                        List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(uploadReq.getFileName(), owner, parentTenant.getName(), userEval, tagMap);
                        evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                        rights = PolicyEvalUtil.getUnionRights(responses);
                    } else {
                        isAdhocPolicy = true;
                    }
                }
            } catch (NxlException e) {
                if (isNxl) {
                    if (e instanceof UnsupportedNxlVersionException) {
                        return new JsonResponse(5006, "Unsupported NXL version").toJson();
                    } else {
                        return new JsonResponse(5001, "Invalid NXL format.").toJson();
                    }
                }
            }

            if (!isNxl) {
                String[] rightsList = req.getParameter("rightsJSON", String[].class);
                rights = SharedFileManager.toRights(rightsList);
                @SuppressWarnings("unchecked")
                Map<String, String[]> tags = req.getParameter("tags", Map.class);
                if (tags == null && (rights == null || rights.length == 0)) {
                    return new JsonResponse(5002, "Classification or Rights required.").toJson();
                }
                tags = Nvl.nvl(tags, Collections.emptyMap());
                String watermark = null;
                if (ArrayUtils.contains(rights, Rights.WATERMARK)) {
                    watermark = req.getParameter("watermark", String.class);
                    if (!StringUtils.hasText(watermark)) {
                        watermark = WatermarkConfigManager.getWaterMarkText(HTTPUtil.getInternalURI(request), AbstractLogin.getDefaultTenant().getName(), ticket, platformId, String.valueOf(userId), clientId);
                    }
                }
                File outputFile = new File(uploadReq.getUploadDir(), tmpFile.getName() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
                String membership = ProjectService.getMembership(principal, project);
                JsonExpiry expiryJson = req.getParameter("expiry", JsonExpiry.class);
                if (expiryJson == null) {
                    expiryJson = new JsonExpiry(0);
                }
                if (!(rights == null || rights.length == 0) && !ExpiryUtil.validateExpiry(expiryJson)) {
                    return new JsonResponse(4003, "Invalid expiry.").toJson();
                }
                try (OutputStream os = new FileOutputStream(outputFile)) {
                    try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, HTTPUtil.getInternalURI(request), membership, rights, watermark, expiryJson, tags, tmpFile, os))) {
                        duid = nxl.getDuid();
                        owner = nxl.getOwner();
                        createdBy = util.getUserId(owner);
                        FileInfo fileInfo = DecryptUtil.getInfo(nxl, null);
                        lastModified = fileInfo.getLastModified();
                        creationTime = fileInfo.getCreationTime();
                        lastModifiedBy = util.getUserId(fileInfo.getModifiedBy());
                    }
                }
                tmpFile = outputFile;
                uploadType = ProjectUploadType.UPLOAD_PROJECT_SYSBUCKET;
            } else {
                if (!StringUtils.endsWithIgnoreCase(fileName, com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
                    return new JsonResponse(5010, "NXL file must have .nxl file extension.").toJson();
                }
            }

            Map<String, String> customMetadata = projectUploadUtil.createCustomMetaDeta(duid, rights, createdBy, lastModifiedBy, creationTime, lastModified);

            if (ProjectUploadType.UPLOAD_EDIT == uploadType && userConfirmedFileOverwrite) {
                return new JsonResponse(400, "Invalid request parameters").toJson();
            }
            if (isNxl && uploadType == ProjectUploadType.UPLOAD_EDIT) {
                allowOverwrite = true;
                if ((!isAdhocPolicy || Integer.parseInt(createdBy) != userId) && !ArrayUtils.contains(rights, Rights.EDIT)) {
                    return new JsonResponse(403, "Access Denied").toJson();
                }
            }
            uniqueResourceId = UploadUtil.getUniqueResourceId(SPACETYPE.PROJECTSPACE, String.valueOf(project.getId()), fileParentPathId, tmpFile.getAbsolutePath());
            if ((allowOverwrite || userConfirmedFileOverwrite) && !LockManager.getInstance().acquireLock(uniqueResourceId, TimeUnit.MINUTES.toMillis(5))) {
                allowOverwrite = false;
                userConfirmedFileOverwrite = false;
                return new JsonResponse(4002, "Another User is editing this file.").toJson();
            }
            UploadedFileMetaData metadata = repository.uploadFile(fileParentPathId, null, tmpFile.getAbsolutePath(), allowOverwrite, "", customMetadata, userConfirmedFileOverwrite);

            if (!isNxl) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, Operations.PROTECT, deviceId, platformId, String.valueOf(projectId), metadata.getPathId(), tmpFile.getName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
                RemoteLoggingMgmt.saveActivityLog(activity);
            }

            Operations ops = ProjectUploadUtil.getUploadOps(uploadType);
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, ops, deviceId, platformId, String.valueOf(projectId), metadata.getPathId(), tmpFile.getName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            RemoteLoggingMgmt.saveActivityLog(activity);
            JsonResponse resp = new JsonResponse("OK");

            JsonRepositoryFileEntry entry = projectUploadUtil.getJsonRepoFileEntry(metadata, tmpFile);
            resp.putResult("entry", entry);
            ProjectService.sendFileUploadedLink(baseURL, tmpFile.getName(), project, us.getUser());
            Audit.audit(request, "API", "ProjectMgmt", "upload", 1, userId, fileParentPathId);
            error = false;
            return resp.toJson();
        } catch (InvalidFileNameException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid fileName (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, fileName, e);
            }
            return new JsonResponse(4001, "Invalid file name").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", uploadReq.getJson(), e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, fileParentPathId, e);
            }
            return new JsonResponse(404, "Parent folder missing.").toJson();
        } catch (ProjectStorageExceededException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Drive Storage Exceeded: {}", e.getStorage());
            }
            return new JsonResponse(6001, "Project Storage Exceeded.").toJson();
        } catch (FileConflictException e) {
            return new JsonResponse(4001, "File already exists").toJson();
        } catch (SocketTimeoutException e) {
            return new JsonResponse(500, "Socket timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when uploading file to Project (user ID: {}, fileName: {}, project: {}): {}", userId, fileName, projectId, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when uploading file to Project (user ID: {}, fileName: {}, project: {}): {}", userId, fileName, projectId, e.getMessage(), e);
            }
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (InvalidFileOverwriteException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Invalid File upload error occurred when uploading file to Project (user ID: {}, fileName: {}, project: {}): {}", userId, fileName, projectId, e.getMessage(), e);
            }
            return new JsonResponse(5007, "User can not overwrite a different File").toJson();

        } catch (ValidateException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Validation error occurred when uploading file to Project (user ID: {}, fileName: {}, project: {}): {}", userId, fileName, projectId, e.getMessage(), e);
            }
            return new JsonResponse(e.getErrorCode(), e.getMessage()).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if ((allowOverwrite || userConfirmedFileOverwrite) && uniqueResourceId != null) {
                LockManager.getInstance().releaseRemoveLock(uniqueResourceId);
            }
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "ProjectMgmt", "upload", !error ? 1 : 0, userId, fileParentPathId, fileName);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/createFolder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFolder(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        String folderName = null;
        String path = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            path = req.getParameter("parentPathId");
            folderName = req.getParameter("name");
            if (!"/".equals(Nvl.nvl(path, "/")) && !ProjectService.pathExists(String.valueOf(projectId), path)) {
                return new JsonResponse(404, "Project folder not found.").toJson();
            }
            boolean autoRename = Boolean.parseBoolean(req.getParameter("autorename"));
            if (!StringUtils.hasText(folderName) || !StringUtils.hasText(path)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (folderName.length() > RepoConstants.MAX_FOLDERNAME_LENGTH) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Invalid folderName (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, folderName);
                }
                return new JsonResponse(4003, "Folder name length limit of " + RepoConstants.MAX_FOLDERNAME_LENGTH + " characters exceeded").toJson();
            }
            RMSUserPrincipal principal;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                Project project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                }
                principal = new RMSUserPrincipal(us, project.getParentTenant());
                repository = ProjectService.getProjectRepository(session, principal, projectId);
                util.updateProjectActionTime(userId, projectId);
            }
            CreateFolderResult result = repository.createFolder(folderName, path, path, autoRename);
            Audit.audit(request, "API", "ProjectMgmt", "createFolder", 1, userId, folderName);
            JsonResponse resp = new JsonResponse("OK");
            JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
            Date lastModifiedTime = result.getLastModified();
            if (lastModifiedTime != null) {
                entry.setLastModified(lastModifiedTime.getTime());
            }
            entry.setFolder(true);
            entry.setName(folderName);
            entry.setPathDisplay(result.getPathDisplay());
            entry.setPathId(result.getPathId());
            entry.setSize(0L);
            resp.putResult("entry", entry);
            error = false;
            return resp.toJson();
        } catch (InvalidFileNameException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid folderName (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, folderName, e);
            }
            return new JsonResponse(4001, "Invalid folder name").toJson();
        } catch (FileAlreadyExistsException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Folder already exists (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, folderName, e);
            }
            return new JsonResponse(4002, "Folder already exists.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (project ID: {}, user ID: {}) in Project: '{}'", projectId, userId, path, e);
            }
            return new JsonResponse(404, "Parent folder missing.").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            Audit.audit(request, "API", "ProjectMgmt", "createFolder", !error ? 1 : 0, userId, path);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteItem(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @PathParam("projectId") int projectId,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {
        String path = null;
        boolean error = true;
        boolean isFolder = false;
        DefaultRepositoryTemplate repository = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            path = req.getParameter("pathId");
            if (!StringUtils.hasText(path)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (path.endsWith("/")) {
                isFolder = true;
            }
            String duid = ProjectService.getProjectFileDUID(projectId, path);
            String fileName = StringUtils.substringAfterLast(path, "/");
            RMSUserPrincipal principal;
            Project project;
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, StandardCharsets.UTF_8.name()) : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return new JsonResponse(400, "Invalid project.").toJson();
                } else if (!StringUtils.equalsIgnoreCase(user.getEmail(), project.getOwner())) {
                    if (!isFolder) {
                        ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(project.getOwner()), userId, Operations.DELETE, deviceId, platformId, String.valueOf(projectId), path, fileName, path, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PROJECT);
                        session.beginTransaction();
                        session.save(log);
                        session.commit();
                    }
                    return new JsonResponse(401, "Unauthorised").toJson();
                }
                principal = new RMSUserPrincipal(us, project.getParentTenant());
                repository = ProjectService.getProjectRepository(session, principal, projectId);
                util.updateProjectActionTime(userId, projectId);
            }
            String pathDisplay = path;
            if (isFolder) {
                pathDisplay = path.substring(0, path.length() - 1);
            }
            DeleteFileMetaData deleteFileMetaData = repository.deleteFile(path, pathDisplay);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("name", deleteFileMetaData.getFileName());
            resp.putResult("pathId", deleteFileMetaData.getFilePath());
            if (!isFolder) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(project.getOwner()), userId, Operations.DELETE, deviceId, platformId, String.valueOf(projectId), path, fileName, path, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
                RemoteLoggingMgmt.saveActivityLog(activity);
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (FileNotFoundException e) {
            if (isFolder) {
                return new JsonResponse(404, "Project folder not found").toJson();
            } else {
                return new JsonResponse(404, "Project file not found").toJson();
            }
        } catch (RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            Audit.audit(request, "API", "ProjectMgmt", "delete", !error ? 1 : 0, userId, path);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/decrypt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response decrypt(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @PathParam("projectId") int projectId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        File outputPath = null;
        RMSUserPrincipal principal;
        Project project;
        Tenant parentTenant;
        Rights[] rightsList;
        String filePath = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            filePath = req.getParameter("pathId");
            if (!StringUtils.hasText(filePath) || !StringUtils.startsWith(filePath, "/")) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing required parameters.").toJson()).build();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, StandardCharsets.UTF_8.name()) : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                project = ProjectService.getProjectWithParentTenant(session, us, projectId);
                if (project == null) {
                    return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Invalid Project.").toJson()).build();
                }
                principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
                principal.setIpAddress(request.getRemoteAddr());
                repository = ProjectService.getProjectRepository(session, principal, projectId);
                parentTenant = project.getParentTenant();
                util.updateProjectActionTime(userId, projectId);
            } catch (UnauthorizedRepositoryException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(e.getMessage(), e);
                }
                return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
            }
            RepositoryContent fileMetadata = repository.getFileMetadata(filePath);
            if (fileMetadata == null) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Missing file.").toJson()).build();
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            String duid = ProjectService.getProjectFileDUID(projectId, filePath);
            if (!StringUtils.hasText(duid)) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Missing file.").toJson()).build();
            }
            String path = HTTPUtil.getInternalURI(request);

            outputPath = RepositoryFileUtil.getTempOutputFolder();
            File output = repository.getFile(filePath, filePath, outputPath.getPath());
            String fileName = fileMetadata.getName();
            Map<String, String[]> tags;
            String originalMembership;
            Membership ownerMembership;

            try (InputStream is = new FileInputStream(output);
                    NxlFile nxl = NxlFile.parse(is)) {
                originalMembership = nxl.getOwner();
                try (DbSession session = DbSession.newSession()) {
                    ownerMembership = session.get(Membership.class, originalMembership);
                    if (ownerMembership == null) {
                        if (!UserMgmt.validateDynamicMembership(originalMembership)) {
                            return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                        }
                        ownerMembership = new Membership();
                        ownerMembership.setName(originalMembership);
                        User owner = new User();
                        owner.setId(Integer.parseInt(originalMembership.substring("user".length(), originalMembership.indexOf('@'))));
                        ownerMembership.setUser(owner);
                    }
                }

                boolean isExpired = false;
                boolean isNotYetValid = false;

                tags = DecryptUtil.getTags(nxl, null);
                EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxl);
                rightsList = evalResponse.getRights();
                if (rightsList.length == 0) {
                    User user = us.getUser();
                    com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, parentTenant.getName(), userEval, tags);
                    evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rightsList = PolicyEvalUtil.getUnionRights(responses);
                } else {
                    // Adhoc-file need check expiration.
                    FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
                    isExpired = AdhocEvalAdapter.isFileExpired(policy);
                    isNotYetValid = AdhocEvalAdapter.isNotYetValid(policy);
                }

                if (!ArrayUtils.contains(rightsList, Rights.DECRYPT) || isExpired || isNotYetValid) {
                    Operations ops = Operations.DECRYPT;
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                }
            }

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            File decryptedFile = null;
            try (InputStream is = new FileInputStream(output);
                    NxlFile nxl = NxlFile.parse(is)) {
                try {
                    byte[] token = DecryptUtil.requestToken(path, principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), duid, nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
                    if (!nxl.isValid(token)) {
                        throw new NxlException("Invalid token.");
                    }
                    FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                    String originalFileName = null;
                    if (fileInfo != null) {
                        originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : util.getFileNameWithoutNXL(output.getName());
                    }
                    if (originalFileName != null) {
                        decryptedFile = new File(output.getParent(), originalFileName);
                        try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                            DecryptUtil.decrypt(nxl, token, fos);
                        }
                    }

                } catch (JsonException e) {
                    if (e.getStatusCode() != 403) {
                        throw new NxlException("Invalid token", e);
                    } else {
                        return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                    }
                }
            }
            if (decryptedFile != null) {
                fileName = decryptedFile.getName();
            }
            output = decryptedFile;
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileName));
            if (output != null && output.length() > 0) {
                response.setHeader("x-rms-file-size", Long.toString(output.length()));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                try (InputStream fis = new FileInputStream(output)) {
                    IOUtils.copy(fis, response.getOutputStream());
                }
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(ownerMembership.getName()), userId, Operations.DECRYPT, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            RemoteLoggingMgmt.saveActivityLog(activity);
            Audit.audit(request, "API", "ProjectMgmt", "decrypt", 1, userId, path);
            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (FileNotFoundException e) {
            return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Not Found.").toJson()).build();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Malformed request.").toJson()).build();
        } catch (RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "Internal Server Error.").toJson()).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "Internal Server Error.").toJson()).build();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
            Audit.audit(request, "API", "ProjectMgmt", "decrypt", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @PathParam("projectId") int projectId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        File outputPath = null;
        RMSUserPrincipal principal;
        Project project;
        Tenant parentTenant;
        Rights[] rightsList = null;
        String filePath = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            filePath = req.getParameter("pathId");
            if (!StringUtils.hasText(filePath) || !StringUtils.startsWith(filePath, "/")) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing required parameters.").toJson()).build();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, StandardCharsets.UTF_8.name()) : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            String fileName;
            Boolean forViewer = req.getParameter("forViewer", Boolean.class);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            boolean partialDownload = start >= 0 && length >= 0;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                project = ProjectService.getProject(session, us, projectId);
                if (project == null) {
                    return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Invalid Project.").toJson()).build();
                }
                principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
                repository = ProjectService.getProjectRepository(session, principal, projectId);
                parentTenant = session.get(Tenant.class, project.getParentTenant().getId());
                util.updateProjectActionTime(userId, projectId);
            } catch (UnauthorizedRepositoryException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(e.getMessage(), e);
                }
                return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
            }
            RepositoryContent fileMetadata = repository.getFileMetadata(filePath);
            if (fileMetadata == null) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Missing file.").toJson()).build();
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            boolean isForViewer = forViewer != null && forViewer;
            Rights rights = isForViewer ? Rights.VIEW : Rights.DOWNLOAD;
            String duid = ProjectService.getProjectFileDUID(projectId, filePath);
            if (!StringUtils.hasText(duid)) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Missing file.").toJson()).build();
            }
            String path = HTTPUtil.getInternalURI(request);

            outputPath = RepositoryFileUtil.getTempOutputFolder();
            File output = repository.getFile(filePath, filePath, outputPath.getPath());
            fileName = fileMetadata.getName();
            Map<String, String[]> tags = null;
            String watermark = null;
            JsonExpiry validity = null;
            String originalMembership;
            Membership ownerMembership = null;
            boolean isAdhocPolicy = false;

            if (!isForViewer) {
                try (InputStream is = new FileInputStream(output);
                        NxlFile nxl = NxlFile.parse(is)) {
                    originalMembership = nxl.getOwner();
                    try (DbSession session = DbSession.newSession()) {
                        ownerMembership = session.get(Membership.class, originalMembership);
                        if (ownerMembership == null) {
                            if (!UserMgmt.validateDynamicMembership(originalMembership)) {
                                return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                            }
                            ownerMembership = new Membership();
                            ownerMembership.setName(originalMembership);
                            User owner = new User();
                            owner.setId(Integer.parseInt(originalMembership.substring("user".length(), originalMembership.indexOf('@'))));
                            ownerMembership.setUser(owner);
                        }
                    }
                    tags = DecryptUtil.getTags(nxl, null);
                    EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxl);
                    rightsList = evalResponse.getRights();
                    if (rightsList.length == 0) {
                        User user = us.getUser();
                        com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                        List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, parentTenant.getName(), userEval, tags);
                        evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                        rightsList = PolicyEvalUtil.getUnionRights(responses);
                    } else {
                        isAdhocPolicy = true;
                    }

                    if (!ArrayUtils.contains(rightsList, rights)) {
                        Operations ops = Operations.DOWNLOAD;
                        if (platformId != DeviceType.WEB.getLow()) {
                            ops = Operations.OFFLINE;
                        }
                        RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PROJECT);
                        RemoteLoggingMgmt.saveActivityLog(activity);
                        return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                    }
                    watermark = evalResponse.getEffectiveWatermark();
                    if (ArrayUtils.contains(rightsList, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                        watermark = WatermarkConfigManager.getWaterMarkText(path, AbstractLogin.getDefaultTenant().getName(), principal.getTicket(), platformId, String.valueOf(principal.getUserId()), principal.getClientId());
                    }
                    FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);

                    if (policy != null) {
                        validity = util.getValidity(policy);
                    }

                }

            }

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            if (partialDownload) {
                byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
                if (data != null) {
                    long contentLength = data.length;
                    response.setHeader("x-rms-file-size", Long.toString(contentLength));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                    response.getOutputStream().write(data);
                }
            } else {
                if (!isForViewer) {
                    try {
                        File outputFile = util.reEncryptDocument(userId, ticket, clientId, duid, principal, rightsList, ownerMembership.getName(), watermark, validity, tags, path, output, platformId, project, isAdhocPolicy, Operations.DOWNLOAD, null);
                        fileName = outputFile.getName();
                        output = outputFile;
                    } catch (UnauthorizedOperationException s) {
                        return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                    }
                }
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileName));
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            }
            Operations ops = null;
            String cmd;
            if (!isForViewer) {
                ops = Operations.DOWNLOAD;
                cmd = "download";
            } else if (platformId != DeviceType.WEB.getLow()) {
                ops = Operations.OFFLINE;
                cmd = "downloadForOffline";
            } else {
                cmd = "downloadForViewer";
            }
            if (ops != null && !partialDownload) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(ownerMembership != null ? ownerMembership.getName() : null), userId, ops, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
                RemoteLoggingMgmt.saveActivityLog(activity);
            }
            Audit.audit(request, "API", "ProjectMgmt", cmd, 1, userId, path);
            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (FileNotFoundException e) {
            return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Not Found.").toJson()).build();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Malformed request.").toJson()).build();
        } catch (RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "Internal Server Error.").toJson()).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "Internal Server Error.").toJson()).build();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
            Audit.audit(request, "API", "ProjectMgmt", "download", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/v2/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadV2(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @PathParam("projectId") int projectId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        RMSUserPrincipal principal;
        String filePath = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        Operations ops;
        File outputPath = null;
        File output;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            filePath = req.getParameter("pathId");
            Validator.validateFilePath(filePath);
            Project project = Validator.validateProject(projectId, us);
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            repository = ProjectService.getRepository(principal, projectId);
            outputPath = RepositoryFileUtil.getTempOutputFolder();

            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            boolean partialDownload = start >= 0 && length >= 0;

            DownloadType downloadType = Validator.validateDownloadType(req.getIntParameter("type", -1), SPACETYPE.PROJECTSPACE);
            boolean isPartialDownloadAllowed = Validator.isPartialDownloadAllowed(downloadType);
            ops = util.getDownloadOps(downloadType, partialDownload);
            String duid = ProjectService.validateFileDuid(projectId, filePath);

            String path = HTTPUtil.getInternalURI(request);
            RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);

            String fileName = fileMetadata.getName();
            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            Rights[] rights = util.getUserFileRights(principal, String.valueOf(projectId), us, filePath, outputPath, downloadType, partialDownload);
            if (rights.length > 0) {
                if (partialDownload && isPartialDownloadAllowed) {
                    byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
                    if (data != null) {
                        long contentLength = data.length;
                        response.setHeader("x-rms-file-size", Long.toString(contentLength));
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                        response.getOutputStream().write(data);
                    }
                }
                if (partialDownload && !isPartialDownloadAllowed) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(util.getFileOwner(duid)), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "This is not a supported operation.").toJson()).build();
                } else {
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileName));
                    if (downloadType == DownloadType.NORMAL) {
                        output = util.downloadCopy(principal, project, us, filePath, path, outputPath, ops, downloadType);
                    } else if (downloadType == DownloadType.FOR_SYSTEMBUCKET) {
                        output = util.downloadCopy(principal, project, us, filePath, path, outputPath, ops, downloadType);
                    } else {
                        output = util.downloadOriginal(principal, project, filePath, outputPath);
                    }
                    if (output != null && output.length() > 0) {
                        response.setHeader("x-rms-file-size", Long.toString(output.length()));
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                        try (InputStream fis = new FileInputStream(output)) {
                            IOUtils.copy(fis, response.getOutputStream());
                        }
                    }
                }
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(util.getFileOwner(duid)), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            util.saveDownloadActivityLog(ops, activity);
            Audit.audit(request, "API", "ProjectMgmt", downloadType.getDisplayName(), 1, userId, path);
            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            FileUtils.deleteQuietly(outputPath);
            Audit.audit(request, "API", "ProjectMgmt", "download", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @PUT
    @Path("/{projectId}/file/classification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reclassify(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, @PathParam("projectId") int projectId, String json) {
        String fileName;
        String fileParentPathId;
        String duid = null;
        Project project;
        boolean error = true;
        boolean holdingLock = false;
        String owner;
        try (DbSession session = DbSession.newSession()) {
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, StandardCharsets.UTF_8.name()) : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            fileName = req.getParameter("fileName");
            fileParentPathId = req.getParameter("parentPathId").toLowerCase();
            if (!StringUtils.hasText(fileName) && !StringUtils.hasText(fileParentPathId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            String filePath = fileParentPathId + fileName;
            filePath = filePath.toLowerCase();

            String clientFileTags = Nvl.nvl(req.getParameter("fileTags"), "");

            project = ProjectService.getProject(session, us, projectId);
            if (project == null) {
                return new JsonResponse(400, "Invalid project.").toJson();
            }

            RMSUserPrincipal principal = new RMSUserPrincipal(us, project.getParentTenant());
            com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(userId)).ticket(ticket).clientId(clientId).platformId(platformId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
            JsonProjectFileInfo fileInfo = ProjectService.getProjectFileInfo(session, principal, userEval, projectId, filePath);
            if (fileInfo == null) {
                return new JsonResponse(4005, "Invalid file name.").toJson();
            }

            if (fileInfo.getProtectionType() != ProtectionType.CENTRAL.ordinal()) {
                return new JsonResponse(403, "Reclassification is only for company rights protected files.").toJson();
            }

            holdingLock = true;

            DefaultRepositoryTemplate repository = ProjectService.getProjectRepository(session, principal, projectId);
            File outputPath = RepositoryFileUtil.getTempOutputFolder();
            File temp = repository.getFile(filePath, fileName, outputPath.getPath());

            UploadedFileMetaData metadata;
            try (InputStream is1 = new FileInputStream(temp); NxlFile nxl1 = NxlFile.parse(is1)) {
                duid = nxl1.getDuid();
                owner = nxl1.getOwner();
                byte[] header = new byte[NxlFile.COMPLETE_HEADER_SIZE];
                IOUtils.read(new FileInputStream(temp), header);
                String encodedHeader = Base64Codec.encodeAsString(header);
                String updateNxlMetadatapath = HTTPUtil.getInternalURI(request) + "/rs/token/" + duid;

                Properties prop = new Properties();
                prop.setProperty("userId", String.valueOf(userId));
                prop.setProperty("ticket", ticket);
                prop.setProperty("clientId", clientId);
                if (platformId == null) {
                    platformId = DeviceType.WEB.getLow();
                }
                prop.setProperty("platformId", String.valueOf(platformId));

                JsonRequest updateNxlMetadataReq = new JsonRequest();
                updateNxlMetadataReq.addParameter("protectionType", nxl1.getProtectionType().ordinal());
                updateNxlMetadataReq.addParameter("fileTags", clientFileTags);
                String existingTags = DecryptUtil.getTagsString(nxl1, null);
                updateNxlMetadataReq.addParameter("existingFileTags", existingTags);
                updateNxlMetadataReq.addParameter("fileHeader", encodedHeader);
                updateNxlMetadataReq.addParameter("ml", "0");
                String ret = RestClient.put(updateNxlMetadatapath, prop, updateNxlMetadataReq.toJson());

                JsonResponse resp = JsonResponse.fromJson(ret);
                if (resp.hasError()) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, Operations.CLASSIFY, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PROJECT);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    return new JsonResponse(resp.getStatusCode(), resp.getMessage()).toJson();
                }

                String fileinfoCheckSumStr = resp.getResultAsString("fileinfoCheckSum");
                String dateModified = String.valueOf(resp.getResultAsLong("dateModified", new Date().getTime()));
                String tagChecksumStr = resp.getResultAsString("tagCheckSum");
                String headerChecksumStr = resp.getResultAsString("headerCheckSum");

                if (!StringUtils.hasText(fileinfoCheckSumStr) || !StringUtils.hasText(dateModified) || !StringUtils.hasText(tagChecksumStr) || !StringUtils.hasText(headerChecksumStr)) {
                    return new JsonResponse(400, "Missing checksum.").toJson();
                }

                byte[] fileinfoChecksum = Base64Codec.decode(fileinfoCheckSumStr);
                byte[] tagChecksum = Base64Codec.decode(tagChecksumStr);
                byte[] headerChecksum = Base64Codec.decode(headerChecksumStr);

                String membership = ProjectService.getMembership(principal, project);
                FileInfo updatedFileInfo = GsonUtils.GSON.fromJson(new String(nxl1.getSection(".FileInfo").getData(), StandardCharsets.UTF_8.name()), FILEINFO_TYPE);
                updatedFileInfo.setLastModified(Long.valueOf(dateModified));
                updatedFileInfo.setModifiedBy(membership);

                /*
                 * Changes to different parts of the nxl file header cannot
                 * be done consecutively, to ensure checksums are calculated
                 * correctly so need to update and re-parse before updating again.
                 */
                try (ByteArrayOutputStream baos1 = new ByteArrayOutputStream()) {
                    EncryptUtil.updateTags(nxl1, clientFileTags, tagChecksum, headerChecksum, baos1);
                    try (InputStream is2 = new ByteArrayInputStream(baos1.toByteArray());
                            NxlFile nxl2 = NxlFile.parse(is2)) {
                        try (RandomAccessFile raf = new RandomAccessFile(temp.getPath(), "rw")) {
                            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                            EncryptUtil.updateFileInfo(nxl2, GsonUtils.GSON.toJson(updatedFileInfo), fileinfoChecksum, headerChecksum, baos2);
                            raf.write(baos2.toByteArray());
                        }
                    }
                }
                Map<String, String> customMetadata = new HashMap<>();
                customMetadata.put("duid", duid);
                customMetadata.put("createdBy", util.getUserId(nxl1.getOwner()));
                customMetadata.put("lastModifiedBy", util.getUserId(membership));
                customMetadata.put("creationTime", Long.toString(fileInfo.getCreationTime()));
                customMetadata.put("lastModified", dateModified);
                customMetadata.put("rights", "0");

                metadata = repository.uploadFile(fileParentPathId, fileName, temp.getPath(), true, "", customMetadata);

            }
            JsonResponse resp = new JsonResponse("OK");

            JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
            Date lastModifiedTime = metadata.getLastModifiedTime();
            if (lastModifiedTime != null) {
                entry.setLastModified(lastModifiedTime.getTime());
            }
            entry.setFolder(false);
            entry.setName(temp.getName());
            entry.setPathDisplay(metadata.getPathDisplay());
            entry.setPathId(metadata.getPathId());
            entry.setSize(temp.length());
            resp.putResult("entry", entry);
            error = false;
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, Operations.CLASSIFY, deviceId, platformId, String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            RemoteLoggingMgmt.saveActivityLog(activity);
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (holdingLock && duid != null) {
                LockManager.getInstance().releaseRemoveLock(duid);
            }
            Audit.audit(request, "API", "ProjectMgmt", "reclassify", error ? 0 : 1, userId, duid);
        }
    }

    @Secured
    @GET
    @Path("/allProjects")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllProjects(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {

        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                String tenantId = us.getLoginTenant();
                List<JsonProject> jsonProjects = ProjectService.getAllProjects(session, us, tenantId);
                JsonResponse response = new JsonResponse("OK");
                response.putResult("detail", jsonProjects);
                error = false;
                return response.toJson();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ProjectMgmt", "getAllProjects", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @POST
    @Path("/{projectId}/fileHeader")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadHeader(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @PathParam("projectId") int projectId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        RMSUserPrincipal principal;
        String filePath = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        Operations ops;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            filePath = req.getParameter("pathId");
            Validator.validateFilePath(filePath);
            Project project = Validator.validateProject(projectId, us);
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            repository = ProjectService.getRepository(principal, projectId);
            DownloadType downloadType = DownloadType.HEADER;
            ops = Operations.HEADER_DOWNLOAD;

            String duid = ProjectService.validateFileDuid(projectId, filePath);
            int start = 0;
            long length = 16384;
            String path = HTTPUtil.getInternalURI(request);
            RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
            String fileName = fileMetadata.getName();
            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));

            byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
            if (data != null) {
                long contentLength = data.length;
                response.setHeader("x-rms-file-size", Long.toString(contentLength));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                response.getOutputStream().write(data);
            }
            String userMembership = ProjectService.getMembership(principal, project);
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(userMembership), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(projectId), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            util.saveDownloadActivityLog(ops, activity);
            Audit.audit(request, "API", "ProjectMgmt", downloadType.getDisplayName(), 1, userId, path);
            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            Audit.audit(request, "API", "ProjectMgmt", "download header", !error ? 1 : 0, userId, filePath);
        }
    }
}
