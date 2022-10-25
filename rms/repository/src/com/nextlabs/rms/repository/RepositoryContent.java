package com.nextlabs.rms.repository;

import com.nextlabs.rms.entity.setting.ServiceProviderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RepositoryContent {

    private String path;

    private String name;

    private String pathId;

    private String fileId;

    private boolean isFolder;

    private boolean isRepo;

    private boolean favorited;

    private boolean fromMyVault;

    private String owner;

    private Long lastModifiedTime;

    private Long fileSize;

    private String duid;

    private String repoId;

    private String repoName;

    private ServiceProviderType repoType;

    private String fileType;

    private boolean protectedFile;

    private boolean usePathId;

    private List<RepositoryContent> children;

    private Map<String, String> customUserMetadata;

    private String projectId;

    private String tenantId;

    private boolean deleted;

    private int protectionType;

    private boolean encryptable;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public RepositoryContent() {
        isRepo = false;
        children = new ArrayList<RepositoryContent>();
        protectionType = -1;
        encryptable = true;
    }

    public boolean isRepo() {
        return isRepo;
    }

    public void setRepo(boolean isRepo) {
        this.isRepo = isRepo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public List<RepositoryContent> getChildren() {
        return children;
    }

    public void setChildren(List<RepositoryContent> children) {
        this.children = children;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public ServiceProviderType getRepoType() {
        return repoType;
    }

    public void setRepoType(ServiceProviderType repoType) {
        this.repoType = repoType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isProtectedFile() {
        return protectedFile;
    }

    public void setProtectedFile(boolean isProtectedFile) {
        this.protectedFile = isProtectedFile;
    }

    public boolean isUsePathId() {
        return usePathId;
    }

    public void setUsePathId(boolean usePathId) {
        this.usePathId = usePathId;
    }

    public Map<String, String> getCustomUserMetadata() {
        return customUserMetadata;
    }

    public void setCustomUserMetadata(Map<String, String> customUserMetadata) {
        this.customUserMetadata = customUserMetadata;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean isFavorite) {
        this.favorited = isFavorite;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isFromMyVault() {
        return fromMyVault;
    }

    public void setFromMyVault(boolean fromMyVault) {
        this.fromMyVault = fromMyVault;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public boolean isEncryptable() {
        return encryptable;
    }

    public void setEncryptable(boolean encryptable) {
        this.encryptable = encryptable;
    }

}
