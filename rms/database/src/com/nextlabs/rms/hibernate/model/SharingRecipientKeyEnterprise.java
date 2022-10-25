package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SharingRecipientKeyEnterprise implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private String tenantId;

    public SharingRecipientKeyEnterprise() {
    }

    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "tenant_id", nullable = false, length = 36)
    public String getTenantId() {
        return tenantId;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SharingRecipientKeyEnterprise) {
            SharingRecipientKeyEnterprise oth = (SharingRecipientKeyEnterprise)obj;
            return StringUtils.equals(getDuid(), oth.getDuid()) && StringUtils.equals(getTenantId(), oth.getTenantId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(getDuid());
        hash = hash * 7 + Objects.hash(getTenantId());
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("duid:").append(duid);
        sb.append(", tenantId:").append(tenantId);
        return sb.toString();
    }
}
