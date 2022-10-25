package com.nextlabs.rms.cc.pojos;

public class ControlCenterObligationParameters {

    private long id;
    private String name;
    private String type;
    private String defaultValue;
    private String value;
    private String listValues;
    private boolean hidden;
    private boolean editable;
    private boolean mandatory;
    private int sortOrder;
    private String shortName;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getListValues() {
        return listValues;
    }

    public void setListValues(String listValues) {
        this.listValues = listValues;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public enum ControlCenterObligationParamType {
        SINGLE_ROW("TEXT_SINGLE_ROW"),
        LIST("LIST"),
        MULTIPLE_ROW("TEXT_MULTIPLE_ROW");

        private String typeValue;

        private ControlCenterObligationParamType(String typeValue) {
            this.typeValue = typeValue;
        }

        public String getTypeValue() {
            return typeValue;
        }
    }
}
