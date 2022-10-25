package com.nextlabs.rms.repository;

public class OperationResult {

    private String pathId;
    private long size;
    private boolean isDirectory;
    private boolean success;
    private UploadedFileMetaData uploadMetadata;
    private DeleteFileMetaData deleteMetadata;

    public OperationResult(String filePath, Long size, boolean isDirectory, boolean success) {
        this.pathId = filePath;
        this.size = size;
        this.isDirectory = isDirectory;
        this.success = success;
    }

    public String getFilePath() {
        return pathId;
    }

    public void setFilePath(String filePath) {
        this.pathId = filePath;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public UploadedFileMetaData getUploadMetadata() {
        return uploadMetadata;
    }

    public void setUploadMetadata(UploadedFileMetaData metadata) {
        this.uploadMetadata = metadata;
    }

    public DeleteFileMetaData getDeleteMetadata() {
        return deleteMetadata;
    }

    public void setDeleteMetadata(DeleteFileMetaData fromMetadata) {
        this.deleteMetadata = fromMetadata;
    }
}
