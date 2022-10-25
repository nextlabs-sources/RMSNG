package com.nextlabs.common.util;

import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.CertificateEntry;
import com.nextlabs.common.security.Entry;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyEntry;
import com.nextlabs.common.security.SecretKeyEntry;
import com.nextlabs.nxl.exception.FIPSError;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class KeyManager {

    private final IKeyStoreManager manager;

    public KeyManager(IKeyStoreManager manager) {
        this.manager = manager;
    }

    public boolean containsAlias(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        return manager.containsAlias(tokenGroupName, alias);
    }

    public List<Entry> createKeyEntry(String tokenGroupName, String alias, Key key, Certificate[] chain)
            throws GeneralSecurityException, IOException {
        List<Entry> entries = new ArrayList<Entry>(2);
        if (alias.startsWith(IKeyStoreManager.PREFIX_ICA)) {
            if (!manager.containsAlias(null, IKeyStoreManager.ROOT_RSA)) {
                CertificateEntry rsaCertEntry = new CertificateEntry();
                rsaCertEntry.setAlias(IKeyStoreManager.ROOT_RSA);
                rsaCertEntry.setCertificate(chain[1]);
                entries.add(rsaCertEntry);
            }
            KeyEntry icaEntry = new KeyEntry();
            icaEntry.setTokenGroupName(tokenGroupName);
            icaEntry.setAlias(alias);
            icaEntry.setKey(key);
            icaEntry.setCertificates(new Certificate[] { chain[0] });
            entries.add(icaEntry);
        } else if (alias.startsWith(IKeyStoreManager.PREFIX_DH)) {
            if (!manager.containsAlias(null, IKeyStoreManager.ROOT_DH)) {
                CertificateEntry dhCertEntry = new CertificateEntry();
                dhCertEntry.setAlias(IKeyStoreManager.ROOT_DH);
                dhCertEntry.setCertificate(chain[1]);
                entries.add(dhCertEntry);
            }
            KeyEntry dhEntry = new KeyEntry();
            dhEntry.setTokenGroupName(tokenGroupName);
            dhEntry.setAlias(alias);
            dhEntry.setKey(key);
            dhEntry.setCertificates(new Certificate[] { chain[0] });
            entries.add(dhEntry);
        } else {
            KeyEntry keyEntry = new KeyEntry();
            keyEntry.setTokenGroupName(tokenGroupName);
            keyEntry.setAlias(alias);
            keyEntry.setKey(key);
            keyEntry.setCertificates(chain);
            entries.add(keyEntry);
        }
        return entries;
    }

    public void createKeyStore(String tokenGroupName, List<Entry> entries)
            throws GeneralSecurityException, IOException {
        manager.createKeyStore(tokenGroupName, entries);
    }

    public SecretKeyEntry createSecretKey(String tokenGroupName, String alias, SecretKey key) {
        SecretKeyEntry entry = new SecretKeyEntry();
        entry.setAlias(alias);
        entry.setTokenGroupName(tokenGroupName);
        entry.setKey(key);
        return entry;
    }

    public Certificate getCertificate(String tokenGroupName, String alias)
            throws GeneralSecurityException, IOException {
        return manager.getCertificate(tokenGroupName, alias);
    }

    public Certificate[] getCertificateChain(String tokenGroupName, String alias)
            throws GeneralSecurityException, IOException {
        if (alias.startsWith(IKeyStoreManager.PREFIX_ICA)) {
            Certificate[] ret = new Certificate[2];
            ret[0] = manager.getCertificate(tokenGroupName, alias);
            ret[1] = manager.getCertificate(null, IKeyStoreManager.ROOT_RSA);
            return ret;
        } else if (alias.startsWith(IKeyStoreManager.PREFIX_DH)) {
            Certificate[] ret = new Certificate[2];
            ret[0] = manager.getCertificate(tokenGroupName, alias);
            ret[1] = manager.getCertificate(null, IKeyStoreManager.ROOT_DH);
            return ret;
        }
        return manager.getCertificateChain(tokenGroupName, alias);
    }

    public Key getKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        return this.manager.getKey(tokenGroupName, alias);
    }

    public SecretKey getSecretKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException {
        return manager.getSecretKey(tokenGroupName, alias);
    }

    public void deleteKeyStore(String tokenGroupName) {
        manager.deleteKeyStore(tokenGroupName);
    }

    public static SecretKey createAgreement(PrivateKey privKey, PublicKey pubKey, PublicKey agreement)
            throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH", "BCFIPS");
        keyAgreement.init(privKey);
        keyAgreement.doPhase(pubKey, false);
        keyAgreement.doPhase(agreement, true);
        return keyAgreement.generateSecret("AES");
    }

    public static PublicKey createDHPublicKey(BigInteger y) throws GeneralSecurityException {
        DHPublicKeySpec spec = new DHPublicKeySpec(y, new BigInteger(IKeyStoreManager.P, 16), new BigInteger(IKeyStoreManager.G, 16));
        KeyFactory factory = KeyFactory.getInstance("DH", "BCFIPS");
        return factory.generatePublic(spec);
    }

    public static DHParameterSpec createParameterSpec() throws GeneralSecurityException {
        AlgorithmParameterGenerator pg = AlgorithmParameterGenerator.getInstance("DH", "BCFIPS");
        pg.init(IKeyStoreManager.KEY_LENGTH);
        AlgorithmParameters params = pg.generateParameters();
        return params.getParameterSpec(DHParameterSpec.class);
    }

    public static PublicKey createPartialAgreement(PrivateKey privKey, PublicKey pubKey)
            throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH", "BCFIPS");
        keyAgreement.init(privKey);
        return (PublicKey)keyAgreement.doPhase(pubKey, false);
    }

    public static byte[] createToken(SecretKey uek, SecretKey hsk, byte[] duid, int maintanenceLevel)
            throws GeneralSecurityException {
        byte[] buf = hsk.getEncoded();
        MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
        for (int i = 0, size = 1000 - maintanenceLevel; i < size; ++i) {
            md.reset();
            buf = md.digest(buf);
        }
        return AuthUtils.hmac(uek.getEncoded(), buf, duid);
    }

    public static KeyPair generateDHKeyPair() throws GeneralSecurityException {
        DHParameterSpec paramSpec = new DHParameterSpec(new BigInteger(IKeyStoreManager.P, 16), new BigInteger(IKeyStoreManager.G, 16));
        KeyPairGenerator kg = KeyPairGenerator.getInstance("DH", "BCFIPS");
        kg.initialize(paramSpec);
        return kg.generateKeyPair();
    }

    public static KeyPair generateRSAKeyPair() throws GeneralSecurityException {
        KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA", "BCFIPS");
        kg.initialize(IKeyStoreManager.KEY_LENGTH);
        return kg.generateKeyPair();
    }

    public static byte[] randomBytes(int size) {
        try {
            SecureRandom random = SecureRandom.getInstance("DEFAULT", "BCFIPS");
            byte[] buf = new byte[size];
            random.nextBytes(buf);
            return buf;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }

    public static String signData(PrivateKey priv, String... data) throws GeneralSecurityException, IOException {
        Signature sig = Signature.getInstance(IKeyStoreManager.ALG_SIGNATURE, "BCFIPS");
        sig.initSign(priv);
        for (String d : data) {
            sig.update(StringUtils.toBytesQuietly(d));
        }
        return Base64Codec.encodeAsString(sig.sign());
    }

    public static boolean verify(Certificate rootCA, Certificate cert, String signature, String... data)
            throws GeneralSecurityException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BCFIPS");
        CertPath cp = cf.generateCertPath(Collections.singletonList(cert));
        TrustAnchor anchor = new TrustAnchor((X509Certificate)rootCA, null);
        PKIXParameters params = new PKIXParameters(Collections.singleton(anchor));
        params.setRevocationEnabled(false);
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX", "BCFIPS");
        try {
            cpv.validate(cp, params);
        } catch (CertPathValidatorException e) {
            return false;
        }

        Signature sig = Signature.getInstance(IKeyStoreManager.ALG_SIGNATURE, "BCFIPS");
        sig.initVerify(cert.getPublicKey());
        try {
            for (String d : data) {
                sig.update(StringUtils.toBytesQuietly(d));
            }
            return sig.verify(Base64Codec.decode(signature));
        } catch (SignatureException e) {
            return false;
        }
    }
}
