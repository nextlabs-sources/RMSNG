package com.nextlabs.rms.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.exception.DriveStorageExceededException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.VaultStorageExceededException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.exception.FileConflictException;
import com.nextlabs.rms.repository.exception.InSufficientSpaceException;
import com.nextlabs.rms.repository.exception.InvalidFileNameException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.NonUniqueFileException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.shared.ExpiryUtil;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.shared.UploadFileRequest;
import com.nextlabs.rms.shared.UploadFileResponse;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UploadFileCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        processReq(request, response);
    }

    public void processReq(HttpServletRequest request, HttpServletResponse response) {
        File tmpDir = null;
        UploadFileResponse uploadResponse = new UploadFileResponse();
        String fileName;
        UploadFileRequest fileRequest;
        RMSUserPrincipal userPrincipal;
        String filePathId;
        String filePathDisplay;
        String rightsJSON;
        String watermark;
        String expiry;
        IRepository repository = null;
        String repoId;
        UploadedFileMetaData fileMetaData;
        boolean userConfirmedFileOverwrite;
        try {
            tmpDir = RepositoryFileUtil.getTempOutputFolder();
            fileRequest = UploadUtil.readFile(request, Constants.FILE_UPLD_THRESHOLD_SIZE, Constants.FILE_UPLD_MAX_REQUEST_SIZE);
            repoId = fileRequest.getRepoId();
            filePathId = fileRequest.getFilePathId();
            filePathDisplay = fileRequest.getFilePathDisplay();
            rightsJSON = fileRequest.getRightsJSON();
            watermark = fileRequest.getWatermark();
            expiry = fileRequest.getExpiry();
            userConfirmedFileOverwrite = fileRequest.isUserConfirmedFileOverwrite();
            fileName = fileRequest.getFileName();
            if (!StringUtils.hasText(repoId) || !StringUtils.hasText(filePathId) || !StringUtils.hasText(filePathDisplay) || !StringUtils.hasText(rightsJSON)) {
                logger.error("Missing parameter");
                deleteTmpDir(tmpDir);
                uploadResponse.setError(RMSMessageHandler.getClientString("fileUploadErr"));
                JsonUtil.writeJsonToResponse(uploadResponse, response);
                return;
            }
            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                repository = RepositoryFactory.getInstance().getRepository(session, userPrincipal, repoId);
                if (DefaultRepositoryManager.getDefaultServiceProvider() == repository.getRepoType()) {
                    DefaultRepositoryManager.checkStorageExceeded(userPrincipal);
                } else {
                    DefaultRepositoryManager.checkVaultStorageExceeded(userPrincipal);
                }

            }

            byte[] bytes = fileRequest.getBytes();
            String destFilePath = tmpDir + File.separator + fileName;
            File file = UploadUtil.writeFileToDisk(bytes, destFilePath);
            if (NxlFile.isNxl(file)) {
                uploadResponse.setError(RMSMessageHandler.getClientString("nxlFileUploadNotAllowed"));
                JsonUtil.writeJsonToResponse(uploadResponse, response);
                return;
            }
            String[] rightsArray = new Gson().fromJson(rightsJSON, GsonUtils.STRING_ARRAY_TYPE);
            if (rightsArray.length > 0) {
                try {
                    if (StringUtils.hasText(expiry) && !ExpiryUtil.validateExpiry(expiry)) {
                        uploadResponse.setError(RMSMessageHandler.getClientString("fileValidityErr"));
                        JsonUtil.writeJsonToResponse(uploadResponse, response);
                        return;
                    }
                } catch (JsonSyntaxException e) {
                    uploadResponse.setError(RMSMessageHandler.getClientString("err.invalid.params"));
                    JsonUtil.writeJsonToResponse(uploadResponse, response);
                    return;
                }
                fileName = fileRequest.getConflictFileName();
                Rights[] rights = SharedFileManager.toRights(rightsArray);
                if (ArrayUtils.contains(rights, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                    JsonWatermark item = WatermarkConfigManager.getWaterMarkConfig(HTTPUtil.getInternalURI(request), userPrincipal.getTenantName(), userPrincipal.getTicket(), userPrincipal.getPlatformId(), String.valueOf(userPrincipal.getUserId()), userPrincipal.getClientId());
                    watermark = item != null ? item.getText() : "";
                }
                File outputFile = new File(tmpDir, fileName);
                Map<String, String[]> emptyTagsHashMap = new HashMap<String, String[]>();
                String path = HTTPUtil.getInternalURI(request);
                String membershipId = SharedFileManager.getMembership(path, userPrincipal);
                int userId = userPrincipal.getUserId();
                String ticket = userPrincipal.getTicket();
                String clientId = userPrincipal.getClientId();
                Integer platformId = userPrincipal.getPlatformId();
                UploadedFileMetaData metaData;
                try {
                    metaData = RepositoryFileUtil.uploadFileToRepo(userPrincipal, repository, filePathId, filePathDisplay, file, false, file.getName(), null, userConfirmedFileOverwrite);
                } catch (InSufficientSpaceException e) {
                    uploadResponse.setError(RMSMessageHandler.getClientString("fileUploadNoSpaceErr"));
                    JsonUtil.writeJsonToResponse(uploadResponse, response);
                    return;
                } catch (RepositoryException | IOException e) {
                    if (e instanceof InvalidFileNameException) {
                        uploadResponse.setError(RMSMessageHandler.getClientString("invalidCharactersInFilename"));
                    } else {
                        uploadResponse.setError(RMSMessageHandler.getClientString("fileUploadErr"));
                    }
                    JsonUtil.writeJsonToResponse(uploadResponse, response);
                    return;
                }
                try (OutputStream os = new FileOutputStream(outputFile)) {
                    try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(userId, ticket, clientId, platformId, path, membershipId, rights, watermark, GsonUtils.GSON.fromJson(expiry, JsonExpiry.class), emptyTagsHashMap, file, os))) {
                        RMSRestHelper.sendActivityLogToRMS(path, ticket, nxl.getDuid(), nxl.getOwner(), userId, clientId, Operations.PROTECT, " ", " ", " ", AccessResult.ALLOW, request, null, null, AccountType.PERSONAL);
                        String filePolicy = DecryptUtil.getFilePolicyStr(nxl, null);
                        SharedFileDAO.updateNxlDBProtect(userId, nxl.getDuid(), nxl.getOwner(), Rights.toInt(rights), outputFile.getName(), filePolicy);
                    }
                }
                filePathDisplay = metaData.getPathDisplay();
                filePathId = metaData.getPathId();
                file = outputFile;
                try {
                    fileMetaData = RepositoryFileUtil.uploadFileToMyVault(userPrincipal, repository.getRepoId(), repository.getRepoName(), repository.getRepoType().name(), filePathId, filePathDisplay, file, false, file.getName(), true, userConfirmedFileOverwrite, request);
                } catch (RepositoryException | IOException e) {
                    uploadResponse.setError(RMSMessageHandler.getClientString("nxlUploadFailMyVault"));
                    JsonUtil.writeJsonToResponse(uploadResponse, response);
                    return;
                }
            } else {
                fileMetaData = RepositoryFileUtil.uploadFileToRepo(userPrincipal, repository, filePathId, filePathDisplay, file, false, fileName, null, userConfirmedFileOverwrite);
            }
            uploadResponse.setName(fileMetaData.getFileNameWithTimeStamp());
            uploadResponse.setMsg(RMSMessageHandler.getClientString("successUploadFileMsg", fileName));
            if (DefaultRepositoryManager.isDefaultRepo(repository)) {
                Map<String, Long> myDriveStatus = DefaultRepositoryManager.getMyDriveStatus(userPrincipal);
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                JsonElement jsonElement = gson.toJsonTree(uploadResponse);
                jsonElement.getAsJsonObject().addProperty(RepoConstants.MY_VAULT_STORAGE_USED, myDriveStatus.get(RepoConstants.MY_VAULT_STORAGE_USED));
                jsonElement.getAsJsonObject().addProperty(RepoConstants.STORAGE_USED, myDriveStatus.get(RepoConstants.STORAGE_USED));
                jsonElement.getAsJsonObject().addProperty(RepoConstants.USER_QUOTA, myDriveStatus.get(RepoConstants.USER_QUOTA));
                JsonUtil.writeJsonToResponse(jsonElement, response);
            } else {
                JsonUtil.writeJsonToResponse(uploadResponse, response);
            }
        } catch (FileUploadException | UnsupportedEncodingException | RepositoryException | GeneralSecurityException
                | NxlException | DriveStorageExceededException | VaultStorageExceededException e) {
            String error = getErrorMsg(e);
            uploadResponse.setError(error);
            JsonUtil.writeJsonToResponse(uploadResponse, response);
        } catch (Throwable e) {
            String error = getErrorMsg(e);
            uploadResponse.setError(error);
            JsonUtil.writeJsonToResponse(uploadResponse, response);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
            deleteTmpDir(tmpDir);
        }
    }

    public static boolean invalidNxlUploadMembership(File file) {

        try (InputStream is = new FileInputStream(file);
                NxlFile nxlMetadata = NxlFile.parse(is);
                DbSession session = DbSession.newSession()) {
            String owner = nxlMetadata.getOwner();
            Membership membership = session.get(Membership.class, owner);
            return StringUtils.hasText(membership.getTenant().getParentId());
        } catch (Throwable e) {
            return false;
        }
    }

    private void deleteTmpDir(File tmpDir) {
        if (tmpDir != null && tmpDir.exists()) {
            boolean deleted = FileUtils.deleteQuietly(tmpDir);
            if (!deleted) {
                logger.warn("Unable to delete tmp folder: " + tmpDir.getAbsolutePath());
            }
        }
    }

    private String getErrorMsg(Throwable e) {
        String error;
        if (e instanceof InvalidTokenException) {
            error = RMSMessageHandler.getClientString("invalidRepositoryToken");
        } else if (e instanceof UnauthorizedRepositoryException) {
            error = RMSMessageHandler.getClientString("unauthorizedRepositoryAccess");
        } else if (e instanceof NonUniqueFileException) {
            error = RMSMessageHandler.getClientString("nonUniqueRepoFileErr");
        } else if (e instanceof InSufficientSpaceException) {
            error = RMSMessageHandler.getClientString("insufficientStorageError", RMSMessageHandler.getClientString("operationTypeUpload"));
        } else if (e instanceof RMSException) {
            error = e.getMessage();
        } else if (e instanceof InvalidFileNameException) {
            error = RMSMessageHandler.getClientString("invalidCharactersInFilename");
        } else if (e instanceof RepositoryException) {
            error = RMSMessageHandler.getClientString("inaccessibleRepository");
        } else if (e instanceof FileConflictException) {
            error = RMSMessageHandler.getClientString("uploadFileRepoDupErrMsg");
        } else if (e instanceof DriveStorageExceededException) {
            error = RMSMessageHandler.getClientString("driveStorageExceedException");
        } else if (e instanceof VaultStorageExceededException) {
            error = RMSMessageHandler.getClientString("vaultStorageExceedException");
        } else if (e instanceof IOException) {
            error = RMSMessageHandler.getClientString("fileUploadErr");
        } else {
            logger.error(e.getMessage(), e);
            error = RMSMessageHandler.getClientString("fileUploadErr");
        }
        return error;
    }
}
