package com.nextlabs.rms.cc.pojos;

public class ControlCenterTag {

    private String id;
    private String key;
    private String label;
    private String type;
    private String status;

    public ControlCenterTag() {

    }

    public ControlCenterTag(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
