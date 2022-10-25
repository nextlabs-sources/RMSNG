package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.client.Client;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.json.PrintFileUrl;
import com.nextlabs.rms.viewer.json.RepoFile;
import com.nextlabs.rms.viewer.json.SharedFile;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.CachedFile.BucketName;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.visualization.GenericVisManager;
import com.nextlabs.rms.viewer.visualization.IVisManager;
import com.nextlabs.rms.viewer.visualization.PDFVisManager;
import com.nextlabs.rms.viewer.visualization.VisManagerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Ehcache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

public final class RMSViewerContentManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);
    private static final String MY_VAULT_FOLDER_PATH_ID = "/nxl_myvault_nxl/";
    private static final RMSViewerContentManager INSTANCE = new RMSViewerContentManager();
    private static final List<String> IMAGE_MIME_TYPES = Collections.unmodifiableList(Arrays.asList("image/png", "image/jpeg"));

    private RMSViewerContentManager() {
    }

    public static RMSViewerContentManager getInstance() {
        return INSTANCE;
    }

    public DocumentMetaData getMetaData(String documentId, String sessionId, String userId) {
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("File cache ID: {}", fileCacheId);
        }
        CachedFile cachedFile = null;
        DocumentMetaData metaData;
        try {
            cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
            if (!StringUtils.equalsIgnoreCase(cachedFile.getViewer().getId(), userId)) {
                metaData = new DocumentMetaData();
                metaData.setErrMsg(ViewerMessageHandler.getClientString("err.unauthorized.view"));
                return metaData;
            }
            metaData = new DocumentMetaData();
            int numPages = -1;
            IVisManager visManager = VisManagerFactory.getInstance().getVisManager(cachedFile.getFileName());
            if (visManager instanceof GenericVisManager || visManager instanceof PDFVisManager && CachedFile.ContentType._2D.equals(cachedFile.getContentType())) {
                numPages = ImageProcessor.getInstance().getNumPages(cachedFile.getFileContent(), cachedFile.getFileName());
                if (numPages <= 0) {
                    metaData.setErrMsg(ViewerMessageHandler.getClientString("err.file.processing"));
                }
            }
            int desiredRights = cachedFile.getRights();
            Rights[] rightsList = Rights.fromInt(desiredRights);
            metaData.setRights(Rights.toStrings(rightsList));

            metaData.setOriginalFileName(cachedFile.getOriginalFileName());
            metaData.setDisplayName(cachedFile.getFileName());
            metaData.setNumPages(numPages);
            metaData.setEfsId(cachedFile.getEfsId());
            metaData.setWatermark(cachedFile.getWaterMark());
            metaData.setTagsMap(cachedFile.getTagMap());
            metaData.setLastModifiedDate(cachedFile.getLastModifiedDate());
            metaData.setFileSize(cachedFile.getFileSize());

            String duid = cachedFile.getDuid();
            String membership = cachedFile.getMembership();
            metaData.setDuid(duid);
            metaData.setOwnerId(membership);
            metaData.setNXL(StringUtils.hasText(duid) && StringUtils.hasText(membership));
            metaData.setProtectionType(cachedFile.getProtectionType());

            boolean isWorkspaceFile = BucketName.WORKSPACE.equals(cachedFile.getBucketName());
            metaData.setWorkspaceFile(isWorkspaceFile);

            boolean isProjectFile = cachedFile.getProjectId() != null;
            metaData.setProjectFile(isProjectFile);
            metaData.setProjectId(cachedFile.getProjectId());

            metaData.setOwner(!isProjectFile && !isWorkspaceFile && cachedFile.isOwner());
            SharedFile sharedFileDetails = cachedFile.getSharedFile();
            if (sharedFileDetails != null) {
                metaData.setTransactionId(sharedFileDetails.getTransactionId());
                metaData.setTransactionCode(sharedFileDetails.getTransactionCode());
            }
            RepoFile repoFileDetails = cachedFile.getRepoFile();
            if (repoFileDetails != null) {
                metaData.setRepoName(repoFileDetails.getRepoName());
                metaData.setRepoId(repoFileDetails.getRepoId());
                metaData.setRepoType(repoFileDetails.getRepoType());
                metaData.setFilePathDisplay(repoFileDetails.getFilePathDisplay());
                String filePath = repoFileDetails.getFileId();
                metaData.setFilePath(filePath);
                metaData.setShowPathInfo(filePath != null && !filePath.startsWith(MY_VAULT_FOLDER_PATH_ID) && cachedFile.isOwner());
                metaData.setRepoReadOnly(!isProjectFile && !isWorkspaceFile && !repoFileDetails.isFromMyDrive());
            }
            String rmsURL = Client.getRMSURL(cachedFile.getViewer().getTenantName());
            metaData.setRmsURL(rmsURL);
            metaData.setSingleImageFile(isSingleImageFile(cachedFile));
            metaData.setValidity(cachedFile.getValidity());

        } catch (RMSException e) {
            metaData = new DocumentMetaData();
            metaData.setErrMsg(e.getMessage());
        } catch (UnsupportedFormatException e) {
            metaData = new DocumentMetaData();
            metaData.setErrMsg(ViewerMessageHandler.getClientString("err.unsupported"));
        } catch (IOException e) {
            LOGGER.error("Unable to get metadata (Document ID: " + documentId + "): " + e.getMessage(), e);
            metaData = new DocumentMetaData();
            metaData.setErrMsg(ViewerMessageHandler.getClientString("err.file.processing"));
        }
        return metaData;
    }

    public boolean removeDocumentFromCache(String documentId, String sessionId) {
        LOGGER.debug("About to remove documentID: " + documentId + " from cache");
        Ehcache cache = ViewerCacheManager.getInstance().getCache(ViewerCacheManager.CACHEID_FILECONTENT);
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        boolean res = cache.remove(fileCacheId);
        LOGGER.debug("Result of removal for documentID: " + documentId + " is:" + res);
        return res;
    }

    public boolean removeDocumentFromTemp(String documentPath) {
        LOGGER.debug("About to remove documentID: " + documentPath + " from cache");
        File pdffile = new File(ViewerConfigManager.getInstance().getWebDir(), documentPath);
        if (pdffile.delete()) {
            LOGGER.debug("Result of removal for documentID: " + documentPath + " is:" + true);
            return true;
        } else {
            LOGGER.debug("Result of removal for documentID: " + documentPath + " is:" + false);
            return false;
        }

    }

    public String getErrorMsg(String documentId, String sessionId) {
        String errMsg = null;
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        errMsg = (String)ViewerCacheManager.getInstance().getFromCache(fileCacheId, ViewerCacheManager.CACHEID_FILECONTENT);
        if (errMsg == null) {
            errMsg = ViewerMessageHandler.getClientString("err.unauthorized.view");
        }
        return errMsg;
    }

    public void generateContent(String documentId, int page, String sessionId, int zoom, HttpServletResponse response)
            throws RMSException, ReflectiveOperationException, IOException {
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
        LOGGER.debug("Getting content from document: {} with documentId: {}", cachedFile.getFileName(), documentId);

        if (isSingleImageFile(cachedFile)) {
            response.setContentType(cachedFile.getMediaType().toString());
            response.getOutputStream().write(cachedFile.getFileContent());
        } else {
            response.setContentType(MediaType.image("png").toString());
            ImageProcessor.getInstance().generateDocContent(cachedFile.getFileContent(), page, cachedFile.getWaterMark(), zoom, response.getOutputStream(), cachedFile.getFileName());
            return;
        }
    }

    private boolean isSingleImageFile(CachedFile cachedFile) {
        WaterMark wm = cachedFile.getWaterMark();
        MediaType type = cachedFile.getMediaType();
        return (wm == null || !StringUtils.hasText(wm.getWaterMarkStr())) && type != null && IMAGE_MIME_TYPES.contains(type.toString());
    }

    public PrintFileUrl generatePDF(String documentId, String folderPath, String sessionId) throws RMSException {
        CachedFile cachedFile = null;
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        try {
            cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
        } catch (RMSException e) {
            LOGGER.error("Unable to find document in cache (Document ID: " + documentId + ")", e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        }
        return ImageProcessor.getInstance().convertFileToPDF(cachedFile.getFileContent(), folderPath, cachedFile.getFileName(), cachedFile.getWaterMark());
    }

    public boolean checkPrintPermission(String documentId, String sessionId) {
        CachedFile cachedFile = null;
        FileCacheId fileCacheId = new FileCacheId(sessionId, documentId);
        try {
            cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
        } catch (RMSException e) {
            LOGGER.error("Unable to find document in cache (Document ID: " + documentId + ")", e);
            return false;
        }
        return cachedFile.isOwner() || isFlagSet(cachedFile.getRights(), Rights.PRINT.getValue());
    }

    public static boolean isFlagSet(int flags, int mask) {
        return (flags & mask) == mask;
    }

}
