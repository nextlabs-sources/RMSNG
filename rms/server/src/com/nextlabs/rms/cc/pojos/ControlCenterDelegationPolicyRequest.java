package com.nextlabs.rms.cc.pojos;

public class ControlCenterDelegationPolicyRequest {

    private String name;
    private String description;
    private String status;
    private String effectType;
    private ControlCenterDelegationSubject subjectComponent;
    private ControlCenterDelegationObligation[] obligations;
    private ControlCenterComponentRequest[] resourceComponents;
    private ControlCenterComponentRequest[] actionComponents;

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

    public ControlCenterDelegationSubject getSubjectComponent() {
        return subjectComponent;
    }

    public void setSubjectComponent(ControlCenterDelegationSubject subjectComponent) {
        this.subjectComponent = subjectComponent;
    }

    public ControlCenterDelegationObligation[] getObligations() {
        return obligations;
    }

    public void setObligations(ControlCenterDelegationObligation[] obligations) {
        this.obligations = obligations;
    }

    public ControlCenterComponentRequest[] getResourceComponents() {
        return resourceComponents;
    }

    public void setResourceComponents(ControlCenterComponentRequest[] resourceComponents) {
        this.resourceComponents = resourceComponents;
    }

    public ControlCenterComponentRequest[] getActionComponents() {
        return actionComponents;
    }

    public void setActionComponents(ControlCenterComponentRequest[] actionComponents) {
        this.actionComponents = actionComponents;
    }

}
