package com.nextlabs.rms.repository.defaultrepo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.SharingRecipientProject;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.InvalidFileOverwriteException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.Nvl;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

public class DefaultRepositoryProject extends DefaultRepositoryTemplate {

    private int projectId;

    private static final String DELETE_SHARING_RECIPIENTS = "DELETE " + SharingRecipientProject.class.getName() + " s where s.id.duid = :duid";
    private static final String DELETE_SHARING_TRANSACTION = "DELETE " + SharingTransaction.class.getName() + " s where s.id in (select s2.id from " + SharingTransaction.class.getName() + " s2 join s2.projectNxl sp where sp.duid = :duid)";

    protected DefaultRepositoryProject(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        super(session, userPrincipal);
        searcher = new RMSProjectDBSearcherImpl();
        bucketName = (String)repo.getSetting().getAttributes().get(ServiceProviderSetting.PROJECT_BUCKET_NAME);
        try {
            Method m = repo.getClass().getMethod("setBucketName", String.class);
            m.invoke(repo, bucketName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public DefaultRepositoryProject(DbSession session, RMSUserPrincipal userPrincipal, int projectId)
            throws InvalidDefaultRepositoryException {
        this(session, userPrincipal);
        this.projectId = projectId;
        repo.setRepoId(String.valueOf(projectId));
    }

    @Override
    protected StoreItem getStoreItem() {
        StoreItem data = new StoreItem();
        data.setProjectId(Integer.parseInt(getRepoId()));
        data.setUserId(getUser().getUserId());
        return data;
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

        data.setProjectId(Integer.parseInt(getRepoId()));
        int userId = Integer.parseInt(customMetadata.get("createdBy"));
        data.setUserId(userId);
        if (customMetadata.get("lastModified") != null) {
            data.setLastModifiedUserId(Integer.parseInt(customMetadata.get("lastModifiedBy")));
        } else {
            data.setLastModifiedUserId(userId);
        }
        data.setCreationTime(new Date(Long.parseLong(customMetadata.get("creationTime"))));
        data.setDuid(customMetadata.get("duid"));
        data.setPermissions(Integer.parseInt(customMetadata.get("rights")));
        long updateSize = fileSize;
        if (existingFile != null) {
            data.setId(existingFile.getId());
            data.setProjectSpaceItemId(existingFile.getProjectSpaceItemId());
            updateSize = fileSize - existingFile.getSize();
            data.setStatus(existingFile.getStatus());
        }

        try {
            searcher.addRepoItem(data);
        } catch (RMSRepositorySearchException rse) {
            logger.error("Error adding record ", rse);
        }

        if (updateSize != 0) {
            updateRepositorySize(updateSize, false);
        }
    }

    public int getProjectId() {
        return projectId;
    }

    private String getExistingProjectSpaceItemId(String duid, int projectId) throws RepositoryException {
        DbSession session = DbSession.newSession();
        String id = null;
        try {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("duid", duid));
            criteria.add(Property.forName("project.id").eq(projectId));
            ProjectSpaceItem projectItemMetadata = (ProjectSpaceItem)criteria.uniqueResult();
            if (projectItemMetadata != null) {
                id = projectItemMetadata.getId();
            }
        } catch (HibernateException he) {
            handleRepoException(he);
        } catch (Exception e) {
            handleRepoException(e);
        } finally {
            session.close();
        }
        return id;
    }

    @Override
    public final UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile,
        boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata) throws RepositoryException {
        return uploadFile(filePathId, filePath, localFile, overwrite, conflictFileName, customMetadata, false);
    }

    @Override
    public final UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile,
        boolean overwrite, String conflictFileName, Map<String, String> customMetadata,
        boolean userConfirmedFileOverwrite) throws RepositoryException {
        File uploadFile = new File(localFile);
        StoreItem existingFile = getExistingStoreItem(filePathId + uploadFile.getName());
        if (existingFile != null && overwrite) {
            String fileDuid = customMetadata.get("duid");
            String projectSpaceItemId = getExistingProjectSpaceItemId(fileDuid, existingFile.getProjectId());
            if (projectSpaceItemId != null) {
                existingFile.setProjectSpaceItemId(projectSpaceItemId);
            } else {
                throw new InvalidFileOverwriteException("The file duid is different from the original file");
            }

        } else if (existingFile != null && userConfirmedFileOverwrite) {
            String projectSpaceItemId = getExistingSpaceItemIdWithFilePath(existingFile.getFilePath(), String.valueOf(existingFile.getProjectId()));
            String existingDuid = getExistingDuidWithFilePath(existingFile.getFilePath(), String.valueOf(existingFile.getProjectId()));
            if (projectSpaceItemId != null) {
                existingFile.setProjectSpaceItemId(projectSpaceItemId);
            }
            resetSharingTransactionStatusForFile(existingDuid);
            existingFile.setStatus(ProjectSpaceItem.Status.ACTIVE);
        }
        if (!userConfirmedFileOverwrite && !overwrite && existingFile != null) {
            throw new FileConflictException("file in default storage would be overwritten by this upload");
        }
        String parentPathDisplay = getParentPathDisplay(filePathId);
        UploadedFileMetaData metada = repo.uploadFile(filePathId, parentPathDisplay, localFile, overwrite, conflictFileName, customMetadata);
        storeData(customMetadata, metada, existingFile, filePathId, false);
        return metada;
    }

