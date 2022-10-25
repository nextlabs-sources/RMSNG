package com.nextlabs.rms.rs;

import com.google.gson.JsonParseException;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonMyVaultMetadata;
import com.nextlabs.common.shared.JsonRepoFile;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.EscapeUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.dto.repository.SharedNxlFile;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.ActivityLog;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.FavoriteFile;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.json.SharedFile;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.IRMSRepositorySearcher;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.RMSRepositoryDBSearcherImpl;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.service.MyVaultService;
import com.nextlabs.rms.share.SharePersonalMapper;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.RabbitMQUtil;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.MyVaultDownloadUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;
import com.nextlabs.rms.util.RestUploadUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@Path("/myVault")
public class MyVaultMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public static final String PARAM_FILEPATH_ID = "pathId";

    @Secured
    @GET
    @Path("/uploadTicket")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTicket(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        JsonResponse resp = new JsonResponse("OK");
        resp.putResult("exchange", RabbitMQUtil.MYVAULT_UPLOAD_EXCHANGE_NAME);
        resp.putResult("routing_key", RabbitMQUtil.generateRoutingKey(userId));
        return resp.toJson();
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/uploadLargeFile/{routingKey}")
    public String uploadLargeFile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("routingKey") String routingKey) {
        if (!StringUtils.hasText(routingKey)) {
            new JsonResponse(400, "Missing routing key").toJson();
        }
        String responseMessage = upload(request, userId, ticket, clientId, platformId);
        RabbitMQUtil.sendDirectMessage(routingKey, RabbitMQUtil.MYVAULT_UPLOAD_EXCHANGE_NAME, responseMessage);
        return responseMessage;
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/upload")
    public String upload(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        RMSUserPrincipal principal = null;
        boolean error = true;
        String fileName = null;
        RestUploadRequest uploadReq = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            DefaultRepositoryManager.checkVaultStorageExceeded(principal);

            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            fileName = uploadReq.getFileName();
            if (!StringUtils.hasText(fileName) || uploadReq.getFileStream() == null || !StringUtils.hasText(uploadReq.getJson())) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (!StringUtils.endsWithIgnoreCase(fileName, com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
                return new JsonResponse(5004, "Invalid file extension.").toJson();
            }
            JsonRequest req = JsonRequest.fromJson(uploadReq.getJson());
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            String sourcePathId = req.getParameter("srcPathId");
            String sourcePathDisplay = req.getParameter("srcPathDisplay");
            String srcRepoId = req.getParameter("srcRepoId");
            String srcRepoName = req.getParameter("srcRepoName");
            String srcRepoType = req.getParameter("srcRepoType");
            boolean userConfirmedFileOverwrite = Boolean.parseBoolean(req.getParameter("userConfirmedFileOverwrite"));
            boolean validPath = Validator.isValidDirectoryPath(fileName, uploadReq.getUploadDir());
            if (!validPath) {
                return new JsonResponse(5003, "Invalid filename").toJson();
            }
            boolean validRepoParams = StringUtils.hasText(srcRepoName) && StringUtils.hasText(srcRepoType);
            if (!validRepoParams) {
                return new JsonResponse(5002, "Invalid repository metadata.").toJson();
            }

            File tmpFile = new File(uploadReq.getUploadDir(), fileName);
            Files.copy(uploadReq.getFileStream(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String duid;
            String owner;
            try (InputStream fis = new FileInputStream(tmpFile);
                    NxlFile nxlMetadata = NxlFile.parse(fis)) {
                duid = nxlMetadata.getDuid();
                owner = nxlMetadata.getOwner();
                if (!StringUtils.hasText(duid) || !StringUtils.hasText(owner)) {
                    return new JsonResponse(400, "Invalid Nxl file").toJson();
                } else {
                    if (nxlMetadata.getContentLength() == 0) {
                        return new JsonResponse(5005, "Empty files are not allowed to be uploaded.").toJson();
                    }
                    try (DbSession session = DbSession.newSession()) {
                        Membership membership = session.get(Membership.class, owner);
                        if (membership == null || membership.getUser().getId() != userId) {
                            return new JsonResponse(403, "Access denied.").toJson();
                        }
                        if (StringUtils.hasText(membership.getTenant().getParentId())) {
                            return new JsonResponse(405, "This '.nxl' file is not eligible for this operation based on its tenant membership").toJson();
                        }
                    }
                    AllNxl nxl;
                    DefaultRepositoryTemplate repository;
                    try (DbSession session = DbSession.newSession()) {
                        nxl = session.get(AllNxl.class, duid);
                        repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
                    }
                    StoreItem existingFile = repository.getExistingStoreItem(RepoConstants.MY_VAULT_FOLDER_PATH_ID + fileName);
                    if (nxl == null) {
                        if (existingFile != null && !userConfirmedFileOverwrite) {
                            return new JsonResponse(4001, "File already exists").toJson();
                        }
                        FilePolicy policy = DecryptUtil.getFilePolicy(nxlMetadata, null);
                        EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, true);
                        Rights[] rights = evalResponse.getRights();
                        SharedFileDAO.updateNxlDBProtect(userId, duid, owner, Rights.toInt(rights), fileName, GsonUtils.GSON.toJson(policy, FilePolicy.class));
                    } else if (userConfirmedFileOverwrite) {
                        if (existingFile != null) {
                            FilePolicy policy = DecryptUtil.getFilePolicy(nxlMetadata, null);
                            EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, true);
                            Rights[] rights = evalResponse.getRights();
                            SharedFileDAO.replaceNxlDBProtect(userId, duid, owner, Rights.toInt(rights), fileName, GsonUtils.GSON.toJson(policy, FilePolicy.class));
                        } else {
                            return new JsonResponse(5006, "File name mismatch while replacing file with same duid").toJson();
                        }
                    } else {
                        return new JsonResponse(5005, "File already exists.").toJson();
                    }
                }
            } catch (NxlException e) {
                return new JsonResponse(5001, "Invalid NXL format.").toJson();
            }

            boolean hasRepo = StringUtils.hasText(srcRepoId) && !"Local".equalsIgnoreCase(srcRepoType);
            UploadedFileMetaData metadata = RepositoryFileUtil.uploadFileToMyVault(principal, srcRepoId, srcRepoName, srcRepoType, sourcePathId, sourcePathDisplay, tmpFile, false, tmpFile.getName(), hasRepo, userConfirmedFileOverwrite, request);
            String pathId = metadata.getPathId();
            String pathDisplay = metadata.getPathDisplay();
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("name", fileName);
            resp.putResult("size", tmpFile.length());
            resp.putResult("lastModified", metadata.getLastModifiedTime().getTime());
            resp.putResult("duid", metadata.getDuid());
            if (pathId != null) {
                resp.putResult("pathId", pathId);
            }
            if (pathDisplay != null) {
                pathDisplay = StringUtils.substringAfter(pathDisplay, RepoConstants.MY_VAULT_FOLDER_PATH_DISPLAY);
                resp.putResult("pathDisplay", pathDisplay.startsWith("/") ? pathDisplay : "/" + pathDisplay);
            }
            error = false;
            return resp.toJson();
        } catch (FileConflictException e) {
            return new JsonResponse(4001, "File already exists").toJson();
        } catch (VaultStorageExceededException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Vault Storage Exceeded: {}", e.getStorage());
            }
            return new JsonResponse(6002, "Vault Storage Exceeded.").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled() && uploadReq != null) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", uploadReq.getJson(), e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (NxlException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(5001, "Invalid NXL format.").toJson();
        } catch (SocketTimeoutException e) {
            return new JsonResponse(500, "Socket timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when uploading file to MyVault (user ID: {}, fileName: {}): {}", userId, fileName, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when uploading file to MyVault (user ID: {}, fileName: {}): {}", userId, fileName, e.getMessage(), e);
            }
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "MyVaultMgmt", "upload", !error ? 1 : 0, principal != null ? principal.getUserId() : 0, fileName);
        }
    }

    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("filter") String filter) {
        boolean error = true;
        String fileName = null;
        try {
            page = page != null && page > 0 ? page : 1;
            size = size != null && size > 0 ? size : -1;
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Repository repository = DefaultRepositoryManager.getDefaultRepository(session, session.load(User.class, userId), tenant);
                String repoId = null;
                if (repository != null) {
                    repoId = repository.getId();
                }

                fileName = request.getParameter("q.fileName");
                List<Order> orders = Collections.emptyList();
                if (StringUtils.hasText(orderBy)) {
                    Map<String, String> supportedFields = new HashMap<>(2);
                    supportedFields.put("fileName", "n.fileName");
                    supportedFields.put("creationTime", "n.creationTime");
                    supportedFields.put("size", "r.size");
                    OrderBuilder builder = new OrderBuilder(supportedFields);
                    List<String> list = StringUtils.tokenize(orderBy, ",");
                    for (String s : list) {
                        builder.add(s);
                    }
                    orders = builder.build();
                }

                List<Criterion> search = new ArrayList<>(2);
                if (StringUtils.hasText(fileName)) {
                    search.add(EscapedLikeRestrictions.ilike("n.fileName", fileName, MatchMode.ANYWHERE));
                }

                Long totalFiles;
                List<SharedFile> fileList;

                if (filter != null && filter.equalsIgnoreCase(SharedFileDAO.FILTER_OPTION_FAVORITE)) {
                    //
                    // Get a list of favorite files
                    //
                    List<FavoriteFile> favFiles = RepositoryFileManager.findMyVaultFavorites(session, repoId, fileName);
                    totalFiles = Long.valueOf(favFiles.size());

                    // Required to restrict to only myVault files
                    search.add(Restrictions.ilike("n.fileName", ".nxl", MatchMode.END));
                    List<SharedNxlFile> filesSharedByUser = SharedFileManager.getFavoriteFilesByUser(session, userId, page, size, search, orders, filter, favFiles);

                    fileList = SharedFileManager.getSharedFileArray(session, repoId, filesSharedByUser);

                } else {
                    totalFiles = SharedFileManager.getTotalFileSharedByUser(session, userId, search, filter);
                    List<SharedNxlFile> filesSharedByUser = SharedFileManager.getFilesSharedByUser(session, userId, page, size, search, orders, filter);
                    fileList = SharedFileManager.getSharedFileArray(session, repoId, filesSharedByUser);
                }
                Map<String, Object> map = new HashMap<>(2);
                map.put("totalFiles", totalFiles);
                map.put("files", fileList);
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("detail", map);
                error = false;
                return resp.toJson();
            }
        } catch (HibernateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "MyVaultMgmt", "list", !error ? 1 : 0, fileName);
        }
    }

    @Secured
    @POST
    @Path("/{duid}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("duid") String duid, String json) {
        boolean error = true;
        String pathId = null;
        DefaultRepositoryTemplate repository = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            pathId = req.getParameter(PARAM_FILEPATH_ID);
            if (!StringUtils.hasText(pathId) || !StringUtils.hasText(duid)) {
                return new JsonResponse(400, "Missing paramters").toJson();
            }
            platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            RMSUserPrincipal principal;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            try (DbSession session = DbSession.newSession()) {

                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }

            StoreItem item = null;
            List<JsonRepoFile> fileList = null;
            try {
                IRMSRepositorySearcher searcher = new RMSRepositoryDBSearcherImpl();
                item = searcher.getRepoItem(repository.getRepoId(), pathId);
                if (item == null) {
                    return new JsonResponse(404, "File not found.").toJson();
                } else if (item.isDeleted()) {
                    return new JsonResponse(5001, "File is deleted.").toJson();
                }
                AllNxl nxlEntity = item.getNxl();
                if (nxlEntity == null) {
                    return new JsonResponse(404, "File not found.").toJson();
                }
                String duidNxl = nxlEntity.getDuid();
                if (!StringUtils.equals(duid, duidNxl)) {
                    return new JsonResponse(403, "Access Denied.").toJson();
                }
                JsonRepoFile jsonFile = new JsonRepoFile(pathId, pathId);
                fileList = new ArrayList<>();
                fileList.add(jsonFile);
                boolean revokeSuccess = new SharePersonalMapper().revokeFile(duid, userId);
                final Date now = new Date();
                try (DbSession session = DbSession.newSession()) {
                    AllNxl nxl = session.get(AllNxl.class, duid);
                    String owner = nxl == null ? "" : Nvl.nvl(nxl.getOwner());
                    ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, owner, userId, Operations.REVOKE, deviceId, platformId, null, null, null, null, null, null, null, revokeSuccess ? AccessResult.ALLOW : AccessResult.DENY, now, null, AccountType.PERSONAL);
                    session.beginTransaction();
                    session.save(log);
                    session.commit();
                }
                if (!revokeSuccess) {
                    return new JsonResponse(400, "Error occurred while revoking before deleting the file.").toJson();
                }
            } catch (FileAlreadyRevokedException e) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("File with DUID '{}' has been revoked", duid);
                }
            } finally {
                try (DbSession session = DbSession.newSession()) {
                    if (fileList != null && !fileList.isEmpty()) {
                        RepositoryFileManager.unmarkFilesAsFavorite(session, repository.getRepoId(), fileList);
                    }
                }
            }
            String pathDisplay = item.getFilePathDisplay();
            pathDisplay = StringUtils.substringAfter(pathDisplay, RepoConstants.MY_VAULT_FOLDER_PATH_DISPLAY);
            RepositoryFileUtil.deleteFileFromRepo(repository, pathId, pathDisplay);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("File is deleted from MyVault (userId: {}): {}", userId, pathDisplay);
            }
            JsonResponse resp = new JsonResponse("OK");
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (ForbiddenOperationException e) {
            return new JsonResponse(403, "Forbidden Operation").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyVaultMgmt", "delete", !error ? 1 : 0, userId, duid, pathId);
        }
    }

    @Secured
    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void download(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json)
            throws IOException {
        File outputPath = null;
        boolean error = true;
        RMSUserPrincipal principal;
        boolean downloadForView = false;
        String pathId = null;
        DefaultRepositoryTemplate repository = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                response.sendError(Status.BAD_REQUEST.getStatusCode(), "Missing request.");
                return;
            }
            pathId = req.getParameter(PARAM_FILEPATH_ID);
            if (!StringUtils.hasText(pathId) || !StringUtils.startsWith(pathId, "/")) {
                response.sendError(Status.BAD_REQUEST.getStatusCode(), "Missing required parameters.");
                return;
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            boolean isNxl = pathId.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            Boolean forViewer = req.getParameter("forViewer", Boolean.class);
            downloadForView = forViewer != null && forViewer;
            Rights rights = downloadForView ? Rights.VIEW : Rights.DOWNLOAD;
            boolean partialDownload = start >= 0 && length >= 0;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                response.sendError(Status.BAD_REQUEST.getStatusCode(), "Invalid tenant.");
                return;
            }
            principal = new RMSUserPrincipal(us, tenant);
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }
            RepositoryContent fileMetadata = repository.getFileMetadata(pathId);
            if (fileMetadata == null) {
                response.sendError(Status.NOT_FOUND.getStatusCode(), "Missing file.");
                return;
            }
            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            String duid = fileMetadata.getDuid();
            if (!StringUtils.hasText(duid) && isNxl) {
                response.sendError(Response.Status.NOT_FOUND.getStatusCode(), "Missing file.");
                return;
            }
            if (isNxl && !MyVaultService.checkRights(duid, userId, rights)) {
                Operations ops = null;
                if (!downloadForView) {
                    ops = Operations.DOWNLOAD;
                } else if (platformId != DeviceType.WEB.getLow()) {
                    ops = Operations.OFFLINE;
                }
                if (ops != null) {
                    try (DbSession session = DbSession.newSession()) {
                        ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(repository.getRepoId()), pathId, pathId, pathId, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PERSONAL);
                        session.beginTransaction();
                        session.save(log);
                        session.commit();
                    }
                }
                response.sendError(Response.Status.FORBIDDEN.getStatusCode(), "Authorization failed.");
                return;
            }

            if (downloadForView) {
                response.setHeader("x-rms-repo-id", repository.getRepoId());
                response.setHeader("x-rms-file-path-id", EscapeUtils.escapeNonASCIICharacters(fileMetadata.getPathId()));
                response.setHeader("x-rms-file-path-display", EscapeUtils.escapeNonASCIICharacters(fileMetadata.getPath()));
            }

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            if (partialDownload) {
                byte[] data = repository.downloadPartialFile(pathId, pathId, start, start + length - 1);
                if (data != null) {
                    long contentLength = data.length;
                    response.setHeader("x-rms-file-size", Long.toString(contentLength));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                    response.getOutputStream().write(data);
                }
            } else {
                outputPath = RepositoryFileUtil.getTempOutputFolder();
                File output = repository.getFile(pathId, pathId, outputPath.getPath());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileMetadata.getName()));
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            }

            Operations ops = null;
            if (!downloadForView) {
                ops = Operations.DOWNLOAD;
            } else if (platformId != DeviceType.WEB.getLow()) {
                ops = Operations.OFFLINE;
            }
            if (ops != null && !partialDownload) {
                try (DbSession session = DbSession.newSession()) {
                    ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(repository.getRepoId()), pathId, pathId, pathId, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PERSONAL);
                    session.beginTransaction();
                    session.save(log);
                    session.commit();
                }
            }
            error = false;
        } catch (FileNotFoundException e) {
            if (!response.isCommitted()) {
                response.sendError(Status.NOT_FOUND.getStatusCode(), "File not found.");
            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(Status.BAD_REQUEST.getStatusCode(), "Malformed request.");
            }
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error.");
            }
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, pathId, e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "IO Error.");
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
            if (!downloadForView) {
                Audit.audit(request, "API", "MyVaultMgmt", "download", !error ? 1 : 0, userId);
            } else if (platformId != DeviceType.WEB.getLow()) {
                Audit.audit(request, "API", "MyVaultMgmt", "downloadForOffline", !error ? 1 : 0, userId);
            } else {
                Audit.audit(request, "API", "MyVaultMgmt", "downloadForView", !error ? 1 : 0, userId);
            }
        }
    }

    @Secured
    @POST
    @Path("/v2/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadV2(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json)
            throws IOException {
        File outputPath = null;
        boolean error = true;
        RMSUserPrincipal principal;
        String pathId = null;
        DefaultRepositoryTemplate repository = null;
        DownloadType downloadType = DownloadType.NORMAL;
        Operations ops;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Missing request.").toJson()).build();

            }
            pathId = req.getParameter(PARAM_FILEPATH_ID);
            if (!StringUtils.hasText(pathId) || !StringUtils.startsWith(pathId, "/")) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Missing required parameters.").toJson()).build();

            }

            if (platformId == null) {
                platformId = DeviceType.WEB.getLow();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            boolean isNxl = pathId.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            int downloadTypeOrdinal = req.getIntParameter("type", -1);
            boolean partialDownload = start >= 0 && length >= 0;

            if (downloadTypeOrdinal < 0 || downloadTypeOrdinal > 2) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Missing/Wrong download type.").toJson()).build();
            }
            downloadType = Validator.validateDownloadType(req.getIntParameter("type", -1), SPACETYPE.MYSPACE);

            MyVaultDownloadUtil util = new MyVaultDownloadUtil();
            ops = util.getDownloadOps(downloadType, partialDownload);
            Rights rights = util.getRights(downloadType);

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Invalid tenant.").toJson()).build();
            }
            principal = new RMSUserPrincipal(us, tenant);
            repository = util.getRepository(principal);
            RepositoryContent fileMetadata = repository.getFileMetadata(pathId);
            if (fileMetadata == null) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Missing file.").toJson()).build();
            }

            String duid = fileMetadata.getDuid();
            if (!StringUtils.hasText(duid) && isNxl) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.NOT_FOUND.getStatusCode(), "Missing file.").toJson()).build();
            }
            boolean isValid = MyVaultService.checkRights(duid, userId, rights);
            if (isNxl && !isValid) {
                if (ops != null) {
                    try (DbSession session = DbSession.newSession()) {
                        ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(repository.getRepoId()), pathId, pathId, pathId, null, null, null, AccessResult.DENY, new Date(), null, AccountType.PERSONAL);
                        session.beginTransaction();
                        session.save(log);
                        session.commit();
                    }
                }

                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.FORBIDDEN.getStatusCode(), "Missing file.").toJson()).build();
            }

            if (downloadType == DownloadType.FOR_VIEWER) {
                response.setHeader("x-rms-repo-id", repository.getRepoId());
                response.setHeader("x-rms-file-path-id", EscapeUtils.escapeNonASCIICharacters(fileMetadata.getPathId()));
                response.setHeader("x-rms-file-path-display", EscapeUtils.escapeNonASCIICharacters(fileMetadata.getPath()));
            }

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            if (partialDownload && isValid) {
                byte[] data = repository.downloadPartialFile(pathId, pathId, start, start + length - 1);
                if (data != null) {
                    long contentLength = data.length;
                    response.setHeader("x-rms-file-size", Long.toString(contentLength));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                    response.getOutputStream().write(data);
                }
            } else {
                outputPath = RepositoryFileUtil.getTempOutputFolder();
                File output = repository.getFile(pathId, pathId, outputPath.getPath());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(fileMetadata.getName()));
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            }

            if (ops != null) {
                try (DbSession session = DbSession.newSession()) {
                    ActivityLog log = RemoteLoggingMgmt.createActivityLog(duid, Nvl.nvl(fileMetadata.getOwner()), userId, ops, deviceId, platformId, String.valueOf(repository.getRepoId()), pathId, pathId, pathId, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PERSONAL);
                    session.beginTransaction();
                    session.save(log);
                    session.commit();
                }
            }
            error = false;

        } catch (FileNotFoundException e) {
            if (!response.isCommitted()) {
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.FORBIDDEN.getStatusCode(), "File not found.").toJson()).build();

            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.BAD_REQUEST.getStatusCode(), "Malformed request.").toJson()).build();
            }
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error.").toJson()).build();
            }
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file (user ID: {}, path ID: '{}'): {}", userId, pathId, e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "IO Error.").toJson()).build();

            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            if (!response.isCommitted()) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error.").toJson()).build();
            }
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
            Audit.audit(request, "API", "MyVaultMgmt", downloadType.getDisplayName(), !error ? 1 : 0, userId);
        }
        return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/file/checkIfExists")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkFileExists(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String pathId = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            pathId = req.getParameter(PARAM_FILEPATH_ID);
            if (!StringUtils.hasText(pathId)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }
            if (!StringUtils.startsWith(pathId, "/")) {
                return new JsonResponse(400, "Invalid pathId.").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant.").toJson();
            }

            boolean fileExists = MyVaultService.checkFileExists(pathId, us);
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
            Audit.audit(request, "API", "MyVaultMgmt", "checkFileExists", !error ? 1 : 0, userId, pathId);
        }
    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{duid}/metadata")
    public String getFileMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("duid") String duid, String json) {
        boolean error = true;
        String pathId = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            pathId = req.getParameter(PARAM_FILEPATH_ID);
            if (!StringUtils.hasText(pathId)) {
                return new JsonResponse(400, "Missing required parameters.").toJson();
            }
            if (!StringUtils.startsWith(pathId, "/")) {
                return new JsonResponse(400, "Invalid pathId.").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant.").toJson();
            }
            JsonMyVaultMetadata metadata = SharedFileManager.getMyVaultMetadata(userId, duid, pathId);
            if (metadata == null) {
                return new JsonResponse(404, "Invalid file.").toJson();
            }
            SharingTransaction firstTransaction = new SharePersonalMapper().getFirstTransactionByDuid(duid);
            if (firstTransaction != null) {
                Date creationTime = firstTransaction.getCreationTime();
                metadata.setSharedOn(creationTime.getTime());
                String baseURL = SharedFileManager.getBaseShareURL(request, us.getLoginTenant());
                String sharedLink = baseURL + SharedFileManager.getSharingURLQueryString(firstTransaction.getId());
                metadata.setFileLink(sharedLink);
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("detail", metadata);
            error = false;
            return response.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "MyVaultMgmt", "getFileMetadata", !error ? 1 : 0, userId, duid, pathId);
        }
    }

    @Secured
    @POST
    @Path("/fileHeader")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadHeader(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {
        RMSUserPrincipal principal;
        String filePath = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        DownloadType downloadType = DownloadType.HEADER;
        Operations ops = Operations.HEADER_DOWNLOAD;
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
            Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            filePath = req.getParameter(PARAM_FILEPATH_ID);
            Validator.validateFilePath(filePath);
            boolean isNxl = filePath.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
            int start = 0;
            long length = 16384;
            String path = HTTPUtil.getInternalURI(request);

            MyVaultDownloadUtil util = new MyVaultDownloadUtil();
            repository = util.getRepository(principal);
            RepositoryContent fileMetadata = Validator.validateFileMetadata(filePath, repository);
            String fileName = fileMetadata.getName();
            String duid = fileMetadata.getDuid();
            util.validateFileDuid(duid, isNxl);

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));

            byte[] data = repository.downloadPartialFile(filePath, filePath, start, start + length - 1);
            if (data != null) {
                long contentLength = data.length;
                response.setHeader("x-rms-file-size", Long.toString(contentLength));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                response.getOutputStream().write(data);
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(fileMetadata.getOwner()), principal.getUserId(), ops, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(repository.getRepoId()), filePath, fileName, filePath, null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.PROJECT);
            util.saveDownloadActivityLog(ops, activity);
            Audit.audit(request, "API", "MyVaultMgmt", downloadType.getDisplayName(), 1, userId, path);
            error = false;
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when download file header (user ID: {}, path ID: '{}'): {}", userId, filePath, e.getMessage(), e);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(500, "IO Error.").toJson()).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly((Closeable)repository);
            }
            Audit.audit(request, "API", "MyVaultMgmt", "download header", !error ? 1 : 0, userId, filePath);
        }
    }

    @Secured
    @GET
    @Path("/fileCount")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTotalFileCount(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        DefaultRepositoryTemplate repository = null;
        RMSUserPrincipal principal = null;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
                Long totalFileCount = repository.getTotalFileCount(session, repository.getRepoId(), true);
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("totalFileCount", totalFileCount);
                error = false;
                return resp.toJson();
            }
        } catch (InvalidDefaultRepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (ClassCastException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyVaultMgmt", "getTotalFileCount", error ? 0 : 1, principal != null ? principal.getUserId() : null);
        }
    }

}
