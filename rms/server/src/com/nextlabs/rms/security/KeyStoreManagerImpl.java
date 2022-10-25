package com.nextlabs.rms.security;

import com.nextlabs.common.security.CertificateEntry;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyEntry;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry.KeyStoreType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class KeyStoreManagerImpl implements IKeyStoreManager {

    public boolean containsAlias(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        KeyStoreEntry entry = getKeyStore(tokenGroupName);
        KeyStore keyStore = toKeyStore(entry);
        return keyStore != null && keyStore.containsAlias(alias);
    }

    @Override
    public void createKeyStore(String tokenGroupName, List<com.nextlabs.common.security.Entry> entries)
            throws GeneralSecurityException, IOException {
        Map<String, List<com.nextlabs.common.security.Entry>> map = new HashMap<>(2);
        for (com.nextlabs.common.security.Entry entry : entries) {
            String tenant = entry.getTokenGroupName();
            if (!StringUtils.hasText(tenant)) {
                tenant = ROOT;
            } else if (tenant != null && !StringUtils.equals(tokenGroupName, tenant)) {
                throw new IllegalArgumentException("Invalid tenant");
            }
            List<com.nextlabs.common.security.Entry> list = map.get(tenant);
            if (list == null) {
                list = new ArrayList<>();
                map.put(tenant, list);
            }
            list.add(entry);
        }
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            for (java.util.Map.Entry<String, List<com.nextlabs.common.security.Entry>> entry : map.entrySet()) { // NOPMD
                String tenant = entry.getKey();
                List<com.nextlabs.common.security.Entry> list = entry.getValue();
                KeyStoreEntry keyStore = getKeyStoreEntry(session, StringUtils.equals(ROOT, tenant) ? null : tenant, list);
                session.saveOrUpdate(keyStore);
            }
            session.commit();
        }
    }

    @Override
    public Certificate getCertificate(String tokenGroupName, String alias)
            throws GeneralSecurityException, IOException {
        KeyStoreEntry entry = getKeyStore(tokenGroupName);
        KeyStore keyStore = toKeyStore(entry);
        return keyStore != null ? keyStore.getCertificate(alias) : null;
    }

    @Override
    public Certificate[] getCertificateChain(String tokenGroupName, String alias)
            throws GeneralSecurityException, IOException {
        KeyStoreEntry entry = getKeyStore(tokenGroupName);
        KeyStore keyStore = toKeyStore(entry);
        return keyStore != null ? keyStore.getCertificateChain(alias) : null;
    }

    @Override
    public Key getKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        KeyStoreEntry entry = getKeyStore(tokenGroupName);
        KeyStore keyStore = toKeyStore(entry);
        if (keyStore != null) {
            String credential = entry.getCredential();
            return keyStore.getKey(alias, credential.toCharArray());
        }
        return null;
    }

    private KeyStoreEntry getKeyStore(DbSession session, String tokenGroupName) {
        Criteria criteria = session.createCriteria(KeyStoreEntry.class);
        if (StringUtils.hasText(tokenGroupName)) {
            criteria.add(Restrictions.eq("tokenGroupName", tokenGroupName));
        } else {
            criteria.add(Restrictions.isNull("tokenGroupName"));
        }
        return (KeyStoreEntry)criteria.uniqueResult();
    }

    public KeyStoreEntry getKeyStore(String tokenGroupName) {
        try (DbSession session = DbSession.newSession()) {
            return getKeyStore(session, tokenGroupName);
        }
    }

    private KeyStoreEntry getKeyStoreEntry(DbSession session, String tokenGroupName,
        List<com.nextlabs.common.security.Entry> entries) throws IOException, GeneralSecurityException {
        KeyStoreEntry keyStoreEntry = getKeyStore(session, tokenGroupName);
        KeyStore keyStore = null;
        String credential = null;
        if (keyStoreEntry != null) {
            byte[] data = keyStoreEntry.getData();
            credential = keyStoreEntry.getCredential();
            keyStore = KeyStore.getInstance(keyStoreEntry.getKeyStoreType().name());
            keyStore.load(new ByteArrayInputStream(data), credential.toCharArray());
        } else {
            credential = UUID.randomUUID().toString().replaceAll("-", "");
            keyStore = KeyStore.getInstance(KeyStoreType.BCFKS.name());
            keyStore.load(null, credential.toCharArray());
            keyStoreEntry = new KeyStoreEntry();
            keyStoreEntry.setCredential(credential);
            keyStoreEntry.setCreationTime(new Date());
            keyStoreEntry.setVersion(0);
            keyStoreEntry.setKeyStoreType(KeyStoreType.BCFKS);
            keyStoreEntry.setTokenGroupName(tokenGroupName);
        }
        for (com.nextlabs.common.security.Entry entry : entries) {
            String alias = entry.getAlias();
            if (entry instanceof CertificateEntry) {
                CertificateEntry certificateEntry = (CertificateEntry)entry;
                Certificate certificate = certificateEntry.getCertificate();
                keyStore.setCertificateEntry(alias, certificate);
            } else if (entry instanceof com.nextlabs.common.security.SecretKeyEntry) {
                com.nextlabs.common.security.SecretKeyEntry secretKeyEntry = (com.nextlabs.common.security.SecretKeyEntry)entry;
                SecretKey key = secretKeyEntry.getKey();
                PasswordProtection passwordProtection = new PasswordProtection(credential.toCharArray());
                SecretKeyEntry keyEntry = new SecretKeyEntry(key);
                keyStore.setEntry(alias, keyEntry, passwordProtection);
            } else if (entry instanceof KeyEntry) {
                KeyEntry keyEntry = (KeyEntry)entry;
                Certificate[] certificates = keyEntry.getCertificates();
                Key key = keyEntry.getKey();
                keyStore.setKeyEntry(alias, key, credential.toCharArray(), certificates);
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keyStore.store(os, credential.toCharArray());
        keyStoreEntry.setData(os.toByteArray());
        return keyStoreEntry;
    }

    @Override
    public SecretKey getSecretKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        KeyStoreEntry entry = getKeyStore(tokenGroupName);
        KeyStore keyStore = toKeyStore(entry);
        if (keyStore == null) {
            return null;
        }
        String credential = entry.getCredential();
        PasswordProtection passwordProtection = new PasswordProtection(credential.toCharArray());
        Entry keyStoreEntry = keyStore.getEntry(alias, passwordProtection);
        if (!(keyStoreEntry instanceof SecretKeyEntry)) {
            return null;
        }
        SecretKeyEntry keyEntry = (SecretKeyEntry)keyStore.getEntry(alias, passwordProtection);
        return keyEntry.getSecretKey();
    }

    @Override
    public void deleteKeyStore(String tokenGroupName) {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(KeyStoreEntry.class);
            criteria.add(Restrictions.eq("tokenGroupName", tokenGroupName));
            KeyStoreEntry keyStore = (KeyStoreEntry)criteria.uniqueResult();
            session.delete(keyStore);
            session.commit();
        }
    }

    private KeyStore toKeyStore(KeyStoreEntry entry) throws GeneralSecurityException, IOException {
        if (entry == null) {
            return null;
        }
        String credential = entry.getCredential();
        byte[] data = entry.getData();
        KeyStore keyStore = KeyStore.getInstance(entry.getKeyStoreType().name());
        keyStore.load(new ByteArrayInputStream(data), credential.toCharArray());
        return keyStore;
    }
}
