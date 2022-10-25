package com.nextlabs.rms.viewer.json;

public class ShowFileResponse {

    private String viewerUrl;
    private String baseURL;
    private int statusCode;
    private int rights;
    private String name;
    private boolean owner;
    private String duid;
    private String membership;

    public ShowFileResponse() {
        statusCode = 200;
        setViewerUrl("");
        baseURL = "";
    }

    public ShowFileResponse(String aBaseURL) {
        super();
        if (aBaseURL != null) {
            baseURL = aBaseURL;
        }
        statusCode = 200;
    }

    public String getViewerUrl() {
        return viewerUrl;
    }

    public void setViewerUrl(String viewerUrl) {
        this.viewerUrl = baseURL + viewerUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getRights() {
        return rights;
    }

    public void setRights(int rights) {
        this.rights = rights;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean isOwner) {
        owner = isOwner;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getMembership() {
        return membership;
    }

    public void setMembership(String membership) {
        this.membership = membership;
    }
}
