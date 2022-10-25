package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "sharing_recipient_personal", indexes = { @Index(name = "idx_duid_share_recpt", columnList = "duid") })
public class SharingRecipientPersonal implements Serializable {

    private static final long serialVersionUID = 1L;

    private SharingRecipientKeyPersonal id;
    private SharingTransaction transaction;
    private Date lastModified;
    private int status;

    public SharingRecipientPersonal() {
    }

    @EmbeddedId
    public SharingRecipientKeyPersonal getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recipient_transaction"))
    public SharingTransaction getTransaction() {
        return transaction;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    public void setId(SharingRecipientKeyPersonal id) {
        this.id = id;
    }

    public void setTransaction(SharingTransaction transaction) {
        this.transaction = transaction;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
