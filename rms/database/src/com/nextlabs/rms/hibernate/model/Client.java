package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "client")
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clientId;
    private String deviceId;
    private int deviceType;
    private String manufacturer;
    private String model;
    private String osVersion;
    private String appName;
    private String appVersion;
    private String pushToken;
    private int status;
    private Date creationDate;
    private Date lastModified;
    private String notes;

    public Client() {
    }

    @Id
    @Column(name = "client_id", nullable = false, length = 32)
    public String getClientId() {
        return clientId;
    }

    @Column(name = "device_id", nullable = false, length = 255)
    public String getDeviceId() {
        return deviceId;
    }

    @Column(name = "device_type", nullable = false)
    public int getDeviceType() {
        return deviceType;
    }

    @Column(name = "manufacturer", nullable = true, length = 50)
    public String getManufacturer() {
        return manufacturer;
    }

    @Column(name = "model", nullable = true, length = 32)
    public String getModel() {
        return model;
    }

    @Column(name = "os_version", nullable = true, length = 64)
    public String getOsVersion() {
        return osVersion;
    }

    @Column(name = "app_name", nullable = true, length = 150)
    public String getAppName() {
        return appName;
    }

    @Column(name = "app_version", nullable = true, length = 20)
    public String getAppVersion() {
        return appVersion;
    }

    @Column(name = "push_token", nullable = true, length = 64)
    public String getPushToken() {
        return pushToken;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false)
    public Date getCreationDate() {
        return creationDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    @Column(name = "notes", nullable = true, length = 50)
    public String getNotes() {
        return notes;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
