package com.nextlabs.rms.util;

import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.repository.RestUploadRequest;

public class SharedWorkspaceUtil {

    private static final String VAL_ERR_MISSING_REQ_PARAMS = "Missing required parameters.";
    private static final String VAL_ERR_INVALID_PATH_PARAM = "Invalid path parameter";
    private static final String VAL_ERR_INVALID_REPO_ID = "Invalid repo Id ";

    public void validateInputsForGetFiles(String path, String sharedRepoId) {
        if (StringUtils.hasText(path) && !path.startsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (!StringUtils.hasText(sharedRepoId)) {
            throw new ValidateException(4001, VAL_ERR_INVALID_REPO_ID);
        }
    }

    public void validateInputsForDownloadFile(JsonRequest jsonRequest, String repoId) {
        if (jsonRequest == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!StringUtils.hasText(jsonRequest.getParameter("path"))) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!jsonRequest.getParameter("path").startsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (jsonRequest.getParameter("path").endsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (!StringUtils.hasText(repoId)) {
            throw new ValidateException(4001, VAL_ERR_INVALID_REPO_ID);
        }
    }

    public static void validateInputsForProtectFiles(JsonRequest jsonRequest, String repoId) {

        if (jsonRequest == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!StringUtils.hasText(jsonRequest.getParameter("path"))) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!StringUtils.hasText(jsonRequest.getParameter("protectionType"))) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }

        int protectionType = jsonRequest.getIntParameter("protectionType", -1);
        if (protectionType < 0 || protectionType > 1) {
            throw new ValidateException(4001, "Invalid protectionType");
        }

        if (!jsonRequest.getParameter("path").startsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (jsonRequest.getParameter("path").endsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (!StringUtils.hasText(repoId)) {
            throw new ValidateException(4001, VAL_ERR_INVALID_REPO_ID);
        }
    }

    public static void validateInputsForGetFileInfoCheckFileExists(JsonRequest jsonRequest, String repoId) {
        if (jsonRequest == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!StringUtils.hasText(jsonRequest.getParameter("path"))) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (!jsonRequest.getParameter("path").startsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (jsonRequest.getParameter("path").endsWith("/")) {
            throw new ValidateException(4001, VAL_ERR_INVALID_PATH_PARAM);
        }
        if (!StringUtils.hasText(repoId)) {
            throw new ValidateException(4001, VAL_ERR_INVALID_REPO_ID);
        }
    }

    public static void validateInputsForUploadFile(String repoId, RestUploadRequest uploadReq) {
        if (!StringUtils.hasText(repoId)) {
            throw new ValidateException(4001, VAL_ERR_INVALID_REPO_ID);
        }
        JsonRequest jsonRequest = JsonRequest.fromJson(uploadReq.getJson());
        if (jsonRequest == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        String fileName = jsonRequest.getParameter("name");
        String fileParentPathId = jsonRequest.getParameter("parentPathId");

        if (!StringUtils.hasText(fileName) || !StringUtils.hasText(fileParentPathId)) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }

        if (!fileParentPathId.startsWith("/")) {
            throw new ValidateException(4001, "Invalid parent folder.");
        }

        if (fileName.length() > Constants.MAX_FILE_NAME_LENGTH) {
            throw new ValidateException(4005, "File name cannot exceed " + Constants.MAX_FILE_NAME_LENGTH + " characters");
        }
        if (!StringUtils.endsWithIgnoreCase(fileName, com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            throw new ValidateException(5010, "NXL file must have .nxl file extension.");
        }
        if (uploadReq.getFileStream() == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }

        int uploadTypeOrdinal = jsonRequest.getIntParameter("type", com.nextlabs.common.shared.Constants.SharedWorkSpaceUploadType.UPLOAD_SYSBUCKET.ordinal());

        if (uploadTypeOrdinal < 0 || uploadTypeOrdinal > 4) {
            throw new ValidateException(400, "Missing/Wrong upload type.");
        }

        if (uploadTypeOrdinal == com.nextlabs.common.shared.Constants.SharedWorkSpaceUploadType.UPLOAD_EDIT.ordinal() && Boolean.parseBoolean(jsonRequest.getParameter("userConfirmedFileOverwrite"))) {
            throw new ValidateException(400, "Invalid request parameters");
        }
    }

}
