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
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "project", uniqueConstraints = {
    @UniqueConstraint(name = "u_project_name", columnNames = { "name", "parent_tenant_id" }) })
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String TENANT_PREF_PROJECT_ADMIN = "PROJECT_ADMIN";

    private int id;
    private String name;
    private Tenant parentTenant;
    private String externalId;
    private int type;
    private KeyStoreEntry keystore;
    private String displayName;
    private String description;
    private String owner;
    private Date creationTime;
    private Date lastModified;
    private String preferences;
    private Status status;
    private Subscription subscription;
    private CustomerAccount customerAccount;
    private String defaultInvitationMsg;
    private Date configurationModified;
    private String expiry;
    private String watermark;

    public Project() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "project_id_seq")
    @SequenceGenerator(name = "project_id_seq", sequenceName = "project_id_seq")
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    @Column(name = "name", nullable = false, length = 50)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_tenant"))
    public Tenant getParentTenant() {
        return parentTenant;
    }

    @Column(name = "external_id", nullable = true, length = 50)
    public String getExternalId() {
        return externalId;
    }

    @Column(name = "type", nullable = false)
    public int getType() {
        return type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keystore_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_keystore"))
    public KeyStoreEntry getKeystore() {
        return keystore;
    }

    @Column(name = "display_name", nullable = false, length = 150)
    public String getDisplayName() {
        return displayName;
    }

    @Column(name = "description", nullable = true, length = 250)
    public String getDescription() {
        return description;
    }

    @Column(name = "owner", nullable = true, length = 250)
    public String getOwner() {
        return owner;
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

    @Column(name = "preferences", nullable = true, length = 2000)
    public String getPreferences() {
        return preferences;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "subscription_id", nullable = true, foreignKey = @ForeignKey(name = "fk_project_subscription"))
    public Subscription getSubscription() {
        return subscription;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_account_id", nullable = true, foreignKey = @ForeignKey(name = "fk_project_customer_account"))
    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    @Column(name = "default_invitation_msg", length = 250)
    public String getDefaultInvitationMsg() {
        return defaultInvitationMsg;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "configuration_modified", nullable = false)
    public Date getConfigurationModified() {
        return configurationModified;
    }

    @Column(name = "expiry", nullable = true)
    public String getExpiry() {
        return expiry;
    }

    @Column(name = "watermark", length = 255, nullable = true)
    public String getWatermark() {
        return watermark;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParentTenant(Tenant tenant) {
        this.parentTenant = tenant;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setKeystore(KeyStoreEntry keystore) {
        this.keystore = keystore;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDefaultInvitationMsg(String defaultInvitationMsg) {
        this.defaultInvitationMsg = defaultInvitationMsg;
    }

    public void setConfigurationModified(Date configurationModified) {
        this.configurationModified = configurationModified;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Project) {
            Project oth = (Project)obj;
            return getId() == oth.getId();
        }
        return false;
    }

    public static enum Status {
        ACTIVE
    }
}
