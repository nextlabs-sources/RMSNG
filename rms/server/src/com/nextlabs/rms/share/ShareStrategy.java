package com.nextlabs.rms.share;

import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.RepositoryException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

/*
* Common interface of sharing strategy
* */
public interface ShareStrategy {

    Map<String, Set<?>> processRecipientsList(List<?> existingRecipients, List<JsonSharing.JsonRecipient> recipients);

    String createRecipientsActivityData(Collection<?> recipients);

    void sendNotification(HttpServletRequest request, SharedFileDTO dto, String transactionId,
        String emailFrom, String loginTenant) throws IOException, GeneralSecurityException;

    boolean isRevoked(StoreItem storeItem);

    boolean isExpired(StoreItem storeItem);

    boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right);

    boolean hasAdminRight(StoreItem storeItem, RMSUserPrincipal principal, UserSession us);

    NxlMetaData resolveNxlMetadataFromStoreItem(StoreItem storeItem, RMSUserPrincipal principal) throws RMSException;

    SharedFileDTO updateSharedFileDTO(SharedFileDTO dto, RMSUserPrincipal principal, JsonSharing shareReq,
        ShareSource source);

    boolean checkRight(StoreItem item, RMSUserPrincipal principal, Rights share, Object src);

    void validateReshareInputs(SharingTransaction sharingTransaction, List<JsonRecipient> recipientList, Object src,
        UserSession us);

    void validateReshareRights(NxlMetaData nxl, Object src, StoreItem item, RMSUserPrincipal principal, UserSession us,
        String transactionId) throws RMSException;

    void validateDecryptRight(StoreItem item, RMSUserPrincipal principal, String transactionId, Object src,
        NxlMetaData nxlMetaData)
            throws RMSException;

    boolean hasAdminRightAtSource(StoreItem storeItem, RMSUserPrincipal principal, Object src, UserSession us);

    String getRecipientMembership(RMSUserPrincipal principal, String recipient) throws RMSException;

    IRepository getDefaultRepository(RMSUserPrincipal principal, ShareSource source)
            throws InvalidDefaultRepositoryException;

    void validateDownloadRights(NxlMetaData nxl, Object src, StoreItem storeItem, RMSUserPrincipal principal,
        String transactionId, boolean downloadForView, NxlMetaData nxlMetaData) throws RMSException;

    Response downloadFile(RMSUserPrincipal principal, StoreItem storeItem, ShareSource source,
        HttpServletRequest request,
        HttpServletResponse response, AccountType accountType, int start, long length, boolean downloadForView,
        Object spaceId) throws InvalidDefaultRepositoryException, RepositoryException, IOException;

    byte[] requestDecryptToken(HttpServletRequest request, RMSUserPrincipal principal, NxlFile nxl, String spaceId)
            throws JsonException, IOException, GeneralSecurityException, NxlException, RMSException;

    void validateShareRights(Integer projectId, String filePathId, String fileName, RMSUserPrincipal principal,
        StoreItem item, UserSession us, NxlMetaData nxlMetaData) throws RMSException;

    void validateShareInputs(JsonSharing shareReq, UserSession us);

    Operations getRemoveRecipientOperation();

    void validateListSharedFiles(Object spaceId, RMSUserPrincipal principal);

    void validateRevokedFiles(StoreItem item, RMSUserPrincipal principal, UserSession us);

    String getSharedLink(HttpServletRequest request, String loginTenant, String transactionId)
            throws GeneralSecurityException, UnsupportedEncodingException;

}
