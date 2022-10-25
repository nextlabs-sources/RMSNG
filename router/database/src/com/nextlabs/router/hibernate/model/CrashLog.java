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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "crash_log")
public class CrashLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String hash;
    private String log;
    private Date creationDate;

    public CrashLog() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "crash_log_id_seq")
    @SequenceGenerator(name = "crash_log_id_seq", sequenceName = "crash_log_id_seq")
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    @Column(name = "hash")
    public String getHash() {
        return hash;
    }

    @Column(name = "log")
    public String getLog() {
        return log;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date")
    public Date getCreationDate() {
        return creationDate;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
