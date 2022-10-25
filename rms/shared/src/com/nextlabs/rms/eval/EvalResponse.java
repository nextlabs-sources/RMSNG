package com.nextlabs.rms.eval;

import com.nextlabs.nxl.Rights;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EvalResponse {

    private final List<Obligation> obligations;
    private final List<com.nextlabs.nxl.FilePolicy.Obligation> adhocObligations;
    private final Rights[] rights;

    public EvalResponse() {
        this.rights = new Rights[0];
        this.adhocObligations = new ArrayList<>(0);
        this.obligations = new ArrayList<>(0);
    }

    public EvalResponse(Rights[] rights) {
        this.rights = rights;
        this.adhocObligations = new ArrayList<>();
        this.obligations = new ArrayList<>();
    }

    public void addAdHocObligations(List<com.nextlabs.nxl.FilePolicy.Obligation> obligations) {
        adhocObligations.addAll(obligations);
    }

    public void addObligations(List<Obligation> obligationList) {
        obligations.addAll(obligationList);
    }

    public com.nextlabs.nxl.FilePolicy.Obligation getAdhocObligation(String name) {
        for (com.nextlabs.nxl.FilePolicy.Obligation obligation : adhocObligations) {
            if (obligation.getName().equals(name)) {
                return obligation;
            }
        }
        return null;
    }

    public List<com.nextlabs.nxl.FilePolicy.Obligation> getAdhocObligations() {
        return adhocObligations;
    }

    public List<Obligation> getObligations() {
        return obligations;
    }

    public Rights[] getRights() {
        return rights;
    }

    public String getEffectiveWatermark() {
        if (!adhocObligations.isEmpty()) {
            for (com.nextlabs.nxl.FilePolicy.Obligation obligation : adhocObligations) {
                if (com.nextlabs.nxl.FilePolicy.Obligation.WATERMARK.equals(obligation.getName())) {
                    Map<String, Object> watermark = obligation.getValue();
                    if (watermark != null) {
                        return (String)watermark.get(com.nextlabs.nxl.FilePolicy.Obligation.WATERMARK_TEXT_KEY);
                    }
                }
            }
        }
        if (!obligations.isEmpty()) {
            for (Obligation obligation : obligations) {
                if (Obligation.WATERMARK.equals(obligation.getName())) {
                    List<Attribute> attributes = obligation.getAttributes();
                    if (!attributes.isEmpty()) {
                        for (Attribute attribute : attributes) {
                            if (Obligation.WATERMARK_TEXT_KEY.equals(attribute.getName())) {
                                return attribute.getValue();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
