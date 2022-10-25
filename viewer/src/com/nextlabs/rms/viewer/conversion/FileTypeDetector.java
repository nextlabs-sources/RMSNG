package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public final class FileTypeDetector {

    private static Detector fileTypeDetector;
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    static {
        try {
            final TikaConfig tikaConfig = new TikaConfig();
            fileTypeDetector = tikaConfig.getDetector();
        } catch (TikaException | IOException e) {
            LOGGER.error("Error occurred while initializing tika detector.", e);
        }
    }

    private FileTypeDetector() {

    }

    public static MediaType getMimeType(byte[] content, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
            return fileTypeDetector.detect(TikaInputStream.get(new ByteArrayInputStream(content)), metadata);
        } catch (IOException e) {
            LOGGER.debug("Failed to determine mime type: {}", e.getMessage(), e);
        }
        return null;
    }

}
