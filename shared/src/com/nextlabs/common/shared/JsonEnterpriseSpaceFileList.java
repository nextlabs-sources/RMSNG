package com.nextlabs.common.shared;

import java.util.List;

public class JsonEnterpriseSpaceFileList {

    private Long totalFiles;
    private List<JsonEnterpriseSpaceFile> files;

    public List<JsonEnterpriseSpaceFile> getFiles() {
        return files;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setFiles(List<JsonEnterpriseSpaceFile> files) {
        this.files = files;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }
}
