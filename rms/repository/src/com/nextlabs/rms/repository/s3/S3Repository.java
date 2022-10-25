package com.nextlabs.rms.repository.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.EscapeUtils;
import com.nextlabs.common.util.GsonUtils;
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
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryFolderAccessException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3Repository implements IRepository, Closeable {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String REGION = "REGION";

    private RMSUserPrincipal userPrincipal;
    private String repoId;
    private final ServiceProviderSetting setting;
    private ServiceProviderType repoType = ServiceProviderType.S3;
    private String bucketName;
    private String repoName;
    private String accountName;
    private boolean isShared;
    private final Map<String, Object> attributes;
    private String accountId;
    private final AmazonS3 s3client;
    private static final long SHARING_EXPIRE_TIME_MS = TimeUnit.DAYS.toMillis(7);

    public S3Repository(RMSUserPrincipal userPrincipal, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = userPrincipal;
        this.setting = setting;
        attributes = new HashMap<String, Object>();
        s3client = getClient(getAWSCredentials());
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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
        S3Object s3object = null;
        S3ObjectInputStream input = null;
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, repoId + fileId);
            request.setRange(startByte, endByte);
            s3object = s3client.getObject(request);
            input = s3object.getObjectContent();
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                IOUtils.copy(input, outStream);
                return outStream.toByteArray();
            }
        } catch (IOException | AmazonClientException e) {
            handleRepoException(e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(s3object);
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
            GetObjectRequest request = new GetObjectRequest(bucketName, repoId + fileId);
            String fileName = StringUtils.hasText(filePath) ? filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length()) : fileId.substring(fileId.lastIndexOf('/') + 1, fileId.length());
            File file = new File(outputPath, fileName);
            s3client.getObject(request, file);
            return file;
        } catch (AmazonClientException e) {
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

        File uploadFile = new File(localFile);
        long fileSize = uploadFile.length();
        InputStream uploadFileInputStream = null;
        UploadedFileMetaData uploadedFileMetaData = null;
        TransferManager transferManager = null;

        if (uploadFile.getName().length() > 255) {
            throw new InvalidFileNameException("Invalid name: " + uploadFile.getName());
        }

        try {
            String baseFilePathId = repoId + parentFilePathId;
            String filePathDisplay = getDisplayPathWithSeparatorFixed(parentFilePathDisplay, uploadFile.getName(), false);

            String key = new StringBuilder(baseFilePathId).append(uploadFile.getName()).toString();
            final String filePathId = key.replaceFirst(repoId, "").toLowerCase();

            transferManager = new TransferManager(s3client);
            uploadFileInputStream = new FileInputStream(uploadFile);
            ObjectMetadata customUserMetadata = new ObjectMetadata();
            customUserMetadata.setContentLength(fileSize);
            if (customMetadata != null) {
                Map<String, String> myVaultDetailMap = GsonUtils.GSON.fromJson(customMetadata.get("myVaultDetail"), GsonUtils.GENERIC_MAP_TYPE);
                customUserMetadata.setUserMetadata(escapeUserMetadata(myVaultDetailMap));
            }
            Upload myUpload = transferManager.upload(bucketName, key.toLowerCase(), uploadFileInputStream, customUserMetadata);
            UploadResult uploadResult = myUpload.waitForUploadResult();
            if (uploadResult != null) {
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
                GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(uploadResult.getBucketName(), uploadResult.getKey());
                ObjectMetadata objectMetadata = getObjectMetadata(getObjectMetadataRequest);
                if (objectMetadata != null) {
                    if (customMetadata != null && customMetadata.get("lastModified") != null) {
                        uploadedFileMetaData.setLastModifiedTime(new Date(Long.parseLong(customMetadata.get("lastModified"))));
                    } else {
                        uploadedFileMetaData.setLastModifiedTime(objectMetadata.getLastModified());
                    }
                }
            }
            return uploadedFileMetaData;
        } catch (AmazonServiceException | InterruptedException e) {
            logger.error("Unable to upload file (path: " + localFile + "): " + e.getMessage(), e);
            handleRepoException(e);
        } catch (AmazonClientException ace) {
            logger.error("Unable to upload file (path: " + localFile + "): " + ace.getMessage(), ace);
            handleRepoException(ace);
        } catch (FileNotFoundException e) {
            logger.error("Unable to upload file (path: " + localFile + "): " + e.getMessage(), e);
            handleRepoException(e);
        } finally {
            IOUtils.closeQuietly(uploadFileInputStream);
            if (transferManager != null) {
                transferManager.shutdownNow(false);
            }
        }
        return uploadedFileMetaData;
    }

    /**
     * call this method when we want to store user metadata for a s3 object
     * @param userMetadata
     */
    private Map<String, String> escapeUserMetadata(Map<String, String> userMetadata) {
        if (userMetadata == null || userMetadata.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> escapedData = new HashMap<String, String>(userMetadata.size());
        for (Iterator<Map.Entry<String, String>> it = userMetadata.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            escapedData.put(entry.getKey(), EscapeUtils.escapeNonASCIICharacters(entry.getValue()));
        }
        return escapedData;
    }

    private ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest)
            throws AmazonServiceException {
        ObjectMetadata objectMetadata = null;
        if (getObjectMetadataRequest != null && getObjectMetadataRequest.getBucketName() != null && getObjectMetadataRequest.getKey() != null) {
            objectMetadata = s3client.getObjectMetadata(getObjectMetadataRequest);
        }
        return objectMetadata;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException {
        String keyWithPrefix = repoId + filePathId;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting file (user ID: {}, repository ID: {}, repository name: {}): {}", userPrincipal.getUserId(), getRepoId(), getRepoName(), filePathId);
            }
            List<KeyVersion> keys = new ArrayList<KeyVersion>();
            if (filePathId.endsWith("/")) {
                for (S3ObjectSummary file : s3client.listObjects(bucketName, keyWithPrefix).getObjectSummaries()) {
                    keys.add(new KeyVersion(file.getKey()));
                }
            } else {
                S3Object s3Object = s3client.getObject(bucketName, keyWithPrefix);
                keys.add(new KeyVersion(s3Object.getKey()));
            }
            if (filePath == null) {
                throw new com.nextlabs.rms.repository.exception.FileNotFoundException("Invalid pathId: " + filePathId);
            }
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName).withKeys(keys);
            s3client.deleteObjects(request);
        } catch (MultiObjectDeleteException mode) {
            logger.error("One or more objects could not be deleted", mode);
            handleRepoException(mode);
        } catch (AmazonServiceException ase) {
            logger.error("Unable to delete file (path: {}): {}", filePath, ase.getMessage(), ase);
            handleRepoException(ase);
        } catch (AmazonClientException ace) {
            logger.error("Unable to delete file (path: {}): {}", filePath, ace.getMessage(), ace);
            handleRepoException(ace);
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
                String path = new StringBuilder(repoId).append(folderFilePath).toString();
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(0);
                // create empty content
                InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
                PutObjectResult result = s3client.putObject(new PutObjectRequest(bucketName, path.toLowerCase(), emptyContent, metadata));

                if (result != null) {
                    ObjectMetadata resultMetadata = result.getMetadata();
                    Date lastUpdated = Nvl.nvl(resultMetadata.getLastModified(), new Date());

                    opResult = new CreateFolderResult(folderFilePath, folderFilePathDisplay, folderName, lastUpdated);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding folder {} to the repository (repository ID: {}).", folderName, getRepoId());
                }
            } catch (AmazonServiceException ase) {
                logger.error("Unable to create folder (folder: {}, repository ID: {}): {}", folderName, getRepoId(), ase.getMessage(), ase);
                handleRepoException(ase);
            } catch (AmazonClientException ace) {
                logger.error("Unable to create folder (folder: {}, repository ID: {}): {}", folderName, getRepoId(), ace.getMessage(), ace);
                handleRepoException(ace);
            }
        }
        return opResult;
    }

    public OperationResult copyFile(String fromObject, String toObject) {
        String fromObjectWithPrefix = repoId + fromObject;
        String toObjectWithPrefix = repoId + toObject;
        OperationResult resultToClient = new OperationResult(toObject, 0L, false, true);
        UploadedFileMetaData uploadedFileMetaData = null;
        try {
            CopyObjectResult copyResult = s3client.copyObject(bucketName, fromObjectWithPrefix.toLowerCase(), bucketName, toObjectWithPrefix.toLowerCase());
            long fileSize = s3client.getObjectMetadata(bucketName, toObjectWithPrefix).getContentLength();
            uploadedFileMetaData = new UploadedFileMetaData();
            uploadedFileMetaData.setPathId(toObject);
            uploadedFileMetaData.setPathDisplay(toObject);
            uploadedFileMetaData.setRepoName(getRepoName());
            uploadedFileMetaData.setSize(fileSize);
            uploadedFileMetaData.setLastModifiedTime(copyResult.getLastModifiedDate());
            resultToClient.setSize(fileSize);
            resultToClient.setUploadMetadata(uploadedFileMetaData);
        } catch (AmazonServiceException e) {
            resultToClient.setSuccess(false);
            logger.debug("Error copying file {}", fromObjectWithPrefix, e);
        }
        return resultToClient;
    }

    public List<OperationResult> copyFolder(String fromObject, String toObject) {
        String fromObjectWithPrefix = repoId + fromObject;
        String toObjectWithPrefix = repoId + toObject;
        List<OperationResult> resultList = new LinkedList<OperationResult>();
        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(fromObjectWithPrefix.toLowerCase());
        ListObjectsV2Result result;
        List<String> keysWithPrefix = new ArrayList<>();
        Map<String, Long> fileSizeMap = new HashMap<String, Long>();
        do {
            result = s3client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                keysWithPrefix.add(objectSummary.getKey());
                fileSizeMap.put(objectSummary.getKey(), objectSummary.getSize());
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());
        for (String str : keysWithPrefix) {
            String destPath = str.replaceFirst(fromObjectWithPrefix, toObjectWithPrefix);
            boolean isFolder = str.endsWith("/") ? true : false;
            String pathId = destPath.replace(repoId, "");
            OperationResult resultToClient = new OperationResult(pathId, fileSizeMap.get(str), isFolder, true);
            try {
                CopyObjectResult copyResult = s3client.copyObject(bucketName, str.toLowerCase(), bucketName, destPath.toLowerCase());
                UploadedFileMetaData uploadedFileMetaData = new UploadedFileMetaData();
                uploadedFileMetaData.setPathId(pathId);
                uploadedFileMetaData.setPathDisplay(pathId);
                uploadedFileMetaData.setRepoName(getRepoName());
                uploadedFileMetaData.setSize(fileSizeMap.get(str));
                uploadedFileMetaData.setLastModifiedTime(copyResult.getLastModifiedDate());
                uploadedFileMetaData.setFolder(isFolder);
                resultToClient.setUploadMetadata(uploadedFileMetaData);
            } catch (AmazonServiceException e) {
                resultToClient.setSuccess(false);
                logger.error("Error copying file " + str + " to " + destPath);
            } finally {
                resultList.add(resultToClient);
            }
        }
        return resultList;
    }

    @Override
    public OperationResult moveFile(String fromObject, String toObject) throws ForbiddenOperationException {
        OperationResult copyResult = copyFile(fromObject, toObject);
        if (copyResult.isSuccess()) {
            String srcFilePathId = fromObject;
            String srcFilePathDisplay = copyResult.isDirectory() ? srcFilePathId.substring(0, srcFilePathId.length() - 1) : srcFilePathId;
            try {
                DeleteFileMetaData metadata = deleteFile(srcFilePathId, srcFilePathDisplay);
                copyResult.setDeleteMetadata(metadata);
            } catch (RepositoryException e) {
                logger.error("Error occured while deleting the original file " + srcFilePathDisplay + " after copying to the new location " + toObject.replaceFirst(repoId, ""), e);
                copyResult.setSuccess(false);
                // Rollback the copied item
                String desFilePathId = toObject;
                String desFilePathDisaply = copyResult.isDirectory() ? desFilePathId.substring(0, desFilePathId.length() - 1) : desFilePathId;
                try {
                    deleteFile(desFilePathId, desFilePathDisaply);
                } catch (RepositoryException e2) {
                    logger.error("Error occured while rollbacking the copied file " + desFilePathDisaply);
                }
            }
        }
        return copyResult;
    }

    @Override
    public List<OperationResult> moveFolder(String fromObject, String toObject) throws RepositoryException,
            ForbiddenOperationException {
        List<OperationResult> copyResults = copyFolder(fromObject, toObject);
        boolean copySuccess = true;
        for (OperationResult copyResult : copyResults) {
            if (!copyResult.isSuccess()) {
                copySuccess = false;
                break;
            }
        }
        if (copySuccess) {
            // Copy completed. Delete the original folder
            OperationResult deleteResult = new OperationResult(fromObject, 0L, true, true);
            copyResults.add(deleteResult);
            try {
                DeleteFileMetaData metadata = deleteFile(fromObject, fromObject);
                deleteResult.setDeleteMetadata(metadata);
            } catch (RepositoryException e) {
                copyResults.remove(deleteResult);
                // delete original folder failed, rollback the whole copy process: copy back what have been successfully delete and delete the destination folder
                if (e.getCause() instanceof MultiObjectDeleteException) {
                    boolean copyBackSuccess = true;
                    List<DeletedObject> deletedObjects = ((MultiObjectDeleteException)e.getCause()).getDeletedObjects();
                    for (DeletedObject deletedObject : deletedObjects) {
                        String copiedFileKey = deletedObject.getKey().replaceFirst(fromObject, toObject);
                        OperationResult result = copyFile(copiedFileKey, deletedObject.getKey());
                        if (!result.isSuccess()) {
                            copyBackSuccess = false;
                            break;
                        }
                    }
                    if (copyBackSuccess) {
                        deleteFile(toObject, toObject);
                    } else {
                        logger.error("Error occured while rolling back.");
                        throw new RepositoryException("Unable to roll back due to unsuccessful COPY back operation.", e);
                    }
                }
                handleRepoException(e);
            }
        } else {
            // Copy failed on one or more files. Rollback the copy action
            deleteFile(toObject, toObject);
        }
        return copyResults;
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

    private AWSCredentials getAWSCredentials() {
        String appID = (String)setting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String appSecret = (String)setting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        return new BasicAWSCredentials(appID, appSecret);
    }

    private AmazonS3 getClient(AWSCredentials awsCreds) {
        String region = Nvl.nvl((String)setting.getAttributes().get(REGION), Regions.US_EAST_1.getName());
        ClientConfiguration config = new ClientConfiguration().withConnectionTimeout(RepoConstants.CONNECTION_TIMEOUT).withSocketTimeout(RepoConstants.READ_TIMEOUT);
        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(region).withClientConfiguration(config).build();
    }

    @Override
    public String getPublicUrl(String fileId) throws RepositoryException {
        try {
            /*
             * presigned urls cannot have exiration of more than 7 days and generation of this URL is purely a client-side operation.
             * However, in case of S3 repository, we do not need user auth-token to perform this action, so we can generate on-demand
             */
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, repoId + fileId);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET);

            Date expiration = new Date();
            long msec = expiration.getTime();
            msec += SHARING_EXPIRE_TIME_MS;
            expiration.setTime(msec);
            generatePresignedUrlRequest.setExpiration(expiration);
            return s3client.generatePresignedUrl(generatePresignedUrlRequest).toString();
        } catch (AmazonClientException ace) {
            logger.error("Unable to get public URL for file  " + fileId + "): " + ace.getMessage(), ace);
            handleRepoException(ace);
        }
        return null;
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        if (e instanceof AmazonS3Exception && ((AmazonS3Exception)e).getStatusCode() == 404) {
            throw new com.nextlabs.rms.repository.exception.FileNotFoundException(e.getMessage(), e);
        } else {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public Usage calculateRepoUsage() {
        final String myVaultKey = repoId + RepoConstants.MY_VAULT_FOLDER_PATH_ID;
        long size = 0L;
        long myVaultSize = 0L;
        ObjectListing objectListing = s3client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(repoId + "/"));
        do {
            List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
            for (S3ObjectSummary summary : summaries) {
                if (summary.getKey().startsWith(myVaultKey)) {
                    myVaultSize += summary.getSize();
                }
                size += summary.getSize();
            }
            objectListing = s3client.listNextBatchOfObjects(objectListing);
        } while (objectListing.isTruncated());
        return new Usage(size, myVaultSize);
    }

    private boolean isFolderNameValid(String folderName) {
        Pattern pattern = Pattern.compile("^[\\u00C0-\\u1FFF\\u2C00-\\uD7FF\\w- ]*$");
        Matcher matcher = pattern.matcher(folderName);
        return StringUtils.hasText(folderName) && matcher.matches() && folderName.length() <= 255;
    }

    @Override
    public void close() throws IOException {
        if (s3client instanceof AmazonWebServiceClient) {
            AmazonWebServiceClient.class.cast(s3client).shutdown();
        }
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }
}
