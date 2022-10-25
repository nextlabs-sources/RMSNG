package com.nextlabs.rms.share;

import com.nextlabs.common.shared.Operations;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;

import java.io.File;

public abstract class ShareSource {

    protected NxlMetaData metaData; // metadata of source nxl file
    protected File file; // nxl file
    protected IRepository repo; // source repository
    protected StoreItem storeItem; // DTO for stored source nxl file
    protected String serverPath;
    protected SharingTransaction existingTransaction;

    public NxlMetaData getMetaData() {
        return metaData;
    }

    public File getFile() {
        return file;
    }

    public StoreItem getStoreItem() {
        return storeItem;
    }

    public SharingTransaction getExistingTransaction() {
        return existingTransaction;
    }

    protected abstract void validateNxlFile(NxlMetaData nxlMetaData, RMSUserPrincipal principal,
        Operations operation, String deviceId, int platformId, String repositoryId,
        String filePathId, String fileName, String filePath);
}
