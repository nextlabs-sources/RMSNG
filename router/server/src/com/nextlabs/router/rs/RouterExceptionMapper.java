package com.nextlabs.router.rs;

import com.nextlabs.router.servlet.WebExceptionMapper;

import javax.ws.rs.ext.Provider;

@Provider
public class RouterExceptionMapper extends WebExceptionMapper {

    public RouterExceptionMapper() {
    }
}
