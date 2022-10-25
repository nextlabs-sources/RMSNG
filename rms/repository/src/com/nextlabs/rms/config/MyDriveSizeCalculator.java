package com.nextlabs.rms.config;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.services.manager.TaskManager;
import com.nextlabs.rms.shared.LogConstants;

import java.io.Closeable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class MyDriveSizeCalculator implements Runnable {

    private static final String RESOURCE = "RESOURCE_DEFAULT_REPO_SIZE";
    private static final long PERIOD = 24 * 60 * 60 * 1000L;
    private static final long TIMEOUT = 24 * 60 * 60 * 1000L;
    private static final int MAX_BATCH_SIZE = 1000;

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        LOGGER.info("MyDrive size computation: START");
        boolean lock = LockManager.getInstance().acquireLock(RESOURCE, TIMEOUT);
        if (lock) {
            try {
                Date date = TaskManager.getInstance().getLastSuccessfulUpdateTime(RESOURCE);
                if (date == null) {
                    date = new Date(0);
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                if ((System.currentTimeMillis() - calendar.getTimeInMillis()) <= PERIOD) {
                    return;
                }
                DbSession session = DbSession.newSession();
                int offset = 0;
                int count = 0;
                String defaultTenantId = null;

                try {
                    Criteria criteria = session.createCriteria(Tenant.class);
                    criteria.add(Restrictions.eq("name", WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, Constants.DEFAULT_TENANT)));
                    List<Tenant> tenants = criteria.list();
                    defaultTenantId = tenants.get(0).getId();

                    count = DefaultRepositoryManager.getInstance().getNumberDefaultRepositories(session, defaultTenantId);
                } catch (InvalidDefaultRepositoryException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    session.close();
                }
                ExecutorService executor = Executors.newFixedThreadPool(10);
                while (offset < count) {
                    Runnable worker = new MyDriveSizeCalculatorThread(defaultTenantId, offset, Math.min(MAX_BATCH_SIZE, count - offset));
                    executor.execute(worker);
                    offset += MAX_BATCH_SIZE;
                }
                executor.shutdown();
                executor.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
                TaskManager.getInstance().updateSuccess(RESOURCE);
                LOGGER.info("MyDrive size computation: END");
            } catch (InterruptedException e) {
                TaskManager.getInstance().updateFail(RESOURCE);
                LOGGER.error(e.getMessage(), e);
                Thread.currentThread().interrupt(); //"InterruptedException" should not be ignored (Sonar)
            } finally {
                LockManager.getInstance().releaseLock(RESOURCE);
            }
        }
    }

    private static class MyDriveSizeCalculatorThread implements Runnable {

        private String defaultTenantId;
        private int offset;
        private int batchSize;

        public MyDriveSizeCalculatorThread(String defaultTenantId, int offset, int batchSize) {
            this.defaultTenantId = defaultTenantId;
            this.offset = offset;
            this.batchSize = batchSize;
        }

        @Override
        public void run() {
            List<DefaultRepositoryTemplate> defaultRepositories = null;
            DbSession session = DbSession.newSession();
            try {
                defaultRepositories = DefaultRepositoryManager.getInstance().getDefaultRepositories(session, defaultTenantId, offset, batchSize);
            } catch (InvalidDefaultRepositoryException | RepositoryException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                session.close();
            }

            if (defaultRepositories != null && !defaultRepositories.isEmpty()) {
                for (DefaultRepositoryTemplate repo : defaultRepositories) {
                    try {
                        repo.updateRepositorySize();
                    } finally {
                        if (repo instanceof Closeable) {
                            IOUtils.closeQuietly(Closeable.class.cast(repo));
                        }
                    }
                }
            }
        }
    }
}
