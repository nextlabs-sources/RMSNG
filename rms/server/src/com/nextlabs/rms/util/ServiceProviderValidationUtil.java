package com.nextlabs.rms.util;

import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.application.ApplicationRepositoryFactory;
import com.nextlabs.rms.application.sharepoint.SharePointApplicationRepository;
import com.nextlabs.rms.application.sharepoint.exception.DriveNotFoundException;
import com.nextlabs.rms.application.sharepoint.exception.InvalidHostNameException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointServiceException;
import com.nextlabs.rms.application.sharepoint.exception.SiteNotFoundException;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.shared.LogConstants;

import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceProviderValidationUtil {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public void validateAdminUser(UserSession us) {
        String loginTenant = us.getLoginTenant();
        if (loginTenant == null) {
            throw new ValidateException(400, "Invalid tenant.");
        }

        try (DbSession session = DbSession.newSession()) {
            if (!UserService.checkTenantAdmin(session, loginTenant, us.getUser().getId())) {
                throw new ValidateException(401, "Unauthorised.");
            }
        }

    }

    public void validateUserBelongsToTenant(UserSession us, ServiceProviderSetting serviceProviderSetting) {
        if (!us.getLoginTenant().equals(serviceProviderSetting.getTenantId())) {
            throw new ValidateException(401, "Unauthorised.");
        }
    }

    private static boolean validateDefaultOAuthSettings(ServiceProviderSetting serviceProviderSetting) {
        String appId = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String appSecret = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        String redirectUrl = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.REDIRECT_URL);
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret) && StringUtils.hasText(redirectUrl);
    }

    private static boolean validateApplicationClassOAuthSettings(
        ServiceProviderSetting serviceProviderSetting) {
        String appId = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_ID);
        String appSecret = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_SECRET);
        String appTenantId = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.APP_TENANT_ID);
        String displayNameRepository = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DISPLAY_NAME);
        String driveName = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DRIVE_NAME);
        String siteURL = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.SITE_URL);
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret) && StringUtils.hasText(appTenantId) && StringUtils.hasText(displayNameRepository) && StringUtils.hasText(driveName) && StringUtils.hasText(siteURL) && displayNameRepository.length() <= 40 && validateRepoName(displayNameRepository);
    }

    private static boolean validateRepoName(String repoName) {
        Matcher matcher = RegularExpressions.REPO_NAME_PATTERN.matcher(repoName);
        return matcher.matches();
    }

    public boolean validAddRequestAttributes(ServiceProviderSetting serviceProviderSetting, DeviceType deviceType) {

        if (serviceProviderSetting == null || deviceType == null || serviceProviderSetting.getAttributes() == null || serviceProviderSetting.getAttributes().isEmpty() || serviceProviderSetting.getTenantId() == null) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        if (serviceProviderSetting.getProviderType() == null) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        ServiceProviderType type = serviceProviderSetting.getProviderType();

        boolean validRequest = false;
        if (ServiceProviderType.BOX == type || ServiceProviderType.DROPBOX == type || ServiceProviderType.GOOGLE_DRIVE == type || ServiceProviderType.ONE_DRIVE == type) {
            validRequest = validateDefaultOAuthSettings(serviceProviderSetting);
        } else if (ServiceProviderType.SHAREPOINT_ONLINE == type) {
            validRequest = validateApplicationClassOAuthSettings(serviceProviderSetting);
        } else if (ServiceProviderType.SHAREPOINT_ONPREMISE == type) {
            validRequest = true;
        }

        return validRequest;
    }

    public int validateSharepointOnlineParameters(ServiceProviderSetting serviceProviderSetting) {
        String siteURL = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.SITE_URL);
        String driveName = serviceProviderSetting.getAttributes().get(ServiceProviderSetting.DRIVE_NAME);
        SharePointApplicationRepository repository = ApplicationRepositoryFactory.getInstance().createSharepointOnlineRepository(serviceProviderSetting);
        int errorCode = 0;
        try {
            repository.configDrive(siteURL, driveName);
        } catch (InvalidTokenException invalidTokenException) {
            LOGGER.error("Invalid App Credentials", invalidTokenException);
            errorCode = 1;
        } catch (InvalidHostNameException invalidHostNameException) {
            LOGGER.error("Invalid Hostname in Sharepoint URL: ", invalidHostNameException);
            errorCode = 2;
        } catch (SharePointServiceException | SiteNotFoundException sharepointSiteException) {
            LOGGER.error("Invalid Sharepoint Site URL: ", sharepointSiteException);
            errorCode = 3;
        } catch (DriveNotFoundException driveNotFoundException) {
            LOGGER.error("Invalid Drive Name: ", driveNotFoundException);
            errorCode = 4;
        } catch (Exception exception) {
            LOGGER.error("Verify provided Repository details: ", exception);
            errorCode = 5;
        }
        return errorCode;
    }

}
