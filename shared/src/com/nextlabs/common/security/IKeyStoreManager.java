package com.nextlabs.common.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.cert.Certificate;
import java.util.List;

import javax.crypto.SecretKey;

public interface IKeyStoreManager {

    public static final String KEYSTORE_TYPE = "BCFKS";
    public static final String BCFKS_TYPE_EXTENSION = ".bcfks";
    public static final String ALG_HMAC_SHA256 = "HmacSHA256";
    public static final String ALG_SHA256 = "SHA256";
    public static final String ALG_MD5 = "MD5";
    public static final String ALG_RSA = "RSA";
    public static final String ALG_SIGNATURE = "SHA1WithRSA";

    public static final int KEY_BYTES = 32;

    public static final String PREFIX_HSK = "hsk_";
    public static final String PREFIX_ICA = "ica_";
    public static final String PREFIX_DH = "dh_";

    public static final String ROOT_RSA = "rootRSA";
    public static final String ROOT_DH = "rootDH";

    public static final String P = "D310125B294DBD856814DFD4BAB4DC767DF6A999C9EDFA8F8D7B12551F8D71EF6032357405C7F11EE147DB0332716FC8FD85ED027585268360D16BD761563D7D1659D4D73DAED617F3E4223F48BCEFA421860C3FC4393D27545677B22459E852F5254D3AC58C0D63DD79DE2D8D868CD940DECF5A274605DB0EEE762020C39D0F6486606580EAACCE16FB70FB7C759EA9AABAB4DCBF941891B0CE94EC4D3D5954217C6E84A9274F1AB86073BDF9DC851E563B90455B8397DAE3A1B998607BB7699CEA0805A7FF013EF44FDE7AF830F1FD051FFAEC539CE4452D8229098AE3EE2008AB9DB7B2C948312CBC0137C082D6672618E1BFE5D5006E810DC7AA7F1E6EE3";
    public static final String G = "64ACEBA5F7BC803EF29731C9C6AE009B86FC5201F81BC2B8F84890FCF71CAD51C1429FD261A2A715C8946154E0E4E28EF6B2D493CC1739F5659E9F14DD14037F5FE72B3BA4D9BCB3B95B8417BDA48F118E61C8214CF8D558DA6774F08B58D97B2CCE20F5AA2F8E9539C014E7761E4E6336CFFC35127DDD527206766AE72045C11B0FF4DA76172523713B31C9F18ABABA92612BDE105141F04DB5DA3C39CDE5C6877B7F8CD96949FCC876E2C1224FB9188D714FDD6CB80682F8967833AD4B51354A8D58598E6B2DEF4571A597AD39BD3177D54B24CA518EDA996EEDBA8A31D5876EFED8AA44023CC9F13D86DCB4DDFCF389C7A1435082EF69703603638325954E";

    public static final int KEY_LENGTH = 2048;

    public static final String ROOT = "<ROOT>";

    public boolean containsAlias(String tokenGroupName, String alias) throws GeneralSecurityException, IOException;

    public void createKeyStore(String tokenGroupName, List<Entry> entries) throws GeneralSecurityException, IOException;

    public Certificate getCertificate(String tokenGroupName, String alias) throws GeneralSecurityException, IOException;

    public Certificate[] getCertificateChain(String tokenGroupName, String alias)
            throws GeneralSecurityException, IOException;

    public Key getKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException;

    public SecretKey getSecretKey(String tokenGroupName, String alias) throws GeneralSecurityException, IOException;

    public void deleteKeyStore(String tokenGroupName);
}
