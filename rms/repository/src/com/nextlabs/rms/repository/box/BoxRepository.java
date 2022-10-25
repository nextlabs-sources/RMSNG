package com.nextlabs.rms.repository.box;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIConnectionListener;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxItem.Info;
import com.box.sdk.BoxSearch;
import com.box.sdk.BoxSearchParameters;
import com.box.sdk.PartialCollection;
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
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BoxRepository implements IRepository {

    private String repoId;
    private RMSUserPrincipal userPrincipal;
    private final ServiceProviderSetting setting;
    private final Map<String, Object> attributes;
    private String accountName;
    private String accountId;
    private String repoName;
    private ServiceProviderType repoType = ServiceProviderType.BOX;

    public BoxRepository(RMSUserPrincipal userPrincipal, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = userPrincipal;
        this.setting = setting;
        this.repoId = repoId;
        attributes = new HashMap<String, Object>();
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
    public String getAccountName() {
        return this.accountName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String getAccountId() {
        return this.accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
        return ServiceProviderType.BOX;
    }

    @Override
    public boolean isShared() {
        return false;
    }

    @Override
    public void setShared(boolean shared) {

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

    private BoxAPIConnection getAPIConnection() throws InvalidTokenException {
        String refreshToken = (String)attributes.get(RepositoryManager.REFRESH_TOKEN);
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidTokenException("Invalid token");
        }
        String clientID = (String)setting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String clientSecret = (String)setting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        BoxAPIConnectionListener listener = new BoxAPIConnectionListener() {

            @Override
            public void onRefresh(BoxAPIConnection api) {
                String token = api.getRefreshToken();
                String state = api.save();
                BoxOAuthHandler.updateState(getRepoId(), getUser().getUserId(), state, token);
                attributes.put(RepositoryManager.REFRESH_TOKEN, token);
                attributes.put(RepositoryManager.BOX_REPOSIOTRY_STATE, state);
            }

            @Override
            public void onError(BoxAPIConnection api, BoxAPIException error) {

            }
        };
        String state = (String)attributes.get(RepositoryManager.BOX_REPOSIOTRY_STATE);
        BoxAPIConnection connection = null;
        if (!StringUtils.hasText(state)) {
            state = BoxOAuthHandler.getState(repoId);
        }
        if (!StringUtils.hasText(state)) {
            connection = new BoxAPIConnection(clientID, clientSecret, null, refreshToken);
        } else {
            connection = BoxAPIConnection.restore(clientID, clientSecret, state);
        }
        connection.addListener(listener);
        return connection;
    }

    @Override
    public List<RepositoryContent> getFileList(String path, FilterOptions filterOptions) throws RepositoryException {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Invalid path");
        }
        List<RepositoryContent> result = new ArrayList<RepositoryContent>();
        BoxAPIConnection connection = getAPIConnection();
        BoxFolder api = null;
        if ("/".equalsIgnoreCase(path)) {
            api = BoxFolder.getRootFolder(connection);
        } else {
            api = new BoxFolder(connection, path);
        }
        try {
            BoxItem.Info pathInfo = api.getInfo();
            StringBuilder parentPathBuilder = new StringBuilder("/");
            while (pathInfo.getParent() != null) {
                parentPathBuilder.insert(0, pathInfo.getName()).insert(0, '/');
                pathInfo = pathInfo.getParent();
            }

            Iterable<Info> it = api.getChildren(BoxFolder.ALL_FIELDS);
            for (BoxItem.Info itemInfo : it) {
                RepositoryContent content = new RepositoryContent();
                if (itemInfo instanceof BoxFile.Info) {
                    BoxFile.Info fileInfo = (BoxFile.Info)itemInfo;
                    boolean isNxl = FileUtils.getRealFileExtension(fileInfo.getName()).equalsIgnoreCase(Constants.NXL_FILE_EXTN);
                    if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                        continue;
                    }
                    String fileExtension = RepositoryFileUtil.getOriginalFileExtension(fileInfo.getName());
                    content.setFileSize(fileInfo.getSize());
                    content.setFileType(fileExtension);
                    content.setProtectedFile(isNxl);
                    content.setFileSize(fileInfo.getSize());
                }
                content.setFileId(itemInfo.getID());
                content.setUsePathId(true);
                content.setLastModifiedTime(itemInfo.getContentModifiedAt() != null ? itemInfo.getContentModifiedAt().getTime() : null);
                content.setFolder(itemInfo instanceof BoxFolder.Info);
                content.setName(itemInfo.getName());
                content.setPath(parentPathBuilder.toString() + itemInfo.getName());
                content.setPathId(itemInfo.getID());
                content.setRepoId(getRepoId());
                content.setRepoName(getRepoName());
                content.setRepoType(ServiceProviderType.BOX);
                result.add(content);
            }
        } catch (BoxAPIException e) {
            BoxOAuthHandler.handleException(e);
        }
        return result;
    }

    @Override
    public List<RepositoryContent> getFileList(String path) throws RepositoryException {
        return getFileList(path, new FilterOptions());
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        return null;
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {

    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        BoxAPIConnection api = getAPIConnection();
        BoxFile file = new BoxFile(api, fileId);
        BoxFile.Info info;
        try {
            info = file.getInfo();
        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                FileNotFoundException ex = new FileNotFoundException("Unable to retrieve file in " + fileId);
                ex.initCause(e);
                throw ex;
            } else {
                throw e;
            }
        }
        File f = new File(outputPath, info.getName());
        try (FileOutputStream fos = new FileOutputStream(f)) {
            file.download(fos);
        } catch (IOException e) {
            BoxOAuthHandler.handleException(e);
        }
        return f;
    }

    @Override
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        Set<String> favoriteFileIdSet = null;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        try {
            BoxAPIConnection connection = getAPIConnection();
            BoxSearch bs = new BoxSearch(connection);
            BoxSearchParameters bsp = new BoxSearchParameters(searchString);
            bsp.setContentTypes(Arrays.asList("name"));
            PartialCollection<BoxItem.Info> results = bs.searchRange(0, 1000, bsp);
            for (BoxItem.Info itemInfo : results) {
                RepositoryContent result = new RepositoryContent();
                if (itemInfo instanceof BoxFile.Info) {
                    result.setFolder(false);
                    result.setFileSize(itemInfo.getSize());
                    result.setLastModifiedTime(itemInfo.getContentModifiedAt() != null ? itemInfo.getContentModifiedAt().getTime() : null);
                    result.setFileType(RepositoryFileUtil.getOriginalFileExtension(itemInfo.getName()));
                    result.setProtectedFile(FileUtils.getRealFileExtension(itemInfo.getName()).equals(Constants.NXL_FILE_EXTN));
                    result.setFavorited(favoriteFileIdSet.contains(itemInfo.getID()));
                } else {
                    result.setFolder(true);
                }
                result.setFileId(itemInfo.getID());
                result.setPathId(itemInfo.getID());
                result.setName(itemInfo.getName());
                BoxItem.Info pathInfo = itemInfo.getParent();
                StringBuilder parentPathBuilder = new StringBuilder("/");
                while (pathInfo.getParent() != null) {
                    parentPathBuilder.insert(0, pathInfo.getName()).insert(0, '/');
                    pathInfo = pathInfo.getParent();
                }
                result.setPath(parentPathBuilder.toString() + itemInfo.getName());
                result.setRepoId(repoId);
                result.setRepoType(repoType);
                result.setRepoName(repoName);
                result.setUsePathId(true);
                contentList.add(result);

            }
        } catch (BoxAPIException e) {
            BoxOAuthHandler.handleException(e);
        }
        return contentList;
    }

    @Override
    public List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException {

        return null;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata) throws RepositoryException {

        return null;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath)
            throws RepositoryException, ForbiddenOperationException {

        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }

        try {

            BoxAPIConnection api = getAPIConnection();
            BoxFile file = new BoxFile(api, fileId);

            if (fileId == null || fileId.length() == 0) {
                throw new IllegalArgumentException("Invalid file path");
            }

            try (ByteArrayOutputStream bosFileRange = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                file.downloadRange(bosFileRange, startByte, endByte);
                return bosFileRange.toByteArray();
            }
        } catch (BoxAPIException | IOException e) {
            BoxOAuthHandler.handleException(e);
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
    public OperationResult moveFile(String fromObject, String toObject)
            throws RepositoryException, ForbiddenOperationException {

        return null;
    }

    @Override
    public List<OperationResult> moveFolder(String fromObject, String toObject)
            throws RepositoryException, ForbiddenOperationException {

        return null;
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {

        return null;
    }

    @Override
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename) throws RepositoryException, FileAlreadyExistsException {

        return null;
    }

    @Override
    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException {

        return null;
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        return null;
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        BoxOAuthHandler.handleException(e);
    }

    @Override
    public ServiceProviderSetting getSetting() {
        return setting;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }
}
