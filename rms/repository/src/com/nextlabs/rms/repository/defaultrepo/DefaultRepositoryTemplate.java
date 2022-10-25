package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.OperationResult;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

public abstract class DefaultRepositoryTemplate implements IRepository {

    protected static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    protected static final String STORAGE_USED = "size";

    protected String bucketName;
    protected IRMSRepositorySearcher searcher;
    protected final IRepository repo;

    protected final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public DefaultRepositoryTemplate(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        repo = getServiceProviderRepository(session, userPrincipal);
    }

    protected DefaultRepositoryTemplate(IRepository repo) {
        this.repo = repo;
    }

    private IRepository getServiceProviderRepository(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        User user = session.load(User.class, userPrincipal.getUserId());
        Tenant tenant = session.load(Tenant.class, userPrincipal.getTenantId());
        Repository repository = DefaultRepositoryManager.getDefaultRepository(session, user, tenant);
        if (repository != null) {
            StorageProvider sp = session.get(StorageProvider.class, repository.getProviderId());
            return RepositoryFactory.getInstance().createRepository(userPrincipal, repository, sp);
        }
        return null;
    }

    protected String getParentPathDisplay(final String parentFilePathId) throws RepositoryException {
        boolean folderExists = false;
        String parentPathDisplay = "/";
        try {
            if (parentFilePathId.endsWith("/")) {
                String fullPath = FilenameUtils.getFullPath(parentFilePathId);
                folderExists = "/".equals(fullPath);
                if (!folderExists) {
                    parentPathDisplay = searcher.getDisplayPath(fullPath, getRepoId());
                    folderExists = parentPathDisplay != null && !parentPathDisplay.isEmpty();
                }
                if (!folderExists && fullPath.equals(RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
                    createFolder(RepoConstants.MY_VAULT_NAME, "/", "/", true);
                    parentPathDisplay = RepoConstants.MY_VAULT_FOLDER_PATH_DISPLAY + "/";
                    folderExists = true;
                }
            }
        } catch (RMSRepositorySearchException e) {
            logger.error("Error occurred while trying to determine if path already exists for upload in default storage");
        } catch (RepositoryException e) {
            logger.error("Error occurred while creating MyVault folder");
        } catch (FileAlreadyExistsException e) {
            logger.error("MyVault folder already exits");
        }
        if (!folderExists) {
            throw new RepositoryFolderAccessException("Parent folder does not exist");
        }
        return parentPathDisplay;
    }

    private boolean pathExists(String filePathId) {
        try {
            return searcher.pathExists(getRepoId(), filePathId);
        } catch (RMSRepositorySearchException e) {
            logger.error("Error while trying to check if file already exists at {}", filePathId);
        }
        return false;
    }

    private String getPathDisplay(String filePathId) {
        if (StringUtils.equals(filePathId, "/")) {
            return "/";
        }
        try {
            return searcher.getDisplayPath(filePathId, getRepoId());
        } catch (RMSRepositorySearchException e) {
            logger.error("Error while trying to get the pathDisplay of pathId {}", filePathId);
        }
        return null;
    }

    public StoreItem getExistingStoreItem(final String filePathId) {
        try {
            return searcher.getRepoItem(getRepoId(), filePathId);
        } catch (RMSRepositorySearchException e) {
            logger.error("Error while trying to determine if duplicate file path in default storage");
        }
        return null;
    }

    private String getCurrentFolderName(String objectPathId) {
        String folderName = null;
        if ("/".equals(objectPathId)) {
            return "";
        } else if (objectPathId.contains("/")) {
            String[] parentFromPath = objectPathId.split("/");
            folderName = parentFromPath[parentFromPath.length - 1];
        }
        return folderName;
    }

    private RepositoryContent toRepositoryContent(StoreItem storeItem) {
        if (storeItem == null) {
            return null;
        }
        RepositoryContent content = new RepositoryContent();
        String pathDisplay = storeItem.getFilePathDisplay();
        if (pathDisplay == null) {
            pathDisplay = storeItem.getFilePath();
        }
        String fileName = pathDisplay.substring(pathDisplay.lastIndexOf('/') + 1);
        content.setFolder(storeItem.isDirectory());
        content.setLastModifiedTime(storeItem.getLastModified().getTime());
        content.setFileSize(storeItem.getSize());
        content.setPathId(storeItem.getFilePath());
        content.setFileId(storeItem.getFilePath());
        content.setPath(pathDisplay);
        content.setName(fileName);
        if (storeItem.getNxl() != null) {
            content.setDuid(storeItem.getNxl().getDuid());
            content.setOwner(storeItem.getNxl().getOwner());
        }
        content.setRepoId(getRepoId());
        content.setRepoName(getRepoName());
        content.setRepoType(getRepoType());
        content.setFileType(RepositoryFileUtil.getOriginalFileExtension(fileName));
        content.setProtectedFile(FileUtils.getRealFileExtension(fileName).equals(Constants.NXL_FILE_EXTN));
        content.setUsePathId(true);
        content.setDeleted(storeItem.isDeleted());
        return content;
    }

    private List<RepositoryContent> toRepositoryContentList(List<RepoItemMetadata> repoItemList,
        Set<String> favoriteFileIdSet, FilterOptions filterOptions) {
        List<RepositoryContent> contentList = new ArrayList<>();
        for (RepoItemMetadata repoItem : repoItemList) {
            if (!repoItem.isDirectory()) {
                if (logger.isTraceEnabled()) {
                    logger.trace(" - " + repoItem.getFilePath() + "  " + "(size = " + repoItem.getSize() + ")");
                }
                String pathId = repoItem.getFilePath();
                boolean isNxl = FileUtils.getRealFileExtension(pathId).equalsIgnoreCase(Constants.NXL_FILE_EXTN);
                if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                    continue;
                }
                RepositoryContent content = new RepositoryContent();
                content.setFolder(false);
                content.setLastModifiedTime(repoItem.getLastModified().getTime());
                content.setFileSize(repoItem.getSize());
                content.setPathId(pathId);
                content.setFileId(pathId);
                content.setFavorited(favoriteFileIdSet.contains(pathId));
                String filePathDisplay = repoItem.getFilePathDisplay();
                content.setPath(filePathDisplay);
                String fileName = filePathDisplay.substring(filePathDisplay.lastIndexOf('/') + 1);
                content.setName(fileName);
                content.setFileType(RepositoryFileUtil.getOriginalFileExtension(fileName));
                content.setRepoId(getRepoId());
                content.setRepoName(getRepoName());
                content.setRepoType(getRepoType());
                content.setProtectedFile(isNxl);
                content.setUsePathId(true);
                contentList.add(content);
            }
        }
        return contentList;
    }

