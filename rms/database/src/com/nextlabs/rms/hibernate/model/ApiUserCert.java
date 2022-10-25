package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "api_user_cert", uniqueConstraints = {
    @UniqueConstraint(name = "u_api_user_cert_alias", columnNames = { "api_user_id", "cert_alias" }) })
public class ApiUserCert implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private Date creationTime;
    private Date lastModified;
    private byte[] data;
    private User apiUser;
    private String certAlias;

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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "api_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_user_id"))
    public User getApiUser() {
        return apiUser;
    }

    @Column(name = "cert_alias", nullable = true, length = 250)
    public String getCertAlias() {
        return certAlias;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setApiUser(User user) {
        this.apiUser = user;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }
}
