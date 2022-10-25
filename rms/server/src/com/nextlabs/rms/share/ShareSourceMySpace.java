package com.nextlabs.rms.share;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RemoteCrypto;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dao.SharedFileDAO;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryFactory;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.share.factory.ShareMapperFactory;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;

public class ShareSourceMySpace extends ShareSource {

    public ShareSourceMySpace(JsonSharing shareReq, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        // initialize sharing resource from share request
        File tempDir = null;
        try {
            if (StringUtils.hasText(shareReq.getEfsId())) {
                File efsIdFolder = new File(WebConfig.getInstance().getCommonSharedTempDir(), shareReq.getEfsId());
                if (efsIdFolder.exists() && efsIdFolder.isDirectory()) {
                    file = new File(efsIdFolder, shareReq.getFileName());
                }
            } else {
                if (StringUtils.hasText(shareReq.getRepositoryId())) {
                    tempDir = RepositoryFileUtil.getTempOutputFolder();
                    try (DbSession session = DbSession.newSession()) {
                        RMSUserPrincipal repoOwner = RepositoryFactory.getInstance().getRepoOwner(session, principal, shareReq.getRepositoryId());
                        repo = RepositoryFactory.getInstance().getRepository(session, repoOwner, shareReq.getRepositoryId());
                    }
                    repo.setUser(principal);
                    file = RepositoryFileUtil.downloadFileFromRepo(repo, shareReq.getFilePathId(), shareReq.getFilePath(), tempDir);
                } else {
                    file = new File(shareReq.getFilePath());
                }
            }
            if (file == null || !file.exists()) {
                throw new ValidateException(404, "File not found");
            }
            serverPath = HTTPUtil.getInternalURI(request);
            encryptForShareIfNotProtected(shareReq, principal);
            uploadToMyVaultIfNotPresent(shareReq, principal, request);
        } catch (TokenGroupNotFoundException | GeneralSecurityException | NxlException
                | InvalidDefaultRepositoryException | RepositoryException | IOException e) {
            throw new RMSException(e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    public ShareSourceMySpace(JsonSharing shareReq, File file, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        this.file = file;
        serverPath = HTTPUtil.getInternalURI(request);
        try {
            for (JsonRecipient recipient : shareReq.getRecipients()) {
                if (!StringUtils.hasText(recipient.getEmail()) || !EmailUtils.validateEmail(recipient.getEmail())) {
                    throw new ValidateException(400, "Invalid recipient");
                }
            }
            encryptForShareIfNotProtected(shareReq, principal);
            uploadToMyVaultIfNotPresent(shareReq, principal, request);
        } catch (IOException | TokenGroupNotFoundException | GeneralSecurityException | RepositoryException
                | InvalidDefaultRepositoryException | NxlException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    public ShareSourceMySpace(SharingTransaction sharingTransaction, RMSUserPrincipal principal,
        HttpServletRequest request) {
        existingTransaction = sharingTransaction;
        IShareMapper mapper = ShareMapperFactory.getInstance().create(sharingTransaction.getFromSpace(), null);
        storeItem = mapper.getStoreItemByDuid(sharingTransaction.getMySpaceNxl().getDuid());
        serverPath = HTTPUtil.getInternalURI(request);
        AllNxl nxl = storeItem.getNxl();
        FilePolicy filePolicy = GsonUtils.GSON.fromJson(nxl.getPolicy(), FilePolicy.class);
        metaData = new NxlMetaData(storeItem.getDuid(), nxl.getOwner(), Constants.TokenGroupType.TOKENGROUP_TENANT, nxl.getStatus() == AllNxl.Status.REVOKED, nxl.getUser().getId() == principal.getUserId(), filePolicy, null);

    }

    private void encryptForShareIfNotProtected(JsonSharing shareReq, RMSUserPrincipal principal)
            throws IOException, TokenGroupNotFoundException, GeneralSecurityException, NxlException {
        if (NxlFile.isNxl(file)) {
            if (!StringUtils.endsWithIgnoreCase(file.getName(), com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
                throw new ValidateException(400, "Invalid file extension");
            }
            metaData = SharedFileManager.getNxlSharableMetadata(file, serverPath, principal);
            validateNxlFile(metaData, principal, Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), shareReq.getRepositoryId(), shareReq.getFilePathId(), file.getName(), shareReq.getFilePath());
        } else {
            if (shareReq.getPermissions() < 1) {
                throw new ValidateException(4009, "Invalid value for permissions");
            }
            File encrypted = new File(file.getPath() + com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
            Rights[] rights = Rights.fromInt(shareReq.getPermissions());
            Map<String, String[]> tags = Nvl.nvl(shareReq.getTags(), new HashMap<>());
            String watermark = shareReq.getWatermark();
            if (ArrayUtils.contains(rights, Rights.WATERMARK) && !StringUtils.hasText(watermark)) {
                JsonWatermark item = WatermarkConfigManager.getWaterMarkConfig(serverPath, principal.getTenantName(), principal.getTicket(), principal.getPlatformId(), String.valueOf(principal.getUserId()), principal.getClientId());
                watermark = item != null ? item.getText() : "";
            }

            try (OutputStream os = new FileOutputStream(encrypted)) {
                NxlFile nxl = null;
                try {
                    nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), serverPath, shareReq.getMembershipId(), rights, watermark, shareReq.getExpiry(), tags, file, os));
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), nxl.getOwner(), principal.getUserId(), Operations.PROTECT, principal.getDeviceId(), principal.getPlatformId(), shareReq.getRepositoryId(), shareReq.getFilePathId(), file.getName(), shareReq.getFilePath(), null, null, null, com.nextlabs.common.shared.Constants.AccessResult.ALLOW, new Date(), null, com.nextlabs.common.shared.Constants.AccountType.PERSONAL);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                } finally {
                    IOUtils.closeQuietly(nxl);
                }
            }
            file = encrypted;
            metaData = SharedFileManager.getNxlSharableMetadata(file, serverPath, principal);
        }
    }

    private void uploadToMyVaultIfNotPresent(JsonSharing shareReq, RMSUserPrincipal principal,
        HttpServletRequest request)
            throws InvalidDefaultRepositoryException, RepositoryException, NxlException, IOException, RMSException {
        if (!StringUtils.startsWith(shareReq.getFilePathId(), RepoConstants.MY_VAULT_FOLDER_PATH_ID)) {
            // we only upload to MyVault if the file is not present
            String originalFilePathDisplay;
            String repoId = null;
            String repoName = null;
            String repoType = null;
            if (repo == null) {
                originalFilePathDisplay = file.getName();
            } else {
                originalFilePathDisplay = shareReq.getFilePath();
                repoId = repo.getRepoId();
                repoName = repo.getRepoName();
                repoType = repo.getRepoType().name();
            }
            //for Manage Local File, filePathId will be local file system path. i.e: C:\temp\data.txt.nxl
            storeItem = RepositoryFileUtil.getMyVaultFileMetadata(principal, "/" + file.getName());
            if (storeItem == null || storeItem.isDeleted() || shareReq.isUserConfirmedFileOverwrite()) {
                if (storeItem != null && (shareReq.isUserConfirmedFileOverwrite() || storeItem.isDeleted())) {
                    SharedFileDAO.replaceNxlDBProtect(principal.getUserId(), metaData.getDuid(), metaData.getOwnerMembership(), Rights.toInt(metaData.getRights()), file.getName(), GsonUtils.GSON.toJson(metaData.getPolicy(), FilePolicy.class));
                } else {
                    SharedFileDAO.updateNxlDBProtect(principal.getUserId(), metaData.getDuid(), metaData.getOwnerMembership(), Rights.toInt(metaData.getRights()), file.getName(), GsonUtils.GSON.toJson(metaData.getPolicy(), FilePolicy.class));
                }
                RepositoryFileUtil.uploadFileToMyVault(principal, repoId, repoName, repoType, shareReq.getFilePathId(), originalFilePathDisplay, file, false, file.getName(), repo != null, shareReq.isUserConfirmedFileOverwrite(), request);
            } else {
                throw new ValidateException(4009, "File already exists");
            }
        }
        if (storeItem == null) {
            storeItem = RepositoryFileUtil.getMyVaultFileMetadata(principal, "/" + file.getName());
        }
    }

    @Override
    protected void validateNxlFile(NxlMetaData nxlMetaData, RMSUserPrincipal principal,
        Operations operation, String deviceId, int platformId, String repositoryId,
        String filePathId, String fileName, String filePath) {
        if (nxlMetaData.getTgType() != Constants.TokenGroupType.TOKENGROUP_TENANT) {
            throw new ValidateException(405, "This '.nxl' file is not eligible for this operation based on its tenant membership");
        }
        try (DbSession session = DbSession.newSession()) {
            Membership membership = session.get(Membership.class, nxlMetaData.getOwnerMembership());
            if (membership == null) {
                throw new ValidateException(400, "Invalid membership");
            }
            final Date now = new Date();
            if (membership.getUser().getId() != principal.getUserId()) {
                operation = Operations.RESHARE;
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxlMetaData.getDuid(), nxlMetaData.getOwnerMembership(), principal.getUserId(), operation, deviceId, platformId, repositoryId, filePathId, fileName, filePath, null, null, null, com.nextlabs.common.shared.Constants.AccessResult.DENY, now, null, com.nextlabs.common.shared.Constants.AccountType.PERSONAL);
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw new ValidateException(4010, "Reshare operation is not allowed through Share APIs.");
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxlMetaData.getDuid(), nxlMetaData.getOwnerMembership(), principal.getUserId(), operation, deviceId, platformId, repositoryId, filePathId, fileName, filePath, null, null, null, com.nextlabs.common.shared.Constants.AccessResult.DENY, now, null, com.nextlabs.common.shared.Constants.AccountType.PERSONAL);
            if (nxlMetaData.isRevoked()) {
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw new ValidateException(4001, "File has been revoked");
            } else if (nxlMetaData.isExpired()) {
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw new ValidateException(4001, "File is Expired");
            } else if (!nxlMetaData.isAllowedToShare() || (!nxlMetaData.isOwner() && !SharedFileManager.isRecipient(nxlMetaData.getDuid(), Collections.singletonList(principal.getEmail()), session))) {
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw new ValidateException(403, "File is not allowed to share");
            }

        }
    }
}
