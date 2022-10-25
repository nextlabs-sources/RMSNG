package com.nextlabs.rms.viewer.config;

import com.nextlabs.rms.shared.CleanUpUtil;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewerNightlyMaintenance extends TimerTask {

    private static final long INACTIVE_FILE_DURATION = 2 * 60 * 60 * 1000L;
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void run() {
        LOGGER.info("Starting Nightly Maintenance");
        try {
            cleanupTempFolders();
        } catch (Exception e) {
            LOGGER.error("Error occurred while cleaning up temp folders", e);
        }
        LOGGER.info("Nightly Maintenance completed");
    }

    private void cleanupTempFolders() throws FileNotFoundException {
        File viewerSharedDir = ViewerConfigManager.getInstance().getViewerSharedTempDir();
        File viewerCommonDir = ViewerConfigManager.getInstance().getCommonSharedTempDir();
        File viewerTmpDir = ViewerConfigManager.getInstance().getTempDir();
        CleanUpUtil.cleanUpFolder(viewerSharedDir, INACTIVE_FILE_DURATION);
        CleanUpUtil.cleanUpFolder(viewerCommonDir, INACTIVE_FILE_DURATION);
        CleanUpUtil.cleanUpFolder(viewerTmpDir, INACTIVE_FILE_DURATION);
    }

}
