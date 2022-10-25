package com.nextlabs.rms.repository;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonRepoFile;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.FavoriteFile;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public final class RepositoryFileManager {

    private static final int INACTIVE_CLEAR_THRESHOLD_DAYS = 30;
    private static final long INACTIVE_CLEAR_THRESHOLD_MILLISECONDS = DateUtils.addDaysAsMilliseconds(INACTIVE_CLEAR_THRESHOLD_DAYS);

    private RepositoryFileManager() {
    }

    public static Map<String, FileSyncResult> getAllFavoriteFiles(DbSession session, String[] repoIds,
        Map<String, FileSyncResult> syncResultMap) throws RepositoryException {
        if (syncResultMap == null) {
            syncResultMap = new HashMap<>();
        }
        Criteria criteria = session.createCriteria(FavoriteFile.class);
        criteria.add(Restrictions.and(Restrictions.in("repository.id", repoIds), Restrictions.eq("status", FavoriteFile.Status.MARKED)));
        @SuppressWarnings("unchecked")
        List<FavoriteFile> files = criteria.list();
        for (FavoriteFile file : files) {
            Repository repo = file.getRepository();
            if (syncResultMap.get(repo.getId()) == null) {
                ServiceProviderType repoType = RepositoryManager.getRepoType(session, repo);
                syncResultMap.put(repo.getId(), new FileSyncResult(repo.getId(), repo.getName(), repoType));
                syncResultMap.get(repo.getId()).setFullCopy(true);
            }
            boolean isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(syncResultMap.get(repo.getId()).getRepoType());
            syncResultMap.get(repo.getId()).getMarkedFavoriteFiles().add(new JsonRepoFile(file.getFilePathId(), file.getFilePath(), isDefaultRepo && fromMyVault(file.getFilePathId())));
        }
        return syncResultMap;
    }

    /**
     * To find all the non protected files from repositories
     * 
     * @param session
     * @param repoIds
     * @param page
     * @param size
     * @param orders
     * @param searchString
     * @return
     */
    public static List<RepositoryContent> getNonProtectedFavoriteFileInList(DbSession session, String[] repoIds,
        int page,
        int size, List<Order> orders, String searchString) {

        List<Criterion> search = new ArrayList<>(1);
        if (StringUtils.hasText(searchString)) {
            search.add(EscapedLikeRestrictions.ilike("f.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }
        search.add(Restrictions.not(Restrictions.ilike("f.filePathSearchSpace", ".nxl", MatchMode.END)));

        return findFavorites(session, repoIds, page, size, orders, search);
    }

    /**
     * To find all the protected files from repositories
     * 
     * @param session
     * @param repoIds
     * @param page
     * @param size
     * @param orders
     * @param searchString
     * @return
     */
    public static List<RepositoryContent> getProtectedFavoriteFileInList(DbSession session, String[] repoIds, int page,
        int size, List<Order> orders, String searchString) {

        List<Criterion> search = new ArrayList<>(1);
        if (StringUtils.hasText(searchString)) {
            search.add(EscapedLikeRestrictions.ilike("f.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }
        search.add(Restrictions.ilike("f.filePathSearchSpace", ".nxl", MatchMode.END));

        return findFavorites(session, repoIds, page, size, orders, search);
    }

    public static List<RepositoryContent> getAllFavoriteFileInList(DbSession session, String[] repoIds, int page,
        int size, List<Order> orders, String searchString) {

        List<Criterion> search = new ArrayList<>(1);
        if (StringUtils.hasText(searchString)) {
            search.add(EscapedLikeRestrictions.ilike("f.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }

        return findFavorites(session, repoIds, page, size, orders, search);

    }

    public static List<FavoriteFile> findMyVaultFavorites(DbSession session, String repoId, String searchString) {

        List<Criterion> search = new ArrayList<>(1);
        if (StringUtils.hasText(searchString)) {
            search.add(EscapedLikeRestrictions.ilike("f.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }
        search.add(Restrictions.ilike("f.filePathSearchSpace", ".nxl", MatchMode.END));

        String[] repoIds = new String[1];
        repoIds[0] = repoId;

        DetachedCriteria dc = buildFavoriteFileListingDetachCriteria(repoIds, search);
        Criteria criteria = dc.getExecutableCriteria(session.getSession());

        @SuppressWarnings("unchecked")
        List<FavoriteFile> results = criteria.list();

        return results;
    }

    private static List<RepositoryContent> findFavorites(DbSession session, String[] repoIds, int page,
        int size, List<Order> orders, List<Criterion> search) {

        DetachedCriteria dc = buildFavoriteFileListingDetachCriteria(repoIds, search);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                dc.addOrder(order);
            }
        }

        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        criteria.setFirstResult((page - 1) * size);
        criteria.setMaxResults(size + 1);

        @SuppressWarnings("unchecked")
        List<FavoriteFile> favList = criteria.list();

        List<RepositoryContent> results = new ArrayList<RepositoryContent>();
        for (FavoriteFile favoriteFile : favList) {
            results.add(toRepositoryContent(favoriteFile));
        }
        return results;
    }

    private static DetachedCriteria buildFavoriteFileListingDetachCriteria(String[] repoIds, List<Criterion> search) {
        DetachedCriteria dc = DetachedCriteria.forClass(FavoriteFile.class, "f");
        dc.add(Restrictions.in("f.repository.id", repoIds));
        dc.add(Restrictions.eq("f.status", FavoriteFile.Status.MARKED));
        if (search != null) {
            for (Criterion criterion : search) {
                dc.add(criterion);
            }
        }
        return dc;
    }

    private static RepositoryContent toRepositoryContent(FavoriteFile favoriteFile) {
        RepositoryContent resultContent = new RepositoryContent();
        String filePathDisplay = favoriteFile.getFilePath();
        String fileName = filePathDisplay.substring(filePathDisplay.lastIndexOf('/') + 1);
        String fileType = RepositoryFileUtil.getOriginalFileExtension(fileName);
        boolean isProtected = FileUtils.getRealFileExtension(fileName).equals(Constants.NXL_FILE_EXTN);
        resultContent.setFavorited(true);
        resultContent.setFileId(favoriteFile.getFilePathId());
        resultContent.setName(fileName);
        resultContent.setPathId(favoriteFile.getFilePathId());
        resultContent.setPath(favoriteFile.getFilePath());
        resultContent.setFileSize(favoriteFile.getFileSize());
        resultContent.setFileType(fileType);
        Date fileLastModified = favoriteFile.getFileLastModified();
        if (fileLastModified != null) {
            resultContent.setLastModifiedTime(fileLastModified.getTime());
        }
        ServiceProviderType repoType = ServiceProviderType.getByOrdinal((RepositoryManager.getStorageProvider(favoriteFile.getRepository().getProviderId()).getType()));
        resultContent.setRepoId(favoriteFile.getRepository().getId());
        resultContent.setRepoType(repoType);
        resultContent.setRepoName(favoriteFile.getRepository().getName());
        resultContent.setFromMyVault(DefaultRepositoryManager.isDefaultServiceProvider(repoType) && fromMyVault(favoriteFile.getFilePathId()));
        resultContent.setProtectedFile(isProtected);
        resultContent.setFolder(false);
        if (isProtected) {
            resultContent.setProtectionType(ProtectionType.ADHOC.ordinal());
        }
        return resultContent;
    }

    public static FileSyncResult getFavoriteFiles(DbSession session, String repoId, String parentFileIdHash) {
        Repository repo = session.load(Repository.class, repoId);
        ServiceProviderType repoType = RepositoryManager.getRepoType(session, repo);
        boolean isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(repoType);
        FileSyncResult result = new FileSyncResult(repo.getId(), repo.getName(), repoType);
        Criteria criteria = session.createCriteria(FavoriteFile.class);
        criteria.add(Restrictions.and(Restrictions.eq("repository.id", repoId), Restrictions.eq("status", FavoriteFile.Status.MARKED)));
        if (parentFileIdHash != null) {
            criteria.add(Restrictions.eq("parentFileIdHash", parentFileIdHash));
        }
        @SuppressWarnings("unchecked")
        List<FavoriteFile> files = criteria.list();
        for (FavoriteFile file : files) {
            result.getMarkedFavoriteFiles().add(new JsonRepoFile(file.getFilePathId(), file.getFilePath(), isDefaultRepo && fromMyVault(file.getFilePathId())));
        }
        result.setFullCopy(true);
        return result;
    }

    public static Set<String> getSetOfFavoriteFileIds(DbSession session, String repoId, String parentFileId) {
        Set<String> favoriteFileIdSet = new HashSet<String>();
        String parentFileIdHash = StringUtils.getMd5Hex(parentFileId);
        List<JsonRepoFile> favoriteFiles = getFavoriteFiles(session, repoId, parentFileIdHash).getMarkedFavoriteFiles();
        if (favoriteFiles != null && !favoriteFiles.isEmpty()) {
            for (JsonRepoFile jsonRepoFile : favoriteFiles) {
                favoriteFileIdSet.add(jsonRepoFile.getPathId());
            }
        }
        return favoriteFileIdSet;
    }

    public static void markFilesAsFavorite(DbSession session, String repoId, List<JsonRepoFile> files) {
        Map<String, JsonRepoFile> filePathIdHashFileMap = new HashMap<>();
        Set<String> entriesToCreateSet = new HashSet<>();
        for (JsonRepoFile jsonFile : files) {
            String pathIdHash = StringUtils.getMd5Hex(jsonFile.getPathId());
            filePathIdHashFileMap.put(pathIdHash, jsonFile);
            entriesToCreateSet.add(pathIdHash);
        }
        session.beginTransaction();
        Criteria criteria = session.createCriteria(FavoriteFile.class);
        criteria.add(Restrictions.eq("repository.id", repoId));
        criteria.add(Restrictions.in("filePathIdHash", filePathIdHashFileMap.keySet()));
        @SuppressWarnings("unchecked")
        List<FavoriteFile> existingFavFiles = criteria.list();
        int count = 0;
        Date currentDate = new Date();
        for (FavoriteFile favoriteFile : existingFavFiles) {
            JsonRepoFile jsonFile = filePathIdHashFileMap.get(favoriteFile.getFilePathIdHash());
            if (StringUtils.equals(jsonFile.getPathDisplay(), favoriteFile.getFilePath())) {
                entriesToCreateSet.remove(favoriteFile.getFilePathIdHash());
                favoriteFile.setFileSize(jsonFile.getFileSize());
                if (jsonFile.getFileLastModified() != null) {
                    favoriteFile.setFileLastModified(new Date(jsonFile.getFileLastModified()));
                }
                favoriteFile.setStatus(FavoriteFile.Status.MARKED);
                favoriteFile.setLastModified(currentDate);
                session.update(favoriteFile);
                ++count;
            } else if (favoriteFile.getStatus().equals(FavoriteFile.Status.MARKED)) {
                favoriteFile.setStatus(FavoriteFile.Status.UNMARKED);
                favoriteFile.setLastModified(currentDate);
                session.update(favoriteFile);
                ++count;
            }
            if (count % 100 == 99) {
                session.flush();
                session.clear();
            }
        }
        if (!entriesToCreateSet.isEmpty()) {
            Repository repo = session.load(Repository.class, repoId);
            for (String newEntryFilePathIdHash : entriesToCreateSet) {
                JsonRepoFile newEntryJsonFile = filePathIdHashFileMap.get(newEntryFilePathIdHash);
                FavoriteFile favoriteFile = new FavoriteFile(repo, newEntryJsonFile.getPathId(), newEntryJsonFile.getPathDisplay());
                favoriteFile.setFilePathIdHash(newEntryFilePathIdHash);
                favoriteFile.setParentFileIdHash(StringUtils.getMd5Hex(newEntryJsonFile.getParentFileId()));
                favoriteFile.setFileSize(newEntryJsonFile.getFileSize());
                String[] parts = newEntryJsonFile.getPathDisplay().split("/");
                favoriteFile.setFilePathSearchSpace(parts[parts.length - 1]);
                if (newEntryJsonFile.getFileLastModified() != null) {
                    favoriteFile.setFileLastModified(new Date(newEntryJsonFile.getFileLastModified()));
                }
                favoriteFile.setLastModified(currentDate);
                favoriteFile.setStatus(FavoriteFile.Status.MARKED);
                session.save(favoriteFile);
                ++count;
                if (count % 100 == 99) {
                    session.flush();
                    session.clear();
                }
            }
        }
        session.commit();
    }

    public static void unmarkFilesAsFavorite(DbSession session, String repoId, List<JsonRepoFile> files) {
        String[] fileIdHashes = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileIdHashes[i] = StringUtils.getMd5Hex(files.get(i).getPathId());
        }
        session.beginTransaction();
        session.createNamedQuery("unmarkFavoriteById").setParameter("repoId", repoId).setParameter("status", FavoriteFile.Status.UNMARKED).setTimestamp("date", new Date()).setParameterList("idList", fileIdHashes).executeUpdate();
        session.commit();
    }

    public static void unmarkFilesUnderFolder(DbSession session, String repoId, String folderId) {
        String folderIdHash = StringUtils.getMd5Hex(folderId);
        session.beginTransaction();
        session.createNamedQuery("unmarkFavoriteByParentId").setParameter("repoId", repoId).setParameter("status", FavoriteFile.Status.UNMARKED).setTimestamp("date", new Date()).setParameter("folderIdHash", folderIdHash).executeUpdate();
        session.commit();
    }

    public static FileSyncResult getFavoriteFilesSince(DbSession session, String repoId,
        Date date) throws RepositoryException {
        Criteria criteria = session.createCriteria(FavoriteFile.class);
        List<JsonRepoFile> markedAsFavorite = new ArrayList<>();
        List<JsonRepoFile> unmarkedAsFavorite = new ArrayList<>();
        Repository repo = session.load(Repository.class, repoId);
        ServiceProviderType repoType = RepositoryManager.getRepoType(session, repo);
        boolean isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(repoType);
        FileSyncResult result = new FileSyncResult(repo.getId(), repo.getName(), repoType);
        if (date == null || new Date().getTime() - date.getTime() > INACTIVE_CLEAR_THRESHOLD_MILLISECONDS) {
            criteria.add(Restrictions.and(Restrictions.eq("repository.id", repoId), Restrictions.eq("status", FavoriteFile.Status.MARKED)));
            @SuppressWarnings("unchecked")
            List<FavoriteFile> files = criteria.list();
            for (FavoriteFile file : files) {
                markedAsFavorite.add(new JsonRepoFile(file.getFilePathId(), file.getFilePath(), isDefaultRepo && fromMyVault(file.getFilePathId())));
            }
            result.setMarkedFavoriteFiles(markedAsFavorite);
            result.setFullCopy(true);
        } else {
            criteria.add(Restrictions.and(Restrictions.eq("repository.id", repoId), Restrictions.ge("lastModified", date)));
            @SuppressWarnings("unchecked")
            List<FavoriteFile> files = criteria.list();
            for (FavoriteFile file : files) {
                if (file.getStatus().equals(FavoriteFile.Status.MARKED)) {
                    markedAsFavorite.add(new JsonRepoFile(file.getFilePathId(), file.getFilePath(), isDefaultRepo && fromMyVault(file.getFilePathId())));
                } else {
                    unmarkedAsFavorite.add(new JsonRepoFile(file.getFilePathId(), file.getFilePath(), isDefaultRepo && fromMyVault(file.getFilePathId())));
                }
            }
            result.setMarkedFavoriteFiles(markedAsFavorite);
            result.setUnmarkedFavoriteFiles(unmarkedAsFavorite);
        }
        return result;
    }

    public static void cleanupInactiveRecords() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -INACTIVE_CLEAR_THRESHOLD_DAYS);
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            session.createNamedQuery("deleteFavoriteAfterDate").setParameter("status", FavoriteFile.Status.UNMARKED).setCalendarDate("date", calendar).executeUpdate();
            session.commit();
        }
    }

    public static String getFileId(String filePathId) {
        int index = filePathId.lastIndexOf('/');
        String fileId = filePathId.substring(index + 1);
        return !StringUtils.hasText(fileId) ? "/" : fileId;
    }

    public static boolean fromMyVault(String filePathId) {
        return filePathId.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID);
    }

    public static class FileSyncResult {

        private String repoId;
        private String repoName;
        private ServiceProviderType repoType;
        private List<JsonRepoFile> markedFavoriteFiles;
        private List<JsonRepoFile> unmarkedFavoriteFiles;
        private boolean isFullCopy;

        public FileSyncResult(String repoId, String repoName, ServiceProviderType repoType) {
            this.repoId = repoId;
            this.repoName = repoName;
            this.repoType = repoType;
            this.markedFavoriteFiles = new ArrayList<>();
            this.unmarkedFavoriteFiles = new ArrayList<>();
            this.isFullCopy = false;
        }

        public boolean isFullCopy() {
            return isFullCopy;
        }

        public void setFullCopy(boolean isFullCopy) {
            this.isFullCopy = isFullCopy;
        }

        public String getRepoId() {
            return repoId;
        }

        public void setRepoId(String repoId) {
            this.repoId = repoId;
        }

        public List<JsonRepoFile> getMarkedFavoriteFiles() {
            return markedFavoriteFiles;
        }

        public void setMarkedFavoriteFiles(List<JsonRepoFile> markedFavoriteFiles) {
            this.markedFavoriteFiles = markedFavoriteFiles;
        }

        public List<JsonRepoFile> getUnmarkedFavoriteFiles() {
            return unmarkedFavoriteFiles;
        }

        public void setUnmarkedFavoriteFiles(List<JsonRepoFile> unmarkedFavoriteFiles) {
            this.unmarkedFavoriteFiles = unmarkedFavoriteFiles;
        }

        public ServiceProviderType getRepoType() {
            return repoType;
        }

        public void setRepoType(ServiceProviderType repoType) {
            this.repoType = repoType;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

    }
}