    private void storeCopyData(UploadedFileMetaData metadata, boolean updateMyVault) {
        StoreItem data = new StoreItem();
        data.setDirectory(false);
        data.setRepoId(getRepoId());
        data.setFileParentPath(StringUtils.getParentPath(metadata.getPathId()));
        data.setFilePath(metadata.getPathId());
        data.setFilePathDisplay(metadata.getPathDisplay());
        data.setSize(metadata.getSize());
        data.setLastModified(Nvl.nvl(metadata.getLastModifiedTime(), new Date()));
        try {
            searcher.addRepoItem(data);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error adding record ", rse);
        }
        if (metadata.getSize() > 0) {
            updateRepositorySize(metadata.getSize(), updateMyVault);
        }
    }

    private void storeCopyFolderData(List<RepoItemMetadata> dataList, long bytesCopied, boolean updateMyVault) {
        try {
            searcher.addRepoItemList(dataList);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error adding record ", rse);
        }
        if (bytesCopied > 0) {
            updateRepositorySize(bytesCopied, updateMyVault);
        }
    }

    protected abstract void storeData(Map<String, String> customMetadata, UploadedFileMetaData uploadedFileMetaData,
        StoreItem existingFile, String parentPathId, boolean updateMyVault);

    protected abstract StoreItem getStoreItem();

