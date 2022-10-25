package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

public class RMSEnterpriseDBSearcherImpl implements IRMSRepositorySearcher {

    private void handleException(Exception e) throws RMSRepositorySearchException {
        throw new RMSRepositorySearchException(e);
    }

    private void normalizeRepoItemMetadata(EnterpriseSpaceItem data) {
        if (data == null) {
            return;
        }

        if (StringUtils.hasText(data.getFilePathDisplay()) && !StringUtils.equals(data.getFilePathDisplay(), "/")) {
            data.setFilePathSearchSpace(StoreItemManager.getObjectName(data.getFilePathDisplay()));
        }

        if (StringUtils.hasText(data.getFilePath())) {
            data.setFilePath(data.getFilePath().toLowerCase());
        }
    }

    /**
     * @param session
     * @param tenantId
     * @param child
     * @param lastModified
     */
    private void updateParentModifiedTime(DbSession session, String tenantId, String child, Date lastModified) {
        List<String> ancestors = StoreItemManager.getAncestors(child);
        if (ancestors.isEmpty()) {
            return;
        }
        Query query = session.createQuery("UPDATE " + EnterpriseSpaceItem.class.getName() + " SET lastModified = :lastModified WHERE tenant.id=:tenantId AND filePath IN :filePaths ");
        query.setParameter("lastModified", lastModified);
        query.setParameter("tenantId", tenantId);
        query.setParameterList("filePaths", ancestors);
        query.executeUpdate();
    }

    @Override
    public void addRepoItem(StoreItem repoItem) throws RMSRepositorySearchException {

        DbSession session = DbSession.newSession();
        EnterpriseSpaceItem item = StoreItemManager.convertToEnterpriseSpace(session, repoItem);
        try {
            normalizeRepoItemMetadata(item);
            session.beginTransaction();
            if (repoItem.getEnterpriseSpaceItemId() != null) {
                item.setId(repoItem.getEnterpriseSpaceItemId());
                item.setLastModified(repoItem.getLastModified());
            }
            session.saveOrUpdate(item);
            session.commit();
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public StoreItem getRepoItem(String repoId, String filePath) throws RMSRepositorySearchException {
        if (repoId == null || filePath == null) {
            throw new NullPointerException("repoId and filePath are mandatory.");
        }
        StoreItem item = null;
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Property.forName("tenant.id").eq(repoId));
            EnterpriseSpaceItem enterpriseItemMetadata = (EnterpriseSpaceItem)criteria.uniqueResult();
            item = StoreItemManager.enterpriseItemToStoreItem(enterpriseItemMetadata);
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        }
        return item;
    }

    @Override
    public void addRepoItemList(List<RepoItemMetadata> repoItems) throws RMSRepositorySearchException {
        // TODO Auto-generated method stub

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> listRepoItems(String tenantId, String parentPath) throws RMSRepositorySearchException {
        List<StoreItem> itemList = new ArrayList<StoreItem>();
        try (DbSession session = DbSession.newSession()) {
            List<EnterpriseSpaceItem> enterpriseItemList = null;
            DetachedCriteria dc = DetachedCriteria.forClass(EnterpriseSpaceItem.class, "ews");
            dc.add(Restrictions.eq("tenant.id", tenantId));
            dc.add(Restrictions.eq("fileParentPath", parentPath));
            Criteria criteria = dc.getExecutableCriteria(session.getSession());
            enterpriseItemList = criteria.list();
            for (EnterpriseSpaceItem ewsItem : enterpriseItemList) {
                itemList.add(StoreItemManager.enterpriseItemToStoreItem(ewsItem));
            }
        }
        return itemList;
    }

    @Override
    public void deleteRepoItems(String tenantId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(tenantId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            Query query = session.createQuery("DELETE FROM EnterpriseSpaceItem where tenant.id = :tenantId AND file_path IN :filePaths");
            query.setParameter("tenantId", tenantId);
            query.setParameterList("filePaths", filePaths);
            query.executeUpdate();
            updateParentModifiedTime(session, tenantId, filePaths.get(0), new Date());
            session.commit();
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public void updateRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateOrDeleteRepoItems(String tenantId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(tenantId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }

        deleteRepoItems(tenantId, filePaths);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> search(String tenantId, String searchString) throws RMSRepositorySearchException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Property.forName("tenant.id").eq(tenantId));
            criteria.add(EscapedLikeRestrictions.like("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
            criteria.addOrder(Order.desc("directory"));
            List<EnterpriseSpaceItem> list = criteria.list();
            List<StoreItem> storeItemList = new ArrayList<>(list.size());
            for (EnterpriseSpaceItem item : list) {
                storeItemList.add(StoreItemManager.enterpriseItemToStoreItem(item));
            }
            return storeItemList;
        }
    }

    @Override
    public boolean pathExists(String tenantId, String searchPath) throws RMSRepositorySearchException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Property.forName("tenant.id").eq(tenantId));
            criteria.add(Restrictions.eq("filePath", searchPath.toLowerCase()));
            criteria.setProjection(Projections.rowCount());
            Number totalRecord = (Number)criteria.uniqueResult();
            return totalRecord != null && totalRecord.longValue() > 0L;
        }
    }

    @Override
    public String getDisplayPath(String path, String tenantId) throws RMSRepositorySearchException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Property.forName("filePath").eq(path));
            criteria.add(Property.forName("tenant.id").eq(tenantId));
            criteria.setProjection(Projections.property("filePathDisplay"));
            return (String)criteria.uniqueResult();
        }
    }

    @Override
    public Map<String, String> getDisplayPathList(List<String> pathList, String repoId)
            throws RMSRepositorySearchException {
        // TODO Auto-generated method stub
        return null;
    }

}
