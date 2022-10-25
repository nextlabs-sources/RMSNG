package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class XacmlRequestReference {

    @JsonProperty("ReferenceId")
    List<String> referenceIds;

    public XacmlRequestReference(List<String> referenceIds) {
        this.referenceIds = referenceIds;
    }

    public List<String> getReferenceIds() {
        return referenceIds;
    }
}
