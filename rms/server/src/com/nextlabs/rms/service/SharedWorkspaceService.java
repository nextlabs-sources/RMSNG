package com.nextlabs.rms.service;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonEnterpriseSpaceMember;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonSharedWorkspaceFileInfo;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.UnsupportedNxlVersionException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.application.ApplicationRepositoryContent;
import com.nextlabs.rms.application.ApplicationRepositoryFactory;
import com.nextlabs.rms.application.FileUploadMetadata;
import com.nextlabs.rms.application.IApplicationRepository;
import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.MembershipException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ExternalRepositoryNxl;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.pojos.RMSSpacePojo;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.repository.SharedWorkspaceRepoManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.rs.TenantMgmt;
import com.nextlabs.rms.rs.TokenMgmt;
import com.nextlabs.rms.rs.dto.ProtectSharedWorkspaceDTO;
import com.nextlabs.rms.rs.dto.UploadSharedWorkspaceDTO;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.EnterpriseWorkspaceDownloadUtil;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;
import com.nextlabs.rms.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service class for handling requests from SharedWorkspaceMgmt
 */
public class SharedWorkspaceService {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String ERROR_REPO_DOES_NOT_EXIST = "Repository does not exist";
    private static final String ERROR_INVALID_REPO_ID = "Invalid repoId.";
    private static final String ERROR_INVALID_FILE = "Invalid file.";
    private static final String ERROR_INVALID_NXL_FILE = "Invalid file.";
    private static final String ERROR_FILE_NOT_FOUND = "File not found";
    private static final String ERROR_INVALID_DOWNLOAD_TYPE = "Missing/Wrong download type.";

    /**
     * Gets list of files for a given repoId and path, with an optional searchString
     * parameter as well
     *
     * @param path         folder path in repoId
     * @param searchString optional string with the folder/file name to be searched
     *                     at root level
     * @param repoId       id of the application repo configured
     * @param request
     * @param hideFiles
     * @return
     * @throws RepositoryException
     * @throws ApplicationRepositoryException
     */
    public List<ApplicationRepositoryContent> listFiles(String path, String searchString, String repoId,
        HttpServletRequest request, String hideFiles) throws RepositoryException, ApplicationRepositoryException {

        RMSUserPrincipal userPrincipal = null;
        try (DbSession session = DbSession.newSession()) {
            userPrincipal = AuthManager.authenticate(session, request);
        }
        IApplicationRepository repository = getRepository(userPrincipal, repoId);

        List<ApplicationRepositoryContent> fileList = null;
        if (StringUtils.hasText(searchString)) {
            fileList = repository.search(searchString);
        } else {
            fileList = repository.getFileList(path, new FilterOptions(hideFiles));
        }
        return fileList;
    }

    /**
     * Downloads a file from an application account for a given repoId and path, to
     * the folder path defined as outputPath
     *
     * @param us
     * @param repoId       id of the application repo configured
     * @param principal
     * @param path
     * @param filepath         path of file to be downloaded in the external repository
     * @param outputPath   output folder to which the file needs to be downloaded
     * @param fileId
     * @param downloadType
     * @return
     * @throws ApplicationRepositoryException
     * @throws RepositoryException
     * @throws OneDriveServiceException
     * @throws IOException
     * @throws UnauthorizedOperationException
     * @throws TokenGroupNotFoundException
     * @throws GeneralSecurityException
     * @throws MembershipException
     * @throws NxlException
     * @throws InvalidDefaultRepositoryException
     */
    public File downloadfile(UserSession us, String repoId, RMSUserPrincipal principal, String path, String filepath,
        String outputPath, String fileId, DownloadType downloadType) throws ApplicationRepositoryException,
            RepositoryException, OneDriveServiceException, IOException, InvalidDefaultRepositoryException, NxlException,
            MembershipException, GeneralSecurityException, TokenGroupNotFoundException, UnauthorizedOperationException {
        IApplicationRepository repository = getRepository(principal, repoId);
        return getDownloadedFile(us, repository, path, filepath, outputPath, fileId, downloadType, repoId);
    }

