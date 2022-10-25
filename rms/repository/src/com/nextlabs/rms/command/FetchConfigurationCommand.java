package com.nextlabs.rms.command;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.locale.RMSMessageHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FetchConfigurationCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        DbSession session = DbSession.newSession();
        try {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (!userPrincipal.isAdmin()) {
                response.sendError(403, RMSMessageHandler.getClientString("userNotAdmin"));
                return;
            }

            Tenant tenant = session.get(Tenant.class, userPrincipal.getLoginTenant());
            String preferences = tenant.getPreference();
            response.getWriter().write(preferences);
        } finally {
            session.close();
        }
    }
}
