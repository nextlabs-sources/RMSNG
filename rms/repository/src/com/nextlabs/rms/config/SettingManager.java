package com.nextlabs.rms.config;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.StorageProvider;
import com.nextlabs.rms.pojo.ServiceProviderSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class SettingManager {

    private SettingManager() {
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

    public static ServiceProviderSetting getStorageProviderSettings(DbSession session, String tenantId,
        ServiceProviderType type) {
        Criteria criteria = session.createCriteria(StorageProvider.class);
        criteria.add(Restrictions.eq("tenantId", tenantId));
        criteria.add(Restrictions.eq("type", type.ordinal()));
        List<?> list = criteria.list();
        if (list.isEmpty()) {
            return null;
        }
        return toServiceProviderSetting((StorageProvider)list.get(0));
    }

    public static StorageProvider getStorageProvider(DbSession session, String tenantId,
        ServiceProviderType type) {
        Criteria criteria = session.createCriteria(StorageProvider.class);
        criteria.add(Restrictions.eq("tenantId", tenantId));
        criteria.add(Restrictions.eq("type", type.ordinal()));
        List<?> list = criteria.list();
        if (list.isEmpty()) {
            return null;
        }
        return (StorageProvider)list.get(0);
    }

    /**
     * Call this method only if ServiceProviderAttributesDO are already loaded in to ServiceProviderDO
     * else
     * 	if associated EntityManager is still open, it will result in N queries to load N attributes
     * 	if associated EntityManager is closed, it will throw exception
     * @param spdo
     * @return
     */
    public static ServiceProviderSetting toServiceProviderSetting(StorageProvider spdo) {
        ServiceProviderSetting sps = new ServiceProviderSetting();
        sps.setId(spdo.getId());
        sps.setProviderType(ServiceProviderType.values()[spdo.getType()]);
        sps.setTenantId(spdo.getTenantId());
        sps.setProviderTypeDisplayName(ServiceProviderSetting.getProviderTypeDisplayName(sps.getProviderType().name()));

        Map<String, Object> map = GsonUtils.GSON.fromJson(spdo.getAttributes(), GsonUtils.GENERIC_MAP_TYPE);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sps.setAttribute(entry.getKey(), entry.getValue().toString());
        }
        return sps;
    }
}
