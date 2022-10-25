package com.nextlabs.rms.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.FIPSError;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.command.UploadFileCommand;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.IRMSRepositorySearcher;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.RMSRepositoryDBSearcherImpl;
import com.nextlabs.rms.repository.defaultrepo.RMSRepositorySearchException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.shared.WatermarkConfigManager;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * All util methods related to Repository files should go in this class
 * @author nnallagatla
 *
 */
public final class RepositoryFileUtil {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String SOURCE_FILE_PATH_ID = "SourceFilePathId";
    public static final String SOURCE_FILE_PATH_DISPLAY = "SourceFilePathDisplay";
    public static final String SOURCE_REPO_ID = "SourceRepoId";
    public static final String SOURCE_REPO_NAME = "SourceRepoName";
    public static final String SOURCE_REPO_TYPE = "SourceRepoType";
    public static final String SYSTEM_BUCKET_NAME_SUFFIX = "_system";

    private RepositoryFileUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method returns the folder into which repo file can be downloaded
     * @return
     * @throws IOException
     */
    public static File getTempOutputFolder() throws IOException {
        File outputFolder;
        File userTempDir = WebConfig.getInstance().getTmpDir();
        do {
            String threadDirId = Long.toHexString(Holder.MSB | Holder.NUMBER_GENERATOR.nextLong()) + Long.toHexString(Holder.MSB | Holder.NUMBER_GENERATOR.nextLong());
            outputFolder = new File(userTempDir, threadDirId);
        } while (outputFolder.exists());
        FileUtils.mkdir(outputFolder);

        return outputFolder;
    }

