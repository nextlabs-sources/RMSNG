package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "builtin_repo_item", uniqueConstraints = { @UniqueConstraint(name = "u_file_path", columnNames = {
    "repo_id", "file_path" }) }, indexes = { @Index(name = "idx_duid", columnList = "duid"),
        @Index(name = "idx_repo_id_parent_hash", columnList = "repo_id, parent_file_path_hash") })
public class RepoItemMetadata implements Serializable {

    private static final long serialVersionUID = 3524261724596443252L;
    private long id;
    private Repository repository;
    private Date lastModified;
    private String fileParentPathHash;
    private String filePath;
    private String filePathSearchSpace;
    private String filePathDisplay;
    private boolean isDirectory;
    private long size;
    private String customUserMetatdata;
    private boolean deleted;
    private AllNxl nxl;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "builtin_repo_item_id_seq")
    @SequenceGenerator(name = "builtin_repo_item_id_seq", sequenceName = "builtin_repo_item_id_seq")
    @Column(name = "id", nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_built_in_repo"))
    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Column(name = "parent_file_path_hash", nullable = false, length = 32)
    public String getFileParentPathHash() {
        return fileParentPathHash;
    }

    public void setFileParentPathHash(String parentFilePath) {
        this.fileParentPathHash = parentFilePath;
    }

    @Column(name = "file_path", nullable = false, length = 2000)
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Column(name = "file_path_display", nullable = false, length = 2000)
    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    @Column(name = "is_dir", nullable = false)
    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    @Column(name = "size_in_bytes")
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Column(name = "file_path_search", nullable = false)
    public String getFilePathSearchSpace() {
        return filePathSearchSpace;
    }

    public void setFilePathSearchSpace(String filePathSearchSpace) {
        this.filePathSearchSpace = filePathSearchSpace;
    }

    @Column(name = "custom_metadata", length = 2000)
    public String getCustomUserMetatdata() {
        return customUserMetatdata;
    }

    public void setCustomUserMetatdata(String customUserMetatdata) {
        this.customUserMetatdata = customUserMetatdata;
    }

    @Column(name = "is_deleted", nullable = false)
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duid", foreignKey = @ForeignKey(name = "fk_built_in_nxl"))
    public AllNxl getNxl() {
        return nxl;
    }

    public void setNxl(AllNxl nxl) {
        this.nxl = nxl;
    }

}