    private void storeFolderData(String parentPathId, String pathId, String pathDisplay, long size, Date lastUpdated) {
        StoreItem data = getStoreItem();
        data.setDirectory(true);
        data.setRepoId(getRepoId());
        data.setFileParentPath(parentPathId);
        data.setFilePath(pathId);
        data.setFilePathDisplay(pathDisplay);
        data.setSize(size);
        data.setLastModified(Nvl.nvl(lastUpdated, new Date()));
        data.setCreationTime(Nvl.nvl(lastUpdated, new Date()));
        try {
            searcher.addRepoItem(data);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error adding record ", rse);
        }

    }

    protected abstract RepositoryContent getRepositoryContent(StoreItem data);

    /**
     * This method updates the size stored as part of the repository prefs.
     * sizeDelta can be positive (upload file)
     * sizeDelta can be negative (delete file/folder)
     * @param sizeDelta
     */
    protected abstract void updateRepositorySize(long sizeDelta, boolean updateMyVaultSize);

    private void doMaintenance(DeleteFileMetaData metadata) {
        Map<String, Long> keySizeMapping = metadata.getKeySizeMapping();
        List<String> deletedKeys = metadata.getDeletedKeys();
        try {
            searcher.updateOrDeleteRepoItems(getRepoId(), deletedKeys);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error deleting record ", rse);
        }
        long sizeDeleted = 0;
        long myVaultSizeDeleted = 0;
        for (String str : deletedKeys) {
            if (keySizeMapping.containsKey(str)) {
                if (str.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
                    myVaultSizeDeleted += keySizeMapping.get(str);
                }
                sizeDeleted += keySizeMapping.get(str);
            }
        }
        if (sizeDeleted > 0) {
            if (myVaultSizeDeleted == sizeDeleted) {
                // all deleted items are in MyVault
                updateRepositorySize(-1 * sizeDeleted, true);
            } else if (myVaultSizeDeleted > 0) {
                // some deleted items in MyVault, the others are not
                updateRepositorySize(-1 * (sizeDeleted - myVaultSizeDeleted), false);
                updateRepositorySize(-1 * myVaultSizeDeleted, true);
            } else {
                // no deleted items are in MyVault
                updateRepositorySize(-1 * sizeDeleted, false);
            }
        }
    }

    @Override
    public final List<RepositoryContent> getFileList(String path) {
        return getFileList(path, new FilterOptions());
    }

    private DeleteFileMetaData getDeleteFileMetaData(DeleteFileMetaData deleteMetaData,
        String pathId, boolean isRoot) throws RepositoryException {
        if (!StringUtils.hasText(pathId)) {
            pathId = "/";
        }
        if (deleteMetaData.getDeletedKeys() == null) {
            deleteMetaData.setDeletedKeys(new ArrayList<>());
        }
        deleteMetaData.getDeletedKeys().add(pathId);
        RepositoryContent metadata = getFileMetadata(pathId);
        if (deleteMetaData.getKeySizeMapping() == null) {
            deleteMetaData.setKeySizeMapping(new HashMap<>());
        }
        deleteMetaData.getKeySizeMapping().put(pathId, metadata.getFileSize());
        if (isRoot) {
            deleteMetaData.setFileName(metadata.getName());
            deleteMetaData.setFilePath(metadata.getPath());
        }
        if (metadata.isFolder()) {
            List<StoreItem> repoItemList;
            try {
                repoItemList = searcher.listRepoItems(getRepoId(), pathId);
            } catch (RMSRepositorySearchException e) {
                throw new RepositoryException(e.getMessage(), e);
            }
            for (StoreItem repoItem : repoItemList) {
                getDeleteFileMetaData(deleteMetaData, repoItem.getFilePath(), false);
            }
        }

        return deleteMetaData;
    }

