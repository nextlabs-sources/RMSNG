package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;
/**
 * @deprecated This class has been dropped.
 *
 */
@Deprecated
public class SharePointSubSiteItem {

    @SerializedName("createdDateTime")
    private String createdDateTime;
    @SerializedName("description")
    private String description;
    @SerializedName("id")
    private String id;
    @SerializedName("lastModifiedDateTime")
    private String lastModifiedDateTime;
    @SerializedName("name")
    private String name;
    @SerializedName("webUrl")
    private String webUrl;
    @SerializedName("displayName")
    private String displayName;
    @SerializedName("parentReference")
    private ParentReference parentReference;

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ParentReference getParentReference() {
        return parentReference;
    }

    public void setParentReference(ParentReference parentReference) {
        this.parentReference = parentReference;
    }

    public static class ParentReference {

        @SerializedName("siteId")
        private String siteId;

        public String getSiteId() {
            return siteId;
        }

        public void setSiteId(String siteId) {
            this.siteId = siteId;
        }
    }
}
