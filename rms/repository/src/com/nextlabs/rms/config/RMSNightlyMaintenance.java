package com.nextlabs.rms.config;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.shared.CleanUpUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RMSNightlyMaintenance extends TimerTask {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final long INACTIVE_FILE_DURATION = 2L * 60 * 60 * 1000;

    @Override
    public void run() {
        logger.info("Starting Nightly Maintenance");
        try {
            cleanupTempFolders();
        } catch (Exception e) {
            logger.error("Error occurred while cleaning up temp folders", e);
        }
        try {
            RepositoryFileManager.cleanupInactiveRecords();
        } catch (Exception e) {
            logger.error("Error occurred while cleaning up inactive database records", e);
        }
        logger.info("Nightly Maintenance completed");
    }

    private void cleanupTempFolders() throws FileNotFoundException {
        File tmpDir = WebConfig.getInstance().getTmpDir();
        File rmsSharedDir = WebConfig.getInstance().getRmsSharedTempDir();
        File commonSharedDir = WebConfig.getInstance().getCommonSharedTempDir();
        CleanUpUtil.cleanUpFolder(tmpDir, INACTIVE_FILE_DURATION);
        CleanUpUtil.cleanUpFolder(rmsSharedDir, INACTIVE_FILE_DURATION);
        CleanUpUtil.cleanUpFolder(commonSharedDir, INACTIVE_FILE_DURATION);
    }
}
