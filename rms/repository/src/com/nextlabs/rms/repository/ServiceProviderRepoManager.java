package com.nextlabs.rms.repository;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.pojo.ServiceProviderSetting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class ServiceProviderRepoManager {

    private ServiceProviderRepoManager() {
    }

    public static void removeServiceProvider(DbSession session, String serviceProviderId) {
        session.beginTransaction();
        StorageProvider storageProvider = session.get(StorageProvider.class, serviceProviderId);
        if (storageProvider == null) {
            throw new ValidateException(4004, "Invalid service provider id.");
        }
        session.delete(storageProvider);
        session.commit();

    }

    public static String addServiceProvider(DbSession session, StorageProvider sp) {
        if (sp == null) {
            return null;
        }
        session.beginTransaction();
        session.save(sp);
        session.commit();
        return sp.getId();
    }

    public static String updateServiceProvider(DbSession session, StorageProvider sp) {
        if (sp == null) {
            return null;
        }
        session.beginTransaction();
        session.saveOrUpdate(sp);
        session.commit();
        return sp.getId();
    }

    @SuppressWarnings("unchecked")
    public static List<ServiceProviderSetting> getStorageProvider(DbSession session, String tenantId, int ordinal) {
        Criteria criteria = session.createCriteria(StorageProvider.class);
        criteria.add(Restrictions.eq("tenantId", tenantId));
        criteria.add(Restrictions.eq("type", ordinal));
        List<StorageProvider> storageProviders = (List<StorageProvider>)criteria.list();
        return storageProviders.parallelStream().map(ServiceProviderRepoManager::toServiceProviderSetting).collect(Collectors.toList());
    }

    public static ServiceProviderSetting getStorageProvider(DbSession session, String id) {
        return toServiceProviderSetting(session.get(StorageProvider.class, id));
    }

    public static List<ServiceProviderSetting> getStorageProviderSettings(DbSession session, String tenantId) {
        Criteria criteria = session.createCriteria(StorageProvider.class);
        criteria.add(Restrictions.eq("tenantId", tenantId));
        List<?> list = criteria.list();

        List<ServiceProviderSetting> settings = new ArrayList<ServiceProviderSetting>(list.size());
        for (Object obj : list) {
            StorageProvider spdo = (StorageProvider)obj;
            settings.add(toServiceProviderSetting(spdo));
        }
        return settings;
    }

    private static ServiceProviderSetting toServiceProviderSetting(StorageProvider spdo) {
        ServiceProviderSetting sps = null;
        if (spdo != null) {
            sps = new ServiceProviderSetting();
            sps.setId(spdo.getId());
            sps.setProviderType(ServiceProviderType.values()[spdo.getType()]);
            sps.setTenantId(spdo.getTenantId());
            sps.setProviderTypeDisplayName(ServiceProviderSetting.getProviderTypeDisplayName(sps.getProviderType().name()));

            Map<String, Object> map = GsonUtils.GSON.fromJson(spdo.getAttributes(), GsonUtils.GENERIC_MAP_TYPE);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sps.setAttribute(entry.getKey(), entry.getValue().toString());
            }
        }
        return sps;
    }

    public static StorageProvider toStorageProvider(ServiceProviderSetting serviceProviderSetting,
        StorageProvider storageProvider) {
        StorageProvider sp;
        if (storageProvider == null) {
            sp = new StorageProvider();
        } else {
            sp = storageProvider;
        }
        if (serviceProviderSetting != null) {
            sp.setId(serviceProviderSetting.getId());
            sp.setTenantId(serviceProviderSetting.getTenantId());
            sp.setType(serviceProviderSetting.getProviderType().ordinal());
            sp.setName(ServiceProviderSetting.getProviderTypeDisplayName(serviceProviderSetting.getProviderType().toString()));
            sp.setAttributes(GsonUtils.GSON.toJson(serviceProviderSetting.getAttributes()));
            sp.setCreationTime(new Date());
        }
        return sp;
    }

}
