package com.nextlabs.rms.service;

import com.googlecode.flyway.core.util.StringUtils;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.Entry;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.security.SecretKeyEntry;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.security.KeyStoreManagerImpl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

public class TokenGroupManager {

    public static final String TENANT_TOKEN_GROUP_QUERY = "SELECT new map(k.tokenGroupName as tokenGroupName, k.id as keystoreId) FROM Tenant t, KeyStoreEntry k WHERE t.name = k.tokenGroupName";
    public static final String PROJECT_TOKEN_GROUP_QUERY = "SELECT new map(m.name as tokenGroupName, p.keystore.id as keystoreId) FROM Membership m, Project p where m.project.id = p.id";

    private String tenantName;
    private String projectName;
    private int projectOwnerId;
    private TokenGroupType groupType;
    private String tokenGroupName;
    private String parentTenantName;

    private TokenGroupManager(String tenantName, TokenGroupType groupType, String tokenGroupName,
        String parentTenantName) {
        this.tenantName = tenantName;
        this.groupType = groupType;
        this.tokenGroupName = tokenGroupName;
        this.parentTenantName = parentTenantName;
    }

    private TokenGroupManager(String tenantName, String projectName, int projectOwnerId, String tokenGroupName,
        String parentTenantName) {
        this.tenantName = tenantName;
        this.groupType = TokenGroupType.TOKENGROUP_PROJECT;
        this.projectName = projectName;
        this.projectOwnerId = projectOwnerId;
        this.tokenGroupName = tokenGroupName;
        this.parentTenantName = parentTenantName;
    }

    public TokenGroupManager(String tenantName, TokenGroupType groupType) throws TokenGroupException {
        this.tenantName = tenantName;
        this.groupType = groupType;
        constructTokenGroupName();
    }

    public TokenGroupManager(String tenantName, String projectName, int projectOwnerId) throws TokenGroupException {
        this.tenantName = tenantName;
        this.groupType = TokenGroupType.TOKENGROUP_PROJECT;
        this.projectName = projectName;
        this.projectOwnerId = projectOwnerId;
        constructTokenGroupName();
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getParentTenantName() {
        return parentTenantName;
    }

    public TokenGroupType getGroupType() {
        return groupType;
    }

    public static TokenGroupManager newInstance(DbSession session, String tokenGroupName, String loginTenantId)
            throws TokenGroupException {

        KeyStoreEntry keyStoreEntry = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
        if (keyStoreEntry == null) {
            throw new TokenGroupException("Invalid or unknown token group name");
        } else {
            Tenant loginTenant = session.get(Tenant.class, loginTenantId);
            ISystemBucketManager sbm = new SystemBucketManagerImpl();
            if (sbm.isSystemBucket(tokenGroupName, loginTenant.getName())) {
                String tenantName = com.nextlabs.common.util.StringUtils.substringBefore(tokenGroupName, com.nextlabs.common.shared.Constants.SYSTEM_BUCKET_NAME_SUFFIX);
                return new TokenGroupManager(tenantName, TokenGroupType.TOKENGROUP_SYSTEMBUCKET, tokenGroupName, tenantName);
            }
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant != null) {
                String parentTenantName = tenant.getParentId() != null ? session.get(Tenant.class, tenant.getParentId()).getName() : null;
                return new TokenGroupManager(tokenGroupName, TokenGroupType.TOKENGROUP_TENANT, tokenGroupName, parentTenantName);
            }

            criteria = session.createCriteria(Project.class);
            criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
            criteria.add(Restrictions.eq("status", Project.Status.ACTIVE));
            Project project = (Project)criteria.uniqueResult();
            if (project != null) {
                int lastIdxOfUnderscore = tokenGroupName.lastIndexOf('_');
                if (lastIdxOfUnderscore != -1) {
                    int projectOwnerId;
                    try {
                        projectOwnerId = Integer.parseInt(tokenGroupName.substring(lastIdxOfUnderscore + 1, tokenGroupName.length()));
                        if (projectOwnerId < 0) {
                            throw new TokenGroupException("Project Owner ID must be a positive integer");
                        }
                    } catch (NumberFormatException e) {
                        throw new TokenGroupException("Project Owner ID must be an integer", e);
                    }
                    String parentTenantName = project.getParentTenant() != null ? session.get(Tenant.class, project.getParentTenant().getId()).getName() : null;
                    return new TokenGroupManager(project.getParentTenant().getName(), project.getName(), projectOwnerId, tokenGroupName, parentTenantName);
                }
            }
        }
        throw new TokenGroupException("Invalid token group name");
    }

