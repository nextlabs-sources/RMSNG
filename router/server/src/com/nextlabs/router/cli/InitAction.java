package com.nextlabs.router.cli;

import com.nextlabs.common.security.Entry;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.util.CertificateAuthority;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.router.Config;
import com.nextlabs.router.security.KeyStoreManagerImpl;
import com.nextlabs.router.servlet.LogConstants;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InitAction implements Action {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @Override
    public void execute(Config config, String[] args) {
        try {
            KeyManager km = new KeyManager(new KeyStoreManagerImpl());
            boolean hasRootRSA = km.containsAlias(null, IKeyStoreManager.ROOT_RSA);
            if (!hasRootRSA) {
                LOGGER.warn("Keystore doesn't exist, creating a new keystore");
                createRootKeystore();
            }
            LOGGER.info("Router initialized.");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void createRootKeystore() throws IOException, GeneralSecurityException {
        KeyPair keypair = KeyManager.generateRSAKeyPair();
        PrivateKey privKey = keypair.getPrivate();
        PublicKey pubKey = keypair.getPublic();
        X509Certificate ca = CertificateAuthority.sign(privKey, pubKey, "r.skydrm.com", "r.skydrm.com", pubKey);

        KeyPair dh = KeyManager.generateDHKeyPair();
        Certificate cert = CertificateAuthority.sign(privKey, ca, "r.skydrm.com", dh.getPublic());

        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
        List<Entry> rsaEntries = km.createKeyEntry(null, IKeyStoreManager.ROOT_RSA, privKey, new Certificate[] { ca });
        List<Entry> dhEntries = km.createKeyEntry(null, IKeyStoreManager.ROOT_DH, dh.getPrivate(), new Certificate[] {
            cert });
        List<Entry> entries = new ArrayList<>(rsaEntries.size() + dhEntries.size());
        entries.addAll(rsaEntries);
        entries.addAll(dhEntries);
        km.createKeyStore(null, entries);
    }
}
