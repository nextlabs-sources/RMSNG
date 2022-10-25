package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CheckSharedRightCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String filePathDisplay = request.getParameter("filePathDisplay");
        String fileName = request.getParameter("fileName");
        JsonExpiry validity = new JsonExpiry();
        RMSUserPrincipal userPrincipal;
        IRepository repository = null;
        JsonResponse resp;
        boolean owner;
        boolean nxl = false;
        Operations operation = Operations.SHARE;
        try {
            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
            }
            Rights[] grantedRights;
            if (fileName.toLowerCase().endsWith(Constants.NXL_FILE_EXTN)) {
                byte[] downloadedFileBytes = RepositoryFileUtil.downloadPartialFileFromRepo(repository, filePath, filePathDisplay);
                NxlFile metadata = NxlFile.parse(downloadedFileBytes);
                try (DbSession session = DbSession.newSession()) {
                    Membership membership = session.get(Membership.class, metadata.getOwner());
                    if (StringUtils.hasText(membership.getTenant().getParentId())) {
                        OperationResult result = new OperationResult();
                        result.setResult(false);
                        result.setMessage(RMSMessageHandler.getClientString("fileUploadTenantMismatchErr"));
                        JsonUtil.writeJsonToResponse(result, response);
                        return;
                    }
                }
                String rmsURL = HTTPUtil.getInternalURI(request);
                owner = SharedFileManager.isOwner(rmsURL, metadata.getOwner(), userPrincipal);
                if (!owner) {
                    operation = Operations.RESHARE;
                }
                String duid = metadata.getDuid();

                try {
                    byte[] token = DecryptUtil.requestToken(rmsURL, userPrincipal.getUserId(), userPrincipal.getTicket(), userPrincipal.getClientId(), userPrincipal.getPlatformId(), userPrincipal.getTenantName(), metadata.getOwner(), duid, metadata.getRootAgreement(), metadata.getMaintenanceLevel(), metadata.getProtectionType(), DecryptUtil.getFilePolicyStr(metadata, null), DecryptUtil.getTagsString(metadata, null));
                    if (!metadata.isValid(token)) {
                        RMSRestHelper.sendActivityLogToRMS(rmsURL, userPrincipal.getTicket(), metadata.getDuid(), metadata.getOwner(), userPrincipal.getUserId(), userPrincipal.getClientId(), operation, repoId, filePath, filePath, AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                        resp = new JsonResponse(HttpServletResponse.SC_UNAUTHORIZED, RMSMessageHandler.getClientString("status_failure_unauthorized_operation"));
                        JsonUtil.writeJsonToResponse(resp, response);
                        return;
                    }
                    FilePolicy policy = DecryptUtil.getFilePolicy(metadata, token);
                    EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, owner);
                    grantedRights = evalResponse.getRights();
                    if (policy != null) {
                        validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                    }
                    if (!owner && !hasShareRights(grantedRights)) {
                        RMSRestHelper.sendActivityLogToRMS(rmsURL, userPrincipal.getTicket(), metadata.getDuid(), metadata.getOwner(), userPrincipal.getUserId(), userPrincipal.getClientId(), operation, repoId, filePath, filePath, AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                        OperationResult result = new OperationResult();
                        result.setResult(false);
                        result.setMessage(RMSMessageHandler.getClientString("status_failure_unauthorized_operation"));
                        JsonUtil.writeJsonToResponse(result, response);
                        return;
                    }
                } catch (GeneralSecurityException | IOException | NxlException e) {
                    RMSRestHelper.sendActivityLogToRMS(rmsURL, userPrincipal.getTicket(), metadata.getDuid(), metadata.getOwner(), userPrincipal.getUserId(), userPrincipal.getClientId(), operation, repoId, filePath, filePath, AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                    resp = new JsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RMSMessageHandler.getClientString("status_failure_unauthorized_operation"));
                    JsonUtil.writeJsonToResponse(resp, response);
                    return;
                } catch (JsonException e) {
                    RMSRestHelper.sendActivityLogToRMS(rmsURL, userPrincipal.getTicket(), metadata.getDuid(), metadata.getOwner(), userPrincipal.getUserId(), userPrincipal.getClientId(), operation, repoId, filePath, filePath, AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                    resp = new JsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RMSMessageHandler.getClientString(e.getStatusCode() == 403 || e.getStatusCode() == 404 ? "status_failure_unauthorized_operation" : "status_error_generic"));
                    JsonUtil.writeJsonToResponse(resp, response);
                    return;
                } finally {
                    IOUtils.closeQuietly(metadata);
                }
                nxl = true;
            } else {
                owner = true;
                grantedRights = Rights.values();
            }
            String[] list = SharedFileManager.toString(grantedRights);
            resp = new JsonResponse("OK");
            resp.putResult("r", list);
            resp.putResult("o", owner);
            resp.putResult("nxl", nxl);
            resp.putResult("mode", SharingMode.LINK);
            resp.putResult("expiry", validity);
            resp.putResult("protectionType", ProtectionType.ADHOC.ordinal());
            JsonUtil.writeJsonToResponse(resp, response);
        } catch (FileNotFoundException e) {
            resp = new JsonResponse(HttpServletResponse.SC_NOT_FOUND, RMSMessageHandler.getClientString("err.file.download.missing"));
            JsonUtil.writeJsonToResponse(resp, response);
        } catch (NxlException e) {
            resp = new JsonResponse(HttpServletResponse.SC_FORBIDDEN, RMSMessageHandler.getClientString("status_failure_invalid_nxl"));
            JsonUtil.writeJsonToResponse(resp, response);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            resp = new JsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RMSMessageHandler.getClientString("status_error_generic"));
            JsonUtil.writeJsonToResponse(resp, response);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
        }
    }

    private boolean hasShareRights(Rights[] rights) {
        if (rights != null) {
            for (Rights r : rights) {
                if (r == Rights.SHARE) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum SharingMode {
        ATTACHMENT,
        LINK,
        NONE
    }
}
