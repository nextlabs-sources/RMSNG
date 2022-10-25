package com.nextlabs.rms.viewer.config;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewerNightlyMaintenanceManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public void scheduleNightlyMaintenance() {
        try {
            GregorianCalendar cal = new GregorianCalendar();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int date = cal.get(Calendar.DATE);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            cal.set(year, month, date, 2, 0);

            if (hour >= 2) { //it is already past 2 AM. Schedule the task for the next day.
                cal.add(Calendar.DATE, 1);
            }
            long initialDelay = cal.getTimeInMillis() - System.currentTimeMillis();
            if (initialDelay < 0) {
                initialDelay = 0L;
            }
            int period = 24 * 60 * 60 * 1000;
            Timer timer = new Timer();
            ViewerNightlyMaintenance maintTask = new ViewerNightlyMaintenance();
            timer.scheduleAtFixedRate(maintTask, initialDelay, period);
            LOGGER.info("Nightly Maintenance scheduled");
        } catch (Throwable e) {
            LOGGER.error("Error occurred while scheduling nightly maintenance", e);
        }
    }
}
