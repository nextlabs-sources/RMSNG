package com.nextlabs.rms.cc.pojos;

public class ControlCenterPolicy {

    private Long id;
    private String name;
    private String description;
    private String expression;
    private String status;
    private String effectType;
    private ControlCenterTag[] tags;
    private ControlCenterComponentRequest[] subjectComponents;
    private ControlCenterComponentRequest[] fromResourceComponents;
    private ControlCenterComponentRequest[] actionComponents;
    private ControlCenterDelegationObligation[] allowObligations;
    private ControlCenterDelegationObligation[] denyObligations;
    private ControlCenterScheduleConfig scheduleConfig;
    private boolean deployed;
    private boolean skipValidate;
    private Integer version;
    private long lastUpdatedDate;
    private boolean skipAddingTrueAllowAttribute;

    public boolean isSkipAddingTrueAllowAttribute() {
        return skipAddingTrueAllowAttribute;
    }

    public void setSkipAddingTrueAllowAttribute(boolean skipAddingTrueAllowAttribute) {
        this.skipAddingTrueAllowAttribute = skipAddingTrueAllowAttribute;
    }

    public ControlCenterScheduleConfig getScheduleConfig() {
        return scheduleConfig;
    }

    public void setScheduleConfig(ControlCenterScheduleConfig scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public ControlCenterTag[] getTags() {
        return tags;
    }

    public void setTags(ControlCenterTag[] tags) {
        this.tags = tags;
    }

    public ControlCenterComponentRequest[] getSubjectComponents() {
        return subjectComponents;
    }

    public void setSubjectComponents(ControlCenterComponentRequest[] subjectComponents) {
        this.subjectComponents = subjectComponents;
    }

    public ControlCenterComponentRequest[] getFromResourceComponents() {
        return fromResourceComponents;
    }

    public void setFromResourceComponents(ControlCenterComponentRequest[] fromResourceComponents) {
        this.fromResourceComponents = fromResourceComponents;
    }

    public ControlCenterComponentRequest[] getActionComponents() {
        return actionComponents;
    }

    public void setActionComponents(ControlCenterComponentRequest[] actionComponents) {
        this.actionComponents = actionComponents;
    }

    public ControlCenterDelegationObligation[] getAllowObligations() {
        return allowObligations;
    }

    public void setAllowObligations(ControlCenterDelegationObligation[] allowObligations) {
        this.allowObligations = allowObligations;
    }

    public ControlCenterDelegationObligation[] getDenyObligations() {
        return denyObligations;
    }

    public void setDenyObligations(ControlCenterDelegationObligation[] denyObligations) {
        this.denyObligations = denyObligations;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
