package com.nextlabs.rms.repository;

import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.ExternalRepositoryNxl;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.User;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class SharedWorkspaceRepoManager {

    private SharedWorkspaceRepoManager() {
    }

    public static void updateExternalNxlDBProtect(int ownerId, String repositoryId, String duid, String owner,
        int rights, String fileName, String filePath, long size) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            ExternalRepositoryNxl nxl = new ExternalRepositoryNxl();
            nxl.setDuid(duid);
            nxl.setRepository(session.load(Repository.class, repositoryId));
            nxl.setOwner(owner);
            nxl.setUser(session.load(User.class, ownerId));
            nxl.setPermissions(rights);
            nxl.setFileName(fileName);
            nxl.setFilePath(filePath);
            nxl.setDisplayName(fileName);
            Date now = new Date();
            nxl.setCreationTime(now);
            nxl.setLastModified(now);
            nxl.setSize(size);
            nxl.setStatus(ExternalRepositoryNxl.Status.ACTIVE);
            nxl.setShared(false);
            session.save(nxl);
            session.commit();
        }
    }

    public static ExternalRepositoryNxl getExternalItem(String repoId, String duid,
        DbSession session) {
        Criteria criteria = session.createCriteria(ExternalRepositoryNxl.class);
        criteria.add(Restrictions.eq("repository.id", repoId));
        criteria.add(Restrictions.eq("duid", duid));
        return (ExternalRepositoryNxl)criteria.uniqueResult();
    }

}
