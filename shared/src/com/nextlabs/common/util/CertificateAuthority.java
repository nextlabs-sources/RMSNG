package com.nextlabs.common.util;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class CertificateAuthority {

    private static final long EXPIRATION = 100L * 365 * 24 * 60 * 60 * 1000;
    private static final String SIGALG = "SHA1withRSA";

    private CertificateAuthority() {
    }

    public static X509Certificate sign(PrivateKey caPrivKey, X509Certificate caCert, String cn, PublicKey pubKey)
            throws GeneralSecurityException {
        X500Principal issuer = caCert.getSubjectX500Principal();
        PublicKey caPubKey = caCert.getPublicKey();
        return sign(caPrivKey, caPubKey, issuer, cn, pubKey);
    }

    public static X509Certificate sign(PrivateKey caPrivKey, PublicKey caPubKey, String caName, String cn,
        PublicKey pubKey) throws GeneralSecurityException {
        X500Principal issuer = new X500Principal("CN=" + caName + ",O=nextlabs,OU=rms,L=San Mateo,ST=California,C=US");
        return sign(caPrivKey, caPubKey, issuer, cn, pubKey);
    }

    public static X509Certificate sign(PrivateKey caPrivKey, PublicKey caPubKey, X500Principal issuer, String cn,
        PublicKey pubKey) throws GeneralSecurityException {
        try {
            X500Principal subject = new X500Principal("CN=" + cn + ",O=nextlabs,OU=rms,L=San Mateo,ST=California,C=US");
            Date begin = new Date();
            Date ends = new Date(begin.getTime() + EXPIRATION);

            BigInteger serial = new BigInteger(StringUtils.toBytesQuietly(UUID.randomUUID().toString(), "UTF-8"));
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(X500Name.getInstance(issuer.getEncoded()), serial, begin, ends, X500Name.getInstance(subject.getEncoded()), pubKey);

            if (subject.equals(issuer)) {
                builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(5));
            } else {
                JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
                builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(pubKey));

                builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(caPubKey));

                builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

                NetscapeCertType netscapeCertType = new NetscapeCertType(NetscapeCertType.sslClient | NetscapeCertType.sslServer);
                builder.addExtension(MiscObjectIdentifiers.netscapeCertType, false, netscapeCertType);

                KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment);
                builder.addExtension(Extension.keyUsage, true, keyUsage);

                KeyPurposeId[] keyPurposeIds = new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth,
                    KeyPurposeId.id_kp_serverAuth };
                ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(keyPurposeIds);
                builder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage);
            }
            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGALG).setProvider("BCFIPS");
            ContentSigner signer = signerBuilder.build(caPrivKey);
            X509CertificateHolder certificateHolder = builder.build(signer);
            X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BCFIPS").getCertificate(certificateHolder);
            /*
             * Next certificate factory trick is needed to make sure that the
             * certificate delivered to the caller is provided by the default
             * security provider instead of BouncyCastle. If we don't do this trick
             * we might run into trouble when trying to use the CertPath validator.
             */
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BCFIPS");
            return (X509Certificate)certificateFactory.generateCertificate(new ByteArrayInputStream(certificate.getEncoded()));
        } catch (CertIOException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        } catch (OperatorCreationException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }
}
