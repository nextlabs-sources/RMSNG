package com.nextlabs.rms.json;

import com.google.gson.JsonElement;

import java.util.List;

public class InitSettings {

    private String userName;
    private String userDisplayName;
    private String loginAccountType;
    private String tenantName;
    private String tenantId;
    private JsonElement userPreferences;
    private boolean isAdmin;
    private boolean isPersonalRepoEnabled;
    private String rmsVersion;
    private boolean isManageProfileAllowed;
    private String redirectPageFromWelcome;
    private String landingPage;
    private String viewerURL;
    private String welcomePageVideoURL;
    private RMDownloadUrls rmDownloadUrls;
    private boolean isFeedbackAllowed;
    private List<String> feedbackAttachmentFormats;
    private String inbuiltServiceProvider;
    private String cookieDomain;
    private String[] roles;
    private boolean isSaasMode;
    private boolean isHideWorkspace;
    private char[] invalidCharactersInFilename;

    public String getLoginAccountType() {
        return loginAccountType;
    }

    public void setLoginAccountType(String loginAccountType) {
        this.loginAccountType = loginAccountType;
    }

    public String getDefaultServiceProvider() {
        return inbuiltServiceProvider;
    }

    public void setDefaultServiceProvider(String inbuiltServiceProvider) {
        this.inbuiltServiceProvider = inbuiltServiceProvider;
    }

    public RMDownloadUrls getRmDownloadUrls() {
        return rmDownloadUrls;
    }

    public void setRmDownloadUrls(RMDownloadUrls rmDownloadUrls) {
        this.rmDownloadUrls = rmDownloadUrls;
    }

    public boolean isFeedbackAllowed() {
        return isFeedbackAllowed;
    }

    public void setFeedbackAllowed(boolean isFeedbackAllowed) {
        this.isFeedbackAllowed = isFeedbackAllowed;
    }

    public String getRmsVersion() {
        return rmsVersion;
    }

    public void setRmsVersion(String rmsVersion) {
        this.rmsVersion = rmsVersion;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean isPersonalRepoEnabled() {
        return isPersonalRepoEnabled;
    }

    public void setPersonalRepoEnabled(boolean isPersonalRepoEnabled) {
        this.isPersonalRepoEnabled = isPersonalRepoEnabled;
    }

    public boolean isManageProfileAllowed() {
        return isManageProfileAllowed;
    }

    public void setManageProfileAllowed(boolean isManageProfileAllowed) {
        this.isManageProfileAllowed = isManageProfileAllowed;
    }

    public String getRedirectPageFromWelcome() {
        return redirectPageFromWelcome;
    }

    public void setRedirectPageFromWelcome(String redirectPageFromWelcome) {
        this.redirectPageFromWelcome = redirectPageFromWelcome;
    }

    public String getLandingPage() {
        return landingPage;
    }

    public void setLandingPage(String landingPage) {
        this.landingPage = landingPage;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getWelcomePageVideoURL() {
        return welcomePageVideoURL;
    }

    public void setWelcomePageVideoURL(String welcomePageVideoURL) {
        this.welcomePageVideoURL = welcomePageVideoURL;
    }

    public List<String> getFeedbackAttachmentFormats() {
        return feedbackAttachmentFormats;
    }

    public void setFeedbackAttachmentFormats(List<String> feedbackAttachmentFormats) {
        this.feedbackAttachmentFormats = feedbackAttachmentFormats;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    /**
     * @return the viewerURL
     */
    public String getViewerURL() {
        return viewerURL;
    }

    /**
     * @param viewerURL the viewerURL to set
     */
    public void setViewerURL(String viewerURL) {
        this.viewerURL = viewerURL;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String displayName) {
        this.userDisplayName = displayName;
    }

    public JsonElement getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(JsonElement userPreferences) {
        this.userPreferences = userPreferences;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public boolean isSaasMode() {
        return isSaasMode;
    }

    public void setSaasMode(boolean isSaasMode) {
        this.isSaasMode = isSaasMode;
    }

    public boolean isHideWorkspace() {
        return isHideWorkspace;
    }

    public void setHideWorkspace(boolean isHideWorkspace) {
        this.isHideWorkspace = isHideWorkspace;
    }

    public char[] getInvalidCharactersInFilename() {
        return invalidCharactersInFilename;
    }

    public void setInvalidCharactersInFilename(char[] invalidCharactersInFilename) {
        this.invalidCharactersInFilename = invalidCharactersInFilename;
    }

}
