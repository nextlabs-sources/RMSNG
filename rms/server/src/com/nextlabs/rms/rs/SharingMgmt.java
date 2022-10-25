package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.share.IShareService;
import com.nextlabs.rms.share.ShareServiceImpl;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.RestUploadUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/share")
public class SharingMgmt {

    private IShareService shareService;

    @PostConstruct
    public void init() {
    }

    @Secured
    @POST
    @Path("/repository")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String shareRepository(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId,
        @NotNull String json) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonSharing shareReq = JsonRequest.fromJson(json).getParameter("sharedDocument", JsonSharing.class);
            shareRequestCheck(shareReq);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            setDeviceInUserPrincipal(deviceId, platformId, principal, request);

            shareService = new ShareServiceImpl(shareReq, principal, request);
            JsonResponse shareResp = shareService.share(request, shareReq);
            error = shareResp.hasError();
            return shareResp.toJson();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "ShareFile", "proxy", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/local")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String shareLocal(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId) {
        RestUploadRequest uploadReq = null;
        String fileName;
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            uploadReq = RestUploadUtil.parseRestUploadRequest(request);
            fileName = uploadReq.getFileName();
            File file = new File(uploadReq.getUploadDir(), fileName);
            Files.copy(uploadReq.getFileStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JsonSharing shareReq = JsonRequest.fromJson(uploadReq.getJson()).getParameter("sharedDocument", JsonSharing.class);
            shareRequestCheck(shareReq);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            setDeviceInUserPrincipal(deviceId, platformId, principal, request);

            shareService = new ShareServiceImpl(shareReq, file, principal, request);
            JsonResponse shareResp = shareService.share(request, shareReq);
            error = shareResp.hasError();
            return shareResp.toJson();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            RestUploadUtil.cleanupRestUploadResources(uploadReq);
            Audit.audit(request, "API", "ShareFile", "proxy", error ? 0 : 1, userId);
        }
    }

    @Secured
    @DELETE
    @Path("/{duid}/revoke")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String revoke(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId,
        @PathParam("duid") @NotNull String duid) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            shareService = new ShareServiceImpl(principal, duid);
            JsonResponse resp = shareService.revoke(request, duid);
            error = resp.hasError();
            return resp.toJson();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "SharingMgmt", "revoke", error ? 0 : 1, userId, duid);
        }
    }

    @Secured
    @POST
    @Path("/{duid}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("deviceId") String deviceId,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("duid") @NotNull String duid, @NotNull String json) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            List<JsonWraper> newRecipientList = req.getParameterAsList("newRecipients");
            List<JsonWraper> removedRecipientList = req.getParameterAsList("removedRecipients");
            String comment = req.getParameter("comment");

            if (StringUtils.hasText(comment) && comment.length() > 250) {
                return new JsonResponse(4007, "Comment too long").toJson();
            }

            List<JsonSharing.JsonRecipient> newRecipients = new ArrayList<>();
            List<JsonSharing.JsonRecipient> removedRecipients = new ArrayList<>();

            if (newRecipientList != null) {
                newRecipientList = new ArrayList<>(new LinkedHashSet<>(newRecipientList));
                for (JsonWraper wraper : newRecipientList) {
                    JsonSharing.JsonRecipient recipient = wraper.getAsObject(JsonSharing.JsonRecipient.class);
                    if (StringUtils.hasText(recipient.getEmail()) && !EmailUtils.validateEmail(recipient.getEmail())) {
                        return new JsonResponse(400, "One or more emails have an invalid format.").toJson();
                    }
                    newRecipients.add(recipient);
                }
            }
            if (removedRecipientList != null) {
                for (JsonWraper wraper : removedRecipientList) {
                    JsonSharing.JsonRecipient recipient = wraper.getAsObject(JsonSharing.JsonRecipient.class);
                    removedRecipients.add(recipient);
                }
            }
            if (newRecipients.isEmpty() && removedRecipients.isEmpty()) {
                return new JsonResponse(304, "No changes.").toJson();
            }
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            shareService = new ShareServiceImpl(principal, duid);
            JsonResponse response = shareService.update(request, duid, newRecipients, removedRecipients, comment);
            error = response.hasError();
            return response.toJson();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "SharingMgmt", "updateRecipients", error ? 0 : 1, userId, duid);
        }
    }

    private void shareRequestCheck(JsonSharing req) throws ValidateException {
        if (req == null) {
            throw new ValidateException(400, "Missing required parameter");
        }
        if (StringUtils.hasText(req.getComment()) && req.getComment().length() > 250) {
            throw new ValidateException(4007, "Comment too long");
        }
        if (!StringUtils.hasText(req.getMembershipId()) || req.getRecipients() == null || req.getRecipients().isEmpty()) {
            throw new ValidateException(400, "Missing required parameter");
        }
        if (req.getExpiry() != null && !ExpiryUtil.validateExpiry(req.getExpiry())) {
            throw new ValidateException(4008, "Invalid expiry parameter");
        }
        for (JsonSharing.JsonRecipient recipient : req.getRecipients()) {
            if (StringUtils.hasText(recipient.getEmail()) && !EmailUtils.validateEmail(recipient.getEmail())) {
                throw new ValidateException(400, "One or more emails have an invalid format.");
            }
        }
    }

    protected static void setDeviceInUserPrincipal(String deviceId, Integer platformId, RMSUserPrincipal principal,
        HttpServletRequest request) throws ValidateException {
        platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
        try {
            deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
        } catch (UnsupportedEncodingException e) { //NOPMD
        }
        DeviceType deviceType = DeviceType.getDeviceType(platformId);
        if (deviceType == null) {
            throw new ValidateException(400, "Unknown platform.");
        }
        principal.setDeviceId(deviceId);
        principal.setDeviceType(deviceType);
    }

}
