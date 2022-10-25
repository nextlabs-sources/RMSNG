package com.nextlabs.rms.cc.pojos;

public class JsonControlCenterComponent {

    private String id;
    private boolean push;
    private long deploymentTime;
    private String type;

    public JsonControlCenterComponent(String id, String type, boolean push, long deploymentTime) {
        this.id = id;
        this.type = type;
        this.push = push;
        this.deploymentTime = deploymentTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public long getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(long deploymentTime) {
        this.deploymentTime = deploymentTime;
    }
}
