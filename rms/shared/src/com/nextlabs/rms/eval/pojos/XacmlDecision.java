package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XacmlDecision {

    @JsonProperty("Action")
    private String action;

    @JsonProperty("Obligations")
    private List<XacmlObligation> obligations;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<XacmlObligation> getObligations() {
        return obligations;
    }

    public void setObligations(List<XacmlObligation> obligations) {
        this.obligations = obligations;
    }
}
