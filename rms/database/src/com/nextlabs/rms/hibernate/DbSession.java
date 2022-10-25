package com.nextlabs.rms.hibernate;

import java.io.Closeable;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;

public class DbSession implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(DbSession.class);
    private Session session;
    private Transaction tx;

    public DbSession(Session session) {
        this.session = session;
    }

    public static DbSession newSession() {
        return HibernateUtils.newSession();
    }

    public Session getSession() {
        return session;
    }

    public long nextCsn() {
        return HibernateUtils.nextCsn(this);
    }

    public void clear() {
        session.clear();
    }

    @Override
    public void close() {
        rollback();
        session.close();
    }

    public Criteria createCriteria(Class<?> persistentClass) {
        return session.createCriteria(persistentClass);
    }

    public Criteria createCriteria(Class<?> persistentClass, String alias) {
        return session.createCriteria(persistentClass, alias);
    }

    public Query createFilter(Object obj, String filter) {
        return session.createFilter(obj, filter);
    }

    public Query createQuery(String queryString) {
        return session.createQuery(queryString);
    }

    public SQLQuery createSQLQuery(String queryString) {
        return session.createSQLQuery(queryString);
    }

    public Query createNamedQuery(String namedQuery) {
        return session.getNamedQuery(namedQuery);
    }

    public void delete(Object obj) {
        session.delete(obj);
    }

    public void evict(Object obj) {
        session.evict(obj);
    }

    public void flush() {
        session.flush();
    }

    public void refresh(Object obj) {
        session.refresh(obj);
    }

    public void update(Object obj) {
        session.update(obj);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Serializable id) {
        return (T)session.get(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> clazz, Serializable id) {
        return (T)session.load(clazz, id);
    }

    public Serializable save(Object obj) {
        return session.save(obj);
    }

    public void saveOrUpdate(Object obj) {
        session.saveOrUpdate(obj);
    }

    public void replicate(Object obj, ReplicationMode mode) {
        session.replicate(obj, mode);
    }

    public void doWork(Work wrk) {
        session.doWork(wrk);
    }

    public Transaction beginTransaction() {
        if (!isActive()) {
            tx = session.beginTransaction();
        }
        return tx;
    }

    public void commit() {
        commit(false);
    }

    public void commit(boolean restart) {
        if (tx != null && tx.isActive()) {
            tx.commit();
            if (restart) {
                tx = session.beginTransaction();
            }
        }
    }

    private void rollback() {
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public boolean isActive() {
        if (tx == null) {
            return false;
        }
        return tx.isActive();
    }
}
