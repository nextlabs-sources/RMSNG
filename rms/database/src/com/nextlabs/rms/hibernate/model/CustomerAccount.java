package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "customer_account")
public class CustomerAccount implements Serializable {

    private static final long serialVersionUID = -6049401526649686404L;

    private Long id;
    private User user;
    private AccountType accountType;
    private Date creationTime;
    private Date lastUpdatedTime;
    private String paymentCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    public AccountType getAccountType() {
        return accountType;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "customer_account_id_seq")
    @SequenceGenerator(name = "customer_account_id_seq", sequenceName = "customer_account_id_seq")
    @Column(name = "id", nullable = false)
    public Long getId() {
        return id;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated_time", nullable = true)
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Column(name = "payment_customer_id", nullable = true, length = 255)
    public String getPaymentCustomerId() {
        return paymentCustomerId;
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_account_user"))
    public User getUser() {
        return user;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public void setPaymentCustomerId(String paymentCustomerId) {
        this.paymentCustomerId = paymentCustomerId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static enum AccountType {
        PROJECT_TRIAL,
        PROJECT,
        ENTERPRISE
    }
}
