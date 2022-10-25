package com.nextlabs.rms.cc.pojos;

public class ControlCenterComponent {

    private Long id;
    private Integer version;
    private String name;
    private String description;
    private ControlCenterTag[] tags;
    private String type;
    private ControlCenterPolicyModel policyModel;
    private ControlCenterCondition[] conditions;
    private String[] actions;
    private ControlCenterComponent[] subComponents;
    private String status;
    private boolean deployed;
    private boolean skipValidate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ControlCenterTag[] getTags() {
        return tags;
    }

    public void setTags(ControlCenterTag[] tags) {
        this.tags = tags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ControlCenterPolicyModel getPolicyModel() {
        return policyModel;
    }

    public void setPolicyModel(ControlCenterPolicyModel policyModel) {
        this.policyModel = policyModel;
    }

    public ControlCenterCondition[] getConditions() {
        return conditions;
    }

    public void setConditions(ControlCenterCondition[] conditions) {
        this.conditions = conditions;
    }

    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    public boolean isSkipValidate() {
        return skipValidate;
    }

    public void setSkipValidate(boolean skipValidate) {
        this.skipValidate = skipValidate;
    }

    public ControlCenterComponent[] getSubComponents() {
        return subComponents;
    }

    public void setSubComponents(ControlCenterComponent[] subComponents) {
        this.subComponents = subComponents;
    }
}
