package com.nextlabs.rms.share;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonSharedWithMeFileList;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.BlackList;
import com.nextlabs.rms.hibernate.model.LoginAccount;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.SharingRecipientKeyPersonal;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.defaultrepo.StoreItemManager;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

public class SharePersonalMapper implements IShareMapper {

    private static final String UPDATE_SHARING_RECIPIENTS_STATUS_FLAG = "UPDATE " + SharingRecipientPersonal.class.getName() + " s set s.status = 1 where s.id.duid = :duid";
    private static final String UPDATE_SHARING_TRANSACTION = "UPDATE " + SharingTransaction.class.getName() + " s set s.status = 1 where s.mySpaceNxl.duid = :duid";

    @Override
    public StoreItem getStoreItemByTransaction(String transactionId) {
        try (DbSession session = DbSession.newSession()) {
            SharingTransaction tx = session.get(SharingTransaction.class, transactionId);
            return getStoreItemByDuid(session, tx.getMySpaceNxl().getDuid());
        }
    }

    private StoreItem getStoreItemByDuid(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(RepoItemMetadata.class);
        criteria.add(Restrictions.eq("nxl.duid", duid));
        criteria.setFetchMode("nxl", FetchMode.SELECT);
        RepoItemMetadata metadata = (RepoItemMetadata)criteria.uniqueResult();
        return StoreItemManager.repoItemToStoreItem(metadata);
    }

    @Override
    public List<?> getRecipientList(String duid,
        Collection<JsonSharing.JsonRecipient> recipients) {
        List<String> emails = new ArrayList<>(recipients.size());
        Locale locale = Locale.getDefault();
        recipients.forEach((recipient) -> emails.add(recipient.getEmail().toLowerCase(locale).trim()));
        try (DbSession session = DbSession.newSession()) {
            return getRecipientList(session, duid, emails);
        }
    }

    public List<SharingRecipientPersonal> getRecipientList(DbSession session, String duid, Collection<String> emails) {
        if (emails.isEmpty()) {
            return Collections.emptyList();
        }
        SharingRecipientKeyPersonal[] ids = new SharingRecipientKeyPersonal[emails.size()];
        int index = 0;
        for (String email : emails) {
            SharingRecipientKeyPersonal key = new SharingRecipientKeyPersonal();
            key.setDuid(duid);
            key.setEmail(email);
            ids[index++] = key;
        }

        Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
        criteria.add(Restrictions.in("id", ids));
        return criteria.list();
    }

