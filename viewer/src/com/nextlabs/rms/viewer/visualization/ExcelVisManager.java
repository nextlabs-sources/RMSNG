package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.conversion.HTMLFileGenerator;
import com.nextlabs.rms.viewer.conversion.HTMLFileGenerator.WatermarkWrapper;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

public class ExcelVisManager implements IVisManager {

    private static final String HTML_FILE_EXTN = ".html";

    private static Logger logger = LogManager.getLogger(ExcelVisManager.class);

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, byte[] fileContent,
        String displayName, String cacheId) throws RMSException {
        /*
                if (!LicenseManager.getInstance().isFeatureLicensed(LicensedFeature.FEATURE_VIEW_GENERIC_FILE)) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.invalid.license"));
                }
         */
        if (!EvaluationHandler.hasDocConversionJarFiles()) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.missing.viewer.package", "Document Viewer", FileUtils.getRealFileExtension(displayName).substring(1).toUpperCase()));
        }

        String redirectURL = null;

        try {
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(new FileCacheId(sessionId, cacheId));
            String htmlFileName = getHtmlFileName(cachedFile.getOriginalFileName());
            WatermarkWrapper watermarkWrapper = HTMLFileGenerator.handleExcelFile(sessionId, cacheId, htmlFileName, fileContent, user, offset, domain, cachedFile.getWaterMark());
            File convertedFile = watermarkWrapper.getFileHTML();
            final int bufferSize = (int)(convertedFile.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : convertedFile.length());
            byte[] convertedFileContent = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            InputStream is = null;
            try {
                is = new FileInputStream(convertedFile);
                IOUtils.copy(is, baos);
                convertedFileContent = baos.toByteArray();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(baos);
            }
            cachedFile.setFileContent(convertedFileContent);
            cachedFile.setMediaType(MediaType.TEXT_HTML);
            cachedFile.setContentType(CachedFile.ContentType._2D);
            cachedFile.setWatermarkPNG(watermarkWrapper.getWatermarkPNG());
            ViewerCacheManager.getInstance().putInCache(new FileCacheId(sessionId, cacheId), cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
            redirectURL = new StringBuilder().append("/RMSViewer/GetFileContent?d=").append(cacheId).append("&s=").append(sessionId).toString();
        } catch (UnsupportedEncodingException e) {
            logger.error("Error occured while encoding HTML File:");
        } catch (IOException e) {
            logger.error("Error occured while converting file to html:");
        }
        return redirectURL;
    }

    private String getHtmlFileName(String origFileName) {
        return origFileName + HTML_FILE_EXTN;
    }

}
