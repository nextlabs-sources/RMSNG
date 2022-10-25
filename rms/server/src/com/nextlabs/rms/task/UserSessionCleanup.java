package com.nextlabs.rms.task;

import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserSession;

import java.util.Date;

import org.hibernate.Query;

public class UserSessionCleanup implements Runnable {

    @Override
    public void run() {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Query query = session.createNamedQuery("remove.inactive.session");
            query.setTimestamp("expirationTime", new Date());
            query.setParameter("status", UserSession.Status.ACTIVE);
            query.executeUpdate();
            session.commit();
        }
    }
}
