package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class SharePointSiteItem {

    @SerializedName("@odata.context")
    private String dataCtx;
    @SerializedName("createdDateTime")
    private Date createdTime;
    @SerializedName("description")
    private String description;
    @SerializedName("id")
    private String id;
    @SerializedName("lastModifiedDateTime")
    private Date lstModifiedTime;
    @SerializedName("name")
    private String name;
    @SerializedName("webUrl")
    private String webUrl;
    @SerializedName("displayName")
    private String displayName;
    @SerializedName("root")
    private SharePointSiteRoot root;
    @SerializedName("siteCollection")
    private SharePointSiteCollection siteCollection;

    public String getDataContext() {
        return dataCtx;
    }

    public void setDataContext(String dataCtx) {
        this.dataCtx = dataCtx;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
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

    public Date getLstModifiedTime() {
        return lstModifiedTime;
    }

    public void setLstModifiedTime(Date lstModifiedTime) {
        this.lstModifiedTime = lstModifiedTime;
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

    public SharePointSiteRoot getRoot() {
        return root;
    }

    public void setRoot(SharePointSiteRoot root) {
        this.root = root;
    }

    public SharePointSiteCollection getSiteCollection() {
        return siteCollection;
    }

    public void setSiteCollection(SharePointSiteCollection siteCollection) {
        this.siteCollection = siteCollection;
    }
}
