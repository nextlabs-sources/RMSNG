/**
 *
 */
package com.nextlabs.rms.repository;

import java.util.Date;

/**
 * @author nnallagatla
 *
 */
public class CreateFolderResult {

    private String pathId;
    private String pathDisplay;
    private String name;
    private Date lastModified;
    private boolean success;

    public CreateFolderResult(String id, String displayName, String folderName, Date lastModified) {
        pathId = id;
        pathDisplay = displayName;
        name = folderName;
        this.lastModified = lastModified;
        this.success = true;
    }

    public CreateFolderResult(boolean success) {
        this.success = success;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
