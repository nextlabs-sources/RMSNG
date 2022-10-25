package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants.LoginType;

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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "user_session", indexes = {
    @Index(name = "idx_user_session_1", columnList = "user_id, status, client_id, device_type") })
@NamedQueries(value = {
    @NamedQuery(name = "deleteByUser", query = "delete from UserSession us where us.user.id in (select id from User u where u.id = :userId and (u.type is null or u.type <> :userType))"),
    @NamedQuery(name = "remove.inactive.session", query = "delete from UserSession where expirationTime < :expirationTime or status <> :status"),
    @NamedQuery(name = "revoke.session", query = "update UserSession us set us.status=:status where us.loginTenant = :tenantId and us.user.id in (select id from User u where u.type is null or u.type <> :userType)") })
public class UserSession {

    private Long id;
    private String clientId;
    private User user;
    private byte[] ticket;
    private long ttl;
    private Date creationTime;
    private Date expirationTime;
    private LoginType loginType;
    private Integer deviceType;
    private Status status;
    private String loginTenant;

    @Column(name = "client_id", nullable = false, length = 32)
    public String getClientId() {
        return clientId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Column(name = "device_type", nullable = false)
    public Integer getDeviceType() {
        return deviceType;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration_time", nullable = false)
    public Date getExpirationTime() {
        return expirationTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_session_id_seq")
    @SequenceGenerator(name = "user_session_id_seq", sequenceName = "user_session_id_seq")
    @Column(name = "id", nullable = false)
    public Long getId() {
        return id;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "login_type", nullable = false)
    public LoginType getLoginType() {
        return loginType;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Column(name = "ticket", nullable = false)
    public byte[] getTicket() {
        return ticket;
    }

    @Column(name = "ttl", nullable = false)
    public long getTtl() {
        return ttl;
    }

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_session_user"))
    public User getUser() {
        return user;
    }

    @Column(name = "login_tenant", nullable = false)
    public String getLoginTenant() {
        return loginTenant;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public void setLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTicket(byte[] ticket) {
        this.ticket = ticket;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setLoginTenant(String loginTenant) {
        this.loginTenant = loginTenant;
    }

    public enum Status {
        ACTIVE,
        EXPIRED,
        REVOKED
    }
}
