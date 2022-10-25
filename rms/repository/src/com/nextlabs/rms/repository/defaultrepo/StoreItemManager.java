package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.repository.RepoConstants;

import java.util.ArrayList;
import java.util.List;

public final class StoreItemManager {

    private static final StoreItemManager MANAGER = new StoreItemManager();

    public static StoreItemManager getInstance() {
        return MANAGER;
    }

    public static EnterpriseSpaceItem convertToEnterpriseSpace(DbSession session, StoreItem storeItem) {
        EnterpriseSpaceItem item = new EnterpriseSpaceItem();
        item.setCreationTime(storeItem.getCreationTime());
        item.setDuid(storeItem.getDuid());
        item.setFilePath(storeItem.getFilePath());
        item.setPermissions(storeItem.getPermissions());
        item.setExpiration(storeItem.getExpiration());
        item.setTenant((Tenant)session.get(Tenant.class, storeItem.getTenantId()));
        item.setFilePathDisplay(storeItem.getFilePathDisplay());
        item.setFilePathSearchSpace(storeItem.getFilePathSearchSpace());
        item.setLastModified(storeItem.getLastModified());
        item.setUploader(session.get(User.class, storeItem.getUserId()));
        item.setDirectory(storeItem.isDirectory());
        item.setSize(storeItem.getSize());
        item.setFileParentPath(storeItem.getFileParentPath());
        item.setLastModifiedUser(session.get(User.class, storeItem.getLastModifiedUserId()));
        return item;

    }

    public static ProjectSpaceItem convertToProjectSpace(DbSession session, StoreItem storeItem) {
        ProjectSpaceItem item = new ProjectSpaceItem();
        item.setCreationTime(storeItem.getCreationTime());
        item.setDuid(storeItem.getDuid());
        item.setFilePath(storeItem.getFilePath());
        item.setPermissions(storeItem.getPermissions());
        item.setExpiration(storeItem.getExpiration());
        item.setProject(session.get(Project.class, storeItem.getProjectId()));
        item.setFilePathDisplay(storeItem.getFilePathDisplay());
        item.setFilePathSearchSpace(storeItem.getFilePathSearchSpace());
        item.setLastModified(storeItem.getLastModified());
        item.setUser(session.get(User.class, storeItem.getUserId()));
        item.setDirectory(storeItem.isDirectory());
        item.setSize(storeItem.getSize());
        item.setFileParentPath(storeItem.getFileParentPath());
        item.setLastModifiedUser(session.get(User.class, storeItem.getLastModifiedUserId()));
        item.setStatus(storeItem.getStatus());
        return item;

    }

    public static RepoItemMetadata convertToRepoItem(StoreItem storeItem) {
        RepoItemMetadata item = new RepoItemMetadata();
        Long id = storeItem.getId();
        if (id != null) {
            item.setId(id);
        }
        Repository repository = null;
        try (DbSession session = DbSession.newSession()) {
            repository = session.load(Repository.class, storeItem.getRepoId());
        }
        item.setDeleted(storeItem.isDeleted());
        item.setDirectory(storeItem.isDirectory());
        item.setFileParentPathHash(StringUtils.getMd5Hex(storeItem.getFileParentPath()));
        item.setFilePath(storeItem.getFilePath());
        item.setFilePathDisplay(storeItem.getFilePathDisplay());
        item.setSize(storeItem.getSize());
        item.setLastModified(storeItem.getLastModified());
        item.setCustomUserMetatdata(storeItem.getCustomUserMetatdata());
        item.setRepository(repository);
        item.setNxl(storeItem.getNxl());
        if (storeItem.getNxl() != null) {
            item.getNxl().setDuid(storeItem.getNxl().getDuid());
        }
        return item;
    }

