package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SPRestResult implements Serializable {

    private static final long serialVersionUID = 178672367263L;

    @SerializedName("message")
    private SPRestMessage message;

    @SerializedName("code")
    private String code;

    public String getCode() {
        return code;
    }

    public SPRestMessage getMessage() {
        return message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(SPRestMessage message) {
        this.message = message;
    }

    public static class SPRestMessage implements Serializable {

        private static final long serialVersionUID = 2888679292782783658L;

        @SerializedName("lang")
        private String lang;

        @SerializedName("value")
        private String value;

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
