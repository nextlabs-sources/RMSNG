package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "external_repository_nxl", indexes = { @Index(name = "idx_extnl_repo_user_id", columnList = "user_id"),
    @Index(name = "idx_extnl_repository_id", columnList = "repository_id") })
public class ExternalRepositoryNxl implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private String owner;
    private User user;
    private Repository repository;
    private int permissions;
    private String filePath;
    private String fileName;
    private String displayName;
    private Date creationTime;
    private Date lastModified;
    private Long size;
    private Status status = Status.ACTIVE;
    private boolean shared;

    public ExternalRepositoryNxl() {
    }

    @Id
    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "owner", length = 36)
    public String getOwner() {
        return owner;
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_extnl_repo_nxl_user"))
    public User getUser() {
        return user;
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, foreignKey = @ForeignKey(name = "fk_extnl_repo_id"))
    public Repository getRepository() {
        return repository;
    }

    @Column(name = "permissions", nullable = false)
    public int getPermissions() {
        return permissions;
    }

    @Column(name = "file_path", nullable = false)
    public String getFilePath() {
        return filePath;
    }

    @Column(name = "file_name", nullable = false)
    public String getFileName() {
        return fileName;
    }

    @Column(name = "display_name", nullable = false)
    public String getDisplayName() {
        return displayName;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    @Column(name = "size_in_bytes")
    public Long getSize() {
        return size;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Column(name = "is_shared", nullable = false)
    public boolean isShared() {
        return shared;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setShared(boolean isShared) {
        this.shared = isShared;
    }

    public static enum Status {
        ACTIVE,
        REVOKED
    }
}
