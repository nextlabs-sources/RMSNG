package com.nextlabs.rms.command;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.json.RepoUrl;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetRepoAuthLinkCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try (DbSession session = DbSession.newSession()) {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        String contextPath = request.getContextPath();
        String repoType = request.getParameter("repoType");
        RepoUrl url = null;
        if (!StringUtils.hasText(repoType)) {
            OperationResult result = new OperationResult();
            result.setResult(false);
            result.setMessage(RMSMessageHandler.getClientString("error.missing.parameter"));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } else if (ServiceProviderType.DROPBOX.name().equalsIgnoreCase(repoType)) {
            url = new RepoUrl(contextPath + '/' + RepoConstants.DROPBOX_AUTH_START_URL);
        } else if (ServiceProviderType.GOOGLE_DRIVE.name().equalsIgnoreCase(repoType)) {
            url = new RepoUrl(contextPath + '/' + RepoConstants.GOOGLE_DRIVE_AUTH_START_URL);
        } else if (ServiceProviderType.ONE_DRIVE.name().equalsIgnoreCase(repoType)) {
            url = new RepoUrl(contextPath + '/' + RepoConstants.ONE_DRIVE_AUTH_START_URL);
        } else if (ServiceProviderType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(repoType)) {
            url = new RepoUrl(contextPath + '/' + RepoConstants.SHAREPOINT_ONLINE_AUTH_START_URL);
        } else {
            OperationResult result = new OperationResult();
            result.setResult(false);
            String repoTypeDisplayName = null;
            try {
                ServiceProviderType.valueOf(repoType.toUpperCase(Locale.US));
                repoTypeDisplayName = ServiceProviderSetting.getProviderTypeDisplayName(repoType);
            } catch (IllegalArgumentException e) {
                repoTypeDisplayName = repoType;
            }
            result.setMessage(RMSMessageHandler.getClientString("errAddRepo", repoTypeDisplayName));
            JsonUtil.writeJsonToResponse(result, response);
            logger.error("Unrecognized Repo: {}", repoType);
            return;
        }
        JsonUtil.writeJsonToResponse(url, response);
    }
}
