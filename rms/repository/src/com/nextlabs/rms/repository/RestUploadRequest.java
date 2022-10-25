package com.nextlabs.rms.repository;

import java.io.File;
import java.io.InputStream;

public class RestUploadRequest {

    String json;
    String fileName;
    InputStream fileStream;
    File uploadDir;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getFileStream() {
        return fileStream;
    }

    public void setFileStream(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    public File getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(File uploadDir) {
        this.uploadDir = uploadDir;
    }
}
