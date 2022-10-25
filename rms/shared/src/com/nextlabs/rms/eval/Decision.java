package com.nextlabs.rms.eval;

public enum Decision {
    NA("NotApplicable"),
    PERMIT("Permit"),
    DENY("Deny"),
    INDETERMINATE("Indeterminate");

    private String description;

    private Decision(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
