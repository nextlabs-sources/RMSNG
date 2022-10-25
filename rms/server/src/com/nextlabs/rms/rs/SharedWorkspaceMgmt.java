package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharedWorkspaceFileInfo;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.application.ApplicationRepositoryContent;
import com.nextlabs.rms.application.FileUploadMetadata;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.rs.dto.ProtectSharedWorkspaceDTO;
import com.nextlabs.rms.rs.dto.UploadSharedWorkspaceDTO;
import com.nextlabs.rms.rs.util.SharedWorkspaceUploadUtil;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.SharedWorkspaceService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.RepositoryFileUtil;
import com.nextlabs.rms.util.RestUploadUtil;
import com.nextlabs.rms.util.SharedWorkspaceDownloadUtil;
import com.nextlabs.rms.util.SharedWorkspaceUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * REST Controller class that handles all REST API requests to /sharedws
 * endpoints
 * 
 */
@Path("/sharedws")
public class SharedWorkspaceMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private SharedWorkspaceService sharedWorkspaceService = new SharedWorkspaceService();
    private SharedWorkspaceUtil sharedWorkspaceUtil = new SharedWorkspaceUtil();
    private SharedWorkspaceDownloadUtil sharedWorkspaceDownloadUtil = new SharedWorkspaceDownloadUtil();
    private static final String API = "API";

    /**
     * Lists all files and folders for a given path in an application repository
     * with id repoId. Or send the result of searching for searchString in the
     * repository.
     * 
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @param path
     * @param deviceId
     * @param searchString
     * @param hideFiles
     * @param repoId
     * @return
     */

    @Secured
    @GET
    @Path("/v1/{repoId}/files")
    @Produces(MediaType.APPLICATION_JSON)
    public String listFiles(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("platformId") Integer platformId,
        @QueryParam("path") String path, @QueryParam("searchString") String searchString,
        @PathParam("repoId") String repoId, @QueryParam("hideFiles") String hideFiles) {
        boolean error = true;
        String loginTenantId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            sharedWorkspaceUtil.validateInputsForGetFiles(path, repoId);
            List<ApplicationRepositoryContent> fileList = sharedWorkspaceService.listFiles(path, searchString, repoId, request, hideFiles);
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", fileList);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "listFiles", !error ? 1 : 0, userId, loginTenantId, path);
        }
    }

    /**
     * Downloads the file at a given path in an application repository with id
     * repoId. As of now it can only process type 1 download
     * 
     * @param request
     * @param response
     * @param userId
     * @param ticket
     * @param clientId
     * @param repoId
     * @param platformId
     * @param deviceId
     * @param json
     * @return
     */
    @Secured
    @POST
    @Path("/v1/{repoId}/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @PathParam("repoId") String repoId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {

        String path = null;
        String filePath = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            String loginTenantId = us.getLoginTenant();
            Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            sharedWorkspaceUtil.validateInputsForDownloadFile(req, repoId);
            filePath = req.getParameter("path");
            DownloadType downloadType = Validator.validateDownloadType(req.getIntParameter("type", 1), SPACETYPE.SHAREDWORKSPACE);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            path = HTTPUtil.getInternalURI(request);
            Validator.validateFilePath(filePath);
            sharedWorkspaceDownloadUtil.downloadFile(repoId, principal, path, filePath, start, length, downloadType, response, us);

            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "download", !error ? 1 : 0, userId, path);
        }
    }

    /**
     * Protects a file in place , for a file at a given path in an application
     * repository with id repoI returns some metadata of the protected file
     * 
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param repoId
     * @param platformId
     * @param deviceId
     * @param json
     * @return
     */
    @Secured
    @POST
    @Path("/v1/{repoId}/protect")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String protectInPlace(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @PathParam("repoId") String repoId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {

        boolean error = true;
        try {
            JsonRequest jsonRequest = JsonRequest.fromJson(json);
            SharedWorkspaceUtil.validateInputsForProtectFiles(jsonRequest, repoId);

            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, EnterpriseWorkspaceService.getTenant(us.getLoginTenant()));
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);

            ProtectSharedWorkspaceDTO protectSharedWorkspaceDTO = new ProtectSharedWorkspaceDTO(repoId, jsonRequest, RepositoryFileUtil.getTempOutputFolder().getPath());

            if (ArrayUtils.contains(protectSharedWorkspaceDTO.getRights(), Rights.WATERMARK) && !StringUtils.hasText(protectSharedWorkspaceDTO.getWatermark())) {
                protectSharedWorkspaceDTO.setWatermark(WatermarkConfigManager.getWaterMarkText(HTTPUtil.getInternalURI(request), AbstractLogin.getDefaultTenant().getName(), ticket, platformId, String.valueOf(userId), clientId));
            }
            FileUploadMetadata fileUploadMetadata = sharedWorkspaceService.protectInPlace(us, protectSharedWorkspaceDTO, principal, HTTPUtil.getInternalURI(request), null);

            JsonResponse response = new JsonResponse("OK");
            response.putResult("entry", new ProtectedFileDetails(fileUploadMetadata));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "protectInPlace", !error ? 1 : 0, userId);
        }
    }

    /**
     * Gets the file metadata information for an nxl file at a given path in an
     * application repository with id repoId.
     * 
     * @param request
     * @param userId
     * @param repoId
     * @param ticket
     * @param clientId
     * @param deviceId
     * @param platformId
     * @param json
     * @return
     */
    @Secured
    @POST
    @Path("/v1/{repoId}/file/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getFileInfo(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @PathParam("repoId") String repoId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String filePath = null;
        try {
            JsonRequest jsonRequest = JsonRequest.fromJson(json);
            SharedWorkspaceUtil.validateInputsForGetFileInfoCheckFileExists(jsonRequest, repoId);
            filePath = jsonRequest.getParameter("path");
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, EnterpriseWorkspaceService.getTenant(us.getLoginTenant()));
            JsonSharedWorkspaceFileInfo fileInfo = sharedWorkspaceService.getFileInfo(us, principal, filePath, request, repoId, null);

            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("fileInfo", fileInfo);
            error = false;
            return resp.toJson();

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "getFileInfo", !error ? 1 : 0, userId);
        }
    }

    /**
     * Method to check if file exists or not , based on the repoId and path
     * parameter sent
     * 
     * @param request
     * @param userId
     * @param repoId
     * @param ticket
     * @param clientId
     * @param deviceId
     * @param platformId
     * @param json
     * @return
     */
    @Secured
    @POST
    @Path("/v1/{repoId}/file/checkIfExists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String checkIfFileExists(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @PathParam("repoId") String repoId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String filePath = null;
        try {
            JsonRequest jsonRequest = JsonRequest.fromJson(json);
            SharedWorkspaceUtil.validateInputsForGetFileInfoCheckFileExists(jsonRequest, repoId);
            filePath = jsonRequest.getParameter("path");
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, EnterpriseWorkspaceService.getTenant(us.getLoginTenant()));
            boolean fileExists = sharedWorkspaceService.checkIfFileExists(principal, repoId, filePath);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("fileExists", fileExists);
            error = false;
            return resp.toJson();

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "checkIfFileExists", !error ? 1 : 0, userId);
        }
    }

    @Secured
    @POST
    @Path("/v1/{repoId}/fileHeader")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadNxlHeader(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @PathParam("repoId") String repoId, @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, String json) {
        // Sanity check first.
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        if (us == null) {
            return Response.status(Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(401, "Unauthorised.").toJson()).build();
        }
        if (platformId == null) {
            platformId = DeviceType.WEB.getLow();
        }
        String loginTenantId = us.getLoginTenant();
        RMSUserPrincipal userPrincipal = new RMSUserPrincipal(us, EnterpriseWorkspaceService.getTenant(loginTenantId));

        String filePath = null;
        boolean error = true;
        try {
            Validator.setDeviceInUserPrincipal(deviceId, platformId, userPrincipal, request);
            // 1. Validation for request json.
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            filePath = req.getParameter("path");
            // 2. Validation for file pathId.
            Validator.validateFilePath(filePath);

            ApplicationRepositoryContent metadata = sharedWorkspaceService.getFileMetadata(request, repoId, filePath);
            if (metadata == null) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "File not found.").toJson()).build();
            }
            if (metadata.isFolder()) {
                return Response.status(4001).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(4001, "Invalid path parameter.").toJson()).build();
            }
            String fileName = metadata.getName();
            DownloadType downloadType = DownloadType.HEADER;
            Operations ops = Operations.HEADER_DOWNLOAD;
            long start = 0;
            long end = start + 0x4000 - 1;
            String duid = "";
            String fileOwner = "";
            String path = HTTPUtil.getInternalURI(request);
            response.setHeader("x-rms-last-modified", String.valueOf(metadata.getLastModifiedTime()));

            byte[] data = sharedWorkspaceService.downloadPartialFile(repoId, userPrincipal, filePath, filePath, start, end);
            if (data != null) {
                boolean nxl = NxlFile.isNxl(data);
                if (!nxl) {
                    return Response.status(5001).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(5001, "Invalid file.").toJson()).build();
                }
                NxlFile nxlFile = NxlFile.parse(data);
                duid = nxlFile.getDuid();
                fileOwner = nxlFile.getOwner();

                long contentLength = data.length;
                response.setHeader("x-rms-file-size", Long.toString(contentLength));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                response.getOutputStream().write(data);
            }

            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, fileOwner, userId, ops, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, Constants.AccessResult.ALLOW, new Date(), null, Constants.AccountType.ENTERPRISEWS);
            RemoteLoggingMgmt.saveActivityLog(activity);
            Audit.audit(request, "API", getClass().getSimpleName(), downloadType.getDisplayName(), 1, userId, path);

            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            Audit.audit(request, "API", getClass().getSimpleName(), "download Nxl Header", !error ? 1 : 0, userId, filePath);
        }
    }

    /**
     * Uploads nxl file to shared workspace repositories , based on repo id and file
     * path given.
     * 
     * @param request
     * @param userId
     * @param ticket
     * @param platformId
     * @param repoId
     * @param deviceId
     * @param clientId
     * @return
     */
    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/v1/{repoId}/file")
    public String upload(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("platformId") Integer platformId,
        @PathParam("repoId") String repoId, @HeaderParam("deviceId") String deviceId,
        @HeaderParam("clientId") String clientId) {

        boolean error = true;
        RestUploadRequest uploadReq = null;
        UploadSharedWorkspaceDTO swsDTO = null;

        try {
            platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, EnterpriseWorkspaceService.getTenant(us.getLoginTenant()));
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            SharedWorkspaceUtil.validateInputsForUploadFile(repoId, uploadReq);
            swsDTO = new UploadSharedWorkspaceDTO(repoId, uploadReq, RepositoryFileUtil.getTempOutputFolder().getPath(), principal);

            FileUploadMetadata metadata = sharedWorkspaceService.upload(swsDTO, principal, uploadReq, us, request);

            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("entry", SharedWorkspaceUploadUtil.getJsonRepositoryFileEntry(metadata, swsDTO.getFile()));
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, SharedWorkspaceMgmt.class.getSimpleName(), "upload", !error ? 1 : 0, userId);

            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "upload", !error ? 1 : 0, userId);
        }
    }
}
