package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.service.ServiceProviderService;
import com.nextlabs.rms.serviceprovider.SupportedProvider;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.ServiceProviderValidationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller class that handles all REST API requests to /serviceprovider endpoints
 *
 * @author jmarthi
 */
@Path("/serviceprovider")
public class ServiceProviderMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private final ServiceProviderService serviceProviderService = new ServiceProviderService();
    private final ServiceProviderValidationUtil serviceProviderValidationUtil = new ServiceProviderValidationUtil();
    private static final String API = "API";

    private static final String VAL_ERR_MISSING_REQ_PARAMS = "Missing required parameters.";
    private static final String VAL_ERR_INVALID_TENANT = "Invalid tenant.";
    private static final String VAL_ERR_INVALID_ATTRIBUTE = "Invalid attributes for provider type.";
    private static Map<Integer, String> sharepointOnlineErrorCodeMap = new HashMap<Integer, String>();
    static {
        sharepointOnlineErrorCodeMap.put(1, "Invalid Application Credentials.");
        sharepointOnlineErrorCodeMap.put(2, "Invalid Hostname in Site Address.");
        sharepointOnlineErrorCodeMap.put(3, "Invalid Site Address.");
        sharepointOnlineErrorCodeMap.put(4, "Invalid Document Library Name.");
        sharepointOnlineErrorCodeMap.put(5, "Verify provided Repository details.");
    }

    /**
     * Lists all configured service providers or a tenant and provides a map of all supported providers and their names
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @return
     */
    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getServiceProviders(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {

        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            String loginTenant = us.getLoginTenant();
            if (loginTenant == null) {
                return new JsonResponse(4001, VAL_ERR_INVALID_TENANT).toJson();
            }

            Map<String, String> supportedProvidersMap = serviceProviderService.getProviderTypeDisplayNames();
            List<String> configuredServiceProviderList = serviceProviderService.getConfiguredServiceProviderNames(loginTenant);

            JsonResponse response = new JsonResponse("OK");
            response.putResult("configuredServiceProviderSettingList", configuredServiceProviderList);
            response.putResult("supportedProvidersMap", supportedProvidersMap);
            error = false;
            return response.toJson();

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "getServiceProviders", error ? 0 : 1, userId);
        }
    }

    /**
     * Provides a map of all supported providers and their details like name, class etc.
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @return
     */
    @Secured
    @GET
    @Path("/supported")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSupportedServiceProviders(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {

        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            Map<String, SupportedProvider> supportedProvidersMap = ServiceProviderService.getSupportedProviderMap();
            JsonResponse response = new JsonResponse("OK");
            response.putResult("supportedProvidersMap", supportedProvidersMap);
            error = false;
            return response.toJson();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "getServiceProvidersSettings", error ? 0 : 1, userId);
        }
    }

    /**
     * Provides a list of configured service providers with the configuration details and can be accessed only by tenant admin.
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @return
     */
    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/v2")
    public String getV2ServiceProviders(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {

        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            serviceProviderValidationUtil.validateAdminUser(us);
            List<ServiceProviderSetting> configuredServiceProviderSettingList = serviceProviderService.getConfiguredServiceProviders(us.getLoginTenant());

            JsonResponse response = new JsonResponse("OK");
            response.putResult("configuredServiceProviderSettingList", configuredServiceProviderSettingList);
            error = false;

            return response.toJson();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "getServiceProviders", error ? 0 : 1, userId);
        }
    }

    /**
     * Adds a service provider configuration . Accessible only by tenant admin users.
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @param json
     * @return
     */
    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addServiceProviderConfiguration(@Context HttpServletRequest request,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, VAL_ERR_MISSING_REQ_PARAMS).toJson();
            }

            ServiceProviderSetting serviceProviderSetting = req.getParameter("serviceProvider", ServiceProviderSetting.class);
            DeviceType deviceType = DeviceType.getDeviceType(platformId);

            if (!serviceProviderValidationUtil.validAddRequestAttributes(serviceProviderSetting, deviceType)) {
                return new JsonResponse(4002, VAL_ERR_INVALID_ATTRIBUTE).toJson();
            }
            if (ServiceProviderType.SHAREPOINT_ONLINE == serviceProviderSetting.getProviderType()) {
                int errorCode = serviceProviderValidationUtil.validateSharepointOnlineParameters(serviceProviderSetting);
                if (errorCode > 0) {
                    return new JsonResponse(4002, sharepointOnlineErrorCodeMap.get(errorCode)).toJson();
                }
            }

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            principal.setDeviceType(deviceType);
            serviceProviderValidationUtil.validateAdminUser(us);
            serviceProviderValidationUtil.validateUserBelongsToTenant(us, serviceProviderSetting);

            String serviceProviderId = serviceProviderService.addServiceProvider(serviceProviderSetting, principal);
            JsonResponse resp = new JsonResponse("Service Provider configured successfully.");
            resp.putResult("id", serviceProviderId);
            error = false;
            return resp.toJson();
        } catch (RepositoryAlreadyExists | DuplicateRepositoryNameException | BadRequestException
                | RepositoryException | ValidateException e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "addServiceProviderConfiguration", error ? 0 : 1, userId);
        }
    }

    /**
     * Deletes a service provider configuration . Accessible only by tenant admin users.
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @param json
     * @return
     */
    @Secured
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteServiceProviderConfiguration(@Context HttpServletRequest request,
        @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String serviceProviderId = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, VAL_ERR_MISSING_REQ_PARAMS).toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            serviceProviderValidationUtil.validateAdminUser(us);

            serviceProviderId = req.getParameter("id");
            if (!StringUtils.hasText(serviceProviderId)) {
                return new JsonResponse(400, VAL_ERR_MISSING_REQ_PARAMS).toJson();
            }

            serviceProviderService.deleteServiceProviderConfiguration(serviceProviderId);
            JsonResponse resp = new JsonResponse(204, "Service provider configuration deleted successfully");
            error = false;
            return resp.toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "deleteServiceProviderConfiguration", error ? 0 : 1, userId);
        }
    }

    /**
     * Updates a service provider configuration . Accessible only by tenant admin users.
     *
     * @param request
     * @param userId
     * @param ticket
     * @param clientId
     * @param platformId
     * @param json
     * @return
     */
    @Secured
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateServiceProviderConfiguration(@Context HttpServletRequest request,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, VAL_ERR_MISSING_REQ_PARAMS).toJson();
            }

            ServiceProviderSetting serviceProviderSetting = req.getParameter("serviceProvider", ServiceProviderSetting.class);
            DeviceType deviceType = DeviceType.getDeviceType(platformId);

            if (!StringUtils.hasText(serviceProviderSetting.getId())) {
                return new JsonResponse(400, VAL_ERR_MISSING_REQ_PARAMS).toJson();
            }

            if (!serviceProviderValidationUtil.validAddRequestAttributes(serviceProviderSetting, deviceType)) {
                return new JsonResponse(4002, VAL_ERR_INVALID_ATTRIBUTE).toJson();
            }
            if (ServiceProviderType.SHAREPOINT_ONLINE == serviceProviderSetting.getProviderType()) {
                int errorCode = serviceProviderValidationUtil.validateSharepointOnlineParameters(serviceProviderSetting);
                if (errorCode > 0) {
                    return new JsonResponse(4002, sharepointOnlineErrorCodeMap.get(errorCode)).toJson();
                }
            }

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            principal.setDeviceType(deviceType);
            serviceProviderValidationUtil.validateAdminUser(us);
            serviceProviderValidationUtil.validateUserBelongsToTenant(us, serviceProviderSetting);

            String serviceProviderId = serviceProviderService.updateServiceProvider(serviceProviderSetting, principal);
            JsonResponse resp = new JsonResponse("Service Provider updated successfully.");
            resp.putResult("id", serviceProviderId);
            error = false;
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, API, ServiceProviderMgmt.class.getSimpleName(), "updateServiceProviderConfiguration", error ? 0 : 1, userId);
        }
    }

}
