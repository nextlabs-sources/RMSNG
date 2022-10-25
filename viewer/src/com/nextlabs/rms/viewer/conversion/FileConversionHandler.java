/**
 *
 */
package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.visualization.IVisManager;
import com.nextlabs.rms.viewer.visualization.VisManagerFactory;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author nnallagatla
 *
 */
public class FileConversionHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public String convertAndGetURL(String user, int offset, String domain, FileCacheId fCacheId, String displayName,
        File evaluatedFile) throws UnsupportedFormatException, RMSException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converting file: {}", evaluatedFile.getName());
        }
        String fileNameWithoutNXL = EvaluationHandler.getFileNameWithoutNXL(evaluatedFile.getName());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Original filename: {}", fileNameWithoutNXL);
        }
        IVisManager genericVisManager = VisManagerFactory.getInstance().getVisManager(fileNameWithoutNXL);

        String redirectURL = "";

        if (evaluatedFile.isDirectory()) {
            redirectURL = genericVisManager.getVisURL(user, offset, domain, fCacheId.getSessionId(), evaluatedFile, displayName, fCacheId.getDocId());
        } else {
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fCacheId);
            redirectURL = genericVisManager.getVisURL(user, offset, domain, fCacheId.getSessionId(), cachedFile.getFileContent(), displayName, fCacheId.getDocId());
        }
        if (!StringUtils.hasText(redirectURL)) {
            redirectURL = "/ShowError.jsp?code=err.file.processing";
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Returning URL for file '{}': {}", evaluatedFile.getName(), redirectURL);
        }
        return redirectURL;
    }
}
