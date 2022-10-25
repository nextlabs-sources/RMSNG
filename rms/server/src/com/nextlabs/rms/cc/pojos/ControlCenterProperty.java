package com.nextlabs.rms.cc.pojos;

public class ControlCenterProperty {

    private String id;
    private String key;
    private String value;

    public ControlCenterProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public ControlCenterProperty(String id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
