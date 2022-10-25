package com.nextlabs.rms.repository.sharepoint.online;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
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
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.sharepoint.SPRestServiceException;
import com.nextlabs.rms.repository.sharepoint.SharePointClient;
import com.nextlabs.rms.repository.sharepoint.SharePointRepoAuthHelper;
import com.nextlabs.rms.repository.sharepoint.response.SharePointTokenResponse;
import com.nextlabs.rms.shared.LogConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SharePointOnlineRepository implements IRepository {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private String accountName;
    private String repoName;
    private ServiceProviderType repoType = ServiceProviderType.SHAREPOINT_ONLINE;
    private String repoId;
    private RMSUserPrincipal user;
    private final Map<String, Object> attributes;
    private SharePointClient spClient;
    private final ServiceProviderSetting setting;
    private boolean isShared;
    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public SharePointOnlineRepository(RMSUserPrincipal userPrincipal, String repoId, ServiceProviderSetting setting) {
        this.user = userPrincipal;
        this.repoId = repoId;
        this.setting = setting;
        attributes = new HashMap<>();
        Iterator<Entry<String, String>> it = setting.getAttributes().entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            attributes.put(next.getKey(), next.getValue());
        }
        spClient = new SharePointClient(SharePointClient.AUTH_TYPE.OAUTH, SharePointClient.RESPONSE_TYPE.JSON_BASIC, true, ServiceProviderType.SHAREPOINT_ONLINE);
    }

    @Override
    public RMSUserPrincipal getUser() {
        return user;
    }

    @Override
    public void setUser(RMSUserPrincipal userPrincipal) {
        this.user = userPrincipal;
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
    public ServiceProviderType getRepoType() {
        return repoType;
    }

    @Override
    public String getRepoName() {
        return repoName;
    }

    @Override
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    private String getSiteUrl() {
        String spServer;
        try {
            String decodedURL = URLDecoder.decode(getAccountName(), "UTF-8");
            URL url = new URL(decodedURL);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            spServer = uri.toString();
        } catch (URISyntaxException | MalformedURLException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return spServer;
    }

    @Override
    public List<RepositoryContent> getFileList(String pathId) throws RepositoryException {
        return getFileList(pathId, new FilterOptions());
    }

    @Override
    public List<RepositoryContent> getFileList(String pathId, FilterOptions filterOptions) throws RepositoryException {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                throw new InvalidTokenException("No access token");
            }
            Set<String> favoriteFileIdSet = null;
            try (DbSession session = DbSession.newSession()) {
                String parentFileId = RepositoryFileManager.getFileId(pathId);
                favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), parentFileId);
            }
            String spServer = getSiteUrl();
            List<RepositoryContent> fileList = spClient.getFileList(pathId, accessToken, spServer, repoId, repoName, filterOptions);
            for (int i = 0; i < fileList.size(); i++) {
                fileList.get(i).setFavorited(favoriteFileIdSet.contains(fileList.get(i).getFileId()));
            }
            return fileList;
        } catch (RepositoryException e) {
            SharePointRepoAuthHelper.handleException(e);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IO Error occured while listing file from SPOL (repository ID: {}, path: '{}'): {}", getRepoId(), pathId, e.getMessage(), e);
            }
            SharePointRepoAuthHelper.handleException(e);
        } catch (Exception e) {
            LOGGER.error("Error occured while listing file from SPOL (repository ID: {}, path: '{}'): {}", getRepoId(), pathId, e.getMessage(), e);
            SharePointRepoAuthHelper.handleException(e);
        }
        return null;
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        getFileList(path);
    }

    @Override
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        //TODO:  Make this configurable
        int searchlimit = 5000;
        if (searchlimit <= 0) {
            searchlimit = 5000;
        }
        Set<String> favoriteFileIdSet = null;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        try {
            String spServer = getSiteUrl();
            String accessToken = getAccessToken();
            List<RepositoryContent> fileList = spClient.search(searchString, accessToken, searchlimit, repoName, repoId, repoType, spServer);
            for (int i = 0; i < fileList.size(); i++) {
                fileList.get(i).setFavorited(favoriteFileIdSet.contains(fileList.get(i).getFileId()));
            }
            return fileList;
        } catch (IOException e) {
            SharePointRepoAuthHelper.handleException(e);
            LOGGER.error("Error performing search on sharepoint online repo. Id: " + repoId, e);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    private String getAccessToken() throws IOException, RepositoryException {
        if (!StringUtils.hasText((String)getAttributes().get(RepositoryManager.REFRESH_TOKEN))) {
            throw new InvalidTokenException("No refresh token");
        }
        String oAuthToken = (String)attributes.get(RepositoryManager.ACCESS_TOKEN);
        long expiryTime = 0L;
        if (oAuthToken != null && attributes.get(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME) != null) {
            expiryTime = Long.parseLong((String)attributes.get(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME));
            if ((System.currentTimeMillis() / 1000) >= expiryTime) {
                //Get new access token
                oAuthToken = getSPOnlineOAuthToken();
            }
        } else {
            //Get new access token from refresh token
            oAuthToken = getSPOnlineOAuthToken();
        }

        return oAuthToken;
    }

    private String getSPOnlineOAuthToken() throws IOException, RepositoryException {
        String secret = (String)getAttributes().get(ServiceProviderSetting.APP_SECRET);
        String contextId = (String)getAttributes().get(ServiceProviderSetting.SP_ONLINE_APP_CONTEXT_ID);
        SharePointTokenResponse tokenResponse = SharePointRepoAuthHelper.getAccessTokenFromRefreshToken(secret, contextId, (String)getAttributes().get(RepositoryManager.REFRESH_TOKEN), accountName);
        String accessToken = tokenResponse.getAccessToken();
        String expiryTime = String.valueOf(tokenResponse.getExpiresOn());
        attributes.put(RepositoryManager.ACCESS_TOKEN, accessToken);
        attributes.put(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME, expiryTime);
        return accessToken;
    }

    @Override
    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    @Override
    public String getRepoId() {
        return repoId;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        if (!StringUtils.hasText(fileId) || !StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        File file = null;
        boolean error = true;
        try {
            String accessToken = getAccessToken();
            String spServer = getSiteUrl();
            String fileName = FileUtils.getName(filePath);
            file = new File(outputPath, fileName);
            InputStream input = null;
            try (OutputStream os = new FileOutputStream(file)) {
                input = spClient.downloadAsStream(fileId, accessToken, spServer);
                IOUtils.copy(input, os);
            } finally {
                IOUtils.closeQuietly(input);
            }
            error = false;
            return file;
        } catch (SPRestServiceException | RepositoryException e) {
            SharePointRepoAuthHelper.handleException(e);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IO Error occured while downloading file from SPOL (repository ID: {}, path: '{}'): {}", getRepoId(), filePath, e.getMessage(), e);
            }
            SharePointRepoAuthHelper.handleException(e);
        } catch (Exception e) {
            LOGGER.error("Error occured while downloading file from SPOL (repository ID: {}, path: '{}'): {}", getRepoId(), filePath, e.getMessage(), e);
            SharePointRepoAuthHelper.handleException(e);
        } finally {
            if (error) {
                FileUtils.deleteQuietly(file);
            }
        }
        return null;
    }

    @Override
    public List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws RepositoryException, com.nextlabs.rms.repository.exception.MissingDependenciesException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException {
        if (!StringUtils.hasText(filePathId) || !StringUtils.hasText(localFile)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        try {
            String accessToken = getAccessToken();
            String serverName = getSiteUrl();
            UploadedFileMetaData metaData = spClient.uploadFile(serverName, accessToken, filePathId, localFile, overwrite, conflictFileName, false);
            metaData.setRepoId(getRepoId());
            metaData.setRepoName(getRepoName());
            return metaData;
        } catch (IOException e) {
            LOGGER.error("Error occured while uploading file to SP online: ", e);
            SharePointRepoAuthHelper.handleException(e);
        }
        return null;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePathDisplay) throws RepositoryException,
            ForbiddenOperationException {
        if ("/".equals(filePathId)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleting root pathId {} is forbidden", filePathId);
            }
            throw new ForbiddenOperationException("Invalid pathId: " + filePathId);
        }
        String serverName = getSiteUrl();
        try {
            String accessToken = getAccessToken();
            spClient.delete(serverName, accessToken, filePathId);
        } catch (IOException e) {
            SharePointRepoAuthHelper.handleException(e);
        } catch (SPRestServiceException e) {
            SharePointRepoAuthHelper.handleException(e);
        }
        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        byte[] receivedBytes = null;
        try {
            String spServer = getSiteUrl();
            String accessToken = getAccessToken();
            receivedBytes = spClient.getPartialItem(fileId, startByte, endByte, spServer, accessToken);
        } catch (RepositoryException | IOException e) {
            SharePointRepoAuthHelper.handleException(e);
        }
        return receivedBytes;
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
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename)
            throws RepositoryException {
        try {
            String accessToken = getAccessToken();
            String serverName = getSiteUrl();
            spClient.createFolder(folderName, folderPathId, accessToken, serverName, false);
        } catch (RepositoryException | SPRestServiceException | IOException e) {
            SharePointRepoAuthHelper.handleException(e);
        }
        /*
         * TODO change spClient later to return CreateFolderResult
         */
        return null;
    }

    @Override
    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException {
        return getCurrentFolderPathDisplay(objectPathId, objectPathDisplay);
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        if (StringUtils.equals("/", objectPathDisplay)) {
            // current object resides in the root folder
            return "";
        } else {
            return objectPathDisplay.substring(0, objectPathDisplay.lastIndexOf('/'));
        }
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {
        String serverName = getSiteUrl();
        try {
            String accessToken = getAccessToken();
            return spClient.getPublicURL(serverName, accessToken, fileId);
        } catch (IOException e) {
            SharePointRepoAuthHelper.handleException(e);
        }
        return null;
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        SharePointRepoAuthHelper.handleException(e);
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
