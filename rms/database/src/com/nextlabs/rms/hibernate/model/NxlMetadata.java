package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants.ProtectionType;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "nxl_metadata")
public class NxlMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private String owner;
    private String filePolicyChecksum;
    private String fileTagsChecksum;
    private ProtectionType protectionType;
    private String tokenGroupName;
    private Status status;
    private String otp;
    private Date creationTime;
    private Date lastModified;

    public NxlMetadata() {
    }

    @Id
    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "owner", nullable = false, length = 150)
    public String getOwner() {
        return owner;
    }

    @Column(name = "file_policy_checksum", nullable = false, length = 256)
    public String getFilePolicyChecksum() {
        return filePolicyChecksum;
    }

    @Column(name = "file_tags_checksum", nullable = false, length = 256)
    public String getFileTagsChecksum() {
        return fileTagsChecksum;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "protection_type", nullable = false)
    public ProtectionType getProtectionType() {
        return protectionType;
    }

    @Column(name = "token_group_name", length = 150)
    public String getTokenGroupName() {
        return tokenGroupName;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Column(name = "otp")
    public String getOtp() {
        return otp;
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

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setFilePolicyChecksum(String filePolicyChecksum) {
        this.filePolicyChecksum = filePolicyChecksum;
    }

    public void setFileTagsChecksum(String fileTagsChecksum) {
        this.fileTagsChecksum = fileTagsChecksum;
    }

    public void setProtectionType(ProtectionType protectionType) {
        this.protectionType = protectionType;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public static enum Status {
        INACTIVE,
        ACTIVE,
        REVOKED
    }
}
