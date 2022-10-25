/**
 *
 */
package com.nextlabs.common.security;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

public class KeyStoreManager {

    private KeyStore keyStore;
    private char[] password;
    private String keyStoreType;
    private File file;
    private RandomAccessFile fileChannelProvider;
    private FileLock lock;

    public enum KeyStoreType {
        JKS,
        BCFKS
    }

    public static final String RMS_KEYSTORE_FILE_SECURE = "rms-keystore.bcfks";

    public static final String RMS_KEYSTORE_SECURE_ALIAS = "rms";

    public static final String RMS_KEYSTORE_FILE_PASSWORD = "123next!";

    public KeyStoreManager(File file, char[] password, String keyStoreType) {
        this.file = file;
        this.password = password;
        this.keyStoreType = keyStoreType;
    }

    public boolean acquireLock() throws IOException {
        if (file == null || !file.exists()) {
            return false;
        }
        if (lock == null) {
            fileChannelProvider = new RandomAccessFile(file, "rw");
            FileChannel channel = fileChannelProvider.getChannel();
            lock = channel.lock();
        }
        return true;
    }

    public void release() {
        if (lock != null) {
            IOUtils.closeQuietly(lock);
            IOUtils.closeQuietly(lock.channel());
            lock = null;
            IOUtils.closeQuietly(fileChannelProvider);
            fileChannelProvider = null;
        }
    }

    public boolean containsAlias(String alias) throws GeneralSecurityException, IOException {
        return getKeyStore().containsAlias(alias);
    }

    public Key getKey(String alias) throws GeneralSecurityException, IOException {
        return getKeyStore().getKey(alias, password);
    }

    public SecretKey getSecretKey(String alias) throws IOException, GeneralSecurityException {
        PasswordProtection passwordProtection = new PasswordProtection(password);
        SecretKeyEntry keyEntry = (SecretKeyEntry)getKeyStore().getEntry(alias, passwordProtection);
        if (keyEntry == null) {
            return null;
        }
        return keyEntry.getSecretKey();
    }

    public void setSecretKey(String alias, SecretKey key) throws GeneralSecurityException, IOException {
        PasswordProtection passwordProtection = new PasswordProtection(password);
        SecretKeyEntry keyEntry = new SecretKeyEntry(key);
        getKeyStore().setEntry(alias, keyEntry, passwordProtection);
    }

    public Certificate getCertificate(String alias) throws GeneralSecurityException, IOException {
        return getKeyStore().getCertificate(alias);
    }

    public Certificate[] getCertificateChain(String alias) throws GeneralSecurityException, IOException {
        return getKeyStore().getCertificateChain(alias);
    }

    public void setKeyEntry(String keyName, Key key, Certificate[] chain) throws IOException, GeneralSecurityException {
        getKeyStore().setKeyEntry(keyName, key, password, chain);
    }

    public void reload() throws GeneralSecurityException, IOException {
        keyStore = null;
        loadKeyStore();
    }

    public File getKeyStoreFile() {
        return file;
    }

    public void save() throws IOException, GeneralSecurityException {
        save(file);
    }

    private void loadKeyStore() throws GeneralSecurityException, IOException {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance(keyStoreType);
            if (file != null && file.exists()) {
                FileInputStream fileInputStream = null;
                if (fileChannelProvider != null) {
                    fileInputStream = new FileInputStream(fileChannelProvider.getFD());
                } else {
                    fileInputStream = new FileInputStream(file);
                }
                try {
                    keyStore.load(fileInputStream, password);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                }
            } else {
                keyStore.load(null);
            }
        }
    }

    public KeyStore getKeyStore() throws GeneralSecurityException, IOException {
        loadKeyStore();
        return keyStore;
    }

    public void save(File keyStoreFile) throws GeneralSecurityException, IOException {
        if (keyStoreFile == null) {
            return;
        }
        loadKeyStore();
        Path path = keyStoreFile.getParentFile().toPath();
        path = Files.createTempFile(path, keyStoreFile.getName(), ".tmp");
        FileUtils.setOwnerFullAccess(path);
        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
        try {
            keyStore.store(fileOutputStream, password);
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }
        Path outputPath = keyStoreFile.toPath();
        Files.move(path, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
