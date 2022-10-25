package com.nextlabs.rms.repository.onedrive;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.IRefreshTokenCallback;
import com.nextlabs.rms.auth.ITokenResponse;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler.GrantType;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler.OneDriveAppInfo;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveInnerResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.repository.onedrive.type.OneDriveFolder;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItem;
import com.nextlabs.rms.repository.onedrive.type.OneDriveItems;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadItem;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadItemWrapper;
import com.nextlabs.rms.repository.onedrive.type.OneDriveUploadSession;
import com.nextlabs.rms.shared.LogConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

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

public class OneDrivePersonalClient extends Observable {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String CONTENT = "/content";
    public static final String API_ROOT = "https://api.onedrive.com/v1.0";
    public static final String DRIVE_ROOT = "/drive/root:";
    private static final String FILE_PATH_API = API_ROOT + DRIVE_ROOT;
    private static final String FILE_ID_API = API_ROOT + "/drive/items/";
    private static final String ROOT_PATH_API = API_ROOT + "/drive/root";
    private static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    public static final String CONTENT_RANGE_HEADER = "Content-Range";
    private static final String RANGE_HEADER = "Range";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String BEARER_SPACE = "Bearer ";
    private static final String ITEM_ATTR_FILTER = "id,name,file,folder,parentReference,@content.downloadUrl,lastModifiedDateTime,size";
    private static final String MAX_RESULT_SIZE = "200";
    private static final int FILE_FRAGMENT_SIZE = 1048576; // 10 MB
    private static final String CONFLICT_BEHAVIOUR_REPLACE = "replace";
    private static final String CONFLICT_BEHAVIOUR_RENAME = "rename";
    private static final String CREATE_FOLDER_PATH_SUFFIX = "/children";
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, OneDriveResponseHandler.DATE_JSON_DESERIALIZER).create();
    private final OneDriveRepository repository;

    public OneDrivePersonalClient(OneDriveRepository repository) {
        this.repository = repository;
    }

    protected String getToken() throws RepositoryException {
        IRefreshTokenCallback callback = () -> {
            String refreshToken = (String)repository.getAttributes().get(RepositoryManager.REFRESH_TOKEN);
            OneDriveAppInfo info = new OneDriveAppInfo(repository.getClientId(), repository.getClientSecret(), repository.getRedirectUrl());
            try {
                OneDriveTokenResponse result = OneDriveOAuthHandler.getAccessToken(info, refreshToken, GrantType.REFRESH_TOKEN);
                Map<String, Object> attrs = new HashMap<>();
                if (result != null) {
                    attrs.put(RepositoryManager.ACCESS_TOKEN, result.getAccessToken());
                    attrs.put(RepositoryManager.REFRESH_TOKEN, result.getRefreshToken());
                    long now = System.currentTimeMillis() / 1000;
                    attrs.put(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME, result.getExpiresInSeconds() + now);
                }

                repository.getAttributes().clear();
                repository.getAttributes().putAll(attrs);
                return result;
            } catch (UnauthorizedRepositoryException e) { //NOPMD
                throw e;
            } catch (OneDriveOAuthException | RepositoryException e) {
                throw new InvalidTokenException(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Error occurred when requesting new access token (repo ID: {}): {}", repository.getRepoId(), e.getMessage(), e);
                throw new InvalidTokenException(e.getMessage(), e);
            }
        };
        ITokenResponse response;
        try {
            Map<String, Object> attrMap = repository.getAttributes();
            if (StringUtils.hasText((String)attrMap.get(RepositoryManager.REFRESH_TOKEN)) && (!StringUtils.hasText((String)attrMap.get(RepositoryManager.ACCESS_TOKEN)) || attrMap.get(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME) == null)) {
                response = callback.execute();
            } else if (!StringUtils.hasText((String)attrMap.get(RepositoryManager.REFRESH_TOKEN))) {
                throw new InvalidTokenException("Invalid token");
            } else {
                long now = System.currentTimeMillis() / 1000;
                Long expiresOn = (Long)repository.getAttributes().get(RepositoryManager.ACCESS_TOKEN_EXPIRY_TIME);
                if (expiresOn == null || now > expiresOn - 180) {
                    // renew before 3 minutes to expire
                    response = callback.execute();
                } else {
                    return (String)repository.getAttributes().get(RepositoryManager.ACCESS_TOKEN);
                }
            }
            return response.getAccessToken();
        } catch (Exception e) {
            //            if (e instanceof RepositoryException) {
            //                throw e;
            //            }
            LOGGER.error("Error occurred when getting token: {}", e.getMessage(), e);
            OneDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    public List<OneDriveItems> listItems(String path, boolean isPathId)
            throws OneDriveServiceException, RepositoryException, IOException {
        String accessToken = getToken();
        List<OneDriveItems> results = new ArrayList<>();
        List<NameValuePair> params = new ArrayList<>(3);
        params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
        params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
        params.add(new BasicNameValuePair(ACCESS_TOKEN_PARAM, accessToken));
        String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
        String api = isPathId ? FILE_ID_API : FILE_PATH_API;
        String uri = api + UrlEscapers.urlFragmentEscaper().escape(path) + (isPathId ? "/children?" : ":/children?") + queryParams;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String token;
            do {
                HttpGet get = new HttpGet(uri);
                try (CloseableHttpResponse response = client.execute(get)) {
                    com.nextlabs.rms.shared.IHTTPResponseHandler<OneDriveItems> handler = new OneDriveResponseHandler<>(OneDriveItems.class);
                    OneDriveItems item = handler.handle(response);
                    token = null;
                    if (item != null) {
                        results.add(item);
                        token = item.getNextLink();
                        uri = token;
                    }
                }
            } while (token != null && token.length() > 0);
        }
        return results;
    }

    public List<OneDriveItems> searchItems(String dir, String searchString)
            throws OneDriveServiceException, RepositoryException, IOException {
        String accessToken = getToken();
        List<OneDriveItems> results = new ArrayList<>();
        List<NameValuePair> params = new ArrayList<>(5);
        params.add(new BasicNameValuePair("q", searchString));
        params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
        params.add(new BasicNameValuePair("filter", "contains(tolower(name),'" + searchString.toLowerCase() + "')"));
        params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
        params.add(new BasicNameValuePair(ACCESS_TOKEN_PARAM, accessToken));
        String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
        String uri = FILE_PATH_API + dir + ":/view.search?" + queryParams;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String token = null;
            do {
                HttpGet get = new HttpGet(uri);
                try (CloseableHttpResponse response = client.execute(get)) {
                    com.nextlabs.rms.shared.IHTTPResponseHandler<OneDriveItems> handler = new OneDriveResponseHandler<>(OneDriveItems.class);
                    OneDriveItems item = handler.handle(response);
                    token = null;
                    if (item != null) {
                        results.add(item);
                        token = item.getNextLink();
                        uri = token;
                    }
                }
            } while (token != null && token.length() > 0);
        }
        return results;
    }

    public UploadedFileMetaData upload(String folderPathId, String folderPathDisplay, File file, boolean overwrite,
        String conflictFileName)
            throws RepositoryException, IOException {
        long fileSize = file.length();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Upload file to OneDrive (repository ID: {}): '{}', size: {}", repository.getRepoId(), file.getName(), fileSize);
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
                    put.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());

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
                            OneDriveUploadSession activeSession = GSON.fromJson(responseBody, OneDriveUploadSession.class);
                            expirationDate = activeSession.getExpirationDateTime();
                            startByte += FILE_FRAGMENT_SIZE;
                            endByte += FILE_FRAGMENT_SIZE;
                            endByte = Math.min(endByte, fileSize - 1);
                        } else if (responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_OK) {
                            uploading = false;
                            uploadeditem = GSON.fromJson(responseBody, OneDriveItem.class);
                        } else {
                            handleErrorResponse(responseBody, responseCode, httpResponse.getStatusLine().getReasonPhrase());
                        }
                    }
                } while (uploading);
            }
            UploadedFileMetaData metaData = new UploadedFileMetaData();
            String parentFolder = uploadeditem.getParentRef().getPath().replaceFirst(DRIVE_ROOT, "");
            metaData.setFileNameWithTimeStamp(conflictFileName);
            metaData.setPathDisplay(URLDecoder.decode(parentFolder, "UTF-8") + "/" + uploadeditem.getName());
            metaData.setPathId(uploadeditem.getId());
            return metaData;
        }
    }

    private OneDriveUploadSession getUploadUrl(CloseableHttpClient client, String repoParentFolder, String fileName,
        boolean overwrite,
        String conflictFileName) throws IOException, RepositoryException {

        OneDriveUploadItem uploadRequest = new OneDriveUploadItem();
        String resourceName;
        if (overwrite) {
            resourceName = fileName;
            uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_REPLACE);
        } else {
            resourceName = conflictFileName;
            uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_RENAME);
            uploadRequest.setName(resourceName);
        }

        String uri = FILE_ID_API + URLEncoder.encode(repoParentFolder, "UTF-8") + ":/" + UrlEscapers.urlFragmentEscaper().escape(resourceName) + ":/upload.createSession";
        HttpPost post = new HttpPost(uri);
        post.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());
        post.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);

        OneDriveUploadItemWrapper wrapper = new OneDriveUploadItemWrapper(uploadRequest);
        StringEntity json = new StringEntity(GSON.toJson(wrapper), "UTF-8");
        post.setEntity(json);

        try (CloseableHttpResponse response = client.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOGGER.error("Error occured while requesting an upload url: " + responseBody);
                throw new IOException(RMSMessageHandler.getClientString("fileUploadErr"));
            }
            return GSON.fromJson(responseBody, OneDriveUploadSession.class);
        }
    }

    public OneDriveItem getItem(String filePath, boolean isPathId) throws OneDriveServiceException,
            RepositoryException, IOException {
        String accessToken = getToken();
        String uri = (isPathId ? FILE_ID_API : FILE_PATH_API) + URLEncoder.encode(filePath, "UTF-8");
        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
        params.add(new BasicNameValuePair(ACCESS_TOKEN_PARAM, accessToken));
        String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
        try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
            HttpGet get = new HttpGet(uri + "?" + queryParams);
            try (CloseableHttpResponse response = client.execute(get)) {
                com.nextlabs.rms.shared.IHTTPResponseHandler<OneDriveItem> handler = new OneDriveResponseHandler<>(OneDriveItem.class);
                return handler.handle(response);
            }
        }
    }

    public byte[] getPartialItem(String fileId, long startByte, long endByte) throws IOException, RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }
        try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
            HttpGet get = new HttpGet(FILE_ID_API + fileId + CONTENT);
            get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());
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

    public void deleteItem(String filePath, boolean isPathId) throws OneDriveServiceException, RepositoryException,
            IOException {
        String uri = (isPathId ? FILE_ID_API : FILE_PATH_API) + URLEncoder.encode(filePath, "UTF-8");
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpDelete delete = new HttpDelete(uri);
            delete.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());
            response = client.execute(delete);
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_NO_CONTENT) {
                LOGGER.debug(filePath + " has been deleted from the repository.");
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                LOGGER.error("Error occured while deleting file from repo: " + responseBody);
                if (code == HttpStatus.SC_NOT_FOUND) {
                    throw new FileNotFoundException(filePath + " does not exist in the repository.");
                } else {
                    handleErrorResponse("Error occurred while deleting file from repo:" + responseBody, code, responseBody);
                }
            }
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }

    public void createFolder(String folderName, String parentFolderId, boolean isPathId, boolean autoRename)
            throws OneDriveServiceException, RepositoryException, IOException {
        String uri = (isPathId ? FILE_ID_API : ROOT_PATH_API) + URLEncoder.encode(parentFolderId, "UTF-8") + CREATE_FOLDER_PATH_SUFFIX;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(uri);
            post.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());
            post.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);

            OneDriveUploadItem folder = new OneDriveUploadItem();
            folder.setName(folderName);
            folder.setFolder(new OneDriveFolder());
            folder.setConflictBehaviour(autoRename ? CONFLICT_BEHAVIOUR_RENAME : CONFLICT_BEHAVIOUR_REPLACE);

            StringEntity json = new StringEntity(GSON.toJson(folder), "UTF-8");
            post.setEntity(json);

            response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_CREATED) {
                String responseBody = EntityUtils.toString(response.getEntity());
                LOGGER.error("Error occurred while creating a folder: " + responseBody);
                handleErrorResponse("Error occurred while creating a folder:" + responseBody, code, responseBody);
            }
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }

    public String getPublicUrl(String fileId) throws RepositoryException, IOException, URISyntaxException {
        String uri = FILE_ID_API + fileId + "/action.createLink";
        OneDriveSharingLinkRequest req = new OneDriveSharingLinkRequest();
        req.setScope("anonymous");
        req.setType("embed");
        StringEntity json = new StringEntity(GSON.toJson(req), "UTF-8");
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(uri);
            post.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getToken());
            post.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);
            post.setEntity(json);

            response = client.execute(post);
            String responseBody = EntityUtils.toString(response.getEntity());
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_OK && code != HttpStatus.SC_CREATED) {
                LOGGER.error("Error occurred while generating public url: " + responseBody);
                handleErrorResponse("Error occurred while generating public url:" + responseBody, code, responseBody);
            } else {
                Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, OneDriveResponseHandler.DATE_JSON_DESERIALIZER).create();
                OneDrivePermission permission = gson.fromJson(responseBody, OneDrivePermission.class);
                String shareUrl = permission.getLink().getWebUrl();

                String path = new URI(shareUrl).getPath();
                if (StringUtils.equalsIgnoreCase(path, "/embed")) {
                    return shareUrl.replace("embed", "download");
                }
            }
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
        return null;
    }

    private void handleErrorResponse(String responseBody, int code, String errorMessage) {
        OneDriveErrorResponse error;
        try {
            error = GSON.fromJson(responseBody, OneDriveErrorResponse.class);
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

    private CloseableHttpClient getExtendedTimeoutHttpClient() {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(RepoConstants.CONNECTION_TIMEOUT).setConnectionRequestTimeout(RepoConstants.CONNECTION_TIMEOUT).setSocketTimeout(RepoConstants.READ_TIMEOUT).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}
