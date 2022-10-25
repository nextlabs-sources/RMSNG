package com.nextlabs.rms.dao;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.AllNxl.Status;
import com.nextlabs.rms.hibernate.model.ExternalRepositoryNxl;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.User;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

/**
 * @author nnallagatla
 */
public final class SharedFileDAO {

    public static final String FIELD_ORDER_SEPARATOR = "!";
    public static final String FIELD_ORDER_DESC = "1";

    public static final String FILTER_OPTION_ACTIVE = "activeTransaction";
    public static final String FILTER_OPTION_ALL_SHARED = "allShared";
    public static final String FILTER_OPTION_PROTECTED = "protected";
    public static final String FILTER_OPTION_DELETED = "deleted";
    public static final String FILTER_OPTION_REVOKED = "revoked";
    public static final String FILTER_OPTION_FAVORITE = "favorite";

    private SharedFileDAO() {
    }

    @SuppressWarnings("unchecked")
    public static List<AllNxl> getSharedFilesBySharingUser(int ownerId, int page, int size,
        List<String> sortFields, String searchString, boolean showRevoked, String duid) {

        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(AllNxl.class);
            criteria.add(Restrictions.eq("user.id", ownerId));
            if (duid != null) {
                criteria.add(Restrictions.eq("duid", duid));
            }
            if (!showRevoked) {
                criteria.add(Restrictions.eq("status", Status.ACTIVE));
            }
            if (searchString != null && searchString.length() > 0) {
                criteria.add(EscapedLikeRestrictions.ilike("fileName", searchString, MatchMode.ANYWHERE));
            }
            if (sortFields != null) {
                for (String element : sortFields) {
                    String[] parts = element.split(FIELD_ORDER_SEPARATOR);
                    String field = parts[0];
                    if (parts.length < 2 || FIELD_ORDER_DESC.equals(parts[1])) {
                        criteria.addOrder(Order.desc(field));
                    } else {
                        criteria.addOrder(Order.asc(field));
                    }
                }
            }
            criteria.setFirstResult((page - 1) * size);
            criteria.setMaxResults(size);
            return criteria.list();
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<RepoItemMetadata> getSharedFilesBySharingUserInBuiltInRepo(DbSession session, int ownerId,
        int page, int size, List<Criterion> search, List<Order> orders, String filterOptions) {

        DetachedCriteria dc = buildFileSharedByUserCriteria(ownerId, search, filterOptions);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                dc.addOrder(order);
            }
        }
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        if (page > 0) {
            criteria.setFirstResult((page - 1) * size);
            criteria.setMaxResults(size);
        }

