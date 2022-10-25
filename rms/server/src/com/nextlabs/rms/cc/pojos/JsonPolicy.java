package com.nextlabs.rms.cc.pojos;

import com.nextlabs.common.shared.JsonABACMembershipObligation;
import com.nextlabs.common.util.StringUtils;

public class JsonPolicy {

    private Long id;
    private Integer version;
    private String name;
    private String description;
    private String effectType;
    private JsonResourceComponent[] resources;
    private String[] actions;
    private JsonAdvancedCondnComponent[] applicationComponents;
    private JsonAdvancedCondnComponent[] userComponents;
    private String advancedConditions;
    private Boolean toDeploy = true;
    private String status;
    private JsonABACMembershipObligation[] jsonABACMembershipObligation;
    private ControlCenterScheduleConfig scheduleConfig;

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

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public JsonResourceComponent[] getResources() {
        return resources;
    }

    public void setResources(JsonResourceComponent[] resources) {
        this.resources = resources;
    }

    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public JsonAdvancedCondnComponent[] getApplicationComponents() {
        return applicationComponents;
    }

    public void setApplicationComponents(
        JsonAdvancedCondnComponent[] applicationComponents) {
        this.applicationComponents = applicationComponents;
    }

    public JsonAdvancedCondnComponent[] getUserComponents() {
        return userComponents;
    }

    public void setUserComponents(JsonAdvancedCondnComponent[] userComponents) {
        this.userComponents = userComponents;
    }

    public String getAdvancedConditions() {
        return advancedConditions;
    }

    public void setAdvancedConditions(String advancedConditions) {
        this.advancedConditions = advancedConditions;
    }

    public boolean hasValidContent() {

        return StringUtils.hasText(this.getName()) && this.userComponents != null && this.resources != null && this.actions != null;
    }

    public Boolean getToDeploy() {
        return toDeploy;
    }

    public void setToDeploy(Boolean toDeploy) {
        this.toDeploy = toDeploy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonABACMembershipObligation[] getJsonABACMembershipObligation() {
        return jsonABACMembershipObligation;
    }

    public void setJsonABACMembershipObligation(
        JsonABACMembershipObligation[] jsonABACMembershipObligation) {
        this.jsonABACMembershipObligation = jsonABACMembershipObligation;
    }

}
