package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "all_nxl", indexes = { @Index(name = "idx_user_id", columnList = "user_id") })
public class AllNxl implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private String owner;
    private User user;
    private int permissions;
    private String metadata;
    private String fileName;
    private String displayName;
    private Date creationTime;
    private Date lastModified;
    private Status status = Status.ACTIVE;
    private boolean shared;
    private List<SharingTransaction> transactions;
    private String policy;

    public AllNxl() {
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
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shared_nxl_user"))
    public User getUser() {
        return user;
    }

    @Column(name = "permissions", nullable = false)
    public int getPermissions() {
        return permissions;
    }

    @Column(name = "metadata", length = 4000)
    public String getMetadata() {
        return metadata;
    }

    @Column(name = "file_name", length = 255)
    public String getFileName() {
        return fileName;
    }

    @Column(name = "display_name", length = 255)
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

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "mySpaceNxl")
    public List<SharingTransaction> getTransactions() {
        return transactions;
    }

    @Column(name = "is_shared", nullable = false)
    public boolean isShared() {
        return shared;
    }

    @Column(name = "policy")
    public String getPolicy() {
        return policy;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setShared(boolean isShared) {
        this.shared = isShared;
    }

    public void setTransactions(List<SharingTransaction> transactions) {
        this.transactions = transactions;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public static enum Status {
        ACTIVE,
        REVOKED
    }
}
