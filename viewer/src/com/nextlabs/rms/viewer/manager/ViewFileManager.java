package com.nextlabs.rms.viewer.manager;

import com.google.gson.Gson;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.FileConversionHandler;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.NotAuthorizedException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.json.RepoFile;
import com.nextlabs.rms.viewer.json.SharedFile;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ViewFileManager {

    private static final ViewFileManager INSTANCE = new ViewFileManager();
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);
    private static final Gson GSON = new Gson();
    private static final String FILE_DOWNLOAD_URL = "/RMSViewer/DownloadFileForViewer";
    private static final String FILE_SHARED_WITH_DOWNLOAD = "/rs/sharedWithMe/download";
    public static final String ROUTER_URL;
    public static final String RMS_INTERNAL_URL;
    public static final String ROUTER_INTERNAL_URL;

    static {
        String routerURL = ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.ROUTER_URL);
        if (!StringUtils.hasText(routerURL)) {
            throw new ExceptionInInitializerError("Router URL is not configured");
        }
        ROUTER_URL = routerURL;

        String rmsInternalURL = ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.RMS_INTERNAL_URL);
        if (StringUtils.hasText(rmsInternalURL)) {
            RMS_INTERNAL_URL = rmsInternalURL;
        } else {
            RMS_INTERNAL_URL = null;
        }

        String routerInternalURL = ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.ROUTER_INTERNAL_URL);
        if (StringUtils.hasText(routerInternalURL)) {
            ROUTER_INTERNAL_URL = routerInternalURL;
        } else {
            ROUTER_INTERNAL_URL = null;
        }
    }

    private ViewFileManager() {
    }

    public static ViewFileManager getInstance() {
        return INSTANCE;
    }

    public ShowFileResult showSharedWorkspaceFile(RepoFile repoFile, User user, int offset, String lastModified,
        String viewingSessionId,
        HttpServletRequest request, HttpServletResponse response) throws NxlException, GeneralSecurityException,
            IOException, RMSException, UnsupportedFormatException, NotAuthorizedException {

        String userId = user.getId();
        String ticket = user.getTicket();
        String userName = user.getEmail();
        String tenantName = user.getTenantName();
        Integer platformId = user.getPlatformId();
        String clientId = user.getClientId();
        String deviceId = user.getDeviceId();
        long lastModifiedDate = 0L;

        File tempDir = new File(ViewerConfigManager.getInstance().getTempDir(), viewingSessionId);
        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, System.currentTimeMillis() + UUID.randomUUID().toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache Id: {}", fileCacheId);
        }
        DownloadResponse downloadResponse = null;
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        final String rmsURL = ViewerUtil.getRMSInternalURL(user.getTenantName());
        try {
            downloadResponse = downloadSharedWorkSpaceFiles(repoFile, tempDir, user);
            try {
                lastModifiedDate = Long.parseLong(lastModified);
            } catch (NumberFormatException ne) {
                LOGGER.warn("Invalid lastModifiedDate", ne);
            }

            EvaluationHandler evalHandler = new EvaluationHandler();
            CachedFile cachedFile = evalHandler.evaluateAndDecrypt(downloadResponse.getFile(), user, offset);
            repoFile.setFromMyDrive(downloadResponse.isFromMyDrive());
            cachedFile.setRepoFile(repoFile);
            cachedFile.setLastModifiedDate(lastModifiedDate);

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);

            String duid = cachedFile.getDuid();
            String owner = cachedFile.getMembership();
            if (StringUtils.hasText(duid) && StringUtils.hasText(owner)) {
                RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, duid, owner, Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, repoFile.getRepoId(), repoFile.getFileId(), repoFile.getFilePathDisplay(), AccessResult.ALLOW, request, prop, null, AccountType.PERSONAL);
            }

            FileConversionHandler conversionHandler = new FileConversionHandler();
            String url = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), downloadResponse.getFile());
            return new ShowFileResult(url, cachedFile.getRights(), cachedFile.isOwner(), duid, owner);
        } catch (NotAuthorizedException ne) {
            LOGGER.error(ne.getMessage(), ne);
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, ne.getDuid(), ne.getOwner(), Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, repoFile.getRepoId(), repoFile.getFileId(), repoFile.getFilePathDisplay(), AccessResult.DENY, request, prop, null, AccountType.PERSONAL);
            throw ne;
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    public ShowFileResult showFile(RepoFile repoFile, User user, int offset, String lastModified,
        String viewingSessionId,
        HttpServletRequest request, HttpServletResponse response) throws NxlException, GeneralSecurityException,
            IOException, RMSException, UnsupportedFormatException, NotAuthorizedException {

        String userId = user.getId();
        String ticket = user.getTicket();
        String userName = user.getEmail();
        String tenantName = user.getTenantName();
        Integer platformId = user.getPlatformId();
        String clientId = user.getClientId();
        String deviceId = user.getDeviceId();
        long lastModifiedDate = 0L;

        File tempDir = new File(ViewerConfigManager.getInstance().getTempDir(), viewingSessionId);
        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, System.currentTimeMillis() + UUID.randomUUID().toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache Id: {}", fileCacheId);
        }
        DownloadResponse downloadResponse = null;
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        final String rmsURL = ViewerUtil.getRMSInternalURL(user.getTenantName());
        try {
            downloadResponse = downloadFilesThroughRMS(repoFile, null, tempDir, user);
            try {
                lastModifiedDate = Long.parseLong(lastModified);
            } catch (NumberFormatException ne) {
                LOGGER.warn("Invalid lastModifiedDate", ne);
            }

            EvaluationHandler evalHandler = new EvaluationHandler();
            CachedFile cachedFile = evalHandler.evaluateAndDecrypt(downloadResponse.getFile(), user, offset);
            repoFile.setFromMyDrive(downloadResponse.isFromMyDrive());
            cachedFile.setRepoFile(repoFile);
            cachedFile.setLastModifiedDate(lastModifiedDate);

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);

            String duid = cachedFile.getDuid();
            String owner = cachedFile.getMembership();
            if (StringUtils.hasText(duid) && StringUtils.hasText(owner)) {
                RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, duid, owner, Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, repoFile.getRepoId(), repoFile.getFileId(), repoFile.getFilePathDisplay(), AccessResult.ALLOW, request, prop, null, AccountType.PERSONAL);
            }

            FileConversionHandler conversionHandler = new FileConversionHandler();
            String url = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), downloadResponse.getFile());
            return new ShowFileResult(url, cachedFile.getRights(), cachedFile.isOwner(), duid, owner);
        } catch (NotAuthorizedException ne) {
            LOGGER.error(ne.getMessage(), ne);
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, ne.getDuid(), ne.getOwner(), Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, repoFile.getRepoId(), repoFile.getFileId(), repoFile.getFilePathDisplay(), AccessResult.DENY, request, prop, null, AccountType.PERSONAL);
            throw ne;
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    public ShowFileResult showSharedFile(SharedFile sharedFile, User user, int offset, String viewingSessionId,
        HttpServletRequest request, HttpServletResponse response, String spaceId, String fromSpace)
            throws NxlException, GeneralSecurityException,
            IOException, RMSException, UnsupportedFormatException, NotAuthorizedException {

        String userId = user.getId();
        String ticket = user.getTicket();
        String userName = user.getEmail();
        String tenantName = user.getTenantName();
        String deviceId = user.getDeviceId();
        String clientId = user.getClientId();
        int platformId = user.getPlatformId();
        long lastModifiedDate = 0L;

        File tempDir = new File(ViewerConfigManager.getInstance().getTempDir(), viewingSessionId);
        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, System.currentTimeMillis() + UUID.randomUUID().toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache Id: {}", fileCacheId);
        }
        DownloadResponse downloadResponse = null;
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        final String rmsURL = ViewerUtil.getRMSInternalURL(user.getTenantName());
        try {
            downloadResponse = downloadSharedFilesThroughRMS(sharedFile, rmsURL, tempDir, user, request, spaceId);
            try {
                lastModifiedDate = Long.parseLong(downloadResponse.getLastModified());
            } catch (NumberFormatException ne) {
                LOGGER.warn("Invalid lastModifiedDate", ne);
            }

            SHARESPACE fromShareSpace = (!StringUtils.hasText(fromSpace)) ? null : SHARESPACE.valueOf(fromSpace);
            EvaluationHandler evalHandler = new EvaluationHandler();

            CachedFile cachedFile = null;
            if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
                cachedFile = evalHandler.evaluateAndDecryptSharedProjectFile(downloadResponse.getFile(), user, offset, fromShareSpace, spaceId, sharedFile);
            } else {
                cachedFile = evalHandler.evaluateAndDecrypt(downloadResponse.getFile(), user, offset);
            }
            cachedFile.setSharedFile(sharedFile);
            cachedFile.setLastModifiedDate(lastModifiedDate);
            if (StringUtils.hasText(fromSpace) && "PROJECTSPACE".equalsIgnoreCase(fromSpace)) {
                cachedFile.setProjectId(spaceId);
            }

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);

            String duid = cachedFile.getDuid();
            String owner = cachedFile.getMembership();

            FileConversionHandler conversionHandler = new FileConversionHandler();
            String url = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), downloadResponse.getFile());
            return new ShowFileResult(url, cachedFile.getRights(), cachedFile.isOwner(), duid, owner);
        } catch (NotAuthorizedException ne) {
            LOGGER.debug(ne.getMessage(), ne);
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, ne.getDuid(), ne.getOwner(), Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, " ", " ", " ", AccessResult.DENY, request, prop, null, AccountType.PERSONAL);
            throw ne;
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    public ShowFileResult showProjectFile(String projectId, String filePath, String filePathDisplay, User user,
        int offset, String lastModifiedDateStr, String viewingSessionId, HttpServletRequest request,
        HttpServletResponse response) throws NxlException, GeneralSecurityException, IOException, RMSException,
            UnsupportedFormatException, NotAuthorizedException {
        long lastModifiedDate = 0;
        try {
            lastModifiedDate = Long.parseLong(lastModifiedDateStr);
        } catch (NumberFormatException ne) {
            LOGGER.warn("Invalid lastModifiedDate", ne);
        }

        String userId = user.getId();
        String ticket = user.getTicket();
        String userName = user.getEmail();
        String tenantName = user.getTenantName();
        String deviceId = user.getDeviceId();
        String clientId = user.getClientId();
        int platformId = user.getPlatformId();

        String tempDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + viewingSessionId;

        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, System.currentTimeMillis() + UUID.randomUUID().toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache Id: {}", fileCacheId);
        }
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        final String rmsURL = ViewerUtil.getRMSInternalURL(tenantName);
        File file = null;
        try {
            file = downloadProjectFile(projectId, filePath, tempDir, user, request);

            EvaluationHandler evalHandler = new EvaluationHandler();
            RepoFile repoFileDetails = new RepoFile(null, null, null, filePath, filePathDisplay);
            CachedFile cachedFile = evalHandler.evaluateAndDecrypt(file, user, offset);
            cachedFile.setRepoFile(repoFileDetails);
            cachedFile.setLastModifiedDate(lastModifiedDate);
            cachedFile.setProjectId(projectId);

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);

            FileConversionHandler conversionHandler = new FileConversionHandler();
            String redirectURL = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), file);

            String duid = cachedFile.getDuid();
            String owner = cachedFile.getMembership();

            return new ShowFileResult(redirectURL, cachedFile.getRights(), cachedFile.isOwner(), duid, owner);
        } catch (NotAuthorizedException ne) {
            LOGGER.error(ne.getMessage(), ne);
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, ne.getDuid(), ne.getOwner(), Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, projectId, filePath, filePathDisplay, AccessResult.DENY, request, prop, null, AccountType.PROJECT);
            throw ne;
        } finally {
            File temp = new File(tempDir);
            if (temp.exists()) {
                try {
                    FileUtils.deleteDirectory(temp);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete temp directory.", e);
                }
            }
        }
    }

    public ShowFileResult showWorkspaceFile(String filePath, String filePathDisplay, User user,
        int offset, String lastModifiedDateStr, String viewingSessionId, HttpServletRequest request,
        HttpServletResponse response) throws NxlException, GeneralSecurityException, IOException, RMSException,
            UnsupportedFormatException, NotAuthorizedException {
        long lastModifiedDate = 0;
        try {
            lastModifiedDate = Long.parseLong(lastModifiedDateStr);
        } catch (NumberFormatException ne) {
            LOGGER.warn("Invalid lastModifiedDate", ne);
        }

        String userId = user.getId();
        String ticket = user.getTicket();
        String userName = user.getEmail();
        String tenantName = user.getTenantName();
        String deviceId = user.getDeviceId();
        String clientId = user.getClientId();
        int platformId = user.getPlatformId();

        String tempDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + viewingSessionId;

        FileCacheId fileCacheId = new FileCacheId(viewingSessionId, System.currentTimeMillis() + UUID.randomUUID().toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache Id: {}", fileCacheId);
        }
        Properties prop = new Properties();
        prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
        final String rmsURL = ViewerUtil.getRMSInternalURL(tenantName);
        File file = null;
        try {
            file = downloadWorkspaceFile(filePath, tempDir, user);

            EvaluationHandler evalHandler = new EvaluationHandler();
            RepoFile repoFileDetails = new RepoFile(null, null, null, filePath, filePathDisplay);
            CachedFile cachedFile = evalHandler.evaluateAndDecrypt(file, user, offset);
            cachedFile.setRepoFile(repoFileDetails);
            cachedFile.setLastModifiedDate(lastModifiedDate);
            cachedFile.setBucketName(CachedFile.BucketName.WORKSPACE);

            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);

            FileConversionHandler conversionHandler = new FileConversionHandler();
            String redirectURL = conversionHandler.convertAndGetURL(userName, offset, tenantName, fileCacheId, cachedFile.getFileName(), file);

            String duid = cachedFile.getDuid();
            String owner = cachedFile.getMembership();
            return new ShowFileResult(redirectURL, cachedFile.getRights(), cachedFile.isOwner(), duid, owner);
        } catch (NotAuthorizedException ne) {
            LOGGER.error(ne.getMessage(), ne);
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, ne.getDuid(), ne.getOwner(), Integer.parseInt(userId), clientId, deviceId, platformId, Operations.VIEW, user.getTenantId(), filePath, filePathDisplay, AccessResult.DENY, request, prop, null, AccountType.ENTERPRISEWS);
            throw ne;
        } finally {
            File temp = new File(tempDir);
            if (temp.exists()) {
                try {
                    FileUtils.deleteDirectory(temp);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete temp directory.", e);
                }
            }
        }
    }

    private File downloadWorkspaceFile(String filePath, String tempDir, User user)
            throws IOException,
            GeneralSecurityException, RMSException {
        String tenantName = user.getTenantName();
        String userId = user.getId();
        String clientId = user.getClientId();
        String ticket = user.getTicket();
        CloseableHttpResponse postResponse = null;
        CloseableHttpClient client = HTTPUtil.getHTTPClient();
        try {
            String rmsURL = ViewerUtil.getRMSInternalURL(tenantName);
            StringBuilder urlBuilder = new StringBuilder(rmsURL);
            urlBuilder.append("/rs/enterprisews/v2/download");
            URL url = new URL(urlBuilder.toString());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("URL to download file: {}", url);
            }
            HttpPost postRequest = new HttpPost(url.toString());
            postRequest.addHeader("userId", userId);
            postRequest.addHeader("ticket", ticket);
            postRequest.addHeader("clientId", clientId);
            JsonRequest requestBody = new JsonRequest();
            requestBody.addParameter("pathId", filePath);
            requestBody.addParameter("type", DownloadType.FOR_VIEWER.ordinal());
            String requestJson = requestBody.toJson();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Request body: {}", requestJson);
            }
            postRequest.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
            postResponse = client.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Response Code: {}", statusCode);
            }
            if (statusCode != 200) {
                LOGGER.error("Error occurred while downloading file: '{}' in enterprise workspace ", filePath);
                if (statusCode == 404) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.workspace.file.deleted"));
                } else if (statusCode == 403) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.unauthorised.view"));
                } else {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            HttpEntity entity = postResponse.getEntity();
            if (entity == null) {
                LOGGER.error("No response entity while downloading file: '{}' in enterprise workspace", filePath);
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }

            String fileName = null;
            Header contentDispositionHeader = postResponse.getFirstHeader("Content-Disposition");
            String disposition = contentDispositionHeader.getValue();
            int index = disposition.indexOf("filename*=UTF-8''");
            fileName = disposition;
            if (index >= 0) {
                fileName = URLDecoder.decode(disposition.substring(index + 17), "UTF-8");
            }

            File tempPath = new File(tempDir);
            if (!tempPath.exists()) {
                boolean success = tempPath.mkdirs();
                if (!success) {
                    LOGGER.error("Unable to create temporary folder: {}", tempDir);
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            File file = new File(tempDir, fileName);
            try (InputStream is = entity.getContent();
                    OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
            }
            if (!file.exists()) {
                LOGGER.error("File does not exist");
                throw new FileNotFoundException("File does not exist");
            }
            return file;
        } finally {
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    private File downloadProjectFile(String projectId, String filePath, String tempDir, User user,
        HttpServletRequest request)
            throws IOException,
            GeneralSecurityException, RMSException {
        String tenantName = user.getTenantName();
        String userId = user.getId();
        String clientId = user.getClientId();
        String ticket = user.getTicket();
        String deviceId = user.getDeviceId();
        // To save VIEW activity log with the correct device id, as the originating IP address of a client, it is added to the request header ‘deviceId’ in viewer. 
        // Since the Project download API call from client is through the viewer, the client IP address cannot be obtained in Project download API
        deviceId = StringUtils.hasText(deviceId) ? deviceId : HTTPUtil.getRemoteAddress(request);
        CloseableHttpResponse postResponse = null;
        CloseableHttpClient client = HTTPUtil.getHTTPClient();
        try {
            String rmsURL = ViewerUtil.getRMSInternalURL(tenantName);
            StringBuilder urlBuilder = new StringBuilder(rmsURL);
            urlBuilder.append("/rs/project/").append(projectId.trim()).append("/v2/download");
            URL url = new URL(urlBuilder.toString());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("URL to download file: {}", url);
            }
            HttpPost postRequest = new HttpPost(url.toString());
            postRequest.addHeader("userId", userId);
            postRequest.addHeader("ticket", ticket);
            postRequest.addHeader("clientId", clientId);
            postRequest.addHeader("deviceId", deviceId);
            JsonRequest requestBody = new JsonRequest();
            requestBody.addParameter("pathId", filePath);
            requestBody.addParameter("type", DownloadType.FOR_VIEWER.ordinal());
            String requestJson = requestBody.toJson();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Request body: {}", requestJson);
            }
            postRequest.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
            postResponse = client.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Response Code: {}", statusCode);
            }
            if (statusCode != 200) {
                LOGGER.error("Error occurred while downloading file: '{}' in project: {}", filePath, projectId);
                if (statusCode == 404) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.project.file.deleted"));
                } else if (statusCode == 403) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.unauthorised.view"));
                } else {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            HttpEntity entity = postResponse.getEntity();
            if (entity == null) {
                LOGGER.error("No response entity while downloading file: '{}' in project: {}", filePath, projectId);
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }

            String fileName = null;
            Header contentDispositionHeader = postResponse.getFirstHeader("Content-Disposition");
            String disposition = contentDispositionHeader.getValue();
            int index = disposition.indexOf("filename*=UTF-8''");
            fileName = disposition;
            if (index >= 0) {
                fileName = URLDecoder.decode(disposition.substring(index + 17), "UTF-8");
            }

            File tempPath = new File(tempDir);
            if (!tempPath.exists()) {
                boolean success = tempPath.mkdirs();
                if (!success) {
                    LOGGER.error("Unable to create temporary folder: {}", tempDir);
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            File file = new File(tempDir, fileName);
            try (InputStream is = entity.getContent();
                    OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
            }
            if (!file.exists()) {
                LOGGER.error("File does not exist");
                throw new FileNotFoundException("File does not exist");
            }
            return file;
        } finally {
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    public DownloadResponse downloadSharedFilesThroughRMS(SharedFile sharedFile, String rmsURL,
        File tempDir, User user, HttpServletRequest request, String spaceId)
            throws IOException, GeneralSecurityException, RMSException, NotAuthorizedException {
        DownloadResponse result = new DownloadResponse();
        try (CloseableHttpClient client = HTTPUtil.getHTTPClient()) {
            URL url = new URL(rmsURL + FILE_SHARED_WITH_DOWNLOAD);
            HttpPost postRequest = new HttpPost(url.toString());
            JsonRequest jsonRequest = new JsonRequest();
            jsonRequest.addParameter("transactionId", sharedFile.getTransactionId());
            jsonRequest.addParameter("transactionCode", sharedFile.getTransactionCode());
            jsonRequest.addParameter("forViewer", "true"); // this is needed in order pass this info to rest api about ignoring the check for download rights
            jsonRequest.addParameter("spaceId", spaceId);
            postRequest.setEntity(new StringEntity(jsonRequest.toJson(), "UTF-8"));
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                postRequest.setHeader(headerName, request.getHeader(headerName));
            }
            postRequest.removeHeaders("Content-Length");
            postRequest.setHeader("userId", user.getId() + "");
            postRequest.setHeader("ticket", user.getTicket());
            postRequest.setHeader("clientId", user.getClientId());
            Integer platformId = user.getPlatformId();
            if (platformId != null) {
                postRequest.setHeader("platformId", String.valueOf(platformId));
            }
            postRequest.setHeader("Content-Type", "application/json");
            postRequest.setHeader(HTTPUtil.HEADER_X_FORWARDED_FOR, HTTPUtil.getRemoteAddress(request));
            String duid = null;
            String membership = null;
            try (CloseableHttpResponse postResponse = client.execute(postRequest)) {
                int statusCode = postResponse.getStatusLine().getStatusCode();
                duid = postResponse.getFirstHeader("x-rms-file-duid").getValue();
                membership = postResponse.getFirstHeader("x-rms-file-membership").getValue();
                if (statusCode == 200) {
                    try (InputStream is = postResponse.getEntity().getContent()) {
                        if (is != null) {
                            Header contentDispositionHeader = postResponse.getFirstHeader("Content-Disposition");
                            String disposition = contentDispositionHeader.getValue();
                            int index = disposition.indexOf("filename*=UTF-8''");
                            String fileName = disposition;
                            if (index >= 0) {
                                fileName = URLDecoder.decode(disposition.substring(index + 17), "UTF-8");
                            }
                            String lastModified = postResponse.getFirstHeader("x-rms-last-modified").getValue();
                            if (!tempDir.exists()) {
                                boolean success = tempDir.mkdirs();
                                if (!success) {
                                    LOGGER.error("Unable to create temporary folder: {}", tempDir);
                                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                                }
                            }
                            File file = new File(tempDir, fileName);
                            try (OutputStream os = new FileOutputStream(file)) {
                                IOUtils.copy(is, os);
                            }

                            if (!file.exists()) {
                                LOGGER.error("File does not exist");
                                throw new FileNotFoundException("File does not exist");
                            }
                            result.setFile(file);
                            result.setRepoOwner(false);
                            result.setLastModified(lastModified);
                            return result;
                        } else {
                            throw new RMSException(ViewerMessageHandler.getClientString("err.shared.with.me.file.download"));
                        }
                    }
                } else if (statusCode == 403) {
                    try (InputStream is = postResponse.getEntity().getContent()) {
                        JsonResponse response = GsonUtils.GSON.fromJson(IOUtils.toString(is, StandardCharsets.UTF_8), JsonResponse.class);
                        if (response.getStatusCode() == 4001) {
                            throw new NotAuthorizedException(ViewerMessageHandler.getClientString("err.unauthorized.view"), duid, membership);
                        } else {
                            throw new RMSException(ViewerMessageHandler.getClientString("err.shared.with.me.unauthorized.view"));
                        }
                    }
                } else {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.shared.with.me.file.download"));
                }
            }
        }
    }

    public DownloadResponse downloadFilesThroughRMS(RepoFile repoFileDetails, List<String> filePathsList,
        File tempDir, User user)
            throws IOException, GeneralSecurityException, RMSException {
        CloseableHttpResponse postResponse = null;
        CloseableHttpClient client = HTTPUtil.getHTTPClient();
        try {
            String rmsURL = ViewerUtil.getRMSInternalURL(user.getTenantName());
            URL url = new URL(rmsURL + FILE_DOWNLOAD_URL);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("URL to download file: {}", url);
            }
            HttpPost postRequest = new HttpPost(url.toString());

            List<BasicNameValuePair> requestParameters = new ArrayList<>();
            requestParameters.add(new BasicNameValuePair("repoId", repoFileDetails.getRepoId()));
            requestParameters.add(new BasicNameValuePair("filePath", repoFileDetails.getFileId()));
            requestParameters.add(new BasicNameValuePair("filePathDisplay", repoFileDetails.getFilePathDisplay()));
            requestParameters.add(new BasicNameValuePair("userId", user.getId()));
            requestParameters.add(new BasicNameValuePair("ticket", user.getTicket()));
            requestParameters.add(new BasicNameValuePair("clientId", user.getClientId()));

            if (filePathsList != null && !filePathsList.isEmpty()) {
                requestParameters.add(new BasicNameValuePair("multiRequest", "true"));
                String filePathsJson = GSON.toJson(filePathsList);
                requestParameters.add(new BasicNameValuePair("filePaths", filePathsJson));
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Request params: {}", requestParameters);
            }
            postRequest.setEntity(new UrlEncodedFormEntity(requestParameters, "utf-8"));

            postResponse = client.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Response Code: {}", statusCode);
            }
            if (statusCode != 200) {
                if (statusCode == 404) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.not.found"));
                }
                LOGGER.error("Error occurred while downloading file");
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }
            HttpEntity entity = postResponse.getEntity();
            if (entity == null) {
                LOGGER.error("No response entity while downloading file");
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }
            Header contentDispositionHeader = postResponse.getFirstHeader("Content-Disposition");
            String disposition = contentDispositionHeader.getValue();
            int index = disposition.indexOf("filename*=UTF-8''");
            String fileName = disposition;
            if (index >= 0) {
                fileName = URLDecoder.decode(disposition.substring(index + 17), "UTF-8");
            }
            if (!tempDir.exists()) {
                boolean success = tempDir.mkdirs();
                if (!success) {
                    LOGGER.error("Unable to create temporary folder: {}", tempDir);
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            File file = new File(tempDir, fileName);
            try (InputStream is = entity.getContent();
                    OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
            }
            if (!file.exists()) {
                LOGGER.error("File does not exist");
                throw new FileNotFoundException("File does not exist");
            }
            Header repoOwnerHeader = postResponse.getFirstHeader("isRepoOwner");
            boolean isRepoOwner = false;
            if (repoOwnerHeader != null) {
                isRepoOwner = Boolean.valueOf(repoOwnerHeader.getValue());
            }
            Header fromMyDriveHeader = postResponse.getFirstHeader("fromMyDrive");
            boolean fromMyDrive = fromMyDriveHeader != null && Boolean.valueOf(fromMyDriveHeader.getValue());
            DownloadResponse result = new DownloadResponse();
            result.setFile(file);
            result.setRepoOwner(isRepoOwner);
            result.setFromMyDrive(fromMyDrive);
            return result;
        } finally {
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    private DownloadResponse downloadSharedWorkSpaceFiles(RepoFile repoFileDetails,
        File tempDir, User user)
            throws IOException, GeneralSecurityException, RMSException {
        CloseableHttpResponse postResponse = null;
        CloseableHttpClient client = HTTPUtil.getHTTPClient();
        try {
            String rmsURL = ViewerUtil.getRMSInternalURL(user.getTenantName());
            URL url = new URL(rmsURL + "/rs/sharedws/v1/" + repoFileDetails.getRepoId() + "/download");
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("URL to download file: {}", url);
            }

            HttpPost postRequest = new HttpPost(url.toString());
            postRequest.addHeader("userId", user.getId());
            postRequest.addHeader("ticket", user.getTicket());
            postRequest.addHeader("clientId", user.getClientId());
            JsonRequest requestBody = new JsonRequest();
            requestBody.addParameter("path", repoFileDetails.getFilePathDisplay());
            String requestJson = requestBody.toJson();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Request body: {}", requestJson);
            }
            postRequest.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));

            postResponse = client.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Response Code: {}", statusCode);
            }
            if (statusCode != 200) {
                LOGGER.error("Error occurred while downloading file");
                if (statusCode == 404) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.not.found"));
                } else if (statusCode == 403) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.unauthorised.view"));
                }
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }
            HttpEntity entity = postResponse.getEntity();
            if (entity == null) {
                LOGGER.error("No response entity while downloading file");
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
            }
            Header contentDispositionHeader = postResponse.getFirstHeader("Content-Disposition");
            String disposition = contentDispositionHeader.getValue();
            int index = disposition.indexOf("filename*=UTF-8''");
            String fileName = disposition;
            if (index >= 0) {
                fileName = URLDecoder.decode(disposition.substring(index + 17), "UTF-8");
            }
            if (!tempDir.exists()) {
                boolean success = tempDir.mkdirs();
                if (!success) {
                    LOGGER.error("Unable to create temporary folder: {}", tempDir);
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.download"));
                }
            }
            File file = new File(tempDir, fileName);
            try (InputStream is = entity.getContent();
                    OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
            }
            if (!file.exists()) {
                LOGGER.error("File does not exist");
                throw new FileNotFoundException("File does not exist");
            }
            DownloadResponse result = new DownloadResponse();
            result.setFile(file);
            result.setRepoOwner(false);
            result.setFromMyDrive(false);
            return result;
        } finally {
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    static class DownloadResponse {

        private File file;
        private boolean repoOwner;
        private boolean fromMyDrive;
        private String lastModified;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public boolean isRepoOwner() {
            return repoOwner;
        }

        public void setRepoOwner(boolean repoOwner) {
            this.repoOwner = repoOwner;
        }

        public boolean isFromMyDrive() {
            return fromMyDrive;
        }

        public void setFromMyDrive(boolean fromMyDrive) {
            this.fromMyDrive = fromMyDrive;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

    }

    public static class ShowFileResult {

        private final String viewerURL;
        private final int rights;
        private final boolean owner;
        private final String duid;
        private final String membership;

        ShowFileResult(String aViewerURL, int aRights, boolean st, String duid, String membership) {
            viewerURL = aViewerURL;
            rights = aRights;
            owner = st;
            this.duid = duid;
            this.membership = membership;
        }

        public String getViewerURL() {
            return viewerURL;
        }

        public int getRights() {
            return rights;
        }

        public boolean isOwner() {
            return owner;
        }

        public String getDuid() {
            return duid;
        }

        public String getMembership() {
            return membership;
        }
    }
}
