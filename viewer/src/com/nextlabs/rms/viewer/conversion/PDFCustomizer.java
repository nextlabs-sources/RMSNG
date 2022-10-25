package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;

public final class PDFCustomizer {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private PDFCustomizer() {
    }

    public static void addAutoPrintOption(File inputFile) {
        PDDocument document = null;
        try {
            document = PDDocument.load(inputFile, MemoryUsageSetting.setupTempFileOnly());
            PDActionJavaScript javascript = new PDActionJavaScript("this.print();");
            document.getDocumentCatalog().setOpenAction(javascript);
            if (document.isEncrypted()) {
                throw new IOException("Encrypted documents are not supported");
            }
            document.save(inputFile);
        } catch (IOException e) {
            LOGGER.error("Error occurred while adding Auto Print option to PDF: " + e.getMessage(), e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    LOGGER.error("Error occurred while closing document", e);
                }
            }
        }
    }
}
