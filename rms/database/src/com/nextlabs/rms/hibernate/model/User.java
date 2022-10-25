package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String displayName;
    private String email;
    private int attempt;
    private Date creationTime;
    private int status;
    private Type type;

    public User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_id_seq")
    @SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq")
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    @Column(name = "display_name", nullable = true, length = 150)
    public String getDisplayName() {
        return displayName;
    }

    @Column(name = "email", nullable = true, length = 255)
    public String getEmail() {
        return email;
    }

    @Column(name = "attempt", nullable = false)
    public int getAttempt() {
        return attempt;
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
    @Column(name = "user_type", nullable = true)
    public Type getType() {
        return type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public static enum Status {
        ACTIVE,
        INACTIVE
    }

    public static enum Type {
        SYSTEM
    }
}
