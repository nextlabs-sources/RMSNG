package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class XacmlMultiRequests {

    @JsonProperty("RequestReference")
    List<XacmlRequestReference> xacmlRequestReferences;

    public XacmlMultiRequests(List<XacmlRequestReference> xacmlRequestReferences) {
        this.xacmlRequestReferences = xacmlRequestReferences;
    }

    public List<XacmlRequestReference> getXacmlRequestReferences() {
        return xacmlRequestReferences;
    }
}
