package com.nextlabs.rms.cc.pojos;

public class JsonPolicyComponent {

    private JsonCondition[] conditions;

    public JsonCondition[] getConditions() {
        return conditions;
    }

    public void setConditions(JsonCondition[] conditions) {
        this.conditions = conditions;
    }

    public boolean hasValidContent() {

        return this.conditions != null;
    }
}
