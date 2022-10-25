package com.nextlabs.rms.share;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonSharedWithMeFileList;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.exception.ExceptionProcessor;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.RESTAPIAuthenticationFilter;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;
import com.nextlabs.rms.share.factory.ShareMapperFactory;
import com.nextlabs.rms.share.factory.ShareSourceFactory;
import com.nextlabs.rms.share.factory.ShareStrategyFactory;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*
 * General sharing service implementation
 * The business logic of different sharing scenarios should be similar
 * Target to solve all sharing scenarios using a single class
 * */
public class ShareServiceImpl implements IShareService {

    private final RMSUserPrincipal principal;
    private final ShareSource source;
    private final IShareMapper mapper;
    private ShareStrategy strategy;
    private Constants.AccountType accountType;

    public ShareServiceImpl(JsonSharing shareReq, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        this.principal = principal;
        this.source = ShareSourceFactory.getInstance().create(shareReq, principal, request);
        this.mapper = ShareMapperFactory.getInstance().create(shareReq.getFromSpace(), shareReq.getToSpace());
        this.strategy = ShareStrategyFactory.getInstance().create(shareReq.getFromSpace(), shareReq.getToSpace());
        this.accountType = getAccountType(shareReq.getFromSpace());
    }

    public ShareServiceImpl(JsonSharing shareReq, File file, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        this.principal = principal;
        this.source = ShareSourceFactory.getInstance().create(shareReq, file, principal, request);
        this.mapper = ShareMapperFactory.getInstance().create(shareReq.getFromSpace(), shareReq.getToSpace());
        this.strategy = ShareStrategyFactory.getInstance().create(shareReq.getFromSpace(), shareReq.getToSpace());
        this.accountType = getAccountType(shareReq.getFromSpace());
    }

    public ShareServiceImpl(RMSUserPrincipal principal, Constants.SHARESPACE fromSpace) {
        this.source = null;
        this.principal = principal;
        this.mapper = ShareMapperFactory.getInstance().create(fromSpace, null);
        this.strategy = ShareStrategyFactory.getInstance().create(fromSpace, null);
        this.accountType = getAccountType(fromSpace);
    }

    public ShareServiceImpl(RMSUserPrincipal principal, String duid) {
        this.source = null;
        this.principal = principal;
        SharingTransaction st = SharedFileManager.getSharingTransactionByDuid(duid);
        this.mapper = ShareMapperFactory.getInstance().create(st.getFromSpace(), null);
        this.strategy = ShareStrategyFactory.getInstance().create(st.getFromSpace(), null);
        this.accountType = getAccountType(st.getFromSpace());
    }

    public ShareServiceImpl(String transactionId, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        this.principal = principal;
        SharingTransaction sharingTransaction = SharedFileManager.getSharingTransactionByTransactionId(transactionId);
        this.source = ShareSourceFactory.getInstance().create(sharingTransaction, principal, request);
        this.mapper = ShareMapperFactory.getInstance().create(sharingTransaction.getFromSpace(), null);
        this.strategy = ShareStrategyFactory.getInstance().create(sharingTransaction.getFromSpace(), null);
        this.accountType = getAccountType(sharingTransaction.getFromSpace());
    }

    private Constants.AccountType getAccountType(Constants.SHARESPACE shareSpace) {
        Constants.AccountType type;
        switch (Nvl.nvl(shareSpace, Constants.SHARESPACE.MYSPACE)) {
            case PROJECTSPACE:
                type = Constants.AccountType.PROJECT;
                break;
            case ENTERPRISESPACE:
                type = Constants.AccountType.ENTERPRISEWS;
                break;
            default:
                type = Constants.AccountType.PERSONAL;
                break;
        }
        return type;
    }

    /**
     * 1. process recipient list
     * 2. create recipient activity data
     * 3. generate sharing dto and update transaction
     * 4. send notification
     * 5. process results
     */
    @Override
    public JsonResponse share(HttpServletRequest request, JsonSharing shareReq) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            NxlMetaData nxl = source.getMetaData();
            List<?> sharedList = mapper.getRecipientList(nxl.getDuid(), shareReq.getRecipients());
            Map<String, Set<?>> sharedMap = strategy.processRecipientsList(sharedList, shareReq.getRecipients());

