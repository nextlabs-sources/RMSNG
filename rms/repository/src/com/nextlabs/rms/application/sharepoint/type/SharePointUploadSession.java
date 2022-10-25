package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class SharePointUploadSession {

    @SerializedName("uploadUrl")
    private String uploadUrl;
    @SerializedName("expirationDateTime")
    private Date expirationDateTime;

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public Date getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(Date expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }
}
