package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "payment_method")
public class PaymentMethod implements Serializable {

    private static final long serialVersionUID = -812275973558564534L;
    private Long id;
    private String paymentCustomerId;
    private String token;
    private Status status;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "payment_method_id_seq")
    @SequenceGenerator(name = "payment_method_id_seq", sequenceName = "payment_method_id_seq")
    @Column(name = "id", nullable = false)
    public Long getId() {
        return id;
    }

    @Column(name = "payment_customer_id", nullable = true, length = 255)
    public String getPaymentCustomerId() {
        return paymentCustomerId;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Column(name = "token", nullable = false, length = 255)
    public String getToken() {
        return token;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPaymentCustomerId(String paymentCustomerId) {
        this.paymentCustomerId = paymentCustomerId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static enum Status {
        ACTIVE
    }
}