    public Membership getStaticMembership(DbSession session, int userId) {

        Membership membership = null;
        KeyStoreEntry keyStoreEntry = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
        if (keyStoreEntry != null) {
            Criteria criteria = null;
            switch (groupType) {
                case TOKENGROUP_PROJECT:
                    criteria = session.createCriteria(Project.class);
                    criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
                    criteria.add(Restrictions.eq("status", Project.Status.ACTIVE));
                    Project project = (Project)criteria.uniqueResult();
                    if (project != null) {
                        criteria = session.createCriteria(Membership.class);
                        criteria.add(Restrictions.eq("user.id", userId));
                        criteria.add(Restrictions.eq("status", Membership.Status.ACTIVE));
                        criteria.add(Restrictions.eq("project.id", project.getId()));
                        membership = (Membership)criteria.uniqueResult();
                    }
                    break;
                case TOKENGROUP_TENANT:
                    criteria = session.createCriteria(Tenant.class);
                    criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
                    Tenant tenant = (Tenant)criteria.uniqueResult();
                    if (tenant != null) {
                        criteria = session.createCriteria(Membership.class);
                        criteria.add(Restrictions.eq("user.id", userId));
                        criteria.add(Restrictions.eq("status", Membership.Status.ACTIVE));
                        criteria.add(Restrictions.eq("tenant.id", tenant.getId()));
                        membership = (Membership)criteria.uniqueResult();
                    }
                    break;
                case TOKENGROUP_SYSTEMBUCKET:
                default:
                    return null;
            }
        }
        return membership;
    }

    private void constructTokenGroupName() throws TokenGroupException {
        StringBuilder sb = new StringBuilder(tenantName);
        switch (groupType) {
            case TOKENGROUP_SYSTEMBUCKET:
                SystemBucketManagerImpl sbmImpl = new SystemBucketManagerImpl();
                sb.append(sbmImpl.getSystemBucketNameSuffix());
                break;
            case TOKENGROUP_PROJECT:
                if (!StringUtils.hasText(projectName) || projectOwnerId == 0) {
                    throw new TokenGroupException("Invalid projectName or projectOwner");
                }
                sb.append('_').append(projectName).append('_').append(projectOwnerId);
                break;
            case TOKENGROUP_TENANT:
                break;
            default:
                throw new TokenGroupException("Invalid token group type");
        }
        tokenGroupName = sb.toString();
    }

    private KeyStoreEntry createKeyStoreBase(String... otp)
            throws GeneralSecurityException, IOException, TokenGroupException {
        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
        String hskAlias = IKeyStoreManager.PREFIX_HSK + tokenGroupName;
        String icaAlias = IKeyStoreManager.PREFIX_ICA + tokenGroupName;
        String dhAlias = IKeyStoreManager.PREFIX_DH + tokenGroupName;
        SecretKey hsk = km.getSecretKey(tokenGroupName, hskAlias);
        if (hsk != null) {
            throw new TokenGroupException("Key store for token group " + tokenGroupName + " has been created already");
        }

        // Generate RSA keypair, DH keypair
        KeyPair icaKeypair = KeyManager.generateRSAKeyPair();
        KeyPair dhKeypair = KeyManager.generateDHKeyPair();

        WebConfig webConfig = WebConfig.getInstance();
        String routerUrl = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL);
        if (!StringUtils.hasText(routerUrl)) {
            routerUrl = webConfig.getProperty(WebConfig.ROUTER_URL, "https://r.skydrm.com");
        }

