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
@Table(name = "resource_lock")
public class ResourceLock implements Serializable {

    private static final long serialVersionUID = 1L;

    private String resource;
    private Date lastUpdated;
    private int status;

    public ResourceLock() {
    }

    @Id
    @Column(name = "id", nullable = false, length = 255)
    public String getResource() {
        return resource;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
