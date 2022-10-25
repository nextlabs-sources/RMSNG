package com.nextlabs.rms.repository.defaultrepo;

import com.google.gson.JsonSyntaxException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.Tenant;
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
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

public class DefaultRepositoryEnterprise extends DefaultRepositoryTemplate {

    private String tenantId;

    protected DefaultRepositoryEnterprise(DbSession session, RMSUserPrincipal userPrincipal)
            throws InvalidDefaultRepositoryException {
        super(session, userPrincipal);
        searcher = new RMSEnterpriseDBSearcherImpl();
        bucketName = (String)repo.getSetting().getAttributes().get(ServiceProviderSetting.ENTERPRISE_BUCKET_NAME);
        try {
            Method m = repo.getClass().getMethod("setBucketName", String.class);
            m.invoke(repo, bucketName);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public DefaultRepositoryEnterprise(DbSession session, RMSUserPrincipal userPrincipal, String tenantId)
            throws InvalidDefaultRepositoryException {
        this(session, userPrincipal);
        this.tenantId = tenantId;
        repo.setRepoId(tenantId);
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

        data.setTenantId(tenantId);
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
            data.setEnterpriseSpaceItemId(existingFile.getEnterpriseSpaceItemId());
            updateSize = fileSize - existingFile.getSize();
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

    private String getExistingEnterpriseSpaceItemId(String duid, String tenantId)
            throws RepositoryException {
        DbSession session = DbSession.newSession();
        String id = null;
        try {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Restrictions.eq("duid", duid));
            criteria.add(Property.forName("tenant.id").eq(tenantId));
            EnterpriseSpaceItem enterpriseItemMetadata = (EnterpriseSpaceItem)criteria.uniqueResult();
            if (enterpriseItemMetadata != null) {
                id = enterpriseItemMetadata.getId();
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
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        File uploadFile = new File(localFile);
        StoreItem existingFile = getExistingStoreItem(filePathId + uploadFile.getName());
        if (existingFile != null && overwrite) {
            String fileDuid = customMetadata.get("duid");
            String enterpriseSpaceItemId = getExistingEnterpriseSpaceItemId(fileDuid, existingFile.getTenantId());
            if (enterpriseSpaceItemId != null) {
                existingFile.setEnterpriseSpaceItemId(enterpriseSpaceItemId);
            } else {
                throw new InvalidFileOverwriteException("The file duid is different from the original file");
            }
        } else if (existingFile != null && userConfirmedFileOverwrite) {
            String enterpriseSpaceItemId = getExistingSpaceItemIdWithFilePath(existingFile.getFilePath(), existingFile.getTenantId());
            if (enterpriseSpaceItemId != null) {
                existingFile.setEnterpriseSpaceItemId(enterpriseSpaceItemId);
            }
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
    protected StoreItem getStoreItem() {
        StoreItem data = new StoreItem();
        data.setTenantId(tenantId);
        data.setUserId(getUser().getUserId());
        return data;
    }

    @Override
    protected RepositoryContent getRepositoryContent(StoreItem data) {
        RepositoryContent searchResult = new RepositoryContent();
        searchResult.setTenantId(tenantId);
        return searchResult;
    }

    @Override
    protected void updateRepositorySize(long sizeDelta, boolean updateMyVaultSize) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();

            Tenant tenant = session.get(Tenant.class, tenantId);
            long currentSize = tenant.getEwsSizeUsed();
            tenant.setEwsSizeUsed(currentSize + sizeDelta);

            session.commit();
        } catch (HibernateException | JsonSyntaxException | NumberFormatException e) {
            logger.error("Error updating storage for Repository : " + getRepoId(), e);
        }
    }

    @Override
    public void updateRepositorySize() {
    }

    public String getExistingSpaceItemIdWithFilePath(String filePath, String repoId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(EnterpriseSpaceItem.class);
            criteria.add(Restrictions.eq("filePath", filePath.toLowerCase()));
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            EnterpriseSpaceItem enterpriseSpaceItem = (EnterpriseSpaceItem)criteria.uniqueResult();
            if (enterpriseSpaceItem == null) {
                return null;
            }
            return enterpriseSpaceItem.getId();
        }
    }

    @Override
    public Long getTotalFileCount(DbSession session, String repoId, boolean isVaultCount) {
        return null;
    }

}
