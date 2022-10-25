package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class SharePointUploadItem {

    @SerializedName("name")
    private String name;
    @SerializedName("folder")
    private SharePointFolder folder;
    @SerializedName("@microsoft.graph.conflictBehavior")
    private String conflictBehaviour;

    public String getConflictBehaviour() {
        return conflictBehaviour;
    }

    public void setConflictBehaviour(String conflictBehaviour) {
        this.conflictBehaviour = conflictBehaviour;
    }

    public SharePointFolder getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public void setFolder(SharePointFolder folder) {
        this.folder = folder;
    }

    public void setName(String name) {
        this.name = name;
    }
}