    public static StoreItem repoItemToStoreItem(RepoItemMetadata repoItem) {
        if (repoItem == null || StringUtils.equalsIgnoreCase(repoItem.getFilePath(), RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
            return null;
        }
        StoreItem item = new StoreItem();
        item.setDeleted(repoItem.isDeleted());
        item.setDirectory(repoItem.isDirectory());
        item.setFileParentPath(StringUtils.getParentPath(repoItem.getFilePath()));
        item.setFilePath(repoItem.getFilePath());
        item.setFilePathDisplay(repoItem.getFilePathDisplay());
        item.setFilePathSearchSpace(repoItem.getFilePathSearchSpace());
        item.setSize(repoItem.getSize());
        item.setLastModified(repoItem.getLastModified());
        item.setCustomUserMetatdata(repoItem.getCustomUserMetatdata());
        item.setRepoId(repoItem.getRepository().getId());
        item.setNxl(repoItem.getNxl());
        if (repoItem.getNxl() != null) {
            item.getNxl().setDuid(repoItem.getNxl().getDuid());
            item.setDuid(repoItem.getNxl().getDuid());
            item.setUserId(repoItem.getNxl().getUser().getId());
        }

        item.setDeleted(repoItem.isDeleted());
        return item;
    }

    public static StoreItem enterpriseItemToStoreItem(EnterpriseSpaceItem enterpriseItem) {
        if (enterpriseItem == null) {
            return null;
        }
        StoreItem item = new StoreItem();
        item.setDirectory(enterpriseItem.isDirectory());
        item.setFilePath(enterpriseItem.getFilePath());
        item.setFilePathDisplay(enterpriseItem.getFilePathDisplay());
        item.setFilePathSearchSpace(enterpriseItem.getFilePathSearchSpace());
        item.setSize(enterpriseItem.getSize());
        item.setLastModified(enterpriseItem.getLastModified());
        item.setTenantId(enterpriseItem.getTenant().getId());
        item.setPermissions(enterpriseItem.getPermissions());
        item.setDuid(enterpriseItem.getDuid());
        item.setUserId(enterpriseItem.getUploader().getId());
        item.setExpiration(enterpriseItem.getExpiration());
        item.setCreationTime(enterpriseItem.getCreationTime());
        item.setFilePathSearchSpace(enterpriseItem.getFilePathSearchSpace());
        return item;
    }

    public static StoreItem projectItemToStoreItem(ProjectSpaceItem projectItem) {
        if (projectItem == null) {
            return null;
        }
        StoreItem item = new StoreItem();
        item.setDirectory(projectItem.isDirectory());
        item.setFilePath(projectItem.getFilePath());
        item.setFilePathDisplay(projectItem.getFilePathDisplay());
        item.setFilePathSearchSpace(projectItem.getFilePathSearchSpace());
        item.setSize(projectItem.getSize());
        item.setLastModified(projectItem.getLastModified());
        item.setProjectId(projectItem.getProject().getId());
        item.setPermissions(projectItem.getPermissions());
        item.setDuid(projectItem.getDuid());
        item.setUserId(projectItem.getUser().getId());
        item.setExpiration(projectItem.getExpiration());
        item.setCreationTime(projectItem.getCreationTime());
        item.setFilePathSearchSpace(projectItem.getFilePathSearchSpace());
        item.setStatus(projectItem.getStatus());
        return item;
    }

    /**
     * This method takes a S3 key and returns the corresponding file/folder Name
     * @param key
     * @return
     */
    public static String getObjectName(String key) {
        if ("/".equals(key)) {
            return "/";
        }
        String[] parts = key.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (StringUtils.hasText(parts[i])) {
                return parts[i].toLowerCase();
            }
        }
        return null;
    }

    public static List<String> getAncestors(String child) {
        if (child.endsWith("/")) {
            child = child.substring(0, child.length() - 1);
        }

        String[] nodes = child.split("/");
        List<String> ancestors = new ArrayList<>();
        StringBuffer currentPath = new StringBuffer();
        for (int i = 0; i < nodes.length - 1; i++) {
            currentPath = currentPath.append(nodes[i]).append("/");
            ancestors.add(currentPath.toString());
        }
        return ancestors;
    }

    private StoreItemManager() {
    }

}
