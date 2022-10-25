package com.nextlabs.rms.command;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserPreferences;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdatePrefCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String LANDING_PAGE = "LANDING_PAGE";

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        DbSession session = DbSession.newSession();
        OperationResult result = new OperationResult();
        try {
            session.beginTransaction();
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String prefName = request.getParameter("prefName");

            UserPreferences userPreferences = session.get(UserPreferences.class, userPrincipal.getUserId());

            Map<String, Object> userPrefsMap = GsonUtils.GSON.fromJson(userPreferences.getPreferences(), GsonUtils.GENERIC_MAP_TYPE);

            if (userPrefsMap == null) {
                userPrefsMap = new HashMap<String, Object>();
            }

            if (LANDING_PAGE.equals(prefName)) {
                String landingPage = GetInitSettingsCommand.getLandingPage(userPrincipal, session);
                userPrefsMap.put(Constants.USER_PREF_LANDING_PAGE, landingPage);
            }
            userPreferences.setPreferences(GsonUtils.GSON.toJson(userPrefsMap));
            session.save(userPreferences);
            session.commit();
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("success_update_user_profile"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("error_update_user_profile"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } finally {
            session.close();
        }
    }
}