    @Override
    protected RepositoryContent getRepositoryContent(StoreItem data) {
        RepositoryContent searchResult = new RepositoryContent();
        searchResult.setProjectId(getRepoId());
        return searchResult;
    }

    @Override
    protected void updateRepositorySize(long sizeDelta, boolean updateMyVaultSize) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();

            Project project = (Project)session.getSession().get(Project.class, Integer.parseInt(getRepoId()), LockOptions.UPGRADE);
            String prefs = project.getPreferences();
            JsonObject attrs = StringUtils.hasText(prefs) ? GsonUtils.GSON.fromJson(prefs, JsonObject.class) : new JsonObject();
            long currentSize = 0L;
            if (attrs.has(STORAGE_USED)) {
                JsonElement elem = attrs.get(STORAGE_USED);
                currentSize = elem.getAsLong();
            }
            currentSize = currentSize + sizeDelta;
            attrs.addProperty(STORAGE_USED, currentSize < 0 ? 0 : currentSize);
            project.setPreferences(GsonUtils.GSON.toJson(attrs));

            session.commit();
        } catch (HibernateException | JsonSyntaxException | NumberFormatException e) {
            logger.error("Error updating storage for Repository : " + getRepoId(), e);
        }
    }

    @Override
    public void updateRepositorySize() {
    }

    private boolean resetSharingTransactionStatusForFile(String existingDuid) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("duid", existingDuid));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item == null) {
                return false;
            }
            final Date now = new Date();
            item.setStatus(ProjectSpaceItem.Status.ACTIVE);
            item.setLastModified(now);
            session.update(item);

            session.createQuery(DELETE_SHARING_RECIPIENTS).setString("duid", existingDuid).executeUpdate();
            session.createQuery(DELETE_SHARING_TRANSACTION).setString("duid", existingDuid).executeUpdate();
            session.commit();
            return true;
        }
    }

    private String getExistingDuidWithFilePath(String filePath, String projectId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Restrictions.eq("project.id", Integer.valueOf(projectId)));
            ProjectSpaceItem projectItemMetadata = (ProjectSpaceItem)criteria.uniqueResult();
            if (projectItemMetadata == null) {
                return null;
            }
            return projectItemMetadata.getDuid();
        }
    }

    public String getExistingSpaceItemIdWithFilePath(String filePath, String projectId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Restrictions.eq("project.id", Integer.valueOf(projectId)));
            ProjectSpaceItem projectItemMetadata = (ProjectSpaceItem)criteria.uniqueResult();
            if (projectItemMetadata == null) {
                return null;
            }
            return projectItemMetadata.getId();
        }
    }

    @Override
    public Long getTotalFileCount(DbSession session, String repoId, boolean isVaultCount) {
        return null;
    }
}
