package com.nextlabs.rms.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.JsonRepoFile;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.json.OperationResult;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteItemCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String PARAM_REPO_ID = "repoId";
    private static final String PARAM_FILE_PATH = "filePath";
    private static final String PARAM_FILE_PATH_DISPLAY = "filePathDisplay";
    private static final String PARAM_FILE_NAME = "fileName";
    private static final String PARAM_IS_FOLDER = "isFolder";
    private static final String LABEL_FOLDER = "folder";
    private static final String LABEL_FILE = "file";

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        OperationResult result = new OperationResult();
        result.setResult(false);

        List<String> missingParams = validateParameters(request);

        if (!missingParams.isEmpty()) {
            logger.debug("Missing parameters: " + StringUtils.join(missingParams));
            result.setMessage(RMSMessageHandler.getClientString("missingParamsMsg"));
            result.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        }

        String repoId = request.getParameter(PARAM_REPO_ID);
        String filePath = request.getParameter(PARAM_FILE_PATH);
        String filePathDisplay = request.getParameter(PARAM_FILE_PATH_DISPLAY);
        String fileName = request.getParameter(PARAM_FILE_NAME);
        boolean isFolder = Boolean.parseBoolean(request.getParameter(PARAM_IS_FOLDER));
        String fileType = isFolder ? LABEL_FOLDER : LABEL_FILE;

        DbSession session = DbSession.newSession();
        IRepository repository = null;
        try {
            RMSUserPrincipal userPrincipal = authenticate(session, request);

            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
            // reject request from MyVault
            if (filePath.startsWith(RepoConstants.MY_VAULT_FOLDER_PATH_ID) && DefaultRepositoryManager.isDefaultRepo(repository)) {
                logger.error("Unable to process request from MyVault");
                result.setMessage("Unable to process request from MyVault");
                result.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
                JsonUtil.writeJsonToResponse(result, response);
                return;
            }
            RepositoryFileUtil.deleteFileFromRepo(repository, filePath, filePathDisplay);
            boolean isDefaultRepo = DefaultRepositoryManager.isDefaultRepo(repository);
            String fileId = isDefaultRepo ? filePath : RepositoryFileManager.getFileId(filePath);
            if (isFolder) {
                RepositoryFileManager.unmarkFilesUnderFolder(session, repoId, fileId);
            } else {
                RepositoryFileManager.unmarkFilesAsFavorite(session, repoId, Arrays.asList(new JsonRepoFile(fileId, filePathDisplay)));
            }
            result.setResult(true);
            result.setMessage(RMSMessageHandler.getClientString("successDeleteMsg", fileType, fileName));
            if (isDefaultRepo) {
                Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(userPrincipal);
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                JsonElement jsonElement = gson.toJsonTree(result);
                jsonElement.getAsJsonObject().addProperty(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
                jsonElement.getAsJsonObject().addProperty(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
                jsonElement.getAsJsonObject().addProperty(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
                JsonUtil.writeJsonToResponse(jsonElement, response);
            } else {
                JsonUtil.writeJsonToResponse(result, response);
            }
        } catch (FileNotFoundException e) {
            result.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
            result.setMessage(RMSMessageHandler.getClientString("err.file.download.missing"));
            JsonUtil.writeJsonToResponse(result, response);
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            result.setMessage(getMessageByException(e));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            result.setMessage(getMessageByException(e));
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            session.close();
        }
    }

    private List<String> validateParameters(HttpServletRequest request) {
        List<String> missingParams = new ArrayList<String>();
        if (!StringUtils.hasText(request.getParameter(PARAM_REPO_ID))) {
            missingParams.add(PARAM_REPO_ID);
        }
        if (!StringUtils.hasText(request.getParameter(PARAM_FILE_PATH))) {
            missingParams.add(PARAM_FILE_PATH);
        }
        if (!StringUtils.hasText(request.getParameter(PARAM_FILE_PATH_DISPLAY))) {
            missingParams.add(PARAM_FILE_PATH_DISPLAY);
        }
        if (!StringUtils.hasText(request.getParameter(PARAM_FILE_NAME))) {
            missingParams.add(PARAM_FILE_NAME);
        }
        if (!StringUtils.hasText(request.getParameter(PARAM_IS_FOLDER))) {
            missingParams.add(PARAM_IS_FOLDER);
        }
        return missingParams;
    }

    private String getMessageByException(Throwable e) {
        String msg = RMSMessageHandler.getClientString("deleteErrMsg");
        if (e instanceof InvalidTokenException) {
            msg = RMSMessageHandler.getClientString("invalidRepositoryToken");
        } else if (e instanceof UnauthorizedRepositoryException) {
            msg = RMSMessageHandler.getClientString("unauthorizedRepositoryAccess");
        } else if (e instanceof FileNotFoundException) {
            msg = RMSMessageHandler.getClientString("fileNotFound");
        }
        return msg;
    }
}
