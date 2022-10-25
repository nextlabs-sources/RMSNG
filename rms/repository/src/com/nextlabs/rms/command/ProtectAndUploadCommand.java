package com.nextlabs.rms.command;

import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.UploadFileResponse;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProtectAndUploadCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UploadFileResponse jsonResponse = new UploadFileResponse();
        RMSUserPrincipal userPrincipal = null;
        IRepository repository = null;
        String path = HTTPUtil.getInternalURI(request);
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePathId");
        String filePathDisplay = request.getParameter("filePathDisplay");
        String rightsGranted = request.getParameter("rightsGranted");
        String watermark = request.getParameter("watermark");
        String expiry = request.getParameter("expiry");
        String userConfirmedFileOverwriteStr = request.getParameter("userConfirmedFileOverwrite");
        if (!StringUtils.hasText(repoId) || !StringUtils.hasText(filePath) || !StringUtils.hasText(filePathDisplay) || !StringUtils.hasText(rightsGranted)) {
            jsonResponse.setError(RMSMessageHandler.getClientString("error.missing.parameter"));
            JsonUtil.writeJsonToResponse(jsonResponse, response);
            return;
        }
        try {
            if (StringUtils.hasText(expiry) && !ExpiryUtil.validateExpiry(expiry)) {
                jsonResponse.setError(RMSMessageHandler.getClientString("fileValidityErr"));
                JsonUtil.writeJsonToResponse(jsonResponse, response);
                return;
            }
        } catch (JsonSyntaxException e) {
            jsonResponse.setError(RMSMessageHandler.getClientString("err.invalid.params"));
            JsonUtil.writeJsonToResponse(jsonResponse, response);
            return;
        }
        String[] rightsGrantedList = rightsGranted.split(",");
        boolean userConfirmedFileOverwrite = false;
        if (StringUtils.hasText(userConfirmedFileOverwriteStr)) {
            userConfirmedFileOverwrite = Boolean.parseBoolean(userConfirmedFileOverwriteStr);
        }
        try (DbSession session = DbSession.newSession()) {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
        } catch (RepositoryException e1) {
            jsonResponse.setError(RMSMessageHandler.getClientString("inaccessibleRepository"));
            JsonUtil.writeJsonToResponse(jsonResponse, response);
            return;
        }
        File fileFromRepo = null;
        UploadedFileMetaData fileMeta;
        String fileName = null;
        try {
            DefaultRepositoryManager.checkVaultStorageExceeded(userPrincipal);
            String membershipId = SharedFileManager.getMembership(path, userPrincipal);
            File outputPath = RepositoryFileUtil.getTempOutputFolder();

            if (filePathDisplay.contains("/")) {
                fileName = filePathDisplay.substring(filePathDisplay.lastIndexOf('/') + 1, filePathDisplay.length());
            }
            Rights[] rights = SharedFileManager.toRights(rightsGrantedList);
            if (rights.length == 0) {
                jsonResponse.setError(RMSMessageHandler.getClientString("error.invalid.rights"));
                JsonUtil.writeJsonToResponse(jsonResponse, response);
                return;
            }
            fileMeta = RepositoryFileUtil.protectAndUpload(userPrincipal, repository, path, filePath, filePathDisplay, outputPath, membershipId, rights, watermark, expiry, userConfirmedFileOverwrite, request);
            fileFromRepo = fileMeta.getFile();
            jsonResponse.setName(fileMeta.getFileNameWithTimeStamp());
            jsonResponse.setProtectionType(ProtectionType.ADHOC.ordinal());
        } catch (InvalidTokenException e) {
            jsonResponse.setError(RMSMessageHandler.getClientString("invalidRepositoryToken"));
        } catch (UnauthorizedRepositoryException e) {
            jsonResponse.setError(RMSMessageHandler.getClientString("unauthorizedRepositoryAccess"));
        } catch (FileNotFoundException e) {
            jsonResponse.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
            jsonResponse.setError(RMSMessageHandler.getClientString("err.file.download.missing"));
        } catch (InSufficientSpaceException e) {
            jsonResponse.setError(RMSMessageHandler.getClientString("insufficientStorageError", RMSMessageHandler.getClientString("operationTypeProtect")));
        } catch (VaultStorageExceededException e) {
            jsonResponse.setError(RMSMessageHandler.getClientString("vaultStorageExceedException"));
        } catch (RMSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            jsonResponse.setError(e.getMessage());
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            jsonResponse.setError(RMSMessageHandler.getClientString("fileProtectErr", fileName));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            String error = RMSMessageHandler.getClientString("fileProtectErr", fileName);
            jsonResponse.setError(error);
        } finally {
            //need to delete entire folder
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            if (fileFromRepo != null && fileFromRepo.getParentFile() != null) {
                FileUtils.deleteQuietly(fileFromRepo.getParentFile());
            }
            JsonUtil.writeJsonToResponse(jsonResponse, response);
        }
    }
}
