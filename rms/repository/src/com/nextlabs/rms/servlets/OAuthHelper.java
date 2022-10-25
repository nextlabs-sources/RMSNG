package com.nextlabs.rms.servlets;

import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.RepositoryManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class OAuthHelper {

    public static final String REDIRECT_URL_MANAGE_REPOSITORIES = "/main#/app/repositories";
    public static final String REDIRECT_URL_REPOSITORIES = "/main#/app/repositories/";
    public static final int NO_REDIRECTION = 0;

    private OAuthHelper() {
    }

    public static JsonResponse addRecordToDB(DbSession session, RMSUserPrincipal user,
        String repoName, String accountIdTo, String accountIdFrom, String accountNameTo, String token,
        ServiceProviderSetting serviceProviderSetting, boolean reauthenticate, boolean isShared,
        HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        Criteria criteria = session.createCriteria(Repository.class);
        criteria.add(Restrictions.eq("userId", user.getUserId()));
        criteria.add(Restrictions.eq("accountId", accountIdTo).ignoreCase());

        /*
         * some repositories use display name as account name so use the following criteria only
         * for SPONLINE where we store siteURL.We want to allow a user to add multiple sites
         * using same account, so we need to include the following condition for SPONLINE
         */
        if (ServiceProviderType.SHAREPOINT_ONLINE.equals(serviceProviderSetting.getProviderType())) {
            //for SPOnline, we already store site in lowercase. so, we can convert the parameter to lowercase
            criteria.add(Restrictions.eq("accountName", accountNameTo.toLowerCase()));
        }

        criteria.add(Restrictions.eq("providerId", serviceProviderSetting.getId()));
        Repository repo = (Repository)criteria.uniqueResult();
        if (reauthenticate) {
            if (repo != null && StringUtils.equals(serviceProviderSetting.getId(), repo.getProviderId()) && accountIdTo.equalsIgnoreCase(accountIdFrom)) {
                repo.setToken(token);
                session.beginTransaction();
                session.update(repo);
                session.flush();
                session.commit();
                JsonRepository jsonRepo = toJson(repo, serviceProviderSetting.getProviderType().name());
                jsonRepo.setToken(null);
                JsonResponse res = new JsonResponse("OK");
                res.putResult("repository", jsonRepo);
                res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_REPOSITORIES + repo.getId());
                return res;
            } else {
                String msg = RMSMessageHandler.getClientString("incorrectRepoAuth", ServiceProviderSetting.getProviderTypeDisplayName(serviceProviderSetting.getProviderType().name()));
                JsonResponse res = new JsonResponse(5006, "wrong service provider account");
                res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
                return res;
            }
        } else {
            if (repo != null) {
                return sendRepoExistsResponse(repo, serviceProviderSetting.getProviderType().name());
            }
        }
        repo = new Repository();
        repo.setName(repoName);
        repo.setProviderId(serviceProviderSetting.getId());
        repo.setUserId(user.getUserId());
        repo.setShared(isShared ? 1 : 0);
        repo.setAccountId(accountIdTo);
        repo.setAccountName(accountNameTo);
        repo.setToken(token);
        repo.setCreationTime(new Date());

        try {
            RepositoryManager.addRepository(session, user, repo);
            RepositoryManager.setCookie(response, "repoAddedFirstTime", "true");
        } catch (RepositoryAlreadyExists e) {
            return sendRepoExistsResponse(repo, serviceProviderSetting.getProviderType().name());
        } catch (DuplicateRepositoryNameException e) {
            String msg = RMSMessageHandler.getClientString("error_duplicate_repo_name", repoName);
            JsonResponse res = new JsonResponse(409, "Duplicate repository name");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
            return res;
        } catch (BadRequestException e) {
            String msg = RMSMessageHandler.getClientString("invalidRepoName");
            JsonResponse res = new JsonResponse(4003, "Repository Name containing illegal special characters");
            res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
            return res;
        }

        JsonRepository jsonRepo = toJson(repo, serviceProviderSetting.getProviderType().name());
        jsonRepo.setToken(null);
        JsonResponse res = new JsonResponse("OK");
        res.putResult("repository", jsonRepo);
        res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_REPOSITORIES + repo.getId());
        return res;
    }

    public static Repository getExistingRepo(int userId, ServiceProviderSetting setting, String accountId,
        String repoName)
            throws UnsupportedEncodingException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Repository.class);
            criteria.add(Restrictions.and(Restrictions.eq("providerId", setting.getId()), Restrictions.and(Restrictions.eq("userId", userId))));
            if (StringUtils.hasText(accountId)) {
                criteria.add(Restrictions.eq("accountId", accountId));
            }
            if (StringUtils.hasText(repoName)) {
                criteria.add(Restrictions.eq("name", repoName));
            }
            return (Repository)criteria.uniqueResult();
        }
    }

    public static JsonResponse sendRepoExistsResponse(Repository repo, String repoType)
            throws UnsupportedEncodingException {
        String msg = RMSMessageHandler.getClientString("repoAlreadyExists", ServiceProviderSetting.getProviderTypeDisplayName(repoType));
        JsonResponse res = new JsonResponse(304, "Repository already exists");
        res.putResult("repository", toJson(repo, repoType));
        res.putResult(RepoConstants.KEY_REDIRECT_URL, REDIRECT_URL_MANAGE_REPOSITORIES + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
        return res;
    }

    private static JsonRepository toJson(Repository repository, String repoType) {
        JsonRepository json = new JsonRepository();
        json.setType(repoType);
        json.setAccountId(repository.getAccountId());
        json.setAccountName(repository.getAccountName());

        Date now = new Date();

        if (repository.getCreationTime() != null) {
            json.setCreationTime(repository.getCreationTime().getTime());
        } else {
            json.setCreationTime(now.getTime());
        }

        json.setName(repository.getName());
        json.setPreference(repository.getPreference());
        json.setRepoId(repository.getId());
        json.setShared(org.apache.commons.lang3.BooleanUtils.toBoolean(repository.getShared()));
        json.setToken(repository.getToken());
        if (repository.getLastUpdatedTime() != null) {
            json.setUpdatedTime(repository.getLastUpdatedTime().getTime());
        } else {
            json.setUpdatedTime(now.getTime());
        }
        return json;
    }
}
