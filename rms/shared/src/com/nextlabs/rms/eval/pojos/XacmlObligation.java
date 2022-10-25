package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class XacmlObligation {

    @JsonProperty("Id")
    private String obligationId;

    @JsonProperty("AttributeAssignment")
    private List<ObligationAttribute> attributeList;

    public String getObligationId() {
        return obligationId;
    }

    public void setObligationId(String obligationId) {
        this.obligationId = obligationId;
    }

    public List<ObligationAttribute> getAttributeList() {
        return attributeList;
    }

    public void setAttributeList(List<ObligationAttribute> attributeList) {
        this.attributeList = attributeList;
    }
}
