package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants;

import java.io.Serializable;
import java.util.Date;

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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "sharing_transaction")
public class SharingTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private AllNxl mySpaceNxl;
    private ProjectSpaceItem projectNxl;
    private EnterpriseSpaceItem ewsNxl;
    private Constants.SHARESPACE fromSpace;
    private User user;
    private String deviceId;
    private int deviceType;
    private Date creationTime;
    private int status;
    private String comment;
    private String parentId;
    private Integer sourceProjectId;

    public SharingTransaction() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @Column(name = "device_id", nullable = true, length = 255)
    public String getDeviceId() {
        return deviceId;
    }

    @Column(name = "device_type", nullable = false)
    public int getDeviceType() {
        return deviceType;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "from_space", nullable = false)
    public Constants.SHARESPACE getFromSpace() {
        return fromSpace;
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sharing_user"))
    public User getUser() {
        return user;
    }

    @Column(name = "comments", nullable = true, length = 250)
    public String getComment() {
        return comment;
    }

    @Column(name = "parent_id", nullable = true, length = 36)
    public String getParentId() {
        return parentId;
    }

    @Column(name = "src_project_id", nullable = true)
    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "myspace_duid")
    public AllNxl getMySpaceNxl() {
        return mySpaceNxl;
    }

    public void setMySpaceNxl(AllNxl mySpaceNxl) {
        this.mySpaceNxl = mySpaceNxl;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_duid", referencedColumnName = "duid")
    public ProjectSpaceItem getProjectNxl() {
        return projectNxl;
    }

    public void setProjectNxl(ProjectSpaceItem projectNxl) {
        this.projectNxl = projectNxl;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ews_duid", referencedColumnName = "duid")
    public EnterpriseSpaceItem getEwsNxl() {
        return ewsNxl;
    }

    public void setEwsNxl(EnterpriseSpaceItem ewsNxl) {
        this.ewsNxl = ewsNxl;
    }

    public void setFromSpace(Constants.SHARESPACE fromSpace) {
        this.fromSpace = fromSpace;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }
}
