package com.nextlabs.rms.cc.pojos;

import com.nextlabs.common.util.StringUtils;

public class JsonAdvancedCondnComponent {

    private String operator;
    private JsonPolicyComponent[] components;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public JsonPolicyComponent[] getComponents() {
        return components;
    }

    public void setComponents(JsonPolicyComponent[] components) {
        this.components = components;
    }

    public boolean hasValidContent() {

        return StringUtils.hasText(this.getOperator()) && ("IN".equals(this.getOperator()) || "NOT".equals(this.getOperator())) && this.getComponents() != null;
    }
}
