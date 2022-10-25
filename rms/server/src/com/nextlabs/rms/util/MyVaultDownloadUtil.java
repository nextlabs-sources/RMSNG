package com.nextlabs.rms.util;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.pojos.RMSSpacePojo;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;

import java.io.File;

public class MyVaultDownloadUtil extends DownloadUtil {

    public Rights getRights(Constants.DownloadType downloadType) {
        Rights rights = null;
        if (downloadType == Constants.DownloadType.OFFLINE) {
            rights = Rights.VIEW;
        } else if (downloadType == Constants.DownloadType.FOR_VIEWER) {
            rights = Rights.VIEW;
        } else if (downloadType == Constants.DownloadType.NORMAL) {
            rights = Rights.DOWNLOAD;
        }
        return rights;
    }

    public DefaultRepositoryTemplate getRepository(RMSUserPrincipal principal)
            throws InvalidDefaultRepositoryException {
        DefaultRepositoryTemplate repository;
        try (DbSession session = DbSession.newSession()) {
            repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
        }
        return repository;
    }

    public String validateFileDuid(String duid, boolean isNxl) throws FileNotFoundException {
        if (!StringUtils.hasText(duid) && isNxl) {
            throw new FileNotFoundException("Missing File.");
        }
        return duid;
    }

    /***
     * This method helps to download file from Myvault and transfer to other SKYDRM spaces
     * @param principal
     * @param sourcePojo
     * @param tempTrfFolder
     * @return
     * @throws InvalidDefaultRepositoryException
     * @throws RepositoryException
     */
    public File downloadFileForTransfer(RMSUserPrincipal principal, RMSSpacePojo sourcePojo, File tempTrfFolder)
            throws InvalidDefaultRepositoryException, RepositoryException {
        DefaultRepositoryTemplate repository = getRepository(principal);
        return repository.getFile(sourcePojo.getFilePathId(), sourcePojo.getFilePathId(), tempTrfFolder.getPath());
    }
}