        return (List<RepoItemMetadata>)criteria.list();
    }

    public static Long getTotalFileSharedByUser(DbSession session, int userId, List<Criterion> search, String filter) {
        DetachedCriteria dc = buildFileSharedByUserCriteria(userId, search, filter);
        dc.setProjection(Projections.rowCount());
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    private static DetachedCriteria buildFileSharedByUserCriteria(int userId, List<Criterion> search, String filter) {
        DetachedCriteria dc = DetachedCriteria.forClass(RepoItemMetadata.class, "r");
        DetachedCriteria nxlDc = dc.createCriteria("nxl", "n", JoinType.INNER_JOIN);
        dc.add(Restrictions.eq("n.user.id", userId));
        if (FILTER_OPTION_ACTIVE.equals(filter)) {
            LogicalExpression and = Restrictions.and(Restrictions.eq("n.shared", true), Restrictions.eq("n.status", Status.ACTIVE));
            nxlDc.add(and);
        } else if (FILTER_OPTION_ALL_SHARED.equals(filter)) {
            nxlDc.add(Restrictions.eq("n.shared", true));
        } else if (FILTER_OPTION_PROTECTED.equals(filter)) {
            nxlDc.add(Restrictions.eq("n.shared", false));
        } else if (FILTER_OPTION_DELETED.equals(filter)) {
            dc.add(Restrictions.eq("r.deleted", true));
        } else if (FILTER_OPTION_REVOKED.equals(filter)) {
            dc.add(Restrictions.eq("n.status", Status.REVOKED));
        }
        if (search != null) {
            for (Criterion criterion : search) {
                dc.add(criterion);
            }
        }
        return dc;
    }

    public static AllNxl getSharedNXL(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(AllNxl.class);
        criteria.add(Restrictions.eq("duid", duid));
        return (AllNxl)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public static List<SharingRecipientPersonal> getSharingRecipients(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        criteria.add(Restrictions.eq("id.duid", duid));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public static List<SharingRecipientPersonal> getSharingRecipients(DbSession session, int userId, AllNxl nxl) {
        int ownerId = nxl.getUser().getId();
        String duid = nxl.getDuid();
        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        criteria.add(Restrictions.eq("id.duid", duid));
        if (ownerId != userId) {
            DetachedCriteria dc = DetachedCriteria.forClass(SharingTransaction.class);
            dc.add(Restrictions.eq("nxl.duid", duid));
            dc.add(Restrictions.eq("user.id", userId));
            dc.setProjection(Projections.property("id"));
            criteria.add(Subqueries.propertyIn("transaction.id", dc));
        }
        return criteria.list();
    }

    public static void updateNxlDBProtect(int ownerId, String duid, String owner, int rights, String fileName,
        String policy) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            AllNxl nxl = new AllNxl();
            nxl.setDuid(duid);
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setStatus(Status.ACTIVE);
            nxl.setShared(false);
            if (StringUtils.hasText(policy)) {
                nxl.setPolicy(policy);
            }
            session.save(nxl);
            session.commit();
        }
    }

    /**
     * @deprecated using replaceExternalNxlDBProtect with size supported.
     * @param ownerId
     * @param repositoryId
     * @param duid
     * @param owner
     * @param rights
     * @param fileName
     * @param filePath
     */
    @Deprecated
    public static void saveOrUpdateExternalRepositoryDB(int ownerId, String repositoryId, String duid, String owner,
        int rights, String fileName, String filePath) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ExternalRepositoryNxl nxl = new ExternalRepositoryNxl();
            nxl.setDuid(duid);
            nxl.setRepository(session.load(Repository.class, repositoryId));
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setFilePath(filePath);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setStatus(ExternalRepositoryNxl.Status.ACTIVE);
            nxl.setShared(false);
            session.saveOrUpdate(nxl);
            session.commit();
        }
    }

    public static void saveOrUpdateExternalRepositoryDB(int ownerId, String repositoryId, String duid, String owner,
        int rights, String fileName, String filePath, long size) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ExternalRepositoryNxl nxl = new ExternalRepositoryNxl();
            nxl.setDuid(duid);
            nxl.setRepository(session.load(Repository.class, repositoryId));
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setFilePath(filePath);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setSize(size);
            nxl.setStatus(ExternalRepositoryNxl.Status.ACTIVE);
            nxl.setShared(false);
            session.saveOrUpdate(nxl);
            session.commit();
        }
    }

    /**
     * @deprecated using updateExternalNxlDBProtect with size supported.
     * @param ownerId
     * @param repositoryId
     * @param duid
     * @param owner
     * @param rights
     * @param fileName
     * @param filePath
     */
    @Deprecated
    public static void updateExternalNxlDBProtect(int ownerId, String repositoryId, String duid, String owner,
        int rights, String fileName, String filePath) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ExternalRepositoryNxl nxl = new ExternalRepositoryNxl();
            nxl.setDuid(duid);
            nxl.setRepository(session.load(Repository.class, repositoryId));
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setFilePath(filePath);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setStatus(ExternalRepositoryNxl.Status.ACTIVE);
            nxl.setShared(false);
            session.save(nxl);
            session.commit();
        }
    }

    public static void updateExternalNxlDBProtect(int ownerId, String repositoryId, String duid, String owner,
        int rights, String fileName, String filePath, long size) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ExternalRepositoryNxl nxl = new ExternalRepositoryNxl();
            nxl.setDuid(duid);
            nxl.setRepository(session.load(Repository.class, repositoryId));
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setFilePath(filePath);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setSize(size);
            nxl.setStatus(ExternalRepositoryNxl.Status.ACTIVE);
            nxl.setShared(false);
            session.save(nxl);
            session.commit();
        }
    }

    public static void replaceNxlDBProtect(int ownerId, String duid, String owner, int rights, String fileName,
        String policy) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            AllNxl nxl = new AllNxl();
            nxl.setDuid(duid);
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setStatus(Status.ACTIVE);
            nxl.setShared(false);
            if (StringUtils.hasText(policy)) {
                nxl.setPolicy(policy);
            }
            session.saveOrUpdate(nxl);
            session.commit();
        }
    }

    public static AllNxl lookupNxl(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(AllNxl.class);
        criteria.add(Restrictions.idEq(duid));
        return (AllNxl)criteria.uniqueResult();
    }
}
