package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.share.IShareService;
import com.nextlabs.rms.share.ShareServiceImpl;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.Audit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sharedWithMe")
public class SharedWithMeMgmt {

    private static final String PARAM_SHARE_WITH = "shareWith";
    private static final String PARAM_TRANSACTION_ID = "transactionId";
    private static final String PARAM_TRANSACTION_CODE = "transactionCode";
    private static final String PARAM_VIEW = "forViewer";
    private static final String VALIDATE_ONLY = "validateOnly";
    private static final String PARAM_COMMENT = "comment";

    private static final String PARAM_SPACE_ID = "spaceId";
    private static final String PARAM_RECIPIENTS = "recipients";
    private IShareService shareService;

    @Secured
    @GET
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String listFiles(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("q") String searchFields, @QueryParam("searchString") String searchString,
        @QueryParam("fromSpace") Integer fromSpace, @QueryParam("spaceId") String spaceId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            Object spaceIdParam;
            fromSpace = Nvl.nvl(fromSpace);
            Constants.SHARESPACE shareSpace = Constants.SHARESPACE.values()[fromSpace];
            shareService = new ShareServiceImpl(principal, shareSpace);
            switch (shareSpace) {
                case PROJECTSPACE:
                    spaceIdParam = Integer.valueOf(spaceId);
                    break;
                case ENTERPRISESPACE:
                    spaceIdParam = spaceId;
                    break;
                default:
                    spaceIdParam = principal.getUserId();
                    break;
            }
            JsonResponse response = shareService.listSharedWithMeFiles(request, page, size, orderBy, searchString, spaceIdParam);
            error = response.hasError();
            return response.toJson();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "list", error ? 0 : 1, userId);
        }
    }

    @Secured
    @GET
    @Path("/metadata/{transactionId}/{transactionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId,
        @PathParam("transactionId") String transactionId, @PathParam("transactionCode") String transactionCode,
        @QueryParam("spaceId") String spaceId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (SharedFileManager.isValidURLAccess(transactionId, transactionCode)) {
                RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
                shareService = new ShareServiceImpl(transactionId, principal, request);
                JsonResponse response = shareService.getSharedWithMeFileMetadata(request, transactionId, spaceId);
                error = response.hasError();
                return response.toJson();
            } else {
                return new JsonResponse(400, "Transaction ID and code mismatch").toJson();
            }
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "getMetadata", error ? 0 : 1, userId, transactionId);
        }
    }

    @Secured
    @POST
    @Path("/reshare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reshare(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, @NotNull String json) {
        boolean error = true;
        String transactionId = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            transactionId = req.getParameter(PARAM_TRANSACTION_ID);
            String transactionCode = req.getParameter(PARAM_TRANSACTION_CODE);
            if (SharedFileManager.isValidURLAccess(transactionId, transactionCode)) {
                RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
                SharingMgmt.setDeviceInUserPrincipal(deviceId, platformId, principal, request);

                String comment = req.getParameter(PARAM_COMMENT);
                Integer spaceId = req.getParameter(PARAM_SPACE_ID, Integer.class);
                Boolean validateOnly = req.getParameter(VALIDATE_ONLY, Boolean.class);
                if (validateOnly == null || !validateOnly) {
                    reShareRequestCheck(req);
                }
                shareService = new ShareServiceImpl(transactionId, principal, request);
                if (validateOnly != null && validateOnly) {
                    JsonResponse response = shareService.reshare(transactionId, null, null, true, request, spaceId);
                    error = response.hasError();
                    return response.toJson();
                }
                List<JsonSharing.JsonRecipient> recipientsListAll = getAllRecipients(req);
                JsonResponse response = shareService.reshare(transactionId, comment, recipientsListAll, false, request, spaceId);
                error = response.hasError();
                return response.toJson();
            } else {
                return new JsonResponse(400, "Transaction ID and code mismatch").toJson();
            }
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJsonResponse(new JsonResponse(), e).toJson();
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "reshare", error ? 0 : 1, userId, transactionId);
        }
    }

    private List<JsonRecipient> getAllRecipients(JsonRequest req) {
        List<JsonSharing.JsonRecipient> recipientsList = new ArrayList<>();
        List<JsonWraper> recipients = req.getParameterAsList(PARAM_RECIPIENTS);
        String paramShareWith = req.getParameter(PARAM_SHARE_WITH);
        List<String> shareWith = StringUtils.tokenize(paramShareWith, ",");
        if (shareWith != null && !shareWith.isEmpty()) {
            shareWith.forEach(email -> recipientsList.add(JsonSharing.emailToJsonRecipient(email)));
        }

        if (recipients != null && recipientsList.isEmpty()) {
            recipients = new ArrayList<>(new LinkedHashSet<>(recipients));
            for (JsonWraper wraper : recipients) {
                JsonSharing.JsonRecipient recipient = wraper.getAsObject(JsonSharing.JsonRecipient.class);
                recipientsList.add(recipient);
            }
        }

        return recipientsList;
    }

    private void reShareRequestCheck(JsonRequest req) throws ValidateException {
        if (req == null) {
            throw new ValidateException(400, "Missing required parameter");
        }
        String comment = req.getParameter(PARAM_COMMENT);
        List<JsonWraper> recipientsAll = req.getParameterAsList(PARAM_RECIPIENTS);
        String paramShareWith = req.getParameter(PARAM_SHARE_WITH);
        if (StringUtils.hasText(comment) && comment.length() > 250) {
            throw new ValidateException(400, "Comment too long");
        }

        if (!StringUtils.hasText(paramShareWith) && (recipientsAll == null || recipientsAll.isEmpty())) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        List<String> shareWith = StringUtils.tokenize(paramShareWith, ",");
        if (shareWith != null && !shareWith.isEmpty() && !EmailUtils.validateEmails(shareWith)) {
            throw new ValidateException(400, "Invalid emails");
        }
    }

    @Secured
    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response download(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @NotNull String json) {
        boolean error = true;

        JsonRequest req = JsonRequest.fromJson(json);
        String transactionId = req.getParameter(PARAM_TRANSACTION_ID);
        String transactionCode = req.getParameter(PARAM_TRANSACTION_CODE);
        Boolean forViewer = req.getParameter(PARAM_VIEW, Boolean.class);
        boolean downloadForView = forViewer != null && forViewer;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!SharedFileManager.isValidURLAccess(transactionId, transactionCode)) {
                JsonResponse resp = new JsonResponse(400, "Invalid transactionCode.");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
            }
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            SharingMgmt.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            shareService = new ShareServiceImpl(transactionId, principal, request);
            int start = req.getIntParameter("start", -1);
            long length = req.getLongParameter("length", -1L);
            Integer spaceId = StringUtils.hasText(req.getParameter(PARAM_SPACE_ID)) ? req.getParameter(PARAM_SPACE_ID, Integer.class) : null;
            Response resp = shareService.downloadSharedWithMeFile(response, transactionId, start, length, downloadForView, spaceId, request);
            error = resp.getStatus() != Response.Status.OK.getStatusCode();
            return resp;
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "download", error ? 0 : 1, transactionId);
        }
    }

    @Secured
    @POST
    @Path("/decrypt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response decrypt(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        boolean error = true;
        JsonRequest req = JsonRequest.fromJson(json);
        String transactionId = req.getParameter(PARAM_TRANSACTION_ID);
        String transactionCode = req.getParameter(PARAM_TRANSACTION_CODE);
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!SharedFileManager.isValidURLAccess(transactionId, transactionCode)) {
                JsonResponse resp = new JsonResponse(400, "Invalid transactionCode.");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
            }
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            SharingMgmt.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            shareService = new ShareServiceImpl(transactionId, principal, request);
            String spaceId = req.getParameter("spaceId");
            Response resp = shareService.decryptSharedWithMeFile(request, response, transactionId, spaceId);
            error = resp.getStatus() != Response.Status.OK.getStatusCode();
            return resp;
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "decrypt", error ? 0 : 1, transactionId);
        }
    }

    @Secured
    @POST
    @Path("/fileHeader")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response downloadHeader(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("deviceId") String deviceId, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId,
        @NotNull String json) {
        boolean error = true;

        JsonRequest req = JsonRequest.fromJson(json);
        String transactionId = req.getParameter(PARAM_TRANSACTION_ID);
        String transactionCode = req.getParameter(PARAM_TRANSACTION_CODE);
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!SharedFileManager.isValidURLAccess(transactionId, transactionCode)) {
                JsonResponse resp = new JsonResponse(400, "Invalid transactionCode.");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
            }
            RMSUserPrincipal principal = new RMSUserPrincipal(us, AbstractLogin.getDefaultTenant());
            SharingMgmt.setDeviceInUserPrincipal(deviceId, platformId, principal, request);
            shareService = new ShareServiceImpl(transactionId, principal, request);
            int start = 0;
            long length = 16384;
            Integer spaceId = StringUtils.hasText(req.getParameter(PARAM_SPACE_ID)) ? req.getParameter(PARAM_SPACE_ID, Integer.class) : null;
            Response resp = shareService.downloadSharedWithMeFileHeader(response, transactionId, start, length, spaceId, request);
            error = resp.getStatus() != Response.Status.OK.getStatusCode();
            return resp;
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            Audit.audit(request, "API", "sharedWithMeMgmt", "downloadHeader", error ? 0 : 1, transactionId);
        }
    }

}
