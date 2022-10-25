package com.nextlabs.common.shared;

import java.util.List;

public class JsonTenantList {

    private Long totalTenants;
    private List<JsonTenant> tenantList;

    public Long getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(Long totalTenants) {
        this.totalTenants = totalTenants;
    }

    public List<JsonTenant> getTenantList() {
        return tenantList;
    }

    public void setTenantList(List<JsonTenant> tenantList) {
        this.tenantList = tenantList;
    }
}
