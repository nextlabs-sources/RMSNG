package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "feedback")
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String type;
    private String summary;
    private String description;
    private String clientId;
    private String deviceId;
    private int deviceType;
    private int userId;
    private Date creationTime;

    public Feedback() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Column(name = "type", nullable = false, length = 250)
    public String getType() {
        return type;
    }

    @Column(name = "summary", nullable = false, length = 100)
    public String getSummary() {
        return summary;
    }

    @Column(name = "description", nullable = false, length = 2000)
    public String getDescription() {
        return description;
    }

    @Column(name = "client_id", nullable = false, length = 32)
    public String getClientId() {
        return clientId;
    }

    @Column(name = "device_id", nullable = true, length = 255)
    public String getDeviceId() {
        return deviceId;
    }

    @Column(name = "device_type", nullable = false)
    public int getDeviceType() {
        return deviceType;
    }

    @Column(name = "user_id", nullable = false)
    public int getUserId() {
        return userId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }
}
