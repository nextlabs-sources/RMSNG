package com.nextlabs.router.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "key_store_entry", uniqueConstraints = {
    @UniqueConstraint(name = "uk_key_store_tenant", columnNames = { "tenant_name" }) })
public class KeyStoreEntry implements Serializable {

    private static final long serialVersionUID = -410484208499692479L;
    private String id;
    private String tenantName;
    private byte[] data;
    private KeyStoreType keyStoreType;
    private Date creationTime;
    private String credential;
    private Integer version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Lob
    @Column(name = "data", nullable = false)
    public byte[] getData() {
        return data;
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "key_store_type", nullable = false, length = 15)
    public KeyStoreType getKeyStoreType() {
        return keyStoreType;
    }

    @Column(name = "tenant_name", nullable = true, length = 250)
    public String getTenantName() {
        return tenantName;
    }

    @Version
    @Column(name = "version", nullable = false)
    public Integer getVersion() {
        return version;
    }

    @Column(name = "credential", nullable = false, length = 255)
    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKeyStoreType(KeyStoreType keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static enum KeyStoreType {
        BCFKS
    }
}
