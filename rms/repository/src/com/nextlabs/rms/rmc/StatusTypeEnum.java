/**
 *
 */
package com.nextlabs.rms.rmc;

/**
 * @author nnallagatla
 *
 */
public enum StatusTypeEnum {
    SUCCESS(0, "status_success"),
    UNKNOWN(500, "status_error_generic"),
    USER_NOT_FOUND(1, "status_error_user_not_found"),
    REPO_NOT_FOUND(2, "status_error_repo_not_found"),
    REPO_ALREADY_EXISTS(3, "status_error_user_repo_already_exists"),
    DUPLICATE_REPO_NAME(4, "status_error_duplicate_repo_name"),
    UNSUPPORTED_STORAGE_PROVIDER(5, "status_error_unsupported_storage_provider"),
    BAD_REQUEST(400, "status_error_bad_request");

    private String description;
    private int code;

    private StatusTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessageLabel() {
        return description;
    }
}
