package com.nextlabs.rms.rs;

import com.box.sdk.BoxAPIConnection;
import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.JsonParseException;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.pojo.SyncProfileDataContainer;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.box.BoxOAuthHandler;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.dropbox.DropBoxOAuthHandler;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.repository.googledrive.GoogleDriveOAuthHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler.GrantType;
import com.nextlabs.rms.repository.onedrive.OneDriveOAuthHandler.OneDriveAppInfo;
import com.nextlabs.rms.repository.onedrive.OneDriveTokenResponse;
import com.nextlabs.rms.repository.sharepoint.SharePointRepoAuthHelper;
import com.nextlabs.rms.repository.sharepoint.response.SharePointTokenResponse;
import com.nextlabs.rms.service.ServiceProviderService;
import com.nextlabs.rms.serviceprovider.SupportedProvider.ProviderClass;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/repository")
public class RepositoryMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final List<ServiceProviderType> SUPPORTED_PROVIDER_TYPE = Arrays.asList(new ServiceProviderType[] {
        ServiceProviderType.DROPBOX,
        ServiceProviderType.GOOGLE_DRIVE, ServiceProviderType.ONE_DRIVE, ServiceProviderType.SHAREPOINT_ONLINE,
        ServiceProviderType.BOX });
    private static final int MAX_REPOSITORY_NAME_LENGTH = 40;

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addRepository(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        ServiceProviderType type = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            String repository = req.getParameter("repository");
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (!StringUtils.hasText(repository) || deviceType == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            JsonRepository jsonRepo = GsonUtils.GSON.fromJson(repository, JsonRepository.class);
            try {
                type = ServiceProviderType.valueOf(jsonRepo.getType());
            } catch (IllegalArgumentException e) {
                return new JsonResponse(400, "Invalid Storage Provider").toJson();
            }
            if (type == DefaultRepositoryManager.getDefaultServiceProvider()) {
                return new JsonResponse(403, "Operation not permitted").toJson();
            }
            if (!validateRepoName(jsonRepo.getName())) {
                return new JsonResponse(4003, "Repository Name contains illegal special characters").toJson();
            }

            if (jsonRepo.getProviderClass().equals(ProviderClass.APPLICATION.name()) || ServiceProviderService.getSupportedProviderMap().get(type.name()).getProviderClass().equals(ProviderClass.APPLICATION)) {
                return new JsonResponse(403, "Operation not permitted").toJson();
            }

            if (jsonRepo.getName().length() > MAX_REPOSITORY_NAME_LENGTH) {
                return new JsonResponse(4001, "Repository Name Too Long").toJson();
            }
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                ServiceProviderSetting provider = SettingManager.getStorageProviderSettings(session, tenant.getId(), type);
                if (provider == null) {
                    return new JsonResponse(400, "Invalid Storage Provider Setting").toJson();
                }
                session.beginTransaction();
                Repository repo = new Repository();
                if (type == ServiceProviderType.SHAREPOINT_ONPREMISE) {
                    repo.setAccountId(jsonRepo.getAccountName());
                } else {
                    repo.setAccountId(jsonRepo.getAccountId());
                }
                repo.setName(jsonRepo.getName());
                repo.setProviderId(provider.getId());
                repo.setUserId(userId);
                repo.setShared(jsonRepo.isShared() ? 1 : 0);
                repo.setAccountName(jsonRepo.getAccountName());
                if (deviceType.isIOS()) {
                    repo.setIosToken(jsonRepo.getToken());
                } else if (deviceType.isAndroid()) {
                    repo.setAndroidToken(jsonRepo.getToken());
                } else {
                    repo.setToken(jsonRepo.getToken());
                }
                if (jsonRepo.getCreationTime() != null) {
                    repo.setCreationTime(new Date(jsonRepo.getCreationTime()));
                } else {
                    repo.setCreationTime(new Date());
                }

                repo = RepositoryManager.addRepository(session, new RMSUserPrincipal(userId, tenant.getId(), ticket, clientId), repo);
                JsonResponse resp = new JsonResponse("Repository successfully added");
                resp.putResult("repoId", repo.getId());
                error = false;
                return resp.toJson();
            }
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (DuplicateRepositoryNameException de) {
            return new JsonResponse(409, "There is already a repository with the given name").toJson();
        } catch (RepositoryAlreadyExists e) {
            return new JsonResponse(304, "Repository already exists").toJson();
        } catch (BadRequestException e) {
            return new JsonResponse(400, "Repository name is not valid").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "addRepository", error ? 0 : 1, userId, type);
        }
    }

    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getRepositories(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        boolean error = true;

        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (deviceType == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                SyncProfileDataContainer container = RepositoryManager.getSyncDataUpdatedOnOrAfter(session, new RMSUserPrincipal(us, tenant), null, deviceType);
                JsonResponse resp = new JsonResponse("OK");

                resp.putResult("isFullCopy", container.isFullCopy());
                resp.putResult("repoItems", container.getRepositoryJsonList());
                if (!container.isFullCopy()) {
                    resp.putResult("deletedRepoItems", container.getDeletedRepositoryList());
                }
                error = false;
                return resp.toJson();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "getRepositories", error ? 0 : 1, userId);
        }
    }

    @Secured
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String removeRepository(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String repoId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            repoId = req.getParameter("repoId");
            if (!StringUtils.hasText(repoId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                session.beginTransaction();
                RepositoryManager.removeRepository(session, new RMSUserPrincipal(us, tenant), repoId);
                JsonResponse resp = new JsonResponse(204, "Repository successfully removed");
                error = false;
                return resp.toJson();
            }
        } catch (RepositoryNotFoundException e) {
            return new JsonResponse(404, "Repository does not exist").toJson();
        } catch (UnauthorizedOperationException | ForbiddenOperationException e) {
            return new JsonResponse(403, "Operation not permitted").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "removeRepository", error ? 0 : 1, userId, repoId);
        }
    }

    @Secured
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateRepository(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String repoId = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            repoId = req.getParameter("repoId");
            String name = req.getParameter("name");
            String token = req.getParameter("token");
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (!StringUtils.hasText(repoId) || !StringUtils.hasText(name) && !StringUtils.hasText(token) || deviceType == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                RMSUserPrincipal userPrincipal = new RMSUserPrincipal(us, tenant);
                session.beginTransaction();
                if (StringUtils.hasText(name)) {
                    if (!validateRepoName(name)) {
                        return new JsonResponse(4003, "Repository Name contains illegal special characters").toJson();
                    }
                    if (name.length() > MAX_REPOSITORY_NAME_LENGTH) {
                        return new JsonResponse(4001, "Repository Name Too Long").toJson();
                    }
                    RepositoryManager.updateRepositoryName(session, userPrincipal, repoId, name);
                }
                if (StringUtils.hasText(token)) {
                    RepositoryManager.updateClientToken(session, userPrincipal, repoId, token, deviceType);
                }
                session.commit();
                JsonResponse resp = new JsonResponse("Repository successfully updated");
                error = false;
                return resp.toJson();
            }
        } catch (DuplicateRepositoryNameException de) {
            return new JsonResponse(409, "There is already a repository with the given name").toJson();
        } catch (UnauthorizedOperationException | ForbiddenOperationException de) {
            return new JsonResponse(403, "Operation not permitted").toJson();
        } catch (RepositoryNotFoundException e) {
            return new JsonResponse(404, "Repository does not exist").toJson();
        } catch (BadRequestException e) {
            return new JsonResponse(400, "Repository name is not valid").toJson();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "updateRepository", error ? 0 : 1, userId, repoId);
        }
    }

    @Secured
    @GET
    @Path("/{repoId}/accessToken")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAccessToken(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @PathParam("repoId") String repositoryId) {
        boolean error = true;
        ServiceProviderType serviceProviderType = null;
        Repository repository = null;
        try {
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (!StringUtils.hasText(repositoryId) || deviceType == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            Tenant tenant = AbstractLogin.getDefaultTenant();
            if (tenant == null) {
                return new JsonResponse(400, "Invalid tenant").toJson();
            }
            StorageProvider storageProvider = null;
            try (DbSession session = DbSession.newSession()) {
                repository = RepositoryManager.getRepository(session, repositoryId);
                if (repository == null) {
                    return new JsonResponse(404, "Not found.").toJson();
                }
                if (repository.getUserId() != userId) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }
                String providerId = repository.getProviderId();
                storageProvider = RepositoryManager.getStorageProvider(session, providerId);
                if (storageProvider == null) {
                    return new JsonResponse(404, "Not found.").toJson();
                }
            }
            serviceProviderType = ServiceProviderType.getByOrdinal(storageProvider.getType());
            if (!SUPPORTED_PROVIDER_TYPE.contains(serviceProviderType)) {
                return new JsonResponse(5001, "Not supported storage provider.").toJson();
            }
            String refreshToken = repository.getToken();
            String attributes = storageProvider.getAttributes();
            if (!StringUtils.hasText(attributes)) {
                return new JsonResponse(5002, "Missing storage provider attributes.").toJson();
            }
            Map<String, Object> map = GsonUtils.GSON.fromJson(attributes, GsonUtils.GENERIC_MAP_TYPE);
            String appId = (String)map.get(ServiceProviderSetting.APP_ID);
            String appSecret = (String)map.get(ServiceProviderSetting.APP_SECRET);
            String redirectURL = (String)map.get(ServiceProviderSetting.REDIRECT_URL);
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(redirectURL)) {
                return new JsonResponse(5003, "Storage provider is not configured.").toJson();
            }
            String accessToken = null;
            if (serviceProviderType == ServiceProviderType.DROPBOX) {
                //This is just dummy call to check whether the token is still valid
                DropBoxOAuthHandler.getSpaceUsage(refreshToken);
                accessToken = refreshToken;
            } else if (serviceProviderType == ServiceProviderType.GOOGLE_DRIVE) {
                ServiceProviderSetting setting = SettingManager.toServiceProviderSetting(storageProvider);
                Credential credential = GoogleDriveOAuthHandler.getAccessTokenFromRefreshToken(setting, refreshToken);
                accessToken = credential.getAccessToken();
            } else if (serviceProviderType == ServiceProviderType.ONE_DRIVE) {
                OneDriveAppInfo info = new OneDriveAppInfo(appId, appSecret, redirectURL);
                OneDriveTokenResponse tokenResponse = OneDriveOAuthHandler.getAccessToken(info, refreshToken, GrantType.REFRESH_TOKEN);
                accessToken = tokenResponse.getAccessToken();
            } else if (serviceProviderType == ServiceProviderType.SHAREPOINT_ONLINE) {
                String contextId = (String)map.get(ServiceProviderSetting.SP_ONLINE_APP_CONTEXT_ID);
                String accountName = repository.getAccountName();
                if (!StringUtils.hasText(contextId)) {
                    return new JsonResponse(5004, "Missing Context ID.").toJson();
                }
                SharePointTokenResponse tokenResponse = SharePointRepoAuthHelper.getAccessTokenFromRefreshToken(appSecret, contextId, refreshToken, accountName);
                accessToken = tokenResponse.getAccessToken();
            } else if (serviceProviderType == ServiceProviderType.BOX) {
                String state = repository.getState();
                BoxAPIConnection connection = null;
                if (!StringUtils.hasText(state)) {
                    connection = new BoxAPIConnection(appId, appSecret, null, refreshToken);
                    BoxOAuthHandler.updateState(repositoryId, userId, connection.save(), connection.getRefreshToken());
                } else {
                    connection = BoxAPIConnection.restore(appId, appSecret, state);
                    if (connection.needsRefresh()) {
                        connection.refresh();
                        BoxOAuthHandler.updateState(repositoryId, userId, connection.save(), connection.getRefreshToken());
                    }
                }
                accessToken = connection.getAccessToken();
            }
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("accessToken", accessToken);
            error = false;
            return resp.toJson();
        } catch (UnauthorizedRepositoryException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Refresh token is expired (repository ID: {}, user ID: {}): {}", repositoryId, userId, e.getMessage(), e);
            }
            JsonResponse resp = new JsonResponse(5005, "Not authorized or expired.");
            String url = RepositoryManager.getAuthorizationUrl(request, serviceProviderType, repository, true);
            if (url != null) {
                resp.putResult("authURL", url);
            }
            try {
                RepositoryManager.setCookieRedirectParameters(response, repository.getAccountId());
            } catch (UnsupportedEncodingException e1) {
                LOGGER.warn("Unable to set cookie parameter (repository ID: {}, account ID: {}): {}", repository.getId(), repository.getAccountId(), e1.getMessage(), e1);
            }
            return resp.toJson();
        } catch (Exception e) {
            LOGGER.error("Error occurred when getting access token (repository ID: {}, user ID: {}): {}", repositoryId, userId, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "getAccessToken", !error ? 1 : 0, userId, repositoryId);
        }
    }

    private boolean validateRepoName(String repoName) {
        Matcher matcher = RegularExpressions.REPO_NAME_PATTERN.matcher(repoName);
        return matcher.matches();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/authURL")
    public String getAuthURL(@Context HttpServletRequest request, @Context HttpServletResponse response, String json) {
        boolean error = true;
        String name = null;
        String redirect = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            String contextPath = request.getContextPath();
            String type = req.getParameter("type");
            name = req.getParameter("name");
            redirect = req.getParameter("redirect");
            String platformId = req.getParameter("platformId");

            if (type == null || type.isEmpty() || name == null || name.isEmpty()) {
                return new JsonResponse(400, "Missing parameters").toJson();
            }
            StringBuilder urlBuilder = new StringBuilder(contextPath).append('/');
            DeviceType deviceType = null;
            if (StringUtils.hasText(platformId)) {
                deviceType = DeviceType.getDeviceType(Integer.parseInt(platformId));
                if (deviceType == null) {
                    return new JsonResponse(400, "Invalid device type").toJson();
                }
            }
            if (redirect == null) {
                if (deviceType != null && (deviceType.isAndroid() || deviceType.isIOS())) {
                    urlBuilder.append("custom/");
                } else {
                    urlBuilder.append("json/");
                }
            }
            if (ServiceProviderType.DROPBOX.name().equalsIgnoreCase(type)) {
                urlBuilder.append(RepoConstants.DROPBOX_AUTH_START_URL).append('?');
            } else if (ServiceProviderType.GOOGLE_DRIVE.name().equalsIgnoreCase(type)) {
                urlBuilder.append(RepoConstants.GOOGLE_DRIVE_AUTH_START_URL).append('?');
            } else if (ServiceProviderType.ONE_DRIVE.name().equalsIgnoreCase(type)) {
                urlBuilder.append(RepoConstants.ONE_DRIVE_AUTH_START_URL).append('?');
            } else if (ServiceProviderType.BOX.name().equalsIgnoreCase(type)) {
                urlBuilder.append(RepoConstants.BOX_AUTH_START_URL).append('?');
            } else if (ServiceProviderType.SHAREPOINT_ONLINE.name().equalsIgnoreCase(type)) {
                urlBuilder.append(RepoConstants.SHAREPOINT_ONLINE_AUTH_START_URL);
                String site = req.getParameter("siteURL");
                if (site == null || site.isEmpty()) {
                    return new JsonResponse(400, "Missing parameters").toJson();
                }
                urlBuilder.append("?siteName=").append(HTTPUtil.encode(site)).append('&');
                if ("true".equals(req.getParameter("isShared"))) {
                    urlBuilder.append("isShared=true&");
                }
            } else {
                return new JsonResponse(5001, "Invalid Storage Provider").toJson();
            }
            urlBuilder.append("name=").append(HTTPUtil.encode(name));
            JsonResponse res = new JsonResponse("OK");
            res.putResult("authURL", urlBuilder.toString());
            error = false;
            return res.toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "RepositoryMgmt", "authURL", !error ? 1 : 0);
        }
    }
}
