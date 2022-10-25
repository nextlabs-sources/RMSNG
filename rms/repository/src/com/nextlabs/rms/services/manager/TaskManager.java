package com.nextlabs.rms.services.manager;

import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.TaskStatus;
import com.nextlabs.rms.shared.LogConstants;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;

public final class TaskManager {

    public enum Status {
        SUCCESS(0),
        FAIL(1);

        private final int status;

        private Status(final int stat) {
            this.status = stat;
        }

        public int getStatus() {
            return status;
        }
    }

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final TaskManager INSTANCE = new TaskManager();

    private TaskManager() {
    }

    public static TaskManager getInstance() {
        return INSTANCE;
    }

    public boolean updateSuccess(String resourceId) {
        boolean result = false;
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            TaskStatus taskStatus = session.get(TaskStatus.class, resourceId);
            if (taskStatus == null) {
                taskStatus = new TaskStatus();
                taskStatus.setResource(resourceId);
                taskStatus.setLastSuccessfulUpdate(new Date());
                taskStatus.setLastFailedUpdate(new Date(0));
                taskStatus.setStatus(Status.SUCCESS.getStatus());
                session.save(taskStatus);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("New task status record created for resource {}", resourceId);
                }
            } else {
                taskStatus.setStatus(Status.SUCCESS.getStatus());
                taskStatus.setLastSuccessfulUpdate(new Date());
            }

            session.commit();
            result = true;
        } catch (HibernateException e) {
            LOGGER.error("General error while trying to update Task status for resource {}", resourceId);
        } catch (Exception e) {
            LOGGER.error("General error while trying to update Task status for resource {}", resourceId);
        } finally {
            session.close();
        }

        return result;
    }

    public Date getLastSuccessfulUpdateTime(String resourceId) {
        DbSession session = DbSession.newSession();
        try {
            TaskStatus taskStatus = session.get(TaskStatus.class, resourceId);
            if (taskStatus == null) {
                return null;
            } else {
                return taskStatus.getLastSuccessfulUpdate();
            }
        } catch (HibernateException e) {
            LOGGER.error("General error while trying to get Task status for resource {}", resourceId);
        } catch (Exception e) {
            LOGGER.error("General error while trying to get Task status for resource {}", resourceId);
        } finally {
            session.close();
        }
        return null;
    }

    public boolean updateFail(String resourceId) {
        boolean result = false;
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            TaskStatus taskStatus = session.get(TaskStatus.class, resourceId);
            if (taskStatus == null) {
                taskStatus = new TaskStatus();
                taskStatus.setResource(resourceId);
                taskStatus.setLastFailedUpdate(new Date());
                taskStatus.setLastSuccessfulUpdate(new Date(0));
                taskStatus.setStatus(Status.FAIL.getStatus());
                session.save(taskStatus);
                LOGGER.debug("New task status record created for resource " + resourceId);
            } else {
                taskStatus.setStatus(Status.FAIL.getStatus());
                taskStatus.setLastFailedUpdate(new Date());
            }
            session.commit();
            result = true;
        } catch (HibernateException e) {
            LOGGER.error("General error while trying to update Task status for resource {}", resourceId);
        } catch (Exception e) {
            LOGGER.error("General error while trying to update Task status for resource {}", resourceId);
        } finally {
            session.close();
        }

        return result;
    }
}
