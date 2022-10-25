package com.nextlabs.rms.repository.sharepoint.online;

import com.nextlabs.rms.repository.sharepoint.response.SharePointOAuthErrorResponse;

public class SharePointOnlineOAuthException extends RuntimeException {

    private static final long serialVersionUID = -6843483485930750332L;
    private final transient SharePointOAuthErrorResponse error;
    private final int statusCode;

    public SharePointOnlineOAuthException(int statusCode, String msg, SharePointOAuthErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public SharePointOAuthErrorResponse getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
