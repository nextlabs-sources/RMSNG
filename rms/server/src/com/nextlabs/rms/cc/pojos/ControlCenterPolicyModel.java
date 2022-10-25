package com.nextlabs.rms.cc.pojos;

import java.util.List;

public class ControlCenterPolicyModel {

    private Long id;
    private String name;
    private String shortName;
    private String description;
    private String type;
    private String status;
    private List<ControlCenterIdentifier> actions;
    private List<ControlCenterObligation> obligations;
    private List<ControlCenterAttribute> attributes;
    private Long version;
    private ControlCenterTag[] tags;

    public ControlCenterPolicyModel() {

    }

    public ControlCenterPolicyModel(Long id, String name) {
        this.id = id;
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ControlCenterIdentifier> getActions() {
        return actions;
    }

    public void setActions(List<ControlCenterIdentifier> actions) {
        this.actions = actions;
    }

    public List<ControlCenterObligation> getObligations() {
        return obligations;
    }

    public void setObligations(List<ControlCenterObligation> obligations) {
        this.obligations = obligations;
    }

    public List<ControlCenterAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ControlCenterAttribute> attributes) {
        this.attributes = attributes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public ControlCenterTag[] getTags() {
        return tags;
    }

    public void setTags(ControlCenterTag[] tags) {
        this.tags = tags;
    }
}
