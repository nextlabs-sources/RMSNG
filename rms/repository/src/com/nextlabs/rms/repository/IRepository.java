package com.nextlabs.rms.repository;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IRepository {

    public static final long INVALID_REPOSITORY_ID = -1;

    public RMSUserPrincipal getUser();

    public void setUser(RMSUserPrincipal userPrincipal);

    public String getAccountName();

    public void setAccountName(String accountName);

    public String getAccountId();

    public void setAccountId(String accountId);

    public String getRepoId();

    public void setRepoId(String repoId);

    public ServiceProviderType getRepoType();

    public boolean isShared();

    public void setShared(boolean shared);

    public String getRepoName();

    public void setRepoName(String repoName);

    public ServiceProviderSetting getSetting();

    public Map<String, Object> getAttributes();

    public List<RepositoryContent> getFileList(String path, FilterOptions filterOptions) throws RepositoryException;

    public List<RepositoryContent> getFileList(String path) throws RepositoryException;

    public RepositoryContent getFileMetadata(String path) throws RepositoryException;

    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException;

    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException;

    public List<RepositoryContent> search(String searchString) throws RepositoryException;

    public List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException;

    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException;

    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException;

    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException;

    public OperationResult copyFile(String fromObject, String toObject) throws RepositoryException;

    public List<OperationResult> copyFolder(String fromObject, String toObject) throws RepositoryException;

    public OperationResult moveFile(String fromObject, String toObject) throws RepositoryException,
            ForbiddenOperationException;

    public List<OperationResult> moveFolder(String fromObject, String toObject) throws RepositoryException,
            ForbiddenOperationException;

    /**
     * Returns the public URL of the file. If it does not exist, this method will create a new one.
     * @param fileId
     * @return publilcUrl
     * @throws RepositoryException
     */
    public String getPublicUrl(String fileId) throws RepositoryException;

    /**
     * @param folderName
     * @param folderPathId
     * @param autoRename -- if user create a folder which has same name with an existing folder in the repository, we provide auto rename function.
     * 						this parameter is not being used now, reserved for future features.
     * @throws RepositoryException
     * @throws FileAlreadyExistsException
     */
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename)
            throws RepositoryException, FileAlreadyExistsException;

    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException;

    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay) throws RepositoryException;

    public Usage calculateRepoUsage();

    public void handleRepoException(Exception e) throws RepositoryException;

    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException;

    public static class Usage {

        private long size;
        private long myVaultSize;

        public Usage(long size, long myVaultSize) {
            this.size = size;
            this.myVaultSize = myVaultSize;
        }

        public long getMyVaultSize() {
            return myVaultSize;
        }

        public long getSize() {
            return size;
        }
    }
}
