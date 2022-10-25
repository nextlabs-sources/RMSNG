package com.nextlabs.rms.repository.onedrive.type;

import com.google.gson.annotations.SerializedName;

public class OneDriveUploadItem {

    @SerializedName("name")
    private String name;
    @SerializedName("folder")
    private OneDriveFolder folder;
    @SerializedName("@name.conflictBehavior")
    private String conflictBehaviour;

    public String getConflictBehaviour() {
        return conflictBehaviour;
    }

    public void setConflictBehaviour(String conflictBehaviour) {
        this.conflictBehaviour = conflictBehaviour;
    }

    public OneDriveFolder getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public void setFolder(OneDriveFolder folder) {
        this.folder = folder;
    }

    public void setName(String name) {
        this.name = name;
    }
}
