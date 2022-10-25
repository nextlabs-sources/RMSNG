package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants.AccountType;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "activity_log", indexes = { @Index(name = "idx_duid_access_result", columnList = "duid, access_result"),
    @Index(name = "idx_duid_access_time", columnList = "duid, access_time") })
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String duid;
    private String owner;
    private int userId;
    private int operation;
    private String deviceId;
    private int deviceType;
    private String repositoryId;
    private String filePathId;
    private String fileName;
    private String filePath;
    private String appName;
    private String appPath;
    private String appPublisher;
    private Date accessTime;
    private int accessResult;
    private String activityData;
    private AccountType accountType = AccountType.PERSONAL;

    public ActivityLog() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "owner", nullable = false, length = 150)
    public String getOwner() {
        return owner;
    }

    @Column(name = "user_id", nullable = false)
    public int getUserId() {
        return userId;
    }

    @Column(name = "operation", nullable = false)
    public int getOperation() {
        return operation;
    }

    @Column(name = "device_id", nullable = true, length = 255)
    public String getDeviceId() {
        return deviceId;
    }

    @Column(name = "device_type", nullable = false)
    public int getDeviceType() {
        return deviceType;
    }

    @Column(name = "repository_id", nullable = true, length = 36)
    public String getRepositoryId() {
        return repositoryId;
    }

    @Column(name = "file_path_id", nullable = true, length = 2000)
    public String getFilePathId() {
        return filePathId;
    }

    @Column(name = "file_name", nullable = true, length = 255)
    public String getFileName() {
        return fileName;
    }

    @Column(name = "file_path", nullable = true, length = 2000)
    public String getFilePath() {
        return filePath;
    }

    @Column(name = "app_name", nullable = true, length = 150)
    public String getAppName() {
        return appName;
    }

    @Column(name = "app_path", nullable = true, length = 512)
    public String getAppPath() {
        return appPath;
    }

    @Column(name = "app_publisher", nullable = true, length = 150)
    public String getAppPublisher() {
        return appPublisher;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "access_time", nullable = false)
    public Date getAccessTime() {
        return accessTime;
    }

    @Column(name = "access_result", nullable = false)
    public int getAccessResult() {
        return accessResult;
    }

    @Column(name = "activity_data", nullable = true, length = 2000)
    public String getActivityData() {
        return activityData;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "account_type", nullable = false)
    public AccountType getAccountType() {
        return accountType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    public void setAppPublisher(String appPublisher) {
        this.appPublisher = appPublisher;
    }

    public void setAccessTime(Date accessTime) {
        this.accessTime = accessTime;
    }

    public void setAccessResult(int accessResult) {
        this.accessResult = accessResult;
    }

    public void setActivityData(String activityData) {
        this.activityData = activityData;
    }

    public void setAccountType(AccountType type) {
        this.accountType = type;
    }
}
