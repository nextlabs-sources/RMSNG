package com.nextlabs.rms.task;

import com.nextlabs.common.util.DateUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ProjectInvitation;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.services.manager.TaskManager;
import com.nextlabs.rms.shared.LogConstants;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.joda.time.DateTime;

public class ProjectCleanup implements Runnable {

    public static final String RESOURCE = "RESOURCE_PROJECT_CLEANUP";

    private static final int INVITATION_RETENTION_DAYS = 7;

    private static final long DEFAULT_LOCK_TIMEOUT = 24 * 60 * 60 * 1000L;

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void run() {
        Date date = TaskManager.getInstance().getLastSuccessfulUpdateTime(RESOURCE);
        if (date == null) {
            date = new Date(0);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if ((System.currentTimeMillis() - calendar.getTimeInMillis()) > ProjectCleanupManager.getCleanupFrequency() * DateUtils.MILLIS_PER_MINUTE) {
            boolean lock = LockManager.getInstance().acquireLock(RESOURCE, DEFAULT_LOCK_TIMEOUT);
            if (lock) {
                try (DbSession session = DbSession.newSession()) {
                    try {
                        int count = setInvitationExpireStatus(session);
                        LOGGER.info("ProjectCleanup: Marked {} invitations as expired.", count);
                        TaskManager.getInstance().updateSuccess(RESOURCE);
                    } catch (Exception e) {
                        TaskManager.getInstance().updateFail(RESOURCE);
                        LOGGER.error("Error occurred while setting invitation expire status", e);
                    }

                    try {
                        int count = cleanupInvitation(session);
                        LOGGER.info("ProjectCleanup: Cleared {} invitations older than {} days.", count, INVITATION_RETENTION_DAYS);
                        TaskManager.getInstance().updateSuccess(RESOURCE);
                    } catch (Exception e) {
                        TaskManager.getInstance().updateFail(RESOURCE);
                        LOGGER.error("Error occurred while cleanup invitation record", e);
                    }
                } finally {
                    LockManager.getInstance().releaseLock(RESOURCE);
                }
            }
        }

    }

    private int setInvitationExpireStatus(DbSession session) throws ParseException {
        session.beginTransaction();
        Query query = session.createQuery("UPDATE ProjectInvitation set status =:status, actionTime =:now where expireDate < :today and status in (:statusList)");
        query.setParameter("status", ProjectInvitation.Status.EXPIRED);
        query.setParameterList("statusList", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING });
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
        query.setTimestamp("now", new Date());
        query.setDate("today", today);
        int count = query.executeUpdate();
        session.commit();
        return count;
    }

    private int cleanupInvitation(DbSession session) {
        session.beginTransaction();
        Query query = session.createQuery("DELETE FROM ProjectInvitation where status IN (:status) AND actionTime <:cleanupFrom");
        query.setParameterList("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.ACCEPTED,
            ProjectInvitation.Status.DECLINED, ProjectInvitation.Status.EXPIRED, ProjectInvitation.Status.REVOKED });
        Date now = new Date();
        Date cleanupFrom = new DateTime(now).minusDays(INVITATION_RETENTION_DAYS).toDate();
        query.setTimestamp("cleanupFrom", cleanupFrom);
        int count = query.executeUpdate();
        session.commit();
        return count;
    }

}
