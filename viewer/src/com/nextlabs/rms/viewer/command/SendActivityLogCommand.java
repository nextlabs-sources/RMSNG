package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.ViewerUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendActivityLogCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String duid = request.getParameter("duid");
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String ticket = request.getParameter("ticket");
        String userId = request.getParameter("userId");
        String ownerId = request.getParameter("ownerId");
        String clientId = request.getParameter("clientId");
        String deviceId = request.getParameter("deviceId");
        String platform = request.getParameter("platformId");
        boolean isProjectFile = Boolean.parseBoolean(request.getParameter("isProjectFile"));
        AccountType type = isProjectFile ? AccountType.PROJECT : AccountType.PERSONAL;
        deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
        Integer platformId = StringUtils.hasText(platform) && org.apache.commons.lang3.StringUtils.isNumeric(platform) ? Integer.parseInt(platform) : null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding activity logs for file ID:" + filePath);
            }
            String ownerTenantName = StringUtils.substringAfter(ownerId, "@");
            String rmsURL = ViewerUtil.getRMSInternalURL(ownerTenantName);
            Properties prop = new Properties();
            prop.setProperty("client_id", ViewerConfigManager.getInstance().getClientId());
            RMSRestHelper.sendActivityLogToRMS(rmsURL, ticket, duid, ownerId, Integer.parseInt(userId), clientId, deviceId, platformId, Operations.PRINT, repoId, filePath, filePath, AccessResult.ALLOW, request, prop, null, type);
        } catch (IOException e) {
            LOGGER.error("Error occured while adding activity logs for file ID: " + filePath + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Error occured while adding activity logs for file ID: " + filePath + e.getMessage(), e);
        }
    }
}
