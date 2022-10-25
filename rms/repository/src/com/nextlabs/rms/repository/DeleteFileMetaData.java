package com.nextlabs.rms.repository;

import java.util.List;
import java.util.Map;

public class DeleteFileMetaData {

    private String filePath;

    private String fileName;

    private Map<String, Long> keySizeMapping;

    private List<String> deletedKeys;

    public Map<String, Long> getKeySizeMapping() {
        return keySizeMapping;
    }

    public void setKeySizeMapping(Map<String, Long> keySizeMapping) {
        this.keySizeMapping = keySizeMapping;
    }

    public List<String> getDeletedKeys() {
        return deletedKeys;
    }

    public void setDeletedKeys(List<String> deletedKeys) {
        this.deletedKeys = deletedKeys;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
