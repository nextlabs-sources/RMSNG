package com.nextlabs.rms.repository.onedrive;

import com.nextlabs.common.io.IOUtils;
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
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.repository.onedrive.type.OneDriveFile;
import com.nextlabs.rms.repository.onedrive.type.OneDriveFolder;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItem;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItems;
import com.nextlabs.rms.repository.onedrive.type.OneDriveParentReference;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OneDriveRepository implements IRepository, Observer {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private RMSUserPrincipal userPrincipal;
    private String repoId;
    private final ServiceProviderType repoType = ServiceProviderType.ONE_DRIVE;
    private String repoName;
    private String accountName;
    private final String clientId;
    private final String clientSecret;
    private String redirectUrl;
    private boolean isShared;
    private String accountId;
    private final OneDrivePersonalClient client;
    private final ServiceProviderSetting setting;
    private final Map<String, Object> attributes = new HashMap<>();

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public boolean isShared() {
        return isShared;
    }

    public OneDriveRepository(RMSUserPrincipal user, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = user;
        this.setting = setting;
        client = new OneDrivePersonalClient(this);
        client.addObserver(this);
        clientId = setting.getAttributes().get(ServiceProviderSetting.APP_ID);
        clientSecret = setting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        redirectUrl = setting.getAttributes().get(ServiceProviderSetting.REDIRECT_URL);
        redirectUrl = redirectUrl + (redirectUrl.endsWith("/") ? "" : "/") + RepoConstants.ONE_DRIVE_AUTH_FINISH_URL;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
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
    public String getRepoName() {
        return repoName;
    }

    @Override
    public void setRepoName(String repoName) {
        this.repoName = repoName;
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

    @Override
    public List<RepositoryContent> getFileList(String path, FilterOptions filterOptions) throws RepositoryException {
        final List<RepositoryContent> contentList = new ArrayList<>();
        List<OneDriveItems> itemList;
        boolean isPathId = !StringUtils.equals("/", path);
        if (path.startsWith("TempId")) {
            isPathId = false;
            path = path.substring(path.indexOf(':') + 1);
        }
        try {
            itemList = client.listItems(path, isPathId);
            Set<String> favoriteFileIdSet;
            try (DbSession session = DbSession.newSession()) {
                String parentFileId = RepositoryFileManager.getFileId(path);
                favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), parentFileId);
            }
            for (OneDriveItems items : itemList) {
                if (items != null) {
                    List<OneDriveItem> list = items.getValue();
                    if (list != null && !list.isEmpty()) {
                        for (OneDriveItem item : list) {
                            OneDriveFile file = item.getFile();
                            OneDriveFolder folder = item.getFolder();
                            String name = item.getName();
                            if (file != null || folder != null) {
                                if (filterOptions.showOnlyFolders() && folder == null) {
                                    continue;
                                }
                                String parentPath = item.getParentRef().getPath();
                                String pathName = URLDecoder.decode(parentPath.replaceFirst(OneDrivePersonalClient.DRIVE_ROOT, ""), "UTF-8") + "/" + name;
                                String pathId = item.getId();
                                boolean isNxl = FileUtils.getRealFileExtension(name).equals(Constants.NXL_FILE_EXTN);
                                RepositoryContent content = new RepositoryContent();
                                if (file != null) {
                                    if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                                        continue;
                                    }
                                    content.setFileSize(item.getSize());
                                    content.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                                    content.setProtectedFile(isNxl);
                                }
                                content.setFileId(item.getId());
                                content.setFavorited(favoriteFileIdSet.contains(item.getId()));
                                content.setLastModifiedTime(item.getLastModifiedTime() != null ? item.getLastModifiedTime().getTime() : null);
                                content.setFolder(folder != null);
                                content.setName(name);
                                content.setPath(pathName);
                                content.setPathId(pathId);
                                content.setUsePathId(true);
                                content.setRepoId(getRepoId());
                                content.setRepoName(getRepoName());
                                content.setRepoType(ServiceProviderType.ONE_DRIVE);
                                contentList.add(content);
                            }
                        }
                    }
                }
            }
        } catch (OneDriveServiceException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return contentList;
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    private File getFile(String fileId, String outputPath, boolean isPathId) throws RepositoryException {
        InputStream is = null;
        OutputStream os = null;
        try {
            OneDriveItem item = client.getItem(fileId, isPathId);
            OneDriveFolder folder = item.getFolder();
            if (folder != null) {
                throw new FileNotFoundException("Requested item is a folder: " + fileId);
            }
            String downloadUrl = item.getDownloadUrl();
            if (!StringUtils.hasText(downloadUrl)) {
                throw new FileNotFoundException("Download URL is not provided for requested item: " + fileId);
            }
            String fileName = item.getName();
            File file = new File(outputPath, fileName);
            is = new URL(downloadUrl).openStream();
            os = new FileOutputStream(file);
            IOUtils.copy(is, os);
            return file;
        } catch (OneDriveServiceException | RepositoryException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
        return null;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        return getFile(fileId, outputPath, true);
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        try {
            getFileList(path);
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
    }

    @Override
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        Set<String> favoriteFileIdSet;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        final List<RepositoryContent> searchResults = new ArrayList<>();
        List<OneDriveItems> itemList;
        try {
            itemList = client.searchItems("/", searchString);
            for (OneDriveItems items : itemList) {
                if (items != null) {
                    List<OneDriveItem> list = items.getValue();
                    if (list != null && !list.isEmpty()) {
                        for (OneDriveItem item : list) {
                            OneDriveFile file = item.getFile();
                            OneDriveFolder folder = item.getFolder();
                            String name = item.getName();

                            if (file != null || folder != null) {
                                String parentPath = item.getParentRef().getPath();
                                String pathId = item.getId();
                                String pathName = URLDecoder.decode(parentPath.replaceFirst(OneDrivePersonalClient.DRIVE_ROOT, ""), "UTF-8") + "/" + name;
                                RepositoryContent result = new RepositoryContent();
                                result.setFolder(folder != null);
                                result.setName(name);
                                result.setFileId(item.getId());
                                result.setPath(pathName);
                                result.setPathId(pathId);
                                result.setRepoId(repoId);
                                result.setRepoType(repoType);
                                result.setRepoName(repoName);
                                result.setFileSize(item.getSize());
                                result.setUsePathId(true);
                                result.setLastModifiedTime(item.getLastModifiedTime() != null ? item.getLastModifiedTime().getTime() : null);
                                if (!result.isFolder()) {
                                    result.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                                    result.setProtectedFile(FileUtils.getRealFileExtension(name).equals(Constants.NXL_FILE_EXTN));
                                }
                                result.setFavorited(favoriteFileIdSet.contains(item.getId()));
                                searchResults.add(result);
                            }
                        }
                    }
                }
            }
        } catch (OneDriveServiceException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return searchResults;
    }

    @Override
    public List<File> downloadFiles(String filePathId, String filePath, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException {
        List<File> downloadedFiles = new ArrayList<>();
        List<String> missingFiles = new ArrayList<>();
        String parentPath = filePath.substring(0, filePath.lastIndexOf('/'));
        try {
            for (String filename : filenames) {
                File file = null;
                try {
                    file = getFile(parentPath + "/" + filename, outputPath, false);
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
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        if (!missingFiles.isEmpty()) {
            throw new MissingDependenciesException(missingFiles);
        }
        return downloadedFiles;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePathDisplay, String srcFilePath,
        boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException {
        if ("/".equals(filePathId) || "/".equals(filePathDisplay)) {
            try {
                filePathDisplay = "";
                filePathId = client.getItem(filePathDisplay, false).getId();
            } catch (OneDriveServiceException | IOException e) {
                OneDriveOAuthHandler.handleException(e);
            }
        }
        try {
            if (filePathId.startsWith("TempId")) {
                filePathId = client.getItem(filePathDisplay, false).getId();
            }
            File localFile = new File(srcFilePath);
            UploadedFileMetaData metaData = client.upload(filePathId, filePathDisplay, localFile, overwrite, conflictFileName);
            metaData.setRepoId(getRepoId());
            metaData.setRepoName(getRepoName());
            return metaData;
        } catch (IOException e) {
            OneDriveOAuthHandler.handleException(e);
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException {
        if ("/".equals(filePathId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting root pathId {} is forbidden", filePathId);
            }
            throw new ForbiddenOperationException("Invalid pathId: " + filePathId);
        }
        try {
            client.deleteItem(filePathId, true);
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        byte[] recievedBytes = null;
        try {
            recievedBytes = client.getPartialItem(fileId, startByte, endByte);
        } catch (IOException | OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return recievedBytes;
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

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void update(Observable o, Object arg) {
        String refreshToken = (String)getAttributes().get(RepositoryManager.REFRESH_TOKEN);
        RepositoryManager.updateRefreshToken(getRepoId(), getUser().getUserId(), refreshToken);
        if (logger.isDebugEnabled()) {
            logger.debug("Update new refresh token in repository successfully (Repo ID: {})", getRepoId());
        }
    }

    @Override
    public CreateFolderResult createFolder(String folderName, String parentFolderId, String folderPathDisplay,
        boolean autoRename)
            throws RepositoryException {
        try {
            boolean isPathId = true;
            if ("/".equals(parentFolderId)) {
                parentFolderId = "";
                isPathId = false;
            }
            if (parentFolderId.startsWith("TempId")) {
                parentFolderId = client.getItem(folderPathDisplay, false).getId();
            }
            client.createFolder(folderName, parentFolderId, isPathId, autoRename);
        } catch (OneDriveServiceException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        /*
         * TODO change oneDriveClient later to return CreateFolderResult
         */
        return null;
    }

    @Override
    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException {
        String currentFolderPathId = null;
        if (objectPathId.isEmpty()) {
            throw new IllegalArgumentException("Empty object pathID!");
        }
        try {
            OneDriveItem object = client.getItem(objectPathId, true);
            if (object.getFile() != null) {
                // object is a file
                OneDriveParentReference parentFolderRef = object.getParentRef();
                currentFolderPathId = parentFolderRef.getId();
            } else {
                currentFolderPathId = objectPathId;
            }
        } catch (OneDriveServiceException | RepositoryException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return currentFolderPathId;
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        String currentFolderPathDisplay = null;
        if (objectPathDisplay.isEmpty()) {
            throw new IllegalArgumentException("Empty object pathID!");
        }
        try {
            OneDriveItem object = client.getItem(objectPathId, true);
            if (object.getFile() != null) {
                // object is a file
                OneDriveParentReference parentFolderRef = object.getParentRef();
                currentFolderPathDisplay = parentFolderRef.getPath();
            } else {
                currentFolderPathDisplay = objectPathDisplay;
            }
        } catch (OneDriveServiceException | RepositoryException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return currentFolderPathDisplay;
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {
        String url = null;
        try {
            url = client.getPublicUrl(fileId);
        } catch (OneDriveServiceException | IOException | URISyntaxException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return url;
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        OneDriveOAuthHandler.handleException(e);
    }

    @Override
    public ServiceProviderSetting getSetting() {
        return setting;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        return uploadFile(filePathId, filePath, localFile, overwrite, conflictFileName, customMetadata);
    }
}
