package com.nextlabs.rms.auth;

import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.util.Hex;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;

public class RMSUserPrincipal {

    private int userId;
    private String domain;
    private String tenantId;
    private String tenantName;
    private String ticket;
    private String name;
    private String email;
    private long ttl;
    private boolean admin;
    private String clientId;
    private Integer platformId;
    private String deviceId;
    private DeviceType deviceType;
    private String loginTenant;
    private String ipAddress;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public RMSUserPrincipal() {
    }

    public RMSUserPrincipal(int userId, String tenantId, String ticket, String clientId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.ticket = ticket;
        this.clientId = clientId;
    }

    public RMSUserPrincipal(UserSession us, Tenant tenant) {
        User user = us.getUser();
        this.userId = user.getId();
        this.tenantId = tenant.getId();
        this.clientId = us.getClientId();
        this.platformId = us.getDeviceType();
        this.tenantName = tenant.getName();
        this.ticket = Hex.toHexString(us.getTicket());
        this.name = user.getDisplayName();
        this.email = user.getEmail();
        this.ttl = us.getTtl();
        this.admin = tenant.isAdmin(user.getEmail());
        this.loginTenant = us.getLoginTenant();
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return ticket;
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

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getTtl() {
        return ttl;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getLoginTenant() {
        return loginTenant;
    }

    public void setLoginTenant(String loginTenant) {
        this.loginTenant = loginTenant;
    }
}
