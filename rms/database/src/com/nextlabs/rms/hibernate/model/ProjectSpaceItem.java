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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "project_space_item", uniqueConstraints = {
    @UniqueConstraint(name = "u_file_path_project", columnNames = { "project_id", "file_path" }) })
public class ProjectSpaceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String duid;
    private User user;
    private Integer permissions;
    private Project project;
    private String filePath;
    private String filePathSearchSpace;
    private String filePathDisplay;
    private Date creationTime;
    private Date expiration;
    private Date lastModified;
    private Long size;
    private boolean directory;
    private String fileParentPath;
    private User lastModifiedUser;
    private Status status = Status.ACTIVE;
    private List<SharingTransaction> transactions;

    public ProjectSpaceItem() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Column(name = "duid")
    public String getDuid() {
        return duid;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projectspace_user"))
    public User getUser() {
        return user;
    }

    @Column(name = "permissions")
    public Integer getPermissions() {
        return permissions;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projectspace_project"))
    public Project getProject() {
        return project;
    }

    @Column(name = "file_path")
    public String getFilePath() {
        return filePath;
    }

    @Column(name = "file_path_display")
    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    @Column(name = "file_path_search")
    public String getFilePathSearchSpace() {
        return filePathSearchSpace;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration")
    public Date getExpiration() {
        return expiration;
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

    @Column(name = "is_dir")
    public boolean isDirectory() {
        return directory;
    }

    @Column(name = "file_parent_path", length = 2000, nullable = true)
    public String getFileParentPath() {
        return fileParentPath;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_user_id", foreignKey = @ForeignKey(name = "fk_projectspace_last_user"))
    public User getLastModifiedUser() {
        return lastModifiedUser;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "projectNxl")
    public List<SharingTransaction> getTransactions() {
        return transactions;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPermissions(Integer permissions) {
        this.permissions = permissions;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    public void setFilePathSearchSpace(String filePathSearchSpace) {
        this.filePathSearchSpace = filePathSearchSpace;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setFileParentPath(String fileParentPath) {
        this.fileParentPath = fileParentPath;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public void setLastModifiedUser(User lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTransactions(List<SharingTransaction> transactions) {
        this.transactions = transactions;
    }

    public static enum Status {
        ACTIVE, // file not shared
        SHARED,
        REVOKED
    }
}
