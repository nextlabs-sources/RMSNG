package com.nextlabs.rms.rs;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonEnterpriseSpaceFileInfo;
import com.nextlabs.common.shared.JsonEnterpriseSpaceFileList;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonRepositoryFileEntry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
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
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.EnterpriseSpaceStorageExceededException;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ActivityLog;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.Membership;
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
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryEnterprise;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.InvalidFileOverwriteException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
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
import com.nextlabs.rms.util.EnterpriseWorkspaceDownloadUtil;
import com.nextlabs.rms.util.PolicyEvalUtil;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
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
import org.hibernate.criterion.Order;

@Path("/enterprisews")
public class EnterpriseWorkspaceMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final Type FILEINFO_TYPE = new TypeToken<FileInfo>() {
    }.getType();
    EnterpriseWorkspaceDownloadUtil util = new EnterpriseWorkspaceDownloadUtil();

    @Secured
    @GET
    @Path("/files")
    @Produces(MediaType.APPLICATION_JSON)
    public String listFiles(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("pathId") String parentPath, @QueryParam("q") String searchFields,
        @QueryParam("searchString") String searchString) {
        boolean error = true;
        String loginTenantId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            if (!"/".equals(Nvl.nvl(parentPath, "/")) && EnterpriseWorkspaceService.isPathExist(loginTenantId, parentPath)) {
                return new JsonResponse(404, "EnterpriseWS folder not found.").toJson();
            }
            JsonEnterpriseSpaceFileList fileList;
            Map<String, Long> enterpriseSpaceStatus;
            Long folderLastUpdatedTime = null;
            try (DbSession session = DbSession.newSession()) {
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = util.getOrdersList(orderBy);
                List<String> searchFieldList = StringUtils.tokenize(searchFields, ",");
                String processedSearchString = StringUtils.hasText(searchString) ? searchString.replace("^\\.+", "").replaceAll("[\\\\/:*?\"\'<>|]", "") : "";
                fileList = EnterpriseWorkspaceService.getJsonFileList(session, loginTenantId, page, size, orders, parentPath, searchFieldList, processedSearchString);
                enterpriseSpaceStatus = EnterpriseWorkspaceService.getEnterpriseSpaceStatus(session, loginTenantId);
                // since system bucket membership is dynamic there is no Membership record whose last action time has to be updated here
                if (parentPath != null) {
                    folderLastUpdatedTime = EnterpriseWorkspaceService.getFolderLastUpdatedTime(session, loginTenantId, parentPath);
                }
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", fileList);
            if (parentPath != null) {
                response.putResult("folderLastUpdatedTime", folderLastUpdatedTime);
            }
            response.putResult("usage", enterpriseSpaceStatus.get(RepoConstants.STORAGE_USED));
            response.putResult("quota", enterpriseSpaceStatus.get(EnterpriseWorkspaceService.ENTERPRISE_SPACE_QUOTA));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "listFiles", !error ? 1 : 0, userId, loginTenantId, parentPath);
        }
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/uploadLargeFile/{routingKey}")
    public String uploadLargeFile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @PathParam("routingKey") String routingKey) {
        if (!StringUtils.hasText(routingKey)) {
            new JsonResponse(400, "Missing routing key").toJson();
        }
        String responseMessage = upload(request, userId, ticket, platformId, deviceId, clientId);
        RabbitMQUtil.sendDirectMessage(routingKey, RabbitMQUtil.ENTERPRISE_SPACE_UPLOAD_EXCHANGE_NAME, responseMessage);
        return responseMessage;
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/file")
    public String upload(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId) {
        DefaultRepositoryTemplate repository = null;
        RestUploadRequest uploadReq = null;
        String fileParentPathId = null;
        String fileName = null;
        String duid = null;
        boolean allowOverwrite = false;
        boolean error = true;
        String loginTenantId = null;
        boolean userConfirmedFileOverwrite = false;
        String uniqueResourceId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (deviceType == null) {
                return new JsonResponse(400, "Unknown platform.").toJson();
            }

            RMSUserPrincipal principal;
            Tenant loginTenant;
            try (DbSession session = DbSession.newSession()) {
                loginTenant = session.get(Tenant.class, us.getLoginTenant());
                loginTenantId = loginTenant.getId();
                principal = new RMSUserPrincipal(us, loginTenant);
                repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
                // since system bucket membership is dynamic there is no Membership record whose last action time has to be updated here
                EnterpriseWorkspaceService.checkEnterpriseSpaceStorageExceeded(session, loginTenantId);
            }

            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            JsonRequest req = JsonRequest.fromJson(uploadReq.getJson());
            if (req == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            fileName = req.getParameter("name");
            fileParentPathId = req.getParameter("parentPathId");
            if (!"/".equals(Nvl.nvl(fileParentPathId, "/")) && EnterpriseWorkspaceService.isPathExist(loginTenantId, fileParentPathId)) {
                return new JsonResponse(404, "EnterpriseWS folder not found.").toJson();
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
            String uploader = null;
            Rights[] rights = null;
            Long lastModified = null;
            Long creationTime = null;
            String createdBy = null;
            String lastModifiedBy = null;

            try (InputStream is = new FileInputStream(tmpFile);
                    NxlFile nxlMetadata = NxlFile.parse(is)) {
                if (nxlMetadata.getContentLength() == 0) {
                    return new JsonResponse(5005, "Empty files are not allowed to be uploaded.").toJson();
                }
                uploader = nxlMetadata.getOwner();
                duid = nxlMetadata.getDuid();
                isNxl = true;
                createdBy = util.getUserId(uploader);
                FileInfo fileInfo = DecryptUtil.getInfo(nxlMetadata, null);
                lastModified = fileInfo.getLastModified();
                creationTime = fileInfo.getCreationTime();
                Map<String, String[]> tagMap = DecryptUtil.getTags(nxlMetadata, null);
                lastModifiedBy = util.getUserId(fileInfo.getModifiedBy());
                String membershipWorkspace = EnterpriseWorkspaceService.getMembershipByTenantName(principal, loginTenant.getName());
                String uploadWorkspaceTokenGroupName = StringUtils.substringAfter(membershipWorkspace, "@");
                if (!util.validateMembership(nxlMetadata, uploadWorkspaceTokenGroupName)) {
                    return new JsonResponse(5003, "The nxl file does not belong to this workspace").toJson();
                }
                if (!TokenMgmt.checkNxlMetadata(duid, DecryptUtil.getFilePolicyStr(nxlMetadata, null), GsonUtils.GSON.toJson(tagMap), nxlMetadata.getProtectionType())) {
                    return new JsonResponse(5008, "The nxl file does not have valid metadata.").toJson();
                }

                try (DbSession session = DbSession.newSession()) {
                    EnterpriseSpaceItem item = EnterpriseWorkspaceService.getEnterpriseSpaceFileByDUID(session, duid);
                    if (item != null && item.getTenant() != null && item.getFilePath() != null && item.getTenant().getId().equals(loginTenant.getId()) && !item.getFilePath().equals(fileParentPathId + fileName.toLowerCase())) {
                        return new JsonResponse(5009, "The nxl file already exists in another location in this workspace.").toJson();
                    }
                }

                EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxlMetadata);
                rights = evalResponse.getRights();
                if (rights.length == 0) {
                    User user = us.getUser();
                    com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(uploadReq.getFileName(), uploader, loginTenant.getName(), userEval, tagMap);
                    evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rights = evalResponse.getRights();
                } else {
                    isAdhocPolicy = true;
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
                String[] rightsList = GsonUtils.GSON.fromJson(req.getParameter("rightsJSON"), GsonUtils.STRING_ARRAY_TYPE);
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
                String membership = EnterpriseWorkspaceService.getMembershipByTenantName(principal, loginTenant.getName());
                String expiry = req.getParameter("expiry", String.class);
                JsonExpiry expiryJson = StringUtils.hasText(expiry) ? GsonUtils.GSON.fromJson(expiry, JsonExpiry.class) : new JsonExpiry(0);
                if (!(rights == null || rights.length == 0) && !ExpiryUtil.validateExpiry(expiryJson)) {
                    return new JsonResponse(4003, "Invalid expiry.").toJson();
                }
                try (OutputStream os = new FileOutputStream(outputFile)) {
                    try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, HTTPUtil.getInternalURI(request), membership, rights, watermark, expiryJson, tags, tmpFile, os))) {
                        duid = nxl.getDuid();
                        uploader = nxl.getOwner();
                        createdBy = Integer.toString(userId);
                        FileInfo fileInfo = DecryptUtil.getInfo(nxl, null);
                        lastModified = fileInfo.getLastModified();
                        creationTime = fileInfo.getCreationTime();
                        lastModifiedBy = util.getUserId(fileInfo.getModifiedBy());
                    }
                }
                tmpFile = outputFile;
            } else {
                if (!StringUtils.endsWithIgnoreCase(fileName, com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
                    return new JsonResponse(5010, "NXL file must have .nxl file extension.").toJson();
                }
            }

            Map<String, String> customMetadata = new HashMap<>();
            customMetadata.put("duid", duid);
            customMetadata.put("rights", String.valueOf(Rights.toInt(rights)));
            customMetadata.put("createdBy", createdBy);
            if (lastModifiedBy != null) {
                customMetadata.put("lastModifiedBy", lastModifiedBy);
            }
            if (creationTime != null) {
                customMetadata.put("creationTime", creationTime.toString());
            }
            if (lastModified != null) {
                customMetadata.put("lastModified", lastModified.toString());
            }
            if (isNxl && req.getIntParameter("type", com.nextlabs.common.shared.Constants.UploadType.UPLOAD_NORMAL.ordinal()) == com.nextlabs.common.shared.Constants.UploadType.UPLOAD_OVERWRITE.ordinal()) {
                allowOverwrite = true;
                if ((!isAdhocPolicy || Integer.parseInt(createdBy) != userId) && !ArrayUtils.contains(rights, Rights.EDIT)) {
                    return new JsonResponse(403, "Access Denied").toJson();
                }
            }

            userConfirmedFileOverwrite = Boolean.parseBoolean(req.getParameter("userConfirmedFileOverwrite"));
            if (allowOverwrite && userConfirmedFileOverwrite) {
                return new JsonResponse(400, "Invalid request parameters").toJson();
            }
            uniqueResourceId = UploadUtil.getUniqueResourceId(SPACETYPE.ENTERPRISESPACE, loginTenant.getId(), fileParentPathId, tmpFile.getAbsolutePath());
            if ((allowOverwrite || userConfirmedFileOverwrite) && !LockManager.getInstance().acquireLock(uniqueResourceId, TimeUnit.MINUTES.toMillis(5))) {
                allowOverwrite = false;
                userConfirmedFileOverwrite = false;
                return new JsonResponse(4002, "Another User is editing this file.").toJson();
            }
            UploadedFileMetaData metadata = repository.uploadFile(fileParentPathId, null, tmpFile.getAbsolutePath(), allowOverwrite, "", customMetadata, userConfirmedFileOverwrite);

            if (!isNxl) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, uploader, userId, Operations.PROTECT, deviceId, platformId, repository.getRepoId(), metadata.getPathId(), tmpFile.getName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
                RemoteLoggingMgmt.saveActivityLog(activity);
            }
            Operations ops = allowOverwrite ? Operations.UPLOAD_EDIT : Operations.UPLOAD_NORMAL;
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, uploader, userId, ops, deviceId, platformId, repository.getRepoId(), metadata.getPathId(), tmpFile.getName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
            RemoteLoggingMgmt.saveActivityLog(activity);
            JsonResponse resp = new JsonResponse("OK");

            JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
            Date lastModifiedTime = metadata.getLastModifiedTime();
            if (lastModifiedTime != null) {
                entry.setLastModified(lastModifiedTime.getTime());
            }
            entry.setFolder(false);
            entry.setName(tmpFile.getName());
            entry.setPathDisplay(metadata.getPathDisplay());
            entry.setPathId(metadata.getPathId());
            entry.setSize(tmpFile.length());
            resp.putResult("entry", entry);
            // TODO: Define email notification semantics
            error = false;
            return resp.toJson();
        } catch (InvalidFileNameException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid fileName (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, fileName, e);
            }
            return new JsonResponse(4001, "Invalid file name").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", uploadReq.getJson(), e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, fileParentPathId, e);
            }
            return new JsonResponse(404, "Parent folder missing.").toJson();
        } catch (EnterpriseSpaceStorageExceededException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Drive Storage Exceeded: {}", e.getStorage());
            }
            return new JsonResponse(6001, "EnterpriseWS Storage Exceeded.").toJson();
        } catch (FileConflictException e) {
            return new JsonResponse(4001, "File already exists").toJson();
        } catch (SocketTimeoutException e) {
            return new JsonResponse(500, "Socket timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when uploading file to EnterpriseWS (user ID: {}, fileName: {}, tenant ID: {}): {}", userId, fileName, loginTenantId, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when uploading file to EnterpriseWS (user ID: {}, fileName: {}, tenant ID: {}): {}", userId, fileName, loginTenantId, e.getMessage(), e);
            }
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (InvalidFileOverwriteException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Invalid File upload error occurred when uploading file to EnterpriseWS (user ID: {}, fileName: {}, tenant ID: {}): {}", userId, fileName, loginTenantId, e.getMessage(), e);
            }
            return new JsonResponse(5007, "User can not overwrite a different File").toJson();

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if ((allowOverwrite || userConfirmedFileOverwrite) && uniqueResourceId != null) {
                LockManager.getInstance().releaseRemoveLock(uniqueResourceId);
            }
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "upload", !error ? 1 : 0, userId, loginTenantId, fileParentPathId, fileName);
        }
    }

    @Secured
    @POST
    @Path("/createFolder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFolder(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        String folderName = null;
        String path = null;
        DefaultRepositoryTemplate repository = null;
        String loginTenantId = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            path = req.getParameter("parentPathId");
            folderName = req.getParameter("name");
            if (!"/".equals(Nvl.nvl(path, "/")) && EnterpriseWorkspaceService.isPathExist(loginTenantId, path)) {
                return new JsonResponse(404, "EnterpriseWS folder not found.").toJson();
            }

            boolean autoRename = Boolean.parseBoolean(req.getParameter("autorename"));
            try (DbSession session = DbSession.newSession()) {
                if (!UserService.checkTenantAdmin(session, loginTenantId, us.getUser().getId())) {
                    return new JsonResponse(401, "Unauthorised").toJson();
                }
            }

            if (!StringUtils.hasText(folderName) || !StringUtils.hasText(path)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (folderName.length() > RepoConstants.MAX_FOLDERNAME_LENGTH) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Invalid folderName (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, folderName);
                }
                return new JsonResponse(4003, "Folder name length limit of " + RepoConstants.MAX_FOLDERNAME_LENGTH + " characters exceeded").toJson();
            }
            RMSUserPrincipal principal;
            try (DbSession session = DbSession.newSession()) {
                Tenant loginTenant = session.get(Tenant.class, loginTenantId);
                principal = new RMSUserPrincipal(us, loginTenant);
                repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
                // since system bucket membership is dynamic there is no Membership record whose last action time has to be updated here
            }
            CreateFolderResult result = repository.createFolder(folderName, path, path, autoRename);
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
                LOGGER.debug("Invalid folderName (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, folderName, e);
            }
            return new JsonResponse(4001, "Invalid folder name").toJson();
        } catch (FileAlreadyExistsException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Folder already exists (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, folderName, e);
            }
            return new JsonResponse(4002, "Folder already exists.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (tenant ID: {}, user ID: {}) in EnterpriseWS: '{}'", loginTenantId, userId, path, e);
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
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "createFolder", !error ? 1 : 0, userId, loginTenantId, path);
        }
    }

    @Secured
    @POST
    @Path("/decrypt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response decrypt(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        File outputPath = null;
        RMSUserPrincipal principal;
        Tenant loginTenant;
        String loginTenantId;
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
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            try (DbSession session = DbSession.newSession()) {
                principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
                repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
                loginTenant = session.get(Tenant.class, loginTenantId);
                // since system bucket membership is dynamic there is no Membership record whose last action time has to be updated here
            }
            RepositoryContent fileMetadata = repository.getFileMetadata(filePath);
            if (fileMetadata == null) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(404, "Missing file.").toJson()).build();
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            String duid = EnterpriseWorkspaceService.getEnterpriseSpaceFileDUID(loginTenantId, filePath);
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
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, loginTenant.getName(), userEval, tags);
                    evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rightsList = evalResponse.getRights();
                } else {
                    // Adhoc-file need check expiration.
                    FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
                    isExpired = AdhocEvalAdapter.isFileExpired(policy);
                    isNotYetValid = AdhocEvalAdapter.isNotYetValid(policy);
                }

                if (!ArrayUtils.contains(rightsList, Rights.DECRYPT) || isExpired || isNotYetValid) {
                    Operations ops = Operations.DECRYPT;
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.ENTERPRISEWS);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    return Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
                }
            }

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            File decryptedFile;
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
                        originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : EnterpriseWorkspaceService.getFileNameWithoutNXL(output.getName());
                    }
                    decryptedFile = new File(output.getParent(), originalFileName);
                    try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                        DecryptUtil.decrypt(nxl, token, fos);
                    }
                } catch (JsonException e) {
                    throw new NxlException("Invalid token", e);
                }
            }
            fileName = decryptedFile.getName();
            output = decryptedFile;
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileName));
            if (output.length() > 0) {
                response.setHeader("x-rms-file-size", Long.toString(output.length()));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                try (InputStream fis = new FileInputStream(output)) {
                    IOUtils.copy(fis, response.getOutputStream());
                }
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(ownerMembership.getName()), userId, Operations.DECRYPT, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
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
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
            Audit.audit(request, "API", "ProjectMgmt", "decrypt", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteItem(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {
        String path = null;
        boolean error = true;
        boolean isFolder = false;
        DefaultRepositoryTemplate repository = null;
        String loginTenantId = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            path = req.getParameter("pathId");
            if (!StringUtils.hasText(path)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            if (path.endsWith("/")) {
                isFolder = true;
            }
            String duid = EnterpriseWorkspaceService.getEnterpriseSpaceFileDUID(loginTenantId, path);
            String fileName = StringUtils.substringAfterLast(path, "/");
            RMSUserPrincipal principal;
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            User user = us.getUser();
            try (DbSession session = DbSession.newSession()) {
                // TODO: Replace this privilege given to uploader with Document Manager in Phase 2
                if (!UserService.checkTenantAdmin(session, loginTenantId, userId)) {
                    if (!isFolder) {
                        ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(user.getEmail()), userId, Operations.DELETE, deviceId, platformId, loginTenantId, path, fileName, path, null, null, null, AccessResult.DENY, new Date(), null, AccountType.ENTERPRISEWS);
                        session.beginTransaction();
                        session.save(log);
                        session.commit();
                    }
                    return new JsonResponse(401, "Unauthorised").toJson();
                }
                Tenant loginTenant = session.get(Tenant.class, loginTenantId);
                principal = new RMSUserPrincipal(us, loginTenant);
                repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
                // since system bucket membership is dynamic there is no Membership record whose last action time has to be updated here
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
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(user.getEmail()), userId, Operations.DELETE, deviceId, platformId, loginTenantId, path, fileName, path, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
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
                return new JsonResponse(404, "EnterpriseWS folder not found").toJson();
            } else {
                return new JsonResponse(404, "EnterpriseWS file not found").toJson();
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
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "delete", !error ? 1 : 0, userId, loginTenantId, path);
        }
    }

    @Secured
    @GET
    @Path("/folderMetadata")
    @Produces(MediaType.APPLICATION_JSON)
    public String folderMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("pathId") String parentPath, @QueryParam("lastModified") long lastModifiedTime) {
        boolean error = true;
        String loginTenantId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            if (lastModifiedTime == 0L || !StringUtils.hasText(parentPath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (!"/".equals(Nvl.nvl(parentPath, "/")) && EnterpriseWorkspaceService.isPathExist(loginTenantId, parentPath)) {
                return new JsonResponse(404, "EnterpriseWS folder not found.").toJson();
            }
            JsonEnterpriseSpaceFileList fileList;
            Map<String, Long> enterpriseSpaceStatus;
            Long folderLastUpdatedTime;
            try (DbSession session = DbSession.newSession()) {
                boolean folderUpdated = EnterpriseWorkspaceService.checkFolderUpdated(session, loginTenantId, parentPath, lastModifiedTime);
                if (!folderUpdated) {
                    return new JsonResponse(304, "Folder not modified.").toJson();
                }
                page = page != null && page > 0 ? page : 1;
                size = size != null && size > 0 ? size : -1;
                List<Order> orders = util.getOrdersList(orderBy);
                fileList = EnterpriseWorkspaceService.getJsonFileList(session, loginTenantId, page, size, orders, parentPath, null, null);
                enterpriseSpaceStatus = EnterpriseWorkspaceService.getEnterpriseSpaceStatus(session, loginTenantId);
                folderLastUpdatedTime = EnterpriseWorkspaceService.getFolderLastUpdatedTime(session, loginTenantId, parentPath);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", fileList);
            response.putResult("folderLastUpdatedTime", folderLastUpdatedTime);
            response.putResult("usage", enterpriseSpaceStatus.get(RepoConstants.STORAGE_USED));
            response.putResult("quota", enterpriseSpaceStatus.get(EnterpriseWorkspaceService.ENTERPRISE_SPACE_QUOTA));
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "folderMetadata", !error ? 1 : 0, userId, parentPath, loginTenantId);
        }
    }

    @Secured
    @POST
    @Path("/file/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getFileInfo(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String filePath = null;
        String loginTenantId = null;
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
            loginTenantId = us.getLoginTenant();
            User user = us.getUser();

            try (DbSession session = DbSession.newSession()) {
                Tenant loginTenant = session.get(Tenant.class, loginTenantId);
                RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
                String deviceId = request.getHeader("deviceId");
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(userId)).ticket(ticket).clientId(clientId).platformId(platformId).deviceId(deviceId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();

                JsonEnterpriseSpaceFileInfo fileInfo = EnterpriseWorkspaceService.getEnterpriseSpaceFileInfo(session, principal, userEval, loginTenantId, filePath);
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
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "getFileInfo", !error ? 1 : 0, userId, loginTenantId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/file/checkIfExists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String checkFileExists(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String filePath = null;
        String loginTenantId = null;
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
            loginTenantId = us.getLoginTenant();

            boolean fileExists = EnterpriseWorkspaceService.checkFileExists(us, loginTenantId, filePath);
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
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "checkFileExists", !error ? 1 : 0, userId, loginTenantId, filePath);
        }
    }

    @Secured
    @PUT
    @Path("/file/classification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reclassify(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, String json) {
        String fileName;
        String fileParentPathId;
        String duid = null;
        Tenant tenant;
        boolean error = true;
        boolean holdingLock = false;
        String loginTenantId = null;
        String owner;
        try (DbSession session = DbSession.newSession()) {
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            User user = us.getUser();
            loginTenantId = us.getLoginTenant();
            tenant = session.get(Tenant.class, loginTenantId);
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

            RMSUserPrincipal principal = new RMSUserPrincipal(us, tenant);
            com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(userId)).ticket(ticket).clientId(clientId).platformId(platformId).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();

            JsonEnterpriseSpaceFileInfo fileInfo = EnterpriseWorkspaceService.getEnterpriseSpaceFileInfo(session, principal, userEval, loginTenantId, filePath);
            if (fileInfo == null) {
                return new JsonResponse(404, "Invalid file.").toJson();
            }

            if (fileInfo.getProtectionType() != ProtectionType.CENTRAL.ordinal()) {
                return new JsonResponse(403, "Reclassification is only for company rights protected files.").toJson();
            }

            holdingLock = true;

            DefaultRepositoryTemplate repository = new DefaultRepositoryEnterprise(session, principal, loginTenantId);
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
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, Operations.CLASSIFY, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.ENTERPRISEWS);
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

                String membership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, loginTenantId);
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
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, owner, userId, Operations.CLASSIFY, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
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
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "reclassify", error ? 0 : 1, userId, loginTenantId, duid);
        }
    }

    @Secured
    @POST
    @Path("/v2/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadV2(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        File outputPath = null;
        RMSUserPrincipal principal;
        String filePath = null;
        boolean error = true;
        Operations ops;
        String loginTenantId;
        DefaultRepositoryTemplate repository = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);
            principal = new RMSUserPrincipal(us, loginTenant);
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            filePath = req.getParameter("pathId");
            Validator.validateFilePath(filePath);
            repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
            RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
            String fileName = fileMetadata.getName();
            DownloadType downloadType = Validator.validateDownloadType(req.getIntParameter("type", -1), SPACETYPE.ENTERPRISESPACE);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            boolean partialDownload = start >= 0 && length >= 0;
            ops = util.getDownloadOps(downloadType, partialDownload);

            String duid = EnterpriseWorkspaceService.validateFileDuid(loginTenantId, filePath);
            String path = HTTPUtil.getInternalURI(request);
            outputPath = RepositoryFileUtil.getTempOutputFolder();
            File output;
            String fileOwner = util.getFileOwner(duid);
            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            Rights[] rightList = util.getUserFileRights(principal, loginTenantId, us, filePath, outputPath, downloadType, partialDownload);
            if (util.checkUserFileRights(downloadType, rightList)) {
                if (partialDownload && downloadType != DownloadType.NORMAL) {
                    byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
                    if (data != null) {
                        long contentLength = data.length;
                        response.setHeader("x-rms-file-size", Long.toString(contentLength));
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                        response.getOutputStream().write(data);
                    }
                }
                if (partialDownload && downloadType == DownloadType.NORMAL) {
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(fileOwner), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.DENY, new Date(), null, AccountType.ENTERPRISEWS);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                    return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "This is not a supported operation.").toJson()).build();
                } else {
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileName));
                    if (downloadType == DownloadType.NORMAL) {
                        output = util.downloadCopy(principal, loginTenantId, us, filePath, path, outputPath);
                    } else {
                        output = util.downloadOriginal(principal, loginTenantId, filePath, outputPath);
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

            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, fileOwner, principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
            RemoteLoggingMgmt.saveActivityLog(activity);
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", downloadType.getDisplayName(), 1, userId, path);
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
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "download", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @POST
    @Path("/fileHeader")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadHeader(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        File outputPath = null;
        RMSUserPrincipal principal;
        String filePath = null;
        boolean error = true;
        Operations ops;
        String loginTenantId;
        DefaultRepositoryTemplate repository = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(400, "Missing request.").toJson()).build();
            }

            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            loginTenantId = us.getLoginTenant();
            Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);
            principal = new RMSUserPrincipal(us, loginTenant);
            principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            filePath = req.getParameter("pathId");
            Validator.validateFilePath(filePath);
            repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
            RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
            String fileName = fileMetadata.getName();

            DownloadType downloadType = DownloadType.HEADER;
            ops = Operations.HEADER_DOWNLOAD;
            int start = 0;
            long length = 16384;
            String duid = EnterpriseWorkspaceService.validateFileDuid(loginTenantId, filePath);
            String fileOwner = util.getFileOwner(duid);
            String path = HTTPUtil.getInternalURI(request);
            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));

            outputPath = RepositoryFileUtil.getTempOutputFolder();

            byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
            if (data != null) {
                long contentLength = data.length;
                response.setHeader("x-rms-file-size", Long.toString(contentLength));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                response.getOutputStream().write(data);
            }

            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, fileOwner, userId, ops, deviceId, platformId, loginTenantId, filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.ENTERPRISEWS);
            RemoteLoggingMgmt.saveActivityLog(activity);
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", downloadType.getDisplayName(), 1, userId, path);

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
            Audit.audit(request, "API", "EnterpriseWorkspaceMgmt", "download header", !error ? 1 : 0, userId, filePath);
        }
    }

}
