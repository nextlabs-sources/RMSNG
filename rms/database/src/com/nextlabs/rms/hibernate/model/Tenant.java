package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.util.GsonUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "tenant", uniqueConstraints = { @UniqueConstraint(name = "u_tenant_name", columnNames = { "name" }) })
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String TENANT_PREF_PROJECT_ADMIN = "PROJECT_ADMIN";

    private String id;
    private String name;
    private String admin;
    private int securityMode;
    private KeyStoreEntry keystore;
    private String dnsName;
    private String displayName;
    private byte[] loginIcon;
    private String preference;
    private Date creationTime;
    private String parentId;
    private Date configurationModified;
    private String description;
    private Long ewsSizeUsed;

    public Tenant() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Column(name = "name", nullable = true, length = 250)
    public String getName() {
        return name;
    }

    @Column(name = "admin", nullable = true, length = 150)
    public String getAdmin() {
        return admin;
    }

    @Column(name = "security_mode", nullable = false)
    public int getSecurityMode() {
        return securityMode;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keystore_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant_keystore"))
    public KeyStoreEntry getKeystore() {
        return keystore;
    }

    @Column(name = "dns_name", nullable = true, length = 150)
    public String getDnsName() {
        return dnsName;
    }

    @Column(name = "display_name", nullable = true, length = 150)
    public String getDisplayName() {
        return displayName;
    }

    @Column(name = "login_icon", nullable = true)
    public byte[] getLoginIcon() {
        return loginIcon;
    }

    @Column(name = "preference", nullable = true, length = 2000)
    public String getPreference() {
        return preference;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Column(name = "parent_id", nullable = true, length = 36)
    public String getParentId() {
        return parentId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "configuration_modified", nullable = false)
    public Date getConfigurationModified() {
        return configurationModified;
    }

    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    @Column(name = "ews_size_used")
    public Long getEwsSizeUsed() {
        return ewsSizeUsed;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public void setSecurityMode(int securityMode) {
        this.securityMode = securityMode;
    }

    public void setKeystore(KeyStoreEntry keystore) {
        this.keystore = keystore;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLoginIcon(byte[] loginIcon) {
        this.loginIcon = loginIcon;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setConfigurationModified(Date classificationModified) {
        this.configurationModified = classificationModified;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEwsSizeUsed(Long ewsSizeUsed) {
        this.ewsSizeUsed = ewsSizeUsed;
    }

    public boolean isAdmin(String email) {
        if (admin == null) {
            return false;
        }
        return Arrays.asList(admin.split(",")).contains(email.toLowerCase());
    }

    public boolean isProjectAdmin(String email) {
        Map<String, Object> tenantPreferences = GsonUtils.GSON.fromJson(preference, GsonUtils.GENERIC_MAP_TYPE);
        @SuppressWarnings("unchecked")
        List<String> projectAdminList = (List<String>)tenantPreferences.get(TENANT_PREF_PROJECT_ADMIN);
        if (projectAdminList != null) {
            return projectAdminList.contains(email.toLowerCase());
        }
        return false;
    }
}
