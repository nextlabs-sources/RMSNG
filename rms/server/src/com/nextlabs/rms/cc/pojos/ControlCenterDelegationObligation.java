package com.nextlabs.rms.cc.pojos;

import java.util.Map;

public class ControlCenterDelegationObligation {

    private String name;
    private Long policyModelId;
    private Map<String, String> params;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPolicyModelId() {
        return policyModelId;
    }

    public void setPolicyModelId(Long policyModelId) {
        this.policyModelId = policyModelId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

}
