package com.nextlabs.rms.repository.sharepoint.onpremise;

import com.google.gson.Gson;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.OperationResult;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.sharepoint.SPRestServiceException;
import com.nextlabs.rms.repository.sharepoint.SharePointClient;
import com.nextlabs.rms.repository.sharepoint.SharePointRepoAuthHelper;
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
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class SharePointOnPremiseRepository implements IRepository {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private String accountName;
    private String repoName;
    protected ServiceProviderType repoType = ServiceProviderType.SHAREPOINT_ONPREMISE;
    private String repoId;
    private RMSUserPrincipal user;
    private final Map<String, Object> attributes;
    private final ServiceProviderSetting setting;
    private SharePointClient spClient;
    private boolean isShared;
    private String accountId;
    private String bucketName;

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

    public SharePointOnPremiseRepository(RMSUserPrincipal userPrincipal, String repoId,
        ServiceProviderSetting setting) {
        this.user = userPrincipal;
        this.repoId = repoId;
        this.setting = setting;
        attributes = new HashMap<>();
        Iterator<Entry<String, String>> it = setting.getAttributes().entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            attributes.put(next.getKey(), next.getValue());
        }
        spClient = new SharePointClient(SharePointClient.AUTH_TYPE.NTLM, SharePointClient.RESPONSE_TYPE.JSON_VERBOSE, false, ServiceProviderType.SHAREPOINT_ONPREMISE);
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

    private HttpClientContext getNtlmClientContext() {
        try {
            String cacheKey = UserAttributeCacheItem.getKey(user.getUserId(), user.getClientId());
            UserAttributeCacheItem item = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
            if (item != null) {
                String userName;
                String password;
                String domainName = item.getUserAttributes().get(UserAttributeCacheItem.ADDOMAIN).get(0);
                if (StringUtils.hasText(bucketName)) {
                    userName = setting.getAttributes().get(ServiceProviderSetting.USER_NAME);
                    password = setting.getAttributes().get(ServiceProviderSetting.USER_SECRET);
                } else {
                    userName = item.getUserAttributes().get(UserAttributeCacheItem.ADUSERNAME).get(0);
                    password = UserAttributeCacheItem.decrypt(item.getUserAttributes().get(UserAttributeCacheItem.ADPASS).get(0));
                }
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(userName, password, "myworkstation", domainName));
                HttpClientContext context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);
                return context;
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error occured while retrieving decrypted password from cache");
        }
        return null;
    }

    @Override
    public List<RepositoryContent> getFileList(String pathId, FilterOptions filterOptions) throws RepositoryException {
        try {
            pathId = toFullFilePath(pathId);
            Set<String> favoriteFileIdSet = null;
            try (DbSession session = DbSession.newSession()) {
                String parentFileId = RepositoryFileManager.getFileId(pathId);
                favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), parentFileId);
            }
            String spServer = getSiteUrl();
            List<RepositoryContent> fileList = spClient.getFileList(pathId, getNtlmClientContext(), spServer, repoId, repoName, filterOptions);
            for (int i = 0; i < fileList.size(); i++) {
                fileList.get(i).setFavorited(favoriteFileIdSet.contains(fileList.get(i).getFileId()));
            }
            return fileList;
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IO Error occured while listing file from SP on-premise (repository ID: {}, path: '{}'): {}", getRepoId(), pathId, e.getMessage(), e);
            }
            SharePointRepoAuthHelper.handleException(e);
        } catch (Exception e) {
            LOGGER.error("Error occured while listing file from SP on-premise (repository ID: {}, path: '{}'): {}", getRepoId(), pathId, e.getMessage(), e);
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
        int searchlimit = 500;
        if (searchlimit <= 0) {
            searchlimit = 500;
        }
        Set<String> favoriteFileIdSet = null;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        try {
            String spServer = getSiteUrl();
            List<RepositoryContent> fileList = spClient.search(searchString, getNtlmClientContext(), searchlimit, repoName, repoId, repoType, spServer);
            for (int i = 0; i < fileList.size(); i++) {
                fileList.get(i).setFavorited(favoriteFileIdSet.contains(fileList.get(i).getFileId()));
            }
            return fileList;
        } catch (IOException e) {
            SharePointRepoAuthHelper.handleException(e);
            LOGGER.error("Error performing search on SP on-premise repo. Id: " + repoId, e);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    @Override
    public String getRepoId() {
        return repoId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        if (!StringUtils.hasText(fileId) && !StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        File file = null;
        boolean error = true;
        try {
            String spServer = getSiteUrl();
            String fileName = FileUtils.getName(filePath);
            file = new File(outputPath, fileName);
            filePath = toFullFilePath(fileId);
            InputStream input = null;
            try (OutputStream os = new FileOutputStream(file)) {
                input = spClient.downloadAsStream(filePath, getNtlmClientContext(), spServer);
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
                LOGGER.debug("IO Error occured while downloading file from SP on-premise (repository ID: {}, path: '{}'): {}", getRepoId(), filePath, e.getMessage(), e);
            }
            SharePointRepoAuthHelper.handleException(e);
        } catch (Exception e) {
            LOGGER.error("Error occured while downloading file from SP on-premise (repository ID: {}, path: '{}'): {}", getRepoId(), filePath, e.getMessage(), e);
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

    private String toFullFilePath(String filePath) throws IOException {
        if (StringUtils.hasText(bucketName)) {
            //Convert Document Library title to root folder
            String title = bucketName.split("/")[0];
            String rootFolder = (String)attributes.get(ServiceProviderSetting.OD4B_ROOTFOLDER);
            if (!StringUtils.hasText(rootFolder)) {
                rootFolder = spClient.getRootFolderNameByTitle(getNtlmClientContext(), getSiteUrl(), title);
                attributes.put(ServiceProviderSetting.OD4B_ROOTFOLDER, rootFolder);
                try (DbSession session = DbSession.newSession()) {
                    Criteria criteria = session.createCriteria(StorageProvider.class);
                    criteria.add(Restrictions.eq("tenantId", setting.getTenantId()));
                    criteria.add(Restrictions.eq("type", setting.getProviderType().ordinal()));
                    StorageProvider sp = (StorageProvider)criteria.uniqueResult();
                    session.beginTransaction();
                    Gson gson = new Gson();
                    sp.setAttributes(gson.toJson(attributes));
                    session.save(sp);
                    session.commit();
                }
            }
            bucketName = bucketName.replaceFirst(title, rootFolder);
            filePath = "/" + bucketName + "/" + repoId + filePath;
        }
        return filePath;
    }

    private String toSimplifiedFilePath(String filePath) {
        if (StringUtils.hasText(bucketName)) {
            String prefix = "/" + bucketName + "/" + repoId;
            if (filePath.indexOf(prefix) >= 0) {
                filePath = filePath.substring(filePath.indexOf(prefix) + prefix.length(), filePath.length());
            }
        }
        return filePath;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException {
        if (!StringUtils.hasText(filePath) || !StringUtils.hasText(localFile)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        String fileName = new File(localFile).getName();
        if (RegularExpressions.OD4B_INVALIDCHARACTERS_FILENAME_PATTERN.matcher(fileName).matches()) {
            throw new InvalidFileNameException("Invalid name: " + fileName);
        }
        try {
            filePath = toFullFilePath(filePath);
            String serverName = getSiteUrl();
            UploadedFileMetaData metaData = spClient.uploadFile(serverName, getNtlmClientContext(), filePath, localFile, overwrite, conflictFileName, StringUtils.hasText(bucketName) ? true : false);
            metaData.setPathDisplay(toSimplifiedFilePath(metaData.getPathDisplay()));
            metaData.setPathId(metaData.getPathDisplay().toLowerCase());
            metaData.setRepoId(getRepoId());
            metaData.setRepoName(getRepoName());
            if (customMetadata != null) {
                metaData.setCreatedBy(customMetadata.get("createdBy"));
            }
            if (customMetadata != null && customMetadata.get("lastModified") != null) {
                metaData.setLastModifiedTime(new Date(Long.parseLong(customMetadata.get("lastModified"))));
            } else {
                metaData.setLastModifiedTime(new Date());
            }
            return metaData;
        } catch (IOException e) {
            LOGGER.error("Error occured while uploading file to SP on-premise: ", e);
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
            filePathDisplay = toFullFilePath(filePathDisplay);
            spClient.delete(serverName, getNtlmClientContext(), filePathDisplay);
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
            fileId = toFullFilePath(fileId);
            receivedBytes = spClient.getPartialItem(fileId, startByte, endByte, spServer, getNtlmClientContext());
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
            folderPathDisplay = toFullFilePath(folderPathDisplay);
            String serverName = getSiteUrl();
            CreateFolderResult result = spClient.createFolder(folderName, folderPathDisplay, getNtlmClientContext(), serverName, StringUtils.hasText(bucketName) ? true : false);
            result.setPathDisplay(toSimplifiedFilePath(result.getPathDisplay()));
            result.setPathId(result.getPathDisplay().toLowerCase());
            return result;
        } catch (RepositoryException | SPRestServiceException | IOException e) {
            SharePointRepoAuthHelper.handleException(e);
        }
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
            return spClient.getPublicURL(serverName, getNtlmClientContext(), fileId);
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
    public ServiceProviderSetting getSetting() {
        return setting;
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        SharePointRepoAuthHelper.handleException(e);
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }
}
