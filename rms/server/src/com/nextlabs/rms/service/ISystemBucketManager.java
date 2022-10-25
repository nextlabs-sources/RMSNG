package com.nextlabs.rms.service;

import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;

public interface ISystemBucketManager {

    String constructSystemBucketName(String tenantName);

    boolean isSystemBucket(String systemBucketName, String tenantName);

    boolean isSystemBucket(String systemBucketName);

    Tenant getParentTenant(String systemBucketName, DbSession session);
}
