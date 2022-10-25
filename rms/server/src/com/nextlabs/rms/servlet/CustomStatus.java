package com.nextlabs.rms.servlet;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

public class CustomStatus implements Response.StatusType {

    private int statusCode;
    private String reasonPhrase;
    private Family family;

    public CustomStatus(int statusCode, String reasonPhrase) {
        this(statusCode, reasonPhrase, Family.SERVER_ERROR);
    }

    public CustomStatus(int statusCod, String message, Family family) {
        this.statusCode = statusCod;
        this.reasonPhrase = message;
        this.family = family;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public Family getFamily() {
        return family;
    }
}
