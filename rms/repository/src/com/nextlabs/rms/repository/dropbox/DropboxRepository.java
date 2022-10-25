package com.nextlabs.rms.repository.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxRequestConfig.Builder;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.DownloadBuilder;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.OperationResult;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropboxRepository implements IRepository {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private RMSUserPrincipal userPrincipal;
    private String repoId;
    private ServiceProviderType repoType = ServiceProviderType.DROPBOX;
    private String repoName;
    private String accountName;
    private boolean isShared;
    private final ServiceProviderSetting setting;
    private final Map<String, Object> attributes;
    private String accountId;

    public DropboxRepository(RMSUserPrincipal user, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = user;
        this.setting = setting;
        this.repoId = repoId;
        attributes = new HashMap<String, Object>();
    }

    public ServiceProviderSetting getSetting() {
        return setting;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
    public List<RepositoryContent> getFileList(String path) throws RepositoryException {
        return getFileList(path, new FilterOptions());
    }

    private DbxRequestConfig buildRequestConfig(HttpRequestor httpRequestor) {
        Builder builder = DbxRequestConfig.newBuilder(RepoConstants.RMS_CLIENT_IDENTIFIER);
        builder.withUserLocaleFrom(Locale.getDefault());
        if (httpRequestor != null) {
            builder.withHttpRequestor(httpRequestor);
        }
        return builder.build();
    }

    private DbxClientV2 getClient(DbxRequestConfig config) throws InvalidTokenException {
        String refreshToken = (String)attributes.get(RepositoryManager.REFRESH_TOKEN);
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidTokenException("No access token");
        }
        return new DbxClientV2(config, refreshToken);
    }

    @Override
    public List<RepositoryContent> getFileList(String path, FilterOptions filterOptions) throws RepositoryException {
        DbxClientV2 client = getClient(buildRequestConfig(null));
        String pathDisplay = "";
        ListFolderResult result = null;
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        Set<String> favoriteFileIdSet = null;
        try (DbSession session = DbSession.newSession()) {
            String parentFileId = RepositoryFileManager.getFileId(path);
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), parentFileId);
        }
        try {
            /*
             * for dropbox, we have to pass empty string to list/search files in root folder
             */

            if (path.startsWith("TempId")) {
                pathDisplay = path.substring(path.indexOf(':') + 1);
            } else if (!"/".equals(path)) {
                pathDisplay = path;
            }
            result = client.files().listFolder(pathDisplay);

            while (true) {

                for (Metadata metadata : result.getEntries()) {
                    RepositoryContent content = new RepositoryContent();

                    if (metadata instanceof FileMetadata) {
                        if (filterOptions.showOnlyFolders()) {
                            continue;
                        }
                        FileMetadata fileMetadata = (FileMetadata)metadata;
                        boolean isNxl = FileUtils.getRealFileExtension(fileMetadata.getName()).equals(Constants.NXL_FILE_EXTN);
                        if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                            continue;
                        }
                        content.setFileId(fileMetadata.getId());
                        content.setFavorited(favoriteFileIdSet.contains(fileMetadata.getId()));
                        content.setFolder(false);
                        content.setPathId(fileMetadata.getId());
                        content.setFileSize(fileMetadata.getSize());
                        content.setLastModifiedTime(fileMetadata.getServerModified().getTime());
                        content.setFileType(RepositoryFileUtil.getOriginalFileExtension(fileMetadata.getName()));
                        content.setProtectedFile(isNxl);
                    } else if (metadata instanceof FolderMetadata) {
                        content.setFolder(true);
                        FolderMetadata folderMetadata = (FolderMetadata)metadata;
                        content.setPathId(folderMetadata.getId());
                        content.setFileId(folderMetadata.getId());
                    } else {
                        // DeletedMetaData
                        /**
                         * Indicates that there used to be a file or folder at
                         * this path, but it no longer exists.
                         */
                        continue;
                    }
                    content.setUsePathId(true);
                    content.setName(metadata.getName());
                    content.setPath(metadata.getPathDisplay());
                    content.setRepoId(getRepoId());
                    content.setRepoName(getRepoName());
                    content.setRepoType(ServiceProviderType.DROPBOX);

                    contentList.add(content);
                }

                if (!result.getHasMore()) {
                    break;
                }
                result = client.files().listFolderContinue(result.getCursor());
            }

        } catch (DbxException e) {
            DropBoxOAuthHandler.handleException(e);
        }
        return contentList;
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        File file = null;
        BufferedOutputStream outStr = null;
        DbxDownloader<FileMetadata> dropboxFile = null;
        try {
            if (fileId == null || fileId.length() == 0) {
                throw new IllegalArgumentException("Invalid file path");
            }
            DbxClientV2 clientLocal = getClient(buildRequestConfig(getExtendedTimeoutHttpRequestor()));

            dropboxFile = clientLocal.files().download(fileId);
            if (dropboxFile == null) {
                throw new FileNotFoundException("Unable to retrieve file in " + fileId);
            }

            FileMetadata metadata = dropboxFile.getResult();
            file = new File(outputPath, metadata.getName());
            outStr = new BufferedOutputStream(new FileOutputStream(file));
            dropboxFile.download(outStr);
            return file;
        } catch (DbxException | IOException e) {
            DropBoxOAuthHandler.handleException(e);
        } finally {
            if (outStr != null) {
                try {
                    outStr.close();
                    outStr = null;
                } catch (IOException e) {
                    logger.error("Error occurred when closing output stream");
                }
            }
            if (dropboxFile == null && file != null && file.exists()) {
                FileUtils.deleteQuietly(file);
            }
        }
        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }
        try {
            if (fileId == null || fileId.length() == 0) {
                throw new IllegalArgumentException("Invalid file path");
            }
            DbxClientV2 clientLocal = getClient(buildRequestConfig(getExtendedTimeoutHttpRequestor()));
            DownloadBuilder downloadBuilder = clientLocal.files().downloadBuilder(fileId);
            try (DbxDownloader<FileMetadata> dropboxFile = downloadBuilder.range(startByte, endByte - startByte + 1).start()) {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                    IOUtils.copy(dropboxFile.getInputStream(), os);
                    return os.toByteArray();
                }
            }
        } catch (DbxException | IOException e) {
            DropBoxOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public OperationResult copyFile(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public List<OperationResult> copyFolder(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public OperationResult moveFile(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public List<OperationResult> moveFolder(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    /*
     * Stub to upload encrypted NXL file to the dropbox
     * dest : parentfoldername+file name
     * src : file which needs to be uploaded
     */
    @Override
    public UploadedFileMetaData uploadFile(String folderPathId, String folderPathDisplay, String srcFilePath,
        boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException {
        Builder builder = DbxRequestConfig.newBuilder(RepoConstants.RMS_CLIENT_IDENTIFIER);
        builder.withUserLocaleFrom(Locale.getDefault());
        DbxRequestConfig config = builder.build();
        DbxClientV2 clientLocal = new DbxClientV2(config, (String)attributes.get(RepositoryManager.REFRESH_TOKEN));
        File uploadFile = new File(srcFilePath);
        InputStream uploadFIS = null;
        try {
            if ("/".equals(folderPathDisplay)) {
                folderPathDisplay = "";
            }
            uploadFIS = new FileInputStream(uploadFile);
            String destinationPath = null;
            WriteMode mode;
            if (overwrite) {
                destinationPath = folderPathDisplay + "/" + uploadFile.getName();
                mode = WriteMode.OVERWRITE;
            } else {
                destinationPath = folderPathDisplay + "/" + conflictFileName;
                mode = WriteMode.ADD;
            }
            FileMetadata metadata = clientLocal.files().uploadBuilder(destinationPath).withMode(mode).uploadAndFinish(uploadFIS);
            UploadedFileMetaData uploadFileMeta = new UploadedFileMetaData();
            uploadFileMeta.setPathId(metadata.getId());
            uploadFileMeta.setPathDisplay(metadata.getPathDisplay());
            uploadFileMeta.setFileNameWithTimeStamp(conflictFileName);
            uploadFileMeta.setRepoId(getRepoId());
            uploadFileMeta.setRepoName(getRepoName());
            return uploadFileMeta;
        } catch (DbxException | IOException e) {
            DropBoxOAuthHandler.handleException(e);
        } finally {
            IOUtils.closeQuietly(uploadFIS);
        }
        return null;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException {
        if ("/".equals(filePath) || "".equals(filePath)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting root path {} is forbidden", filePath);
            }
            throw new ForbiddenOperationException("Invalid filePath: " + filePath);
        }
        DbxClientV2 clientLocal = getClient(buildRequestConfig(null));
        try {
            clientLocal.files().deleteV2(filePath);
        } catch (DbxException e) {
            logger.error("Unable to delete file (path: {}): {}", filePath, e.getMessage(), e);
            DropBoxOAuthHandler.handleException(e);
        }
        return null;
    }

    private HttpRequestor getExtendedTimeoutHttpRequestor() {
        return new StandardHttpRequestor(StandardHttpRequestor.Config.builder().withConnectTimeout(RepoConstants.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS).withReadTimeout(RepoConstants.READ_TIMEOUT, TimeUnit.MILLISECONDS).build());
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        getFileList(path);
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
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        Set<String> favoriteFileIdSet = null;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        try {
            DbxClientV2 clientLocal = getClient(buildRequestConfig(null));

            com.dropbox.core.v2.files.SearchResult results = clientLocal.files().search("", searchString);

            for (SearchMatch entry : results.getMatches()) {
                RepositoryContent result = new RepositoryContent();
                Metadata mdata = entry.getMetadata();
                if (mdata instanceof FileMetadata) {
                    FileMetadata fmData = (FileMetadata)mdata;
                    result.setFileId(fmData.getId());
                    result.setPathId(fmData.getId());
                    result.setFolder(false);
                    result.setFileSize(fmData.getSize());
                    result.setLastModifiedTime(fmData.getServerModified().getTime());
                    result.setFileType(RepositoryFileUtil.getOriginalFileExtension(mdata.getName()));
                    result.setProtectedFile(FileUtils.getRealFileExtension(mdata.getName()).equals(Constants.NXL_FILE_EXTN));
                    result.setFavorited(favoriteFileIdSet.contains(fmData.getId()));
                } else if (mdata instanceof FolderMetadata) {
                    FolderMetadata fmData = (FolderMetadata)mdata;
                    result.setFileId(fmData.getId());
                    result.setPathId(fmData.getId());
                    result.setFolder(true);
                    result.setFavorited(favoriteFileIdSet.contains(fmData.getId()));
                } else {
                    continue;
                }
                result.setName(mdata.getName());
                result.setPath(mdata.getPathDisplay());
                result.setRepoId(repoId);
                result.setRepoType(repoType);
                result.setRepoName(repoName);
                result.setUsePathId(true);
                contentList.add(result);
            }
        } catch (DbxException e) {
            DropBoxOAuthHandler.handleException(e);
        }
        return contentList;
    }

    @Override
    public List<File> downloadFiles(String filePathId, String filePath, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException {
        List<File> downloadedFiles = new ArrayList<File>();
        List<String> missingFiles = new ArrayList<String>();
        String parentPath = filePath.substring(0, filePath.lastIndexOf('/'));
        if (filenames != null && filenames.length > 0) {
            for (String filename : filenames) {
                File file = null;
                try {
                    file = getFile(parentPath + "/" + filename, parentPath + "/" + filename, outputPath);
                } catch (FileNotFoundException e) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage(), e);
                    }
                }
                if (file == null || !file.exists()) {
                    missingFiles.add(filename);
                } else {
                    downloadedFiles.add(file);
                }
            }
        }
        if (!missingFiles.isEmpty()) {
            throw new MissingDependenciesException(missingFiles);
        }
        return downloadedFiles;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename)
            throws RepositoryException {
        CreateFolderResult result = null;
        if (!StringUtils.hasText(folderPathDisplay) || !StringUtils.hasText(folderName)) {
            throw new IllegalArgumentException("Empty folderName or folderPathId");
        } else {
            DbxClientV2 client = getClient(buildRequestConfig(null));
            try {
                DbxUserFilesRequests db = client.files();
                String path = null;
                if (folderPathDisplay.contentEquals("/")) {
                    path = folderPathDisplay + folderName;
                } else {
                    path = folderPathDisplay + "/" + folderName;
                }
                com.dropbox.core.v2.files.CreateFolderResult mdata = db.createFolderV2(path, false);
                FolderMetadata metadata = mdata.getMetadata();
                result = new CreateFolderResult(metadata.getId(), metadata.getPathDisplay(), metadata.getName(), new Date());
            } catch (CreateFolderErrorException e) {
                if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                    logger.debug("Folder name with the same name already exists at the path.");
                    DropBoxOAuthHandler.handleException(e, folderName);
                } else {
                    logger.debug("Error occurred while creating a folder");
                    DropBoxOAuthHandler.handleException(e);
                }
            } catch (DbxException e) {
                logger.debug("Dropbox exception - invalid access token or network issue");
                DropBoxOAuthHandler.handleException(e);
            }
        }
        return result;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    @Override
    public String getCurrentFolderPathId(String filePathId, String filePathDisplay) throws RepositoryException {
        if (filePathDisplay == null) {
            throw new IllegalArgumentException("filePathDisplay cannot be null!");
        }
        DbxClientV2 client = getClient(buildRequestConfig(null));
        Metadata metadata = null;
        String currentFolderPathId = null;
        try {
            metadata = client.files().getMetadata(filePathDisplay);
            if (metadata instanceof FolderMetadata) {
                currentFolderPathId = ((FolderMetadata)metadata).getId();
            } else if (metadata instanceof FileMetadata) {
                String currentFolderPath = filePathDisplay.substring(0, filePathDisplay.lastIndexOf('/'));
                if (currentFolderPath.isEmpty()) {
                    currentFolderPathId = "";
                    return currentFolderPathId;
                } else {
                    FolderMetadata currentFolderMetadata = (FolderMetadata)client.files().getMetadata(currentFolderPath);
                    currentFolderPathId = currentFolderMetadata.getId();
                }
            }
        } catch (DbxException e) {
            logger.debug("Dropbox exception - invalid access token or network issue");
            DropBoxOAuthHandler.handleException(e);
        }
        return currentFolderPathId;
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        String currentFoldPathId = getCurrentFolderPathId(objectPathId, objectPathDisplay);
        if (currentFoldPathId == null) {
            // current object resides in the root folder
            return "";
        }
        if (currentFoldPathId.equals(objectPathId)) {
            // current object is a folder
            return objectPathDisplay;
        } else {
            return objectPathDisplay.substring(0, objectPathDisplay.lastIndexOf('/'));
        }
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {
        DbxClientV2 client = getClient(buildRequestConfig(null));
        try {
            SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings(fileId);
            return new URIBuilder(sharedLinkMetadata.getUrl()).setParameter("dl", "1").build().toString();
        } catch (URISyntaxException e) {
            logger.error("Dropbox exception - URISyntaxException", e);
            DropBoxOAuthHandler.handleException(e);
        } catch (CreateSharedLinkWithSettingsErrorException e) {
            logger.error("Dropbox exception - CreateSharedLinkWithSettingsErrorException", e);
            DropBoxOAuthHandler.handleException(e);
        } catch (DbxException e) {
            logger.debug("Dropbox exception - invalid access token or network issue");
            DropBoxOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        DropBoxOAuthHandler.handleException(e);
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        return uploadFile(filePathId, filePath, localFile, overwrite, conflictFileName, customMetadata);
    }
}
