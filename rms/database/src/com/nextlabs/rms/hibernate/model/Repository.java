package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "repository", uniqueConstraints = { @UniqueConstraint(name = "u_repository_account", columnNames = {
    "user_id", "account_id", "account_name" }) }, indexes = {
        @Index(name = "idx_repo_user_provider", columnList = "user_id, provider_id") })
@NamedQueries(value = {
    @NamedQuery(name = "deleteUnsharedRepoByProviderId", query = "delete from Repository where providerId = :providerId and shared=0") })
public class Repository implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String providerId;
    private int userId;
    private String name;
    private int shared;
    private String accountName;
    private String accountId;
    private String token;
    private String iosToken;
    private String androidToken;
    private String preference;
    private Date creationTime;
    private Date lastUpdatedTime;
    private String state;
    private int providerClass;

    public Repository() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @Column(name = "provider_id", nullable = false, length = 36)
    public String getProviderId() {
        return providerId;
    }

    @Column(name = "user_id", nullable = false)
    public int getUserId() {
        return userId;
    }

    @Column(name = "name", nullable = true, length = 150)
    public String getName() {
        return name;
    }

    @Column(name = "shared", nullable = false)
    public int getShared() {
        return shared;
    }

    @Column(name = "account_name", nullable = true, length = 250)
    public String getAccountName() {
        return accountName;
    }

    @Column(name = "account_id", nullable = true, length = 250)
    public String getAccountId() {
        return accountId;
    }

    @Column(name = "token", nullable = true, length = 6000)
    public String getToken() {
        return token;
    }

    @Column(name = "ios_token", nullable = true, length = 2000)
    public String getIosToken() {
        return iosToken;
    }

    @Column(name = "preference", nullable = true, length = 2000)
    public String getPreference() {
        return preference;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = true)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated_time", nullable = true)
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Column(name = "android_token", nullable = true, length = 2000)
    public String getAndroidToken() {
        return androidToken;
    }

    @Column(name = "state", nullable = true, length = 1000)
    public String getState() {
        return state;
    }

    @Column(name = "class", nullable = false)
    public int getProviderClass() {
        return providerClass;
    }

    public void setAndroidToken(String androidToken) {
        this.androidToken = androidToken;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShared(int shared) {
        this.shared = shared;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setIosToken(String rmcToken) {
        this.iosToken = rmcToken;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setProviderClass(int providerClass) {
        this.providerClass = providerClass;
    }

}
