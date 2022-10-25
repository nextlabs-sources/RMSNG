package com.nextlabs.rms.cc.pojos;

import com.nextlabs.common.util.StringUtils;

public class JsonResourceComponent {

    private String operator;
    private ControlCenterComponent[] components;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public ControlCenterComponent[] getComponents() {
        return components;
    }

    public void setComponents(ControlCenterComponent[] components) {
        this.components = components;
    }

    public boolean isValid() {

        return StringUtils.hasText(this.getOperator()) && this.getComponents() != null;
    }

}
