package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.ConverterFactory;
import com.nextlabs.rms.viewer.conversion.FileTypeDetector;
import com.nextlabs.rms.viewer.conversion.IFileConverter;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

public class CADVisManager implements IVisManager {

    private static final Logger LOGGER = LogManager.getLogger(CADVisManager.class);

    @Override
    public String getVisURL(String user, int offset, String domain,
        String sessionId, byte[] fileContent, String fileNameWithoutNXL, String cacheId) throws RMSException {

        String redirectURL = null;
        String dataDir = ViewerConfigManager.getInstance().getTempDir() + File.separator + sessionId;
        File inputFile = new File(dataDir, fileNameWithoutNXL);
        try {
            EvaluationHandler.writeContentsToFile(dataDir, fileNameWithoutNXL, fileContent);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error occurred while processing the file: " + fileNameWithoutNXL + e.getMessage());
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } catch (IOException e) {
            LOGGER.error("Error occurred while processing the file: " + fileNameWithoutNXL + e.getMessage());
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        }
        redirectURL = getVisURL(user, offset, domain, sessionId, inputFile, inputFile.getName(), cacheId);
        return redirectURL;
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException {
        /*
        if (!LicenseManager.getInstance().isFeatureLicensed(LicensedFeature.FEATURE_VIEW_CAD_FILE)) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.invalid.license"));
        }
        */

        String redirectURL = null;
        String filePath = null;
        String docIdWithExt = null;
        String fileNameWithoutNXL = folderpath.getName();
        boolean isFileNameAsDocId = ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.USE_FILENAME_AS_DOCUMENTID);
        try {
            docIdWithExt = cacheId + VisManagerFactory.SCS_FILE_EXTN;
            if (isFileNameAsDocId) {
                //for testing purpose
                filePath = new StringBuilder(ViewerConfigManager.USE_FILENAME_AS_DOCUMENTID).append('/').append(cacheId).append('/').append(replaceFileType(fileNameWithoutNXL)).toString();
            } else {
                filePath = new StringBuilder(ViewerConfigManager.TEMPDIR_NAME).append('/').append(sessionId).append('/').append(cacheId).append('/').append(docIdWithExt).toString();
            }
            String destinationPath = ViewerConfigManager.getInstance().getWebDir() + filePath;
            File destFile = new File(destinationPath);
            //to create temp/session folder if it is not present
            FileUtils.mkdirParent(destFile);
            IFileConverter cadconverter = ConverterFactory.getInstance().getConverter(ConverterFactory.CONVERTER_TYPE_CAD);
            boolean conversionResult = cadconverter.convertFile(folderpath.getAbsolutePath(), destinationPath);
            FileCacheId fileCacheId = new FileCacheId(sessionId, cacheId);
            CachedFile cachedFile = ViewerCacheManager.getInstance().getCachedFile(fileCacheId);
            cachedFile.setContentType(CachedFile.ContentType._3D);
            if (conversionResult) {
                File convertedFile = new File(destinationPath);
                if (!convertedFile.exists()) {
                    if (FileUtils.getRealFileExtension(fileNameWithoutNXL).equalsIgnoreCase(ViewerConfigManager.PDF_FILE_EXTN)) {
                        LOGGER.warn("3d conversion failed.About to start default conversion for file :" + fileNameWithoutNXL);
                        GenericVisManager genericVisManager = new GenericVisManager();
                        byte[] fileContent = Files.readAllBytes(folderpath.toPath());
                        redirectURL = genericVisManager.getVisURL(user, offset, domain, sessionId, fileContent, fileNameWithoutNXL, cacheId);
                        return redirectURL;
                    }
                    LOGGER.error("File conversion failed and not present in " + destinationPath);
                    throw new RMSException("There was an error while processing the file.");
                }

                final int bufferSize = (int)(convertedFile.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : convertedFile.length());
                byte[] fileContent = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
                InputStream is = null;
                try {
                    is = new FileInputStream(convertedFile);
                    IOUtils.copy(is, baos);
                    fileContent = baos.toByteArray();
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(baos);
                }
                cachedFile.setFileContent(fileContent);
                MediaType type = FileTypeDetector.getMimeType(fileContent, fileNameWithoutNXL);
                cachedFile.setMediaType(type);
                ViewerCacheManager.getInstance().putInCache(fileCacheId, cachedFile, ViewerCacheManager.CACHEID_FILECONTENT);
                redirectURL = new StringBuilder("/CADViewer.jsp?d=").append(cacheId).append("&s=").append(sessionId).toString();
            } else {
                LOGGER.error("Error occurred while processing the file:" + fileNameWithoutNXL);
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"));
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Error occurred while processing the file: " + fileNameWithoutNXL + e.getMessage());
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } catch (IOException e) {
            LOGGER.error("Error occurred while processing the file: " + fileNameWithoutNXL + e.getMessage());
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } finally {
            try {
                FileUtils.deleteDirectory(folderpath.getParentFile());
            } catch (IOException e) {
                LOGGER.error("Failed to delete temp directory.", e);
            }
        }
        return redirectURL;
    }

    private String replaceFileType(String fileNameWithoutNXL) {
        String fileExtension = getFileExtension(fileNameWithoutNXL);
        return fileNameWithoutNXL.replaceFirst(fileExtension, VisManagerFactory.SCS_FILE_EXTN);
    }

    private String getFileExtension(String fileNameWithoutNXL) {
        int index = fileNameWithoutNXL.toLowerCase().lastIndexOf(".");
        return fileNameWithoutNXL.substring(index, fileNameWithoutNXL.length());
    }
}