            Set<?> newShared = sharedMap.get("newShared");
            String activityData = strategy.createRecipientsActivityData(sharedMap.get("totalShared"));

            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try {
                strategy.validateShareInputs(shareReq, us);
                strategy.validateShareRights(shareReq.getProjectId(), shareReq.getFilePathId(), shareReq.getFileName(), principal, source.getStoreItem(), us, nxl);
            } catch (ValidateException ex) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), nxl.getOwnerMembership(), principal.getUserId(), Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), shareReq.getRepositoryId(), shareReq.getFilePath(), shareReq.getFileName(), shareReq.getFilePath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw ex;
            }

            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(nxl.getDuid(), nxl.getOwnerMembership(), principal.getUserId(), Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), source.getStoreItem().getRepoId(), source.getStoreItem().getFilePath(), source.getFile().getName(), source.getStoreItem().getFilePathDisplay(), null, null, null, Constants.AccessResult.ALLOW, new Date(), activityData, accountType);
            RemoteLoggingMgmt.saveActivityLog(activity);
            String transactionId = null;
            if (!newShared.isEmpty()) {
                SharedFileDTO dto = SharedFileManager.generateSharedFileDTO(principal.getDeviceId(), principal.getDeviceType().name(), nxl.getDuid(), source.getStoreItem().getFilePathDisplay(), source.getStoreItem().getFilePath(), source.getFile().getName(), source.getStoreItem().getRepoId(), null, principal.getTenantId(), principal.getUserId(), principal.getTicket(), principal.getEmail(), newShared, source.getMetaData().getRights(), new Date(), false, nxl.getOwnerMembership(), principal.getClientId(), principal.getPlatformId(), shareReq.getComment(), GsonUtils.GSON.toJson(source.getMetaData().getPolicy()));
                dto.setSourceProjectId(shareReq.getProjectId());
                strategy.updateSharedFileDTO(dto, principal, shareReq, source);
                transactionId = mapper.updateSharingTransaction(dto, principal.getUserId(), shareReq.getMetadata());
                strategy.sendNotification(request, dto, transactionId, principal.getName(), principal.getLoginTenant());
            }

            resp.putResult("duid", nxl.getDuid());
            resp.putResult("fileName", FileUtils.getName((String)source.storeItem.getFilePathDisplay()));
            resp.putResult("filePathId", source.getStoreItem().getFilePath());
            resp.putResult("expiry", nxl.getValidity());
            resp.putResult("transactionId", transactionId);
            resp.putResult("alreadySharedList", sharedMap.get("alreadyShared"));
            resp.putResult("newSharedList", sharedMap.get("newShared"));
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    /**
     * 1. resolve source based on transactionId (nxl metadata)
     * 2. validate share right with nxl metadata
     * 3. process recipient list
     * 4. create recipient activity data
     * 5. generate sharing dto and update sharing transaction
     * 6. send notification
     * 7. process result
     */
    @Override
    public JsonResponse reshare(String transactionId, String comment, List<JsonRecipient> recipientList,
        boolean validateOnly, HttpServletRequest request, Object src) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            StoreItem item = source.getStoreItem();
            NxlMetaData nxl = source.getMetaData();
            String fileName = StringUtils.substringAfterLast(item.getFilePathDisplay(), "/");
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!validateOnly) {
                strategy.validateReshareInputs(source.getExistingTransaction(), recipientList, src, us);
            }
            try {
                strategy.validateReshareRights(nxl, src, item, principal, us, source.existingTransaction.getId());
            } catch (ValidateException ex) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(item.getDuid(), nxl.getOwnerMembership(), principal.getUserId(), Operations.RESHARE, principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.DENY, new Date(), null, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw ex;
            }

            if ((validateOnly)) {
                return resp;
            }
            List<?> sharedList = mapper.getRecipientList(item.getDuid(), recipientList);
            Map<String, Set<?>> sharedMap = strategy.processRecipientsList(sharedList, recipientList);
            Set<?> newShared = sharedMap.get("newShared");
            String activityData = strategy.createRecipientsActivityData(sharedMap.get("totalShared"));
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(item.getDuid(), nxl.getOwnerMembership(), principal.getUserId(), Operations.RESHARE, principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.ALLOW, new Date(), activityData, accountType);
            RemoteLoggingMgmt.saveActivityLog(activity);
            String newTransactionId = null;

            if (!newShared.isEmpty()) {
                SharedFileDTO dto = SharedFileManager.generateSharedFileDTO(principal.getDeviceId(), principal.getDeviceType().name(), item.getDuid(), item.getFilePathDisplay(), item.getFilePath(), fileName, item.getRepoId(), null, principal.getTenantId(), principal.getUserId(), principal.getTicket(), principal.getEmail(), (Set<String>)newShared, nxl.getRights(), new Date(), false, nxl.getOwnerMembership(), principal.getClientId(), principal.getPlatformId(), comment, GsonUtils.GSON.toJson(nxl.getPolicy()));
                dto.setExpiryStr(SharedFileManager.getExpiryString(nxl.getValidity()));
                if (SHARESPACE.PROJECTSPACE.equals(source.getExistingTransaction().getFromSpace())) {
                    dto.setProjectId(item.getProjectId());
                    dto.setSourceProjectId((Integer)src);
                }
                newTransactionId = mapper.updateSharingTransaction(dto, principal.getUserId(), null);
                strategy.sendNotification(request, dto, newTransactionId, principal.getName(), principal.getLoginTenant());
            }
            if (newTransactionId != null) {
                resp.putResult("newTransactionId", newTransactionId);
                resp.putResult("sharedLink", strategy.getSharedLink(request, principal.getLoginTenant(), newTransactionId));
            }
            resp.putResult("alreadySharedList", sharedMap.get("alreadyShared"));
            resp.putResult("newSharedList", sharedMap.get("newShared"));
            resp.putResult("protectionType", nxl.getProtectionType());
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    /*
     * 1. process sharing list
     * 2. get nxlMetadata with duid
     * 3. check share rights by nxl metadata
     * 4. create recipient activity data
     * 5. generate sharing dto and update sharing transaction
     * 6. send notification
     * 7. process result
     */
    @Override
    public JsonResponse update(HttpServletRequest request, String duid, List<JsonRecipient> newRecipients,
        List<JsonRecipient> removedRecipients, String comment) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            List<?> sharedList = mapper.getRecipientList(duid, newRecipients);

            Map<String, Set<?>> sharedMap = strategy.processRecipientsList(sharedList, newRecipients);
            StoreItem item = mapper.getStoreItemByDuid(duid);
            if (item == null) {
                throw new ValidateException(403, "Access denied");
            }

            String fileName = StringUtils.substringAfterLast(item.getFilePathDisplay(), "/");
            NxlMetaData nxl = strategy.resolveNxlMetadataFromStoreItem(item, principal);
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);

            try {
                strategy.validateShareRights(item.getProjectId(), item.getFilePath(), fileName, principal, item, us, nxl);
            } catch (ValidateException ex) {
                String activityData = strategy.createRecipientsActivityData(sharedMap.get("newShared"));
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, nxl.getOwnerMembership(), principal.getUserId(), Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.DENY, new Date(), activityData, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw ex;
            }
            SharingTransaction sharingTransaction = mapper.getFirstTransactionByDuid(duid);
            if (sharingTransaction == null) {
                throw new ValidateException(4002, "No sharing transaction has been performed");
            }

            if (strategy.isExpired(item)) {
                if (!newRecipients.isEmpty()) {
                    String activityData = strategy.createRecipientsActivityData(newRecipients);
                    RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, nxl.getOwnerMembership(), principal.getUserId(), Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.DENY, new Date(), activityData, accountType);
                    RemoteLoggingMgmt.saveActivityLog(activity);
                }
                throw new ValidateException(4003, "File is expired");
            }

            List<?> list = mapper.getRecipientList(duid, removedRecipients);
            Set<?> removed = null;
            if (list != null && !list.isEmpty()) {
                removed = mapper.removeRecipients(list);
                String activityData = strategy.createRecipientsActivityData(removed);
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, nxl.getOwnerMembership(), principal.getUserId(), strategy.getRemoveRecipientOperation(), principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.ALLOW, new Date(), activityData, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
            }

            Set<?> newShared = sharedMap.get("newShared");
            if (!newShared.isEmpty()) {
                SharedFileDTO dto = SharedFileManager.generateSharedFileDTO(principal.getDeviceId(), principal.getDeviceType().name(), duid, item.getFilePathDisplay(), item.getFilePath(), fileName, item.getRepoId(), null, null, principal.getUserId(), principal.getTicket(), principal.getEmail(), (Set<String>)newShared, nxl.getRights(), new Date(), false, null, principal.getClientId(), principal.getPlatformId(), comment, GsonUtils.GSON.toJson(nxl.getPolicy()));
                dto.setSourceProjectId(item.getProjectId());
                dto.setExpiryStr(SharedFileManager.getExpiryString(nxl.getValidity()));
                String transactionId = mapper.updateSharingTransaction(dto, principal.getUserId(), null);
                String activityData = strategy.createRecipientsActivityData(newShared);
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, nxl.getOwnerMembership(), principal.getUserId(), Operations.SHARE, principal.getDeviceId(), principal.getPlatformId(), item.getRepoId(), item.getFilePath(), fileName, item.getFilePathDisplay(), null, null, null, Constants.AccessResult.ALLOW, new Date(), activityData, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
                strategy.sendNotification(request, dto, transactionId, principal.getName(), principal.getLoginTenant());
            }

            resp.putResult("newRecipients", newShared);
            resp.putResult("removedRecipients", removed);
            resp.putResult("alreadySharedList", sharedMap.get("alreadyShared"));
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    /*
     * 1. get nxl metadata by duid
     * 2. check revoke rights
     * 3. revoke sharing transaction
     * */
    @Override
    public JsonResponse revoke(HttpServletRequest request, String duid) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            StoreItem item = mapper.getStoreItemByDuid(duid);
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            strategy.validateRevokedFiles(item, principal, us);
            NxlMetaData metaData = strategy.resolveNxlMetadataFromStoreItem(item, principal);
            boolean revoked = mapper.revokeFile(duid, principal.getUserId());
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(duid, metaData.getOwnerMembership(), principal.getUserId(), Operations.REVOKE, principal.getDeviceId(), principal.getPlatformId(), null, null, null, null, null, null, null, revoked ? Constants.AccessResult.ALLOW : Constants.AccessResult.DENY, new Date(), null, accountType);
            RemoteLoggingMgmt.saveActivityLog(activity);
            if (!revoked) {
                throw new ValidateException(403, "Forbidden");
            }
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    /*
     * 1. call list files in mapper class
     * 2. set sharing link
     * */
    @Override
    public JsonResponse listSharedWithMeFiles(HttpServletRequest request, Integer page, Integer size,
        String orderBy,
        String searchString, Object spaceId) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            strategy.validateListSharedFiles(spaceId, principal);
            JsonSharedWithMeFileList list = mapper.getSharedWithMeFiles(page, size, orderBy, searchString, spaceId);
            for (JsonSharedWithMeFile jsonSharedWithMeFile : list.getFiles()) {
                jsonSharedWithMeFile.setSharedLink(strategy.getSharedLink(request, principal.getLoginTenant(), jsonSharedWithMeFile.getTransactionId()));
            }
            resp.putResult("detail", list);
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    @Override
    public JsonResponse getSharedWithMeFileMetadata(HttpServletRequest request, String transactionId, String spaceId) {
        JsonResponse resp = new JsonResponse("OK");
        try {
            String recipientMembership = strategy.getRecipientMembership(principal, spaceId);
            JsonSharedWithMeFile fileMetadata = mapper.getSharedWithMeFile(transactionId, principal, recipientMembership);
            fileMetadata.setSharedLink(strategy.getSharedLink(request, principal.getLoginTenant(), fileMetadata.getTransactionId()));
            resp.putResult("detail", fileMetadata);
        } catch (Throwable e) {
            ExceptionProcessor.parseToJsonResponse(resp, e);
        }
        return resp;
    }

    /*
     * 1. get storeItem by transactionId
     * 2. check download right
     * 3. use default repository to download file
     * 4. activity log
     * */
    @Override
    public Response downloadSharedWithMeFile(HttpServletResponse response, String transactionId, int start, long length,
        boolean downloadForView, Object spaceId, HttpServletRequest request) {
        try {
            StoreItem storeItem = source.getStoreItem();
            response.setHeader("x-rms-last-modified", String.valueOf(storeItem.getLastModified().getTime()));
            response.setHeader("x-rms-file-duid", storeItem.getDuid());
            response.setHeader("x-rms-file-membership", Nvl.nvl(source.getMetaData().getOwnerMembership()));
            RemoteLoggingMgmt.Activity activity = null;
            boolean partialDownload = start >= 0 && length >= 0;
            try {
                strategy.validateDownloadRights(source.getMetaData(), spaceId, source.getStoreItem(), principal, source.existingTransaction.getId(), downloadForView, source.getMetaData());
                IRepository repository = strategy.getDefaultRepository(principal, source);
                strategy.downloadFile(principal, storeItem, source, request, response, accountType, start, length, downloadForView, spaceId);
                activity = new RemoteLoggingMgmt.Activity(storeItem.getDuid(), Nvl.nvl(source.getMetaData().getOwnerMembership()), principal.getUserId(), Operations.DOWNLOAD, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(repository.getRepoId()), storeItem.getFilePath(), storeItem.getFilePath(), storeItem.getFilePath(), null, null, null, Constants.AccessResult.ALLOW, new Date(), null, accountType);
            } catch (ValidateException ex) {
                activity = new RemoteLoggingMgmt.Activity(storeItem.getDuid(), Nvl.nvl(source.getMetaData().getOwnerMembership()), principal.getUserId(), Operations.VIEW, principal.getDeviceId(), principal.getPlatformId(), storeItem.getRepoId(), storeItem.getFilePath(), storeItem.getFilePath(), storeItem.getFilePath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, accountType);
                createLog(activity, partialDownload, downloadForView);
                throw ex;
            }
            createLog(activity, partialDownload, downloadForView);
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        }
    }

    private void createLog(RemoteLoggingMgmt.Activity activity, boolean partialDownload, boolean downloadForView) {

        if (partialDownload && !downloadForView) {
            activity.setOperation(Operations.DOWNLOAD_PARTIAL_DOWNLOAD);
        }

        if (partialDownload && downloadForView) {
            activity.setOperation(Operations.VIEW_PARTIAL_DOWNLOAD);
        }

        if (!downloadForView && !partialDownload) {
            activity.setOperation(Operations.DOWNLOAD);
        }

        if (downloadForView && !partialDownload) {
            activity.setOperation(Operations.VIEW);
        }
        RemoteLoggingMgmt.saveActivityLog(activity);

    }

    /*
     * 1. get storeItem by transactionId
     * 2. check no right
     * 3. use default repository to download file header
     * 4. activity log
     * */
    @Override
    public Response downloadSharedWithMeFileHeader(HttpServletResponse response, String transactionId, int start,
        long length, Object spaceId, HttpServletRequest request)
            throws InvalidDefaultRepositoryException, RepositoryException, IOException {
        try {
            StoreItem storeItem = source.getStoreItem();
            response.setHeader("x-rms-last-modified", String.valueOf(storeItem.getLastModified().getTime()));
            response.setHeader("x-rms-file-duid", storeItem.getDuid());
            response.setHeader("x-rms-file-membership", Nvl.nvl(source.getMetaData().getOwnerMembership()));

            IRepository repository = strategy.getDefaultRepository(principal, source);
            strategy.downloadFile(principal, storeItem, source, request, response, accountType, start, length, false, spaceId);

            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(storeItem.getDuid(), Nvl.nvl(source.getMetaData().getOwnerMembership()), principal.getUserId(), Operations.HEADER_DOWNLOAD, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(repository.getRepoId()), storeItem.getFilePath(), storeItem.getFilePath(), storeItem.getFilePath(), null, null, null, Constants.AccessResult.ALLOW, new Date(), null, accountType);
            RemoteLoggingMgmt.saveActivityLog(activity);

            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        }
    }

    @Override
    public Response decryptSharedWithMeFile(HttpServletRequest request, HttpServletResponse response,
        String transactionId, String spaceId) {
        File outputPath = null;
        try {
            StoreItem storeItem = source.getStoreItem();
            response.setHeader("x-rms-last-modified", String.valueOf(storeItem.getLastModified().getTime()));
            response.setHeader("x-rms-file-duid", storeItem.getDuid());
            response.setHeader("x-rms-file-membership", Nvl.nvl(source.getMetaData().getOwnerMembership()));

            try {
                strategy.validateDecryptRight(storeItem, principal, transactionId, Integer.valueOf(spaceId), source.getMetaData());
            } catch (ValidateException ex) {
                RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(storeItem.getDuid(), Nvl.nvl(source.getMetaData().getOwnerMembership()), principal.getUserId(), Operations.DECRYPT, principal.getDeviceId(), principal.getPlatformId(), storeItem.getRepoId(), storeItem.getFilePath(), storeItem.getFilePath(), storeItem.getFilePath(), null, null, null, Constants.AccessResult.DENY, new Date(), null, accountType);
                RemoteLoggingMgmt.saveActivityLog(activity);
                throw ex;
            }

            IRepository repository = strategy.getDefaultRepository(principal, source);

            outputPath = RepositoryFileUtil.getTempOutputFolder();
            File output = repository.getFile(storeItem.getFilePath(), storeItem.getFilePath(), outputPath.getPath());

            File decryptedFile = null;
            try (InputStream is = new FileInputStream(output);
                    NxlFile nxl = NxlFile.parse(is)) {
                try {
                    byte[] token = strategy.requestDecryptToken(request, principal, nxl, spaceId);
                    if (!nxl.isValid(token)) {
                        throw new NxlException("Invalid token.");
                    }
                    FileInfo fileInfo = DecryptUtil.getInfo(nxl, token);
                    String originalFileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : FileUtils.removeExtension(output.getName());
                    decryptedFile = new File(output.getParent(), originalFileName);
                    try (OutputStream fos = new FileOutputStream(decryptedFile)) {
                        DecryptUtil.decrypt(nxl, token, fos);
                    }
                } catch (JsonException e) {
                    NxlException ne = new NxlException("Invalid token");
                    ne.initCause(e);
                    throw ne;
                }
            }
            output = decryptedFile;

            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, HTTPUtil.getContentDisposition(decryptedFile.getName()));
            if (output.length() > 0) {
                response.setHeader("x-rms-file-size", Long.toString(output.length()));
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(output.length()));
                try (InputStream fis = new FileInputStream(output)) {
                    IOUtils.copy(fis, response.getOutputStream());
                }
            }
            RemoteLoggingMgmt.Activity activity = new RemoteLoggingMgmt.Activity(storeItem.getDuid(), Nvl.nvl(source.getMetaData().getOwnerMembership()), principal.getUserId(), Operations.DECRYPT, principal.getDeviceId(), principal.getPlatformId(), String.valueOf(repository.getRepoId()), storeItem.getFilePath(), storeItem.getFilePath(), storeItem.getFilePath(), null, null, null, Constants.AccessResult.ALLOW, new Date(), null, accountType);
            RemoteLoggingMgmt.saveActivityLog(activity);
            return Response.ok().type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            return ExceptionProcessor.parseToJAXRSResponse(e);
        } finally {
            if (outputPath != null) {
                FileUtils.deleteQuietly(outputPath);
            }
        }
    }

    public File downloadFileForTransfer(RMSUserPrincipal principal, File tempTrfFolder)
            throws InvalidDefaultRepositoryException, RepositoryException {
        IRepository repository = strategy.getDefaultRepository(principal, source);
        StoreItem storeItem = source.getStoreItem();
        return repository.getFile(storeItem.getFilePath(), storeItem.getFilePath(), tempTrfFolder.getPath());
    }

    public byte[] downloadPartialFile(long startByte, long endByte)
            throws InvalidDefaultRepositoryException, RepositoryException {
        IRepository repository = strategy.getDefaultRepository(principal, source);
        StoreItem storeItem = source.getStoreItem();
        return repository.downloadPartialFile(storeItem.getFilePath(), storeItem.getFilePath(), startByte, endByte);
    }

    public boolean checkFileExists() {
        StoreItem storeItem = source.getStoreItem();
        return storeItem != null && !storeItem.isDeleted();
    }
}