    @SuppressWarnings("unchecked")
    public final List<RepositoryContent> getFileList(String pathId, FilterOptions filterOptions) {
        List<RepositoryContent> contentList = new ArrayList<>();
        boolean checkMyVault = false;
        if (!StringUtils.hasText(pathId)) {
            pathId = "/";
        }
        if ("/".equalsIgnoreCase(pathId)) {
            checkMyVault = true;
        }
        Set<String> favoriteFileIdSet;
        List<RepoItemMetadata> repoItemList;
        try (DbSession session = DbSession.newSession()) {
            DetachedCriteria dc = DetachedCriteria.forClass(RepoItemMetadata.class, "r");
            dc.add(Restrictions.eq("repository.id", getRepoId()));
            dc.add(Restrictions.eq("fileParentPathHash", StringUtils.getMd5Hex(pathId)));
            Criteria criteria = dc.getExecutableCriteria(session.getSession());
            repoItemList = criteria.list();
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), pathId);
        }
        for (RepoItemMetadata repoItem : repoItemList) {
            String folderName = getCurrentFolderName(repoItem.getFilePathDisplay());
            if (checkMyVault && RepoConstants.MY_VAULT_NAME.equalsIgnoreCase(folderName)) {
                continue;
            }
            RepositoryContent content = new RepositoryContent();
            // add folders first
            if (repoItem.isDirectory()) {
                content.setFolder(true);
                content.setName(folderName);
                content.setPathId(repoItem.getFilePath());
                content.setPath(repoItem.getFilePathDisplay());
                content.setFileId(repoItem.getFilePath());
                // incase we support favorite folder in the future
                content.setFavorited(favoriteFileIdSet.contains(repoItem.getFilePath()));
                content.setRepoId(getRepoId());
                content.setRepoName(getRepoName());
                content.setRepoType(getRepoType());
                content.setUsePathId(true);
                content.setLastModifiedTime(repoItem.getLastModified().getTime());
                contentList.add(content);
            }
        }

        // add filtered files
        if (!filterOptions.showOnlyFolders()) {
            contentList.addAll(toRepositoryContentList(repoItemList, favoriteFileIdSet, filterOptions));
        }
        return contentList;
    }

    private void toSearchResultList(List<RepositoryContent> contentList, Set<String> favoriteFileIdSet,
        List<StoreItem> results) {
        for (StoreItem data : results) {
            RepositoryContent searchResult = getRepositoryContent(data);
            searchResult.setFileSize(data.getSize());
            searchResult.setFileType(RepositoryFileUtil.getOriginalFileExtension(data.getFilePath()));
            searchResult.setProtectedFile(FileUtils.getRealFileExtension(data.getFilePath()).equals(Constants.NXL_FILE_EXTN));
            searchResult.setFolder(data.isDirectory());
            searchResult.setLastModifiedTime(data.getLastModified() == null ? 0 : data.getLastModified().getTime());
            String filePathDisplay = data.getFilePathDisplay();
            if (data.isDirectory() && filePathDisplay != null && filePathDisplay.endsWith("/")) {
                filePathDisplay = filePathDisplay.substring(0, filePathDisplay.length() - 1);
            }
            searchResult.setName(FileUtils.getName(filePathDisplay));
            searchResult.setPath(filePathDisplay);
            searchResult.setPathId(data.getFilePath());
            searchResult.setFavorited(favoriteFileIdSet.contains(data.getFilePath()));
            contentList.add(searchResult);
        }
    }

    @Override
    public final List<RepositoryContent> search(String searchString) throws RepositoryException {
        Set<String> favoriteFileIdSet;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }
        List<RepositoryContent> contentList = new ArrayList<>();
        try {
            List<StoreItem> results = searcher.search(getRepoId(), searchString);
            toSearchResultList(contentList, favoriteFileIdSet, results);
        } catch (RMSRepositorySearchException e) {
            handleRepoException(e);
        }
        return contentList;
    }

    @Override
    public final RepositoryContent getFileMetadata(String path) throws RepositoryException {
        RepositoryContent content;
        try {
            StoreItem item = searcher.getRepoItem(getRepoId(), path);
            content = toRepositoryContent(item);
        } catch (RMSRepositorySearchException e) {
            throw new RepositoryException("Error getting FileMetadata", e);
        }
        return content;
    }

    @Override
    public final DeleteFileMetaData deleteFile(String filePathId, String filePath)
            throws RepositoryException, ForbiddenOperationException {
        if ("/".equals(filePathId) || RepoConstants.MY_VAULT_FOLDER_PATH_ID.equals(filePathId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting root pathId {} is forbidden", filePathId);
            }
            throw new ForbiddenOperationException("Invalid pathId: " + filePathId);
        }
        String pathDisplay = getPathDisplay(filePathId);
        if (pathDisplay == null) {
            throw new FileNotFoundException("File/Folder does not exist");
        }
        //Query database to generate DeleteFileMetaData
        DeleteFileMetaData metadata = getDeleteFileMetaData(new DeleteFileMetaData(), filePathId, true);
        repo.deleteFile(filePathId, pathDisplay);
        doMaintenance(metadata);
        return metadata;
    }

    @Override
    public final CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename) throws RepositoryException, FileAlreadyExistsException {
        String parentPathDisplay = getPathDisplay(folderPathId);
        String folderFilePath = folderPathId + folderName.toLowerCase() + "/";
        if (!autoRename && pathExists(folderFilePath)) {
            throw new FileAlreadyExistsException("Folder already exists");
        }
        CreateFolderResult result = repo.createFolder(folderName, folderPathId, parentPathDisplay, autoRename);
        storeFolderData(folderPathId, result.getPathId(), result.getPathDisplay(), 0, result.getLastModified());
        return result;
    }

    @Override
    public final OperationResult copyFile(String fromObject, String toObject) throws RepositoryException {
        OperationResult result = repo.copyFile(fromObject, toObject);
        if (result.isSuccess()) {
            UploadedFileMetaData metadata = result.getUploadMetadata();
            storeCopyData(metadata, isFromMyVault(toObject));
            result.setUploadMetadata(null);
        }
        return result;
    }

    @Override
    public final List<OperationResult> copyFolder(String fromObject, String toObject) throws RepositoryException {
        List<OperationResult> results = repo.copyFolder(fromObject, toObject);
        return doAddMaintenance(results, toObject);
    }

    private List<OperationResult> doAddMaintenance(List<OperationResult> results, String toObject) {
        Repository repository;
        long bytesCopied = 0;
        try (DbSession session = DbSession.newSession()) {
            repository = session.load(Repository.class, getRepoId());
        }
        List<RepoItemMetadata> dataList = new LinkedList<>();
        for (OperationResult result : results) {
            if (result.isSuccess()) {
                UploadedFileMetaData metadata = result.getUploadMetadata();
                RepoItemMetadata data = new RepoItemMetadata();
                data.setLastModified(Nvl.nvl(metadata.getLastModifiedTime(), new Date()));
                data.setDirectory(metadata.isFolder());
                data.setRepository(repository);
                data.setFileParentPathHash(StringUtils.getMd5Hex(StringUtils.getParentPath(metadata.getPathId())));
                data.setFilePath(metadata.getPathId());
                data.setFilePathDisplay(metadata.getPathDisplay());
                data.setSize(metadata.getSize());
                bytesCopied += metadata.getSize();
                dataList.add(data);
                result.setUploadMetadata(null);
            }
        }
        storeCopyFolderData(dataList, bytesCopied, isFromMyVault(toObject));
        return results;
    }

    @Override
    public final OperationResult moveFile(String fromObject, String toObject)
            throws RepositoryException, ForbiddenOperationException {
        OperationResult result = repo.moveFile(fromObject, toObject);
        if (result.isSuccess()) {
            UploadedFileMetaData metadata = result.getUploadMetadata();
            storeCopyData(metadata, isFromMyVault(toObject));
            result.setUploadMetadata(null);
            DeleteFileMetaData deleteMetadata = result.getDeleteMetadata();
            doMaintenance(deleteMetadata);
            result.setDeleteMetadata(null);
        }
        return result;
    }

    @Override
    public final List<OperationResult> moveFolder(String fromObject, String toObject)
            throws RepositoryException, ForbiddenOperationException {
        List<OperationResult> results = repo.moveFolder(fromObject, toObject);
        if (!results.isEmpty()) {
            OperationResult deleteResult = results.get(results.size() - 1);
            if (deleteResult != null) {
                DeleteFileMetaData metadata = deleteResult.getDeleteMetadata();
                if (metadata != null) {
                    results.remove(deleteResult);
                    doAddMaintenance(results, toObject);
                    doMaintenance(metadata);
                }
            }
        }
        return results;
    }

    @Override
    public final void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        repo.refreshFileList(request, response, path);
    }

    @Override
    public final File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        return repo.getFile(fileId, filePath, outputPath);
    }

    @Override
    public final List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames,
        String outputPath)
            throws RepositoryException, MissingDependenciesException {
        return repo.downloadFiles(parentPathId, parentPathName, filenames, outputPath);
    }

    @Override
    public final byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        return repo.downloadPartialFile(fileId, filePath, startByte, endByte);
    }

    @Override
    public final String getPublicUrl(String fileId) throws RepositoryException {
        return repo.getPublicUrl(fileId);
    }

    @Override
    public final String getCurrentFolderPathId(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        return repo.getCurrentFolderPathId(objectPathId, objectPathDisplay);
    }

    @Override
    public final String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        return repo.getCurrentFolderPathDisplay(objectPathId, objectPathDisplay);
    }

    @Override
    public final Usage calculateRepoUsage() {
        return repo.calculateRepoUsage();
    }

    @Override
    public final void handleRepoException(Exception e) throws RepositoryException {
        repo.handleRepoException(e);
    }

    @Override
    public final RMSUserPrincipal getUser() {
        return repo.getUser();
    }

    @Override
    public final void setUser(RMSUserPrincipal userPrincipal) {
        repo.setUser(userPrincipal);
    }

    @Override
    public final String getAccountName() {
        return repo.getAccountName();
    }

    @Override
    public final void setAccountName(String accountName) {
        repo.setAccountName(accountName);
    }

    @Override
    public final String getAccountId() {
        return repo.getAccountId();
    }

    @Override
    public final void setAccountId(String accountId) {
        repo.setAccountId(accountId);
    }

    @Override
    public final String getRepoId() {
        return repo.getRepoId();
    }

    @Override
    public final void setRepoId(String repoId) {
        repo.setRepoId(repoId);
    }

    @Override
    public final ServiceProviderType getRepoType() {
        return repo.getRepoType();
    }

    @Override
    public final boolean isShared() {
        return repo.isShared();
    }

    @Override
    public final void setShared(boolean shared) {
        repo.setShared(shared);
    }

    @Override
    public final String getRepoName() {
        return repo.getRepoName();
    }

    @Override
    public final void setRepoName(String repoName) {
        repo.setRepoName(repoName);
    }

    @Override
    public final Map<String, Object> getAttributes() {
        return repo.getAttributes();
    }

    protected boolean isFromMyVault(String path) {
        return path.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID);
    }

    @Override
    public final ServiceProviderSetting getSetting() {
        return repo.getSetting();
    }

    public abstract void updateRepositorySize();

    public abstract String getExistingSpaceItemIdWithFilePath(String filePath, String repoId)
            throws RepositoryException;

    public abstract Long getTotalFileCount(DbSession session, String repoId, boolean isVaultCount);

}
