package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.ContextInfo;

public class ContextInfoVerbose {

    @SerializedName("d")
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        @SerializedName("GetContextWebInformation")
        private ContextInfo contextInfo;

        public ContextInfo getContextInfo() {
            return contextInfo;
        }

        public void setContextInfo(ContextInfo contextInfo) {
            this.contextInfo = contextInfo;
        }
    }
}
