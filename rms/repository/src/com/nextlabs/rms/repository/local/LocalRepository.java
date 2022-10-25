package com.nextlabs.rms.repository.local;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.OperationResult;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.shared.LogConstants;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalRepository implements IRepository, Closeable {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private RMSUserPrincipal userPrincipal;
    private String repoId;
    private final ServiceProviderSetting setting;
    private ServiceProviderType repoType = ServiceProviderType.LOCAL_DRIVE;
    private String bucketName;
    private String repoName;
    private String accountName;

    private boolean isShared;
    private final Map<String, Object> attributes;

    public LocalRepository(RMSUserPrincipal userPrincipal, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = userPrincipal;
        this.setting = setting;
        attributes = new HashMap<String, Object>();
    }

    private File getRepoFolder() throws IOException {
        if (!StringUtils.hasText(bucketName)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(50);
        String rootPath = setting.getAttributes().get(ServiceProviderSetting.LOCAL_DRIVE_PATH);
        sb.append(rootPath);
        if (!rootPath.endsWith("/")) {
            sb.append('/');
        }
        sb.append("defaultRepo");
        if (!bucketName.startsWith("/")) {
            sb.append('/');
        }
        sb.append(bucketName).append('/').append(repoId);
        File repoFolder = new File(sb.toString());
        if (!repoFolder.exists()) {
            FileUtils.mkdir(repoFolder);
        }
        return repoFolder;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public ServiceProviderSetting getSetting() {
        return setting;
    }

    @Override
    public RMSUserPrincipal getUser() {
        return userPrincipal;
    }

    @Override
    public void setUser(RMSUserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public String getRepoId() {
        return repoId;
    }

    @Override
    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    @Override
    public ServiceProviderType getRepoType() {
        return repoType;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public List<RepositoryContent> getFileList(String pathId) throws RepositoryException {
        return getFileList(pathId, new FilterOptions());
    }

    @Override
    public List<RepositoryContent> getFileList(String pathId, FilterOptions filterOptions) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }
        FileInputStream input = null;
        try {
            File file = new File(getRepoFolder(), fileId);
            if (!file.exists()) {
                throw new RepositoryException("File " + fileId + " does not exist");
            }
            input = new FileInputStream(file);
            if (input.skip(startByte) < startByte) {
                throw new IOException("Invalid request range");
            }
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                IOUtils.copy(input, outStream, 0, endByte - startByte + 1);
                return outStream.toByteArray();
            }
        } catch (IOException e) {
            logger.error("Unable to get file  " + fileId + "): " + e.getMessage(), e);
            handleRepoException(e);
        } finally {
            IOUtils.closeQuietly(input);
        }
        return null;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException,
            IllegalArgumentException {
        if (!StringUtils.hasText(fileId) && !StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("fileId and filePath cannot both be empty");
        }
        try {
            File file = new File(getRepoFolder(), fileId);
            if (!file.exists()) {
                throw new RepositoryException("File " + fileId + " does not exist");
            }
            String fileName = StringUtils.hasText(filePath) ? filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length()) : fileId.substring(fileId.lastIndexOf('/') + 1, fileId.length());
            File output = new File(outputPath, fileName);
            Files.copy(file.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return output;
        } catch (IOException e) {
            logger.error("Unable to get file  " + fileId + "): " + e.getMessage(), e);
            handleRepoException(e);
        }
        return null;
    }

    @Override
    public List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadedFileMetaData uploadFile(final String parentFilePathId, final String parentFilePathDisplay,
        final String localFile, final boolean overwrite, final String conflictFileName,
        final Map<String, String> customMetadata) throws RepositoryException {

        UploadedFileMetaData uploadedFileMetaData = null;
        File uploadFile = new File(localFile);
        long fileSize = uploadFile.length();

        if (uploadFile.getName().length() > 255) {
            throw new InvalidFileNameException("Invalid name: " + uploadFile.getName());
        }
        String filePathDisplay = getDisplayPathWithSeparatorFixed(parentFilePathDisplay, uploadFile.getName(), false);
        String key = new StringBuilder(parentFilePathId).append(uploadFile.getName().toLowerCase()).toString();

        try {
            File destFile = new File(getRepoFolder(), key);
            if (!destFile.getParentFile().exists()) {
                FileUtils.mkdir(destFile.getParentFile());
            }
            Files.copy(uploadFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try (FileInputStream fileinputstream = new FileInputStream(destFile.getAbsolutePath())) {
                if (uploadFile.length() != fileinputstream.getChannel().size()) {
                    throw new InSufficientSpaceException("uploaded file is corrupted, please check storage size");
                }
            }
            final String filePathId = key.toLowerCase();

            uploadedFileMetaData = new UploadedFileMetaData();
            uploadedFileMetaData.setPathDisplay(filePathDisplay);
            uploadedFileMetaData.setPathId(filePathId);
            uploadedFileMetaData.setFileNameWithTimeStamp(conflictFileName);
            uploadedFileMetaData.setRepoId(getRepoId());
            uploadedFileMetaData.setRepoName(getRepoName());
            uploadedFileMetaData.setSize(fileSize);
            if (customMetadata != null) {
                uploadedFileMetaData.setCreatedBy(customMetadata.get("createdBy"));
            }
            if (customMetadata != null && customMetadata.get("lastModified") != null) {
                uploadedFileMetaData.setLastModifiedTime(new Date(Long.parseLong(customMetadata.get("lastModified"))));
            } else {
                uploadedFileMetaData.setLastModifiedTime(new Date(uploadFile.lastModified()));
            }
        } catch (IOException e) {
            logger.error("Unable to upload file (path: " + localFile + "): " + e.getMessage(), e);
            handleRepoException(e);
        }

        return uploadedFileMetaData;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException {
        if (!StringUtils.hasText(filePathId) && !StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("fileId and filePath cannot both be empty");
        }
        try {
            File file = new File(getRepoFolder(), filePathId);
            FileUtils.deleteQuietly(file);
        } catch (IOException e) {
            logger.error("Unable to delete file  " + filePathId + "): " + e.getMessage(), e);
            handleRepoException(e);
        }
        return null;
    }

    /**
     * If the parentPathDisplay does not end with /, this method will add it so that we don't have paths with missing separators
     * @param parentPathDisplay
     * @param name
     * @param isDir
     * @return
     */
    private String getDisplayPathWithSeparatorFixed(String parentPathDisplay, String name, boolean isDir) {
        StringBuilder displayPathBuilder = new StringBuilder(parentPathDisplay);
        if (!parentPathDisplay.endsWith("/")) {
            displayPathBuilder.append('/');
        }
        displayPathBuilder.append(name);
        if (isDir) {
            displayPathBuilder.append('/');
        }
        return displayPathBuilder.toString();
    }

    @Override
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean overwrite) throws RepositoryException, FileAlreadyExistsException {
        CreateFolderResult opResult = null;

        if (!StringUtils.hasText(folderPathId) || !StringUtils.hasText(folderName) || !StringUtils.hasText(folderPathDisplay)) {
            throw new RepositoryFolderAccessException("Empty folderName or folderPathId or folderPathDisplay");
        } else {
            if (!isFolderNameValid(folderName)) {
                throw new InvalidFileNameException("Invalid name: " + folderName);
            }
            try {
                String folderFilePath = new StringBuilder(folderPathId).append(folderName.toLowerCase()).append("/").toString();
                String folderFilePathDisplay = getDisplayPathWithSeparatorFixed(folderPathDisplay, folderName, true);
                File newFolder = new File(getRepoFolder(), folderFilePath);
                FileUtils.mkdir(newFolder);
                opResult = new CreateFolderResult(folderFilePath, folderFilePathDisplay, folderName, new Date(newFolder.lastModified()));
            } catch (IOException e) {
                logger.error("Unable to create folder (folder: {}, repository ID: {}): {}", folderName, getRepoId(), e.getMessage(), e);
                handleRepoException(e);
            }
        }
        return opResult;
    }

    public OperationResult copyFile(String fromObject, String toObject) {
        return null;
    }

    public List<OperationResult> copyFolder(String fromObject, String toObject) {
        return null;
    }

    @Override
    public OperationResult moveFile(String fromObject, String toObject) throws ForbiddenOperationException {
        return null;
    }

    @Override
    public List<OperationResult> moveFolder(String fromObject, String toObject) throws RepositoryException,
            ForbiddenOperationException {
        return null;
    }

    @Override
    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException {
        if (objectPathDisplay == null || objectPathId == null) {
            throw new IllegalArgumentException("objectPathId and objectPathDisplay cannot be null!");
        }
        return objectPathId.substring(0, objectPathId.lastIndexOf('/') + 1);
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        return getCurrentFolderPathId(objectPathId, objectPathDisplay);
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    @Override
    public String getRepoName() {
        return repoName;
    }

    @Override
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {

        return null;
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        throw new RepositoryException(e.getMessage(), e);
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    private boolean isFolderNameValid(String folderName) {
        Pattern pattern = Pattern.compile("^[\\u00C0-\\u1FFF\\u2C00-\\uD7FF\\w- ]*$");
        Matcher matcher = pattern.matcher(folderName);
        return StringUtils.hasText(folderName) && matcher.matches() && folderName.length() <= 255;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String getAccountId() {
        return null;
    }

    @Override
    public void setAccountId(String accountId) {
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }
}