    /**
     * Protects a file in place , for a file in application repository and returns
     * the metadata of the uploaded file
     *
     * @param protectSharedWorkspaceDTO
     * @param principal
     * @param url
     * @param fileId
     * @return
     * @throws RepositoryException
     * @throws OneDriveServiceException
     * @throws ApplicationRepositoryException
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws NxlException
     * @throws TokenGroupNotFoundException
     * @throws UnauthorizedOperationException
     * @throws MembershipException
     * @throws InvalidDefaultRepositoryException
     */
    public FileUploadMetadata protectInPlace(UserSession us, ProtectSharedWorkspaceDTO protectSharedWorkspaceDTO,
        RMSUserPrincipal principal, String url, String fileId) throws ApplicationRepositoryException,
            RepositoryException, IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException,
            InvalidDefaultRepositoryException, MembershipException, UnauthorizedOperationException {

        IApplicationRepository repository = getRepository(principal, protectSharedWorkspaceDTO.getRepoId());
        File file = getDownloadedFile(us, repository, null, protectSharedWorkspaceDTO.getPath(), protectSharedWorkspaceDTO.getOutputPath(), fileId, DownloadType.FOR_VIEWER, null);

        File outputFile = new File(protectSharedWorkspaceDTO.getOutputPath(), file.getName() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        String membership = EnterpriseWorkspaceService.getMembershipByTenantName(principal, principal.getTenantName());

        try (OutputStream os = new FileOutputStream(outputFile)) {
            try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), url, membership, protectSharedWorkspaceDTO.getRights(), protectSharedWorkspaceDTO.getWatermark(), protectSharedWorkspaceDTO.getExpiry(), protectSharedWorkspaceDTO.getTags(), file, os, protectSharedWorkspaceDTO.getProtectionType()))) {

                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), membership, principal.getUserId(), Operations.PROTECT, principal.getDeviceId(), principal.getPlatformId(), protectSharedWorkspaceDTO.getRepoId(), file.getPath(), outputFile.getName(), file.getPath(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.SHAREDWS);
                RemoteLoggingMgmt.saveActivityLog(activity);

                if (!protectSharedWorkspaceDTO.isUserConfirmedFileOverwrite() && checkIfFileExists(principal, protectSharedWorkspaceDTO.getRepoId(), protectSharedWorkspaceDTO.getPath() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
                    throw new ValidateException(4001, "File already exists");
                }

                FileUploadMetadata fileMetadata = uploadFile(repository, protectSharedWorkspaceDTO.getPath(), outputFile.getPath(), protectSharedWorkspaceDTO.isUserConfirmedFileOverwrite(), outputFile.getName());
                SharedFileDAO.updateExternalNxlDBProtect(principal.getUserId(), protectSharedWorkspaceDTO.getRepoId(), nxl.getDuid(), nxl.getOwner(), Rights.toInt(protectSharedWorkspaceDTO.getRights()), outputFile.getName(), fileMetadata.getPathDisplay(), fileMetadata.getSize());

                RemoteLoggingMgmt.Activity uploadActivity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), membership, principal.getUserId(), Operations.UPLOAD_NORMAL, principal.getDeviceId(), principal.getPlatformId(), protectSharedWorkspaceDTO.getRepoId(), file.getPath(), outputFile.getName(), file.getPath(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.SHAREDWS);
                RemoteLoggingMgmt.saveActivityLog(uploadActivity);

                return fileMetadata;
            }
        }

    }

    /**
     * Partially downloads a file from an application account for a given repoId and
     * path, based on the specified start and end indices
     *
     * @param repoId
     * @param principal
     * @param fileId
     * @param filePath
     * @param start
     * @param end
     * @return
     * @throws ApplicationRepositoryException
     * @throws RepositoryException
     * @throws IOException
     */
    public byte[] downloadPartialFile(String repoId, RMSUserPrincipal principal, String fileId, String filePath,
        long start, long end) throws ApplicationRepositoryException, RepositoryException, IOException {
        IApplicationRepository repository = getRepository(principal, repoId);
        // TODO need to complete activity logging for nxl files
        return getPartialDownloadedFile(repository, filePath, fileId, start, end);
    }

    /**
     * Gets the metadata info for a specified nxl file from an application account
     * for a given repoId and path. Will throw exception for non-nxl files
     *
     * @param us
     * @param principal
     * @param filePath
     * @param request
     * @param repoId
     * @param fileId
     * @return
     * @throws IOException
     * @throws ApplicationRepositoryException
     * @throws RepositoryException
     */
    public JsonSharedWorkspaceFileInfo getFileInfo(UserSession us, RMSUserPrincipal principal, String filePath,
        HttpServletRequest request, String repoId, String fileId)
            throws IOException, ApplicationRepositoryException, RepositoryException {
        try (DbSession session = DbSession.newSession()) {
            User user = us.getUser();
            com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(principal.getUserId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
            IApplicationRepository repository = getRepository(principal, repoId);
            byte[] getPartialDownloadedFile = getPartialDownloadedFile(repository, filePath, fileId, 0L, NxlFile.COMPLETE_HEADER_SIZE - 1L);

            ExternalRepositoryNxl file = null;
            try (NxlFile metaData = NxlFile.parse(getPartialDownloadedFile)) {

                file = SharedWorkspaceRepoManager.getExternalItem(repoId, metaData.getDuid(), session);
                if (file == null) {
                    LOGGER.error(ERROR_INVALID_NXL_FILE);
                    throw new ValidateException(5001, ERROR_INVALID_NXL_FILE);
                }
                return evaluatePolicyToGetSharedWorkspaceFileInfo(metaData, file, userEval, session, filePath);

            } catch (IOException | NxlException e) {
                LOGGER.error("Error occurred while parsing the nxl file", e.getMessage(), e);
                ValidateException ve = new ValidateException(5001, ERROR_INVALID_FILE);
                ve.initCause(e);
                throw ve;
            }
        }
    }

    /**
     * Check if file exists in the external application repository as identified by
     * repoId and filePath
     *
     * @param principal
     * @param repoId
     * @param filePath
     * @return
     * @throws ApplicationRepositoryException
     * @throws RepositoryException
     */
    public boolean checkIfFileExists(RMSUserPrincipal principal, String repoId, String filePath)
            throws ApplicationRepositoryException, RepositoryException {
        IApplicationRepository repository = getRepository(principal, repoId);
        boolean fileExists;
        try {
            fileExists = repository.checkIfFileExists(filePath);
        } catch (FileNotFoundException e) {
            LOGGER.error(ERROR_FILE_NOT_FOUND, e.getMessage(), e);
            fileExists = false;
        }
        return fileExists;
    }

    public ApplicationRepositoryContent getFileMetadata(HttpServletRequest request, String repoId, String path)
            throws RepositoryException, ApplicationRepositoryException {
        RMSUserPrincipal userPrincipal;
        try (DbSession session = DbSession.newSession()) {
            userPrincipal = AuthManager.authenticate(session, request);
        }
        try {
            IApplicationRepository repository = getRepository(userPrincipal, repoId);
            return repository.getFileMetadata(path);
        } catch (RepositoryException e) {
            LOGGER.error("getFileMetadata invoke error with:" + e.getMessage());
            if (e.getCause() instanceof OneDriveServiceException) {
                OneDriveServiceException oe = (OneDriveServiceException)e.getCause();
                ValidateException ve = new ValidateException(oe.getStatusCode(), oe.getMessage());
                ve.initCause(e);
                throw ve;
            }
            if (e instanceof FileNotFoundException) {
                ValidateException ve = new ValidateException(404, "File not found.");
                ve.initCause(e);
                throw ve;
            }
        }
        return null;
    }

    private JsonSharedWorkspaceFileInfo evaluatePolicyToGetSharedWorkspaceFileInfo(NxlFile metaData,
        ExternalRepositoryNxl file, com.nextlabs.rms.eval.User user, DbSession session, String filePath) {
        Map<String, String[]> tags = null;
        Rights[] rights = null;
        JsonExpiry expiry = new JsonExpiry();
        String[] rightsList = null;
        int protectionType = -1;
        try {
            String membership = metaData.getOwner();
            tags = DecryptUtil.getTags(metaData, null);
            FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
            List<Policy> adhocPolicies = policy != null ? policy.getPolicies() : null;
            String tokenGroupName = StringUtils.substringAfter(membership, "@");
            if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                rights = Rights.fromInt(file.getPermissions());
                expiry = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                protectionType = ProtectionType.ADHOC.ordinal();
            } else if (EvaluationAdapterFactory.isInitialized()) {
                protectionType = ProtectionType.CENTRAL.ordinal();
                Tenant parentTenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(file.getFileName(), membership, parentTenant.getName(), user, tags);
                EvalResponse evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                rights = evalResponse.getRights();
            }
            rightsList = SharedFileManager.toString(rights);

        } catch (GeneralSecurityException | IOException | NxlException e) {
            LOGGER.error("Error occurred while getting the tags", e.getMessage(), e);
            ValidateException ve = new ValidateException(5001, ERROR_INVALID_NXL_FILE);
            ve.initCause(e);
            throw ve;
        }
        return setFileInfoDetails(rightsList, expiry, tags, file, filePath, protectionType);
    }

    private JsonSharedWorkspaceFileInfo setFileInfoDetails(String[] rightsList, JsonExpiry expiry,
        Map<String, String[]> tags, ExternalRepositoryNxl file, String filePath, int protectionType) {
        JsonSharedWorkspaceFileInfo fileInfo = new JsonSharedWorkspaceFileInfo();
        fileInfo.setRights(rightsList);
        fileInfo.setExpiry(expiry);
        fileInfo.setProtectedFile(true);
        fileInfo.setTags(tags);
        fileInfo.setName(file.getFileName());
        fileInfo.setPathId(file.getFilePath());
        fileInfo.setPath(filePath);
        fileInfo.setProtectionType(protectionType);
        fileInfo.setLastModified(file.getLastModified().getTime());
        fileInfo.setCreationTime(file.getCreationTime().getTime());
        fileInfo.setSize(file.getSize());
        String fileExt;
        JsonEnterpriseSpaceMember fileUser = null;
        if (filePath.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            String filePathNoNxl = filePath.substring(0, filePath.lastIndexOf('.'));
            fileExt = StringUtils.substringAfterLast(filePathNoNxl, ".");
        } else {
            fileExt = StringUtils.substringAfterLast(filePath, ".");
        }
        User uploadedByUser = file.getUser();
        if (uploadedByUser != null) {
            fileUser = new JsonEnterpriseSpaceMember();
            fileUser.setUserId(uploadedByUser.getId());
            fileUser.setDisplayName(uploadedByUser.getDisplayName());
            fileUser.setEmail(uploadedByUser.getEmail());
        }
        fileInfo.setFileType(fileExt);
        fileInfo.setUploadedBy(fileUser);
        fileInfo.setLastModifiedUser(fileUser);
        return fileInfo;
    }

    private File getDownloadedFile(UserSession us, IApplicationRepository repository, String path, String filePath,
        String outputPath, String fileId, DownloadType downloadType, String repoID)
            throws ApplicationRepositoryException, RepositoryException, IOException, InvalidDefaultRepositoryException,
            NxlException, MembershipException, GeneralSecurityException, TokenGroupNotFoundException,
            UnauthorizedOperationException {

        RMSUserPrincipal principal;
        String loginTenantId;

        loginTenantId = us.getLoginTenant();
        Tenant loginTenant = getTenant(loginTenantId);
        principal = new RMSUserPrincipal(us, loginTenant);
        File outputPath2;
        outputPath2 = RepositoryFileUtil.getTempOutputFolder();

        File downloadedFile = null;
        try {
            switch (downloadType) {
                case FOR_VIEWER:
                case OFFLINE:
                    downloadedFile = repository.getFile(fileId, filePath, outputPath);
                    break;
                case NORMAL:
                    downloadedFile = downloadCopy(fileId, principal, loginTenantId, us, filePath, path, outputPath2, repoID);
                    break;

                default:
                    throw new ValidateException(400, ERROR_INVALID_DOWNLOAD_TYPE);
            }

            // TODO need to complete activity logging for nxl files after deriving Operation
            // based on download type

        } catch (OneDriveServiceException oe) {
            LOGGER.error(ERROR_FILE_NOT_FOUND);
            ValidateException ve = new ValidateException(oe.getStatusCode(), oe.getMessage());
            ve.initCause(oe);
            throw ve;
        }
        return downloadedFile;
    }

    public static Tenant getTenant(String loginTenantId) {
        try (DbSession session = DbSession.newSession()) {
            return session.get(Tenant.class, loginTenantId);
        }
    }

    public File downloadCopy(String fileId, RMSUserPrincipal principal, String loginTenantId, UserSession us,
        String filePath, String path, File outputPath, String repoID)
            throws RepositoryException, InvalidDefaultRepositoryException, IOException, NxlException,
            MembershipException, GeneralSecurityException, TokenGroupNotFoundException, UnauthorizedOperationException,
            OneDriveServiceException, ApplicationRepositoryException {

        EnterpriseWorkspaceDownloadUtil util = new EnterpriseWorkspaceDownloadUtil();
        Map<String, String[]> tags;
        String watermark;
        JsonExpiry validity;
        String originalMembership;
        Membership ownerMembership;
        boolean isAdhocPolicy = false;
        Rights[] rightsList;
        File output;
        String duid;

        IApplicationRepository repository = getRepository(principal, repoID);
        File input = repository.getFile(filePath, filePath, outputPath.getPath());
        String fileName = input.getName();
        Tenant loginTenant = EnterpriseWorkspaceService.getTenant(loginTenantId);

        try (InputStream is = new FileInputStream(input); NxlFile nxl = NxlFile.parse(is)) {

            originalMembership = nxl.getOwner();
            ownerMembership = Validator.validateMembership(originalMembership);
            duid = nxl.getDuid();
            tags = DecryptUtil.getTags(nxl, null);
            EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxl);
            rightsList = evalResponse.getRights();
            if (rightsList.length > 0) {
                isAdhocPolicy = true;
            } else {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(principal.getIpAddress()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(fileName, originalMembership, loginTenant.getName(), userEval, tags);
                evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                rightsList = evalResponse.getRights();
            }
            watermark = evalResponse.getEffectiveWatermark();
            FilePolicy policy = DecryptUtil.getFilePolicy(nxl, null);
            validity = util.getValidity(policy);
            if (ArrayUtils.contains(rightsList, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                String tenantName = AbstractLogin.getDefaultTenant().getName();
                watermark = WatermarkConfigManager.getWaterMarkText(path, tenantName, principal.getTicket(), principal.getPlatformId(), String.valueOf(principal.getUserId()), principal.getClientId());
            }
            output = util.reEncryptDocument(principal.getUserId(), principal.getTicket(), principal.getClientId(), duid, principal, rightsList, ownerMembership.getName(), watermark, validity, tags, path, input, principal.getPlatformId(), loginTenantId, isAdhocPolicy);
        }
        return output;
    }

    private byte[] getPartialDownloadedFile(IApplicationRepository repository, String filePath, String fileId,
        long start, long end) throws ApplicationRepositoryException, RepositoryException, IOException {
        byte[] downloadedFile = null;
        try {
            downloadedFile = repository.downloadPartialFile(fileId, filePath, start, end);
        } catch (OneDriveServiceException oe) {
            LOGGER.error(ERROR_FILE_NOT_FOUND);
            ValidateException ve = new ValidateException(oe.getStatusCode(), oe.getMessage());
            ve.initCause(oe);
            throw ve;
        }
        return downloadedFile;
    }

    private FileUploadMetadata uploadFile(IApplicationRepository repository, String remoteFile, String localFile,
        boolean userConfirmedFileOverwrite, String fileName)
            throws ApplicationRepositoryException, ValidateException {
        String remoteFolder = remoteFile != null ? remoteFile.substring(0, remoteFile.lastIndexOf('/') + 1) : null;
        FileUploadMetadata fileMetadata = null;
        try {
            fileMetadata = repository.uploadFile(repository.getCurrentFolderId(remoteFile), remoteFolder, localFile, userConfirmedFileOverwrite, fileName, null);
        } catch (RepositoryException re) {
            if (re.getCause() instanceof OneDriveServiceException) {
                OneDriveServiceException odse = (OneDriveServiceException)re.getCause();
                if (odse.getStatusCode() == 400) {
                    ValidateException ve = new ValidateException(400, odse.getMessage());
                    ve.initCause(re);
                    throw ve;
                } else {
                    ValidateException ve = new ValidateException(odse.getStatusCode(), odse.getMessage());
                    ve.initCause(re);
                    throw ve;
                }
            }
            if (re instanceof FileNotFoundException) {
                ValidateException ve = new ValidateException(404, "Parent folder not found.");
                ve.initCause(re);
                throw ve;
            }
            LOGGER.error("Upload failed - File already exists");
            ValidateException ve = new ValidateException(4002, "File already exists");
            ve.initCause(re);
            throw ve;
        }
        return fileMetadata;
    }

    private IApplicationRepository getRepository(RMSUserPrincipal principal, String repoId) {
        IApplicationRepository repository;
        try (DbSession session = DbSession.newSession()) {
            repository = ApplicationRepositoryFactory.getInstance().getRepository(session, principal, repoId);
        } catch (RepositoryException | ApplicationRepositoryException re) {
            LOGGER.error(ERROR_REPO_DOES_NOT_EXIST);
            ValidateException ve = new ValidateException(404, ERROR_INVALID_REPO_ID);
            ve.initCause(re);
            throw ve;
        }
        if (repository == null) {
            LOGGER.error(ERROR_REPO_DOES_NOT_EXIST);
            throw new ValidateException(404, ERROR_INVALID_REPO_ID);
        }
        return repository;
    }

    public FileUploadMetadata upload(UploadSharedWorkspaceDTO swsDTO, RMSUserPrincipal up, RestUploadRequest uploadReq,
        UserSession us, HttpServletRequest request)
            throws ApplicationRepositoryException, IOException, GeneralSecurityException, RepositoryException {
        String uploader = null;
        Rights[] rights = null;
        String duid = null;
        String membershipWorkspace;
        boolean isNxl = false;
        boolean isAdhocPolicy = false;
        String createdBy = null;

        EnterpriseWorkspaceDownloadUtil util = new EnterpriseWorkspaceDownloadUtil();

        try (InputStream is = new FileInputStream(swsDTO.getFile()); NxlFile nxlMetadata = NxlFile.parse(is)) {
            if (nxlMetadata.getContentLength() == 0) {
                throw new ValidateException(5005, "Empty files are not allowed to be uploaded.");
            }
            uploader = nxlMetadata.getOwner();
            duid = nxlMetadata.getDuid();
            if (StringUtils.hasText(uploader)) {
                isNxl = true;
                createdBy = util.getUserId(uploader);
            }

            Map<String, String[]> tagMap = DecryptUtil.getTags(nxlMetadata, null);
            membershipWorkspace = EnterpriseWorkspaceService.getMembershipByTenantName(up, up.getTenantName());
            String uploadWorkspaceTokenGroupName = StringUtils.substringAfter(membershipWorkspace, "@");
            if (!util.validateMembership(nxlMetadata, uploadWorkspaceTokenGroupName)) {
                throw new ValidateException(5003, "The nxl file does not belong to this workspace");
            }
            if (!TokenMgmt.checkNxlMetadata(duid, DecryptUtil.getFilePolicyStr(nxlMetadata, null), GsonUtils.GSON.toJson(tagMap), nxlMetadata.getProtectionType())) {
                throw new ValidateException(5008, "The nxl file does not have valid metadata.");
            }

            EvalResponse evalResponse = util.getAdhocEvaluationResponse(nxlMetadata);
            rights = evalResponse.getRights();
            if (rights.length == 0) {
                User user = us.getUser();
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(user.getId())).ticket(up.getTicket()).clientId(up.getClientId()).platformId(up.getPlatformId()).deviceId(up.getDeviceId()).email(user.getEmail()).displayName(user.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(uploadReq.getFileName(), uploader, up.getTenantName(), userEval, tagMap);
                evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                rights = evalResponse.getRights();
            } else {
                isAdhocPolicy = true;
            }

            if (swsDTO.isUserConfirmedFileOverwrite() && swsDTO.getUploadTypeOridinal() == Constants.SharedWorkSpaceUploadType.UPLOAD_EDIT.ordinal()) {
                throw new ValidateException(400, "Invalid request parameters");
            }
            if (isNxl && swsDTO.getUploadTypeOridinal() == Constants.SharedWorkSpaceUploadType.UPLOAD_EDIT.ordinal()) {
                swsDTO.setAllowOverwrite(true);
                if ((!isAdhocPolicy || Integer.parseInt(createdBy) != us.getId()) && !ArrayUtils.contains(rights, Rights.EDIT)) {
                    throw new ValidateException(403, "Access Denied");
                }
            }

            if ((swsDTO.isAllowOverwrite() || swsDTO.isUserConfirmedFileOverwrite()) && !LockManager.getInstance().acquireLock(swsDTO.getUniqueResourceId(), TimeUnit.MINUTES.toMillis(5))) {
                swsDTO.setUserConfirmedFileOverwrite(false);
                swsDTO.setAllowOverwrite(false);
                throw new ValidateException(4002, "Another User is editing this file.");
            }
            IApplicationRepository repository = getRepository(up, swsDTO.getRepoId());
            FileUploadMetadata metadata = null;

            if (!swsDTO.getFileParentPathId().endsWith("/")) {
                swsDTO.setFileParentPathId(swsDTO.getFileParentPathId() + "/");
            }

            if (swsDTO.getUploadTypeOridinal() == Constants.SharedWorkSpaceUploadType.UPLOAD_NO_REENCRYPTION.ordinal()) {
                if (!swsDTO.isUserConfirmedFileOverwrite() && checkIfFileExists(up, swsDTO.getRepoId(), swsDTO.getFileParentPathId() + swsDTO.getFileName())) {
                    throw new ValidateException(4001, "File already exists");
                }
                metadata = uploadFile(repository, swsDTO.getFileParentPathId(), swsDTO.getFile().getPath(), swsDTO.isUserConfirmedFileOverwrite(), swsDTO.getFileName());
                SharedFileDAO.saveOrUpdateExternalRepositoryDB(up.getUserId(), swsDTO.getRepoId(), duid, uploader, Rights.toInt(rights), swsDTO.getFileName(), metadata.getPathDisplay(), metadata.getSize());

                RemoteLoggingMgmt.Activity uploadActivity = new RemoteLoggingMgmt.Activity(duid, membershipWorkspace, up.getUserId(), swsDTO.getUploadOps(), up.getDeviceId(), up.getPlatformId(), swsDTO.getRepoId(), metadata.getPathDisplay(), swsDTO.getFileName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.SHAREDWS);
                RemoteLoggingMgmt.saveActivityLog(uploadActivity);
            } else if (swsDTO.getUploadTypeOridinal() == Constants.SharedWorkSpaceUploadType.UPLOAD_EDIT.ordinal()) {
                metadata = uploadFile(repository, swsDTO.getFileParentPathId(), swsDTO.getFile().getPath(), swsDTO.isAllowOverwrite(), swsDTO.getFileName());
                SharedFileDAO.saveOrUpdateExternalRepositoryDB(up.getUserId(), swsDTO.getRepoId(), duid, uploader, Rights.toInt(rights), swsDTO.getFileName(), metadata.getPathDisplay(), metadata.getSize());

                RemoteLoggingMgmt.Activity uploadActivity = new RemoteLoggingMgmt.Activity(duid, membershipWorkspace, up.getUserId(), swsDTO.getUploadOps(), up.getDeviceId(), up.getPlatformId(), swsDTO.getRepoId(), metadata.getPathDisplay(), swsDTO.getFileName(), metadata.getPathDisplay(), null, null, null, AccessResult.ALLOW, new Date(), null, AccountType.SHAREDWS);
                RemoteLoggingMgmt.saveActivityLog(uploadActivity);
            } else {
                throw new ValidateException(400, "Unsupported upload type.");
            }
            return metadata;
        } catch (NxlException e) {
            if (e instanceof UnsupportedNxlVersionException) {
                ValidateException ve = new ValidateException(5006, "Unsupported NXL version.");
                ve.initCause(e);
                throw ve;
            } else {
                ValidateException ve = new ValidateException(5001, "Invalid NXL format.");
                ve.initCause(e);
                throw ve;
            }
        } finally {
            if (swsDTO != null && (swsDTO.isAllowOverwrite() || swsDTO.isUserConfirmedFileOverwrite()) && swsDTO.getUniqueResourceId() != null) {
                LockManager.getInstance().releaseRemoveLock(swsDTO.getUniqueResourceId());
            }
        }
    }

    /***
     * this method helps to transfer the file from one space to upload in another space as a part of copy api requirement
     * @param principal
     * @param destPojo
     * @param uploadFilePath
     * @param isOverwrite
     * @return returns file metadata to save in the external_Repository table
     * @throws ApplicationRepositoryException
     */
    public FileUploadMetadata transferFile(RMSUserPrincipal principal, RMSSpacePojo destPojo, String uploadFilePath,
        boolean isOverwrite) throws ApplicationRepositoryException {

        IApplicationRepository repository = getRepository(principal, destPojo.getSpaceId());
        return uploadFile(repository, destPojo.getParentPathId(), uploadFilePath, isOverwrite, destPojo.getFileName());
    }

    /***
     * this method helps to download the file from one space to  another space as a part of copy api requirement
     * @param principal
     * @param sourcePojo
     * @param tempTrfFolder
     * @return
     * @throws RepositoryException
     * @throws ApplicationRepositoryException
     * @throws IOException
     */
    public File downloadFileForTransfer(RMSUserPrincipal principal, RMSSpacePojo sourcePojo, File tempTrfFolder)
            throws RepositoryException, ApplicationRepositoryException, IOException {
        IApplicationRepository repository = getRepository(principal, sourcePojo.getSpaceId());
        return repository.getFile(sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), tempTrfFolder.getPath());
    }
}
