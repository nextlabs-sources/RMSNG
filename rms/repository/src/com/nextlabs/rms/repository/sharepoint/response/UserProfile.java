package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserProfile {

    @SerializedName("d")
    private Detail detail;

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }

    public static class Detail {

        @SerializedName("DisplayName")
        private String displayName;
        @SerializedName("Email")
        private String email;
        @SerializedName("PersonalUrl")
        private String personalUrl;
        @SerializedName("UserUrl")
        private String userUrl;
        @SerializedName("IsFollowed")
        private boolean isFollowed;
        @SerializedName("LatestPost")
        private String latestPost;
        @SerializedName("PictureUrl")
        private String pictureUrl;
        @SerializedName("Title")
        private String title;
        @SerializedName("UserProfileProperties")
        private UserProfileProperties properties;

        public String getDisplayName() {
            return displayName;
        }

        public String getEmail() {
            return email;
        }

        public String getLatestPost() {
            return latestPost;
        }

        public String getPersonalUrl() {
            return personalUrl;
        }

        public String getPictureUrl() {
            return pictureUrl;
        }

        public UserProfileProperties getProperties() {
            return properties;
        }

        public String getTitle() {
            return title;
        }

        public String getUserUrl() {
            return userUrl;
        }

        public boolean isFollowed() {
            return isFollowed;
        }
    }

    static class UserProfileProperties {

        @SerializedName("results")
        private List<UserProfilePropertiesResult> results;

        public List<UserProfilePropertiesResult> getResults() {
            return results;
        }
    }

    static class UserProfilePropertiesResult {

        @SerializedName("Key")
        private String key;
        @SerializedName("Value")
        private String value;

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
