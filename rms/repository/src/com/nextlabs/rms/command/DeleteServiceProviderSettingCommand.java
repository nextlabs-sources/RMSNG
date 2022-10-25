package com.nextlabs.rms.command;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author nnallagatla
 */
public class DeleteServiceProviderSettingCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!userPrincipal.isAdmin()) {
                response.sendError(403, RMSMessageHandler.getClientString("userNotAdmin"));
                return;
            }
            OperationResult result = new OperationResult();

            String spId = request.getParameter("serviceProviderId");
            StorageProvider sp = session.get(StorageProvider.class, spId);
            if (sp == null || !sp.getTenantId().equals(userPrincipal.getTenantId())) {
                result.setResult(false);
                result.setMessage(RMSMessageHandler.getClientString("error_service_provider_not_found"));
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            session.delete(sp);
            session.commit();

            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("success_delete_service_provider_setting"));
            JsonUtil.writeJsonToResponse(result, response);
        } finally {
            session.close();
        }
    }
}
