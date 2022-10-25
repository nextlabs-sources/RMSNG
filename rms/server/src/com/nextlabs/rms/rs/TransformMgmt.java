package com.nextlabs.rms.rs;

import com.google.common.collect.Maps;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.application.ApplicationRepositoryFactory;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.pojos.RMSSpacePojo;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ISystemBucketManager;
import com.nextlabs.rms.service.MyVaultService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.SharedWorkspaceService;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.share.ShareServiceImpl;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.DownloadUtil;
import com.nextlabs.rms.util.MyVaultDownloadUtil;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RestUploadUtil;
import com.nextlabs.rms.util.TransformUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/transform")
public class TransformMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String PERMISSION_DENIED = "Permission Denied.";
    public static final String SOURCE_FILE_IS_NOT_FOUND = "File Not Found.";
    public static final String USER_IS_NOT_A_PROJECT_MEMBER = "User is not a project member";
    public static final String NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_EXTRACT_PERMISSION = "Neither the user is project owner nor the file has EXTRACT permission";
    public static final String NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_SAVE_AS_PERMISSION = "Neither the user is project owner nor the file has SAVE AS permission";
    public static final String USER_IS_NOT_FILE_OWNER = "User is not file owner";
    private static final int NXL_HEADER_START = 0;
    private static final long NXL_HEADER_END = 16383;
    private static final Map<String, List<String>> SOURCE_ST_TO_SUPPORTED_DESTINATION_ST = Maps.newHashMap();

    static {
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.MY_VAULT.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.SHARED_WITH_ME.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.PROJECT.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        for (String externalRepository : TransformUtil.EXTERNAL_REPOSITORIES) {
            SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(externalRepository, Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Constants.TransferSpaceType.LOCAL_DRIVE.name())).collect(Collectors.toList()));
        }
        SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.put(Constants.TransferSpaceType.LOCAL_DRIVE.name(), Stream.concat(TransformUtil.EXTERNAL_REPOSITORIES.stream(), Stream.of(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())).collect(Collectors.toList()));
    }

    /***
     *
     * @param request
     * @param response
     * @param ticket
     * @param clientId
     * @param deviceId
     * @param platformId
     * @param accept
     * @return a json acknowledgement or file-copy as stream
     */
    @Secured
    @POST
    @Path("/transfer")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM })
    public String transferFile(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("deviceId") String deviceId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("Accept") String accept) {
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        String loginTenantId = us.getLoginTenant();
        Tenant loginTenant = AbstractLogin.getDefaultTenant();
        RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
        principal = Validator.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
        User user = us.getUser();
        int userId = user.getId();
        String rmsUrl = HTTPUtil.getInternalURI(request);
        String contentType = request.getContentType();
        Rights[] rights = null;
        boolean error = true;

        if (!StringUtils.hasText(accept)) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), new ValidateException(404, "Accept header is missing")).toJson();
        }

        try (DbSession session = DbSession.newSession()) {
            RMSSpacePojo sourcePojo = new RMSSpacePojo();
            RMSSpacePojo destPojo = new RMSSpacePojo();

            boolean isFileUpload = false;
            boolean isFileDownload = false;
            boolean isOverwrite = false;
            File sourceFile = null;
            Exception exception = null;

            if (MediaType.APPLICATION_JSON.equals(contentType) && accept.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                // download to local drive
                String payload = IOUtils.toString(request.getInputStream());
                JsonRequest req = JsonRequest.fromJson(payload);
                TransformUtil.validateRequest(req);

                TransformUtil.validateSourcePayload(req.getParameter("src", Map.class));
                sourcePojo = req.getParameter("src", RMSSpacePojo.class);
                if (TransformUtil.EXTERNAL_REPOSITORIES.stream().noneMatch(sourcePojo.getSpaceType()::equalsIgnoreCase) && StringUtils.hasText(sourcePojo.getFilePathId())) {
                    sourcePojo.setFilePathId(sourcePojo.getFilePathId().toLowerCase());
                }

                if (Constants.TransferSpaceType.LOCAL_DRIVE.name().equals(sourcePojo.getSpaceType())) {
                    throw new ValidateException(403, "Invalid source space type");
                }

                TransformUtil.validateDestinationPayload(req.getParameter("dest", Map.class));
                destPojo = req.getParameter("dest", RMSSpacePojo.class);
                isFileDownload = true;
            } else if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA) && accept.contains(MediaType.APPLICATION_JSON)) {
                // local drive to other destinations
                RestUploadRequest restUploadRequest = RestUploadUtil.parseRestUploadRequest(request);
                JsonRequest req = JsonRequest.fromJson(restUploadRequest.getJson());
                TransformUtil.validateRequest(req);
                isOverwrite = Boolean.parseBoolean(req.getParameter("overwrite"));

                sourceFile = new File(restUploadRequest.getUploadDir(), restUploadRequest.getFileName());
                if (!"nxl".equalsIgnoreCase(FileUtils.getExtension(sourceFile))) {
                    throw new ValidateException(400, "Source file is not a NXL file");
                }
                Files.copy(restUploadRequest.getFileStream(), sourceFile.toPath());
                sourcePojo = new RMSSpacePojo();
                sourcePojo.setFileName(restUploadRequest.getFileName());
                sourcePojo.setSpaceType(Constants.TransferSpaceType.LOCAL_DRIVE.name());

                TransformUtil.validateDestinationPayload(req.getParameter("dest", Map.class));
                destPojo = req.getParameter("dest", RMSSpacePojo.class);

                isFileUpload = true;
            } else if (MediaType.APPLICATION_JSON.equals(contentType) && MediaType.APPLICATION_JSON.equals(accept)) {
                // copy across other space types
                String payload = IOUtils.toString(request.getInputStream());
                JsonRequest req = JsonRequest.fromJson(payload);
                isOverwrite = Boolean.parseBoolean(req.getParameter("overwrite"));

                TransformUtil.validateSourcePayload(req.getParameter("src", Map.class));
                sourcePojo = req.getParameter("src", RMSSpacePojo.class);
                if (TransformUtil.EXTERNAL_REPOSITORIES.stream().noneMatch(sourcePojo.getSpaceType()::equalsIgnoreCase) && StringUtils.hasText(sourcePojo.getFilePathId())) {
                    sourcePojo.setFilePathId(sourcePojo.getFilePathId().toLowerCase());
                }

                TransformUtil.validateDestinationPayload(req.getParameter("dest", Map.class));
                destPojo = req.getParameter("dest", RMSSpacePojo.class);
            }

            checkSourceSpaceTypeToDestinationSpaceTypeValidity(sourcePojo, destPojo);
            if (!isFileUpload) {
                checkFileExist(sourcePojo, principal, us, loginTenantId, request);
            }
            String sourceSpaceType = sourcePojo.getSpaceType();
            String destSpaceType = destPojo.getSpaceType();
            NxlFile nxlFile = null;

            if (!isFileDownload && destSpaceType.equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) {
                String parentPathId = destPojo.getParentPathId();
                if (!parentPathId.endsWith("/")) {
                    destPojo.setParentPathId(parentPathId + "/");
                }
            }

            if (ServiceProviderType.BOX.name().equalsIgnoreCase(destSpaceType)) {
                throw new ValidateException(403, "BOX as a destination space is not supported");
            }

            checkDestinationRepositoryExists(principal, session, destPojo, destSpaceType);

            if (Constants.TransferSpaceType.MY_VAULT.name().equalsIgnoreCase(sourceSpaceType)) {
                DefaultRepositoryTemplate repository = new MyVaultDownloadUtil().getRepository(principal);
                byte[] header = repository.downloadPartialFile(sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), NXL_HEADER_START, NXL_HEADER_END);
                nxlFile = NxlFile.parse(header);
                rights = getRights(loginTenant, principal, user, nxlFile);
            } else if (Constants.TransferSpaceType.SHARED_WITH_ME.name().equalsIgnoreCase(sourceSpaceType)) {
                ShareServiceImpl shareService = new ShareServiceImpl(sourcePojo.getTransactionId(), principal, request);
                byte[] header = shareService.downloadPartialFile(NXL_HEADER_START, NXL_HEADER_END);
                nxlFile = NxlFile.parse(header);
                rights = getRights(loginTenant, principal, user, nxlFile);

                boolean isRecipient = SharedFileManager.isRecipient(nxlFile.getDuid(), user.getEmail(), sourcePojo.getTransactionId(), session);
                if (!isRecipient) {
                    LOGGER.error("User is not a recipient of the file or file is not found");
                    throw new ValidateException(404, SOURCE_FILE_IS_NOT_FOUND);
                }
                if (!TransformUtil.hasRight(rights, Rights.DOWNLOAD)) {
                    LOGGER.error("File does not have SAVE AS permission");
                    exception = new ValidateException(403, PERMISSION_DENIED);
                }
            } else if (Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name().equalsIgnoreCase(sourceSpaceType)) {
                DefaultRepositoryTemplate repository = EnterpriseWorkspaceService.getRepository(principal, loginTenantId);
                byte[] header = repository.downloadPartialFile(sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), NXL_HEADER_START, NXL_HEADER_END);
                nxlFile = NxlFile.parse(header);
                rights = getRights(loginTenant, principal, user, nxlFile);
                if (TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(destSpaceType)) {
                    boolean isTenantAdmin = UserService.checkTenantAdmin(session, loginTenantId, userId);
                    if (!TransformUtil.hasRight(rights, Rights.DOWNLOAD) && !isTenantAdmin) {
                        LOGGER.error("Neither the user is tenant admin nor the file has SAVE AS permission");
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                }
            } else if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(sourceSpaceType)) {
                SharedWorkspaceService sharedWorkspaceService = new SharedWorkspaceService();
                byte[] header = sharedWorkspaceService.downloadPartialFile(sourcePojo.getSpaceId(), principal, null, sourcePojo.getFilePathId(), NXL_HEADER_START, NXL_HEADER_END);
                nxlFile = NxlFile.parse(header);
                String owner = nxlFile.getOwner();
                String tokenGroupFragment = StringUtils.substringAfter(owner, "@");
                boolean isTenantAdmin = UserService.checkTenantAdmin(session, loginTenantId, userId);
                rights = getRights(loginTenant, principal, user, nxlFile);

                ISystemBucketManager systemBucketManager = new SystemBucketManagerImpl();
                if (systemBucketManager.isSystemBucket(tokenGroupFragment)) {
                    // tenant TG
                    if ((TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equals(destSpaceType)) && (!isTenantAdmin && !TransformUtil.hasRight(rights, Rights.DOWNLOAD))) {
                        LOGGER.error("Neither the user is tenant admin nor the file has SAVE AS permission");
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                } else if (tokenGroupFragment.equals(loginTenant.getName())) {
                    // individual TG
                    boolean isOwner = SharedFileManager.isOwner(rmsUrl, owner, principal);
                    if (!isOwner) {
                        LOGGER.error(USER_IS_NOT_FILE_OWNER);
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                } else {
                    // project TG
                    Membership membership = session.get(Membership.class, owner);
                    if (membership == null) {
                        LOGGER.error("User does not have access to project");
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                    if (exception == null) {
                        Project project = membership.getProject();
                        int projectId = project.getId();
                        boolean isUserProjectMember = ProjectService.checkUserProjectMembership(session, us, projectId, true);
                        if (!isUserProjectMember) {
                            LOGGER.error(USER_IS_NOT_A_PROJECT_MEMBER);
                            exception = new ValidateException(403, PERMISSION_DENIED);
                        }
                        if (exception == null) {
                            boolean isUserProjectOwner = project.getOwner().equalsIgnoreCase(user.getEmail());
                            if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(destSpaceType)) {
                                if (!TransformUtil.hasRight(rights, Rights.DECRYPT) && !isUserProjectOwner) {
                                    LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_EXTRACT_PERMISSION);
                                    exception = new ValidateException(403, PERMISSION_DENIED);
                                }
                            } else if ((TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(destSpaceType)) && !TransformUtil.hasRight(rights, Rights.DOWNLOAD) && !isUserProjectOwner) {
                                LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_SAVE_AS_PERMISSION);
                                exception = new ValidateException(403, PERMISSION_DENIED);
                            }
                        }
                    }
                }
            } else if (Constants.TransferSpaceType.PROJECT.name().equalsIgnoreCase(sourceSpaceType)) {
                int projectId;
                try {
                    projectId = Integer.parseInt(sourcePojo.getSpaceId());
                } catch (NumberFormatException e) {
                    ValidateException validationException = new ValidateException(403, "Invalid Project Id.");
                    validationException.initCause(e);
                    throw validationException;
                }
                Project project = Validator.validateProject(projectId, us);
                sourcePojo.setSpaceRepoName(project.getName());
                boolean isUserProjectMember = ProjectService.checkUserProjectMembership(session, us, projectId, true);
                if (!isUserProjectMember) {
                    LOGGER.error(USER_IS_NOT_A_PROJECT_MEMBER);
                    exception = new ValidateException(403, PERMISSION_DENIED);
                }
                if (exception == null) {
                    DefaultRepositoryTemplate repository = ProjectService.getRepository(principal, projectId);
                    byte[] header = repository.downloadPartialFile(sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), NXL_HEADER_START, NXL_HEADER_END);
                    nxlFile = NxlFile.parse(header);
                    rights = getRights(loginTenant, principal, user, nxlFile);
                    boolean isUserProjectOwner = project.getOwner().equalsIgnoreCase(user.getEmail());
                    if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(destSpaceType)) {
                        if (!TransformUtil.hasRight(rights, Rights.DECRYPT) && !isUserProjectOwner) {
                            LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_EXTRACT_PERMISSION);
                            exception = new ValidateException(403, PERMISSION_DENIED);
                        }
                    } else if ((TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(destSpaceType)) && !TransformUtil.hasRight(rights, Rights.DOWNLOAD) && !isUserProjectOwner) {
                        LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_SAVE_AS_PERMISSION);
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                }
            } else if (TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(sourceSpaceType::equalsIgnoreCase)) {
                IRepository repository;
                try {
                    repository = RepositoryFactory.getInstance().getRepository(session, principal, sourcePojo.getSpaceId());
                } catch (RepositoryException e) {
                    ValidateException spaceIdException = new ValidateException(404, "Invalid spaceId");
                    spaceIdException.initCause(e);
                    throw spaceIdException;
                }
                byte[] header = repository.downloadPartialFile(sourcePojo.getFilePathId(), sourcePojo.getFileName(), NXL_HEADER_START, NXL_HEADER_END);
                nxlFile = NxlFile.parse(header);
                rights = getRights(loginTenant, principal, user, nxlFile);
                String owner = nxlFile.getOwner();
                String tokenGroupFragment = StringUtils.substringAfter(owner, "@");

                ISystemBucketManager systemBucketManager = new SystemBucketManagerImpl();
                if (systemBucketManager.isSystemBucket(tokenGroupFragment)) {
                    // tenant TG
                    boolean isTenantAdmin = UserService.checkTenantAdmin(session, loginTenantId, userId);
                    if (!isTenantAdmin && !TransformUtil.hasRight(rights, Rights.DOWNLOAD)) {
                        LOGGER.error("Neither the user is tenant admin nor the file has SAVE AS permission");
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                } else if (tokenGroupFragment.equals(loginTenant.getName())) {
                    // individual TG
                    boolean isOwner = SharedFileManager.isOwner(rmsUrl, owner, principal);
                    if (!isOwner) {
                        LOGGER.error(USER_IS_NOT_FILE_OWNER);
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                } else {
                    // project TG
                    Membership membership = session.get(Membership.class, owner);
                    if (membership == null) {
                        LOGGER.error("User does not have access to project");
                        exception = new ValidateException(403, PERMISSION_DENIED);
                    }
                    if (exception == null) {
                        Project project = membership.getProject();
                        int projectId = project.getId();
                        boolean isUserProjectMember = ProjectService.checkUserProjectMembership(session, us, projectId, true);
                        if (!isUserProjectMember) {
                            LOGGER.error(USER_IS_NOT_A_PROJECT_MEMBER);
                            exception = new ValidateException(403, PERMISSION_DENIED);
                        }
                        if (exception == null) {
                            boolean isUserProjectOwner = project.getOwner().equalsIgnoreCase(user.getEmail());
                            if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(destSpaceType)) {
                                if (!TransformUtil.hasRight(rights, Rights.DECRYPT) && !isUserProjectOwner) {
                                    LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_EXTRACT_PERMISSION);
                                    exception = new ValidateException(403, PERMISSION_DENIED);
                                }
                            } else if ((TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(destSpaceType)) && !TransformUtil.hasRight(rights, Rights.DOWNLOAD) && !isUserProjectOwner) {
                                LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_SAVE_AS_PERMISSION);
                                exception = new ValidateException(403, PERMISSION_DENIED);
                            }
                        }
                    }
                }
            } else if (Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(sourceSpaceType)) {
                try (InputStream fis = new FileInputStream(sourceFile.getAbsoluteFile())) {
                    nxlFile = NxlFile.parse(fis);
                    rights = getRights(loginTenant, principal, user, nxlFile);

                    String owner = nxlFile.getOwner();
                    String tokenGroupFragment = StringUtils.substringAfter(owner, "@");

                    ISystemBucketManager systemBucketManager = new SystemBucketManagerImpl();
                    if (systemBucketManager.isSystemBucket(tokenGroupFragment)) {
                        // tenant TG
                        // no checks to perform
                    } else if (tokenGroupFragment.equals(loginTenant.getName())) {
                        // individual TG
                        boolean isOwner = SharedFileManager.isOwner(rmsUrl, owner, principal);
                        if (!isOwner) {
                            LOGGER.error(USER_IS_NOT_FILE_OWNER);
                            exception = new ValidateException(403, PERMISSION_DENIED);
                        }
                    } else {
                        // project TG
                        Membership membership = session.get(Membership.class, owner);
                        Project project = membership.getProject();
                        int projectId = project.getId();
                        boolean isUserProjectMember = ProjectService.checkUserProjectMembership(session, us, projectId, true);
                        if (!isUserProjectMember) {
                            LOGGER.error(USER_IS_NOT_A_PROJECT_MEMBER);
                            exception = new ValidateException(403, PERMISSION_DENIED);
                        }
                        if (exception == null) {
                            boolean isUserProjectOwner = project.getOwner().equalsIgnoreCase(user.getEmail());
                            if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(destSpaceType)) {
                                if (!TransformUtil.hasRight(rights, Rights.DECRYPT) && !isUserProjectOwner) {
                                    LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_EXTRACT_PERMISSION);
                                    exception = new ValidateException(403, PERMISSION_DENIED);
                                }
                            } else if ((TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase) || Constants.TransferSpaceType.LOCAL_DRIVE.name().equalsIgnoreCase(destSpaceType)) && !TransformUtil.hasRight(rights, Rights.DOWNLOAD) && !isUserProjectOwner) {
                                LOGGER.error(NEITHER_THE_USER_IS_PROJECT_OWNER_NOR_THE_FILE_HAS_SAVE_AS_PERMISSION);
                                exception = new ValidateException(403, PERMISSION_DENIED);
                            }
                        }
                    }
                }
            }

            if (nxlFile != null) {
                if (!UserService.isAPIUserSession(us) && rights != null && !TransformUtil.hasRight(rights, Rights.VIEW)) {
                    LOGGER.error("File doesn't have VIEW right");
                    exception = new ValidateException(403, PERMISSION_DENIED);
                }
                if (!(Constants.TransferSpaceType.MY_VAULT.name().equalsIgnoreCase(sourceSpaceType) && SharedFileManager.isOwner(rmsUrl, nxlFile.getOwner(), principal))) {
                    boolean isFileValid = isFileValid(nxlFile);
                    if (!isFileValid) {
                        exception = new ValidateException(403, "File expired or set for future validation");
                    }
                }
                FileInfo fileInfo = DecryptUtil.getInfo(nxlFile, null);
                sourcePojo.setFileName(fileInfo.getFileName());
            }

            TransformUtil transformUtil = new TransformUtil(principal, loginTenantId, sourcePojo, destPojo, rmsUrl, request, rights, isFileUpload, isOverwrite, session);
            if (exception != null) {
                transformUtil.setDUID(nxlFile.getDuid());
                transformUtil.setOwnerMembership(nxlFile.getOwner());
                transformUtil.updateAuditLog(Constants.AccessResult.DENY);
                throw exception;
            }

            if (isFileUpload) {
                transformUtil.setSrcDownloadNxlFile(sourceFile);
            }
            transformUtil.transferFile();

            if (isFileDownload) {
                File file = transformUtil.getDestUploadNxlFile();
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(file.getName()));
                error = false;
                try (InputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, response.getOutputStream());
                    response.flushBuffer();
                    return null;
                }
            } else {
                String resp = transformUtil.getResponse();
                error = false;
                return new JsonResponse(resp).toJson();
            }
        } catch (RuntimeException e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } catch (Exception e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "TransformMgmt", "transferFile", error ? 0 : 1, clientId, platformId);
        }
    }

    private void checkDestinationRepositoryExists(RMSUserPrincipal principal, DbSession session, RMSSpacePojo destPojo,
        String destSpaceType) {
        try {
            if (TransformUtil.EXTERNAL_REPOSITORIES.stream().anyMatch(destSpaceType::equalsIgnoreCase)) {
                RepositoryFactory.getInstance().getRepository(session, principal, destPojo.getSpaceId());
            } else if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(destSpaceType)) {
                ApplicationRepositoryFactory.getInstance().getRepository(session, principal, destPojo.getSpaceId());
            }
        } catch (RepositoryException | ApplicationRepositoryException e) {
            ValidateException spaceIdException = new ValidateException(404, "Invalid spaceId");
            spaceIdException.initCause(e);
            throw spaceIdException;
        }
    }

    private void checkSourceSpaceTypeToDestinationSpaceTypeValidity(RMSSpacePojo sourcePojo, RMSSpacePojo destPojo) {
        String sourceSpaceType = sourcePojo.getSpaceType();
        String destSpaceType = destPojo.getSpaceType();
        List<String> destinationSpaceTypes = SOURCE_ST_TO_SUPPORTED_DESTINATION_ST.get(sourceSpaceType);
        if (destinationSpaceTypes == null) {
            throw new ValidateException(400, "Invalid space type in source payload");
        }
        if (!destinationSpaceTypes.contains(destSpaceType)) {
            throw new ValidateException(500, "Source space type and destination space type combination is not supported.");
        }
    }

    private void checkFileExist(RMSSpacePojo sourcePojo, RMSUserPrincipal principal, UserSession us, String tenantId,
        HttpServletRequest request)
            throws InvalidDefaultRepositoryException, RepositoryException, ApplicationRepositoryException,
            RMSException {
        boolean isFileExists = true;
        if (Constants.TransferSpaceType.MY_VAULT.name().equalsIgnoreCase(sourcePojo.getSpaceType())) {
            isFileExists = MyVaultService.checkFileExists(sourcePojo.getFilePathId(), us);
        } else if (Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name().equalsIgnoreCase(sourcePojo.getSpaceType())) {
            isFileExists = EnterpriseWorkspaceService.checkFileExists(us, tenantId, sourcePojo.getFilePathId());
        } else if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(sourcePojo.getSpaceType())) {
            SharedWorkspaceService sharedWorkspaceService = new SharedWorkspaceService();
            isFileExists = sharedWorkspaceService.checkIfFileExists(principal, sourcePojo.getSpaceId(), sourcePojo.getFilePathId());
        } else if (Constants.TransferSpaceType.PROJECT.name().equalsIgnoreCase(sourcePojo.getSpaceType())) {
            int projectId;
            try {
                projectId = Integer.parseInt(sourcePojo.getSpaceId());
            } catch (NumberFormatException e) {
                ValidateException validationException = new ValidateException(403, "Invalid Project Id.");
                validationException.initCause(e);
                throw validationException;
            }
            Validator.validateProject(projectId, us);
            isFileExists = ProjectService.checkFileExists(us, tenantId, projectId, sourcePojo.getFilePathId());
        } else if (Constants.TransferSpaceType.SHARED_WITH_ME.name().equalsIgnoreCase(sourcePojo.getSpaceType())) {
            if (SharedFileManager.getSharingTransactionByTransactionId(sourcePojo.getTransactionId()) == null) {
                isFileExists = false;
            } else {
                ShareServiceImpl shareService = new ShareServiceImpl(sourcePojo.getTransactionId(), principal, request);
                isFileExists = shareService.checkFileExists();
            }
        }

        if (!isFileExists) {
            throw new ValidateException(404, SOURCE_FILE_IS_NOT_FOUND);
        }
    }

    private Rights[] getRights(Tenant loginTenant, RMSUserPrincipal principal, User user, NxlFile nxlFile)
            throws NxlException, GeneralSecurityException, IOException {
        Constants.ProtectionType protectionType = nxlFile.getProtectionType();
        EvalResponse evalResponse;
        if (Constants.ProtectionType.ADHOC == protectionType) {
            evalResponse = new DownloadUtil().getAdhocEvaluationResponse(nxlFile);
        } else {
            com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
            FileInfo fileInfo = DecryptUtil.getInfo(nxlFile, null);
            List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileInfo.getFileName(), nxlFile.getOwner(), loginTenant.getName(), userEval, DecryptUtil.getTags(nxlFile, null));
            evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
        }
        return evalResponse.getRights();
    }

    private boolean isFileValid(NxlFile nxlFile)
            throws NxlException, GeneralSecurityException, IOException {
        if (Constants.ProtectionType.ADHOC != nxlFile.getProtectionType()) {
            return true;
        }
        FilePolicy filePolicy = DecryptUtil.getFilePolicy(nxlFile, null);
        boolean isExpired = AdhocEvalAdapter.isFileExpired(filePolicy);
        boolean isNotYetValid = AdhocEvalAdapter.isNotYetValid(filePolicy);
        return !isExpired && !isNotYetValid;
    }

}
