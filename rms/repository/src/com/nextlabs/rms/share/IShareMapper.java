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

public interface IShareMapper {

    StoreItem getStoreItemByTransaction(String transactionId);

    List<?> getRecipientList(String duid, Collection<JsonSharing.JsonRecipient> recipients);

    String updateSharingTransaction(SharedFileDTO dto, int userId, String metadata)
            throws ValidateException, RMSException;

    SharingTransaction getFirstTransactionByDuid(String duid);

    StoreItem getStoreItemByDuid(String duid);

    Set<?> removeRecipients(List<?> recipients);

    SharingTransaction getSharingTransaction(String transactionId);

    JsonSharedWithMeFileList getSharedWithMeFiles(Integer page, Integer size, String orderBy, String searchString,
        Object recipientId) throws GeneralSecurityException;

    JsonSharedWithMeFile getSharedWithMeFile(String transactionId, RMSUserPrincipal principal,
        String recipientMembership) throws RMSException;

    boolean revokeFile(String duid, int userId) throws FileAlreadyRevokedException;
}