    private Set<String> getUserEmails(DbSession session, User user) {
        HashSet<String> set = new HashSet<String>();
        Criteria criteria = session.createCriteria(LoginAccount.class);
        criteria.add(Restrictions.eq("userId", user.getId()));
        boolean debugMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.DEBUG, "false"));
        if (!debugMode) {
            criteria.add(Restrictions.eq("status", Constants.Status.ACTIVE.ordinal()));
        }
        List<?> list = criteria.list();
        for (Object obj : list) {
            LoginAccount account = (LoginAccount)obj;
            String email = account.getEmail();
            if (email != null) {
                set.add(email);
            }
        }
        return set;
    }

    @Override
    public String updateSharingTransaction(SharedFileDTO dto, int userId, String metadata) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            AllNxl sharedNxl = session.get(AllNxl.class, dto.getDocumentUID());
            if (sharedNxl == null) {
                sharedNxl = new AllNxl();
                sharedNxl.setDuid(dto.getDocumentUID());
                sharedNxl.setOwner(dto.getOwner());
                sharedNxl.setUser(session.load(User.class, dto.getUserId()));
                sharedNxl.setPermissions(dto.getGrantedRights());
                sharedNxl.setMetadata(metadata);

                sharedNxl.setFileName(dto.getFileName());
                sharedNxl.setShared(true);
                String filePath = dto.getFilePath();
                if (StringUtils.hasText(filePath)) {
                    String[] displayNameArr = filePath.split("/");
                    sharedNxl.setDisplayName(displayNameArr[displayNameArr.length - 1]);
                }
                sharedNxl.setCreationTime(dto.getCreatedDate());
                sharedNxl.setLastModified(dto.getUpdatedDate());
                sharedNxl.setPolicy(dto.getPolicy());
                session.save(sharedNxl);
            } else {
                if (dto.getUserId() != sharedNxl.getUser().getId()) {
                    Rights[] rights = Rights.fromInt(sharedNxl.getPermissions());
                    if (!ArrayUtils.contains(rights, Rights.SHARE)) {
                        throw new ValidateException(403, "Access denied");
                    }
                    User user = session.load(User.class, userId);
                    Set<String> emails = getUserEmails(session, user);
                    if (getRecipientList(session, dto.getDocumentUID(), emails).isEmpty()) {
                        throw new ValidateException(403, "Access denied");
                    }
                } else {
                    sharedNxl.setLastModified(dto.getUpdatedDate());
                    sharedNxl.setShared(true);
                    session.update(sharedNxl);
                }
            }

            SharingTransaction st = new SharingTransaction();
            st.setFromSpace(Constants.SHARESPACE.MYSPACE);
            st.setMySpaceNxl(sharedNxl);
            st.setUser(session.load(User.class, dto.getUserId()));
            st.setDeviceId(dto.getDeviceId());
            st.setDeviceType(DeviceType.valueOf(dto.getDeviceType()).ordinal());
            st.setCreationTime(dto.getCreatedDate());
            st.setComment(dto.getComment());
            session.save(st);
            session.flush();
            session.refresh(st);
            String transactionId = st.getId();

            Set<String> emails = (Set<String>)dto.getShareWith();

            List<SharingRecipientPersonal> list = getRecipientList(session, st.getMySpaceNxl().getDuid(), emails);
            for (SharingRecipientPersonal recipient : list) {
                recipient.setTransaction(st);
                recipient.setLastModified(st.getCreationTime());
                emails.remove(recipient.getId().getEmail());
            }

            for (String email : emails) {
                SharingRecipientPersonal recipient = new SharingRecipientPersonal();
                SharingRecipientKeyPersonal key = new SharingRecipientKeyPersonal();
                key.setDuid(st.getMySpaceNxl().getDuid());
                key.setEmail(email);
                recipient.setId(key);
                recipient.setTransaction(st);
                recipient.setLastModified(st.getCreationTime());
                session.save(recipient);
            }
            session.commit();
            return transactionId;
        }
    }

    @Override
    public SharingTransaction getFirstTransactionByDuid(String duid) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(SharingTransaction.class);
            criteria.add(Restrictions.eq("mySpaceNxl.duid", duid));
            criteria.add(Restrictions.eq("status", 0));
            criteria.addOrder(Order.asc("creationTime"));
            criteria.setFirstResult(0);
            criteria.setMaxResults(1);
            List<SharingTransaction> transactions = criteria.list();
            return transactions == null || transactions.isEmpty() ? null : transactions.get(0);
        }
    }

    @Override
    public StoreItem getStoreItemByDuid(String duid) {
        try (DbSession session = DbSession.newSession()) {
            return getStoreItemByDuid(session, duid);
        }
    }

    @Override
    public Set<?> removeRecipients(List<?> recipients) {
        try (DbSession session = DbSession.newSession()) {
            List<SharingRecipientPersonal> list = (List<SharingRecipientPersonal>)recipients;
            Set<String> removed = new HashSet<>(list != null && !list.isEmpty() ? list.size() : 0);
            session.beginTransaction();
            for (SharingRecipientPersonal recipient : list) {
                removed.add(recipient.getId().getEmail());
                session.delete(recipient);
            }
            session.commit();
            return removed;
        }
    }

    @Override
    public SharingTransaction getSharingTransaction(String transactionId) {
        try (DbSession session = DbSession.newSession()) {
            return session.get(SharingTransaction.class, transactionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonSharedWithMeFileList getSharedWithMeFiles(Integer page, Integer size, String orderBy,
        String searchString, Object userId) throws GeneralSecurityException {
        page = page != null && page.intValue() > 0 ? page : 1;
        size = size != null && size.intValue() > 0 ? size : -1;
        List<Order> orders = Collections.emptyList();
        if (StringUtils.hasText(orderBy)) {
            Map<String, String> supportedFields = new HashMap<>(4);
            supportedFields.put("sharedBy", "stu.email");
            supportedFields.put("sharedDate", "st.creationTime");
            supportedFields.put("name", "rm.filePathSearchSpace");
            supportedFields.put("size", "rm.size");
            OrderBuilder builder = new OrderBuilder(supportedFields);
            List<String> list = StringUtils.tokenize(orderBy, ",");
            for (String s : list) {
                builder.add(s);
            }
            orders = builder.build();
        }
        JsonSharedWithMeFileList result = new JsonSharedWithMeFileList();
        try (DbSession session = DbSession.newSession()) {
            User user = session.load(User.class, (Integer)userId);
            List<JsonSharedWithMeFile> fileInfo = getSharedWithMeFileList(session, page, size, orders, searchString, user);
            result.setFiles(fileInfo);
            result.setTotalFiles(getTotalFiles(session, searchString, user));
        }
        return result;
    }

    @Override
    public JsonSharedWithMeFile getSharedWithMeFile(String transactionId, RMSUserPrincipal principal,
        String recipientMembership)
            throws RMSException {
        try (DbSession session = DbSession.newSession()) {
            SharingTransaction st = session.get(SharingTransaction.class, transactionId);
            AllNxl nxl = st.getMySpaceNxl();

            Rights[] rights = Rights.fromInt(nxl.getPermissions());
            JsonExpiry validity = new JsonExpiry();
            if (StringUtils.hasText(nxl.getPolicy())) {
                FilePolicy filePolicy = GsonUtils.GSON.fromJson(nxl.getPolicy(), FilePolicy.class);
                rights = AdhocEvalAdapter.evaluate(filePolicy, true).getRights();
                validity = AdhocEvalAdapter.getFirstPolicyExpiry(filePolicy);
            }
            JsonSharedWithMeFile file = new JsonSharedWithMeFile();
            file.setDuid(nxl.getDuid());
            file.setFileName(nxl.getFileName());
            file.setFileType(RepositoryFileUtil.getOriginalFileExtension(nxl.getFileName()));
            if (st.getCreationTime() != null) {
                file.setSharedDate(st.getCreationTime().getTime());
            }
            file.setSharedBy(st.getUser().getEmail());
            file.setTransactionId(st.getId());
            file.setTransactionCode(SharedFileManager.getTransactionCode(st.getId()));
            file.setRights(Rights.toStrings(rights));
            file.setValidity(validity);
            file.setComment(StringEscapeUtils.escapeHtml4(st.getComment()));
            file.setOwner(file.getSharedBy().equalsIgnoreCase(principal.getEmail()));
            file.setProtectionType(Constants.ProtectionType.ADHOC.ordinal());
            return file;
        } catch (GeneralSecurityException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    private long getTotalFiles(DbSession session, String searchString, User user) {
        Criteria c = getSharedWithMeFilesListQuery(session, searchString, user);
        c.setProjection(Projections.rowCount());
        Number total = (Number)c.uniqueResult();
        return total.longValue();
    }

    private Criteria getSharedWithMeFilesListQuery(DbSession session, String searchString, User user) {
        DetachedCriteria dc = DetachedCriteria.forClass(SharingRecipientPersonal.class);
        dc.add(Restrictions.eq("id.email", user.getEmail()).ignoreCase());
        dc.setProjection(Projections.property("transaction.id"));
        Criteria c = session.createCriteria(RepoItemMetadata.class, "rm").createAlias("nxl", "n", JoinType.INNER_JOIN).createAlias("n.transactions", "st", JoinType.INNER_JOIN).createAlias("st.user", "stu", JoinType.INNER_JOIN);
        c.add(Subqueries.propertyIn("st.id", dc));
        c.add(Restrictions.ne("rm.deleted", true));
        c.add(Restrictions.eq("n.status", AllNxl.Status.ACTIVE));
        if (StringUtils.hasText(searchString)) {
            c.add(Restrictions.ilike("rm.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private List<JsonSharedWithMeFile> getSharedWithMeFileList(DbSession session, Integer page, Integer size,
        List<Order> orders, String searchString, User user) throws GeneralSecurityException {
        Criteria c = getSharedWithMeFilesListQuery(session, searchString, user);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                c.addOrder(order);
            }
        }
        if (size > 0) {
            c.setMaxResults(size);
            c.setFirstResult((page - 1) * size);
        }
        ProjectionList projectionList = Projections.projectionList();
        projectionList.add(Projections.property("n.id").as("duid"));
        projectionList.add(Projections.property("rm.filePathDisplay").as("filePathDisplay"));
        projectionList.add(Projections.property("rm.size").as("size"));
        projectionList.add(Projections.property("st.creationTime").as("sharedDate"));
        projectionList.add(Projections.property("stu.email").as("sharedBy"));
        projectionList.add(Projections.property("st.id").as("transactionId"));
        projectionList.add(Projections.property("n.permissions").as("permissions"));
        projectionList.add(Projections.property("st.comment").as("comment"));
        c.setProjection(projectionList);
        List<Object[]> list = c.list();
        List<JsonSharedWithMeFile> result = new ArrayList<>(list.size());
        for (Object[] item : list) {
            JsonSharedWithMeFile file = new JsonSharedWithMeFile();
            file.setDuid((String)item[0]);
            file.setFileName(FileUtils.getName((String)item[1]));
            file.setFileType(RepositoryFileUtil.getOriginalFileExtension(file.getFileName()));
            file.setSize((Long)item[2]);
            Date sharedDate = (Date)item[3];
            if (sharedDate != null) {
                file.setSharedDate(sharedDate.getTime());
            }
            file.setSharedBy((String)item[4]);
            file.setTransactionId((String)item[5]);
            file.setTransactionCode(SharedFileManager.getTransactionCode((String)item[5]));
            int permissions = (int)item[6];
            Rights[] rights = Rights.fromInt(permissions);
            file.setRights(Rights.toStrings(rights));
            if (item[7] != null) {
                file.setComment(StringEscapeUtils.escapeHtml4((String)item[7]));
            }
            if (file.getSharedBy().equalsIgnoreCase(user.getEmail())) {
                file.setOwner(true);
            } else {
                file.setOwner(false);
            }
            file.setProtectionType(Constants.ProtectionType.ADHOC.ordinal());
            result.add(file);
        }
        return result;
    }

    @Override
    public boolean revokeFile(String duid, int userId) throws FileAlreadyRevokedException {
        try (DbSession session = DbSession.newSession()) {
            AllNxl sharedNxl = session.get(AllNxl.class, duid);
            if (sharedNxl == null || sharedNxl.getUser().getId() != userId) {
                return false;
            }
            BlackList blackList = session.get(BlackList.class, duid);
            if (blackList != null) {
                throw new FileAlreadyRevokedException("File has already been revoked.");
            }
            final Date now = new Date();
            session.beginTransaction();
            blackList = new BlackList();
            blackList.setDuid(duid);
            blackList.setCreationTime(now);
            // What is this field about?
            blackList.setExpiration(sharedNxl.getCreationTime());
            blackList.setUser(session.load(User.class, userId));
            session.save(blackList);
            sharedNxl.setStatus(AllNxl.Status.REVOKED);
            sharedNxl.setLastModified(now);
            session.update(sharedNxl);
            session.createQuery(UPDATE_SHARING_TRANSACTION).setString("duid", duid).executeUpdate();
            session.createQuery(UPDATE_SHARING_RECIPIENTS_STATUS_FLAG).setString("duid", duid).executeUpdate();
            session.commit();
            return true;
        }
    }
}
