package com.nextlabs.common.shared;

public class JsonMembership {

    private String id;
    private int type;
    private String tenantId;
    private Integer projectId;
    private String tokenGroupName;

    public JsonMembership() {
    }

    public JsonMembership(String id, int type, String tenantId, Integer projectId, String tokenGroupName) {
        this.id = id;
        this.type = type;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.tokenGroupName = tokenGroupName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof JsonMembership)) {
            return false;
        }
        return id.equals(((JsonMembership)object).id);
    }

    @Override
    public int hashCode() {
        return 37 + id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }
}
