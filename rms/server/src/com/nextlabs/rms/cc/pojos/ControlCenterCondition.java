package com.nextlabs.rms.cc.pojos;

public class ControlCenterCondition {

    private String attribute;
    private String operator;
    private String value;

    public ControlCenterCondition(String attribute, String operator, String value) {
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
