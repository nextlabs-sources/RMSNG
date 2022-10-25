package com.nextlabs.rms.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.application.FileUploadMetadata;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.pojos.RMSSpacePojo;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.SharedWorkspaceService;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.share.ShareServiceImpl;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransformUtil {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String VAL_ERR_MISSING_REQ_PARAMS = "Missing required parameters.";
    private static final String SPACE_TYPE = "spaceType";
    private static final String FILE_NAME = "fileName";
    private static final String FILE_PATH_ID = "filePathId";
    private static final String PARENT_PATH_ID = "parentPathId";
    private static final String TRANSACTION_ID = "transactionId";
    private static final String SPACE_ID = "spaceId";
    public static final List<String> EXTERNAL_REPOSITORIES = Collections.unmodifiableList(Arrays.asList(ServiceProviderType.BOX.name(), ServiceProviderType.DROPBOX.name(), ServiceProviderType.GOOGLE_DRIVE.name(), ServiceProviderType.ONE_DRIVE.name()));
    private static final Map<String, List<String>> SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS = Maps.newHashMap();
    private static final Map<String, List<String>> DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS = Maps.newHashMap();
    private final Rights[] rights;

    Map<String, String[]> tags;
    FilePolicy filePolicy;
    DbSession session;
    String ownerMembership;
    File tempTrfFolder;
    File srcDownloadNxlFile;
    File srcOriginalFile;
    File destUploadNxlFile;
    RMSSpacePojo sourcePojo;
    RMSSpacePojo destPojo;
    String rmsUrl;
    String loginTenantId;
    RMSUserPrincipal principal;
    private String newDuid;
    private String duid;
    String response = "Transfer of File Failed";
    boolean isAdhocPolicy;
    boolean isOverWrite;
    HttpServletRequest request;
    boolean isFileUpload;

    static {
        SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.MY_VAULT.name(), Arrays.asList(FILE_PATH_ID));
        SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.SHARED_WITH_ME.name(), Arrays.asList(TRANSACTION_ID));
        SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name(), Arrays.asList(FILE_PATH_ID));
        SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Arrays.asList(FILE_PATH_ID, SPACE_ID));
        SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.PROJECT.name(), Arrays.asList(FILE_PATH_ID, SPACE_ID));
        for (String externalRepository : TransformUtil.EXTERNAL_REPOSITORIES) {
            SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.put(externalRepository, Arrays.asList(FILE_PATH_ID, SPACE_ID));
        }

        DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name(), Arrays.asList(FILE_NAME, PARENT_PATH_ID, SPACE_ID));
        DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.LOCAL_DRIVE.name(), Arrays.asList());
        DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.put(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name(), Arrays.asList(FILE_NAME, PARENT_PATH_ID));
        for (String externalRepository : TransformUtil.EXTERNAL_REPOSITORIES) {
            DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.put(externalRepository, Arrays.asList(FILE_NAME, PARENT_PATH_ID, SPACE_ID));
        }
    }

    public String getResponse() {
        return response;
    }

    public File getSrcDownloadNxlFile() {
        return srcDownloadNxlFile;
    }

    public void setSrcDownloadNxlFile(File srcDownloadNxlFile) {
        this.srcDownloadNxlFile = srcDownloadNxlFile;
    }

    public void setDUID(String duid) {
        this.duid = duid;
    }

    public void setOwnerMembership(String ownerMembership) {
        this.ownerMembership = ownerMembership;
    }

    public static void validateRequest(JsonRequest request) {
        if (request == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
    }

    public static void validateSourcePayload(Map source) {
        if (source == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        String spaceType = (String)source.get(SPACE_TYPE);
        if (!StringUtils.hasText(spaceType)) {
            LOGGER.error("spaceType param is missing");
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        List<String> mandatoryFields = SOURCE_SPACE_TYPE_TO_MANDATORY_FIELDS.get(spaceType);
        if (mandatoryFields == null) {
            throw new ValidateException(400, "Invalid space type in source payload");
        }
        checkMandatoryFields(source, mandatoryFields, null);

        if (EXTERNAL_REPOSITORIES.stream().noneMatch(spaceType::equalsIgnoreCase) && !Constants.TransferSpaceType.SHARED_WITH_ME.name().equalsIgnoreCase(spaceType)) {
            String fileName = (String)source.get(FILE_PATH_ID);
            if (!StringUtils.hasText(fileName)) {
                fileName = (String)source.get(FILE_PATH_ID);
            }
            if (!"nxl".equalsIgnoreCase(FileUtils.getExtension(fileName))) {
                throw new ValidateException(400, "Source file is not a NXL file");
            }
        }
    }

    public static void validateDestinationPayload(Map destination) {
        if (destination == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        String spaceType = (String)destination.get(SPACE_TYPE);
        if (!StringUtils.hasText(spaceType)) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }

        List<String> mandatoryFields = DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.get(spaceType);
        if (mandatoryFields == null) {
            throw new ValidateException(400, "Invalid space type in destination payload");
        }
        Set<String> emptyValueAllowedFields = null;
        // for dropbox filePathId for root would be ""
        if (ServiceProviderType.DROPBOX.name().equalsIgnoreCase(spaceType)) {
            emptyValueAllowedFields = Sets.newHashSet(FILE_PATH_ID);
        }
        checkMandatoryFields(destination, mandatoryFields, emptyValueAllowedFields);

        if (DESTINATION_SPACE_TYPE_TO_MANDATORY_FIELDS.get(spaceType).contains(FILE_NAME) && !"nxl".equalsIgnoreCase(FileUtils.getExtension(destination.get(FILE_NAME).toString()))) {
            throw new ValidateException(400, "Invalid destination name");
        }

        if (Constants.TransferSpaceType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(spaceType) && !destination.get(PARENT_PATH_ID).toString().startsWith("/")) {
            throw new ValidateException(400, "Invalid parent path id");
        }
    }

    private static void checkMandatoryFields(Map payload, List<String> mandatoryFields,
        Set<String> emptyValueAllowedFields) {
        if (emptyValueAllowedFields == null) {
            emptyValueAllowedFields = Sets.newHashSet();
        }
        for (String field : mandatoryFields) {
            Object value = payload.get(field);
            if (value == null || (!emptyValueAllowedFields.contains(field) && !StringUtils.hasText(value.toString()))) {
                LOGGER.error("{} param is missing", field);
                throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
            }
        }
    }

    public static boolean hasRight(Rights[] rights, Rights right) {
        return ArrayUtils.contains(rights, right);
    }

    public File getDestUploadNxlFile() {
        return destUploadNxlFile;
    }

    public TransformUtil(RMSUserPrincipal principal, String loginTenant, RMSSpacePojo sourcePojo, RMSSpacePojo destPojo,
        String rmsUrl, HttpServletRequest request, Rights[] rights, boolean isFileUpload, boolean isOverWrite,
        DbSession session)
            throws IOException {
        this.sourcePojo = sourcePojo;
        this.destPojo = destPojo;
        this.loginTenantId = loginTenant;
        this.rmsUrl = rmsUrl;
        this.principal = principal;
        this.request = request;
        this.rights = rights;
        this.isFileUpload = isFileUpload;
        this.isOverWrite = isOverWrite;
        this.session = session;
        try {
            tempTrfFolder = RepositoryFileUtil.getTempOutputFolder();
        } catch (IOException e) {
            LOGGER.error("Error: Unable to create a temporary folder", e);
            throw e;
        }
    }

    public void transferFile() throws IOException, RepositoryException, InvalidDefaultRepositoryException, NxlException,
            ApplicationRepositoryException, RMSException, TokenGroupException {
        try {
            if (!isFileUpload) {
                downloadSourceNXLFile();
            }
            decryptSourceNXLFile();
            transferTokenGroup();
            encryptSourceOriginalFile();
            if (!destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name())) {
                uploadDestUploadFile();
            }
            updateAuditLog(Constants.AccessResult.ALLOW);
            response = "File Successfully Transferred from Source to Destination";

        } catch (IOException e) {
            LOGGER.error("Error occurred while transferring the file", e);
            throw e;
        } catch (RepositoryException | InvalidDefaultRepositoryException | ApplicationRepositoryException e) {
            LOGGER.error("Error occurred while downloading file from " + sourcePojo.getSpaceType(), e);
            throw e;
        } catch (NxlException e) {
            LOGGER.error("Error occurred while parsing a nxl file", e);
            throw e;
        } catch (RMSException e) {
            LOGGER.error("Internal Server Error Occured", e);
            throw e;
        }
    }

    public void updateAuditLog(Constants.AccessResult effect) {
        String savedFrom = "Saved from {0} of {1}";
        String saveAs = "Save {0} to {1}";
        String addFileTo = "Add {0} to {1}";
        String addedFrom = "Added from {0} of {1}";

        if (destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(destPojo)) {
            saveActivityLog(effect, Operations.SAVED_FROM, MessageFormat.format(savedFrom, sourcePojo.getFileName(), isRepoNametoBeAudited(sourcePojo) ? sourcePojo.getSpaceRepoName() : sourcePojo.getSpaceType()), sourcePojo, duid);

            if (Constants.AccessResult.ALLOW == effect) {
                saveActivityLog(effect, Operations.SAVE_AS, MessageFormat.format(saveAs, destPojo.getFileName(), isRepoNametoBeAudited(destPojo) ? destPojo.getSpaceRepoName() : destPojo.getSpaceType()), destPojo, newDuid);
            }
        } else {
            saveActivityLog(effect, Operations.ADDED_FROM, MessageFormat.format(addedFrom, sourcePojo.getFileName(), isRepoNametoBeAudited(sourcePojo) ? sourcePojo.getSpaceRepoName() : sourcePojo.getSpaceType()), sourcePojo, duid);

            if (Constants.AccessResult.ALLOW == effect) {
                saveActivityLog(effect, Operations.ADD_FILE_TO, MessageFormat.format(addFileTo, destPojo.getFileName(), isRepoNametoBeAudited(destPojo) ? destPojo.getSpaceRepoName() : destPojo.getSpaceType()), destPojo, newDuid);
            }
        }

    }

    private boolean isRepoNametoBeAudited(RMSSpacePojo spacePojo) {
        boolean flag = false;
        if (spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.PROJECT.name()) || spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) {
            flag = true;
        }
        return flag;
    }

    private void saveActivityLog(Constants.AccessResult effect, Operations operation, String activityDetail,
        RMSSpacePojo spacePojo, String duid) {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(Constants.ACTIVITY_DATA_ACTIVITY_DETAIL, activityDetail);
        RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, Nvl.nvl(ownerMembership), principal.getUserId(), operation, principal.getDeviceId(), principal.getPlatformId(), loginTenantId, spacePojo.getFilePathId(), spacePojo.getFileName(), spacePojo.getFilePathId(), null, null, null, effect, new Date(), GsonUtils.GSON.toJson(attributes), getAccountType(spacePojo));
        RemoteLoggingMgmt.saveActivityLog(activity, session);
    }

    private Constants.AccountType getAccountType(RMSSpacePojo spacePojo) {
        if (spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(spacePojo)) {
            return Constants.AccountType.LOCALDRIVE;
        } else if (spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.PROJECT.name())) {
            return Constants.AccountType.PROJECT;
        } else if (spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name())) {
            return Constants.AccountType.ENTERPRISEWS;
        } else if (spacePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) {
            return Constants.AccountType.SHAREDWS;
        } else {
            return Constants.AccountType.PERSONAL;
        }
    }

    private void transferTokenGroup() throws TokenGroupException, IOException {
        Constants.TokenGroupType tokenGroupType = getTokenGroupType();
        if ((sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.PROJECT.name()) || sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHARED_WITH_ME.name()) || sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.MY_VAULT.name())) && (destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name()) || destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name()))) {
            ownerMembership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, loginTenantId);
        } else if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHARED_WITH_ME.name()) && (destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(destPojo))) {
            ownerMembership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, loginTenantId);
        } else if ((sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(sourcePojo) || sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) && (destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name()) || destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name()))) {
            if (tokenGroupType == Constants.TokenGroupType.TOKENGROUP_TENANT || tokenGroupType == Constants.TokenGroupType.TOKENGROUP_PROJECT) {
                ownerMembership = EnterpriseWorkspaceService.getMembershipByTenantId(principal, loginTenantId);
            }
        } else if ((sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name()) || sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.PROJECT.name()) || sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(sourcePojo)) && ((destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.LOCAL_DRIVE.name()) || isSpaceTypePersonalRepository(destPojo)) && tokenGroupType == Constants.TokenGroupType.TOKENGROUP_PROJECT)) {
            Project project = getProject();
            ownerMembership = ProjectService.getMembership(principal, project);
        }
        LOGGER.debug("Transfer API Destination owner: " + ownerMembership);
    }

    private Project getProject() {
        Membership membership = session.get(Membership.class, ownerMembership);
        return membership.getProject();
    }

    private Constants.TokenGroupType getTokenGroupType() throws TokenGroupException {
        String ownerTokenGroupName = StringUtils.substringAfter(ownerMembership, "@");
        TokenGroupManager ownerTokenGroupManager = null;
        ownerTokenGroupManager = TokenGroupManager.newInstance(session, ownerTokenGroupName, loginTenantId);
        return ownerTokenGroupManager.getGroupType();
    }

    private void uploadDestUploadFile() throws RepositoryException {
        if (destPojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) {
            try {
                FileUploadMetadata fileMetadata = new SharedWorkspaceService().transferFile(principal, destPojo, destUploadNxlFile.getPath(), isOverWrite);
                SharedFileDAO.saveOrUpdateExternalRepositoryDB(principal.getUserId(), destPojo.getSpaceId(), newDuid, ownerMembership, rights != null ? Rights.toInt(rights) : 0, destPojo.getFileName(), fileMetadata.getPathDisplay(), fileMetadata.getSize());
                Repository repo = RepositoryManager.getRepository(destPojo.getSpaceId());
                destPojo.setSpaceRepoName(repo.getName());
            } catch (ApplicationRepositoryException e) {
                throw new RepositoryException("Unable to upload the file to Shared Workspace", e);
            }

        } else if (isSpaceTypePersonalRepository(destPojo)) {
            IRepository repository = getPersonalRepository(destPojo);
            UploadedFileMetaData fileMetaData = repository.uploadFile(destPojo.getParentPathId(), destPojo.getParentPathId(), destUploadNxlFile.getPath(), isOverWrite, destUploadNxlFile.getName(), null, isOverWrite);
            SharedFileDAO.saveOrUpdateExternalRepositoryDB(principal.getUserId(), repository.getRepoId(), newDuid, ownerMembership, rights != null ? Rights.toInt(rights) : 0, destUploadNxlFile.getName(), fileMetaData.getPathDisplay(), fileMetaData.getSize());
        }
        boolean deleted = destUploadNxlFile.delete();
        if (!deleted) {
            LOGGER.warn("Unable to delete uploaded file: {}", destUploadNxlFile.getAbsolutePath());
        }
    }

    private boolean isSpaceTypePersonalRepository(RMSSpacePojo spacePojo) {
        return EXTERNAL_REPOSITORIES.stream().anyMatch(spacePojo.getSpaceType()::equalsIgnoreCase);
    }

    private void encryptSourceOriginalFile() throws NxlException, IOException {
        String destUploadNXLFileName = srcOriginalFile.getName() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN;
        if (destPojo.getFileName() != null && destPojo.getFileName().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            destUploadNXLFileName = destPojo.getFileName();
        }
        destUploadNxlFile = new File(tempTrfFolder.getPath(), destUploadNXLFileName);
        try (OutputStream os = new FileOutputStream(destUploadNxlFile)) {
            try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), rmsUrl, ownerMembership, filePolicy, tags, srcOriginalFile, os, isAdhocPolicy))) {
                newDuid = nxl.getDuid();
                destPojo.setFileName(destUploadNXLFileName);
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Source File not found for encryption", e);
        } catch (IOException e) {
            throw new IOException("Unable to read/write the source/destination files", e);
        } catch (NxlException e) {
            throw new NxlException("Unable to Re-Encrypt the file to destination", e);
        } catch (GeneralSecurityException e) {
            throw new NxlException("Security Exception while encrypting the file", e);
        } catch (TokenGroupNotFoundException e) {
            throw new NxlException("TokenGroup not found", e);
        }
        LOGGER.debug("The Re-Encrypted Transfer nxl file is in the path :" + destUploadNxlFile.getPath());
        LOGGER.debug("The file is transferred to destination successfully with new DUID :" + newDuid);
        boolean deleted = srcOriginalFile.delete();
        if (!deleted) {
            LOGGER.warn("Unable to delete uploaded file: {}", srcOriginalFile.getAbsolutePath());
        }
    }

    private void decryptSourceNXLFile() throws IOException, NxlException {

        try (InputStream is = new FileInputStream(srcDownloadNxlFile);
                NxlFile nxl = NxlFile.parse(is)) {
            try {
                if (nxl.getProtectionType() == Constants.ProtectionType.ADHOC) {
                    isAdhocPolicy = true;
                }
                ownerMembership = nxl.getOwner();
                filePolicy = DecryptUtil.getFilePolicy(nxl, null);
                LOGGER.debug("Transfer API filepolicy issuer:" + filePolicy.getIssuer());
                LOGGER.debug("Transfer API Source owner: " + ownerMembership);
                LOGGER.debug("Transfer API isAdhocPolicy: " + isAdhocPolicy);
                tags = DecryptUtil.getTags(nxl, null);
                duid = nxl.getDuid();
                byte[] token = DecryptUtil.requestToken(rmsUrl, principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), ownerMembership, duid, nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                String originalFileName = null;
                if (fileInfo != null) {
                    originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : EnterpriseWorkspaceService.getFileNameWithoutNXL(srcDownloadNxlFile.getName());
                }
                if (originalFileName != null) {
                    srcOriginalFile = new File(srcDownloadNxlFile.getParent(), originalFileName);
                    try (OutputStream fos = new FileOutputStream(srcOriginalFile)) {
                        DecryptUtil.decrypt(nxl, token, fos);
                    }
                }

            } catch (JsonException e) {
                throw new NxlException("Invalid token", e);
            } catch (GeneralSecurityException e) {
                throw new NxlException("Not able to retrieve Protection type or File Policy or File Tags", e);
            }
        }
        LOGGER.debug("The Source Transfer nxl file is decrypted to path :" + srcOriginalFile.getPath());
        boolean deleted = srcDownloadNxlFile.delete();
        if (!deleted) {
            LOGGER.warn("Unable to delete uploaded file: {}", srcDownloadNxlFile.getAbsolutePath());
        }
    }

    private void downloadSourceNXLFile() throws RepositoryException, InvalidDefaultRepositoryException,
            ApplicationRepositoryException, IOException, RMSException {
        if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.ENTERPRISE_WORKSPACE.name())) {
            srcDownloadNxlFile = new EnterpriseWorkspaceDownloadUtil().downloadOriginal(principal, loginTenantId, sourcePojo.getFilePathId(), tempTrfFolder);
        } else if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.PROJECT.name())) {
            Project project = new Project();
            project.setId(Integer.parseInt(sourcePojo.getSpaceId()));
            srcDownloadNxlFile = new ProjectDownloadUtil().downloadOriginal(principal, project, sourcePojo.getFilePathId(), tempTrfFolder);
        } else if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHAREPOINT_ONLINE.name())) {
            srcDownloadNxlFile = new SharedWorkspaceService().downloadFileForTransfer(principal, sourcePojo, tempTrfFolder);
            Repository repo = RepositoryManager.getRepository(sourcePojo.getSpaceId());
            sourcePojo.setSpaceRepoName(repo.getName());
        } else if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.MY_VAULT.name())) {
            srcDownloadNxlFile = new MyVaultDownloadUtil().downloadFileForTransfer(principal, sourcePojo, tempTrfFolder);
        } else if (isSpaceTypePersonalRepository(sourcePojo)) {
            IRepository repository = getPersonalRepository(sourcePojo);
            srcDownloadNxlFile = RepositoryFileUtil.downloadFileFromRepo(repository, sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), tempTrfFolder);
        } else if (sourcePojo.getSpaceType().equalsIgnoreCase(Constants.TransferSpaceType.SHARED_WITH_ME.name())) {
            ShareServiceImpl shareService = new ShareServiceImpl(sourcePojo.getTransactionId(), principal, request);
            srcDownloadNxlFile = shareService.downloadFileForTransfer(principal, tempTrfFolder);
        }
        sourcePojo.setFileName(srcDownloadNxlFile.getName());
        LOGGER.debug("The Source Transfer nxl file is downloaded to path :" + srcDownloadNxlFile.getPath());
    }

    private IRepository getPersonalRepository(RMSSpacePojo sourcePojo) throws RepositoryException {
        IRepository repository;
        try {
            repository = RepositoryFactory.getInstance().getRepository(session, principal, sourcePojo.getSpaceId());
        } catch (RepositoryException e1) {
            throw new RepositoryException("Repository not found", e1);
        }
        return repository;
    }
}
