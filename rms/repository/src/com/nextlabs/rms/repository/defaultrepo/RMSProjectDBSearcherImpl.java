package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
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

public class RMSProjectDBSearcherImpl implements IRMSRepositorySearcher {

    private void handleException(Exception e) throws RMSRepositorySearchException {
        throw new RMSRepositorySearchException(e);
    }

    private void normalizeRepoItemMetadata(ProjectSpaceItem data) {
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

    @Override
    public void addRepoItem(StoreItem storeitem) throws RMSRepositorySearchException {

        DbSession session = DbSession.newSession();
        ProjectSpaceItem item = StoreItemManager.convertToProjectSpace(session, storeitem);
        try {
            normalizeRepoItemMetadata(item);
            session.beginTransaction();
            if (storeitem.getProjectSpaceItemId() != null) {
                item.setId(storeitem.getProjectSpaceItemId());
                item.setLastModified(storeitem.getLastModified());
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

    /**
     * @param session
     * @param projectId
     * @param child
     * @param lastModified
     */
    private void updateParentModifiedTime(DbSession session, int projectId, String child, Date lastModified) {
        List<String> ancestors = StoreItemManager.getAncestors(child);
        if (ancestors.isEmpty()) {
            return;
        }
        Query query = session.createQuery("UPDATE " + ProjectSpaceItem.class.getName() + " SET lastModified = :lastModified WHERE project.id=:projectId AND filePath IN :filePaths ");
        query.setParameter("lastModified", lastModified);
        query.setParameter("projectId", projectId);
        query.setParameterList("filePaths", ancestors);
        query.executeUpdate();
    }

    @Override
    public StoreItem getRepoItem(String projectId, String filePath) throws RMSRepositorySearchException {
        if (projectId == null || filePath == null) {
            throw new NullPointerException("repoId and filePath are mandatory.");
        }
        StoreItem item = null;
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Property.forName("project.id").eq(Integer.parseInt(projectId)));
            ProjectSpaceItem projectItemMetadata = (ProjectSpaceItem)criteria.uniqueResult();
            item = StoreItemManager.projectItemToStoreItem(projectItemMetadata);
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

    @Override
    public void deleteRepoItems(String projectId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(projectId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            int projId = Integer.parseInt(projectId);
            Query query = session.createQuery("DELETE FROM ProjectSpaceItem where project.id = :projectId AND file_path IN :filePaths");
            query.setParameter("projectId", projId);
            query.setParameterList("filePaths", filePaths);
            query.executeUpdate();
            updateParentModifiedTime(session, projId, filePaths.get(0), new Date());
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
    public void updateOrDeleteRepoItems(String projectId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(projectId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }

        deleteRepoItems(projectId, filePaths);

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> search(String projectId, String searchString) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Property.forName("project.id").eq(Integer.parseInt(projectId)));
            criteria.add(EscapedLikeRestrictions.like("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
            criteria.addOrder(Order.desc("directory"));
            List<ProjectSpaceItem> list = criteria.list();
            List<StoreItem> storeItemList = new ArrayList<>(list.size());
            for (ProjectSpaceItem item : list) {
                storeItemList.add(StoreItemManager.projectItemToStoreItem(item));
            }
            return storeItemList;
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public boolean pathExists(String projectId, String searchPath) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Property.forName("project.id").eq(Integer.parseInt(projectId)));
            criteria.add(Restrictions.eq("filePath", searchPath.toLowerCase()));
            criteria.setProjection(Projections.rowCount());
            Number totalRecord = (Number)criteria.uniqueResult();
            return totalRecord.longValue() > 0L;
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
        return false;
    }

    @Override
    public String getDisplayPath(String path, String projectId) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Property.forName("filePath").eq(path));
            criteria.add(Property.forName("project.id").eq(Integer.parseInt(projectId)));
            criteria.setProjection(Projections.property("filePathDisplay"));
            return (String)criteria.uniqueResult();
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public Map<String, String> getDisplayPathList(List<String> pathList, String repoId)
            throws RMSRepositorySearchException {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> listRepoItems(String repoId, String parentPath) throws RMSRepositorySearchException {
        List<StoreItem> itemList = new ArrayList<StoreItem>();
        try (DbSession session = DbSession.newSession()) {
            List<ProjectSpaceItem> projectItemList = null;
            DetachedCriteria dc = DetachedCriteria.forClass(ProjectSpaceItem.class, "p");
            dc.add(Restrictions.eq("project.id", Integer.valueOf(repoId)));
            dc.add(Restrictions.eq("fileParentPath", parentPath));
            Criteria criteria = dc.getExecutableCriteria(session.getSession());
            projectItemList = criteria.list();
            for (ProjectSpaceItem projItem : projectItemList) {
                itemList.add(StoreItemManager.projectItemToStoreItem(projItem));
            }
        }
        return itemList;
    }

}
