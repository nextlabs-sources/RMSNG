package com.nextlabs.common.shared;

import java.io.Serializable;
import java.util.Map;

public class JsonIdentityProvider implements Serializable {

    private static final long serialVersionUID = -2276392131518033119L;
    private int id;
    private Integer type;
    private String name;
    private String attributes;
    private Map<String, String> userAttrMap;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getUserAttrMap() {
        return userAttrMap;
    }

    public void setUserAttrMap(Map<String, String> userAttrMap) {
        this.userAttrMap = userAttrMap;
    }

}
