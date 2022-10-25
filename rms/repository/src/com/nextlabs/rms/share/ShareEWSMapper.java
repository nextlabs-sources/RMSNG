package com.nextlabs.rms.share;

import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonSharedWithMeFileList;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ShareEWSMapper implements IShareMapper {

    @Override
    public StoreItem getStoreItemByTransaction(String transactionId) {
        return null;
    }

    @Override
    public List<?> getRecipientList(String duid, Collection<JsonSharing.JsonRecipient> recipients) {
        return null;
    }

    @Override
    public String updateSharingTransaction(SharedFileDTO dto, int userId, String metadata)
            throws ValidateException, RMSException {
        return null;
    }

    @Override
    public SharingTransaction getFirstTransactionByDuid(String duid) {
        return null;
    }

    @Override
    public StoreItem getStoreItemByDuid(String duid) {
        return null;
    }

    @Override
    public Set<String> removeRecipients(List<?> recipients) {
        return null;
    }

    @Override
    public SharingTransaction getSharingTransaction(String transactionId) {
        return null;
    }

    @Override
    public JsonSharedWithMeFileList getSharedWithMeFiles(Integer page, Integer size, String orderBy,
        String searchString, Object tenantName) throws GeneralSecurityException {
        return null;
    }

    @Override
    public JsonSharedWithMeFile getSharedWithMeFile(String transactionId, RMSUserPrincipal principal,
        String recipientMembership)
            throws RMSException {
        return null;
    }

    @Override
    public boolean revokeFile(String duid, int userId) throws FileAlreadyRevokedException {
        return false;
    }
}
