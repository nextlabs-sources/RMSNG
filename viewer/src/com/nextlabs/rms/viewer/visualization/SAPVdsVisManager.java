package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SAPVdsVisManager implements IVisManager {

    private static final Logger LOGGER = LogManager.getLogger(SAPVdsVisManager.class);

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, byte[] fileContent,
        String fileNameWithoutNXL, String cacheId) throws RMSException {
        /*
        if (!LicenseManager.getInstance().isFeatureLicensed(LicensedFeature.FEATURE_VIEW_SAP_3D_FILE)) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.invalid.license"));
        }
        */
        File sapVDSViewer = ViewerConfigManager.getInstance().getSAPViewerDir();
        if (!sapVDSViewer.exists()) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.missing.viewer.package", "SAP Viewer", "VDS"));
        }

        String docIdWithExt = null;
        String redirectURL = null;
        String dataDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + sessionId;
        docIdWithExt = cacheId + ViewerConfigManager.VDS_FILE_EXTN;
        File inputFile = new File(dataDir, docIdWithExt);
        try {
            EvaluationHandler.writeContentsToFile(inputFile.getParent(), docIdWithExt, fileContent);
            FileCacheId fileCacheId = new FileCacheId(sessionId, cacheId);
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
            cachedFile.setContentType(CachedFile.ContentType._3D);
            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
            redirectURL = new StringBuilder("/VDSViewer.jsp?d=").append(cacheId).append("&s=").append(sessionId).toString();
            LOGGER.debug("ReDirectUrl: " + redirectURL);
        } catch (IOException e) {
            LOGGER.error("Error occurred while processing the file: " + fileNameWithoutNXL + " with docId: " + docIdWithExt, e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        }
        return redirectURL;
    }

}
