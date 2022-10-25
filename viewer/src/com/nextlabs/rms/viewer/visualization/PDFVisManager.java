package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class PDFVisManager implements IVisManager {

    private static final Logger LOGGER = LogManager.getLogger(PDFVisManager.class);
    private static final IVisManager[] PDF_HANDLERS = { new CADVisManager(), new GenericVisManager() };
    private static final String PDF_3D = "3D";
    private static final String U3D = "U3D";
    private static final String[] ELEMENTS_3D = { PDF_3D, U3D };

    public enum PDFType {
        PDF_2D,
        PDF_3D,
        UNKNOWN
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVisURL(String user, int offset, String domain, String sessionId, byte[] fileContent,
        String displayName, String cacheId) throws RMSException {
        long start = System.currentTimeMillis();
        PDFType pdfType = determinePDFType(fileContent, displayName);
        long finish = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Time taken to determine PDF type of '" + displayName + "' (type: " + pdfType + "): " + (finish - start) + " ms");
        }
        String url = null;
        try {
            if (pdfType == PDFType.PDF_3D) {
                IVisManager[] managers = { PDF_HANDLERS[0], PDF_HANDLERS[1] };
                url = generateVisURL(user, offset, domain, sessionId, fileContent, displayName, cacheId, managers);
            } else if (pdfType == PDFType.PDF_2D) {
                IVisManager[] managers = { PDF_HANDLERS[1], PDF_HANDLERS[0] };
                url = generateVisURL(user, offset, domain, sessionId, fileContent, displayName, cacheId, managers);
            } else {
                ViewerConfigManager configManager = ViewerConfigManager.getInstance();
                boolean use2DPDF = configManager.getBooleanProperty(ViewerConfigManager.USE_2D_PDF_VIEWER);
                if (use2DPDF) {
                    IVisManager[] managers = { PDF_HANDLERS[1], PDF_HANDLERS[0] };
                    url = generateVisURL(user, offset, domain, sessionId, fileContent, displayName, cacheId, managers);
                } else {
                    IVisManager[] managers = { PDF_HANDLERS[0], PDF_HANDLERS[1] };
                    url = generateVisURL(user, offset, domain, sessionId, fileContent, displayName, cacheId, managers);
                }
            }
            return url;
        } catch (Exception e) {
            LOGGER.error("Unable to process file '" + displayName + "': " + e.getMessage(), e);
            if (e instanceof RMSException) {
                throw e;
            }
            throw new RMSException("There was an error while processing the file.", e);
        }
    }

    private String generateVisURL(String user, int offset, String domain, String sessionId,
        byte[] fileContent,
        String displayName, String cacheId, IVisManager... handlers) throws RMSException {
        if (handlers != null) {
            int total = handlers.length;
            int idx = 0;
            for (IVisManager handler : handlers) {
                try {
                    return handler.getVisURL(user, offset, domain, sessionId, fileContent, displayName, cacheId);
                } catch (Exception e) {
                    if (idx == total - 1) {
                        if (e instanceof RMSException) {
                            throw (RMSException)e;
                        } else {
                            throw new RMSException(e.getMessage(), e);
                        }
                    }
                }
                ++idx;
            }
        }
        throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"));
    }

    public static PDFType determinePDFType(byte[] contents, String displayName) {
        try (PDDocument doc = PDDocument.load(contents)) {
            boolean pdf3D = false;
            PDPageTree pages = doc.getPages();
            for (PDPage page : pages) {
                List<PDAnnotation> annotations = page.getAnnotations();
                for (PDAnnotation annotation : annotations) {
                    if (StringUtils.containsElement(ELEMENTS_3D, annotation.getSubtype(), false)) {
                        pdf3D = true;
                        break;
                    }
                }
                if (pdf3D) {
                    // if we already any of 3D element, we can break the loop
                    break;
                }
            }
            return pdf3D ? PDFType.PDF_3D : PDFType.PDF_2D;
        } catch (IOException e) {
            LOGGER.error("Unable to determine PDF type of '" + displayName + "': " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unable to determine PDF type of '" + displayName + "': " + e.getMessage(), e);
        }
        return PDFType.UNKNOWN;
    }

    public static PDFType determinePDFType(InputStream is, String displayName) {
        try (PDDocument doc = PDDocument.load(is)) {
            boolean pdf3D = false;
            PDPageTree pages = doc.getPages();
            for (PDPage page : pages) {
                List<PDAnnotation> annotations = page.getAnnotations();
                for (PDAnnotation annotation : annotations) {
                    if (StringUtils.containsElement(ELEMENTS_3D, annotation.getSubtype(), false)) {
                        pdf3D = true;
                        break;
                    }
                }
                if (pdf3D) {
                    // if we already any of 3D element, we can break the loop
                    break;
                }
            }
            return pdf3D ? PDFType.PDF_3D : PDFType.PDF_2D;
        } catch (IOException e) {
            LOGGER.error("Unable to determine PDF type of '" + displayName + "': " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unable to determine PDF type of '" + displayName + "': " + e.getMessage(), e);
        }
        return PDFType.UNKNOWN;
    }
}
