package com.nextlabs.common.shared;

public class JsonRepositoryFileEntry {

    private String pathId;
    private String pathDisplay;
    private Long size;
    private String name;
    private boolean folder;
    private Long lastModified;

    public Long getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public String getPathId() {
        return pathId;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public Long getSize() {
        return size;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
