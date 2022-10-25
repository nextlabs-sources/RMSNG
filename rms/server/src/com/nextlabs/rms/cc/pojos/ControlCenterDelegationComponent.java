package com.nextlabs.rms.cc.pojos;

import java.util.Map;

public class ControlCenterDelegationComponent {

    public static final String KEY_ID = "policy_model_id";
    public static final String KEY_NAME = "policy_model_name";

    private long id;
    private String name;
    private Map<String, String> data;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
