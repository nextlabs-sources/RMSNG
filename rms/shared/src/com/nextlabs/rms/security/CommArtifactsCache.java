package com.nextlabs.rms.security;

import com.bluejungle.framework.crypt.ReversibleEncryptor;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.rms.shared.LogConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CommArtifactsCache {

    public static final boolean MODE_UNSECURE = false;
    public static final boolean MODE_SECURE = true;
    private static final CommArtifactsCache INSTANCE = new CommArtifactsCache();
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private final Map<String, Artifact> artifactMap = new HashMap<String, Artifact>();
    private KeyStore trustStoreSecure;

    public static CommArtifactsCache getInstance() {
        return INSTANCE;
    }

    private CommArtifactsCache() {
    }

    private KeyManager[] cacheSecureKeyFile(File certFile) {
        if (!certFile.exists() || !certFile.isFile() || !certFile.canRead()) {
            return null;
        }
        String password;
        try {
            password = getCredential();
        } catch (FileNotFoundException e) {
            return null;
        }
        if (password == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(certFile);
            KeyStore kstore = KeyStore.getInstance(SSLSocketFactoryGenerator.KEYSTORE_TYPE);
            kstore.load(is, password.toCharArray());
            KeyManagerFactory kmfSecure = KeyManagerFactory.getInstance(SSLSocketFactoryGenerator.SSL_ALGORITHM);
            kmfSecure.init(kstore, password.toCharArray());
            synchronized (kmfSecure) {
                Artifact tempArtifact = new Artifact(certFile.lastModified(), kmfSecure.getKeyManagers());
                artifactMap.put(certFile.getAbsolutePath(), tempArtifact);
            }
            return kmfSecure.getKeyManagers();
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("Error occurred when accessing {}: {}", certFile.getName(), e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private String getCredential() throws FileNotFoundException {
        File agentKeyFile = new File(SSLSocketFactoryGenerator.CERT_PATH, SSLSocketFactoryGenerator.KEYSTORE_FILE_PASS);
        if (!agentKeyFile.exists() || !agentKeyFile.isFile() || !agentKeyFile.canRead()) {
            throw new FileNotFoundException("Unable to read " + agentKeyFile.getName());
        }
        InputStream is = null;
        try {
            is = new FileInputStream(agentKeyFile);
            String result = IOUtils.toString(is);
            ReversibleEncryptor decryptor = new ReversibleEncryptor();
            result = decryptor.decrypt(result);
            result = result.substring(5);
            return result;
        } catch (IOException e) {
            LOGGER.error("Error occurred when accessing {}: {}", agentKeyFile.getName(), e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private TrustManager[] cacheTrustManagersSecureFile(File certFile) {
        if (!certFile.exists() || !certFile.isFile() || !certFile.canRead()) {
            return null;
        }
        String password;
        try {
            password = getCredential();
        } catch (FileNotFoundException e) {
            return null;
        }
        if (password == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(certFile);
            KeyStore kstore = KeyStore.getInstance(SSLSocketFactoryGenerator.KEYSTORE_TYPE);
            kstore.load(is, password.toCharArray());
            trustStoreSecure = kstore;
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(kstore);
            synchronized (artifactMap) {
                Artifact tempArtifact = new Artifact(certFile.lastModified(), tmf.getTrustManagers());
                artifactMap.put(certFile.getAbsolutePath(), tempArtifact);
            }
            return tmf.getTrustManagers();
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("Error occurred when accessing {}: {}", certFile.getName(), e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private KeyManager[] cacheUnsecureKeyFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            KeyStore kstore = KeyStore.getInstance(SSLSocketFactoryGenerator.KEYSTORE_TYPE);
            kstore.load(is, SSLSocketFactoryGenerator.KEYSTORE_PASS_UNSECURE.toCharArray());
            KeyManagerFactory instance = KeyManagerFactory.getInstance(SSLSocketFactoryGenerator.SSL_ALGORITHM);
            instance.init(kstore, SSLSocketFactoryGenerator.KEYSTORE_PASS_UNSECURE.toCharArray());
            synchronized (artifactMap) {
                Artifact keyArtifact = new Artifact(file.lastModified(), instance.getKeyManagers());
                artifactMap.put(file.getAbsolutePath(), keyArtifact);
            }
            return instance.getKeyManagers();
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("Error occurred when accessing temporary keystore: ", e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public KeyManager[] getKeyManagersSecure() {
        File certFile = new File(SSLSocketFactoryGenerator.CERT_PATH, SSLSocketFactoryGenerator.KEYSTORE_FILE_SECURE);
        if (artifactMap.get(certFile.getAbsolutePath()) == null) {
            return cacheSecureKeyFile(certFile);
        } else {
            Artifact ar = artifactMap.get(certFile.getAbsolutePath());
            long lastModifiedTime = certFile.lastModified();
            if (ar.getTimeStamp() != lastModifiedTime) {
                return cacheSecureKeyFile(certFile);
            } else {
                return (KeyManager[])artifactMap.get(certFile.getAbsolutePath()).getKeyManagers();
            }
        }
    }

    public KeyManager[] getKeyManagersUnsecure() {
        File file = new File(SSLSocketFactoryGenerator.CERT_PATH, SSLSocketFactoryGenerator.KEYSTORE_FILE_UNSECURE);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        long lastModifiedTime = file.lastModified();
        if (artifactMap.get(file.getAbsolutePath()) == null) {
            return cacheUnsecureKeyFile(file);
        } else {
            Artifact ar = artifactMap.get(file.getAbsolutePath());
            if (ar.getTimeStamp() != lastModifiedTime) {
                return cacheUnsecureKeyFile(file);
            } else {
                return (KeyManager[])artifactMap.get(file.getAbsolutePath()).getKeyManagers();
            }
        }
    }

    public TrustManager[] getTrustManagersSecure() {
        File certFile = new File(SSLSocketFactoryGenerator.CERT_PATH, SSLSocketFactoryGenerator.TRUSTSTORE_FILE_SECURE);
        long lastModifiedTime = certFile.lastModified();
        if (artifactMap.get(certFile.getAbsolutePath()) == null) {
            return cacheTrustManagersSecureFile(certFile);
        } else {
            Artifact ar = artifactMap.get(certFile.getAbsolutePath());
            if (ar.getTimeStamp() != lastModifiedTime) {
                return cacheTrustManagersSecureFile(certFile);
            } else {
                return (TrustManager[])artifactMap.get(certFile.getAbsolutePath()).getKeyManagers();
            }
        }
    }

    public KeyStore getTrustStoreSecure() {
        return trustStoreSecure;
    }

}
