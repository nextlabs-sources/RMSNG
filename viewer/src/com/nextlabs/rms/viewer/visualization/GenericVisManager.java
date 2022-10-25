package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;

import java.io.File;

public class GenericVisManager implements IVisManager {

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId,
        byte[] fileContent, String displayName, String cacheId) throws RMSException {
        /*
        if (!LicenseManager.getInstance().isFeatureLicensed(LicensedFeature.FEATURE_VIEW_GENERIC_FILE)) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.invalid.license"));
        }
         */
        if (!EvaluationHandler.hasDocConversionJarFiles()) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.missing.viewer.package", "Document Viewer", FileUtils.getRealFileExtension(displayName).substring(1).toUpperCase()));
        }
        FileCacheId fileCacheId = new FileCacheId(sessionId, cacheId);
        CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
        cachedFile.setContentType(CachedFile.ContentType._2D);
        ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
        return new StringBuilder("/DocViewer.jsp?d=").append(cacheId).append("&s=").append(sessionId).toString();
    }

}
