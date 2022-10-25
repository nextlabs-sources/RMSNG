package com.nextlabs.rms.services.manager;

import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ResourceLock;
import com.nextlabs.rms.shared.LogConstants;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

public final class LockManager {

    public static final long DEFAULT_PERIOD = 0L;
    public static final long DEFAULT_TIMEOUT = 60 * 60 * 1000L;

    public enum Status {
        DISABLED(0),
        ACTIVE(1);

        private final int status;

        private Status(final int stat) {
            status = stat;
        }

        public int getStatus() {
            return status;
        }
    }

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static LockManager instance = new LockManager();

    private LockManager() {
    }

    public static LockManager getInstance() {
        return instance;
    }

    public boolean acquireLock(String resourceId) {
        return acquireLock(resourceId, DEFAULT_TIMEOUT);
    }

    public boolean acquireLock(String resourceId, long timeout) {
        boolean result = false;
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            ResourceLock lock = (ResourceLock)session.getSession().get(ResourceLock.class, resourceId, new LockOptions(LockMode.UPGRADE_NOWAIT));
            if (lock == null) {
                lock = new ResourceLock();
                lock.setResource(resourceId);
                lock.setLastUpdated(new Date());
                lock.setStatus(Status.ACTIVE.getStatus());
                session.save(lock);
                result = true;
                LOGGER.debug("New locking record created for resource " + resourceId);
            } else if (lock.getStatus() == Status.DISABLED.getStatus()) {
                lock.setStatus(Status.ACTIVE.getStatus());
                lock.setLastUpdated(new Date());
                result = true;
            } else if (lock.getStatus() == Status.ACTIVE.getStatus() && System.currentTimeMillis() > lock.getLastUpdated().getTime() + timeout) {
                lock.setLastUpdated(new Date());
                result = true;
            }

            session.commit();
        } catch (HibernateException e) {
            LOGGER.debug("Attempted to acquire lock for resource " + resourceId + ", but was denied");
            result = false;
        } catch (Exception e) {
            LOGGER.error("General error while trying to acquire lock for resource " + resourceId);
            result = false;
        } finally {
            session.close();
        }

        return result;
    }

    public void releaseLock(String resourceId) {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            ResourceLock lock = (ResourceLock)session.getSession().get(ResourceLock.class, resourceId, LockOptions.UPGRADE);
            if (lock == null) {
                LOGGER.error("Attempted to release lock for resource " + resourceId + ", but this resource lock does not exist.");
            } else {
                lock.setStatus(Status.DISABLED.getStatus());
                lock.setLastUpdated(new Date());
            }

            session.commit();
        } catch (HibernateException e) {
            LOGGER.error("Attempted to release lock for resource " + resourceId + ", but was blocked by other thread");
        } catch (Exception e) {
            LOGGER.error("General error while trying to release lock for resource " + resourceId);
        } finally {
            session.close();
        }
    }

    public void releaseRemoveLock(String resourceId) {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            ResourceLock lock = (ResourceLock)session.getSession().get(ResourceLock.class, resourceId, LockOptions.UPGRADE);
            if (lock == null) {
                LOGGER.error("Attempted to release and remove lock for resource " + resourceId + ", but this resource lock does not exist.");
            } else {
                session.delete(lock);
            }

            session.commit();
        } catch (HibernateException e) {
            LOGGER.error("Attempted to release and remove lock for resource " + resourceId + ", but was blocked by other thread");
        } catch (Exception e) {
            LOGGER.error("General error while trying to release lock for resource " + resourceId);
        } finally {
            session.close();
        }
    }
}
