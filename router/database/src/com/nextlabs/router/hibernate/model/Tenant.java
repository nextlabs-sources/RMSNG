//
// This file was generated.
// DO NOT EDIT!!!
//
package com.nextlabs.router.hibernate.model;

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
@Table(name = "tenant")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private byte[] otp;
    private byte[] hsk;
    private String server;
    private String displayName;
    private String description;
    private String email;
    private Date creationTime;

    public Tenant() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    @Column(name = "otp")
    public byte[] getOtp() {
        return otp;
    }

    @Column(name = "hsk")
    public byte[] getHsk() {
        return hsk;
    }

    @Column(name = "server")
    public String getServer() {
        return server;
    }

    @Column(name = "display_name")
    public String getDisplayName() {
        return displayName;
    }

    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    @Column(name = "email")
    public String getEmail() {
        return email;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time")
    public Date getCreationTime() {
        return creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOtp(byte[] otp) {
        this.otp = otp;
    }

    public void setHsk(byte[] hsk) {
        this.hsk = hsk;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }
}
