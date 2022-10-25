package com.nextlabs.rms.application.sharepoint;

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
import com.nextlabs.rms.application.sharepoint.auth.SharePointAppOAuthHandler;
import com.nextlabs.rms.application.sharepoint.exception.DriveNotFoundException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointErrorResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointInnerResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointOAuthException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointServiceException;
import com.nextlabs.rms.application.sharepoint.exception.SiteNotFoundException;
import com.nextlabs.rms.application.sharepoint.type.DriveItem;
import com.nextlabs.rms.application.sharepoint.type.DriveItems;
import com.nextlabs.rms.application.sharepoint.type.SharePointFile;
import com.nextlabs.rms.application.sharepoint.type.SharePointFolder;
import com.nextlabs.rms.application.sharepoint.type.SharePointItem;
import com.nextlabs.rms.application.sharepoint.type.SharePointItems;
import com.nextlabs.rms.application.sharepoint.type.SharePointSiteItem;
import com.nextlabs.rms.application.sharepoint.type.SharePointUploadItem;
import com.nextlabs.rms.application.sharepoint.type.SharePointUploadItemWrapper;
import com.nextlabs.rms.application.sharepoint.type.SharePointUploadSession;
import com.nextlabs.rms.auth.IRefreshTokenCallback;
import com.nextlabs.rms.auth.ITokenResponse;
import com.nextlabs.rms.exception.ApplicationRepositoryException;
import com.nextlabs.rms.exception.UnauthorizedApplicationRepositoryException;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.shared.IHTTPResponseHandler;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
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

public class SharePointApplicationRepository implements IApplicationRepository {

    private static final Logger LOGGER = LogManager.getLogger("com.nextlabs.rms.server");
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    private String driveId;

    private final Client client;
    private final Map<String, Object> attributes = new HashMap<>();

