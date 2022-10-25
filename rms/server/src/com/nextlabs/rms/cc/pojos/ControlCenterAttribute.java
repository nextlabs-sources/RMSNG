package com.nextlabs.rms.cc.pojos;

public class ControlCenterAttribute {

    private Long id;
    private String name;
    private String shortName;
    private String dataType;
    private String regExPattern;
    private ControlCenterOperator[] operatorConfigs;
    private String[] values; // Not used by CC. Only used by RMS to store classification labels
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getRegExPattern() {
        return regExPattern;
    }

    public void setRegExPattern(String regExPattern) {
        this.regExPattern = regExPattern;
    }

    public ControlCenterOperator[] getOperatorConfigs() {
        return operatorConfigs;
    }

    public void setOperatorConfigs(ControlCenterOperator[] operatorConfigs) {
        this.operatorConfigs = operatorConfigs;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }
}
