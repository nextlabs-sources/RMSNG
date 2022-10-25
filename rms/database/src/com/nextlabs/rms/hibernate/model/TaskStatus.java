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
@Table(name = "task_status")
public class TaskStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String resource;
    private Date lastSuccessfulUpdate;
    private Date lastFailedUpdate;
    private int status;

    public TaskStatus() {
    }

    @Id
    @Column(name = "id", nullable = false, length = 255)
    public String getResource() {
        return resource;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_successful_update", nullable = false)
    public Date getLastSuccessfulUpdate() {
        return lastSuccessfulUpdate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_failed_update", nullable = false)
    public Date getLastFailedUpdate() {
        return lastFailedUpdate;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setLastSuccessfulUpdate(Date lastUpdated) {
        this.lastSuccessfulUpdate = lastUpdated;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setLastFailedUpdate(Date lastfailedUpdate) {
        this.lastFailedUpdate = lastfailedUpdate;
    }

}