    public SharePointApplicationRepository(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.client = new Client();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void configDrive(String siteUrl, String driveName)
            throws ApplicationRepositoryException, RepositoryException {
        if (siteUrl == null || siteUrl.isEmpty()) {
            throw new IllegalArgumentException("Invalid site url.");
        }
        if (driveName == null || driveName.isEmpty()) {
            throw new IllegalArgumentException("Invalid drive name.");
        }
        try {
            DriveItem drive = client.getDrive(siteUrl, driveName);
            if (drive == null) {
                throw new DriveNotFoundException("Drive not found.");
            }
            driveId = drive.getId();
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
    }

    public boolean isSiteValid(String siteUrl)
            throws ApplicationRepositoryException, RepositoryException {
        if (siteUrl == null || siteUrl.isEmpty()) {
            throw new IllegalArgumentException("Invalid site url.");
        }
        try {
            SharePointSiteItem site = client.getSite(siteUrl);
            return site != null;
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return false;
    }

    public boolean isDriveValid(String siteUrl, String driveName)
            throws ApplicationRepositoryException, RepositoryException {
        if (siteUrl == null || siteUrl.isEmpty()) {
            throw new IllegalArgumentException("Invalid site url.");
        }
        if (driveName == null || driveName.isEmpty()) {
            throw new IllegalArgumentException("Invalid drive name.");
        }
        try {
            DriveItem drive = client.getDrive(siteUrl, driveName);
            return drive != null;
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return false;
    }

    @Override
    public List<ApplicationRepositoryContent> getFileList(String path)
            throws ApplicationRepositoryException, RepositoryException {
        return getFileList(path, new FilterOptions());
    }

    @Override
    public List<ApplicationRepositoryContent> getFileList(String path, FilterOptions filterOptions)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        List<ApplicationRepositoryContent> rt = new ArrayList<>();
        try {
            List<SharePointItem> sharePointItems = client.listItemByPath(driveId, path);
            for (SharePointItem item : sharePointItems) {
                if (item == null) {
                    continue;
                }
                SharePointFile file = item.getFile();
                SharePointFolder folder = item.getFolder();
                String name = item.getName();
                if (file != null || folder != null) {
                    String parentPath = item.getParentRef().getPath();
                    String pathName;
                    if (driveId == null || driveId.isEmpty()) {
                        pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                    } else {
                        pathName = parentPath.replaceFirst(Client.DRIVES + driveId + Client.ROOT, "") + "/" + name;
                    }
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
                    content.setLastModifiedTime(item.getLstModifiedTime() != null ? item.getLstModifiedTime().getTime() : null);
                    content.setFolder(folder != null);
                    content.setName(name);
                    content.setPath(pathName.toLowerCase());
                    content.setPathId(pathId);

                    rt.add(content);
                }
            }
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return rt;
    }

    @Override
    public ApplicationRepositoryContent getFileMetadata(String path)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        try {
            SharePointItem item = client.getItem(driveId, path);

            SharePointFile file = item.getFile();
            SharePointFolder folder = item.getFolder();
            String name = item.getName();

            if (file != null || folder != null) {
                String parentPath = item.getParentRef().getPath();
                String pathName;
                if (driveId == null || driveId.isEmpty()) {
                    pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                } else {
                    pathName = parentPath.replaceFirst(Client.DRIVES + driveId + Client.ROOT, "") + "/" + name;
                }
                String pathId = item.getId();
                boolean isNxl = FileUtils.getRealFileExtension(name).equals(Constants.NXL_FILE_EXTN);
                ApplicationRepositoryContent content = new ApplicationRepositoryContent();
                if (file != null) {
                    content.setFileSize(item.getSize());
                    content.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                    content.setProtectedFile(isNxl);
                }
                content.setFileId(item.getId());
                content.setCreatedTime(item.getCreatedDateTime() != null ? item.getCreatedDateTime().getTime() : null);
                content.setLastModifiedTime(item.getLstModifiedTime() != null ? item.getLstModifiedTime().getTime() : null);
                content.setFolder(folder != null);
                content.setName(name);
                content.setPath(pathName.toLowerCase());
                content.setPathId(pathId);

                return content;
            }
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath)
            throws ApplicationRepositoryException, RepositoryException, OneDriveServiceException, IOException {
        checkConfiguration();
        return client.getFileByPath(driveId, filePath, outputPath);
    }

    @Override
    public List<ApplicationRepositoryContent> search(String searchString)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        List<ApplicationRepositoryContent> rt = new ArrayList<>();
        try {
            List<SharePointItems> sharePointItems = client.searchItems(driveId, "/", searchString);
            if (sharePointItems.isEmpty()) {
                return rt;
            }
            for (SharePointItems items : sharePointItems) {
                if (items == null) {
                    continue;
                }
                List<SharePointItem> value = items.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                for (SharePointItem item : value) {
                    if (item == null) {
                        continue;
                    }
                    SharePointFile file = item.getFile();
                    SharePointFolder folder = item.getFolder();
                    String name = item.getName();

                    if (file != null || folder != null) {
                        String parentPath = item.getParentRef().getPath();
                        String pathId = item.getId();

                        String pathName;
                        if (parentPath != null) {
                            if (driveId == null || driveId.isEmpty()) {
                                pathName = parentPath.replaceFirst(Client.DRIVE_ROOT, "") + "/" + name;
                            } else {
                                pathName = parentPath.replaceFirst(Client.DRIVES + driveId + Client.ROOT, "") + "/" + name;
                            }
                        } else {
                            pathName = "/" + name;
                        }

                        ApplicationRepositoryContent result = new ApplicationRepositoryContent();
                        result.setFolder(folder != null);
                        result.setName(name);
                        result.setFileId(item.getId());
                        result.setPath(pathName.toLowerCase());
                        result.setPathId(pathId);
                        result.setLastModifiedTime(item.getLstModifiedTime() != null ? item.getLstModifiedTime().getTime() : null);
                        result.setCreatedTime(item.getCreatedDateTime() != null ? item.getCreatedDateTime().getTime() : null);
                        if (!result.isFolder()) {
                            result.setFileType(RepositoryFileUtil.getOriginalFileExtension(name));
                            result.setProtectedFile(Constants.NXL_FILE_EXTN.equals(FileUtils.getRealFileExtension(name)));
                            result.setFileSize(item.getSize());
                        }
                        rt.add(result);
                    }
                }
            }
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return rt;
    }

    @Override
    public List<File> downloadFiles(String parentPathId, String parentPathName,
        String[] filenames, String outputPath)
            throws ApplicationRepositoryException {
        return null;
    }

    @Override
    public FileUploadMetadata uploadFile(String filePathId, String filePath,
        String localFile, boolean overwrite,
        String conflictFileName,
        Map<String, String> customMetadata)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        if (localFile == null || localFile.isEmpty()) {
            throw new IllegalArgumentException("Invalid src file path.");
        }
        try {
            File src = new File(localFile);
            if (!src.exists() || !src.isFile()) {
                throw new IllegalStateException("Invalid src file status.");
            }
            return client.uploadByPath(driveId, filePath, src, overwrite, conflictFileName);
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public FileDeleteMetaData deleteFile(String filePathId, String filePath)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Invalid delete path.");
        }
        if ("/".equals(filePath)) {
            throw new ApplicationRepositoryException("Invalid path " + filePath + ".");
        }
        try {
            client.deleteItemByPath(driveId, filePath);
            return new FileDeleteMetaData();
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public String getCurrentFolderId(String path)
            throws RepositoryException, ApplicationRepositoryException {
        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath,
        long startByte, long endByte)
            throws RepositoryException, ApplicationRepositoryException {
        checkConfiguration();
        try {
            return client.getPartialItem(driveId, filePath, startByte, endByte);
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public boolean checkIfFileExists(String filePath)
            throws ApplicationRepositoryException, RepositoryException {
        checkConfiguration();
        try {
            return client.getItem(driveId, filePath) != null;
        } catch (SharePointServiceException | IOException e) {
            SharePointAppOAuthHandler.handleException(e);
        }
        return false;
    }

    private void checkConfiguration() throws ApplicationRepositoryException {
        if (driveId == null || driveId.isEmpty()) {
            throw new ApplicationRepositoryException("Please configure drive first before invoking api.");
        }
    }

    private final class Client {

        private static final String API_ROOT = "https://graph.microsoft.com/v1.0";
        private static final String DRIVE_ROOT = "/drive/root:";
        private static final String DRIVES = "/drives/";
        private static final String ROOT = "/root:";
        private static final String AUTHORIZATION_HEADER = "Authorization";
        private static final String BEARER_SPACE = "Bearer ";
        private static final String ITEM_ATTR_FILTER = "id,name,file,folder,parentReference,@microsoft.graph.downloadUrl,lastModifiedDateTime,size";
        private static final String MAX_RESULT_SIZE = "200";
        private static final String CONFLICT_BEHAVIOUR_REPLACE = "replace";
        private static final String CONFLICT_BEHAVIOUR_RENAME = "rename";
        private static final String CONTENT_TYPE_HEADER = "Content-Type";
        private static final String CONTENT_TYPE_JSON = "application/json";
        private static final int FILE_FRAGMENT_SIZE = 1048576; // 10 MB
        private static final String CONTENT_RANGE_HEADER = "Content-Range";
        private static final String RANGE_HEADER = "Range";
        public static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
        public static final int READ_TIMEOUT = 5 * 60 * 1000;
        private static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        private final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, SharePointResponseHandler.DATE_JSON_DESERIALIZER).create();

        String getAccessToken() throws ApplicationRepositoryException, RepositoryException {
            IRefreshTokenCallback callback = () -> {
                try {
                    SharePointAppOAuthHandler.SharePointApplicationInfo info = new SharePointAppOAuthHandler.SharePointApplicationInfo(tenantId, clientId, clientSecret);
                    ITokenResponse result = SharePointAppOAuthHandler.getAccessToken(info);
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
                } catch (SharePointOAuthException | ApplicationRepositoryException e) {
                    throw new InvalidTokenException(e.getMessage(), e);
                } catch (Exception e) {
                    //LOGGER.error("Error occurred when requesting new access token : {}", e.getMessage(), e);
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
                LOGGER.error("Error occurred when getting token: {}", e.getMessage(), e);
                throw e;
            }

        }

        SharePointSiteItem getSite(String siteUrl)
                throws SharePointServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            if (siteUrl == null || siteUrl.isEmpty()) {
                throw new IllegalArgumentException("Invalid site url.");
            }
            if (siteUrl.endsWith("/")) {
                siteUrl = siteUrl.substring(0, siteUrl.lastIndexOf('/'));
            }
            URI uri = URI.create(siteUrl);
            String host = uri.getHost();
            String path = uri.getPath();

            String url;
            if (path == null || path.isEmpty()) {
                url = API_ROOT + "/sites/" + host;
            } else {
                url = API_ROOT + "/sites/" + host + ":" + path + ":/";
            }

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(url);
                get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());

                try (CloseableHttpResponse response = client.execute(get)) {
                    IHTTPResponseHandler<SharePointSiteItem> handler = new SharePointResponseHandler<>(SharePointSiteItem.class);
                    return handler.handle(response);
                } catch (SharePointServiceException e) {
                    if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        throw new SiteNotFoundException(e.getMessage(), e);
                    }
                    throw e;
                }
            }
        }

        DriveItem getDrive(String siteUrl, String driveName)
                throws SharePointServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            if (siteUrl == null || siteUrl.isEmpty()) {
                throw new IllegalArgumentException("Invalid site url.");
            }
            if (driveName == null || driveName.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive name.");
            }
            List<DriveItem> driveItems = listDrives(siteUrl);
            if (driveItems.isEmpty()) {
                return null;
            }
            DriveItem rt = null;
            for (DriveItem item : driveItems) {
                if (item == null) {
                    continue;
                }
                if (StringUtils.equalsIgnoreCase(item.getName(), driveName)) {
                    rt = item;
                    break;
                }
            }
            return rt;
        }

        List<DriveItem> listDrives(String siteUrl)
                throws SharePointServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            if (siteUrl == null || siteUrl.isEmpty()) {
                throw new IllegalArgumentException("Invalid site url.");
            }

            List<DriveItem> rt = new ArrayList<>();
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);
            SharePointSiteItem site = getSite(siteUrl);

            String url = API_ROOT + "/sites/" + site.getId() + "/drives" + "?" + queryParams;

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String nextPageUrl;
                do {
                    HttpGet get = new HttpGet(url);

                    get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                    try (CloseableHttpResponse response = client.execute(get)) {
                        IHTTPResponseHandler<DriveItems> handler = new SharePointResponseHandler<>(DriveItems.class);
                        DriveItems item = handler.handle(response);
                        nextPageUrl = null;
                        if (item != null) {
                            List<DriveItem> value = item.getValue();
                            if (value != null) {
                                rt.addAll(value);
                            }
                            nextPageUrl = item.getNextLink();
                            url = nextPageUrl;
                        }
                    }
                } while (nextPageUrl != null && nextPageUrl.length() > 0);
            }
            return rt;
        }

