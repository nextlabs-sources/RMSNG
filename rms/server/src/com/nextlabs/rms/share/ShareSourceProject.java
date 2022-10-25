package com.nextlabs.rms.share;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryProject;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.RESTAPIAuthenticationFilter;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class ShareSourceProject extends ShareSource {

    public ShareSourceProject(JsonSharing shareReq, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        File tempDir = null;
        try {
            if (shareReq.getFromSpace() != Constants.SHARESPACE.PROJECTSPACE) {
                throw new ValidateException(400, "File is not from project space.");
            }
            serverPath = HTTPUtil.getInternalURI(request);
            try (DbSession session = DbSession.newSession()) {

                List<Integer> validateProjIds = new ArrayList<Integer>();
                validateProjIds.add(shareReq.getProjectId());
                UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
                if (!ProjectService.isValidMemberProjectIds(validateProjIds, us)) {
                    throw new ValidateException(400, "Invalid project id");
                }

                DefaultRepositoryProject repository = new DefaultRepositoryProject(session, principal, shareReq.getProjectId());
                tempDir = RepositoryFileUtil.getTempOutputFolder();

                RepositoryContent fileMetadata = repository.getFileMetadata(shareReq.getFilePathId());
                if (fileMetadata == null) {
                    throw new ValidateException(404, "Invalid filePathId. File not found");
                }

                file = repository.getFile(shareReq.getFilePathId(), shareReq.getFilePath(), tempDir.getPath());
                metaData = SharedFileManager.getProjectFileNxlSharableMetadata(file, serverPath, principal);
                validateNxlFile(metaData, principal, Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), shareReq.getRepositoryId(), shareReq.getFilePathId(), file.getName(), shareReq.getFilePath());
                storeItem = new ShareProjectMapper().getStoreItemByDuid(metaData.getDuid());
            }
        } catch (InvalidDefaultRepositoryException | IOException | RepositoryException e) {
            throw new RMSException(e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    @Override
    protected void validateNxlFile(NxlMetaData nxlMetaData, RMSUserPrincipal principal, Operations operation,
        String deviceId, int platformId, String repositoryId, String filePathId, String fileName, String filePath) {
        if (nxlMetaData.getTgType() != Constants.TokenGroupType.TOKENGROUP_PROJECT) {
            throw new ValidateException(405, "This '.nxl' file is not eligible for this operation based on its tenant membership");
        }
    }

    public ShareSourceProject(SharingTransaction sharingTransaction, RMSUserPrincipal principal,
        HttpServletRequest request) throws RMSException {
        File tempDir = null;
        try {
            if (sharingTransaction.getFromSpace() != Constants.SHARESPACE.PROJECTSPACE) {
                throw new ValidateException(400, "File is not from project space.");
            }
            try (DbSession session = DbSession.newSession()) {
                existingTransaction = sharingTransaction;
                serverPath = HTTPUtil.getInternalURI(request);
                tempDir = RepositoryFileUtil.getTempOutputFolder();
                repo = ShareProjectMapper.getProjectRepository(session, principal, existingTransaction.getProjectNxl().getProject().getId());
                file = repo.getFile(existingTransaction.getProjectNxl().getFilePath(), existingTransaction.getProjectNxl().getFilePathDisplay(), tempDir.getPath());
                metaData = SharedFileManager.getProjectFileNxlSharableMetadata(file, serverPath, principal);
                storeItem = new ShareProjectMapper().getStoreItemByDuid(session, metaData.getDuid());
            }
        } catch (InvalidDefaultRepositoryException | RepositoryException | IOException e) {
            throw new RMSException(e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

}
