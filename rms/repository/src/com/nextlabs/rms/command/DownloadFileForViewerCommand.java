package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LocalizationUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadFileForViewerCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String repoId = request.getParameter("repoId");
        String filePath = request.getParameter("filePath");
        String filePathDisplay = request.getParameter("filePathDisplay");

        RMSUserPrincipal userPrincipal = null;
        RMSUserPrincipal ownerPrincipal = null;
        IRepository repo = null;
        try (DbSession session = DbSession.newSession()) {
            userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            ownerPrincipal = RepositoryFactory.getInstance().getRepoOwner(session, userPrincipal, repoId);
            repo = RepositoryFactory.getInstance().getRepository(session, ownerPrincipal, repoId);
            if (repo.isShared()) {
                repo.setUser(userPrincipal);
            }
        } catch (RepositoryException e) {
            logger.error("Invalid repository: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        File outputPath = RepositoryFileUtil.getTempOutputFolder();
        InputStream is = null;
        try {
            File downloadedFile = RepositoryFileUtil.downloadFileFromRepo(repo, filePath, filePathDisplay, outputPath);
            boolean isRepoOwner = ownerPrincipal.getUserId() == userPrincipal.getUserId();
            boolean fromMyDrive = repo.getRepoType() == DefaultRepositoryManager.getDefaultServiceProvider();
            boolean isNXLFile = downloadedFile.getName().toLowerCase().endsWith(Constants.NXL_FILE_EXTN);
            if (!isRepoOwner && !repo.isShared() && !isNXLFile) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, LocalizationUtil.getMessage(request, "err.file.dowload", null, Locale.getDefault()));
                return;
            }
            is = new FileInputStream(downloadedFile);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", HTTPUtil.getContentDisposition(downloadedFile.getName()));
            response.setHeader("isRepoOwner", String.valueOf(isRepoOwner));
            response.setHeader("fromMyDrive", String.valueOf(fromMyDrive));
            long size = downloadedFile.length();
            response.setContentLength(size <= Integer.MAX_VALUE ? (int)size : -1);
            try (ServletOutputStream out = response.getOutputStream()) {
                IOUtils.copy(is, out);
                out.flush();
            }
        } catch (InvalidTokenException e) {
            response.sendError(400, LocalizationUtil.getMessage(request, "invalidRepositoryToken", null, Locale.getDefault()));
            return;
        } catch (UnauthorizedRepositoryException e) {
            response.sendError(403, LocalizationUtil.getMessage(request, "unauthorizedRepositoryAccess", null, Locale.getDefault()));
            return;
        } catch (FileNotFoundException e) {
            response.sendError(404, LocalizationUtil.getMessage(request, "err.file.download.missing", null, Locale.getDefault()));
            return;
        } catch (RepositoryException re) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error downloading file {} from the repository: {}", filePath, re.getMessage(), re);
            }
            String error = LocalizationUtil.getMessage(request, "err.file.dowload", null, Locale.getDefault());
            int code = 520;
            response.sendError(code, error);
            return;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            String error = LocalizationUtil.getMessage(request, "err.file.dowload", null, Locale.getDefault());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
            return;
        } finally {
            IOUtils.closeQuietly(is);
            if (repo instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repo));
            }
            if (outputPath.exists()) {
                FileUtils.deleteDirectory(outputPath);
            }
        }
    }
}
