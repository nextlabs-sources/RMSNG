package com.nextlabs.rms.service;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.security.KeyStoreManagerImpl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class SystemBucketManagerImpl implements ISystemBucketManager {

    private KeyStoreManagerImpl ksm;

    public SystemBucketManagerImpl() {
        this.ksm = new KeyStoreManagerImpl();
    }

    @Override
    public String constructSystemBucketName(String tenantName) {
        return tenantName.concat(com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX);
    }

    @Override
    public boolean isSystemBucket(String systemBucketName, String tenantName) {
        return ksm.getKeyStore(systemBucketName) != null && constructSystemBucketName(tenantName).equals(systemBucketName);
    }

    @Override
    public boolean isSystemBucket(String systemBucketName) {
        return ksm.getKeyStore(systemBucketName) != null && systemBucketName.endsWith(com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX);
    }

    @Override
    public Tenant getParentTenant(String systemBucketName, DbSession session) {
        String tenantName = StringUtils.substringBefore(systemBucketName, com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX);
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("name", tenantName));
        return (Tenant)criteria.uniqueResult();
    }

    String getSystemBucketNameSuffix() {
        return com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX;
    }
}
