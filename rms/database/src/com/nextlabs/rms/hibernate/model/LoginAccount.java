package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "login_account")
public class LoginAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String loginName;
    private int type;
    private byte[] password;
    private String email;
    private User user;
    private int userId;
    private int attempt;
    private byte[] otp;
    private Date creationTime;
    private Date lastAttempt;
    private Date lastLogin;
    private int status;

    public LoginAccount() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "login_account_id_seq")
    @SequenceGenerator(name = "login_account_id_seq", sequenceName = "login_account_id_seq")
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    @Column(name = "login_name", nullable = false, length = 150)
    public String getLoginName() {
        return loginName;
    }

    @Column(name = "type", nullable = false)
    public int getType() {
        return type;
    }

    @Column(name = "password", nullable = true)
    public byte[] getPassword() {
        return password;
    }

    @Column(name = "email", nullable = false, length = 255)
    public String getEmail() {
        return email;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_login_account"))
    public User getUser() {
        return user;
    }

    @Column(name = "user_id", insertable = false, updatable = false)
    public int getUserId() {
        return userId;
    }

    @Column(name = "attempt", nullable = false)
    public int getAttempt() {
        return attempt;
    }

    @Column(name = "otp", nullable = true)
    public byte[] getOtp() {
        return otp;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_attempt", nullable = true)
    public Date getLastAttempt() {
        return lastAttempt;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login", nullable = true)
    public Date getLastLogin() {
        return lastLogin;
    }

    @Column(name = "status", nullable = false)
    public int getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public void setOtp(byte[] otp) {
        this.otp = otp;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastAttempt(Date lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
