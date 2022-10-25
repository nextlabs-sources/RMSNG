package com.nextlabs.rms.repository.sharepoint;

import com.google.common.net.UrlEscapers;
import com.google.gson.JsonObject;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.sharepoint.response.ContextInfo;
import com.nextlabs.rms.repository.sharepoint.response.CreateAnonymousLink;
import com.nextlabs.rms.repository.sharepoint.response.DocumentList;
import com.nextlabs.rms.repository.sharepoint.response.DocumentList.DataType;
import com.nextlabs.rms.repository.sharepoint.response.SPRestList;
import com.nextlabs.rms.repository.sharepoint.response.SharePointBase;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFileMetadata;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFileUploadResponse;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFoldersMetadata;
import com.nextlabs.rms.repository.sharepoint.response.SharePointSearchJson;
import com.nextlabs.rms.repository.sharepoint.response.SharePointSearchJson.Cells;
import com.nextlabs.rms.repository.sharepoint.response.SharePointSearchJson.Rows;
import com.nextlabs.rms.repository.sharepoint.response.verbose.ContextInfoVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.DocumentListVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SPRestListVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointBaseVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointFileMetadataVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointFileUploadResponseVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointFoldersMetadataVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointSearchJsonVerbose;
import com.nextlabs.rms.repository.sharepoint.response.verbose.SharePointSearchJsonVerbose.RowResults;
import com.nextlabs.rms.shared.IHTTPResponseHandler;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SharePointClient {

    private AUTH_TYPE authType;
    private RESPONSE_TYPE responseType;
    private boolean useId;
    private ServiceProviderType sharePointType;
    private String siteRelativeUrl;
    private HttpClientContext ntlmContext;

    static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    static final String ORIGINAL_PATH_FIELD = "OriginalPath";
    static final String SECONDARY_FILE_EXTENSION_FIELD = "SecondaryFileExtension";
    static final String PARENT_LINK_FIELD = "ParentLink";
    static final String LAST_MODIFIED_FIELD = "LastModifiedTime";
    static final String SIZE_FIELD = "Size";
    static final String UNIQUE_ID_FIELD = "UniqueId";
    static final String TITLE_FIELD = "Title";
    static final String SP_DEFAULT_TITLE = "DispForm.aspx";

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String ACCEPT_HEADER = "Accept";
    static final String IF_MATCH_HEADER = "IF-MATCH";
    static final String X_REQUEST_DIGEST_HEADER = "X-RequestDigest";

    static final String BEARER_SPACE = "Bearer ";
    static final String APPLICATION_JSON = "application/json";
    static final String APPLICATION_JSON_VERBOSE = "application/json;odata=verbose";
    static final String CONTENT_TYPE = "Content-Type";
    static final String DOCUMENTS = "Documents";
    static final String SHARED_DOCUMENTS = "Shared Documents";

    static final String BASE_URL = "%s/_api/web";

    static final String BASE_GET_FILE_BY_PATH_URL = BASE_URL + "/GetFileByServerRelativeUrl('%s')";
    static final String BASE_GET_FOLDER_BY_PATH_URL = BASE_URL + "/GetFolderByServerRelativeUrl('%s')";
    static final String BASE_GET_FOLDER_BY_ID_URL = BASE_URL + "/GetFolderById('%s')";
    static final String LIST_FOLDER_BY_PATH_URL = BASE_GET_FOLDER_BY_PATH_URL + "/folders";
    static final String LIST_FILES_BY_PATH_URL = BASE_GET_FOLDER_BY_PATH_URL + "/files";
    static final String LIST_FOLDER_BY_ID_URL = BASE_GET_FOLDER_BY_ID_URL + "/folders";
    static final String LIST_FILES_BY_ID_URL = BASE_GET_FOLDER_BY_ID_URL + "/files";
    static final String UPLOAD_FILE_BY_ID_URL = BASE_GET_FOLDER_BY_ID_URL + "/Files/Add(url='%s',overwrite=%b)";
    static final String UPLOAD_FILE_BY_PATH_URL = BASE_GET_FOLDER_BY_PATH_URL + "/Files/Add(url='%s',overwrite=%b)";

    static final String BASE_GET_FILE_BY_ID_URL = BASE_URL + "/GetFileById('%s')";
    static final String DOWNLOAD_FILE_BY_ID_URL = BASE_GET_FILE_BY_ID_URL + "/$value";
    static final String DOWNLOAD_FILE_BY_PATH_URL = BASE_GET_FILE_BY_PATH_URL + "/$value";

    static final String LIST_ROOT_SITE_URL = BASE_URL + "/lists";
    static final String BASE_LIST_BY_GUID_URL = LIST_ROOT_SITE_URL + "(guid'%s')";
    static final String ROOT_FOLDER_BY_GUID_URL = BASE_LIST_BY_GUID_URL + "/RootFolder";
    static final String FOLDERS_URL = BASE_URL + "/folders";

    public SharePointClient(AUTH_TYPE authType, RESPONSE_TYPE responseType, boolean useId,
        ServiceProviderType sharePointType) {
        this.authType = authType;
        this.responseType = responseType;
        this.useId = useId;
        this.sharePointType = sharePointType;
    }

    private HttpHost getSharePointHost(String siteUrl) throws MalformedURLException {
        URL serverUrl = new URL(siteUrl);
        String host = serverUrl.getHost();
        int port = serverUrl.getPort();
        String protocol = serverUrl.getProtocol();
        return new HttpHost(host, port, protocol);
    }

    private void setSharepointClientMetadata(CloseableHttpClient client, String spServer, Object authPrincipal)
            throws ClientProtocolException, IOException { //need to be called when a new httpclient is initialized
        if (authType == AUTH_TYPE.NTLM) {
            ntlmContext = (HttpClientContext)authPrincipal;
        }
        if (sharePointType == ServiceProviderType.SHAREPOINT_ONPREMISE) {
            HttpGet request = new HttpGet(String.format(BASE_URL, spServer));
            if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                request.addHeader(ACCEPT_HEADER, APPLICATION_JSON);
            } else {
                request.addHeader(ACCEPT_HEADER, APPLICATION_JSON_VERBOSE);
            }
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, request)) {
                if (!useId) {
                    if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                        IHTTPResponseHandler<SharePointBase> handler = new SharePointResponseHandler<>(SharePointBase.class);
                        SharePointBase base = handler.handle(response);
                        siteRelativeUrl = base.getServerRelativeUrl();
                    } else {
                        IHTTPResponseHandler<SharePointBaseVerbose> handler = new SharePointResponseHandler<>(SharePointBaseVerbose.class);
                        SharePointBaseVerbose baseVerbose = handler.handle(response);
                        siteRelativeUrl = baseVerbose.getBase().getServerRelativeUrl();
                    }
                }
                Header[] headers = response.getAllHeaders();
                if (headers == null) {
                    responseType = RESPONSE_TYPE.JSON_VERBOSE;
                    return;
                }
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].getName() != null && headers[i].getValue() != null && "MicrosoftSharePointTeamServices".equalsIgnoreCase(headers[i].getName())) {
                        if (headers[i].getValue().startsWith("16")) { //SharePoint 2016
                            responseType = RESPONSE_TYPE.JSON_BASIC;
                            break;
                        } else {
                            responseType = RESPONSE_TYPE.JSON_VERBOSE; //SharePoint 2013
                            break;
                        }
                    }
                }
            }
        }
    }

    private String prependPathWithSiteRelativeUrl(String path) {
        return !useId && StringUtils.hasText(siteRelativeUrl) && !path.startsWith(siteRelativeUrl) ? siteRelativeUrl + path : path;
    }

    private List<RepositoryContent> toRepositoryContents(final String parentPath,
        final List<DocumentList.Results> input, String repoId, String repoName, FilterOptions filterOptions) {
        List<RepositoryContent> results = new ArrayList<>(input.size());
        for (DocumentList.Results result : input) {
            String fileName = result.getName();
            String id = result.getId();
            boolean file = result.isFile();
            RepositoryContent content = new RepositoryContent();
            content.setUsePathId(useId);
            content.setName(fileName);

            content.setFileSize(result.getSize());
            if (file) {
                boolean isNxl = fileName.toLowerCase().endsWith(Constants.NXL_FILE_EXTN);
                if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                    continue;
                }
                content.setFolder(false);
                content.setFileType(RepositoryFileUtil.getOriginalFileExtension(fileName));
                content.setProtectedFile(isNxl);
            } else {
                content.setFolder(true);
            }
            if (result.getLastModifiedTime() != null) {
                try {
                    Date date = DateUtils.parseISO8601(result.getLastModifiedTime());
                    content.setLastModifiedTime(date.getTime());
                } catch (ParseException e) {
                    content.setLastModifiedTime(null);
                }
            }
            String pathDisplay;
            if (StringUtils.hasText(siteRelativeUrl)) {
                pathDisplay = result.getServerRelativeUrl().replaceFirst(siteRelativeUrl, "");
            } else {
                pathDisplay = result.getServerRelativeUrl();
            }
            pathDisplay = pathDisplay.replaceFirst("/", "");
            content.setPath("/" + pathDisplay);
            content.setFileId(id);
            if (!useId) {
                content.setPathId(content.getPath().toLowerCase());
            } else {
                content.setPathId(parentPath + "/" + id);
            }
            content.setRepoId(repoId);
            content.setRepoName(repoName);
            content.setRepoType(sharePointType);
            results.add(content);
        }
        return results;
    }

    private String getPathIdByPathDisplay(String pathDisplay, String siteUrl, Object authPrincipal)
            throws RepositoryException, IOException {
        pathDisplay = UrlEscapers.urlFragmentEscaper().escape(pathDisplay);
        final String metadataUri = String.format(SharePointClient.BASE_GET_FOLDER_BY_PATH_URL, siteUrl, pathDisplay);
        HttpGet get = new HttpGet(metadataUri);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            setSharepointClientMetadata(client, siteUrl, authPrincipal);
            setRequestHeader(get, authPrincipal);
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, get)) {
                if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                    IHTTPResponseHandler<SharePointFileMetadata> handler = new SharePointResponseHandler<>(SharePointFileMetadata.class);
                    SharePointFileMetadata metadata = handler.handle(response);
                    return metadata.getUniqueId();
                } else {
                    IHTTPResponseHandler<SharePointFileMetadataVerbose> handler = new SharePointResponseHandler<>(SharePointFileMetadataVerbose.class);
                    SharePointFileMetadataVerbose metadataVerbose = handler.handle(response);
                    return metadataVerbose.getMetadata().getUniqueId();
                }
            }
        }
    }

    private void setRequestHeader(HttpRequestBase request, Object oauthToken) {
        if (responseType == RESPONSE_TYPE.JSON_BASIC) {
            request.addHeader(ACCEPT_HEADER, APPLICATION_JSON);
        } else {
            request.addHeader(ACCEPT_HEADER, APPLICATION_JSON_VERBOSE);
        }
        if (authType == AUTH_TYPE.OAUTH) {
            request.addHeader(AUTHORIZATION_HEADER, BEARER_SPACE + (String)oauthToken);
        }
    }

    private CloseableHttpResponse getCloseableHttpResponse(CloseableHttpClient client, String siteUrl,
        HttpRequestBase request) throws ClientProtocolException, IOException {
        CloseableHttpResponse response = null;
        if (authType == AUTH_TYPE.OAUTH) {
            response = client.execute(request);
        } else {
            response = client.execute(getSharePointHost(siteUrl), request, ntlmContext);
        }
        return response;
    }

    public List<RepositoryContent> getFileList(String path, Object authPrincipal, String spServer, String repoId,
        String repoName, FilterOptions filterOptions) throws RepositoryException, IOException {
        if ("/".equalsIgnoreCase(path)) {
            return getSiteContents(authPrincipal, spServer, repoName, repoId);
        }
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        if (useId && path.startsWith("TempId")) {
            String pathDisplay = path.substring(path.indexOf(':') + 1);
            path = getPathIdByPathDisplay(pathDisplay, siteUrl, authPrincipal);
        }
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        try {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                setSharepointClientMetadata(client, spServer, authPrincipal);
                path = prependPathWithSiteRelativeUrl(path);
                boolean hideForm = false;
                List<String> pathToken = StringUtils.tokenize(path, "/");
                final String resource = pathToken.get(pathToken.size() - 1);
                String site = path.substring(0, path.indexOf(resource) - 1);
                if ((useId && pathToken.size() == 1) || (!useId && StringUtils.endsWithIgnoreCase(URLDecoder.decode(siteUrl, "UTF-8"), site))) {
                    URIBuilder uriBuilder;
                    if (useId) {
                        uriBuilder = new URIBuilder(String.format(FOLDERS_URL, siteUrl));
                        uriBuilder.addParameter("$filter", "UniqueId eq guid'" + resource + "'");
                    } else {
                        uriBuilder = new URIBuilder(String.format(FOLDERS_URL, siteUrl));
                        uriBuilder.addParameter("$filter", "Name eq '" + resource + "'");
                    }
                    HttpGet httpGet = new HttpGet(uriBuilder.build());
                    setRequestHeader(httpGet, authPrincipal);
                    try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, httpGet)) {
                        List<SharePointFileMetadata> list;
                        if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                            IHTTPResponseHandler<SharePointFoldersMetadata> handler = new SharePointResponseHandler<>(SharePointFoldersMetadata.class);
                            SharePointFoldersMetadata foldersMetadata = handler.handle(response);
                            list = foldersMetadata.getValues();
                        } else {
                            IHTTPResponseHandler<SharePointFoldersMetadataVerbose> handler = new SharePointResponseHandler<>(SharePointFoldersMetadataVerbose.class);
                            SharePointFoldersMetadataVerbose foldersMetadata = handler.handle(response);
                            list = foldersMetadata.getResults();
                        }
                        hideForm = list != null && !list.isEmpty();
                    }
                }
                final String folderUrl;
                final String fileUrl;
                if (useId) {
                    folderUrl = String.format(LIST_FOLDER_BY_ID_URL, siteUrl, resource);
                    fileUrl = String.format(LIST_FILES_BY_ID_URL, siteUrl, resource);
                } else {
                    folderUrl = String.format(LIST_FOLDER_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(path));
                    fileUrl = String.format(LIST_FILES_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(path));
                }
                URIBuilder uriBuilder = new URIBuilder(folderUrl);
                if (hideForm) {
                    uriBuilder.addParameter("$filter", "Name ne 'Forms'");
                }
                URI uri = uriBuilder.build();
                HttpGet get = new HttpGet(uri);
                setRequestHeader(get, authPrincipal);
                List<DocumentList.Results> folders = null;
                try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                    if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                        IHTTPResponseHandler<DocumentList> handler = new SharePointResponseHandler<>(DocumentList.class);
                        DocumentList list = handler.handle(response);
                        folders = list.getResults();
                    } else {
                        IHTTPResponseHandler<DocumentListVerbose> handler = new SharePointResponseHandler<>(DocumentListVerbose.class);
                        DocumentListVerbose list = handler.handle(response);
                        folders = list.getResults();
                    }
                }
                List<RepositoryContent> folderList = toRepositoryContents(path, folders, repoId, repoName, filterOptions);
                if (!folderList.isEmpty()) {
                    contentList.addAll(folderList);
                }
                if (!filterOptions.showOnlyFolders()) {
                    uriBuilder = new URIBuilder(fileUrl);
                    get.setURI(uriBuilder.build());
                    List<DocumentList.Results> files = null;
                    try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                        if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                            IHTTPResponseHandler<DocumentList> handler = new SharePointResponseHandler<>(DocumentList.class);
                            DocumentList list = handler.handle(response);
                            files = list.getResults();
                        } else {
                            IHTTPResponseHandler<DocumentListVerbose> handler = new SharePointResponseHandler<>(DocumentListVerbose.class);
                            DocumentListVerbose list = handler.handle(response);
                            files = list.getResults();
                        }
                    }
                    List<RepositoryContent> fileList = toRepositoryContents(path, files, repoId, repoName, filterOptions);
                    if (!fileList.isEmpty()) {
                        contentList.addAll(fileList);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return contentList;
    }

    public String getRootFolderNameByTitle(Object authPrincipal, String spServer, String title) throws IOException {
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            for (SPRestList.Results eachResult : getDocumentLibraries(client, siteUrl, authPrincipal)) {
                if (eachResult.getTitle().equals(title)) {
                    String id = eachResult.getId();
                    URIBuilder builder = new URIBuilder(String.format(ROOT_FOLDER_BY_GUID_URL, siteUrl, id));
                    HttpGet get = new HttpGet(builder.build());
                    setRequestHeader(get, authPrincipal);
                    SharePointFileMetadata rootFolder = null;
                    try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                        if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                            IHTTPResponseHandler<SharePointFileMetadata> handler = new SharePointResponseHandler<>(SharePointFileMetadata.class);
                            rootFolder = handler.handle(response);
                        } else {
                            IHTTPResponseHandler<SharePointFileMetadataVerbose> handler = new SharePointResponseHandler<>(SharePointFileMetadataVerbose.class);
                            SharePointFileMetadataVerbose rootFolderVerbose = handler.handle(response);
                            rootFolder = rootFolderVerbose.getMetadata();
                        }
                        return rootFolder.getName();
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return null;
    }

    private List<SPRestList.Results> getDocumentLibraries(CloseableHttpClient client, String siteUrl,
        Object authPrincipal) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(String.format(LIST_ROOT_SITE_URL, siteUrl));
            builder.addParameter("$select", "Title,LastItemModifiedDate,Created,Id,ParentWebUrl");
            builder.addParameter("$filter", "(BaseTemplate eq 101 or BaseTemplate eq 700) and IsSiteAssetsLibrary eq false and Title ne 'Site Assets' and Title ne 'Style Library'");
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);

            setSharepointClientMetadata(client, siteUrl, authPrincipal);
            setRequestHeader(get, authPrincipal);
            List<SPRestList.Results> results = null;
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, get)) {
                if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                    IHTTPResponseHandler<SPRestList> handler = new SharePointResponseHandler<>(SPRestList.class);
                    SPRestList list = handler.handle(response);
                    results = list.getResults();
                } else {
                    IHTTPResponseHandler<SPRestListVerbose> handler = new SharePointResponseHandler<>(SPRestListVerbose.class);
                    SPRestListVerbose list = handler.handle(response);
                    results = list.getResults();
                }
                return results;
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private List<RepositoryContent> getSiteContents(Object authPrincipal, String spServer, String repoName,
        String repoId) throws IOException {
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        try {
            final String siteUrl = StringUtils.stripTrailing(spServer, "/");
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                for (SPRestList.Results eachResult : getDocumentLibraries(client, siteUrl, authPrincipal)) {
                    RepositoryContent content = new RepositoryContent();
                    String title = eachResult.getTitle();
                    String id = eachResult.getId();
                    URIBuilder builder = new URIBuilder(String.format(ROOT_FOLDER_BY_GUID_URL, siteUrl, id));
                    HttpGet get = new HttpGet(builder.build());
                    setRequestHeader(get, authPrincipal);
                    SharePointFileMetadata rootFolder = null;
                    try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                        if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                            IHTTPResponseHandler<SharePointFileMetadata> handler = new SharePointResponseHandler<>(SharePointFileMetadata.class);
                            rootFolder = handler.handle(response);
                        } else {
                            IHTTPResponseHandler<SharePointFileMetadataVerbose> handler = new SharePointResponseHandler<>(SharePointFileMetadataVerbose.class);
                            SharePointFileMetadataVerbose rootFolderVerbose = handler.handle(response);
                            rootFolder = rootFolderVerbose.getMetadata();
                        }
                    }
                    if (eachResult.getLastModifiedTime() != null) {
                        try {
                            Date date = DateUtils.parseISO8601(eachResult.getLastModifiedTime());
                            content.setLastModifiedTime(date.getTime());
                        } catch (ParseException ex) {
                            content.setLastModifiedTime(null);
                        }
                    }
                    content.setName(title);
                    content.setFolder(true);
                    content.setUsePathId(useId);
                    String pathDisplay;
                    if (StringUtils.hasText(siteRelativeUrl)) {
                        pathDisplay = rootFolder.getServerRelativeUrl().replaceFirst(siteRelativeUrl, "");
                    } else {
                        pathDisplay = rootFolder.getServerRelativeUrl();
                    }
                    pathDisplay = pathDisplay.replaceFirst("/", "");
                    content.setPath("/" + pathDisplay);
                    if (!useId) {
                        content.setPathId(content.getPath().toLowerCase());
                    } else {
                        content.setPathId("/" + rootFolder.getUniqueId());
                    }
                    content.setFileId(rootFolder.getUniqueId());
                    content.setRepoId(repoId + "");
                    content.setRepoName(repoName);
                    contentList.add(content);
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return contentList;
    }

    public byte[] downloadFile(String fileId, Object authPrincipal, String spServer) throws RepositoryException,
            IOException {
        try (InputStream is = downloadAsStream(fileId, authPrincipal, spServer)) {
            return IOUtils.toByteArray(is);
        }
    }

    public InputStream downloadAsStream(String path, Object authPrincipal, String spServer) throws RepositoryException,
            IOException {
        List<String> pathToken = StringUtils.tokenize(path, "/");
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        final String uniqueId = pathToken.get(pathToken.size() - 1);

        CloseableHttpClient client = getExtendedTimeoutHttpClient();
        setSharepointClientMetadata(client, spServer, authPrincipal);
        path = prependPathWithSiteRelativeUrl(path);
        String uri;
        if (useId) {
            uri = String.format(DOWNLOAD_FILE_BY_ID_URL, siteUrl, uniqueId);
        } else {
            uri = String.format(DOWNLOAD_FILE_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(path));
        }
        HttpGet get = new HttpGet(uri);
        setRequestHeader(get, authPrincipal);
        HttpResponse response = getCloseableHttpResponse(client, spServer, get);
        IHTTPResponseHandler<InputStream> handler = new SharePointResponseHandler<>(InputStream.class);
        return handler.handle(response);
    }

    public List<RepositoryContent> search(String searchText, Object authPrincipal, int searchlimit, String repoName,
        String repoId, ServiceProviderType repoType, String spServer) throws IOException {
        List<RepositoryContent> contentList = new ArrayList<RepositoryContent>();
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            setSharepointClientMetadata(client, spServer, authPrincipal);
            spServer = spServer.charAt(spServer.length() - 1) == '/' ? spServer : spServer + "/";
            String decodedServerUrl = URLDecoder.decode(spServer, "UTF-8");
            URIBuilder builder = new URIBuilder(spServer + "_api/search/query");
            builder.addParameter("querytext", "'" + searchText + "* site:\"" + decodedServerUrl + "\"'");
            builder.addParameter("refinementfilters", "'filename:(\"*" + searchText + "*\")'");
            builder.addParameter("trimduplicates", "false");
            builder.addParameter("rowlimit", String.valueOf(searchlimit));
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            setRequestHeader(get, authPrincipal);
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                List<Rows> rowResults = null;
                if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                    IHTTPResponseHandler<SharePointSearchJson> handler = new SharePointResponseHandler<>(SharePointSearchJson.class);
                    SharePointSearchJson helper = handler.handle(response);
                    rowResults = helper.getPrimaryQueryResult().getRelevantResults().getTable().getRows();
                } else {
                    IHTTPResponseHandler<SharePointSearchJsonVerbose> handler = new SharePointResponseHandler<>(SharePointSearchJsonVerbose.class);
                    SharePointSearchJsonVerbose helper = handler.handle(response);
                    List<RowResults> rowResultsVerbose = helper.getRowResults();
                    rowResults = new ArrayList<Rows>(rowResultsVerbose.size());
                    for (RowResults result : rowResultsVerbose) {
                        rowResults.add(new Rows(result.getCellresult().getCells()));
                    }
                }

                for (Rows eachResult : rowResults) {
                    List<Cells> cellResultList = eachResult.getCells();
                    RepositoryContent sr = new RepositoryContent();
                    String title = null;
                    String fullPath = null;
                    String fileExt = null;
                    String parentLink = null;
                    Date lastModifiedDate = null;
                    long fileSize = 0;
                    String uniqueString = null;
                    for (Cells eachMap : cellResultList) {
                        if (ORIGINAL_PATH_FIELD.equals(eachMap.getKey())) {
                            fullPath = eachMap.getValue();
                        } else if (SECONDARY_FILE_EXTENSION_FIELD.equals(eachMap.getKey())) {
                            fileExt = eachMap.getValue();
                        } else if (PARENT_LINK_FIELD.equals(eachMap.getKey())) {
                            parentLink = eachMap.getValue();
                        } else if (LAST_MODIFIED_FIELD.equals(eachMap.getKey())) {
                            try {
                                if (eachMap.getValue() != null) {
                                    lastModifiedDate = DateUtils.parseISO8601(eachMap.getValue());
                                }
                            } catch (ParseException e) {
                                LOGGER.debug("Invalid date: " + eachMap.getValue(), e);
                            }
                        } else if (SIZE_FIELD.equals(eachMap.getKey())) {
                            try {
                                fileSize = Long.parseLong(eachMap.getValue());
                            } catch (NumberFormatException nfe) {
                                LOGGER.debug("Invalid size: " + fileSize, nfe);
                            }
                        } else if (TITLE_FIELD.equals(eachMap.getKey())) {
                            title = eachMap.getValue();
                        } else if (UNIQUE_ID_FIELD.equals(eachMap.getKey())) {
                            uniqueString = eachMap.getValue();
                        }
                    }

                    if (!StringUtils.equalsIgnoreCase(decodedServerUrl, parentLink + "/") && parentLink != null && StringUtils.startsWithIgnoreCase(parentLink, decodedServerUrl)) {
                        boolean isFolder = fileExt == null ? true : false;
                        String titleFromPath = fullPath.substring(fullPath.lastIndexOf('/') + 1, fullPath.length());
                        /*
                         * sometimes sharepoint-online returns paths of the form /Documents/boomerang/DispForm.aspx?ID=554
                         */
                        if (titleFromPath.contains(SP_DEFAULT_TITLE)) {
                            if (!isFolder) {
                                title = new StringBuilder(title).append(".").append(fileExt).toString();
                            }
                        } else {
                            title = titleFromPath;
                        }

                        String relativePath = null;
                        String[] parentLinkArr = parentLink.split("(?i)" + decodedServerUrl);
                        String parentRelativePath = parentLinkArr[1];
                        String[] parentFolderArr = parentRelativePath.split("/");
                        if (parentFolderArr.length > 1 && StringUtils.equals("Forms", parentFolderArr[1])) {
                            sr.setName(title);
                            relativePath = parentFolderArr[0] + "/" + title;
                        } else {
                            sr.setName(title);
                            parentRelativePath = new StringBuilder(parentRelativePath).append('/').append(title).toString();
                            relativePath = parentRelativePath;
                        }

                        sr.setPath("/" + relativePath);
                        sr.setUsePathId(useId);
                        if (useId && uniqueString != null) {
                            //uniqueID contains {}, i.e: {A5E10EFB-9C89-4AAB-BB6A-5F756E53B9FA}
                            final String uniqueId = uniqueString.replaceAll("\\{", "").replaceAll("\\}", "").toLowerCase();
                            sr.setPathId("/" + uniqueId);
                            sr.setFileId(uniqueId);
                        } else {
                            sr.setPathId(sr.getPath());
                        }
                        sr.setFolder(isFolder);
                        if (!isFolder) {
                            sr.setFileSize(fileSize);
                        }
                        sr.setRepoId(repoId + "");
                        sr.setRepoName(repoName);
                        sr.setRepoType(repoType);
                        if (lastModifiedDate != null) {
                            sr.setLastModifiedTime(lastModifiedDate.getTime());
                        }
                        if (title != null) {
                            sr.setFileType(RepositoryFileUtil.getOriginalFileExtension(title));
                            sr.setProtectedFile(title.toLowerCase().endsWith(Constants.NXL_FILE_EXTN));
                        }
                        contentList.add(sr);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return contentList;
    }

    public void delete(String spServer, Object authPrincipal, String path)
            throws IOException, RepositoryException {
        List<String> pathToken = StringUtils.tokenize(path, "/");
        final String itemId = pathToken.get(pathToken.size() - 1);
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            setSharepointClientMetadata(client, spServer, authPrincipal);
            String fileUri;
            path = prependPathWithSiteRelativeUrl(path);
            if (useId) {
                fileUri = String.format(BASE_GET_FILE_BY_ID_URL, siteUrl, itemId);
            } else {
                fileUri = String.format(BASE_GET_FILE_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(path));
            }
            HttpGet get = new HttpGet(fileUri);
            setRequestHeader(get, authPrincipal);
            int fileStatusCode = -1;
            int folderStatusCode = -1;
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, get)) {
                fileStatusCode = response.getStatusLine().getStatusCode();
            }
            String deleteUri = null;
            if (fileStatusCode == HttpStatus.SC_OK) {
                deleteUri = fileUri;
            } else {
                if (useId) {
                    fileUri = String.format(BASE_GET_FOLDER_BY_ID_URL, siteUrl, itemId);
                } else {
                    fileUri = String.format(BASE_GET_FOLDER_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(path));
                }
                get.setURI(URI.create(fileUri));
                try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, get)) {
                    folderStatusCode = response.getStatusLine().getStatusCode();
                }
                if (folderStatusCode == HttpStatus.SC_OK) {
                    deleteUri = fileUri;
                }
            }
            if (fileStatusCode != HttpStatus.SC_OK && folderStatusCode != HttpStatus.SC_OK) {
                throw new FileNotFoundException("File not found");
            }
            ContextInfo contextInfo = getContextInfo(client, siteUrl, authPrincipal);
            String formDigest = contextInfo.getFormDigestValue();
            HttpDelete delete = new HttpDelete(deleteUri);
            setRequestHeader(delete, authPrincipal);
            delete.setHeader(IF_MATCH_HEADER, "*");
            delete.setHeader(X_REQUEST_DIGEST_HEADER, formDigest);
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, delete)) {
                IHTTPResponseHandler<Void> handler = new SharePointResponseHandler<>(Void.class);
                handler.handle(response);
            }
        }
    }

    private CreateFolderResult createFolder(CloseableHttpClient client, String folderName, String parentPath,
        Object authPrincipal,
        String spServer, boolean autoCreateParent) throws IOException, RepositoryException {
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        JsonObject data = new JsonObject();
        JsonObject nestedData = new JsonObject();
        if (!"/".equals(parentPath)) {
            if (useId && parentPath.startsWith("TempId")) {
                String pathDisplay = parentPath.substring(parentPath.indexOf(':') + 1);
                parentPath = getPathIdByPathDisplay(pathDisplay, siteUrl, authPrincipal);
            }
            parentPath = prependPathWithSiteRelativeUrl(parentPath);
            List<String> pathToken = StringUtils.tokenize(parentPath, "/");
            final String folderId = pathToken.get(pathToken.size() - 1);
            if (autoCreateParent) {
                createFolder(client, folderId, parentPath.substring(0, parentPath.indexOf(folderId)), authPrincipal, spServer, false);
            }
            final String metadataUri;
            if (useId) {
                metadataUri = String.format(BASE_GET_FOLDER_BY_ID_URL, siteUrl, folderId);
            } else {
                metadataUri = String.format(BASE_GET_FOLDER_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(parentPath));
            }
            HttpGet get = new HttpGet(metadataUri);
            setRequestHeader(get, authPrincipal);
            SharePointFileMetadata metadata = null;
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, get)) {
                if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                    IHTTPResponseHandler<SharePointFileMetadata> handler = new SharePointResponseHandler<>(SharePointFileMetadata.class);
                    metadata = handler.handle(response);
                } else {
                    IHTTPResponseHandler<SharePointFileMetadataVerbose> handler = new SharePointResponseHandler<>(SharePointFileMetadataVerbose.class);
                    metadata = handler.handle(response).getMetadata();
                }
            }
            nestedData.addProperty("type", DataType.FOLDER.getMetadata());
            data.addProperty("ServerRelativeUrl", metadata.getServerRelativeUrl() + '/' + folderName);
        } else {
            nestedData.addProperty("type", DataType.LIST.getMetadata());
            data.addProperty("AllowContentTypes", true);
            data.addProperty("BaseTemplate", 101);
            data.addProperty("ContentTypesEnabled", true);
            data.addProperty("Title", folderName);
            data.addProperty("Description", folderName);
        }
        data.add("__metadata", nestedData);

        String json = GsonUtils.GSON.toJson(data);
        HttpEntity entity = new StringEntity(json, "UTF-8");

        ContextInfo contextInfo = getContextInfo(client, spServer, authPrincipal);
        final String formDigest = contextInfo.getFormDigestValue();
        String uri = String.format("/".equals(parentPath) ? LIST_ROOT_SITE_URL : FOLDERS_URL, StringUtils.stripTrailing(spServer, "/"));
        HttpPost post = new HttpPost(uri);
        post.setEntity(entity);
        setRequestHeader(post, authPrincipal);
        post.setHeader(CONTENT_TYPE, APPLICATION_JSON_VERBOSE);
        post.setHeader(X_REQUEST_DIGEST_HEADER, formDigest);
        try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, post)) {
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_CREATED) {
                LOGGER.error("Error occurred while creating folder:" + folderName);
                throw new SPRestServiceException(code, "Error occurred while creating folder:" + folderName, null);
            }
        }
        CreateFolderResult result = new CreateFolderResult(true);
        result.setLastModified(new Date());
        result.setName(folderName);
        result.setPathDisplay(data.get("ServerRelativeUrl").getAsString() + "/");
        result.setPathId(data.get("ServerRelativeUrl").getAsString() + "/");
        return result;
    }

    public CreateFolderResult createFolder(String folderName, String parentPath, Object authPrincipal, String spServer,
        boolean autoCreateParent)
            throws IOException, RepositoryException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            setSharepointClientMetadata(client, spServer, authPrincipal);
            return createFolder(client, folderName, parentPath, authPrincipal, spServer, autoCreateParent);
        }
    }

    public String getPublicURL(String siteUrl, Object authPrincipal, String filePath) throws IOException,
            RepositoryException {
        if (filePath.startsWith("/")) {
            filePath = filePath.replaceFirst("/", "");
        }
        String uri = siteUrl + "_api/SP.Web.CreateAnonymousLink";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            setSharepointClientMetadata(client, siteUrl, authPrincipal);
            filePath = prependPathWithSiteRelativeUrl(filePath);
            HttpPost post = new HttpPost(uri);
            setRequestHeader(post, authPrincipal);
            post.setHeader(IF_MATCH_HEADER, "*");
            post.setHeader(CONTENT_TYPE, APPLICATION_JSON_VERBOSE);

            JsonObject data = new JsonObject();
            data.addProperty("url", siteUrl + filePath);
            data.addProperty("isEditLink", false);
            HttpEntity entity = new ByteArrayEntity(data.toString().getBytes("UTF-8"));
            post.setEntity(entity);
            try (CloseableHttpResponse response = getCloseableHttpResponse(client, siteUrl, post)) {
                IHTTPResponseHandler<CreateAnonymousLink> handler = new SharePointResponseHandler<>(CreateAnonymousLink.class);
                CreateAnonymousLink helper = handler.handle(response);
                String guestAccessLink = helper.getCreateAnonymousLinkResponse().getAnonymousLink();
                if (!StringUtils.hasText(guestAccessLink)) {
                    return guestAccessLink;
                }
                return guestAccessLink.replaceFirst("guestaccess.aspx", "download.aspx");
            }
        }
    }

    public UploadedFileMetaData uploadFile(String spServer, Object authPrincipal, String folderPath,
        String localFile,
        boolean overwrite,
        String conflictFileName,
        boolean autoCreateParent) throws IOException, RepositoryException {
        final String siteUrl = StringUtils.stripTrailing(spServer, "/");
        if (useId && folderPath.startsWith("TempId")) {
            String pathDisplay = folderPath.substring(folderPath.indexOf(':') + 1);
            folderPath = getPathIdByPathDisplay(pathDisplay, siteUrl, authPrincipal);
        }
        List<String> pathToken = StringUtils.tokenize(folderPath, "/");
        final String folderId = pathToken.get(pathToken.size() - 1);
        File file = new File(localFile);
        if (!file.exists()) {
            throw new FileNotFoundException("File is not present: " + file.getName());
        }
        String encodedFileName = UrlEscapers.urlFragmentEscaper().escape(file.getName());
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout((int)TimeUnit.SECONDS.toMillis(30)).build();
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            setSharepointClientMetadata(client, spServer, authPrincipal);
            if (autoCreateParent) {
                createFolder(client, folderId, folderPath.substring(0, folderPath.indexOf(folderId)), authPrincipal, spServer, false);
            }
            folderPath = prependPathWithSiteRelativeUrl(folderPath);
            ContextInfo contextInfo = getContextInfo(client, spServer, authPrincipal);
            String formDigest = contextInfo.getFormDigestValue();
            try {
                final String uploadUri;
                if (useId) {
                    uploadUri = String.format(UPLOAD_FILE_BY_ID_URL, siteUrl, folderId, encodedFileName, overwrite);
                } else {
                    uploadUri = String.format(UPLOAD_FILE_BY_PATH_URL, siteUrl, UrlEscapers.urlFragmentEscaper().escape(folderPath), encodedFileName, overwrite);
                }
                URIBuilder builder = new URIBuilder(uploadUri);
                builder.addParameter("$select", "Title,ServerRelativeUrl,Length,TimeCreated,TimeLastModified,UniqueId,Name");
                HttpPost post = new HttpPost(builder.build());
                setRequestHeader(post, authPrincipal);
                post.setHeader(X_REQUEST_DIGEST_HEADER, formDigest);
                post.setEntity(new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM));
                try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, post)) {
                    SharePointFileUploadResponse result;
                    if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                        IHTTPResponseHandler<SharePointFileUploadResponse> handler = new SharePointResponseHandler<>(SharePointFileUploadResponse.class);
                        result = handler.handle(response);
                    } else {
                        IHTTPResponseHandler<SharePointFileUploadResponseVerbose> handler = new SharePointResponseHandler<>(SharePointFileUploadResponseVerbose.class);
                        SharePointFileUploadResponseVerbose resultVerbose = handler.handle(response);
                        result = resultVerbose.getData();
                    }
                    String serverRelativeUrl = result.getServerRelativeUrl();
                    String uniqueId = result.getUniqueId();
                    UploadedFileMetaData metaData = new UploadedFileMetaData();
                    metaData.setFileNameWithTimeStamp(file.getName());
                    if (StringUtils.hasText(siteRelativeUrl)) {
                        serverRelativeUrl = serverRelativeUrl.replaceFirst(siteRelativeUrl, "");
                    }
                    metaData.setPathDisplay(serverRelativeUrl);
                    if (!useId) {
                        metaData.setPathId(serverRelativeUrl);
                    } else {
                        metaData.setPathId(folderPath + "/" + uniqueId);
                    }
                    metaData.setSize(file.length());
                    return metaData;
                }
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    private ContextInfo getContextInfo(CloseableHttpClient client, String spServer, Object authPrincipal)
            throws IOException {
        final String uri = StringUtils.stripTrailing(spServer, "/") + "/_api/contextinfo";
        HttpPost post = new HttpPost(uri);
        setRequestHeader(post, authPrincipal);
        try (CloseableHttpResponse response = getCloseableHttpResponse(client, spServer, post)) {
            if (responseType == RESPONSE_TYPE.JSON_BASIC) {
                IHTTPResponseHandler<ContextInfo> handler = new SharePointResponseHandler<>(ContextInfo.class);
                return handler.handle(response);
            } else {
                IHTTPResponseHandler<ContextInfoVerbose> handler = new SharePointResponseHandler<>(ContextInfoVerbose.class);
                ContextInfoVerbose contextVerbose = handler.handle(response);
                return contextVerbose.getData().getContextInfo();
            }

        }
    }

    public byte[] getPartialItem(String fileId, long startByte, long endByte, String spServer, Object authPrincipal)
            throws IOException, RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }
        try (InputStream is = downloadAsStream(fileId, authPrincipal, spServer)) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream((int)(endByte - startByte + 1))) {
                IOUtils.copy(is, os, startByte, endByte - startByte + 1);
                return os.toByteArray();
            }
        }
    }

    private CloseableHttpClient getExtendedTimeoutHttpClient() {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(RepoConstants.CONNECTION_TIMEOUT).setConnectionRequestTimeout(RepoConstants.CONNECTION_TIMEOUT).setSocketTimeout(RepoConstants.READ_TIMEOUT).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public enum AUTH_TYPE {
        OAUTH,
        NTLM
    }

    public enum RESPONSE_TYPE {
        JSON_BASIC,
        JSON_VERBOSE
    }

    public enum SHAREPOINT_ONPREM_VERSION {
        SP_2013,
        SP_2016
    }
}
