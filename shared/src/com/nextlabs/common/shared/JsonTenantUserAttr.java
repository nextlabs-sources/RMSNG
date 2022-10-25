package com.nextlabs.common.shared;

import java.io.Serializable;

public class JsonTenantUserAttr implements Serializable {

    private static final long serialVersionUID = -2276392131518033119L;
    private String name;
    private Boolean custom;
    private Boolean selected;

    public JsonTenantUserAttr(String name, Boolean custom, Boolean selected) {
        super();
        this.name = name;
        this.custom = custom;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
