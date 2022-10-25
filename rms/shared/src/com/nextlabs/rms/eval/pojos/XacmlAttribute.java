package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XacmlAttribute {

    public static final String DEFAULT_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

    @JsonProperty("DataType")
    private String dataType = DEFAULT_DATA_TYPE;

    @JsonProperty("IncludeInResult")
    private boolean includeInResult;

    @JsonProperty("AttributeId")
    private String attributeId;

    @JsonProperty("Value")
    private Object value;

    public XacmlAttribute() {
    }

    public XacmlAttribute(String attributeId, Object value) {
        this.attributeId = attributeId;
        this.value = value;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isIncludeInResult() {
        return includeInResult;
    }

    public void setIncludeInResult(boolean includeInResult) {
        this.includeInResult = includeInResult;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
