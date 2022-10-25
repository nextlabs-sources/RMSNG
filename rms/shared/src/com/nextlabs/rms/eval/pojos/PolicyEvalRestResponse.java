package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PolicyEvalRestResponse {

    @JsonProperty("ActionsAndObligations")
    private Map<String, List<XacmlDecision>> decisionMap;

    public Map<String, List<XacmlDecision>> getDecisionMap() {
        return decisionMap;
    }

    public void setDecisionMap(Map<String, List<XacmlDecision>> decisionMap) {
        this.decisionMap = decisionMap;
    }
}
