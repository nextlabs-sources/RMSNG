package com.nextlabs.rms.task;

import com.nextlabs.common.Environment;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.shared.LogConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class BackupKeyStore implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private String tokenGroupName;

    public BackupKeyStore(String tenantName) {
        this.tokenGroupName = tenantName;
    }

    @Override
    public void run() {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(KeyStoreEntry.class);
            if (StringUtils.hasText(tokenGroupName)) {
                criteria.add(Restrictions.eq("tokenGroupName", tokenGroupName));
            } else {
                criteria.add(Restrictions.isNull("tokenGroupName"));
            }
            KeyStoreEntry keyStoreEntry = (KeyStoreEntry)criteria.uniqueResult();
            if (keyStoreEntry == null) {
                LOGGER.warn("No keystore entry present for the tenant", tokenGroupName);
                return;
            }
            KeyStore keyStore = KeyStore.getInstance(keyStoreEntry.getKeyStoreType().name());
            char[] credential = keyStoreEntry.getCredential().toCharArray();
            keyStore.load(new ByteArrayInputStream(keyStoreEntry.getData()), credential);
            char[] newPassword = WebConfig.getInstance().getProperty(WebConfig.KEYSTORE_PASS, "123next!").toCharArray();
            PasswordProtection passwordProtection = new PasswordProtection(newPassword);
            KeyStore tempKeyStore = KeyStore.getInstance(IKeyStoreManager.KEYSTORE_TYPE);
            tempKeyStore.load(null, newPassword);
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String entry = aliases.nextElement();
                tempKeyStore.setEntry(entry, keyStore.getEntry(entry, new PasswordProtection(credential)), passwordProtection);
            }
            File bckupDir = new File(Environment.getInstance().getSharedDir(), "bkpKeys");
            if (!bckupDir.exists()) {
                FileUtils.mkdir(bckupDir);
            }
            try (FileOutputStream fos = new FileOutputStream(bckupDir + File.separator + tokenGroupName + IKeyStoreManager.BCFKS_TYPE_EXTENSION)) {
                tempKeyStore.store(fos, newPassword);
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error occurred while creating keystore backup for  (Tenant: {}): {}", tokenGroupName, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error("Error occurred while creating keystore backup for  (Tenant: {}): {}", tokenGroupName, e.getMessage(), e);
        }
    }

}
