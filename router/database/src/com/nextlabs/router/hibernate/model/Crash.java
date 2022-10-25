//
// This file was generated.
// DO NOT EDIT!!!
//
package com.nextlabs.router.hibernate.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "crash")
public class Crash implements Serializable {

    private static final long serialVersionUID = 1L;

    private String hash;
    private String clientId;
    private String stacktrace;

    public Crash() {
    }

    @Id
    @Column(name = "hash", nullable = false)
    public String getHash() {
        return hash;
    }

    @Column(name = "client_id")
    public String getClientId() {
        return clientId;
    }

    @Column(name = "stacktrace")
    public String getStacktrace() {
        return stacktrace;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }
}
