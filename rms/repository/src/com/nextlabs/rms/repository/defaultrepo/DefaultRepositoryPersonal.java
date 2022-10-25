package com.nextlabs.rms.repository.defaultrepo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.BlackList;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.UploadUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public class DefaultRepositoryPersonal extends DefaultRepositoryTemplate {

    private static final String MYVAULT_STORAGE_USED = "my_vault_size";
    private static final String DELETE_BLACK_LIST = "DELETE " + BlackList.class.getName() + " b where b.duid = :duid";
    private static final String DELETE_SHARING_RECIPIENTS = "DELETE " + SharingRecipientPersonal.class.getName() + " s where s.id.duid = :duid";
    private static final String DELETE_SHARING_TRANSACTION = "DELETE " + SharingTransaction.class.getName() + " s where s.id in (select s2.id from " + SharingTransaction.class.getName() + " s2 join s2.mySpaceNxl sp where sp.duid = :duid)";

    public DefaultRepositoryPersonal(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        super(session, userPrincipal);
        searcher = new RMSRepositoryDBSearcherImpl();
        bucketName = repo.getSetting().getAttributes().get(ServiceProviderSetting.MYSPACE_BUCKET_NAME);
        try {
            Method m = repo.getClass().getMethod("setBucketName", String.class);
            m.invoke(repo, bucketName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public DefaultRepositoryPersonal(IRepository repo) {
        super(repo);
        searcher = new RMSRepositoryDBSearcherImpl();
        bucketName = repo.getSetting().getAttributes().get(ServiceProviderSetting.MYSPACE_BUCKET_NAME);
        try {
            Method m = repo.getClass().getMethod("setBucketName", String.class);
            m.invoke(repo, bucketName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    protected StoreItem getStoreItem() {
        return new StoreItem();
    }

    @Override
    protected void storeData(Map<String, String> customMetadata, UploadedFileMetaData uploadedFileMetaData,
        StoreItem existingFile, String parentPathId, boolean updateMyVault) {
        Date lastUpdated = uploadedFileMetaData.getLastModifiedTime();
        long fileSize = uploadedFileMetaData.getSize();
        StoreItem data = new StoreItem();
        data.setDirectory(false);
        data.setRepoId(getRepoId());
        data.setFilePath(uploadedFileMetaData.getPathId());
        data.setFilePathDisplay(uploadedFileMetaData.getPathDisplay());
        data.setFileParentPath(parentPathId);
        data.setSize(fileSize);
        if (customMetadata != null && customMetadata.get("lastModified") != null) {
            data.setLastModified(new Date(Long.parseLong(customMetadata.get("lastModified"))));
        } else {
            data.setLastModified(Nvl.nvl(lastUpdated, new Date()));
        }

        if (customMetadata != null) {
            data.setCustomUserMetatdata(customMetadata.get("myVaultDetail"));
            String duid = customMetadata.get("duid");
            if (duid != null) {
                data.setNxl(new AllNxl());
                data.getNxl().setDuid(duid);
                data.setDeleted(false);
            }
        }
        long updateSize = fileSize;
        if (existingFile != null) {
            data.setId(existingFile.getId());
            data.setProjectSpaceItemId(existingFile.getProjectSpaceItemId());
            updateSize = fileSize - existingFile.getSize();
        }

        try {
            searcher.addRepoItem(data);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error adding record ", rse);
        }

        if (updateSize != 0) {
            updateRepositorySize(updateSize, updateMyVault);
        }
    }

    @Override
    public final UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile,
        boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata) throws RepositoryException {
        return uploadFile(filePathId, filePath, localFile, overwrite, conflictFileName, customMetadata, false);
    }

    @Override
    protected RepositoryContent getRepositoryContent(StoreItem data) {
        RepositoryContent searchResult = new RepositoryContent();
        searchResult.setRepoId(getRepoId());
        searchResult.setRepoType(getRepoType());
        searchResult.setRepoName(getRepoName());
        searchResult.setUsePathId(true);
        searchResult.setFileId(data.getFilePath());
        return searchResult;
    }

    @Override
    protected void updateRepositorySize(long sizeDelta, boolean updateMyVaultSize) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();

            Repository repository = (Repository)session.getSession().get(Repository.class, getRepoId(), LockOptions.UPGRADE);
            String prefs = repository.getPreference();
            JsonObject attrs = StringUtils.hasText(prefs) ? GsonUtils.GSON.fromJson(prefs, JsonObject.class) : new JsonObject();
            long currentSize = 0L;
            if (attrs.has(STORAGE_USED)) {
                JsonElement elem = attrs.get(STORAGE_USED);
                currentSize = elem.getAsLong();
            }
            long currentMyVaultSize = 0L;
            if (attrs.has(MYVAULT_STORAGE_USED)) {
                JsonElement elem = attrs.get(MYVAULT_STORAGE_USED);
                currentMyVaultSize = elem.getAsLong();
            }
            if (updateMyVaultSize) {
                currentMyVaultSize = currentMyVaultSize + sizeDelta;
            }
            currentSize = currentSize + sizeDelta;
            attrs.addProperty(STORAGE_USED, currentSize < 0 ? 0 : currentSize);
            attrs.addProperty(MYVAULT_STORAGE_USED, currentMyVaultSize < 0 ? 0 : currentMyVaultSize);
            repository.setPreference(GsonUtils.GSON.toJson(attrs));

            session.commit();
        } catch (HibernateException | JsonSyntaxException | NumberFormatException e) {
            logger.error("Error updating storage for Repository : " + getRepoId(), e);
        }
    }

    @Override
    public void updateRepositorySize() {
        Usage usage = calculateRepoUsage();
        updateRepositorySize(usage.getSize(), usage.getMyVaultSize());
    }

    private void updateRepositorySize(long size, long myVaultSize) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Repository repository = (Repository)session.getSession().get(Repository.class, getRepoId(), LockOptions.UPGRADE);
            String prefs = repository.getPreference();
            JsonObject attrs = StringUtils.hasText(prefs) ? GsonUtils.GSON.fromJson(prefs, JsonObject.class) : new JsonObject();
            attrs.addProperty(STORAGE_USED, size);
            attrs.addProperty(MYVAULT_STORAGE_USED, myVaultSize);
            repository.setPreference(GsonUtils.GSON.toJson(attrs));
            session.commit();
        } catch (HibernateException | JsonSyntaxException e) {
            logger.error("Error updating storage for Repository : " + getRepoId(), e);
        }
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        File uploadFile = new File(localFile);
        String uniqueResourceId = null;
        try {
            uniqueResourceId = UploadUtil.getUniqueResourceId(SPACETYPE.MYSPACE, getRepoId(), filePathId, uploadFile.getName());

            StoreItem existingFile = getExistingStoreItem(filePathId + uploadFile.getName());
            if (existingFile != null && existingFile.isDeleted() && !userConfirmedFileOverwrite) {
                userConfirmedFileOverwrite = true;
            }

            if (userConfirmedFileOverwrite && !LockManager.getInstance().acquireLock(uniqueResourceId, TimeUnit.MINUTES.toMillis(5))) {
                throw new FileConflictException("File is edited by another user");
            }

            if (existingFile != null && userConfirmedFileOverwrite) {
                String repoItemId = getAllExistingSpaceItemIdWithFilePath(existingFile.getFilePath(), getRepoId());
                String existingDuid = getExistingDuidWithFilePath(existingFile.getFilePath(), getRepoId());
                if (repoItemId != null) {
                    existingFile.setId(Long.valueOf(repoItemId));
                }
                resetSharingTransactionStatusForFile(existingDuid);
            }
            if (!userConfirmedFileOverwrite && !overwrite && existingFile != null) {
                throw new FileConflictException("file in default storage would be overwritten by this upload");
            }

            String parentPathDisplay = getParentPathDisplay(filePathId);
            UploadedFileMetaData metada = repo.uploadFile(filePathId, parentPathDisplay, localFile, overwrite, conflictFileName, customMetadata);
            storeData(customMetadata, metada, existingFile, filePathId, isFromMyVault(filePathId));
            return metada;
        } catch (UnsupportedEncodingException e) {
            FileConflictException ex = new FileConflictException("Could not lock the file for updating");
            ex.initCause(e);
            throw ex;
        } finally {
            if (userConfirmedFileOverwrite && uniqueResourceId != null) {
                LockManager.getInstance().releaseRemoveLock(uniqueResourceId);
            }
        }
    }

    private boolean resetSharingTransactionStatusForFile(String existingDuid) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            session.createQuery(DELETE_SHARING_RECIPIENTS).setString("duid", existingDuid).executeUpdate();
            session.createQuery(DELETE_SHARING_TRANSACTION).setString("duid", existingDuid).executeUpdate();
            session.createQuery(DELETE_BLACK_LIST).setString("duid", existingDuid).executeUpdate();
            session.commit();
            return true;
        }
    }

    private String getExistingDuidWithFilePath(String fileName, String repoId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Restrictions.eq("filePath", fileName.toLowerCase()));
            criteria.add(Restrictions.eq("repository.id", repoId));
            RepoItemMetadata repoItemMetadata = (RepoItemMetadata)criteria.uniqueResult();
            if (repoItemMetadata == null || repoItemMetadata.getNxl() == null) {
                return null;
            }
            return String.valueOf(repoItemMetadata.getNxl().getDuid());

        }
    }

    @Override
    public String getExistingSpaceItemIdWithFilePath(String fileName, String repoId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Restrictions.eq("filePath", fileName.toLowerCase()));
            criteria.add(Restrictions.eq("repository.id", repoId));
            criteria.add(Restrictions.eq("deleted", false));
            RepoItemMetadata repoItemMetadata = (RepoItemMetadata)criteria.uniqueResult();
            if (repoItemMetadata == null) {
                return null;
            }
            return String.valueOf(repoItemMetadata.getId());

        }
    }

    private String getAllExistingSpaceItemIdWithFilePath(String fileName, String repoId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(RepoItemMetadata.class);
            criteria.add(Restrictions.eq("filePath", fileName.toLowerCase()));
            criteria.add(Restrictions.eq("repository.id", repoId));
            RepoItemMetadata repoItemMetadata = (RepoItemMetadata)criteria.uniqueResult();
            if (repoItemMetadata == null) {
                return null;
            }
            return String.valueOf(repoItemMetadata.getId());

        }
    }

    @Override
    public Long getTotalFileCount(DbSession session, String repoId, boolean isVaultCount) {
        String parentFileIdHash = StringUtils.getMd5Hex(RepoConstants.MY_VAULT_FOLDER_PATH_ID);
        Criteria criteria = session.createCriteria(RepoItemMetadata.class);
        criteria.add(Restrictions.eq("repository.id", repoId));
        criteria.add(Restrictions.eq("deleted", false));
        criteria.add(Restrictions.eq("directory", false));
        if (isVaultCount) {
            criteria.add(Restrictions.eq("fileParentPathHash", parentFileIdHash));
        } else {
            criteria.add(Restrictions.ne("fileParentPathHash", parentFileIdHash));
        }
        criteria.setProjection(Projections.rowCount());
        return (Long)criteria.uniqueResult();
    }
}
