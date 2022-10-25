package com.nextlabs.rms.services.manager;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnSupportedStorageProviderException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.pojo.SyncProfileDataContainer;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.rmc.AddRepositoryRequestDocument;
import com.nextlabs.rms.rmc.AddRepositoryResponseDocument;
import com.nextlabs.rms.rmc.AddRepositoryResponseDocument.AddRepositoryResponse;
import com.nextlabs.rms.rmc.GetRepositoryDetailsRequestDocument;
import com.nextlabs.rms.rmc.GetRepositoryDetailsResponseDocument;
import com.nextlabs.rms.rmc.GetRepositoryDetailsResponseDocument.GetRepositoryDetailsResponse;
import com.nextlabs.rms.rmc.RemoveRepositoryRequestDocument;
import com.nextlabs.rms.rmc.RemoveRepositoryResponseDocument;
import com.nextlabs.rms.rmc.RemoveRepositoryResponseDocument.RemoveRepositoryResponse;
import com.nextlabs.rms.rmc.StatusTypeEnum;
import com.nextlabs.rms.rmc.UpdateRepositoryRequestDocument;
import com.nextlabs.rms.rmc.UpdateRepositoryResponseDocument;
import com.nextlabs.rms.rmc.UpdateRepositoryResponseDocument.UpdateRepositoryResponse;
import com.nextlabs.rms.rmc.types.DeletedItemIdListType;
import com.nextlabs.rms.rmc.types.RepositoryListType;
import com.nextlabs.rms.rmc.types.RepositoryType;
import com.nextlabs.rms.rmc.types.StatusType;
import com.nextlabs.rms.rmc.types.StorageProviderTypeEnum;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public final class RepositorySvcManager {

    private static RepositorySvcManager instance = new RepositorySvcManager();

    private RepositorySvcManager() {
    }

    public static RepositorySvcManager getInstance() {
        return instance;
    }

    private StatusType getStatus(int statusCode, String statusMsgLabel) {
        StatusType status = StatusType.Factory.newInstance();
        status.setCode(statusCode);
        status.setMessage(RMSMessageHandler.getClientString(statusMsgLabel));
        return status;
    }

    public AddRepositoryResponseDocument addRepositoryRequest(RMSUserPrincipal user,
        AddRepositoryRequestDocument.AddRepositoryRequest request) throws UserNotFoundException,
            RepositoryAlreadyExists, DuplicateRepositoryNameException, UnSupportedStorageProviderException,
            BadRequestException {
        AddRepositoryResponseDocument doc = AddRepositoryResponseDocument.Factory.newInstance();
        AddRepositoryResponse response = doc.addNewAddRepositoryResponse();
        RepositoryType repo = request.getRepository();
        DbSession session = DbSession.newSession();
        try {
            StorageProviderTypeEnum.Enum xmlType = repo.getType();
            if (xmlType == null) {
                throw new UnSupportedStorageProviderException();
            }
            ServiceProviderType type = ServiceProviderType.valueOf(xmlType.toString());
            if (type == null) {
                throw new UnSupportedStorageProviderException();
            }
            ServiceProviderSetting provider = SettingManager.getStorageProviderSettings(session, user.getTenantId(), type);
            Repository repoDO = toRepositoryDO(user, repo, provider.getId());
            repoDO = RepositoryManager.addRepository(session, user, repoDO);
            response.setRepoId(repoDO.getId());
            response.setStatus(getStatus(StatusTypeEnum.SUCCESS.getCode(), StatusTypeEnum.SUCCESS.getMessageLabel()));
            return doc;
        } finally {
            session.close();
        }
    }

    public UpdateRepositoryResponseDocument updateRepositoryRequest(RMSUserPrincipal user,
        UpdateRepositoryRequestDocument.UpdateRepositoryRequest request) throws RepositoryNotFoundException,
            UserNotFoundException, UnauthorizedOperationException, DuplicateRepositoryNameException,
            BadRequestException, ForbiddenOperationException {
        UpdateRepositoryResponseDocument doc = UpdateRepositoryResponseDocument.Factory.newInstance();
        UpdateRepositoryResponse response = doc.addNewUpdateRepositoryResponse();
        String repoId = request.getRepoId();
        String name = request.getName();
        String token = request.getToken();

        if (!StringUtils.hasText(repoId)) {
            throw new RepositoryNotFoundException(null);
        }

        if (!StringUtils.hasText(token) && !StringUtils.hasText(name)) {
            response.setStatus(getStatus(StatusTypeEnum.BAD_REQUEST.getCode(), StatusTypeEnum.BAD_REQUEST.getMessageLabel()));
            return doc;
        }

        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            if (StringUtils.hasText(token)) {
                RepositoryManager.updateClientToken(session, user, repoId, token);
            }
            if (StringUtils.hasText(name)) {
                RepositoryManager.updateRepositoryName(session, user, repoId, name);
            }
            session.commit();
            response.setStatus(getStatus(StatusTypeEnum.SUCCESS.getCode(), StatusTypeEnum.SUCCESS.getMessageLabel()));
            return doc;
        } finally {
            session.close();
        }
    }

    private Repository toRepositoryDO(RMSUserPrincipal user, RepositoryType xmlRepo, String spId) {
        Repository repo = new Repository();
        repo.setAccountId(xmlRepo.getAccountId());
        repo.setName(xmlRepo.getName());
        repo.setProviderId(spId);
        repo.setUserId(user.getUserId());
        repo.setShared(xmlRepo.getIsShared() ? 1 : 0);
        repo.setAccountName(xmlRepo.getAccountName());
        repo.setAccountId(xmlRepo.getAccountId());
        repo.setIosToken(xmlRepo.getToken());
        repo.setCreationTime(new Date(xmlRepo.getCreationTime()));
        return repo;
    }

    public RemoveRepositoryResponseDocument removeRepositoryRequest(RMSUserPrincipal user,
        RemoveRepositoryRequestDocument.RemoveRepositoryRequest request)
            throws RepositoryNotFoundException, UnauthorizedOperationException, ForbiddenOperationException {
        RemoveRepositoryResponseDocument doc = RemoveRepositoryResponseDocument.Factory.newInstance();
        RemoveRepositoryResponse response = doc.addNewRemoveRepositoryResponse();
        String repoId = request.getRepoId();

        DbSession session = DbSession.newSession();
        try {
            RepositoryManager.removeRepository(session, user, repoId);
            response.setStatus(getStatus(StatusTypeEnum.SUCCESS.getCode(), StatusTypeEnum.SUCCESS.getMessageLabel()));
            return doc;
        } finally {
            session.close();
        }
    }

    public GetRepositoryDetailsResponseDocument getRepositoryDetailsRequest(RMSUserPrincipal user,
        GetRepositoryDetailsRequestDocument.GetRepositoryDetailsRequest request) {
        GetRepositoryDetailsResponseDocument doc = GetRepositoryDetailsResponseDocument.Factory.newInstance();
        Calendar timestamp = request.getFetchSinceGMTTimestamp();
        GetRepositoryDetailsResponseDocument.GetRepositoryDetailsResponse response = fetchAndPopulateSyncData(user, timestamp.getTime());
        response.setStatus(getStatus(StatusTypeEnum.SUCCESS.getCode(), StatusTypeEnum.SUCCESS.getMessageLabel()));
        doc.setGetRepositoryDetailsResponse(response);
        return doc;
    }

    private void convertFromDO(Repository repoDO, RepositoryType repo, String repoType) {
        if (repoDO == null || repo == null) {
            return;
        }
        repo.setAccountId(repoDO.getAccountId());
        repo.setIsShared(repoDO.getShared() == 1);
        repo.setRepoId(repoDO.getId());
        repo.setName(repoDO.getName());
        repo.setType(StorageProviderTypeEnum.Enum.forString(repoType));
        repo.setAccountName(repoDO.getAccountName());
        repo.setToken(repoDO.getIosToken());
        repo.setCreationTime(repoDO.getCreationTime().getTime());
        repo.setUpdatedTime(repoDO.getCreationTime().getTime());
        repo.setPreference(repoDO.getPreference());
    }

    private RepositoryListType convertToXMLRepoItems(DbSession session, List<Repository> repoDOList) {
        RepositoryListType repoList = RepositoryListType.Factory.newInstance();
        for (Repository repoDO : repoDOList) {
            StorageProvider provider = session.get(StorageProvider.class, repoDO.getProviderId());
            if (provider == null) {
                continue;
            }
            ServiceProviderType type = ServiceProviderType.getByOrdinal(provider.getType());
            if (type != null) {
                convertFromDO(repoDO, repoList.addNewRepository(), type.name());
            }
        }
        return repoList;
    }

    private GetRepositoryDetailsResponse fetchAndPopulateSyncData(RMSUserPrincipal tUser, Date timestamp) {
        GetRepositoryDetailsResponse response = GetRepositoryDetailsResponse.Factory.newInstance();
        DbSession session = DbSession.newSession();
        try {
            SyncProfileDataContainer container = RepositoryManager.getSyncDataUpdatedOnOrAfter(session, tUser, timestamp);
            response.setIsFullCopy(container.isFullCopy());
            response.setRepoItems(convertToXMLRepoItems(session, container.getRepositoryList()));

            if (!container.isFullCopy()) {
                response.setDeletedRepoItems(convertToDeletedItemListType(container.getDeletedRepositoryList()));
            }
            return response;
        } finally {
            session.close();
        }

    }

    private DeletedItemIdListType convertToDeletedItemListType(List<String> deletedList) {
        DeletedItemIdListType list = DeletedItemIdListType.Factory.newInstance();

        if (deletedList == null || deletedList.isEmpty()) {
            return list;
        }

        for (String id : deletedList) {
            list.addItemId(id);
        }
        return list;
    }
}
