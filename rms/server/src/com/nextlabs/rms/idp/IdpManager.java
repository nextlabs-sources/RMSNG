package com.nextlabs.rms.idp;

import com.google.gson.Gson;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.IdentityProvider;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.services.manager.LockManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public final class IdpManager {

    public static final String RESOURCE = "RESOURCE_IDENTITY_PROVIDER";

    private IdpManager() {
    }

    @SuppressWarnings("unchecked")
    public static void bootstrap() {
        boolean lock = LockManager.getInstance().acquireLock(RESOURCE, TimeUnit.MINUTES.toMillis(5));
        if (lock) {
            try (DbSession session = DbSession.newSession()) {
                Tenant publicTenant = AbstractLogin.getDefaultTenant();
                Criteria criteria = session.createCriteria(IdentityProvider.class);
                criteria.add(Restrictions.eq("tenant.id", publicTenant.getId()));
                List<IdentityProvider> defaultIDPs = criteria.list();
                if (defaultIDPs != null && !defaultIDPs.isEmpty()) {
                    return;
                }
                criteria = session.createCriteria(IdentityProvider.class);
                criteria.add(Restrictions.eq("tenant.id", publicTenant.getId()));
                criteria.add(Restrictions.eq("type", Constants.LoginType.SAML));
                criteria.addOrder(Order.asc("id"));
                List<IdentityProvider> samlIdpsInDB = (List<IdentityProvider>)criteria.list();

                criteria = session.createCriteria(IdentityProvider.class);
                criteria.add(Restrictions.eq("tenant.id", publicTenant.getId()));
                criteria.add(Restrictions.eq("type", Constants.LoginType.LDAP));
                criteria.addOrder(Order.asc("id"));
                List<IdentityProvider> ldapIdpsInDB = (List<IdentityProvider>)criteria.list();

                criteria = session.createCriteria(IdentityProvider.class);
                criteria.add(Restrictions.eq("tenant.id", publicTenant.getId()));
                criteria.add(Restrictions.eq("type", Constants.LoginType.AZUREAD));
                criteria.addOrder(Order.asc("id"));
                List<IdentityProvider> azureIdpsInDB = (List<IdentityProvider>)criteria.list();

                session.beginTransaction();
                String queryString = "delete from IdentityProvider where tenant.id = :tenantId";
                Query query = session.createQuery(queryString);
                query.setParameter("tenantId", publicTenant.getId());
                query.executeUpdate();
                RMSCacheManager.getInstance().getIdpCache().remove(publicTenant.getId());
                WebConfig webConfig = WebConfig.getInstance();
                String rmsIdpDetails = webConfig.getProperty(WebConfig.IDP_RMS_ATTRIBUTES);
                String googleIdpDetails = webConfig.getProperty(WebConfig.IDP_GOOGLE_ATTRIBUTES);
                String fbIdpDetails = webConfig.getProperty(WebConfig.IDP_FB_ATTRIBUTES);
                String samlNum = webConfig.getProperty(WebConfig.IDP_SAML_COUNT);
                String ldapNum = webConfig.getProperty(WebConfig.IDP_LDAP_COUNT);
                if (StringUtils.hasText(rmsIdpDetails)) {
                    IdentityProvider rmsIdp = new IdentityProvider();
                    rmsIdp.setType(Constants.LoginType.DB);
                    rmsIdp.setAttributes(rmsIdpDetails);
                    rmsIdp.setTenant(publicTenant);
                    session.saveOrUpdate(rmsIdp);
                }
                if (StringUtils.hasText(googleIdpDetails)) {
                    IdentityProvider googleIdp = new IdentityProvider();
                    googleIdp.setType(Constants.LoginType.GOOGLE);
                    googleIdp.setAttributes(googleIdpDetails);
                    googleIdp.setTenant(publicTenant);
                    session.saveOrUpdate(googleIdp);
                }
                if (StringUtils.hasText(fbIdpDetails)) {
                    IdentityProvider fbIdp = new IdentityProvider();
                    fbIdp.setType(Constants.LoginType.FACEBOOK);
                    fbIdp.setAttributes(fbIdpDetails);
                    fbIdp.setTenant(publicTenant);
                    session.saveOrUpdate(fbIdp);
                }
                if (StringUtils.hasText(samlNum)) {
                    int nSaml = Integer.parseInt(samlNum);
                    for (int i = 0; i < nSaml; i++) {
                        String samlIdpDetails = webConfig.getProperty(WebConfig.IDP_SAML_ATTRIBUTES + (i + 1) + ".attributes");
                        if (StringUtils.hasText(samlIdpDetails)) {
                            IdentityProvider samlIdp = new IdentityProvider();
                            samlIdp.setType(Constants.LoginType.SAML);
                            samlIdp.setAttributes(samlIdpDetails);
                            samlIdp.setTenant(publicTenant);
                            if (!samlIdpsInDB.isEmpty() && samlIdpsInDB.get(i) != null) {
                                samlIdp.setUserAttributeMap(samlIdpsInDB.get(i).getUserAttributeMap());
                            }
                            session.saveOrUpdate(samlIdp);
                        }
                    }
                }
                if (StringUtils.hasText(ldapNum)) {
                    int nLdap = Integer.parseInt(ldapNum);
                    for (int i = 0; i < nLdap; i++) {
                        String ldapIdpDetails = webConfig.getProperty(WebConfig.IDP_LDAP_ATTRIBUTES + (i + 1) + ".attributes");
                        if (StringUtils.hasText(ldapIdpDetails)) {
                            IdentityProvider ldapIdp = new IdentityProvider();
                            ldapIdp.setType(Constants.LoginType.LDAP);
                            ldapIdp.setAttributes(ldapIdpDetails);
                            ldapIdp.setTenant(publicTenant);
                            if (!ldapIdpsInDB.isEmpty() && ldapIdpsInDB.get(i) != null) {
                                ldapIdp.setUserAttributeMap(ldapIdpsInDB.get(i).getUserAttributeMap());
                            }
                            session.saveOrUpdate(ldapIdp);
                        }
                    }
                }

                // Azure AD
                String azureNum = webConfig.getProperty(WebConfig.IDP_AZUREAD_COUNT);
                if (StringUtils.hasText(azureNum)) {
                    int nAzure = Integer.parseInt(azureNum);
                    for (int i = 0; i < nAzure; i++) {
                        String azureIdpDetails = webConfig.getProperty(WebConfig.IDP_AZUREAD_ATTRIBUTES + (i + 1) + ".attributes");
                        if (StringUtils.hasText(azureIdpDetails)) {
                            IdentityProvider azureIdp = new IdentityProvider();
                            azureIdp.setType(Constants.LoginType.AZUREAD);
                            azureIdp.setAttributes(azureIdpDetails);
                            azureIdp.setTenant(publicTenant);
                            if (!azureIdpsInDB.isEmpty() && azureIdpsInDB.get(i) != null) {
                                azureIdp.setUserAttributeMap(azureIdpsInDB.get(i).getUserAttributeMap());
                            }
                            session.saveOrUpdate(azureIdp);
                        }
                    }
                }

                session.commit();
            } finally {
                LockManager.getInstance().releaseLock(RESOURCE);
            }
        }
    }

    public static Object getIdpAttributes(String idpId) {
        try (DbSession session = DbSession.newSession()) {
            IdentityProvider idp = session.get(IdentityProvider.class, Integer.parseInt(idpId));
            String attributesJson = idp.getAttributes();
            Gson gson = new Gson();
            switch (idp.getType()) {
                case FACEBOOK:
                    return gson.fromJson(attributesJson, FacebookIdpAttributes.class);
                case GOOGLE:
                    return gson.fromJson(attributesJson, GoogleIdpAttributes.class);
                case SAML:
                    return gson.fromJson(attributesJson, SamlIdpAttributes.class);
                case LDAP:
                    return gson.fromJson(attributesJson, LdapIdpAttributes.class);
                case AZUREAD:
                    return gson.fromJson(attributesJson, AzureAdIdpAttributes.class);
                default:
                    return null;
            }
        }
    }

    public static AzureAdIdpAttributes getAzureAttributes(String tenantId) {
        Object obj = getIdpAttributes(Constants.LoginType.AZUREAD, tenantId);

        if (obj instanceof AzureAdIdpAttributes) {
            return (AzureAdIdpAttributes)obj;
        }

        return null;
    }

    public static GoogleIdpAttributes getGoogleAttributes(String tenantId) {
        Object obj = getIdpAttributes(Constants.LoginType.GOOGLE, tenantId);

        if (obj instanceof GoogleIdpAttributes) {
            return (GoogleIdpAttributes)obj;
        }

        return null;
    }

    public static FacebookIdpAttributes getFacebookAttributes(String tenantId) {
        Object obj = getIdpAttributes(Constants.LoginType.FACEBOOK, tenantId);

        if (obj instanceof FacebookIdpAttributes) {
            return (FacebookIdpAttributes)obj;
        }

        return null;
    }

    public static LocalIdpAttributes getLocalAttributes(String tenantId) {
        Object obj = getIdpAttributes(Constants.LoginType.DB, tenantId);

        if (obj instanceof LocalIdpAttributes) {
            return (LocalIdpAttributes)obj;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object getIdpAttributes(Constants.LoginType type, String tenantId) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(IdentityProvider.class);
            criteria.add(Restrictions.eq("type", type));
            if (!StringUtils.hasText(tenantId)) {
                tenantId = AbstractLogin.getDefaultTenant().getId();
            }
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            if (type == Constants.LoginType.FACEBOOK) {
                IdentityProvider fbIdp = (IdentityProvider)criteria.uniqueResult();
                String attributesJson = fbIdp.getAttributes();
                Gson gson = new Gson();
                return gson.fromJson(attributesJson, FacebookIdpAttributes.class);
            } else if (type == Constants.LoginType.GOOGLE) {
                IdentityProvider googleIdp = (IdentityProvider)criteria.uniqueResult();
                String attributesJson = googleIdp.getAttributes();
                Gson gson = new Gson();
                return gson.fromJson(attributesJson, GoogleIdpAttributes.class);
            } else if (type == Constants.LoginType.DB) {
                IdentityProvider localIdp = (IdentityProvider)criteria.uniqueResult();
                String attributesJson = localIdp.getAttributes();
                Gson gson = new Gson();
                return gson.fromJson(attributesJson, LocalIdpAttributes.class);
            } else if (type == Constants.LoginType.LDAP) {
                List<IdentityProvider> ldapIdps = criteria.list();
                List<LdapIdpAttributes> ldapAttributes = new ArrayList<LdapIdpAttributes>();
                Gson gson = new Gson();
                for (int i = 0; i < ldapIdps.size(); i++) {
                    String attributesJson = ldapIdps.get(i).getAttributes();
                    ldapAttributes.add(gson.fromJson(attributesJson, LdapIdpAttributes.class));
                }
                return ldapAttributes;
            } else if (type == Constants.LoginType.AZUREAD) {
                List<IdentityProvider> azureIdps = criteria.list();
                List<AzureAdIdpAttributes> azureAttributes = new ArrayList<AzureAdIdpAttributes>();
                Gson gson = new Gson();
                for (int i = 0; i < azureIdps.size(); i++) {
                    String attributesJson = azureIdps.get(i).getAttributes();
                    azureAttributes.add(gson.fromJson(attributesJson, AzureAdIdpAttributes.class));
                }

                // TODO: Should only support one Azure IDP. This is temporary fix.
                return azureAttributes.get(0);

            } else if (type == Constants.LoginType.SAML) {
                List<IdentityProvider> samlIdps = criteria.list();
                List<SamlIdpAttributes> samlAttributes = new ArrayList<SamlIdpAttributes>();
                Gson gson = new Gson();
                for (int i = 0; i < samlIdps.size(); i++) {
                    String attributesJson = samlIdps.get(i).getAttributes();
                    samlAttributes.add(gson.fromJson(attributesJson, SamlIdpAttributes.class));
                }
                return samlAttributes;
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getUserAttributeMap(String idpId) {
        try (DbSession session = DbSession.newSession()) {
            IdentityProvider idp = session.get(IdentityProvider.class, Integer.parseInt(idpId));
            String attributeMapJson = idp.getUserAttributeMap();
            Gson gson = new Gson();
            return gson.fromJson(attributeMapJson, Map.class);
        }
    }

    /***
     *
     * @param evalUserIdAttribute
     * @return
     */
    public static Map<String, List<String>> getEAPAttributes(List<String> evalUserIdAttribute) {
        Map<String, List<String>> eapAttributes = new HashMap<>();
        List<Object> userAttributes = null;
        if (evalUserIdAttribute != null && !evalUserIdAttribute.isEmpty()) {
            String userId = evalUserIdAttribute.get(0);
            if (userId != null && !userId.isEmpty()) {
                userAttributes = RMSCacheManager.getInstance().getEapCache().get(userId.toLowerCase());
            }
        }
        if (userAttributes != null && !userAttributes.isEmpty()) {
            Set<String> attributes = RMSCacheManager.getInstance().getEapAttrCache().keySet();
            for (String attribute : attributes) {
                HashMap<String, Integer> attributeProperties = RMSCacheManager.getInstance().getEapAttrCache().get(attribute);
                Object attributeValue = userAttributes.get(attributeProperties.get(UserAttributeCacheItem.ATTRIBUTE_INDEX));

                Integer attributeType = attributeProperties.get(UserAttributeCacheItem.ATTRIBUTE_TYPE);
                if (attributeType == UserAttributeCacheItem.STRING_VALUE) {
                    eapAttributes.put(attribute, Collections.singletonList((String)attributeValue));
                } else if (attributeType == UserAttributeCacheItem.NUMBER_VALUE) {
                    eapAttributes.put(attribute, Collections.singletonList(String.valueOf(attributeValue)));
                } else {
                    eapAttributes.put(attribute, (List<String>)attributeValue);
                }

            }
        }
        return eapAttributes;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getUserAttributeMap(String tenantId, Constants.LoginType idpType) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(IdentityProvider.class);
            criteria.add(Restrictions.eq("tenant.id", tenantId));
            criteria.add(Restrictions.eq("type", idpType));
            IdentityProvider idp = (IdentityProvider)criteria.uniqueResult();

            String attributeMapJson = idp.getUserAttributeMap();
            if (attributeMapJson == null || attributeMapJson.isEmpty()) {
                return new HashMap<String, String>();
            }
            Gson gson = new Gson();
            return gson.fromJson(attributeMapJson, Map.class);
        }
    }

}
