package com.nextlabs.rms.service;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.LoginAccount;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryEnterprise;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class MyVaultService {

    private MyVaultService() {
    }

    public static boolean checkRights(String duid, int userId, Rights right) {
        try (DbSession session = DbSession.newSession()) {
            return checkRights(session, duid, userId, right);
        }
    }

    public static void validateFileDuid(String duid, boolean isNxl) throws FileNotFoundException {
        if (!StringUtils.hasText(duid) && isNxl) {
            throw new FileNotFoundException("Missing File.");
        }
    }

    public static boolean checkRights(DbSession session, String duid, int userId, Rights right) {
        AllNxl allNxl = session.get(AllNxl.class, duid);

        if (allNxl != null) {

            if (allNxl.getUser().getId() == userId) {
                return true;
            }

            String policy = allNxl.getPolicy();
            Rights[] rights = StringUtils.hasText(policy) ? AdhocEvalAdapter.evaluate(GsonUtils.GSON.fromJson(policy, FilePolicy.class), false).getRights() : Rights.fromInt(allNxl.getPermissions());
            if (!ArrayUtils.contains(rights, right)) {
                return false;
            }
            if (StringUtils.hasText(policy)) {
                boolean isExpiryValid = AdhocEvalAdapter.isFileExpired(GsonUtils.GSON.fromJson(policy, FilePolicy.class));
                boolean isNotYetValid = AdhocEvalAdapter.isNotYetValid(GsonUtils.GSON.fromJson(policy, FilePolicy.class));
                if (isExpiryValid || isNotYetValid) {
                    return false;
                }
            }

            Criteria criteria = session.createCriteria(LoginAccount.class);
            criteria.add(Restrictions.eq("userId", userId));
            List<LoginAccount> accounts = criteria.list();
            List<String> emails = new ArrayList<>();
            for (LoginAccount account : accounts) {
                String email = account.getEmail();
                emails.add(email);
            }
            return SharedFileManager.isRecipient(duid, emails, session);
        }
        return false;
    }

    public static DefaultRepositoryTemplate getRepository(RMSUserPrincipal principal, String tenantId)
            throws InvalidDefaultRepositoryException {
        DefaultRepositoryTemplate repository;
        try (DbSession session = DbSession.newSession()) {
            repository = new DefaultRepositoryEnterprise(session, principal, tenantId);
        }
        return repository;
    }

    public static boolean checkFileExists(String pathId, UserSession us)
            throws RepositoryException, InvalidDefaultRepositoryException {
        try (DbSession session = DbSession.newSession()) {
            String loginTenantId = us.getLoginTenant();
            Tenant loginTenant = session.get(Tenant.class, loginTenantId);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
            DefaultRepositoryTemplate repository = DefaultRepositoryManager.getInstance().getDefaultPersonalRepository(session, principal);
            String id = repository.getExistingSpaceItemIdWithFilePath(pathId, repository.getRepoId());
            return id != null;
        }
    }

}
