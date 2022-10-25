package com.nextlabs.rms.application.onedrive;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.application.ApplicationRepositoryContent;
import com.nextlabs.rms.application.FileDeleteMetaData;
import com.nextlabs.rms.application.FileUploadMetadata;
import com.nextlabs.rms.application.IApplicationRepository;
import com.nextlabs.rms.application.onedrive.authentication.ApplicationOneDriveOAuthHandler;
import com.nextlabs.rms.auth.IRefreshTokenCallback;
import com.nextlabs.rms.auth.ITokenResponse;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.UnauthorizedApplicationRepositoryException;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveResponseHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveTokenResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveInnerResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.repository.onedrive.type.OneDriveFile;
import com.nextlabs.rms.repository.onedrive.type.OneDriveFolder;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItem;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItems;
import com.nextlabs.rms.repository.onedrive.type.OneDriveParentReference;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadItem;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadItemWrapper;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadSession;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OneDriveApplicationRepository implements IApplicationRepository {

    private static final Logger LOGGER = LogManager.getLogger("com.nextlabs.rms.server");
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    private final Client client;
    private final Map<String, Object> attributes = new HashMap<>();

    public OneDriveApplicationRepository(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.client = new Client();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public List<ApplicationRepositoryContent> getFileList(String path)
            throws RepositoryException, ApplicationRepositoryException {
        return getFileList(path, new FilterOptions());
    }

    @Override
    public List<ApplicationRepositoryContent> getFileList(String path, FilterOptions filterOptions)
            throws ApplicationRepositoryException, RepositoryException {
        List<ApplicationRepositoryContent> rt = new ArrayList<>();

        try {

            List<OneDriveItems> oneDriveItems = client.listItemsWithPath(path);
            for (OneDriveItems items : oneDriveItems) {
                if (items == null) {
                    continue;
                }
                List<OneDriveItem> values = items.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                for (OneDriveItem item : values) {
                    if (item == null) {
                        continue;
                    }
                    OneDriveFile file = item.getFile();
                    OneDriveFolder folder = item.getFolder();
                    String name = item.getName();
                    if (file != null || folder != null) {
                        String parentPath = item.getParentRef().getPath();
                        String pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                        String pathId = item.getId();
                        boolean isNxl = FileUtils.getRealFileExtension(name).equals(Constants.NXL_FILE_EXTN);
                        if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                            continue;
                        }
                        ApplicationRepositoryContent content = new ApplicationRepositoryContent();
                        if (file != null) {
                            content.setFileSize(item.getSize());
                            content.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                            content.setProtectedFile(isNxl);
                        }
                        content.setFileId(item.getId());
                        content.setLastModifiedTime(item.getLastModifiedTime() != null ? item.getLastModifiedTime().getTime() : null);
                        content.setFolder(folder != null);
                        content.setName(name);
                        content.setPath(pathName);
                        content.setPathId(pathId);

                        rt.add(content);
                    }
                }
            }
        } catch (OneDriveServiceException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }

        return rt;
    }

    @Override
    public ApplicationRepositoryContent getFileMetadata(String path)
            throws ApplicationRepositoryException, RepositoryException {
        try {
            OneDriveItem item = client.getItem(path);
            OneDriveFile file = item.getFile();
            OneDriveFolder folder = item.getFolder();
            String name = item.getName();

            if (file != null || folder != null) {
                String parentPath = item.getParentRef().getPath();
                String pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                String pathId = item.getId();
                boolean isNxl = FileUtils.getRealFileExtension(name).equals(Constants.NXL_FILE_EXTN);
                ApplicationRepositoryContent content = new ApplicationRepositoryContent();
                if (file != null) {
                    content.setFileSize(item.getSize());
                    content.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                    content.setProtectedFile(isNxl);
                }
                content.setFileId(item.getId());
                content.setLastModifiedTime(item.getLastModifiedTime() != null ? item.getLastModifiedTime().getTime() : null);
                content.setFolder(folder != null);
                content.setName(name);
                content.setPath(pathName);
                content.setPathId(pathId);

                return content;
            }
        } catch (RepositoryException | IOException | OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath)
            throws ApplicationRepositoryException, RepositoryException, OneDriveServiceException, IOException {
        return client.getFile(filePath, outputPath);
    }

    @Override
    public List<ApplicationRepositoryContent> search(String searchString)
            throws ApplicationRepositoryException, RepositoryException {
        List<ApplicationRepositoryContent> rt = new ArrayList<>();
        try {
            List<OneDriveItems> oneDriveItems = client.searchItems("/", searchString);
            if (oneDriveItems.isEmpty()) {
                return rt;
            }
            for (OneDriveItems items : oneDriveItems) {
                if (items == null) {
                    continue;
                }
                List<OneDriveItem> value = items.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                for (OneDriveItem item : value) {
                    if (item == null) {
                        continue;
                    }
                    OneDriveFile file = item.getFile();
                    OneDriveFolder folder = item.getFolder();
                    String name = item.getName();

                    if (file != null || folder != null) {
                        String parentPath = item.getParentRef().getPath();
                        String pathId = item.getId();

                        String pathName;
                        if (parentPath != null) {
                            pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                        } else {
                            pathName = "/" + name;
                        }

                        ApplicationRepositoryContent result = new ApplicationRepositoryContent();
                        result.setFolder(folder != null);
                        result.setName(name);
                        result.setFileId(item.getId());
                        result.setPath(pathName);
                        result.setPathId(pathId);
                        result.setLastModifiedTime(item.getLastModifiedTime() != null ? item.getLastModifiedTime().getTime() : null);
                        if (!result.isFolder()) {
                            result.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                            result.setProtectedFile(Constants.NXL_FILE_EXTN.equals(FileUtils.getRealFileExtension(name)));
                            result.setFileSize(item.getSize());
                        }
                        rt.add(result);
                    }
                }
            }

        } catch (OneDriveServiceException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }

        return rt;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException, ApplicationRepositoryException {
        byte[] recievedBytes = null;
        try {
            recievedBytes = client.getPartialItem(filePath, startByte, endByte);
        } catch (IOException | OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return recievedBytes;
    }

    @Override
    public List<File> downloadFiles(String parentPathId, String parentPathName, String[] filenames, String outputPath)
            throws ApplicationRepositoryException {
        return null;
    }

    @Override
    public FileUploadMetadata uploadFile(String filePathId, String filePathDisplay,
        String srcFilePath, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws ApplicationRepositoryException, RepositoryException {

        if ("/".equals(filePathId) || "/".equals(filePathDisplay)) {
            try {
                filePathDisplay = "/";
                filePathId = client.getRoot().getId();
            } catch (OneDriveServiceException | IOException e) {
                OneDriveOAuthHandler.handleException(e);
            }
        }
        try {
            File localFile = new File(srcFilePath);
            return client.upload(filePathId, filePathDisplay, localFile, overwrite, conflictFileName);
        } catch (IOException e) {
            OneDriveOAuthHandler.handleException(e);
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public FileDeleteMetaData deleteFile(String filePathId, String filePath)
            throws ApplicationRepositoryException, RepositoryException {
        if ("/".equals(filePathId)) {
            throw new ApplicationRepositoryException("Invalid pathId" + filePathId);
        }
        try {
            client.deleteItem(filePathId, filePath);
        } catch (IOException e) {
            //            e.printStackTrace();
        } catch (OneDriveServiceException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public String getCurrentFolderId(String path)
            throws RepositoryException, ApplicationRepositoryException {
        String currentFolderId = null;
        try {
            OneDriveItem object = client.getItem(path);
            if (object.getFile() != null) {
                OneDriveParentReference parentFolderRef = object.getParentRef();
                currentFolderId = parentFolderRef.getId();
            } else {
                currentFolderId = object.getId();
            }
        } catch (OneDriveServiceException | RepositoryException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return currentFolderId;
    }

    @Override
    public boolean checkIfFileExists(String path) throws ApplicationRepositoryException, RepositoryException {
        String fileId = null;
        try {
            OneDriveItem item = client.getItem(path);
            if (item.getFile() != null) {
                fileId = item.getId();
            }
        } catch (OneDriveServiceException | RepositoryException | IOException e) {
            OneDriveOAuthHandler.handleException(e);
        }
        return fileId != null;
    }

    private final class Client {

        private static final String API_ROOT = "https://graph.microsoft.com/v1.0";
        public static final String DRIVE_ROOT = "/drive/root:";
        //        private static final String FILE_PATH_API = API_ROOT + DRIVE_ROOT;
        private static final String FILE_ID_API = API_ROOT + "/drive/items/";

        private static final String AUTHORIZATION_HEADER = "Authorization";
        private static final String BEARER_SPACE = "Bearer ";
        private static final String ITEM_ATTR_FILTER = "id,name,file,folder,parentReference,@microsoft.graph.downloadUrl,lastModifiedDateTime,size";
        private static final String MAX_RESULT_SIZE = "200";
        private static final String CONFLICT_BEHAVIOUR_REPLACE = "replace";
        private static final String CONFLICT_BEHAVIOUR_RENAME = "rename";
        private static final String CONTENT_TYPE_HEADER = "Content-Type";
        private static final String CONTENT_TYPE_JSON = "application/json";
        private static final int FILE_FRAGMENT_SIZE = 1048576; // 10 MB
        public static final String CONTENT_RANGE_HEADER = "Content-Range";
        private static final String RANGE_HEADER = "Range";
        private static final String CONTENT = "/content";
        public static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
        public static final int READ_TIMEOUT = 5 * 60 * 1000;
        private static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        private static final String FILE_PATH_API = API_ROOT + DRIVE_ROOT;

        private final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, OneDriveResponseHandler.DATE_JSON_DESERIALIZER).create();

        String getAccessToken() throws ApplicationRepositoryException, RepositoryException {
            IRefreshTokenCallback callback = () -> {
                try {
                    ApplicationOneDriveOAuthHandler.OneDriveApplicationInfo info = new ApplicationOneDriveOAuthHandler.OneDriveApplicationInfo(tenantId, clientId, clientSecret);
                    OneDriveTokenResponse result = ApplicationOneDriveOAuthHandler.getAccessToken(info);
                    Map<String, Object> attrs = new HashMap<>();
                    if (result != null) {
                        attrs.put("access_token", result.getAccessToken());
                        long now = System.currentTimeMillis() / 1000;
                        attrs.put("access_token_expiry_time", result.getExpiresInSeconds() + now);
                    }
                    attributes.clear();
                    attributes.putAll(attrs);
                    return result;
                } catch (UnauthorizedApplicationRepositoryException e) { //NOPMD
                    throw e;
                } catch (OneDriveOAuthException | ApplicationRepositoryException e) {
                    throw new InvalidTokenException(e.getMessage(), e);
                } catch (Exception e) {
                    LOGGER.error("Error occurred when requesting new access token : {}", e.getMessage(), e);
                    throw new InvalidTokenException(e.getMessage(), e);
                }
            };

            ITokenResponse response;
            try {
                String accessToken = (String)attributes.get("access_token");
                if (accessToken == null || accessToken.isEmpty() || attributes.get("access_token_expiry_time") == null) {
                    response = callback.execute();
                } else {
                    long now = System.currentTimeMillis() / 1000;
                    Long expiresOn = (Long)attributes.get("access_token_expiry_time");
                    if (expiresOn == null || now > expiresOn - 180) {
                        response = callback.execute();
                    } else {
                        return (String)attributes.get("access_token");
                    }
                }
                return response.getAccessToken();
            } catch (Exception e) {
                if (e instanceof ApplicationRepositoryException) {
                    throw e;
                }
                LOGGER.error("Error occurred when getting token: {}", e.getMessage(), e);
                OneDriveOAuthHandler.handleException(e);
            }
            return null;
        }

        public OneDriveItem getRoot() throws OneDriveServiceException,
                ApplicationRepositoryException, IOException, RepositoryException {
            String accessToken = getAccessToken();
            String uri = API_ROOT + "/drive/root";
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
                HttpGet get = new HttpGet(uri + "?" + queryParams);
                get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + accessToken);
                try (CloseableHttpResponse response = client.execute(get)) {
                    OneDriveResponseHandler<OneDriveItem> handler = new OneDriveResponseHandler<>(OneDriveItem.class);
                    return handler.handle(response);
                }
            }
        }

        List<OneDriveItems> listItemsWithPath(String path)
                throws OneDriveServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            String accessToken = getAccessToken();

            List<OneDriveItems> ret = new ArrayList<>();
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            String url = API_ROOT + "/drive/items/root{path}/children";
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            if ("/".equals(path)) {
                url = url.replace("{path}", "") + "?" + queryParams;
            } else {
                url = url.replace("{path}", ":" + UrlEscapers.urlFragmentEscaper().escape(path) + ":") + "?" + queryParams;
            }

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String nextPageUrl;
                do {
                    HttpGet get = new HttpGet(url);
                    get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + accessToken);
                    try (CloseableHttpResponse response = client.execute(get)) {
                        OneDriveResponseHandler<OneDriveItems> handler = new OneDriveResponseHandler<>(OneDriveItems.class);
                        OneDriveItems item = handler.handle(response);
                        nextPageUrl = null;
                        if (item != null) {
                            ret.add(item);
                            nextPageUrl = item.getNextLink();
                            url = nextPageUrl;
                        }
                    }
                } while (nextPageUrl != null && nextPageUrl.length() > 0);
            }
            return ret;
        }

        public byte[] getPartialItem(String filePath, long startByte, long endByte)
                throws IOException, RepositoryException, ApplicationRepositoryException {
            if (endByte < startByte) {
                throw new IllegalArgumentException("Invalid range");
            }

            OneDriveItem item = client.getItem(filePath);
            OneDriveFolder folder = item.getFolder();
            if (folder != null) {
                throw new FileNotFoundException("Requested item is a folder: " + filePath);
            }

            try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
                HttpGet get = new HttpGet(FILE_ID_API + item.getId() + CONTENT);
                get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                get.setHeader(RANGE_HEADER, "bytes=" + startByte + '-' + endByte);
                try (CloseableHttpResponse response = client.execute(get)) {
                    com.nextlabs.rms.shared.IHTTPResponseHandler<InputStream> handler = new OneDriveResponseHandler<>(InputStream.class);
                    try (InputStream is = handler.handle(response)) {
                        try (ByteArrayOutputStream os = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                            IOUtils.copy(is, os);
                            return os.toByteArray();
                        }
                    }
                }
            }
        }

        File getFile(String filePath, String outPath)
                throws ApplicationRepositoryException, RepositoryException, OneDriveServiceException, IOException {
            String accessToken = getAccessToken();

            OneDriveItem item = client.getItem(filePath);
            OneDriveFolder folder = item.getFolder();
            if (folder != null) {
                throw new FileNotFoundException("Requested item is a folder: " + filePath);
            }

            File rt = new File(outPath, item.getName());

            InputStream is = null;
            OutputStream os = null;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

                String url = API_ROOT + "/drive/items/{item-id}/content";
                url = url.replace("{item-id}", item.getId());

                HttpGet get = new HttpGet(url);
                get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + accessToken);
                try (CloseableHttpResponse response = client.execute(get)) {
                    OneDriveResponseHandler<InputStream> handler = new OneDriveResponseHandler<>(InputStream.class);
                    is = handler.handle(response);
                    os = new FileOutputStream(rt);

                    IOUtils.copy(is, os);
                    return rt;
                }
            } catch (IOException e) {
                OneDriveOAuthHandler.handleException(e);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
            return null;
        }

        void deleteItem(String fileId, String filePath)
                throws IOException, ApplicationRepositoryException, OneDriveServiceException, RepositoryException {
            String accessToken = getAccessToken();

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String url = API_ROOT + "/drive/items/{item-id}";
                url = url.replace("{item-id}", fileId);

                HttpDelete delete = new HttpDelete(url);
                delete.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + accessToken);
                try (CloseableHttpResponse response = client.execute(delete)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_NO_CONTENT) {
                        LOGGER.debug(filePath + " has been deleted from the repository.");
                    } else {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        LOGGER.error("Error occured while deleting file from repo: " + responseBody);
                        if (statusCode == HttpStatus.SC_NOT_FOUND) {
                            throw new FileNotFoundException(fileId + " does not exist in the repository.");
                        } else {
                            handleErrorResponse("Error occurred while deleting file from repo:" + responseBody, statusCode, responseBody);
                        }
                    }
                }
            }
        }

        FileUploadMetadata upload(String folderPathId, String folderPathDisplay, File file, boolean overwrite,
            String conflictFileName) throws ApplicationRepositoryException, IOException, RepositoryException {
            long fileSize = file.length();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Upload file to OneDrive : '{}', size: {}", file.getName(), fileSize);
            }
            OneDriveItem uploadeditem = null;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                OneDriveUploadSession session = getUploadUrl(client, folderPathId, file.getName(), overwrite, conflictFileName);
                Date expirationDate = session.getExpirationDateTime();
                String uploadUrl = session.getUploadUrl();
                HttpPut put = new HttpPut(uploadUrl);
                int minFragSize = fileSize > FILE_FRAGMENT_SIZE ? FILE_FRAGMENT_SIZE : (int)fileSize;
                long startByte = 0;
                long endByte = minFragSize - 1L;
                byte[] fileFragment = new byte[minFragSize];
                try (InputStream fis = new FileInputStream(file)) {
                    boolean uploading = true;
                    do {
                        if (endByte == fileSize - 1) {
                            int remainingBytes = (int)(endByte - startByte + 1);
                            fileFragment = new byte[remainingBytes];
                            int actual = fis.read(fileFragment);
                            if (actual != remainingBytes) {
                                LOGGER.warn("Some length mismatch during uploading file '{}' (length to read: {}, actual: {})", file.getName(), remainingBytes, actual);
                            }
                        } else {
                            int actual = fis.read(fileFragment, 0, minFragSize);
                            if (actual != minFragSize) {
                                LOGGER.warn("Some length mismatch during uploading file '{}' (length to read: {}, actual: {})", file.getName(), minFragSize, actual);
                            }
                        }
                        put.setEntity(new ByteArrayEntity(fileFragment));
                        put.setHeader(CONTENT_RANGE_HEADER, "bytes " + startByte + "-" + endByte + "/" + fileSize);
                        put.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());

                        if (new Date().after(expirationDate)) {
                            handleErrorResponse("Upload request session expired at " + new SimpleDateFormat(ISO_8601_DATETIME_FORMAT).format(expirationDate), 400, "Bad Request");
                        }

                        try (CloseableHttpResponse httpResponse = client.execute(put)) {
                            int responseCode = httpResponse.getStatusLine().getStatusCode();
                            String responseBody = EntityUtils.toString(httpResponse.getEntity());
                            if (responseCode == HttpStatus.SC_ACCEPTED) {
                                if (LOGGER.isTraceEnabled()) {
                                    LOGGER.trace("Uploading bytes {}-{}", startByte, endByte);
                                }
                                OneDriveUploadSession activeSession = gson.fromJson(responseBody, OneDriveUploadSession.class);
                                expirationDate = activeSession.getExpirationDateTime();
                                startByte += FILE_FRAGMENT_SIZE;
                                endByte += FILE_FRAGMENT_SIZE;
                                endByte = Math.min(endByte, fileSize - 1);
                            } else if (responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_OK) {
                                uploading = false;
                                uploadeditem = gson.fromJson(responseBody, OneDriveItem.class);
                            } else {
                                handleErrorResponse(responseBody, responseCode, httpResponse.getStatusLine().getReasonPhrase());
                            }
                        }
                    } while (uploading);
                }
                FileUploadMetadata metaData = new FileUploadMetadata();
                String parentFolder = uploadeditem.getParentRef().getPath().replaceFirst(DRIVE_ROOT, "");
                metaData.setFileNameWithTimeStamp(conflictFileName);
                metaData.setPathDisplay(parentFolder + "/" + uploadeditem.getName());
                metaData.setFileId(uploadeditem.getId());
                metaData.setLastModifiedTime(uploadeditem.getLastModifiedTime());
                metaData.setSize(uploadeditem.getSize());
                return metaData;
            }
        }

        private OneDriveUploadSession getUploadUrl(CloseableHttpClient client, String repoParentFolder, String fileName,
            boolean overwrite,
            String conflictFileName) throws IOException, ApplicationRepositoryException, RepositoryException {

            OneDriveUploadItem uploadRequest = new OneDriveUploadItem();
            String resourceName;
            if (overwrite) {
                resourceName = fileName;
                uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_REPLACE);
            } else {
                resourceName = conflictFileName;
                uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_RENAME);
            }

            String url = API_ROOT + "/drive/items/{itemId}:/{fileName}:/createUploadSession";
            url = url.replace("{itemId}", repoParentFolder).replace("{fileName}", UrlEscapers.urlFragmentEscaper().escape(resourceName));
            HttpPost post = new HttpPost(url);
            post.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
            post.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);

            OneDriveUploadItemWrapper wrapper = new OneDriveUploadItemWrapper(uploadRequest);
            StringEntity json = new StringEntity(gson.toJson(wrapper), "UTF-8");
            post.setEntity(json);

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    LOGGER.error("Error occured while requesting an upload url: " + responseBody);
                    throw new IOException("Error occured while requesting an upload url:" + responseBody);
                }
                return gson.fromJson(responseBody, OneDriveUploadSession.class);
            }
        }

        private void handleErrorResponse(String responseBody, int code, String errorMessage) {
            OneDriveErrorResponse error;
            try {
                error = new Gson().fromJson(responseBody, OneDriveErrorResponse.class);
            } catch (final Exception ex) {
                error = new OneDriveErrorResponse();
                OneDriveOAuthException.OneDriveResponse e = new OneDriveOAuthException.OneDriveResponse();
                OneDriveInnerResponse innerError = new OneDriveInnerResponse();
                innerError.setCode(ex.getMessage());
                e.setCode(String.valueOf(code));
                e.setMessage("Raw error: " + (StringUtils.hasText(responseBody) ? responseBody : ex.getMessage()));
                e.setInnerError(innerError);
                error.setError(e);
            }
            throw new OneDriveServiceException(code, errorMessage, error);
        }

        List<OneDriveItems> searchItems(String rootPathId, String searchString)
                throws OneDriveServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            List<OneDriveItems> rt = new ArrayList<>();
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            if (rootPathId == null || rootPathId.isEmpty()) {
                rootPathId = "root";
            }
            if ("/".equals(rootPathId)) {
                rootPathId = "root";
            }
            String url = FILE_ID_API + rootPathId + "/search(q='" + searchString + "')" + "?" + queryParams;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String token = null;
                do {
                    HttpGet get = new HttpGet(url);
                    get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                    try (CloseableHttpResponse response = client.execute(get)) {
                        OneDriveResponseHandler<OneDriveItems> handler = new OneDriveResponseHandler<>(OneDriveItems.class);
                        OneDriveItems item = handler.handle(response);
                        token = null;
                        if (item != null) {
                            rt.add(item);
                            token = item.getNextLink();
                            url = token;
                        }
                    }
                } while (token != null && token.length() > 0);
            }
            return rt;
        }

        private CloseableHttpClient getExtendedTimeoutHttpClient() {
            RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).setSocketTimeout(READ_TIMEOUT).build();
            return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        }

        /**
         * Note: This call is very slow and takes about 2 seconds to get response .
         */
        public OneDriveItem getItem(String filePath) throws OneDriveServiceException,
                RepositoryException, IOException, ApplicationRepositoryException {
            String uri;
            if (filePath == null || filePath.isEmpty()) {
                filePath = "/";
            }
            if ("/".equals(filePath)) {
                uri = API_ROOT + "/drive/root";
            } else {
                uri = (FILE_PATH_API) + UrlEscapers.urlFragmentEscaper().escape(filePath);
            }
            List<NameValuePair> params = new ArrayList<>(1);
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
                HttpGet get = new HttpGet(uri + "?" + queryParams);
                get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                try (CloseableHttpResponse response = client.execute(get)) {
                    OneDriveResponseHandler<OneDriveItem> handler = new OneDriveResponseHandler<>(OneDriveItem.class);
                    return handler.handle(response);
                }
            }
        }

    }

}
