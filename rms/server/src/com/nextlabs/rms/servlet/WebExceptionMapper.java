package com.nextlabs.rms.servlet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class WebExceptionMapper implements ExceptionMapper<WebApplicationException> {

    public WebExceptionMapper() {
    }

    @Override
    public Response toResponse(WebApplicationException ex) {
        String message = ex.getMessage();
        int code = ex.getResponse().getStatus();
        return Response.status(new CustomStatus(code, message)).entity(message).build();
    }
}
