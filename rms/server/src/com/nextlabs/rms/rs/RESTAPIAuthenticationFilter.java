package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserSession;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.math.NumberUtils;

@Secured
@Provider
@Priority(1)
public class RESTAPIAuthenticationFilter implements ContainerRequestFilter {

    public static final String USERSESSION = "UserSession";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        int userId = NumberUtils.toInt(requestContext.getHeaderString("userId"));
        String ticket = requestContext.getHeaderString("ticket");
        String clientId = requestContext.getHeaderString("clientId");
        Integer platformId = null;
        if (StringUtils.hasText(requestContext.getHeaderString("platformId"))) {
            platformId = NumberUtils.toInt(requestContext.getHeaderString("platformId"));
        }
        if (userId < 0 || !StringUtils.hasText(ticket) || !StringUtils.hasText(clientId)) {
            String[] params = getParamsFromCookies(requestContext, "userId", "ticket", "clientId", "platformId");
            if (params != null) {
                userId = NumberUtils.toInt(params[0]);
                ticket = params[1];
                clientId = params[2];
                if (StringUtils.hasText(params[3])) {
                    platformId = NumberUtils.toInt(params[3]);
                }
            }
            if (userId < 0 || !StringUtils.hasText(ticket) || !StringUtils.hasText(clientId)) {
                requestContext.setProperty("error", new JsonResponse(401, "Missing login parameters").toJson());
                throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(new JsonResponse(401, "Missing Login parameters").toJson()).build());

            }
        }
        try (DbSession session = DbSession.newSession()) {
            UserSession us = UserMgmt.authenticate(session, userId, ticket, clientId, platformId);
            if (us == null) {
                throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(new JsonResponse(401, "Authentication failed").toJson()).build());
            } else {

                requestContext.setProperty(USERSESSION, us);
            }
        }

    }

    private static String[] getParamsFromCookies(ContainerRequestContext request, String... params) {
        if (params != null && params.length > 0) {
            Map<String, Cookie> cookies = request.getCookies();
            if (cookies != null && !cookies.isEmpty()) {
                String[] values = new String[params.length];
                for (int i = 0; i < params.length; i++) {
                    if (cookies.containsKey(params[i])) {
                        values[i] = cookies.get(params[i]).getValue();
                    }
                }
                return values;
            }
        }
        return null;
    }

}
