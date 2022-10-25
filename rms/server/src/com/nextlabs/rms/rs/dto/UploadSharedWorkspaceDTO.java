package com.nextlabs.rms.rs.dto;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.shared.Constants.SharedWorkSpaceUploadType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.shared.UploadUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UploadSharedWorkspaceDTO {

    private String fileName;
    private String fileParentPathId;
    private String repoId;
    private String path;
    private File file;
    private boolean userConfirmedFileOverwrite;
    private String uniqueResourceId;
    private int uploadTypeOridinal;
    private boolean allowOverwrite;
    private Operations uploadOps;

    public UploadSharedWorkspaceDTO(String repoId, RestUploadRequest uploadReq, String path, RMSUserPrincipal principal)
            throws IOException {
        this.repoId = repoId;
        this.path = path;
        JsonRequest jsonRequest = JsonRequest.fromJson(uploadReq.getJson());
        this.fileName = jsonRequest.getParameter("name");
        this.fileParentPathId = jsonRequest.getParameter("parentPathId");
        this.file = new File(uploadReq.getUploadDir(), fileName);
        Files.copy(uploadReq.getFileStream(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (file.length() == 0) {
            throw new ValidateException(5005, "Empty files are not allowed to be uploaded.");
        }

        this.userConfirmedFileOverwrite = Boolean.parseBoolean(jsonRequest.getParameter("userConfirmedFileOverwrite"));
        this.uniqueResourceId = UploadUtil.getUniqueResourceId(SPACETYPE.ENTERPRISESPACE, principal.getTenantId(), fileParentPathId, file.getAbsolutePath());
        this.uploadTypeOridinal = jsonRequest.getIntParameter("type", Constants.SharedWorkSpaceUploadType.UPLOAD_SYSBUCKET.ordinal());
        this.getUploadOps(SharedWorkSpaceUploadType.values()[uploadTypeOridinal]);
    }

    private void getUploadOps(Constants.SharedWorkSpaceUploadType uploadType) {
        if (uploadType == Constants.SharedWorkSpaceUploadType.UPLOAD_EDIT) {
            this.uploadOps = Operations.UPLOAD_EDIT;
        } else if (uploadType == Constants.SharedWorkSpaceUploadType.UPLOAD_SYSBUCKET) {
            this.uploadOps = Operations.UPLOAD_NORMAL;
        } else if (uploadType == Constants.SharedWorkSpaceUploadType.UPLOAD_VIEW) {
            this.uploadOps = Operations.UPLOAD_VIEW;
        } else if (uploadType == Constants.SharedWorkSpaceUploadType.UPLOAD_NO_REENCRYPTION) {
            this.uploadOps = Operations.UPLOAD_ASIS;
        } else {
            this.uploadOps = Operations.UPLOAD_NORMAL;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileParentPathId() {
        return fileParentPathId;
    }

    public void setFileParentPathId(String fileParentPathId) {
        this.fileParentPathId = fileParentPathId;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isUserConfirmedFileOverwrite() {
        return userConfirmedFileOverwrite;
    }

    public void setUserConfirmedFileOverwrite(boolean userConfirmedFileOverwrite) {
        this.userConfirmedFileOverwrite = userConfirmedFileOverwrite;
    }

    public String getUniqueResourceId() {
        return uniqueResourceId;
    }

    public void setUniqueResourceId(String uniqueResourceId) {
        this.uniqueResourceId = uniqueResourceId;
    }

    public int getUploadTypeOridinal() {
        return uploadTypeOridinal;
    }

    public void setUploadTypeOridinal(int uploadTypeOridinal) {
        this.uploadTypeOridinal = uploadTypeOridinal;
    }

    public boolean isAllowOverwrite() {
        return allowOverwrite;
    }

    public void setAllowOverwrite(boolean allowOverwrite) {
        this.allowOverwrite = allowOverwrite;
    }

    public Operations getUploadOps() {
        return uploadOps;
    }

    public void setUploadOps(Operations uploadOps) {
        this.uploadOps = uploadOps;
    }

}
