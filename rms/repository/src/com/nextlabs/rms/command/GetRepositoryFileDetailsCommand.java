package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ExternalRepositoryNxl;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.json.FileDetails;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class GetRepositoryFileDetailsCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int UNAUTHORIZED_REPOSITORY_STATUS_CODE = 4003;
    private static final int FILE_NOT_FOUND_STATUS_CODE = HttpServletResponse.SC_NOT_FOUND;
    private static final int FILE_UNABLE_DOWNLOAD_STATUS_CODE = 4005;
    private static final int UNAUTHORIZED_ERROR_STATUS_CODE = 5003;
    private static final int INVALID_NXL_STATUS_CODE = 5007;
    private static final int INVALID_TOKEN_GROUP_STATUS_CODE = 5008;
    private static final int GENERIC_ERROR_STATUS_CODE = 0;

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String filePathDisplay = request.getParameter("filePathDisplay");
        if (!StringUtils.hasText(filePath) || !StringUtils.hasText(repoId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        IRepository repository;
        RMSUserPrincipal userPrincipal;
        NxlFile metaData = null;
        String membership = null;
        com.nextlabs.rms.eval.User user;

        try (DbSession session = DbSession.newSession()) {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            User loginUser = session.load(User.class, userPrincipal.getUserId());
            user = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(userPrincipal.getUserId())).ticket(userPrincipal.getTicket()).clientId(userPrincipal.getClientId()).platformId(userPrincipal.getPlatformId()).deviceId(userPrincipal.getDeviceId()).email(loginUser.getEmail()).displayName(loginUser.getDisplayName()).ipAddress(request.getRemoteAddr()).build();
            repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
        } catch (RepositoryException e) {
            String error = RMSMessageHandler.getClientString("unauthorizedRepositoryAccess");
            OperationResult result = new OperationResult(UNAUTHORIZED_REPOSITORY_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        }
        byte[] localFile;
        Tenant tenant = null;
        try {
            localFile = RepositoryFileUtil.downloadPartialFileFromRepo(repository, filePath, filePathDisplay);
            if (NxlFile.isNxl(localFile)) {
                metaData = NxlFile.parse(localFile);
                membership = metaData.getOwner();
            } else if (StringUtils.hasText(filePathDisplay) && filePathDisplay.toLowerCase().endsWith(Constants.NXL_FILE_EXTN)) {
                String error = RMSMessageHandler.getClientString("status_failure_invalid_nxl");
                OperationResult result = new OperationResult(INVALID_NXL_STATUS_CODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            String tokenGroupName = StringUtils.substringAfter(membership, "@");
            tenant = getTenantByTokenGroupName(tokenGroupName);
            if (tenant == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Invalid NXL file (user ID: {}, repo ID: {}, file path: {})", userPrincipal.getUserId(), repoId, filePath);
                }
                String error = RMSMessageHandler.getClientString("status_failure_invalid_token_group");
                OperationResult result = new OperationResult(INVALID_TOKEN_GROUP_STATUS_CODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
        } catch (FileNotFoundException fnfe) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), fnfe.getMessage(), fnfe);
            }
            String error = RMSMessageHandler.getClientString("err.file.download.missing");
            OperationResult result = new OperationResult(FILE_NOT_FOUND_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (UnauthorizedRepositoryException ure) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), ure.getMessage(), ure);
            }
            String error = "Unauthorized repository";
            OperationResult result = new OperationResult(UNAUTHORIZED_REPOSITORY_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (RepositoryException re) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), re.getMessage(), re);
            }
            String error = RMSMessageHandler.getClientString("err.file.download");
            OperationResult result = new OperationResult(FILE_UNABLE_DOWNLOAD_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (NxlException nxlException) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invalid NXL file (user ID: {}, repo ID: {}, file path: {})", userPrincipal.getUserId(), repoId, filePath);
            }
            String error = RMSMessageHandler.getClientString("status_failure_invalid_nxl");
            OperationResult result = new OperationResult(INVALID_NXL_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        }

        boolean isNxl = metaData != null;
        boolean revoked;
        JsonExpiry validity = new JsonExpiry();
        FileDetails fileDetails = new FileDetails();
        if (isNxl) {
            Rights[] rightsList = null;
            boolean owner;
            try (DbSession session = DbSession.newSession()) {
                Criteria cr = session.createCriteria(ExternalRepositoryNxl.class);
                cr.add(Restrictions.eq("duid", metaData.getDuid()));
                ExternalRepositoryNxl nxlFile = (ExternalRepositoryNxl)cr.uniqueResult();
                if (nxlFile != null) {
                    revoked = nxlFile.getStatus().equals(ExternalRepositoryNxl.Status.REVOKED);
                    String path = HTTPUtil.getInternalURI(request);
                    owner = SharedFileManager.isOwner(path, metaData.getOwner(), userPrincipal);

                    Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
                    criteria.add(Restrictions.eq("id.duid", metaData.getDuid()));
                    criteria.add(Restrictions.eq("id.email", userPrincipal.getEmail()).ignoreCase());
                    FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
                    List<FilePolicy.Policy> adhocPolicies = policy.getPolicies();

                    if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                        rightsList = Rights.fromInt(nxlFile.getPermissions());
                        validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                        fileDetails.setProtectionType(ProtectionType.ADHOC.ordinal());
                        fileDetails.setTags(Collections.emptyMap());
                    } else if (EvaluationAdapterFactory.isInitialized()) {
                        Map<String, String[]> tags = DecryptUtil.getTags(metaData, null);
                        if (!tags.isEmpty()) {
                            fileDetails.setTags(tags);
                        }
                        fileDetails.setProtectionType(ProtectionType.CENTRAL.ordinal());
                        List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(nxlFile.getFileName(), membership, tenant.getName(), user, tags);
                        EvalResponse evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                        rightsList = evalResponse.getRights();
                    }
                    String[] rights = SharedFileManager.toString(rightsList);

                    if (metaData.getDuid() != null) {
                        fileDetails.setDuid(metaData.getDuid());
                    }
                    fileDetails.setRights(rights);
                    fileDetails.setOwner(owner);
                    fileDetails.setNxl(true);
                    fileDetails.setRevoked(revoked);
                    fileDetails.setValidity(validity);
                    JsonUtil.writeJsonToResponse(fileDetails, response);

                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Invalid NXL file (user ID: {}, repo ID: {}, file path: {})", userPrincipal.getUserId(), repoId, filePath);
                    }
                }

            } catch (NxlException e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Invalid NXL file (user ID: {}, repo ID: {}, file path: {})", userPrincipal.getUserId(), repoId, filePath);
                }
                String error = RMSMessageHandler.getClientString("status_failure_invalid_nxl");
                OperationResult result = new OperationResult(INVALID_NXL_STATUS_CODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
            } catch (Throwable e) {
                String error = RMSMessageHandler.getClientString("status_error_generic");
                OperationResult result = new OperationResult(GENERIC_ERROR_STATUS_CODE, false, error);
                logger.error(e.getMessage(), e);
                JsonUtil.writeJsonToResponse(result, response);
            } finally {
                if (repository instanceof Closeable) {
                    IOUtils.closeQuietly(Closeable.class.cast(repository));
                }
                IOUtils.closeQuietly(metaData);
            }
        } else {
            String error = RMSMessageHandler.getClientString("status_failure_unauthorized_operation");
            if (logger.isDebugEnabled()) {
                logger.debug(error + " File: " + filePath + " DUID: " + null + " User ID:" + userPrincipal.getUserId());
            }
            OperationResult result = new OperationResult(UNAUTHORIZED_ERROR_STATUS_CODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
        }
    }

    public Tenant getTenantByTokenGroupName(String tokenGroupName) {
        if (tokenGroupName.contains(com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX)) {
            try (DbSession session = DbSession.newSession()) {
                String tenantName = StringUtils.substringBefore(tokenGroupName, com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX);
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", tenantName));
                return (Tenant)criteria.uniqueResult();
            }
        }
        return null;
    }

}
