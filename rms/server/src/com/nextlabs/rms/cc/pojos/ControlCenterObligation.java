package com.nextlabs.rms.cc.pojos;

public class ControlCenterObligation {

    private long id;
    private String name;
    private String shortName;
    private String runAt;
    private ControlCenterObligationParameters[] parameters;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getRunAt() {
        return runAt;
    }

    public void setRunAt(String runAt) {
        this.runAt = runAt;
    }

    public ControlCenterObligationParameters[] getParameters() {
        return parameters;
    }

    public void setParameters(ControlCenterObligationParameters[] parameters) {
        this.parameters = parameters;
    }

}
