package com.nextlabs.rms.rs.dto;

import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.manager.SharedFileManager;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

public class ProtectSharedWorkspaceDTO {

    private static final String VAL_ERR_MISSING_REQ_PARAMS = "Missing required parameters.";
    private static final String INPUT_PARAM_PATH = "path";
    private static final String INPUT_PARAM_PROTECTION_TYPE = "protectionType";
    private static final String INPUT_PARAM_RIGHT = "rights";
    private static final String INPUT_PARAM_TAGS = "tags";
    private static final String INPUT_PARAM_EXPIRY = "expiry";
    private static final String INPUT_PARAM_WATERMARK = "watermark";
    private static final String INPUT_PARAM_OVERWRITE = "userConfirmedFileOverwrite";

    private String outputPath;
    private String repoId;
    private String path;
    private int protectionType;
    private boolean userConfirmedFileOverwrite;
    private Rights[] rights;
    private JsonExpiry expiry;
    private Map<String, String[]> tags;
    private String watermark;

    @SuppressWarnings("unchecked")
    public ProtectSharedWorkspaceDTO(String repoId, JsonRequest jsonRequest, String outputPath) {
        this.repoId = repoId;
        this.outputPath = outputPath;
        this.path = jsonRequest.getParameter(INPUT_PARAM_PATH);
        this.protectionType = jsonRequest.getParameter(INPUT_PARAM_PROTECTION_TYPE, Integer.class);
        String[] rightsList = jsonRequest.getParameter(INPUT_PARAM_RIGHT, String[].class);
        if (protectionType == 0 && (rightsList == null || rightsList.length < 1)) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }
        if (protectionType == 1 && jsonRequest.getParameter(INPUT_PARAM_TAGS, Map.class) == null) {
            throw new ValidateException(400, VAL_ERR_MISSING_REQ_PARAMS);
        }

        this.expiry = jsonRequest.getParameter(INPUT_PARAM_EXPIRY, JsonExpiry.class);
        if (this.expiry == null) {
            this.expiry = new JsonExpiry(0);
        }
        this.tags = jsonRequest.getParameter(INPUT_PARAM_TAGS, Map.class);
        this.rights = SharedFileManager.toRights(rightsList);
        if (ArrayUtils.contains(this.rights, Rights.WATERMARK)) {
            this.watermark = jsonRequest.getParameter(INPUT_PARAM_WATERMARK, String.class);
        }
        this.userConfirmedFileOverwrite = Boolean.parseBoolean(jsonRequest.getParameter(INPUT_PARAM_OVERWRITE));
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public boolean isUserConfirmedFileOverwrite() {
        return userConfirmedFileOverwrite;
    }

    public void setUserConfirmedFileOverwrite(boolean userConfirmedFileOverwrite) {
        this.userConfirmedFileOverwrite = userConfirmedFileOverwrite;
    }

    public Rights[] getRights() {
        return rights;
    }

    public void setRights(Rights[] rights) {
        this.rights = rights;
    }

    public JsonExpiry getExpiry() {
        return expiry;
    }

    public void setExpiry(JsonExpiry expiry) {
        this.expiry = expiry;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tags) {
        this.tags = tags;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

}
