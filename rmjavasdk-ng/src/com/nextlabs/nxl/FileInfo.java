package com.nextlabs.nxl;

import com.nextlabs.common.io.file.FileUtils;

import java.io.File;
import java.util.Date;

public class FileInfo {

    private String fileExtension;
    private String fileName;
    private String modifiedBy;
    private String createdBy;
    private Long dateModified;
    private Long dateCreated;

    private FileInfo(String name) {
        fileName = name;
        fileExtension = FileUtils.getExtension(fileName);
    }

    private FileInfo(File file) {
        fileExtension = '.' + FileUtils.getExtension(file);
        fileName = file.getName();
    }

    public FileInfo(String name, String createdBy) {
        this(name);
        initFileInfo(createdBy);
    }

    public FileInfo(File file, String createdBy) {
        this(file);
        initFileInfo(createdBy);
    }

    private void initFileInfo(String createdBy) {
        this.createdBy = createdBy;
        this.modifiedBy = createdBy;
        long now = new Date().getTime();
        this.dateModified = now;
        this.dateCreated = now;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getFileName() {
        return fileName;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Long getLastModified() {
        return dateModified;
    }

    public void setLastModified(Long lastModified) {
        this.dateModified = lastModified;
    }

    public Long getCreationTime() {
        return dateCreated;
    }
}
