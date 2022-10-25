package com.nextlabs.common.shared;

import java.util.List;

public class JsonSharedWithMeFileList {

    private Long totalFiles;

    private List<JsonSharedWithMeFile> files;

    public List<JsonSharedWithMeFile> getFiles() {
        return files;
    }

    public void setFiles(List<JsonSharedWithMeFile> files) {
        this.files = files;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }
}
