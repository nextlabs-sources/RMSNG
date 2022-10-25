package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyEvalRestResponseWrapper {

    @JsonProperty("Response")
    private List<PolicyEvalRestResponse> policyEvalRestResponseList;

    public List<PolicyEvalRestResponse> getPolicyEvalRestResponseList() {
        return policyEvalRestResponseList;
    }

    public void setPolicyEvalRestResponseList(List<PolicyEvalRestResponse> policyEvalRestResponseList) {
        this.policyEvalRestResponseList = policyEvalRestResponseList;
    }
}