    /**
     * This method downloads the repository file to outputFolder. If outputFolder is not valid, an attempt is made to create it
     * @param repo
     * @param filePath
     * @param fileDisplayPath
     * @param outputFolder
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    public static File downloadFileFromRepo(IRepository repo, String filePath,
        String fileDisplayPath, File outputFolder) throws RepositoryException, IOException {
        if (repo == null) {
            LOGGER.error("Null passed as repository");
            throw new RepositoryException("repository does not exist");
        }
        FileUtils.mkdir(outputFolder);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to download file: '{}' from repository (ID: {}, name: {}).", filePath, repo.getRepoId(), repo.getRepoName());
        }
        return repo.getFile(filePath, fileDisplayPath, outputFolder.getAbsolutePath());
    }

    public static byte[] downloadPartialFileFromRepo(IRepository repo, String filePathId, String filePath)
            throws RepositoryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to partially download file: 'ID:{} PATH:{}' from repository (ID: {}, name: {}).", filePathId, filePath, repo.getRepoId(), repo.getRepoName());
        }
        //TODO: Make this future proof by figuring out a better way to find out where section data ends. Currently we only support 3 sections so the content always start at 0x4000.
        return repo.downloadPartialFile(filePathId, filePath, 0, NxlFile.COMPLETE_HEADER_SIZE - 1L);
    }

    public static void downloadFilesFromRepo(IRepository repo, String filePathId,
        String filePath, List<String> filePaths, String outputPath) throws RepositoryException,
            MissingDependenciesException, IOException {
        if (repo == null) {
            LOGGER.error("Null passed as repository");
            throw new RepositoryException("repository does not exist");
        }
        //IRepository repo = RepositoryFactory.getInstance().getRepository(userPrincipal, repoId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to download related files of: '{}' from repository (ID: {}, name: {}).", filePath, repo.getRepoId(), repo.getRepoName());
        }
        File outputFolder = new File(outputPath);
        if (!outputFolder.exists()) {
            FileUtils.mkdir(outputFolder);
        }
        repo.downloadFiles(filePathId, filePath, filePaths.toArray(new String[filePaths.size()]), outputPath);
    }

    /**
     * This method uploads the encrypted file to the repository.
     * @return
     * @throws IOException
     */
    public static UploadedFileMetaData uploadFileToRepo(RMSUserPrincipal userPrincipal, IRepository repo,
        String folderPathId,
        String folderPathDisplay, File fileFromRepo, boolean overwrite, String conflictFileName,
        Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException, IOException {
        if (repo == null) {
            LOGGER.error("Null passed as repository");
            throw new RepositoryException("repository does not exist");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to upload file: '{}' to repository (ID: {}, name: {}, user ID: {})", fileFromRepo.getName(), repo.getRepoId(), repo.getRepoName(), userPrincipal.getUserId());
        }
        File outputFolder = new File(fileFromRepo.getPath());
        if (!outputFolder.exists()) {
            FileUtils.mkdir(outputFolder);
        }
        boolean isUploadToMyVault = true;
        IRepository defaultRepo = null;
        try {
            try (DbSession session = DbSession.newSession()) {
                defaultRepo = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, userPrincipal);
                if (!repo.getRepoId().equals(defaultRepo.getRepoId()) || !folderPathId.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
                    isUploadToMyVault = false;
                }
            } catch (InvalidDefaultRepositoryException e) {
                LOGGER.error("Unable to get default repository for user ID: {}", userPrincipal.getUserId(), e);
            }
            if (isUploadToMyVault) {
                throw new RepositoryFolderAccessException("blocked attempt to upload regular file to MyVault");
            }
            return repo.uploadFile(folderPathId, folderPathDisplay, fileFromRepo.getPath(), overwrite, conflictFileName, customMetadata, userConfirmedFileOverwrite);
        } finally {
            if (defaultRepo instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(defaultRepo));
            }
        }
    }

    /**
     * Get file metadata from MyVault
     * @param userPrincipal logged in user
     * @param pathId File path ID without /nxl_myvault_nxl
     * @return metadata of the file in MyVault
     * @throws InvalidDefaultRepositoryException
     */
    public static StoreItem getMyVaultFileMetadata(RMSUserPrincipal userPrincipal, String pathId)
            throws InvalidDefaultRepositoryException {
        IRepository myDrive;
        try (DbSession session = DbSession.newSession()) {
            myDrive = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, userPrincipal);
        }
        IRMSRepositorySearcher searcher = new RMSRepositoryDBSearcherImpl();
        String filePathId = RepoConstants.MY_VAULT_FOLDER_PATH_ID + (pathId.startsWith("/") ? pathId.substring(1) : pathId).toLowerCase();
        try {
            return searcher.getRepoItem(myDrive.getRepoId(), filePathId);
        } catch (RMSRepositorySearchException e) {
            LOGGER.error("Error while trying to determine if duplicate file path in default storage");
        } finally {
            if (myDrive instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(myDrive));
            }
        }
        return null;
    }

    /**
     * Uploads an incoming file to MyVault and returns metadata wrapper.
     * Current usage is that all incoming files are already NXL encrypted.
     * This method is the only entry point for the entire RMS application.
     * The repo metadata parameters (repoId, repoName, and repoType) are
     * optional, but all other parameters should be considered mandatory
     *
     * @param  userPrincipal            logged in user
     * @param  repoId                   the ID of repo associated with this file (null if from manage local file page)
     * @param  repoName                 the name of repo associated with this file (null if from manage local file page)
     * @param  repoType                 the type of repo associated with this file (null if from manage local file page)
     * @param  originalFilePathId       original file path ID
     * @param  originalFilePathDisplay  original file path display name
     * @param  fileFromRepo             the handle for file to be uploaded
     * @param  overwrite                flag for whether upload should overwrite already existing file (false only for
     *                                  uploadnandprotect or protectandupload)
     * @param conflictFileName          filename used when saving file to MyVault
     * @param hasRepo                   flag for whether current upload originates from an actual repo
     * @param userConfirmedFileOverwrite file for whether user has confirmed to replace the file, if same file path exists
     * @return                          metadata object corresponding to file which has been uploaded
     * @throws RepositoryException
     * @throws IOException
     * @throws NxlException
     */
    public static UploadedFileMetaData uploadFileToMyVault(RMSUserPrincipal userPrincipal, String repoId,
        String repoName, String repoType, String originalFilePathId,
        String originalFilePathDisplay, File fileFromRepo, boolean overwrite, String conflictFileName,
        boolean hasRepo, boolean userConfirmedFileOverwrite, HttpServletRequest request)
            throws RepositoryException, IOException, NxlException {
        Map<String, String> myVaultDetailMetadata;
        Map<String, String> repoMetadata = new HashMap<>();
        if (fileFromRepo == null) {
            throw new NullPointerException("File is mandatory");
        } else if (!fileFromRepo.exists() || !fileFromRepo.isFile()) {
            throw new FileNotFoundException("Unable to find file: " + fileFromRepo.getAbsolutePath());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to upload file: '{}' to MyVault (user ID: {})", fileFromRepo.getName(), userPrincipal.getUserId());
        }
        String duid;
        NxlFile nxlMetadata = null;
        try (InputStream fis = new FileInputStream(fileFromRepo)) {
            final int length = 1024 * 16;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(fis, os, 0, length, length);
            byte[] nxlHeader = os.toByteArray();
            boolean nxl = NxlFile.isNxl(nxlHeader);
            if (!nxl) {
                throw new NxlException();
            }
            nxlMetadata = NxlFile.parse(nxlHeader);
            duid = nxlMetadata.getDuid();
        } finally {
            IOUtils.closeQuietly(nxlMetadata);
        }
        UploadedFileMetaData metaData = null;
        DefaultRepositoryTemplate myDrive = null;
        try {
            try (DbSession session = DbSession.newSession()) {
                myDrive = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, userPrincipal);
            }
            if (!hasRepo) {
                myVaultDetailMetadata = getCustomMetadata(originalFilePathId, originalFilePathDisplay, repoId, "Local", "Local");
            } else {
                myVaultDetailMetadata = getCustomMetadata(originalFilePathId, originalFilePathDisplay, repoId, repoName, repoType);
            }
            if (!hasRepo || !StringUtils.equals(myDrive.getRepoId(), repoId) || !StringUtils.startsWith(originalFilePathId, RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
                File outputFolder = new File(fileFromRepo.getPath());
                if (!outputFolder.exists()) {
                    FileUtils.mkdir(outputFolder);
                }
                repoMetadata.put("myVaultDetail", GsonUtils.GSON.toJson(myVaultDetailMetadata));
                repoMetadata.put("duid", duid);
                metaData = myDrive.uploadFile(RepoConstants.MY_VAULT_FOLDER_PATH_ID, RepoConstants.MY_VAULT_FOLDER_PATH_DISPLAY, fileFromRepo.getPath(), overwrite, conflictFileName, repoMetadata, userConfirmedFileOverwrite);
                metaData.setDuid(duid);
            }
        } catch (InvalidDefaultRepositoryException e) {
            LOGGER.error("Unable to get default repository for user ID: {}", userPrincipal.getUserId(), e);
        } finally {
            if (myDrive instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(myDrive));
            }
        }
        String ticket = userPrincipal.getTicket();
        int userId = userPrincipal.getUserId();
        String clientId = userPrincipal.getClientId();
        String path = HTTPUtil.getInternalURI(request);
        String fileRepoId = " ";
        String filePathId = " ";
        String filePathDisplay = " ";
        if (metaData != null) {
            fileRepoId = metaData.getRepoId();
            filePathId = metaData.getPathId();
            filePathDisplay = metaData.getPathDisplay();
        }
        RMSRestHelper.sendActivityLogToRMS(path, ticket, duid, nxlMetadata.getOwner(), userId, clientId, Operations.UPLOAD_NORMAL, fileRepoId, filePathId, filePathDisplay, AccessResult.ALLOW, request, null, null, AccountType.PERSONAL);
        return metaData;
    }

    public static void deleteFileFromRepo(IRepository repo, String filePath,
        String filePathDisplay) throws RepositoryException, ForbiddenOperationException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to delete file: '{}' from repository (ID: {}, name: {}).", filePath, repo.getRepoId(), repo.getRepoName());
        }
        repo.deleteFile(filePath, filePathDisplay);
    }

    public static UploadedFileMetaData protectAndUpload(RMSUserPrincipal userPrincipal, IRepository repo, String path,
        String filePathId, String filePathDisplay, File outputFolder, String membershipId, Rights[] rights,
        String watermark, String expiry, boolean userConfirmedFileOverwrite, HttpServletRequest request)
            throws RepositoryException,
            IOException, RMSException, NxlException, GeneralSecurityException, TokenGroupNotFoundException {
        File fileFromRepo;
        File outputFile;
        String conflictName;
        fileFromRepo = downloadFileFromRepo(repo, filePathId, filePathDisplay, outputFolder);
        String ticket = userPrincipal.getTicket();
        int userId = userPrincipal.getUserId();
        String clientId = userPrincipal.getClientId();
        Integer platformId = userPrincipal.getPlatformId();
        if (fileFromRepo == null) {
            throw new FileNotFoundException(RMSMessageHandler.getClientString("fileProcessErr"));
        } else {
            if (UploadFileCommand.invalidNxlUploadMembership(fileFromRepo)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("This '.nxl' file is not eligible for this operation based on its tenant membership.");
                }
                throw new RMSException(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
            }
            conflictName = fileFromRepo.getName() + Constants.NXL_FILE_EXTN;
            String newOutputFileNameString = conflictName;
            outputFile = new File(outputFolder, newOutputFileNameString);
            Map<String, String[]> emptyTagsHashMap = new HashMap<>();
            if (ArrayUtils.contains(rights, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                JsonWatermark item = WatermarkConfigManager.getWaterMarkConfig(path, userPrincipal.getTenantName(), userPrincipal.getTicket(), userPrincipal.getPlatformId(), String.valueOf(userPrincipal.getUserId()), userPrincipal.getClientId());
                watermark = item != null ? item.getText() : "";
            }
            try (OutputStream os = new FileOutputStream(outputFile)) {
                try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membershipId, rights, watermark, GsonUtils.GSON.fromJson(expiry, JsonExpiry.class), emptyTagsHashMap, fileFromRepo, os))) {
                    String duid = nxl.getDuid();
                    RMSRestHelper.sendActivityLogToRMS(path, ticket, duid, nxl.getOwner(), userId, clientId, Operations.PROTECT, repo.getRepoId(), filePathId, filePathDisplay, AccessResult.ALLOW, request, null, null, AccountType.PERSONAL);
                    String filePolicy = DecryptUtil.getFilePolicyStr(nxl, null);
                    SharedFileDAO.updateNxlDBProtect(userId, duid, nxl.getOwner(), Rights.toInt(rights), conflictName, filePolicy);
                    UploadedFileMetaData fileMetaData = uploadFileToMyVault(userPrincipal, repo.getRepoId(), repo.getRepoName(), repo.getRepoType().name(), filePathId, filePathDisplay, outputFile, false, conflictName, true, userConfirmedFileOverwrite, request);
                    if (fileMetaData != null) {
                        fileMetaData.setDuid(duid);
                        fileMetaData.setFile(fileFromRepo);
                    }
                    return fileMetaData;
                }
            }
        }
    }

    public static UploadedFileMetaData protectAndUploadInPlace(RMSUserPrincipal userPrincipal, IRepository repo,
        String path,
        String filePathId, String filePathDisplay, File outputFolder, String membershipId, Rights[] rights,
        String watermark, String expiry, boolean userConfirmedFileOverwrite, HttpServletRequest request)
            throws RepositoryException,
            IOException, RMSException, NxlException, GeneralSecurityException, TokenGroupNotFoundException {
        File fileFromRepo;
        File outputFile;
        String conflictName;
        fileFromRepo = downloadFileFromRepo(repo, filePathId, filePathDisplay, outputFolder);
        String ticket = userPrincipal.getTicket();
        int userId = userPrincipal.getUserId();
        String clientId = userPrincipal.getClientId();
        Integer platformId = userPrincipal.getPlatformId();
        if (fileFromRepo == null) {
            throw new FileNotFoundException(RMSMessageHandler.getClientString("fileProcessErr"));
        } else {
            if (UploadFileCommand.invalidNxlUploadMembership(fileFromRepo)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("This '.nxl' file is not eligible for this operation based on its tenant membership.");
                }
                throw new RMSException(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
            }
            conflictName = fileFromRepo.getName() + Constants.NXL_FILE_EXTN;
            String newOutputFileNameString = conflictName;
            outputFile = new File(outputFolder, newOutputFileNameString);

            Map<String, String[]> tags = new HashMap<>();
            String tagsSelected = request.getParameter("tags");
            if (StringUtils.hasText(tagsSelected)) {
                JsonObject jsonTag = new JsonParser().parse(tagsSelected).getAsJsonObject();
                for (Map.Entry<String, JsonElement> set : jsonTag.entrySet()) {
                    JsonArray arr = set.getValue().getAsJsonArray();
                    String[] tagValues = new String[arr.size()];
                    for (int i = 0; i < arr.size(); ++i) {
                        tagValues[i] = arr.get(i).getAsString();
                    }
                    tags.put(set.getKey(), tagValues);
                }
            }

            if (ArrayUtils.contains(rights, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                JsonWatermark item = WatermarkConfigManager.getWaterMarkConfig(path, userPrincipal.getTenantName(), userPrincipal.getTicket(), userPrincipal.getPlatformId(), String.valueOf(userPrincipal.getUserId()), userPrincipal.getClientId());
                watermark = item != null ? item.getText() : "";
            }
            try (OutputStream os = new FileOutputStream(outputFile)) {
                try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membershipId, rights, watermark, GsonUtils.GSON.fromJson(expiry, JsonExpiry.class), tags, fileFromRepo, os))) {
                    String duid = nxl.getDuid();
                    RMSRestHelper.sendActivityLogToRMS(path, ticket, duid, nxl.getOwner(), userId, clientId, Operations.PROTECT, repo.getRepoId(), filePathId, filePathDisplay, AccessResult.ALLOW, request, null, null, AccountType.PERSONAL);

                    UploadedFileMetaData fileMetaData = uploadFileToRepo(userPrincipal, repo, repo.getCurrentFolderPathId(filePathId, filePathDisplay), repo.getCurrentFolderPathId(filePathId, filePathDisplay), outputFile, userConfirmedFileOverwrite, conflictName, null, userConfirmedFileOverwrite);
                    SharedFileDAO.updateExternalNxlDBProtect(userId, repo.getRepoId(), duid, nxl.getOwner(), Rights.toInt(rights), conflictName, fileMetaData.getPathDisplay());
                    fileMetaData.setDuid(duid);
                    fileMetaData.setFile(fileFromRepo);
                    return fileMetaData;
                }
            }
        }
    }

    public static String getConflictFileName(String fileName) {
        int index = fileName.lastIndexOf('.');
        String fileNameWithoutExt = fileName;
        String extension = "";
        if (index != -1) {
            fileNameWithoutExt = fileName.substring(0, index);
            extension = fileName.substring(index);
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String timestamp = fmt.format(new Date());
        return fileNameWithoutExt + "-" + timestamp + extension;
    }

    public static String getOriginalFileExtension(String fileName) {
        String fileExtension = "";
        if (!StringUtils.hasText(fileName)) {
            return fileExtension;
        }
        String extension = FileUtils.getRealFileExtension(fileName);
        if (Constants.NXL_FILE_EXTN.equalsIgnoreCase(extension)) {
            fileName = fileName.substring(0, fileName.length() - Constants.NXL_FILE_EXTN.length());
        }
        fileExtension = FileUtils.getRealFileExtension(fileName);
        if (StringUtils.hasText(fileExtension)) {
            fileExtension = fileExtension.substring(1);
        }
        return fileExtension;
    }

    public static Map<String, String> getCustomMetadata(String sourceFilePathId, String sourceFilePathDisplay,
        String sourceRepoId, String sourceRepoName, String sourceRepoType) {
        Map<String, String> customMetadata = new HashMap<>(5);
        customMetadata.put(SOURCE_FILE_PATH_ID, sourceFilePathId);
        customMetadata.put(SOURCE_FILE_PATH_DISPLAY, sourceFilePathDisplay);
        customMetadata.put(SOURCE_REPO_ID, sourceRepoId);
        customMetadata.put(SOURCE_REPO_NAME, sourceRepoName);
        customMetadata.put(SOURCE_REPO_TYPE, sourceRepoType);
        return customMetadata;
    }

    public static String getDynamicMembershipName(int userId, String tenantName) {
        return "user" + userId + '@' + tenantName.concat(SYSTEM_BUCKET_NAME_SUFFIX);

    }

    private static final class Holder {

        static final long MSB = 0x8000000000000000L;
        static final SecureRandom NUMBER_GENERATOR = createDRBG();

        private static SecureRandom createDRBG() {
            try {
                return SecureRandom.getInstance("DEFAULT", "BCFIPS");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                LOGGER.error("DRBG algorithm or provider not available");
                throw new FIPSError("DRBG algorithm or provider not available", e);
            }
        }
    }

}
