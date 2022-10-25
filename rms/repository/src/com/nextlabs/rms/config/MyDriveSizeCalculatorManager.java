package com.nextlabs.rms.config;

import com.nextlabs.rms.shared.LogConstants;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyDriveSizeCalculatorManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final long TWENTY_FOUR_HOURS_IN_MILLIS = 24 * 60 * 60 * 1000L;

    public void scheduleReadDefaultStorage() {
        try {
            GregorianCalendar cal = new GregorianCalendar();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int date = cal.get(Calendar.DATE);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            cal.set(year, month, date, hour + 1, 0);
            long initialDelay = cal.getTimeInMillis() - System.currentTimeMillis();
            if (initialDelay < 0) {
                initialDelay = 0L;
            }
            MyDriveSizeCalculator task = new MyDriveSizeCalculator();
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(task, initialDelay, TWENTY_FOUR_HOURS_IN_MILLIS, TimeUnit.MILLISECONDS);
            LOGGER.info("Default storage usage scheduled");
        } catch (Throwable e) {
            LOGGER.error("Error occurred while scheduling default storage usage update", e);
        }
    }
}
