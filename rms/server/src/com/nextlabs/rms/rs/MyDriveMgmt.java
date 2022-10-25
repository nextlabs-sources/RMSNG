package com.nextlabs.rms.rs;

import com.google.gson.JsonParseException;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.JsonRepoFile;
import com.nextlabs.common.shared.JsonRepositoryFileEntry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.DriveStorageExceededException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RabbitMQUtil;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.RepositoryFileUtil;
import com.nextlabs.rms.util.RestUploadUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/myDrive")
public class MyDriveMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String PATH_ID_KEY = "pathId";
    private static final String PARENT_PATH_ID_KEY = "parentPathId";
    private static final String LAST_MODIFIED_TIME_KEY = "lastModifiedTime";
    private static final String NAME_KEY = "name";
    private static final String FILE_OVERWRITE = "userConfirmedFileOverwrite";

    protected void dummyMethodToFixPMD() {

    }

    @Secured
    @POST
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        RMSUserPrincipal principal = null;
        String pathId = null;
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }

            pathId = req.getParameter(PATH_ID_KEY);
            if (!StringUtils.hasLength(pathId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            } catch (InvalidDefaultRepositoryException e) {
                LOGGER.error(e.getMessage(), e);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }
            List<JsonRepositoryFileEntry> fileListEntry = getFileList(pathId, repository);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("entries", fileListEntry);
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
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
            Audit.audit(request, "API", "MyDriveMgmt", "list", error ? 0 : 1, principal != null ? principal.getUserId() : null, pathId);
        }
    }

    @Secured
    @POST
    @Path("/folderMetadata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String folderMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        String pathId = null;
        boolean error = true;
        RMSUserPrincipal principal = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }

            pathId = req.getParameter(PATH_ID_KEY);
            String lastModifiedTime = req.getParameter(LAST_MODIFIED_TIME_KEY);
            if (!StringUtils.hasLength(pathId) || !StringUtils.hasLength(lastModifiedTime)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            RepoItemMetadata folder = null;
            try (DbSession session = DbSession.newSession()) {

                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
                folder = RepositoryManager.getRepoItem(session, pathId, Long.parseLong(lastModifiedTime), repository.getRepoId());
                if (folder == null) {
                    return new JsonResponse(304, "Folder not modified").toJson();
                }
            } catch (InvalidDefaultRepositoryException e) {
                LOGGER.error(e.getMessage(), e);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }
            List<JsonRepositoryFileEntry> fileListEntry = getFileList(pathId, repository);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("entries", fileListEntry);
            resp.putResult("lastModified", folder.getLastModified().getTime());
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
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
            Audit.audit(request, "API", "MyDriveMgmt", "folderMetadata", error ? 0 : 1, principal != null ? principal.getUserId() : null, pathId);
        }
    }

    private List<JsonRepositoryFileEntry> getFileList(String pathId, DefaultRepositoryTemplate repository)
            throws RepositoryException {
        List<RepositoryContent> fileList = repository.getFileList(pathId);
        List<JsonRepositoryFileEntry> fileListEntry = new ArrayList<>(fileList.size());
        for (RepositoryContent file : fileList) {
            JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
            entry.setFolder(file.isFolder());
            entry.setName(file.getName());
            entry.setPathId(file.getPathId());
            entry.setPathDisplay(file.getPath());
            entry.setLastModified(file.getLastModifiedTime());
            if (!file.isFolder()) {
                entry.setSize(file.getFileSize());
            }
            fileListEntry.add(entry);
        }
        return fileListEntry;
    }

    @Secured
    @GET
    @Path("/uploadTicket")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTicket(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        JsonResponse resp = new JsonResponse("OK");
        resp.putResult("exchange", RabbitMQUtil.MYDRIVE_UPLOAD_EXCHANGE_NAME);
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
        RabbitMQUtil.sendDirectMessage(routingKey, RabbitMQUtil.MYDRIVE_UPLOAD_EXCHANGE_NAME, responseMessage);
        return responseMessage;
    }

    @Secured
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/uploadFile")
    public String upload(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        RestUploadRequest uploadReq = null;
        String parentPathId = null;
        String fileName = null;
        boolean error = true;
        DefaultRepositoryTemplate repository = null;
        boolean userConfirmedFileOverwrite = false;
        try {
            RMSUserPrincipal principal = null;
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }

            DefaultRepositoryManager.checkStorageExceeded(principal);
            uploadReq = RestUploadUtil.parseRestUploadRequest(request);

            JsonRequest req = JsonRequest.fromJson(uploadReq.getJson());
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            parentPathId = req.getParameter(PARENT_PATH_ID_KEY);
            fileName = req.getParameter(NAME_KEY);
            userConfirmedFileOverwrite = Boolean.parseBoolean(req.getParameter(FILE_OVERWRITE));
            if (!StringUtils.hasText(parentPathId) || !StringUtils.hasText(fileName)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (fileName.length() > Constants.MAX_FILE_NAME_LENGTH) {
                return new JsonResponse(4005, "File name cannot exceed " + Constants.MAX_FILE_NAME_LENGTH + " characters").toJson();
            }
            boolean validPath = Validator.isValidDirectoryPath(fileName, uploadReq.getUploadDir());
            if (!validPath) {
                return new JsonResponse(4001, "Invalid filename.").toJson();
            }
            File tmpFile = null;
            @SuppressWarnings("unchecked")
            Map<String, String> source = req.getParameter("source", Map.class);
            if (source != null) {
                String repoId = source.get("repoId");
                String pathId = source.get("pathId");
                if (repoId == null || pathId == null) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                }
                if (repoId.equals(repository.getRepoId())) {
                    return new JsonResponse(5001, "File from myDrive is not allowed to upload").toJson();
                }
                IRepository personalRepo = null;
                try (DbSession session = DbSession.newSession()) {
                    personalRepo = RepositoryFactory.getInstance().getRepository(session, principal, repoId);
                }
                tmpFile = RepositoryFileUtil.downloadFileFromRepo(personalRepo, pathId, null, uploadReq.getUploadDir());
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
            if (NxlFile.isNxl(tmpFile)) {
                return new JsonResponse(403, "Nxl files are not eligible for upload to MyDrive").toJson();
            }
            UploadedFileMetaData metadata = repository.uploadFile(parentPathId, parentPathId, tmpFile.getPath(), false, fileName, null, userConfirmedFileOverwrite);
            JsonResponse resp = new JsonResponse("OK");
            JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
            Date lastModifiedTime = metadata.getLastModifiedTime();
            if (lastModifiedTime != null) {
                entry.setLastModified(lastModifiedTime.getTime());
            }
            entry.setFolder(false);
            entry.setName(fileName);
            entry.setPathDisplay(metadata.getPathDisplay());
            entry.setPathId(metadata.getPathId());
            entry.setSize(tmpFile.length());
            resp.putResult("entry", entry);
            Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(principal);
            resp.putResult(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
            resp.putResult(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
            resp.putResult(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
            error = false;
            return resp.toJson();
        } catch (InvalidFileNameException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid fileName (user ID: {}) in MyDrive: '{}'", userId, fileName, e);
            }
            return new JsonResponse(4001, "Invalid filename.").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (user ID: {}) in MyDrive: '{}'", userId, parentPathId, e);
            }
            return new JsonResponse(404, "Parent folder missing.").toJson();
        } catch (DriveStorageExceededException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Drive Storage Exceeded: {}", e.getStorage());
            }
            return new JsonResponse(6001, "Drive Storage Exceeded.").toJson();
        } catch (FileConflictException e) {
            return new JsonResponse(4002, "File already exists").toJson();
        } catch (SocketTimeoutException e) {
            return new JsonResponse(500, "Socket timeout.").toJson();
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when uploading file to MyDrive (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO Error occurred when uploading file to MyDrive (user ID: {}, fileName: {}): {}", userId, fileName, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "MyDriveMgmt", "uploadFile", !error ? 1 : 0, userId, parentPathId, fileName);
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
        String parentPathId = null;
        RMSUserPrincipal principal = null;
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            parentPathId = req.getParameter(PARENT_PATH_ID_KEY);
            folderName = req.getParameter(NAME_KEY);
            if (StringUtils.hasText(folderName) && folderName.length() > RepoConstants.MAX_FOLDERNAME_LENGTH) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Invalid folderName (user ID: {}) in MyDrive: '{}'", userId, folderName);
                }
                return new JsonResponse(4003, "Folder name length limit of " + RepoConstants.MAX_FOLDERNAME_LENGTH + " characters exceeded").toJson();
            }
            boolean autoRename = Boolean.valueOf(req.getParameter("autorename"));
            if (!StringUtils.hasText(parentPathId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }

            CreateFolderResult result = repository.createFolder(folderName, parentPathId, parentPathId, autoRename);
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
                LOGGER.debug("Invalid folderName (user ID: {}) in MyDrive: '{}'", userId, folderName, e);
            }
            return new JsonResponse(4001, "Invalid folder name").toJson();
        } catch (FileAlreadyExistsException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Folder already exists (user ID: {}) in MyDrive: '{}'", userId, folderName);
            }
            return new JsonResponse(4002, "Folder already exists.").toJson();
        } catch (RepositoryFolderAccessException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent folder missing (user ID: {}) in MyDrive: '{}'", userId, parentPathId, e);
            }
            return new JsonResponse(404, "Parent folder missing.").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "createFolder", error ? 0 : 1, principal != null ? principal.getUserId() : null, folderName);
        }
    }

    @Secured
    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteItem(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String pathId = null;
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        Tenant tenant = AbstractLogin.getDefaultTenant();
        if (tenant == null) {
            return new JsonResponse(400, "Invalid tenant").toJson();
        }
        JsonRequest req = JsonRequest.fromJson(json);
        if (req == null) {
            return new JsonResponse(400, "Missing request.").toJson();
        }
        pathId = req.getParameter(PATH_ID_KEY);
        if (!StringUtils.hasText(pathId)) {
            return new JsonResponse(400, "Missing required parameters").toJson();
        }
        RMSUserPrincipal principal = null;
        if (pathId.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
            LOGGER.error("Unable to process request from MyVault");
            return new JsonResponse(400, "Unable to process request from MyVault").toJson();
        }
        String pathDisplay = pathId;
        // reject request from MyVault
        boolean isFolder = false;
        if (pathId.endsWith("/")) {
            isFolder = true;
            pathDisplay = pathId.substring(0, pathId.length() - 1);
        }
        DefaultRepositoryTemplate repository = null;
        try (DbSession session = DbSession.newSession()) {
            principal = new RMSUserPrincipal(us, tenant);
            repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            DeleteFileMetaData metadata = repository.deleteFile(pathId, pathDisplay);
            if (isFolder) {
                RepositoryFileManager.unmarkFilesUnderFolder(session, repository.getRepoId(), pathId);
            } else {
                RepositoryFileManager.unmarkFilesAsFavorite(session, repository.getRepoId(), Arrays.asList(new JsonRepoFile(pathId, pathDisplay)));
            }
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult(NAME_KEY, metadata.getFileName());
            resp.putResult(PATH_ID_KEY, pathId);
            Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(principal);
            resp.putResult(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
            resp.putResult(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
            resp.putResult(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (ForbiddenOperationException e) {
            return new JsonResponse(403, "Forbidden Operation").toJson();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(404, "File not found").toJson();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "deleteItem", !error ? 1 : 0, userId, pathId);
        }
    }

    @Secured
    @POST
    @Path("/copy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String copy(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        RMSUserPrincipal principal = null;
        String srcPath = null;
        String destPath = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            srcPath = req.getParameter("srcPathId");
            destPath = req.getParameter("destPathId");
            if (!StringUtils.hasText(srcPath) || !StringUtils.hasText(destPath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }
            DefaultRepositoryManager.checkStorageExceeded(principal);
            JsonResponse resp = new JsonResponse("OK");
            Object result = null;
            if (srcPath.endsWith("/")) {
                result = repository.copyFolder(srcPath, destPath);
            } else {
                result = repository.copyFile(srcPath, destPath);
            }
            resp.putResult("details", result);
            Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(principal);
            resp.putResult(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
            resp.putResult(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
            resp.putResult(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();

        } catch (DriveStorageExceededException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Drive Storage Exceeded:{}", e.getStorage());
            }
            return new JsonResponse(6001, "Drive Storage Exceeded.").toJson();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "copy", error ? 0 : 1, principal != null ? principal.getUserId() : null, srcPath, destPath);
        }
    }

    @Secured
    @POST
    @Path("/move")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String move(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        RMSUserPrincipal principal = null;
        String srcPath = null;
        String destPath = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            srcPath = req.getParameter("srcPathId");
            destPath = req.getParameter("destPathId");
            if (!StringUtils.hasText(srcPath) || !StringUtils.hasText(destPath)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            } catch (InvalidDefaultRepositoryException e) {
                LOGGER.error(e.getMessage(), e);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }
            JsonResponse resp = new JsonResponse(200, "OK");
            Object result = null;
            if (srcPath.endsWith("/")) {
                result = repository.moveFolder(srcPath, destPath);
            } else {
                result = repository.moveFile(srcPath, destPath);
            }
            resp.putResult("details", result);
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (ForbiddenOperationException e) {
            return new JsonResponse(403, "Forbidden Operation").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "move", error ? 0 : 1, principal != null ? principal.getUserId() : null, srcPath, destPath);
        }
    }

    @Secured
    @POST
    @Path("/PublicUrl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getPublicUrl(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        JsonResponse resp;
        boolean error = true;
        String pathId = null;
        RMSUserPrincipal principal = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            pathId = req.getParameter(PATH_ID_KEY);

            if (!StringUtils.hasText(pathId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            }
            String url = repository.getPublicUrl(pathId);
            resp = new JsonResponse("OK");
            resp.putResult("url", url);
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "getPublicUrl", error ? 0 : 1, principal != null ? principal.getUserId() : null, pathId);
        }
    }

    @Secured
    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String search(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        DefaultRepositoryTemplate repository = null;
        boolean error = true;
        String searchString = null;
        RMSUserPrincipal principal = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            searchString = req.getParameter("query");
            if (!StringUtils.hasText(searchString)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                // ToDo Once the default provider / repository is finalized need
                // to get
                repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            } catch (InvalidDefaultRepositoryException e) {
                LOGGER.error(e.getMessage(), e);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }

            List<RepositoryContent> results = repository.search(searchString);
            List<JsonRepositoryFileEntry> matches = new ArrayList<>(results.size());
            for (RepositoryContent result : results) {
                JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
                entry.setName(result.getName());
                entry.setLastModified(result.getLastModifiedTime());
                entry.setSize(result.getFileSize());
                entry.setPathId(result.getPathId());
                entry.setPathDisplay(result.getPath());
                entry.setFolder(result.isFolder());
                matches.add(entry);
            }
            JsonResponse data = new JsonResponse("OK");
            data.putResult("matches", matches);
            error = false;
            return data.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            Audit.audit(request, "API", "MyDriveMgmt", "search", error ? 0 : 1, principal != null ? principal.getUserId() : null, searchString);
        }
    }

    @Secured
    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void download(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json)
            throws IOException {
        DefaultRepositoryTemplate repository = null;
        File outputPath = null;
        boolean error = true;
        String pathId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                response.sendError(Status.BAD_REQUEST.getStatusCode());
                return;
            }
            pathId = req.getParameter(PATH_ID_KEY);
            if (!StringUtils.hasText(pathId)) {
                response.sendError(Status.BAD_REQUEST.getStatusCode());
                return;
            }
            int start = 0;
            int length = 0;
            String startParam = req.getParameter("start");
            String lengthParam = req.getParameter("length");
            boolean isPartialDownload = false;
            if (startParam != null && lengthParam != null) {
                start = Integer.parseInt(startParam);
                length = Integer.parseInt(lengthParam);
                if (start < 0 || length < 0) {
                    response.sendError(Status.BAD_REQUEST.getStatusCode());
                    return;
                }
                isPartialDownload = true;
            }
            int end = start + length - 1;
            RMSUserPrincipal principal = null;
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                response.sendError(Status.BAD_REQUEST.getStatusCode());
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

            response.setHeader("x-rms-last-modified", String.valueOf(fileMetadata.getLastModifiedTime()));
            if (fileMetadata.getFileSize() == 0) {
                isPartialDownload = false;
            } else if (start > fileMetadata.getFileSize()) {
                response.sendError(Status.BAD_REQUEST.getStatusCode());
                return;
            } else if (end > fileMetadata.getFileSize()) {
                end = fileMetadata.getFileSize().intValue();
            }
            long contentLength = 0L;
            if (isPartialDownload) {
                byte[] data = repository.downloadPartialFile(pathId, pathId, start, end);
                if (data != null) {
                    contentLength = data.length;
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
                    try (FileInputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            }
            error = false;
        } catch (FileNotFoundException e) {
            if (!response.isCommitted()) {
                response.sendError(Status.NOT_FOUND.getStatusCode(), "File not found.");
            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(Status.BAD_REQUEST.getStatusCode());
            }
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            if (!response.isCommitted()) {
                response.sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode());
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
            Audit.audit(request, "API", "MyDriveMgmt", "download", !error ? 1 : 0, userId, pathId);
        }
    }

    @Secured
    @POST
    @Path("/getUsage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getMyDriveUsage(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        JsonResponse resp;
        boolean error = true;
        RMSUserPrincipal principal = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request.").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            principal = new RMSUserPrincipal(us, tenant);
            resp = new JsonResponse("OK");
            Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(principal);
            resp.putResult(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
            resp.putResult(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
            resp.putResult(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
            resp.putResult(RepoConstants.USER_VAULT_QUOTA, myDriveStatus.get(RepoConstants.USER_VAULT_QUOTA));
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (InvalidDefaultRepositoryException | RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "MyDriveMgmt", "getUsage", error ? 0 : 1, principal != null ? principal.getUserId() : null);
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
                Long totalFileCount = repository.getTotalFileCount(session, repository.getRepoId(), false);
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
            Audit.audit(request, "API", "MyDriveMgmt", "getTotalFileCount", error ? 0 : 1, principal != null ? principal.getUserId() : null);
        }
    }

}
