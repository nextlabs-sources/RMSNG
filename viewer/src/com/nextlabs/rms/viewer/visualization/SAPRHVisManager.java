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

public class SAPRHVisManager implements IVisManager {

    private static final Logger LOGGER = LogManager.getLogger(SAPRHVisManager.class);

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

        File sapRHViewer = ViewerConfigManager.getInstance().getSAPViewerDir();

        if (!sapRHViewer.exists()) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.missing.viewer.package", "SAP Viewer", "RH"));
        }

        String redirectURL = null;
        String docIdWithExt = null;
        String dataDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + sessionId;
        docIdWithExt = cacheId + ViewerConfigManager.RH_FILE_EXTN;
        File inputFile = new File(dataDir, docIdWithExt);
        try {
            EvaluationHandler.writeContentsToFile(inputFile.getParent(), docIdWithExt, fileContent);
            // at this moment, watermark is not supported by RH viewer

            // Obligation obligation = evalRes.getObligation(EvalRequest.OBLIGATION_WATERMARK);
            // WaterMark waterMark = WatermarkUtil.build(obligation);
            // if (waterMark != null) {
            // waterMark.setWaterMarkStr(WatermarkUtil.updateWaterMark(waterMark.getWaterMarkStr(), user.getUserName(), waterMark));
            // }
            FileCacheId fileCacheId = new FileCacheId(sessionId, cacheId);
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
            cachedFile.setContentType(CachedFile.ContentType._3D);
            ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
            redirectURL = new StringBuilder("/RHViewer.jsp?d=").append(cacheId).append("&s=").append(sessionId).toString();
        } catch (IOException e) {
            LOGGER.error("Failed while processing file : " + fileNameWithoutNXL + " with docId: " + docIdWithExt, e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } catch (Exception e) {
            LOGGER.error("Failed while processing file : " + fileNameWithoutNXL + " with docId: " + docIdWithExt, e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        }
        return redirectURL;
    }

}
