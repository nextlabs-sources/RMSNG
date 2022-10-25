package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
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
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class GetFileDetailsCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int UNAUTHORIZED_REPOSITORY_STATUSCODE = 4003;
    private static final int FILE_NOT_FOUND_STATUSCODE = HttpServletResponse.SC_NOT_FOUND;
    private static final int FILE_UNABLE_DOWNLOAD_STATUSCODE = 4005;
    private static final int UNAUTHORIZED_ERROR_STATUSCODE = 5003;
    private static final int INVALID_NXL_STATUSCODE = 5007;
    private static final int GENERIC_ERROR_STATUSCODE = 0;

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
        IRepository repository = null;
        RMSUserPrincipal userPrincipal = null;
        NxlFile metaData = null;
        try {
            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
            } catch (RepositoryException e) {
                String error = RMSMessageHandler.getClientString("unauthorizedRepositoryAccess");
                OperationResult result = new OperationResult(UNAUTHORIZED_REPOSITORY_STATUSCODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            byte[] localFile;
            try {
                localFile = RepositoryFileUtil.downloadPartialFileFromRepo(repository, filePath, filePathDisplay);
                if (NxlFile.isNxl(localFile)) {
                    metaData = NxlFile.parse(localFile);
                } else if (StringUtils.hasText(filePathDisplay) && filePathDisplay.toLowerCase().endsWith(Constants.NXL_FILE_EXTN)) {
                    String error = RMSMessageHandler.getClientString("status_failure_invalid_nxl");
                    OperationResult result = new OperationResult(INVALID_NXL_STATUSCODE, false, error);
                    JsonUtil.writeJsonToResponse(result, response);
                    return;
                }
            } catch (FileNotFoundException fnfe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), fnfe.getMessage(), fnfe);
                }
                String error = RMSMessageHandler.getClientString("err.file.download.missing");
                OperationResult result = new OperationResult(FILE_NOT_FOUND_STATUSCODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (UnauthorizedRepositoryException ure) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), ure.getMessage(), ure);
                }
                String error = "Unauthorized repository";
                OperationResult result = new OperationResult(UNAUTHORIZED_REPOSITORY_STATUSCODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            } catch (RepositoryException re) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error downloading file '{}' from the repository (ID: {}): {}", filePath, repository.getRepoId(), re.getMessage(), re);
                }
                String error = RMSMessageHandler.getClientString("err.file.download");
                OperationResult result = new OperationResult(FILE_UNABLE_DOWNLOAD_STATUSCODE, false, error);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            Rights[] rightsList = null;
            boolean owner = true;
            boolean isNxl = metaData != null;
            boolean revoked = false;
            JsonExpiry validity = new JsonExpiry();

            if (isNxl) {
                try (DbSession session = DbSession.newSession()) {
                    Criteria cr = session.createCriteria(AllNxl.class);
                    cr.add(Restrictions.eq("duid", metaData.getDuid()));
                    AllNxl nxlFile = (AllNxl)cr.uniqueResult();
                    if (nxlFile != null) {
                        revoked = nxlFile.getStatus().equals(AllNxl.Status.REVOKED);
                        String path = HTTPUtil.getInternalURI(request);
                        owner = SharedFileManager.isOwner(path, metaData.getOwner(), userPrincipal);
                        if (!owner) {
                            if (!revoked) {
                                // querying Sharing_Recipient table to check if user is still a valid recipient of the NXL file
                                Criteria criteria = session.createCriteria(SharingRecipientPersonal.class);
                                criteria.add(Restrictions.eq("id.duid", metaData.getDuid()));
                                criteria.add(Restrictions.eq("id.email", userPrincipal.getEmail()).ignoreCase());
                                SharingRecipientPersonal recipient = (SharingRecipientPersonal)criteria.uniqueResult();
                                if (recipient == null) {
                                    String error = RMSMessageHandler.getClientString("status_failure_unauthorized_operation");
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(error + " File: " + filePath + " DUID: " + metaData.getDuid() + " User ID:" + userPrincipal.getUserId());
                                    }
                                    OperationResult result = new OperationResult(UNAUTHORIZED_ERROR_STATUSCODE, false, error);
                                    JsonUtil.writeJsonToResponse(result, response);
                                    return;
                                }
                                rightsList = Rights.fromInt(nxlFile.getPermissions());
                                String policy = nxlFile.getPolicy();
                                if (StringUtils.hasText(policy)) {
                                    FilePolicy filePolicy = GsonUtils.GSON.fromJson(policy, FilePolicy.class);
                                    rightsList = AdhocEvalAdapter.evaluate(filePolicy, true).getRights();
                                    validity = AdhocEvalAdapter.getFirstPolicyExpiry(filePolicy);
                                }
                                if (rightsList.length == 0) {
                                    String error = RMSMessageHandler.getClientString("status_failure_unauthorized_operation");
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Error: Empty Rights! File: " + filePath + " DUID: " + metaData.getDuid() + " User ID:" + userPrincipal.getUserId());
                                    }
                                    OperationResult result = new OperationResult(UNAUTHORIZED_ERROR_STATUSCODE, false, error);
                                    JsonUtil.writeJsonToResponse(result, response);
                                }
                            } else {
                                // if a file has been revoked by owner, there's no need to query Sharing_Recipient table
                                String error = RMSMessageHandler.getClientString("status_failure_unauthorized_operation");
                                if (logger.isDebugEnabled()) {
                                    logger.debug(error + " File: " + filePath + " DUID: " + metaData.getDuid() + " User ID:" + userPrincipal.getUserId());
                                }
                                OperationResult result = new OperationResult(UNAUTHORIZED_ERROR_STATUSCODE, false, error);
                                JsonUtil.writeJsonToResponse(result, response);
                            }
                        } else {
                            rightsList = Rights.fromInt(nxlFile.getPermissions());
                            String policy = nxlFile.getPolicy();
                            if (StringUtils.hasText(policy)) {
                                FilePolicy filePolicy = GsonUtils.GSON.fromJson(policy, FilePolicy.class);
                                validity = AdhocEvalAdapter.getFirstPolicyExpiry(filePolicy);
                            }
                        }
                    } else {
                        // DUID not present in AllNxl table meaning that the file's not a valid NXL
                        String error = RMSMessageHandler.getClientString("status_failure_unauthorized_operation");
                        if (logger.isDebugEnabled()) {
                            logger.debug(error + " File: " + filePath + " DUID: " + metaData.getDuid() + " User ID:" + userPrincipal.getUserId());
                        }
                        OperationResult result = new OperationResult(UNAUTHORIZED_ERROR_STATUSCODE, false, error);
                        JsonUtil.writeJsonToResponse(result, response);
                    }

                } finally {
                    IOUtils.closeQuietly(metaData);
                }
            } else {
                // This file is a native file
                rightsList = Rights.values();
            }

            String[] rights = SharedFileManager.toString(rightsList);
            FileDetails fileDetails = new FileDetails();
            if (metaData != null && metaData.getDuid() != null) {
                fileDetails.setDuid(metaData.getDuid());
            }
            fileDetails.setTags(Collections.emptyMap());
            fileDetails.setRights(rights);
            fileDetails.setOwner(owner);
            fileDetails.setNxl(isNxl);
            fileDetails.setRevoked(revoked);
            fileDetails.setValidity(validity);
            if (isNxl) {
                fileDetails.setProtectionType(ProtectionType.ADHOC.ordinal());
            }
            JsonUtil.writeJsonToResponse(fileDetails, response);
        } catch (NxlException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invalid NXL file (user ID: {}, repo ID: {}, file path: {})", userPrincipal.getUserId(), repoId, filePath);
            }
            String error = RMSMessageHandler.getClientString("status_failure_invalid_nxl");
            OperationResult result = new OperationResult(INVALID_NXL_STATUSCODE, false, error);
            JsonUtil.writeJsonToResponse(result, response);
        } catch (Throwable e) {
            String error = RMSMessageHandler.getClientString("status_error_generic");
            OperationResult result = new OperationResult(GENERIC_ERROR_STATUSCODE, false, error);
            logger.error(e.getMessage(), e);
            JsonUtil.writeJsonToResponse(result, response);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
        }
    }
}
