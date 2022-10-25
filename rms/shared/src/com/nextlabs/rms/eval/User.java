package com.nextlabs.rms.eval;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class User implements Serializable {

    private static final long serialVersionUID = 7077646156769197202L;
    private String id;
    private String ticket;
    private String clientId;
    private Integer platformId;
    private String tenantId;
    private String tenantName;
    private String displayName;
    private String email;
    private String deviceId;
    private String ipAddress;
    private Map<String, List<String>> attributes;

    private User(Builder builder) {
        this.id = builder.id;
        this.ticket = builder.ticket;
        this.clientId = builder.clientId;
        this.platformId = builder.platformId;
        this.tenantId = builder.tenantId;
        this.tenantName = builder.tenantName;
        this.displayName = builder.displayName;
        this.email = builder.email;
        this.deviceId = builder.deviceId;
        this.ipAddress = builder.ipAddress;
        this.attributes = builder.attributes;
    }

    public User(User other) {
        this.id = other.id;
        this.ticket = other.ticket;
        this.clientId = other.clientId;
        this.platformId = other.platformId;
        this.tenantId = other.tenantId;
        this.tenantName = other.tenantName;
        this.displayName = other.displayName;
        this.email = other.email;
        this.deviceId = other.deviceId;
        this.ipAddress = other.ipAddress;
        this.attributes = new HashMap<>();
        if (other.getAttributes() != null) {
            this.attributes.putAll(other.getAttributes());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof User) {
            User oth = (User)obj;
            return StringUtils.equals(getId(), oth.getId()) && StringUtils.equals(getEmail(), oth.getEmail()) && getAttributes().equals(oth.getAttributes());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getId() != null ? getId().hashCode() : 0;
        hash = 31 * hash + (getEmail() != null ? getEmail().hashCode() : 0);
        hash = 31 * hash + getAttributes().hashCode();
        return hash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (StringUtils.hasText(deviceId)) {
            try {
                this.deviceId = URLDecoder.decode(deviceId, "UTF-8");
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public static class Builder {

        private String id;
        private String ticket;
        private String clientId;
        private Integer platformId;
        private String tenantId;
        private String tenantName;
        private String displayName;
        private String email;
        private String deviceId;
        private String ipAddress;
        private Map<String, List<String>> attributes = new HashMap<String, List<String>>();

        public Builder() {
            this.attributes = new HashMap<String, List<String>>();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder ticket(String ticket) {
            this.ticket = ticket;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder platformId(Integer platformId) {
            this.platformId = platformId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder tenantName(String tenantName) {
            this.tenantName = tenantName;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder deviceId(String deviceId) {
            if (StringUtils.hasText(deviceId)) {
                try {
                    this.deviceId = URLDecoder.decode(deviceId, "UTF-8");
                } catch (UnsupportedEncodingException e) { //NOPMD
                }
            }
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder attributes(Map<String, List<String>> attributes) {
            this.attributes = attributes;
            return this;
        }

        public User build() {
            return new User(this); //NOPMD
        }
    }
}
