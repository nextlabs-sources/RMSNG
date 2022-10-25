package com.nextlabs.rms.command;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Feedback;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.mail.Mail;
import com.nextlabs.rms.mail.Sender;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SubmitFeedbackCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OperationResult result = new OperationResult();
        DbSession session = DbSession.newSession();
        RMSUserPrincipal userPrincipal = null;
        try {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } finally {
            session.close();
        }
        try {
            sendMail(request, userPrincipal);
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("feedbackMailSuccess"));
        } catch (Exception e) {
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("feedbackMailFail"));
        } finally {
            JsonUtil.writeJsonToResponse(result, response);
            saveFeedback(request, userPrincipal);
        }
    }

    private void saveFeedback(HttpServletRequest request, RMSUserPrincipal userPrincipal) {

        String feedbackType = request.getParameter("feedbackType");
        String summary = request.getParameter("feedbackSummary");
        String description = request.getParameter("feedbackDescription");
        //these 3 fields above cannot be empty from UI

        String deviceId = userPrincipal.getDeviceId();
        deviceId = StringUtils.hasText(deviceId) ? deviceId : HTTPUtil.getRemoteAddress(request);
        Feedback feedback = new Feedback();

        feedback.setType(feedbackType);
        feedback.setSummary(summary);
        feedback.setDescription(description);
        feedback.setClientId(userPrincipal.getClientId());
        feedback.setDeviceId(deviceId);
        feedback.setDeviceType(userPrincipal.getPlatformId());
        feedback.setUserId(userPrincipal.getUserId());
        feedback.setCreationTime(new Date());

        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            session.save(feedback);
            session.commit();
        }
    }

    private void sendMail(HttpServletRequest request, RMSUserPrincipal userPrincipal) {
        String feedbackType = request.getParameter("feedbackType");
        String summary = request.getParameter("feedbackSummary");
        String description = request.getParameter("feedbackDescription");
        Properties prop = new Properties();
        prop.setProperty(Mail.KEY_RECIPIENT, WebConfig.getInstance().getProperty(WebConfig.RMS_FEEDBACK_MAILID));
        prop.setProperty(Mail.FEEDBACK_TYPE, feedbackType);
        prop.setProperty(Mail.FEEDBACK_SUMMARY, summary);
        prop.setProperty(Mail.FEEDBACK_DESCRIPTION, description);
        prop.setProperty(Mail.FEEDBACK_USEREMAIL, userPrincipal.getEmail());
        prop.setProperty(Mail.BASE_URL, HTTPUtil.getURI(request));
        Locale locale = request.getLocale();
        Sender.send(prop, "feedback", locale);
    }
}
