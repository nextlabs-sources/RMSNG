package com.nextlabs.rms.cc.pojos;

import com.nextlabs.common.util.StringUtils;

import java.util.Arrays;

public class JsonCondition {

    private static final String[] VALID_OPERATORS = { "=", "!=", ">", ">=", "<", "<=" };
    private String attribute;
    private String operator;
    private String combiner;
    private String[] value;

    public JsonCondition(String attribute, String operator, String[] value, String combiner) {
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
        this.combiner = combiner;
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

    public String getCombiner() {
        return combiner;
    }

    public void setCombiner(String combiner) {
        this.combiner = combiner;
    }

    public String[] getValue() {
        return value;
    }

    public void setValue(String[] value) {
        this.value = value;
    }

    public boolean hasValidContent() {

        return StringUtils.hasText(this.getAttribute()) && StringUtils.hasText(this.getOperator()) && Arrays.asList(VALID_OPERATORS).indexOf(this.getOperator()) != -1 && StringUtils.hasText(this.getCombiner()) && ("or".equalsIgnoreCase(this.getCombiner()) || "and".equalsIgnoreCase(this.getCombiner())) && this.getValue() != null;
    }
}
