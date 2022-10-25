package com.nextlabs.rms.repository.onedrive;

import com.google.gson.annotations.SerializedName;

public class OneDriveOwner {

    @SerializedName("user")
    private OneDriveUser user;

    public OneDriveUser getUser() {
        return user;
    }

    public void setUser(OneDriveUser user) {
        this.user = user;
    }

    public static class OneDriveUser {

        @SerializedName("id")
        private String id;
        @SerializedName("displayName")
        private String displayName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

}