        List<SharePointItem> listItemByPath(String driveId, String itemPath)
                throws SharePointServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                itemPath = "/";
            }

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);

            String formatter = API_ROOT + "/drives/{drive-id}/items/{item-id}/children" + "?" + queryParams;

            String url = formatter.replace("{drive-id}", driveId).replace("{item-id}", "/".equals(itemPath) ? "root" : "root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath) + ":");

            List<SharePointItem> rt = new ArrayList<>();
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String nextPageUrl;
                do {
                    HttpGet get = new HttpGet(url);
                    get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                    try (CloseableHttpResponse response = client.execute(get)) {
                        IHTTPResponseHandler<SharePointItems> handler = new SharePointResponseHandler<>(SharePointItems.class);
                        SharePointItems item = handler.handle(response);
                        nextPageUrl = null;
                        if (item != null) {
                            List<SharePointItem> value = item.getValue();
                            if (value != null) {
                                rt.addAll(value);
                            }
                            nextPageUrl = item.getNextLink();
                            url = nextPageUrl;
                        }
                    }
                } while (nextPageUrl != null && nextPageUrl.length() > 0);
            }
            return rt;
        }

        File getFileByPath(String driveId, String itemPath, String outPath)
                throws ApplicationRepositoryException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                throw new IllegalArgumentException("Invalid item path.");
            }
            if ("/".equals(itemPath) || itemPath.endsWith("/")) {
                throw new IllegalStateException("Invalid item path.");
            }

            String formatter = API_ROOT + "/drives/{drive-id}/items/{item-id}/content";
            String uri = formatter.replace("{drive-id}", driveId).replace("{item-id}", "root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath) + ":");

            InputStream is = null;
            OutputStream os = null;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

                HttpGet get = new HttpGet(uri);
                get.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                try (CloseableHttpResponse response = client.execute(get)) {
                    File rt = new File(outPath);
                    //if out-path is a dir then try getting name from content-disposition header
                    if (rt.isDirectory()) {
                        String fileName = getFileNameFromContentDisposition(response);
                        //if file-name unable to parse from content-disposition header then
                        //try parsing from item-path.
                        if (fileName == null || fileName.isEmpty()) {
                            int idx = itemPath.lastIndexOf('/');
                            if (idx != -1) {
                                fileName = itemPath.substring(idx + 1);
                            } else {
                                fileName = "unknown";
                            }
                        }
                        rt = new File(outPath, fileName);
                    }
                    IHTTPResponseHandler<InputStream> handler = new SharePointResponseHandler<>(InputStream.class);
                    is = handler.handle(response);
                    os = new FileOutputStream(rt);

                    IOUtils.copy(is, os);
                    return rt;
                }
            } catch (IOException e) {
                SharePointAppOAuthHandler.handleException(e);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
            return null;
        }

        byte[] getPartialItem(String driveId, String itemPath, long startByte, long endByte)
                throws IOException, ApplicationRepositoryException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                throw new IllegalArgumentException("Invalid item path.");
            }
            if ("/".equals(itemPath) || itemPath.endsWith("/")) {
                throw new IllegalStateException("Invalid item path.");
            }
            if (startByte < 0) {
                throw new IllegalArgumentException("Invalid start range.");
            }
            if (endByte < 0) {
                throw new IllegalArgumentException("Invalid end range.");
            }
            if (endByte < startByte) {
                throw new IllegalArgumentException("Invalid range.");
            }

            String formatter = API_ROOT + "/drives/{drive-id}/items/{item-id}/content";
            String uri = formatter.replace("{drive-id}", driveId).replace("{item-id}", "root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath) + ":");

            try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
                HttpGet get = new HttpGet(uri);
                get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                get.setHeader(RANGE_HEADER, "bytes=" + startByte + '-' + endByte);
                try (CloseableHttpResponse response = client.execute(get)) {
                    IHTTPResponseHandler<InputStream> handler = new SharePointResponseHandler<>(InputStream.class);
                    try (InputStream is = handler.handle(response)) {
                        try (ByteArrayOutputStream os = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                            IOUtils.copy(is, os);
                            return os.toByteArray();
                        }
                    }
                }
            }
        }

        void deleteItemByPath(String driveId, String itemPath)
                throws IOException, ApplicationRepositoryException, SharePointServiceException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                throw new IllegalArgumentException("Invalid item path.");
            }
            if ("/".equals(itemPath)) {
                throw new IllegalStateException("Invalid item path.");
            }

            String formatter = API_ROOT + "/drives/{drive-id}/items/{item-id}";
            String uri = formatter.replace("{drive-id}", driveId).replace("{item-id}", "root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath) + ":");

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpDelete delete = new HttpDelete(uri);
                delete.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                try (CloseableHttpResponse response = client.execute(delete)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_NO_CONTENT) {
                        LOGGER.debug(itemPath + " has been deleted from the repository.");
                    } else {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        LOGGER.error("Error occured while deleting file from repo: " + responseBody);
                        if (statusCode == HttpStatus.SC_NOT_FOUND) {
                            throw new FileNotFoundException(itemPath + " does not exists in the repository.");
                        } else {
                            handleErrorResponse("Error occurred while deleting file from repo:" + responseBody, statusCode, responseBody);
                        }
                    }
                }
            }
        }

        FileUploadMetadata uploadByPath(String driveId, String parentPath,
            File file, boolean overwrite, String conflictFileName)
                throws ApplicationRepositoryException, IOException, RepositoryException {
            long fileSize = file.length();
            SharePointItem uploadeditem = null;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                SharePointUploadSession session = getUploadUrlByPath(driveId, parentPath, file.getName(), overwrite, conflictFileName);

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
                                SharePointUploadSession activeSession = gson.fromJson(responseBody, SharePointUploadSession.class);
                                expirationDate = activeSession.getExpirationDateTime();
                                startByte += FILE_FRAGMENT_SIZE;
                                endByte += FILE_FRAGMENT_SIZE;
                                endByte = Math.min(endByte, fileSize - 1);
                            } else if (responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_OK) {
                                uploading = false;
                                uploadeditem = gson.fromJson(responseBody, SharePointItem.class);
                            } else {
                                handleErrorResponse(responseBody, responseCode, httpResponse.getStatusLine().getReasonPhrase());
                            }
                        }
                    } while (uploading);
                }
                if (uploadeditem == null) {
                    return null;
                }
                FileUploadMetadata metaData = new FileUploadMetadata();
                String parentFolder = uploadeditem.getParentRef().getPath().replaceFirst(Client.DRIVES + driveId + Client.ROOT, "");
                metaData.setFileNameWithTimeStamp(uploadeditem.getName());
                metaData.setName(uploadeditem.getName());
                metaData.setPathDisplay(parentFolder + "/" + uploadeditem.getName());
                metaData.setFileId(uploadeditem.getId());
                metaData.setCreatedDateTime(uploadeditem.getCreatedDateTime());
                metaData.setLastModifiedTime(uploadeditem.getLstModifiedTime());
                metaData.setSize(uploadeditem.getSize());
                return metaData;
            }
        }

        private SharePointUploadSession getUploadUrlByPath(String driveId, String parentPath,
            String fileName, boolean overwrite,
            String conflictFileName)
                throws IOException, ApplicationRepositoryException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (parentPath == null || parentPath.isEmpty()) {
                parentPath = "/";
            }
            SharePointUploadItem uploadRequest = new SharePointUploadItem();
            String resourceName;
            if (overwrite) {
                resourceName = fileName;
                uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_REPLACE);
            } else {
                resourceName = conflictFileName;
                uploadRequest.setConflictBehaviour(CONFLICT_BEHAVIOUR_RENAME);
            }

            String formatter = API_ROOT + "/drives/{drive-id}/items/{item-id}:/{fileName}:/createUploadSession";
            String uri = formatter.replace("{drive-id}", driveId).replace("{item-id}:", "/".equals(parentPath) ? "root:" : "root:" + parentPath).replace("{fileName}", resourceName);

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpPost post = new HttpPost(UrlEscapers.urlFragmentEscaper().escape(uri));
                post.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                post.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);

                SharePointUploadItemWrapper wrapper = new SharePointUploadItemWrapper(uploadRequest);
                StringEntity json = new StringEntity(gson.toJson(wrapper), "UTF-8");
                post.setEntity(json);
                try (CloseableHttpResponse response = client.execute(post)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        throw new IOException("Error occurred while requesting an upload url:" + responseBody);
                    }
                    return gson.fromJson(responseBody, SharePointUploadSession.class);
                }
            }
        }

        public SharePointItem getItem(String driveId, String itemPath)
                throws SharePointServiceException, ApplicationRepositoryException, IOException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                itemPath = "/";
            }

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);

            String formatter;
            if ("/".equals(itemPath)) {
                formatter = API_ROOT + "/drives/{drive-id}/root";
            } else {
                formatter = API_ROOT + "/drives/{drive-id}/root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath);
            }

            String uri = formatter.replace("{drive-id}", driveId);

            try (CloseableHttpClient client = getExtendedTimeoutHttpClient()) {
                HttpGet get = new HttpGet(uri + "?" + queryParams);
                get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                try (CloseableHttpResponse response = client.execute(get)) {
                    IHTTPResponseHandler<SharePointItem> handler = new SharePointResponseHandler<>(SharePointItem.class);
                    return handler.handle(response);
                }
            }
        }

        List<SharePointItems> searchItems(String driveId, String itemPath, String searchString)
                throws ApplicationRepositoryException, SharePointServiceException, IOException, RepositoryException {
            if (driveId == null || driveId.isEmpty()) {
                throw new IllegalArgumentException("Invalid drive id.");
            }
            if (itemPath == null || itemPath.isEmpty()) {
                itemPath = "/";
            }
            List<SharePointItems> rt = new ArrayList<>();

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("select", ITEM_ATTR_FILTER));
            params.add(new BasicNameValuePair("top", MAX_RESULT_SIZE));
            String queryParams = URLEncodedUtils.format(params, StandardCharsets.ISO_8859_1);

            searchString = UrlEscapers.urlFragmentEscaper().escape(searchString);
            String formatter = "";
            if ("/".equals(itemPath)) {
                formatter = API_ROOT + "/drives/{drive-id}/root" + "/search(q='" + searchString + "')";
            } else {
                formatter = API_ROOT + "/drives/{drive-id}/root:" + UrlEscapers.urlFragmentEscaper().escape(itemPath) + ":" + "/search(q='" + searchString + "')";
            }

            String uri = formatter.replace("{drive-id}", driveId);

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String token;
                do {
                    HttpGet get = new HttpGet(uri + "?" + queryParams);
                    get.setHeader(AUTHORIZATION_HEADER, BEARER_SPACE + getAccessToken());
                    try (CloseableHttpResponse response = client.execute(get)) {
                        IHTTPResponseHandler<SharePointItems> handler = new SharePointResponseHandler<>(SharePointItems.class);
                        SharePointItems item = handler.handle(response);
                        token = null;
                        if (item != null) {
                            rt.add(item);
                            token = item.getNextLink();
                            uri = token;
                        }
                    }
                } while (token != null && token.length() > 0);
            }
            return rt;
        }

        private String getFileNameFromContentDisposition(CloseableHttpResponse response) {
            if (response == null) {
                return null;
            }
            Header[] headers = response.getHeaders("Content-Disposition");
            for (Header h : headers) {
                if (h == null) {
                    continue;
                }
                HeaderElement[] elements = h.getElements();
                for (HeaderElement e : elements) {
                    if (e == null) {
                        continue;
                    }
                    NameValuePair pair = e.getParameterByName("filename");
                    if (pair != null) {
                        return pair.getValue();
                    }
                }
            }
            return null;
        }

        private void handleErrorResponse(String responseBody, int code, String errorMessage) {
            SharePointErrorResponse error;
            try {
                error = new Gson().fromJson(responseBody, SharePointErrorResponse.class);
            } catch (final Exception ex) {
                error = new SharePointErrorResponse();
                SharePointResponse e = new SharePointResponse();
                SharePointInnerResponse innerError = new SharePointInnerResponse();
                innerError.setCode(ex.getMessage());
                e.setCode(String.valueOf(code));
                e.setMessage("Raw error: " + (StringUtils.hasText(responseBody) ? responseBody : ex.getMessage()));
                e.setInnerError(innerError);
                error.setError(e);
            }
            throw new SharePointServiceException(code, errorMessage, error);
        }

        private CloseableHttpClient getExtendedTimeoutHttpClient() {
            RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).setSocketTimeout(READ_TIMEOUT).build();
            return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        }
    }
}
