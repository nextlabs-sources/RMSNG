package com.nextlabs.rms.task;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.shared.LogConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProjectCleanupManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int PROJECT_CLEANUP_FREQUENCY = 24 * 60;

    public void scheduleInvitationCleanup() {
        try {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            ProjectCleanup cleanupTask = new ProjectCleanup();
            scheduler.scheduleAtFixedRate(cleanupTask, 3, getCleanupFrequency(), TimeUnit.MINUTES);
            LOGGER.info("Project cleanup setting scheduled");
        } catch (Throwable e) {
            LOGGER.error("Error occurred while scheduling project cleanup", e);
        }
    }

    public static int getCleanupFrequency() {
        int frequency = PROJECT_CLEANUP_FREQUENCY;
        if (WebConfig.getInstance().getProperty(WebConfig.PROJECT_CLEANUP_FREQUENCY) != null) {
            frequency = Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.PROJECT_CLEANUP_FREQUENCY));
        }
        return frequency;
    }
}
