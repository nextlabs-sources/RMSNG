package com.nextlabs.rms.application;

import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IApplicationRepository {

    List<ApplicationRepositoryContent> getFileList(String path)
            throws ApplicationRepositoryException, RepositoryException;

    List<ApplicationRepositoryContent> getFileList(String path, FilterOptions filterOptions)
            throws ApplicationRepositoryException, RepositoryException;

    ApplicationRepositoryContent getFileMetadata(String path)
            throws ApplicationRepositoryException, RepositoryException;

    File getFile(String fileId, String filePath, String outputPath)
            throws ApplicationRepositoryException, RepositoryException, OneDriveServiceException, IOException;

    List<ApplicationRepositoryContent> search(String searchString)
            throws ApplicationRepositoryException, RepositoryException;

    List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws ApplicationRepositoryException;

    FileUploadMetadata uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws ApplicationRepositoryException, RepositoryException;

    FileDeleteMetaData deleteFile(String filePathId, String filePath)
            throws ApplicationRepositoryException, RepositoryException;

    String getCurrentFolderId(String path) throws RepositoryException, ApplicationRepositoryException;

    byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException, ApplicationRepositoryException;

    boolean checkIfFileExists(String filePath) throws ApplicationRepositoryException, RepositoryException;
}
