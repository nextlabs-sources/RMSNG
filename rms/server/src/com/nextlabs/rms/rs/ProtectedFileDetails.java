package com.nextlabs.rms.rs;

import com.nextlabs.rms.application.FileUploadMetadata;

public class ProtectedFileDetails {

    private String path;
    private long lastModified;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public ProtectedFileDetails(FileUploadMetadata fileUploadMetadata) {
        this.path = fileUploadMetadata.getPathDisplay();
        this.lastModified = fileUploadMetadata.getLastModifiedTime().getTime();
    }

}
