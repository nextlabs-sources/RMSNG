package com.nextlabs.nxl;

import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.CryptoBaseRequest.EncryptionRequest;
import com.nextlabs.nxl.DecryptUtil.TokenResponse;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.Certificate;

public final class ProjectDownloadCrypto extends RemoteCrypto {

    private final String ownerMembership;

    public ProjectDownloadCrypto(String ownerMembership) {
        this.ownerMembership = ownerMembership;
    }

    @Override
    public NxlFile encrypt(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException {
        if (!StringUtils.hasText(ownerMembership)) {
            throw new IllegalArgumentException("Owner membership is required");
        }
        validateRequest(request);
        RemoteEncryptionRequest req = (RemoteEncryptionRequest)request;
        KeyPair keyPair = KeyManager.generateDHKeyPair();
        Certificate[] chain = requestMemberCertificate(req, keyPair.getPublic());
        TokenResponse tokenResponse = requestNewTokens(req, chain, keyPair.getPrivate());
        request.setMembership(ownerMembership);
        if (request.getFilePolicy() != null) {
            request.getFilePolicy().setIssuer(ownerMembership);
        }
        return EncryptUtil.doEncrypt(request, keyPair, chain, tokenResponse);
    }
}
