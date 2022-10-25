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
@Table(name = "storage_provider")
public class StorageProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String name;
    private int type;
    private String attributes;
    private Date creationTime;

    public StorageProvider() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @Column(name = "tenant_id", nullable = false, length = 36)
    public String getTenantId() {
        return tenantId;
    }

    @Column(name = "name", nullable = true, length = 150)
    public String getName() {
        return name;
    }

    @Column(name = "type", nullable = false)
    public int getType() {
        return type;
    }

    @Column(name = "attributes", nullable = true, length = 2000)
    public String getAttributes() {
        return attributes;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = true)
    public Date getCreationTime() {
        return creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }
}
