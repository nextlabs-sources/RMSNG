package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Operations;
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
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadFileCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String filePathDisplay = request.getParameter("filePathDisplay");

        RMSUserPrincipal principal = null;
        IRepository repo = null;
        String path = HTTPUtil.getInternalURI(request);
        RMSUserPrincipal repoOwner = null;
        try (DbSession session = DbSession.newSession()) {
            principal = authenticate(session, request);
            if (principal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            repoOwner = RepositoryFactory.getInstance().getRepoOwner(session, principal, repoId);
            repo = RepositoryFactory.getInstance().getRepository(session, repoOwner, repoId);
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid repository: {}", e.getMessage(), e);
            }
            redirectToErrorPage(request, response, "status_error_repo_not_found");
            return;
        }
        File outputPath = RepositoryFileUtil.getTempOutputFolder();

        File fileFromRepo = null;
        boolean error = true;
        try {
            fileFromRepo = RepositoryFileUtil.downloadFileFromRepo(repo, filePath, filePathDisplay, outputPath);
            error = false;
        } catch (FileNotFoundException e) {
            redirectToErrorPage(request, response, "err.file.download.missing");
            return;
        } catch (RepositoryException re) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error downloading file {} from the repository: {}", filePath, re.getMessage(), re);
            }
            redirectToErrorPage(request, response, "err.file.dowload");
            return;
        } catch (Exception e) {
            logger.error("Error downloading file {} from the repository: {}", filePath, e.getMessage(), e);
            redirectToErrorPage(request, response, "err.file.dowload");
            return;
        } finally {
            if (error) {
                FileUtils.deleteQuietly(outputPath);
            }
        }

        if (fileFromRepo == null || !fileFromRepo.exists()) {
            logger.error("File {} could not be obtained from the repository.", filePath);
            redirectToErrorPage(request, response, "fileNotFound");
            return;
        }
        boolean isNXLFile = false;
        try (InputStream fis = new FileInputStream(fileFromRepo)) {
            final int length = NxlFile.BASIC_HEADER_SIZE;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(fis, os, 0, length, length);
            byte[] nxlHeader = os.toByteArray();
            isNXLFile = NxlFile.isNxl(nxlHeader);
        }
        if (isNXLFile) {
            String deviceId = principal.getDeviceId();
            Integer platformId = principal.getPlatformId();
            String duid = null;
            String ownership = null;
            boolean allowToDownload = false;
            error = false;
            try (InputStream fis = new FileInputStream(fileFromRepo);
                    NxlFile nxl = NxlFile.parse(fis)) {
                duid = nxl.getDuid();
                ownership = nxl.getOwner();
                //try to obtain token
                try {
                    byte[] token = DecryptUtil.requestToken(path, principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), nxl.getDuid(), nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
                    boolean validToken = nxl.isValid(token);
                    boolean owner = SharedFileManager.isOwner(path, nxl.getOwner(), principal);
                    FilePolicy policy = DecryptUtil.getFilePolicy(nxl, token);
                    EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, owner);
                    Rights[] rights = evalResponse.getRights();
                    allowToDownload = validToken && (owner || hasDownloadRights(rights));
                } catch (IOException | NxlException | GeneralSecurityException e) {
                    allowToDownload = false;
                    error = true;
                }
                AccessResult accessResult = allowToDownload ? Constants.AccessResult.ALLOW : Constants.AccessResult.DENY;
                RMSRestHelper.sendActivityLogToRMS(path, principal.getTicket(), duid, ownership, principal.getUserId(), principal.getClientId(), deviceId, platformId, Operations.DOWNLOAD, repoId, filePath, filePathDisplay, accessResult, request, null, null, AccountType.PERSONAL);
                if (!allowToDownload) {
                    redirectToErrorPage(request, response, "error.download.no.permission");
                    return;
                }
            } catch (NxlException e) {
                logger.debug("Error parsing NXL file {} from the repository: {}", filePath, e.getMessage(), e);
                redirectToErrorPage(request, response, "err.file.dowload");
                error = true;
                return;
            } catch (JsonException e) {
                if (e.getStatusCode() == 403) {
                    RMSRestHelper.sendActivityLogToRMS(path, principal.getTicket(), duid, ownership, principal.getUserId(), principal.getClientId(), deviceId, platformId, Operations.DOWNLOAD, repoId, filePath, filePathDisplay, Constants.AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                    redirectToErrorPage(request, response, "status_failure_unathorized_operation");
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error downloading file {} from the repository: {}, status: {}", filePath, e.getMessage(), e.getStatusCode(), e);
                    }
                    redirectToErrorPage(request, response, "err.file.download");
                }
                error = true;
                return;
            } finally {
                if (!allowToDownload || error) {
                    FileUtils.deleteQuietly(fileFromRepo.getParentFile());
                }
            }
        } else {
            if (repoOwner.getUserId() != principal.getUserId()) {
                try {
                    logger.error("User {} trying to download file {} from repository: {}", principal.getEmail(), fileFromRepo.getName(), repoId);
                    redirectToErrorPage(request, response, "error.download.no.permission");
                    return;
                } finally {
                    FileUtils.deleteQuietly(fileFromRepo.getParentFile());
                }
            }
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", HTTPUtil.getContentDisposition(fileFromRepo.getName()));
        try (InputStream fis = new FileInputStream(fileFromRepo)) {
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } finally {
            FileUtils.deleteQuietly(fileFromRepo.getParentFile());
            if (repo instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repo));
            }
        }
    }

    private boolean hasDownloadRights(Rights[] rights) {
        if (rights != null) {
            for (Rights r : rights) {
                if (r == Rights.DOWNLOAD) {
                    return true;
                }
            }
        }
        return false;
    }

    private void redirectToErrorPage(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        String redirectURL = request.getContextPath() + "/error?code=" + code;
        if (!response.isCommitted()) {
            response.sendRedirect(response.encodeURL(redirectURL));
        }
    }
}
