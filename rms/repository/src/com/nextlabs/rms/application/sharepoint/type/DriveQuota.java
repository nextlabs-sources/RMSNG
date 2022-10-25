package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class DriveQuota {

    @SerializedName("deleted")
    private long deleted;
    @SerializedName("remaining")
    private long remaining;
    @SerializedName("state")
    private String state;
    @SerializedName("total")
    private long total;
    @SerializedName("used")
    private long used;

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }
}
