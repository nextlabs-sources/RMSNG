package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SPRestList {

    @SerializedName("value")
    private List<Results> results;

    public List<Results> getResults() {
        return results;
    }

    public void setResults(List<Results> results) {
        this.results = results;
    }

    public static class Results {

        @SerializedName("Title")
        private String title;
        @SerializedName("LastItemModifiedDate")
        private String lastModifiedTime;
        @SerializedName("Id")
        private String id;
        @SerializedName("Created")
        private String createdTime;
        @SerializedName("ParentWebUrl")
        private String parentWebUrl;

        public String getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(String createdDate) {
            this.createdTime = createdDate;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLastModifiedTime() {
            return lastModifiedTime;
        }

        public void setLastModifiedTime(String aLastItemModifiedDate) {
            lastModifiedTime = aLastItemModifiedDate;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String aTitle) {
            title = aTitle;
        }

        public String getParentWebUrl() {
            return parentWebUrl;
        }

        public void setParentWebUrl(String parentWebUrl) {
            this.parentWebUrl = parentWebUrl;
        }
    }
}
