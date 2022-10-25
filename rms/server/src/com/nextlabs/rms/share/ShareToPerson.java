package com.nextlabs.rms.share;

import com.google.gson.JsonObject;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.EmailUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.SharingRecipientPersonal;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.service.MyVaultService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ShareToPerson implements ShareStrategy {

    @Override
    public Map<String, Set<?>> processRecipientsList(List<?> existingList, List<JsonSharing.JsonRecipient> recipients) {

        Set<String> alreadyShared = new LinkedHashSet<>(recipients.size());
        Set<String> newShared = new LinkedHashSet<>(recipients.size());
        Set<String> totalShared = new LinkedHashSet<>(recipients.size());

        Locale locale = Locale.getDefault();
        recipients.forEach((recipient) -> {
            String email = recipient.getEmail().toLowerCase(locale).trim();
            newShared.add(email);
            totalShared.add(email);
        });
        existingList.forEach((existing) -> {
            String email = ((SharingRecipientPersonal)existing).getId().getEmail();
            alreadyShared.add(email);
            newShared.remove(email);
            totalShared.add(email);
        });

        Map<String, Set<?>> sharedMap = new HashMap<>();
        sharedMap.put("alreadyShared", alreadyShared);
        sharedMap.put("newShared", newShared);
        sharedMap.put("totalShared", totalShared);
        return sharedMap;
    }

    @Override
    public String createRecipientsActivityData(Collection<?> recipients) {
        JsonObject data = new JsonObject();
        data.addProperty("recipients", StringUtils.join((Collection<String>)recipients, "\\,"));
        return data.toString();
    }

    @Override
    public void sendNotification(HttpServletRequest request, SharedFileDTO dto, String transactionId,
        String emailFrom, String loginTenant) throws IOException, GeneralSecurityException {
        String path = HTTPUtil.getURI(request);
        String baseURL = SharedFileManager.getBaseShareURL(request, loginTenant);
        String shareURL = "";

        dto.setTransactionID(transactionId);
        shareURL = baseURL + SharedFileManager.getSharingURLQueryString(dto.getTransactionID());

        Set<String> sharedWith = (Set<String>)dto.getShareWith();
        for (String recipient : sharedWith) {
            SharedFileManager.sendEmailToUsers(dto, shareURL + "&e=" + recipient, recipient, emailFrom, path, null);
        }
    }

    @Override
    public boolean isRevoked(StoreItem storeItem) {
        return storeItem.getNxl().getStatus() == AllNxl.Status.REVOKED;
    }

    @Override
    public boolean isExpired(StoreItem storeItem) {
        FilePolicy policy = GsonUtils.GSON.fromJson(storeItem.getNxl().getPolicy(), FilePolicy.class);
        return AdhocEvalAdapter.isFileExpired(policy);
    }

    @Override
    public boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right, Object src) {
        return MyVaultService.checkRights(storeItem.getDuid(), principal.getUserId(), right);
    }

    @Override
    public boolean hasAdminRight(StoreItem storeItem, RMSUserPrincipal principal, UserSession us) {
        return storeItem.getUserId() == principal.getUserId();
    }

    @Override
    public NxlMetaData resolveNxlMetadataFromStoreItem(StoreItem storeItem, RMSUserPrincipal principal) {
        AllNxl nxl = storeItem.getNxl();
        FilePolicy filePolicy = GsonUtils.GSON.fromJson(nxl.getPolicy(), FilePolicy.class);
        return new NxlMetaData(storeItem.getDuid(), nxl.getOwner(), Constants.TokenGroupType.TOKENGROUP_TENANT, nxl.getStatus() == AllNxl.Status.REVOKED, nxl.getUser().getId() == principal.getUserId(), filePolicy, null);
    }

    @Override
    public SharedFileDTO updateSharedFileDTO(SharedFileDTO dto, RMSUserPrincipal principal, JsonSharing shareReq,
        ShareSource source) {
        dto.setExpiryStr(SharedFileManager.getExpiryString(source.getMetaData().getValidity()));
        return dto;
    }

    @Override
    public boolean checkRight(StoreItem storeItem, RMSUserPrincipal principal, Rights right) {
        return MyVaultService.checkRights(storeItem.getDuid(), principal.getUserId(), right);
    }

    @Override
    public void validateReshareInputs(SharingTransaction sharingTransaction, List<JsonRecipient> recipientList,
        Object src, UserSession us) {

        if (recipientList == null || recipientList.isEmpty()) {
            throw new ValidateException(400, "Missing required parameters.");
        }

        for (JsonRecipient jsonRecipient : recipientList) {
            if (jsonRecipient.getProjectId() != 0 || StringUtils.hasText(jsonRecipient.getTenantName())) {
                throw new ValidateException(400, "Invalid recipient");
            }

            if (StringUtils.hasText(jsonRecipient.getEmail()) && !EmailUtils.validateEmail(jsonRecipient.getEmail())) {
                throw new ValidateException(400, "One or more emails have an invalid format.");
            }
        }

    }

    @Override
    public void validateReshareRights(NxlMetaData nxl, Object src, StoreItem item, RMSUserPrincipal principal,
        UserSession us, String transactionId) throws RMSException {
        if (nxl.isRevoked()) {
            throw new ValidateException(4001, "File sharing has been revoked");
        }

        if (hasNoAccessValidity(nxl)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }

        if (!checkRight(item, principal, Rights.SHARE, src)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }
    }

    @Override
    public void validateDecryptRight(StoreItem item, RMSUserPrincipal principal, String transactionId, Object src,
        NxlMetaData nxlMetaData) {

    }

    @Override
    public boolean hasAdminRightAtSource(StoreItem storeItem, RMSUserPrincipal principal, Object src, UserSession us) {
        return storeItem.getUserId() == principal.getUserId();
    }

    @Override
    public String getRecipientMembership(RMSUserPrincipal principal, String recipient) {
        return null;
    }

    public IRepository getDefaultRepository(RMSUserPrincipal principal, ShareSource source)
            throws InvalidDefaultRepositoryException {
        RMSUserPrincipal stewardPrincipal = new RMSUserPrincipal(source.storeItem.getUserId(), principal.getTenantId(), null, null);
        IRepository repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(stewardPrincipal);
        repository.setUser(principal);
        return repository;
    }

    @Override
    public void validateDownloadRights(NxlMetaData nxl, Object src, StoreItem storeItem, RMSUserPrincipal principal,
        String transactionId, boolean downloadForView, NxlMetaData nxlMetaData) throws RMSException {

        if (isRevoked(storeItem)) {
            boolean isOwner = storeItem.getNxl().getUser().getId() == principal.getUserId();
            if (!isOwner) {
                throw new ValidateException(4001, "File has been revoked");
            }
        }

        if (!downloadForView && !checkRight(storeItem, principal, Rights.DOWNLOAD)) {
            throw new ValidateException(403, "You are not allowed to download this file.");
        }

        if (downloadForView && !checkRight(storeItem, principal, Rights.VIEW)) {
            throw new ValidateException(403, "You are not allowed to view this file.");
        }
    }

    @Override
    public Response downloadFile(RMSUserPrincipal principal, StoreItem storeItem, ShareSource source,
        HttpServletRequest request,
        HttpServletResponse response, AccountType accountType, int start, long length, boolean downloadForView,
        Object spaceId) throws InvalidDefaultRepositoryException, RepositoryException, IOException {
        File outputPath = null;
        try {
            IRepository repository = getDefaultRepository(principal, source);
            boolean partialDownload = start >= 0 && length >= 0;
            if (partialDownload) {
                byte[] data = repository.downloadPartialFile(storeItem.getFilePath(), storeItem.getFilePath(), start, start + length - 1);
                if (data != null) {
                    long contentLength = data.length;
                    response.setHeader("x-rms-file-size", Long.toString(contentLength));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                    response.getOutputStream().write(data);
                }
            } else {
                outputPath = RepositoryFileUtil.getTempOutputFolder();
                File output = repository.getFile(storeItem.getFilePath(), storeItem.getFilePath(), outputPath.getPath());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(FileUtils.getName(storeItem.getFilePathDisplay())));
                if (output != null && output.length() > 0) {
                    response.setHeader("x-rms-file-size", Long.toString(output.length()));
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                    try (InputStream fis = new FileInputStream(output)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                }
            }
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } finally {
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
        }
    }

    @Override
    public byte[] requestDecryptToken(HttpServletRequest request, RMSUserPrincipal principal, NxlFile nxl,
        String spaceId) throws JsonException, IOException, GeneralSecurityException, NxlException {
        return DecryptUtil.requestToken(HTTPUtil.getInternalURI(request), principal.getUserId(), principal.getTicket(), principal.getClientId(), principal.getPlatformId(), principal.getTenantName(), nxl.getOwner(), nxl.getDuid(), nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
    }

    @Override
    public void validateShareRights(Integer projectId, String filePathId, String fileName, RMSUserPrincipal principal,
        StoreItem item, UserSession us, NxlMetaData nxlMetaData) throws RMSException {
        if (hasNoAccessValidity(nxlMetaData)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }

        if (!hasAdminRight(item, principal, us)) {
            throw new ValidateException(403, "You are not allowed to share this file.");
        }
    }

    @Override
    public void validateShareInputs(JsonSharing shareReq, UserSession us) {

    }

    @Override
    public Operations getRemoveRecipientOperation() {
        return Operations.REMOVE_USER;
    }

    @Override
    public void validateListSharedFiles(Object spaceId, RMSUserPrincipal principal) {

    }

    @Override
    public void validateRevokedFiles(StoreItem item, RMSUserPrincipal principal, UserSession us) {
        if (item == null) {
            throw new ValidateException(404, "Nxl file not found");
        }
        if (isRevoked(item)) {
            throw new ValidateException(304, "File already revoked");
        }
        if (!hasAdminRight(item, principal, us)) {
            throw new ValidateException(403, "You are not allowed to revoke the file");
        }
    }

    @Override
    public String getSharedLink(HttpServletRequest request, String loginTenant, String transactionId)
            throws GeneralSecurityException, UnsupportedEncodingException {
        return SharedFileManager.getBaseShareURL(request, loginTenant) + SharedFileManager.getSharingURLQueryString(transactionId);
    }

    private boolean hasNoAccessValidity(NxlMetaData nxlMetaData) {
        return (nxlMetaData.isExpired() || nxlMetaData.isNotYetValid());
    }

}
