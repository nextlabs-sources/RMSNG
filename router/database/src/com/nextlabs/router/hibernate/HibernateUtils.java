package com.nextlabs.router.hibernate;

import com.googlecode.flyway.core.Flyway;

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hibernate.service.ServiceRegistry;

/**
 * Startup Hibernate and provide access to the singleton SessionFactory
 */
public final class HibernateUtils {

    private static final Logger LOGGER = LogManager.getLogger(HibernateUtils.class);
    private static SessionFactory sessionFactory;

    private HibernateUtils() {
    }

    public static synchronized void bootstrap(Properties prop) {
        Configuration config = new Configuration().configure();
        if (prop != null && !prop.isEmpty()) {
            config.addProperties(prop);
        }

        StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder();
        ServiceRegistry serviceRegistry = srb.applySettings(config.getProperties()).build();
        sessionFactory = config.buildSessionFactory(serviceRegistry);
        Session session = sessionFactory.openSession();
        try {
            session.doWork(new Work() {

                @Override
                public void execute(Connection connection) {
                    try {
                        Flyway flyway = new Flyway();
                        flyway.setDataSource(new HibernateDataSource(connection));
                        DatabaseMetaData data = connection.getMetaData();
                        String databaseName = data.getDatabaseProductName();
                        flyway.setLocations("db/migration", "db/" + databaseName);
                        flyway.migrate();
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Database migration finished.");
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            });
        } finally {
            session.close();
        }
    }

    public static SessionFactory getSessionFactory() {
        // Alternatively, we could look up in JNDI here
        if (sessionFactory == null) {
            bootstrap(null);
        }
        return sessionFactory;
    }

    public static DbSession newSession() {
        return new DbSession(getSessionFactory().openSession());
    }

    public static void shutdown() {
        // Close caches and connection pools
        if (sessionFactory != null && !getSessionFactory().isClosed()) {
            getSessionFactory().close();
        }
    }

    public static Object getEntity(Class<?> clazz, Serializable id) {
        DbSession session = newSession();
        try {
            return session.get(clazz, id);
        } finally {
            session.close();
        }
    }

    public static void addEntity(Object obj) {
        DbSession session = newSession();
        session.beginTransaction();
        try {
            session.save(obj);
            session.commit();
        } finally {
            session.close();
        }
    }

    public static Date timeAdd(int field, int amount) {
        return timeAdd(System.currentTimeMillis(), field, amount);
    }

    public static Date timeAdd(long start, int field, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        cal.add(field, amount);
        return cal.getTime();
    }

    public static void updateEntity(Object obj) {
        DbSession session = newSession();
        session.beginTransaction();
        try {
            session.update(obj);
            session.commit();
        } finally {
            session.close();
        }
    }

    public static long nextCsn(DbSession session) {
        return nextSequence(session, "csn");
    }

    public static long nextSequence(DbSession session, String seqName) {
        SQLQuery query = session.createSQLQuery("SELECT nextval('" + seqName + "')");
        return ((BigInteger)query.uniqueResult()).longValue();
    }

    public static long currentCsn(DbSession session) {
        SQLQuery query = session.createSQLQuery("SELECT last_value from csn");
        return ((BigInteger)query.uniqueResult()).longValue();
    }

    public static void setLimit(Criteria criteria, int offset, int limit) {
        if (offset > 0) {
            criteria.setFirstResult(offset);
        }
        if (limit > 0) {
            criteria.setMaxResults(limit);
        }
    }

    public static void setLimit(Query query, int offset, int limit) {
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
    }

    private static final class HibernateDataSource implements DataSource {

        private Connection connection;
        private PrintWriter writer;

        public HibernateDataSource(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return writer;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.writer = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T unwrap(Class<T> iface) {
            if (DataSource.class.equals(iface)) {
                return (T)this;
            }
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return DataSource.class.equals(iface);
        }

        public java.util.logging.Logger getParentLogger() {
            return null;
        }
    }
}
