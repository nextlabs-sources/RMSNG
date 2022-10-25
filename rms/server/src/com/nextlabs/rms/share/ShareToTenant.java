package com.nextlabs.rms.share;

import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

public class ShareToTenant implements ShareStrategy {

    @Override
    public Map<String, Set<?>> processRecipientsList(List<?> existingRecipients,
        List<JsonSharing.JsonRecipient> recipients) {
        return null;
    }

    @Override
    public String createRecipientsActivityData(Collection<?> recipients) {
        return null;
    }

    @Override
    public void sendNotification(HttpServletRequest request, SharedFileDTO dto, String transactionId, String emailFrom,
        String loginTenant) throws IOException, GeneralSecurityException {

    }

    @Override
    public boolean isRevoked(StoreItem storeItem) {
        return false;
    }

    @Override
    public boolean isExpired(StoreItem storeItem) {
        return false;
    }

    @Override
    public boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right) {
        return false;
    }

    @Override
    public boolean hasAdminRight(StoreItem storeItem, RMSUserPrincipal principal, UserSession us) {
        return false;
    }

    @Override
    public NxlMetaData resolveNxlMetadataFromStoreItem(StoreItem storeItem, RMSUserPrincipal principal) {
        return null;
    }

    @Override
    public SharedFileDTO updateSharedFileDTO(SharedFileDTO dto, RMSUserPrincipal principal, JsonSharing shareReq,
        ShareSource source) {
        return null;
    }

    @Override
    public boolean checkRight(StoreItem item, RMSUserPrincipal principal, Rights share, Object src) {
        return false;
    }

    @Override
    public void validateReshareInputs(SharingTransaction sharingTransaction, List<JsonRecipient> recipientList,
        Object src, UserSession us) {
        if (recipientList == null || recipientList.isEmpty()) {
            throw new ValidateException(400, "Missing required parameters.");
        }
    }

    @Override
    public void validateReshareRights(NxlMetaData nxl, Object src, StoreItem item, RMSUserPrincipal principal,
        UserSession us, String transactionId) throws RMSException {

    }

    @Override
    public void validateDecryptRight(StoreItem item, RMSUserPrincipal principal, String transactionId, Object src,
        NxlMetaData nxlMetaData) {

    }

    @Override
    public boolean hasAdminRightAtSource(StoreItem storeItem, RMSUserPrincipal principal, Object src, UserSession us) {
        return false;
    }

    @Override
    public String getRecipientMembership(RMSUserPrincipal principal, String recipient) {
        return null;
    }

    @Override
    public IRepository getDefaultRepository(RMSUserPrincipal principal, ShareSource source)
            throws InvalidDefaultRepositoryException {
        return null;
    }

    @Override
    public void validateDownloadRights(NxlMetaData nxl, Object src, StoreItem item, RMSUserPrincipal principal,
        String transactionId, boolean downloadForView, NxlMetaData nxlMetaData) throws RMSException {

    }

    @Override
    public Response downloadFile(RMSUserPrincipal principal, StoreItem storeItem, ShareSource source,
        HttpServletRequest request,
        HttpServletResponse response, AccountType accountType, int start, long length, boolean downloadForView,
        Object spaceId) {
        return null;
    }

    @Override
    public byte[] requestDecryptToken(HttpServletRequest request, RMSUserPrincipal principal, NxlFile nxl,
        String spaceId) {
        return null;
    }

    @Override
    public void validateShareRights(Integer projectId, String filePathId, String fileName, RMSUserPrincipal principal,
        StoreItem item, UserSession us, NxlMetaData nxlMetaData) throws RMSException {

    }

    @Override
    public void validateShareInputs(JsonSharing shareReq, UserSession us) {

    }

    @Override
    public Operations getRemoveRecipientOperation() {
        return null;
    }

    @Override
    public void validateListSharedFiles(Object spaceId, RMSUserPrincipal principal) {

    }

    @Override
    public void validateRevokedFiles(StoreItem item, RMSUserPrincipal principal, UserSession us) {

    }

    @Override
    public String getSharedLink(HttpServletRequest request, String loginTenant, String transactionId)
            throws GeneralSecurityException {
        return null;
    }
}
