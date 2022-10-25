package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "favorite_file", uniqueConstraints = {
    @UniqueConstraint(name = "uk_fav_file_1", columnNames = {
        "repository_id", "file_path_id", "file_path" }) }, indexes = {
            @Index(name = "idx_status_last_modified", columnList = "repository_id, status, last_modified"),
            @Index(name = "idx_file_path_id_hash", columnList = "repository_id, file_path_id_hash, status"),
            @Index(name = "idx_parent_file_id_hash", columnList = "repository_id, parent_file_id_hash, status")
        })
@NamedQueries(value = {
    @NamedQuery(name = "deleteFavoriteAfterDate", query = "DELETE from FavoriteFile f where f.status=:status AND f.lastModified < :date"),
    @NamedQuery(name = "markFavoriteById", query = "UPDATE FavoriteFile f SET f.status=:status, f.lastModified = :date WHERE f.repository.id=:repoId AND f.filePathIdHash IN :idList"),
    @NamedQuery(name = "unmarkFavoriteByParentId", query = "UPDATE FavoriteFile f SET f.status=:status, f.lastModified = :date WHERE f.repository.id=:repoId AND f.parentFileIdHash=:folderIdHash"),
    @NamedQuery(name = "unmarkFavoriteById", query = "UPDATE FavoriteFile f SET f.status=:status, f.lastModified = :date WHERE f.repository.id=:repoId AND f.filePathIdHash IN :idList")
})
public class FavoriteFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private Repository repository;
    private String filePathId;
    private String filePath;
    private String filePathIdHash;
    private String parentFileIdHash;
    private String filePathSearchSpace;
    private Long fileSize;
    private Date fileLastModified;
    private Date lastModified;
    private Status status = Status.UNMARKED;

    public FavoriteFile() {
    }

    public FavoriteFile(Repository repository, String filePathId, String filePath) {
        this.repository = repository;
        this.filePathId = filePathId;
        this.filePath = filePath;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "favorite_file_id_seq")
    @SequenceGenerator(name = "favorite_file_id_seq", sequenceName = "favorite_file_id_seq")
    @Column(name = "id", nullable = false)
    public long getId() {
        return id;
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_file"))
    public Repository getRepository() {
        return repository;
    }

    @Column(name = "file_path_id", nullable = false, length = 2000)
    public String getFilePathId() {
        return filePathId;
    }

    @Column(name = "file_path_id_hash", nullable = false, length = 32)
    public String getFilePathIdHash() {
        return filePathIdHash;
    }

    @Column(name = "file_path", nullable = false, length = 2000)
    public String getFilePath() {
        return filePath;
    }

    @Column(name = "parent_file_id_hash", nullable = false, length = 32)
    public String getParentFileIdHash() {
        return parentFileIdHash;
    }

    @Column(name = "file_path_search", nullable = false, length = 255)
    public String getFilePathSearchSpace() {
        return filePathSearchSpace;
    }

    @Column(name = "file_size", nullable = true)
    public Long getFileSize() {
        return fileSize;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "file_last_modified", nullable = true)
    public Date getFileLastModified() {
        return fileLastModified;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public void setFilePathIdHash(String filePathIdHash) {
        this.filePathIdHash = filePathIdHash;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setParentFileIdHash(String parentFileIdHash) {
        this.parentFileIdHash = parentFileIdHash;
    }

    public void setFilePathSearchSpace(String filePathSearchSpace) {
        this.filePathSearchSpace = filePathSearchSpace;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileLastModified(Date fileLastModified) {
        this.fileLastModified = fileLastModified;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public static enum Status {
        UNMARKED,
        MARKED
    }

}