        JsonRequest proxyReq = new JsonRequest();
        proxyReq.addParameter("name", tokenGroupName);
        proxyReq.addParameter("icaPublicKey", Base64Codec.encodeAsString(icaKeypair.getPublic().getEncoded()));
        proxyReq.addParameter("dhPublicKey", Base64Codec.encodeAsString(dhKeypair.getPublic().getEncoded()));
        String ret;
        if (otp != null && otp.length > 0) {
            String otpStr = otp[0];
            String restUrl = routerUrl + "/rs/tenant";
            proxyReq.addParameter("otp", otpStr);
            ret = RestClient.post(restUrl, proxyReq.toJson(), RestClient.getConnectionTimeout(), (int)TimeUnit.MINUTES.toMillis(1));
        } else {
            String signature = KeyManager.signData((PrivateKey)km.getKey(tenantName, IKeyStoreManager.PREFIX_ICA + tenantName), tokenGroupName);
            Certificate cert = km.getCertificate(tenantName, IKeyStoreManager.PREFIX_ICA + tenantName);

            String restUrl = routerUrl + "/rs/tokenGroup";
            proxyReq.addParameter("certificate", Base64Codec.encodeAsString(cert.getEncoded()));
            proxyReq.addParameter("signature", signature);
            ret = RestClient.put(restUrl, proxyReq.toJson(), RestClient.getConnectionTimeout(), (int)TimeUnit.MINUTES.toMillis(1));
        }
        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new TokenGroupException("Failed to register token group " + tokenGroupName + (otp == null ? " in" : " to") + " central server : " + resp.toJson());
        }

        String icaCerts = resp.getResultAsString("icaCertificates");
        String dhCerts = resp.getResultAsString("dhCertificates");
        String hskData = resp.getResultAsString("hsk");
        Certificate[] icaChain = KeyUtils.readCertificateChain(icaCerts);
        Certificate[] dhChain = KeyUtils.readCertificateChain(dhCerts);
        hsk = new SecretKeySpec(Hex.toByteArray(hskData), "AES");
        List<Entry> icaEntries = km.createKeyEntry(tokenGroupName, icaAlias, icaKeypair.getPrivate(), icaChain);
        List<Entry> dhEntries = km.createKeyEntry(tokenGroupName, dhAlias, dhKeypair.getPrivate(), dhChain);
        SecretKeyEntry hskEntry = km.createSecretKey(tokenGroupName, hskAlias, hsk);
        List<Entry> entries = new ArrayList<>(icaEntries.size() + dhEntries.size() + 1);
        entries.addAll(icaEntries);
        entries.addAll(dhEntries);
        entries.add(hskEntry);
        km.createKeyStore(tokenGroupName, entries);
        return new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
    }

    public static List<Map<String, String>> getTokenGroupResourceTypeMappings() {
        try (DbSession session = DbSession.newSession()) {
            Query tenantQuery = session.createQuery(TENANT_TOKEN_GROUP_QUERY);
            List<Map<String, String>> tokenGroups = tenantQuery.list();
            Query projectQuery = session.createQuery(PROJECT_TOKEN_GROUP_QUERY);
            tokenGroups.addAll(projectQuery.list());
            tokenGroups.forEach(mapEntry -> {
                mapEntry.put("keystoreId", ControlCenterManager.escapeIllegalChars(mapEntry.get("keystoreId")));
                if (mapEntry.get("tokenGroupName").contains("@")) {
                    mapEntry.put("tokenGroupName", com.nextlabs.common.util.StringUtils.substringAfter(mapEntry.get("tokenGroupName"), "@"));
                }
            });
            return tokenGroups;
        }
    }

    public KeyStoreEntry createKeyStore(String otp) throws GeneralSecurityException, IOException, TokenGroupException {
        return createKeyStoreBase(new String[] { otp });
    }

    public KeyStoreEntry createKeyStore() throws GeneralSecurityException, IOException, TokenGroupException {
        return createKeyStoreBase();
    }
}
