package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SPRestErrorResponse implements Serializable {

    private static final long serialVersionUID = 1787878L;

    @SerializedName("odata.error")
    private SPRestResult error;

    public SPRestResult getError() {
        return error;
    }

    public void setError(SPRestResult error) {
        this.error = error;
    }
}
