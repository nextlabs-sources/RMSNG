package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants.TokenGroupType;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "membership")
public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private Project project;
    private Tenant tenant;
    private User user;
    private String externalId;
    private TokenGroupType type;
    private transient Blob keystore;
    private String preferences;
    private Date creationTime;
    private Date lastModified;
    private Status status;
    private User inviter;
    private Date invitedOn;
    private Date projectActionTime;

    public Membership() {
    }

    @Id
    @Column(name = "name", nullable = false, length = 150)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_membership_project"))
    public Project getProject() {
        return project;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_membership_tenant"))
    public Tenant getTenant() {
        return tenant;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    public User getUser() {
        return user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = true, foreignKey = @ForeignKey(name = "fk_membership_inviter"))
    public User getInviter() {
        return inviter;
    }

    @Column(name = "external_id", length = 50)
    public String getExternalId() {
        return externalId;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type", nullable = false)
    public TokenGroupType getType() {
        return type;
    }

    @Lob
    @Column(name = "keystore")
    public Blob getKeystore() {
        return keystore;
    }

    @Column(name = "preferences", length = 2000)
    public String getPreferences() {
        return preferences;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invited_on", nullable = true)
    public Date getInvitedOn() {
        return invitedOn;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "project_action_time", nullable = false)
    public Date getProjectActionTime() {
        return projectActionTime;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setType(TokenGroupType type) {
        this.type = type;
    }

    public void setKeystore(Blob keystore) {
        this.keystore = keystore;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setInviter(User inviter) {
        this.inviter = inviter;
    }

    public void setInvitedOn(Date invitedOn) {
        this.invitedOn = invitedOn;
    }

    public void setProjectActionTime(Date projectActionTime) {
        this.projectActionTime = projectActionTime;
    }

    public static enum Status {
        ACTIVE,
        REMOVED
    }
}
