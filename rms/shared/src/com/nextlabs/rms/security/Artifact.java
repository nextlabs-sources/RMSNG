package com.nextlabs.rms.security;

public class Artifact {

    private long timeStamp;

    private Object artifact;

    public Artifact(long timeStamp, Object artifact) {
        this.timeStamp = timeStamp;
        this.artifact = artifact;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Object getKeyManagers() {
        return artifact;
    }

    public void setKeyManagers(Object artifact) {
        this.artifact = artifact;
    }
}
