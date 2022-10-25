package com.nextlabs.rms.repository;

import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.RepoItemMetadata;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.pojo.SyncProfileDataContainer;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryPersonal;
import com.nextlabs.rms.serviceprovider.SupportedProvider;
import com.nextlabs.rms.shared.HTTPUtil;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public final class RepositoryManager {

    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ACCESS_TOKEN_EXPIRY_TIME = "access_token_expiry_time";
    public static final String REPOSITORY_COOKIE_NAME = "rpredirect";
    public static final String BOX_REPOSIOTRY_STATE = "box_state";

    private RepositoryManager() {
    }

    public static boolean isRepoHiddenFromUser(UserSession userSession, RMSUserPrincipal userPrincipal,
        ServiceProviderType spType) {
        boolean ldapUser = userSession != null && userSession.getLoginType() == LoginType.LDAP;
        boolean admin = userPrincipal.isAdmin();
        return spType == ServiceProviderType.SHAREPOINT_ONPREMISE && !ldapUser && !admin;
    }

    private static List<Repository> getRepoListWithTenant(DbSession session, RMSUserPrincipal userPrincipal,
        boolean personalRepoOnly) {
        UserSession userSession = AuthManager.getUserSession(session, userPrincipal);
        Criteria criteria = session.createCriteria(Repository.class);
        Criterion userCriterion = Restrictions.eq("userId", userPrincipal.getUserId());
        Criterion applicationAccountCriterion = Restrictions.eq("providerClass", SupportedProvider.ProviderClass.APPLICATION.ordinal());
        if (personalRepoOnly) {
            criteria.add(Restrictions.or(userCriterion, applicationAccountCriterion));
        } else {
            criteria.add(Restrictions.or(Restrictions.or(userCriterion, applicationAccountCriterion), Restrictions.eq("shared", 1)));
        }
        @SuppressWarnings("unchecked")
        List<Repository> repoList = (List<Repository>)criteria.list();
        Iterator<Repository> it = repoList.iterator();
        while (it.hasNext()) {
            Repository repo = it.next();
            StorageProvider sp = session.get(StorageProvider.class, repo.getProviderId());
            if (!StringUtils.equalsIgnoreCase(sp.getTenantId(), userPrincipal.getTenantId()) || isRepoHiddenFromUser(userSession, userPrincipal, ServiceProviderType.getByOrdinal(sp.getType()))) {
                it.remove();
            }
        }
        return repoList;
    }

    private static List<JsonRepository> getRepositoryListByTenant(DbSession session, RMSUserPrincipal userPrincipal,
        DeviceType deviceType) {
        List<Repository> repositories = getRepoListWithTenant(session, userPrincipal, true);
        List<JsonRepository> result = Collections.emptyList();
        if (!repositories.isEmpty()) {
            Set<String> providerIds = new HashSet<>(repositories.size());
            for (Repository repository : repositories) {
                providerIds.add(repository.getProviderId());
            }
            Criteria criteria = session.createCriteria(StorageProvider.class);
            criteria.add(Restrictions.in("id", providerIds));
            @SuppressWarnings("unchecked")
            List<StorageProvider> storageProviders = criteria.list();
            Map<String, StorageProvider> cache = new HashMap<>(storageProviders.size());
            for (StorageProvider provider : storageProviders) {
                cache.put(provider.getId(), provider);
            }
            result = new ArrayList<>(repositories.size());
            for (Repository repository : repositories) {
                String providerId = repository.getProviderId();
                Date lastUpdatedTime = repository.getLastUpdatedTime();
                StorageProvider storageProvider = cache.get(providerId);
                JsonRepository json = new JsonRepository();
                json.setType(ServiceProviderType.getByOrdinal(storageProvider.getType()).name());
                json.setAccountId(repository.getAccountId());
                json.setAccountName(repository.getAccountName());
                json.setCreationTime(repository.getCreationTime().getTime());
                json.setName(repository.getName());
                json.setPreference(repository.getPreference());
                json.setRepoId(repository.getId());
                json.setShared(org.apache.commons.lang3.BooleanUtils.toBoolean(repository.getShared()));
                json.setDefault(repository.getName().equals(DefaultRepositoryManager.IN_BUILT_REPO_NAME));
                json.setProviderClass(SupportedProvider.ProviderClass.values()[repository.getProviderClass()].name());

                if (deviceType.isIOS()) {
                    json.setToken(repository.getIosToken());
                } else if (deviceType.isAndroid()) {
                    json.setToken(repository.getAndroidToken());
                } else {
                    json.setToken(repository.getToken());
                }
                if (lastUpdatedTime != null) {
                    json.setUpdatedTime(lastUpdatedTime.getTime());
                }
                result.add(json);
            }
        }
        return result;
    }

    public static boolean isRepoOwner(DbSession session, User user, String repoId) {
        Repository repository = session.get(Repository.class, repoId);
        return repository != null && repository.getUserId() == user.getId();
    }

    public static String[] getRepoIdsWithTenant(DbSession session, User user) {
        Criteria criteria = session.createCriteria(Repository.class);
        criteria.add(Restrictions.eq("userId", user.getId()));
        criteria.setProjection(Projections.property("id"));
        @SuppressWarnings("unchecked")
        List<String> repoList = (List<String>)criteria.list();
        return repoList.toArray(new String[repoList.size()]);
    }

    public static List<IRepository> getRepositoryList(DbSession session, RMSUserPrincipal userPrincipal,
        boolean isPersonalOnly) {
        List<Repository> reposWithTenant = getRepoListWithTenant(session, userPrincipal, isPersonalOnly);
        List<IRepository> repoList = new ArrayList<IRepository>();
        for (Repository repo : reposWithTenant) {
            StorageProvider sp = session.get(StorageProvider.class, repo.getProviderId());
            ServiceProviderType repoType = ServiceProviderType.values()[sp.getType()];
            boolean isDefaultRepo = DefaultRepositoryManager.isDefaultServiceProvider(repoType) && repo.getName().equals(DefaultRepositoryManager.IN_BUILT_REPO_NAME);
            if (repo.getProviderClass() == SupportedProvider.ProviderClass.PERSONAL.ordinal()) {
                IRepository repository = RepositoryFactory.getInstance().createRepository(userPrincipal, repo, sp);
                repository = isDefaultRepo ? new DefaultRepositoryPersonal(repository) : repository;
                repository.setUser(userPrincipal);
                String token = repo.getToken();
                if (token != null) {
                    repository.getAttributes().put(RepositoryManager.REFRESH_TOKEN, token);
                }
                repository.setRepoName(repo.getName());
                String accountName = repo.getAccountName();
                if (!StringUtils.hasText(accountName)) {
                    accountName = repo.getAccountId();
                }
                repository.setAccountName(accountName);
                repoList.add(repository);
            }
        }
        return repoList;
    }

    public static List<IRepository> removePersonalRepo(List<IRepository> repoList) {
        List<IRepository> newRepoList = new ArrayList<IRepository>();
        for (IRepository repo : repoList) {
            if (repo.isShared()) {
                newRepoList.add(repo);
            }
        }
        return newRepoList;
    }

    /**
     * Called from locations where we auto-update refresh tokens
     *
     * @param repoId
     * @param userId
     * @param refreshToken
     */
    public static void updateRefreshToken(String repoId, int userId, String refreshToken) {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            Repository repo = session.get(Repository.class, repoId);
            if (repo != null && repo.getUserId() == userId) {
                repo.setToken(refreshToken);
            }

            session.commit();
        } finally {
            session.close();
        }
    }

    public static void updateClientToken(DbSession session, RMSUserPrincipal userPrincipal, String repoId,
        String refreshToken,
        DeviceType deviceType) {
        Repository repo = session.get(Repository.class, repoId);
        if (deviceType.isIOS()) {
            repo.setIosToken(refreshToken);
        } else if (deviceType.isAndroid()) {
            repo.setAndroidToken(refreshToken);
        } else {
            repo.setToken(refreshToken);
        }
        session.save(repo);
    }

    /**
     * invoked by rmc mobile via xml services
     *
     * @param repoId
     * @param userPrincipal
     * @param refreshToken
     * @throws UnauthorizedOperationException
     */
    public static void updateClientToken(DbSession session, RMSUserPrincipal userPrincipal, String repoId,
        String refreshToken) throws UnauthorizedOperationException {
        updateClientToken(session, userPrincipal, repoId, refreshToken, DeviceType.IPHONE);
    }

    public static Repository getRepository(String id) {
        try (DbSession session = DbSession.newSession()) {
            return getRepository(session, id);
        }
    }

    public static ServiceProviderType getRepoType(DbSession session, Repository repo) {
        return ServiceProviderType.getByOrdinal(session.load(StorageProvider.class, repo.getProviderId()).getType());
    }

    public static Repository getRepository(DbSession session, String id) {
        return session.get(Repository.class, id);
    }

    public static StorageProvider getStorageProvider(String id) {
        try (DbSession session = DbSession.newSession()) {
            return getStorageProvider(session, id);
        }
    }

    public static StorageProvider getStorageProvider(DbSession session, String id) {
        return session.get(StorageProvider.class, id);
    }

    public static Repository addRepository(DbSession session, RMSUserPrincipal userPrincipal, Repository repository)
            throws RepositoryAlreadyExists, DuplicateRepositoryNameException, BadRequestException {

        if (repository == null) {
            return null;
        }

        if (!isNameValid(repository.getName())) {
            throw new BadRequestException();
        }

        Criteria criteria = session.createCriteria(Repository.class);
        LogicalExpression expr1 = Restrictions.and(Restrictions.and(Restrictions.eq("userId", userPrincipal.getUserId()), Restrictions.eq("accountId", repository.getAccountId())), Restrictions.eq("providerId", repository.getProviderId()));
        StorageProvider storageProvider = session.get(StorageProvider.class, repository.getProviderId());
        ServiceProviderType providerType = ServiceProviderType.getByOrdinal(storageProvider.getType());
        if (ServiceProviderType.SHAREPOINT_ONLINE.equals(providerType) || ServiceProviderType.SHAREPOINT_ONPREMISE.equals(providerType)) {
            repository.setAccountName(repository.getAccountName().toLowerCase());
            expr1 = Restrictions.and(expr1, Restrictions.eq("accountName", repository.getAccountName()));
        }
        LogicalExpression expr2 = Restrictions.and(Restrictions.eq("userId", userPrincipal.getUserId()), Restrictions.eq("name", repository.getName()));
        criteria.add(Restrictions.or(expr1, expr2));

        Repository repo = (Repository)criteria.uniqueResult();
        if (repo != null) {
            if (repository.getName().equals(repo.getName())) {
                throw new DuplicateRepositoryNameException(repo.getName(), String.format("Repository with name %s already exists.", repository.getName()));
            }
            throw new RepositoryAlreadyExists();
        }

        session.beginTransaction();
        session.save(repository);
        session.commit();
        return repository;
    }

    public static void updateRepositoryName(DbSession session, RMSUserPrincipal userPrincipal, String repoId,
        String newName) throws DuplicateRepositoryNameException, RepositoryNotFoundException, BadRequestException,
            UnauthorizedOperationException, ForbiddenOperationException {

        if (!isNameValid(newName)) {
            throw new BadRequestException();
        }

        Repository repo = session.get(Repository.class, repoId);
        if (repo == null) {
            throw new RepositoryNotFoundException(repoId);
        }

        if (DefaultRepositoryManager.isDefaultServiceProvider(getRepoType(session, repo))) {
            throw new ForbiddenOperationException();
        }

        if (repo.getName().equals(newName)) {
            return;
        }

        Criteria criteria = session.createCriteria(Repository.class);

        LogicalExpression expr2 = Restrictions.and(Restrictions.eq("userId", userPrincipal.getUserId()), Restrictions.eq("name", newName));
        criteria.add(expr2);

        Repository repoWithSameName = (Repository)criteria.uniqueResult();

        if (repoWithSameName != null && !repoWithSameName.getId().equals(repo.getId())) {
            throw new DuplicateRepositoryNameException(repo.getName());
        }
        repo.setName(newName);
        repo.setLastUpdatedTime(new Date());
        session.save(repo);
    }

    public static void removeRepository(DbSession session, RMSUserPrincipal userPrincipal, String repoId)
            throws UnauthorizedOperationException, RepositoryNotFoundException, ForbiddenOperationException {
        session.beginTransaction();
        Repository repo = session.get(Repository.class, repoId);
        if (repo == null) {
            throw new RepositoryNotFoundException(repoId);
        }

        if (repo.getUserId() != userPrincipal.getUserId()) {
            throw new UnauthorizedOperationException();
        }

        if (DefaultRepositoryManager.isDefaultServiceProvider(getRepoType(session, repo)) || repo.getProviderClass() == SupportedProvider.ProviderClass.APPLICATION.ordinal()) {
            throw new ForbiddenOperationException();
        }

        session.delete(repo);
        session.commit();
    }

    public static SyncProfileDataContainer getSyncDataUpdatedOnOrAfter(DbSession session, RMSUserPrincipal user,
        Date gmtDate) {
        return getSyncDataUpdatedOnOrAfter(session, user, gmtDate, DeviceType.IPHONE);
    }

    public static SyncProfileDataContainer getSyncDataUpdatedOnOrAfter(DbSession session, RMSUserPrincipal user,
        Date gmtDate, DeviceType deviceType) {
        SyncProfileDataContainer container = new SyncProfileDataContainer();
        List<JsonRepository> repoList = getRepositoryListByTenant(session, user, deviceType);
        container.setFullCopy(true);
        container.setRepositoryJsonList(repoList);
        return container;
    }

    public static void clearCookieRedirectParameters(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (StringUtils.equals(cookie.getName(), REPOSITORY_COOKIE_NAME)) {
                    cookie.setMaxAge(0);
                    cookie.setValue(null);
                    cookie.setPath(request.getContextPath());
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }

    public static boolean validateCookieRedirectParameters(HttpServletRequest request, HttpServletResponse response,
        String accountIdFrom) throws UnsupportedEncodingException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (StringUtils.equals(cookie.getName(), REPOSITORY_COOKIE_NAME)) {
                    String decoded = URLDecoder.decode(cookie.getValue(), "UTF-8");
                    if (StringUtils.equals(decoded, accountIdFrom)) {
                        return true;
                    } else {
                        cookie.setMaxAge(0);
                        cookie.setValue(null);
                        response.addCookie(cookie);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static void setCookieRedirectParameters(HttpServletResponse response, String accountId)
            throws UnsupportedEncodingException {
        setCookie(response, REPOSITORY_COOKIE_NAME, URLEncoder.encode(accountId, "UTF-8"));
    }

    public static void setCookie(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setSecure(true);
        cookie.setMaxAge((int)TimeUnit.MINUTES.toSeconds(30L));
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static String getAuthorizationUrl(HttpServletRequest request, ServiceProviderType providerType,
        Repository repository, boolean useJsonEndPoint) {
        final String url = HTTPUtil.getURI(request);
        StringBuilder urlBuilder = new StringBuilder(url);
        if (useJsonEndPoint) {
            urlBuilder.append("/json");
        }
        urlBuilder.append('/');
        if (ServiceProviderType.DROPBOX == providerType) {
            return urlBuilder.append(RepoConstants.DROPBOX_AUTH_START_URL).toString();
        } else if (ServiceProviderType.GOOGLE_DRIVE == providerType) {
            return urlBuilder.append(RepoConstants.GOOGLE_DRIVE_AUTH_START_URL).toString();
        } else if (ServiceProviderType.BOX == providerType) {
            return urlBuilder.append(RepoConstants.BOX_AUTH_START_URL).toString();
        } else if (ServiceProviderType.ONE_DRIVE == providerType) {
            return urlBuilder.append(RepoConstants.ONE_DRIVE_AUTH_START_URL).toString();
        } else if (ServiceProviderType.SHAREPOINT_ONLINE == providerType) {
            try {
                URIBuilder builder = new URIBuilder(urlBuilder.append(RepoConstants.SHAREPOINT_ONLINE_AUTH_START_URL).toString());
                builder.addParameter("name", repository.getName());
                builder.addParameter("isShared", String.valueOf(repository.getShared()));
                builder.addParameter("siteName", repository.getAccountName());
                builder.addParameter("repoType", ServiceProviderType.SHAREPOINT_ONLINE.name());
                builder.addParameter("redirectCode", "1");
                builder.addParameter("repoId", String.valueOf(repository.getId()));
                return builder.toString();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        return null;
    }

    private static boolean isNameValid(String name) {
        Matcher matcher = RegularExpressions.REPO_NAME_PATTERN.matcher(name);
        return matcher.matches() && name.length() <= 40;
    }

    public static RepoItemMetadata getRepoItem(DbSession session, String pathId, long lastModifiedTime, String repoId) {
        Criteria criteria = session.createCriteria(RepoItemMetadata.class);
        criteria.add(Restrictions.and(Restrictions.eq("repository.id", repoId), Restrictions.eq("filePath", pathId), Restrictions.gt("lastModified", new Date(lastModifiedTime))));
        return (RepoItemMetadata)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRepoIdsWithTenantAndServiceProviderId(DbSession session, String providerId) {
        Criteria criteria = session.createCriteria(Repository.class);
        criteria.add(Restrictions.eq("providerId", providerId));
        criteria.setProjection(Projections.property("id"));
        return (List<String>)criteria.list();
    }
}
