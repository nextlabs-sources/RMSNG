package com.nextlabs.common.shared;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;

public class JsonUser {

    private Integer userId;
    private String ticket;
    private String tenantId;
    private String lt;
    private String ltId;
    private String tokenGroupName;

    private Long ttl;
    private String name;
    private String email;
    private JsonElement preferences;
    private Integer idpType;
    private List<JsonMembership> memberships;
    private String defaultTenant;
    private String defaultTenantUrl;
    private Map<String, List<String>> attributes;
    private Long creationTime;
    private Integer status;
    private Boolean isSuperAdmin;
    private Boolean appCertImported;

    public JsonUser() {
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPreferences(JsonElement preferences) {
        this.preferences = preferences;
    }

    public JsonElement getPreferences() {
        return preferences;
    }

    public void setMemberships(List<JsonMembership> memberships) {
        this.memberships = memberships;
    }

    public List<JsonMembership> getMemberships() {
        return memberships;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLt() {
        return lt;
    }

    public void setLt(String lt) {
        this.lt = lt;
    }

    public String getLtId() {
        return ltId;
    }

    public void setLtId(String ltId) {
        this.ltId = ltId;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }

    public Integer getIdpType() {
        return idpType;
    }

    public void setIdpType(Integer idpType) {
        this.idpType = idpType;
    }

    public String getDefaultTenant() {
        return defaultTenant;
    }

    public void setDefaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public String getDefaultTenantUrl() {
        return defaultTenantUrl;
    }

    public void setDefaultTenantUrl(String defaultTenantUrl) {
        this.defaultTenantUrl = defaultTenantUrl;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getSuperAdmin() {
        return isSuperAdmin;
    }

    public void setSuperAdmin(Boolean superAdmin) {
        isSuperAdmin = superAdmin;
    }

    public Boolean getAppCertImported() {
        return appCertImported;
    }

    public void setAppCertImported(Boolean appCertImported) {
        this.appCertImported = appCertImported;
    }
}
