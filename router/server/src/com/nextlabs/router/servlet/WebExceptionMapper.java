package com.nextlabs.router.servlet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.ExceptionMapper;

public class WebExceptionMapper implements ExceptionMapper<WebApplicationException> {

    public WebExceptionMapper() {
    }

    @Override
    public Response toResponse(WebApplicationException ex) {
        String message = ex.getMessage();
        int code = ex.getResponse().getStatus();
        return Response.status(new Status(code, message)).entity(message).build();
    }

    private static final class Status implements Response.StatusType {

        private int statusCod;
        private String reasonPhrase;
        private Family family;

        public Status(int statusCod, String reasonPhrase) {
            this(statusCod, reasonPhrase, Family.SERVER_ERROR);
        }

        public Status(int statusCod, String message, Family family) {
            this.statusCod = statusCod;
            this.reasonPhrase = message;
            this.family = family;
        }

        @Override
        public int getStatusCode() {
            return statusCod;
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
}
