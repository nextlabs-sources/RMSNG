/**
 *
 */
package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.repository.RepoConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

/**
 * @author nnallagatla
 *
 */
public class RMSRepositoryDBSearcherImpl implements IRMSRepositorySearcher {

    private void normalizeRepoItemMetadata(RepoItemMetadata data) {
        if (StringUtils.hasText(data.getFilePathDisplay())) {
            data.setFilePathSearchSpace(StoreItemManager.getObjectName(data.getFilePathDisplay()));
        }

        if (StringUtils.hasText(data.getFilePath())) {
            data.setFilePath(data.getFilePath().toLowerCase());
        }
    }

    @Override
    public void addRepoItemList(List<RepoItemMetadata> repoItems) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            int count = 0;
            for (RepoItemMetadata repoItem : repoItems) {
                normalizeRepoItemMetadata(repoItem);
                session.saveOrUpdate(repoItem);
                updateParentModifiedTime(session, repoItem.getRepository().getId(), repoItem.getFilePath(), repoItem.getLastModified());
                if (count % 20 == 19) {
                    session.flush();
                    session.clear();
                }
                ++count;
            }
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
            session.beginTransaction();
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Restrictions.eq("repository.id", repoId));
            RepoItemMetadata repoItemMetadata = (RepoItemMetadata)criteria.uniqueResult();
            item = StoreItemManager.repoItemToStoreItem(repoItemMetadata);
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        }
        return item;
    }

    @Override
    public void deleteRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(repoId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            Query query = session.createQuery("DELETE FROM " + RepoItemMetadata.class.getName() + " where repository.id = :repoId AND filePath IN :filePaths");
            query.setParameter("repoId", repoId);
            query.setParameterList("filePaths", filePaths);
            query.executeUpdate();
            updateParentModifiedTime(session, repoId, filePaths.get(0), new Date());
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
        if (!StringUtils.hasText(repoId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            final Date now = new Date();
            Query query = session.createQuery("UPDATE " + RepoItemMetadata.class.getName() + " SET lastModified=:lastModified, deleted =:deleted where repository.id =:repoId AND filePath IN :filePaths");
            query.setParameter("deleted", true);
            query.setParameter("lastModified", now);
            query.setParameter("repoId", repoId);
            query.setParameterList("filePaths", filePaths);
            query.executeUpdate();
            updateParentModifiedTime(session, repoId, filePaths.get(0), now);
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
    public void updateOrDeleteRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException {
        if (!StringUtils.hasText(repoId) || filePaths == null || filePaths.isEmpty()) {
            return;
        }

        if (filePaths.get(0).startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
            updateRepoItems(repoId, filePaths);
        } else {
            deleteRepoItems(repoId, filePaths);
        }
    }

    /**
     * @param session
     * @param repoId
     * @param child
     * @param lastModified
     */
    private void updateParentModifiedTime(DbSession session, String repoId, String child, Date lastModified) {
        List<String> ancestors = StoreItemManager.getAncestors(child);
        if (ancestors.isEmpty()) {
            return;
        }
        Query query = session.createQuery("UPDATE " + RepoItemMetadata.class.getName() + " SET lastModified = :lastModified WHERE repository.id=:repoId AND filePath IN :filePaths");
        query.setParameter("lastModified", lastModified);
        query.setParameter("repoId", repoId);
        query.setParameterList("filePaths", ancestors);
        query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> search(String repoId, String searchString) throws RMSRepositorySearchException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Property.forName("repository.id").eq(repoId));
            criteria.add(EscapedLikeRestrictions.like("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
            /*
             * generating (field > 'value' OR field < 'value') instead of field != value so that the generated SQL query
             * uses the index idx_repo_id_parent_hash
             */
            String myVaultHash = StringUtils.getMd5Hex(RepoConstants.MY_VAULT_FOLDER_PATH_ID);
            SimpleExpression gt = Restrictions.gt("fileParentPathHash", myVaultHash);
            SimpleExpression lt = Restrictions.lt("fileParentPathHash", myVaultHash);
            criteria.add(Restrictions.or(gt, lt));
            List<RepoItemMetadata> list = criteria.list();
            List<StoreItem> storeItemList = new ArrayList<>(list.size());
            for (RepoItemMetadata item : list) {
                StoreItem storeItem = StoreItemManager.repoItemToStoreItem(item);
                if (storeItem != null) {
                    storeItemList.add(storeItem);
                }
            }
            return storeItemList;
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    @Override
    public boolean pathExists(String repoId, String searchPath) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Property.forName("repository.id").eq(repoId));
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

    private void handleException(Exception e) throws RMSRepositorySearchException {
        throw new RMSRepositorySearchException(e);
    }

    @Override
    public String getDisplayPath(String path, String repoId) throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Property.forName("filePath").eq(path));
            criteria.add(Property.forName("repository.id").eq(repoId));
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

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getDisplayPathList(List<String> pathList, String repoId)
            throws RMSRepositorySearchException {
        DbSession session = DbSession.newSession();
        try {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Property.forName("repository.id").eq(repoId));
            criteria.add(Restrictions.in("filePath", pathList));
            criteria.setProjection(Projections.property("filePathDisplay"));
            List<String> pathDisplayList = (List<String>)criteria.list();
            Map<String, String> pathDisplayMap = new HashMap<String, String>();
            for (String pathDisplay : pathDisplayList) {
                pathDisplayMap.put(pathDisplay.toLowerCase(), pathDisplay);
            }
            return pathDisplayMap;
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
    public void addRepoItem(StoreItem storeItem) throws RMSRepositorySearchException {
        RepoItemMetadata item = StoreItemManager.convertToRepoItem(storeItem);
        DbSession session = DbSession.newSession();
        session.beginTransaction();
        try {
            session.beginTransaction();
            normalizeRepoItemMetadata(item);
            session.saveOrUpdate(item);
            updateParentModifiedTime(session, item.getRepository().getId(), item.getFilePath(), item.getLastModified());
            session.commit();
        } catch (HibernateException he) {
            handleException(he);
        } catch (Exception e) {
            handleException(e);
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StoreItem> listRepoItems(String repoId, String parentPath) throws RMSRepositorySearchException {
        List<StoreItem> itemList = new ArrayList<StoreItem>();
        try (DbSession session = DbSession.newSession()) {
            List<RepoItemMetadata> repoItemList = null;
            DetachedCriteria dc = DetachedCriteria.forClass(RepoItemMetadata.class, "r");
            dc.add(Restrictions.eq("repository.id", repoId));
            dc.add(Restrictions.eq("fileParentPathHash", StringUtils.getMd5Hex(parentPath)));
            Criteria criteria = dc.getExecutableCriteria(session.getSession());
            repoItemList = criteria.list();
            for (RepoItemMetadata repoItem : repoItemList) {
                itemList.add(StoreItemManager.repoItemToStoreItem(repoItem));
            }
        }
        return itemList;
    }

}
