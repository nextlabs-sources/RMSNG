package com.nextlabs.common.shared;

import java.util.List;

public class JsonProjectFileList {

    private Long totalFiles;
    private List<JsonProjectFile> files;

    public List<JsonProjectFile> getFiles() {
        return files;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setFiles(List<JsonProjectFile> files) {
        this.files = files;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }
}
