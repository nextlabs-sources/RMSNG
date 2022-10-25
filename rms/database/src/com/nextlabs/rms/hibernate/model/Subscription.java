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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "subscription")
public class Subscription implements Serializable {

    private static final long serialVersionUID = 4295731925246987722L;
    private Long id;
    private String subscriptionId;
    private PaymentMethod paymentMethod;
    private Date billingDate;
    private Date trialPeriod;
    private Integer billingCycleLength;
    private Integer noOfBillingCycle;
    private BillingStatus billingStatus;

    @Column(name = "billing_cycle_length", nullable = false)
    public Integer getBillingCycleLength() {
        return billingCycleLength;
    }

    @Column(name = "billing_date", nullable = true)
    public Date getBillingDate() {
        return billingDate;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "billing_status", nullable = false)
    public BillingStatus getBillingStatus() {
        return billingStatus;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "subscription_id_seq")
    @SequenceGenerator(name = "subscription_id_seq", sequenceName = "subscription_id_seq")
    @Column(name = "id", nullable = false)
    public Long getId() {
        return id;
    }

    @Column(name = "no_of_billing_cycle", nullable = true)
    public Integer getNoOfBillingCycle() {
        return noOfBillingCycle;
    }

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payment_method_id", nullable = true, foreignKey = @ForeignKey(name = "fk_subscription_payment_method"))
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Column(name = "subscription_id", nullable = false, length = 100)
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Temporal(TemporalType.DATE)
    @Column(name = "trial_period", nullable = true)
    public Date getTrialPeriod() {
        return trialPeriod;
    }

    public void setBillingCycleLength(Integer billingCycleLength) {
        this.billingCycleLength = billingCycleLength;
    }

    public void setBillingDate(Date billingDate) {
        this.billingDate = billingDate;
    }

    public void setBillingStatus(BillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNoOfBillingCycle(Integer noOfBillingCycle) {
        this.noOfBillingCycle = noOfBillingCycle;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setTrialPeriod(Date trialPeriod) {
        this.trialPeriod = trialPeriod;
    }

    public static enum BillingStatus {
        PENDING,
        ACTIVE,
        PASS_DUE,
        EXPIRED,
        CANCELLED
    }
}
