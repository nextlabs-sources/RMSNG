package com.nextlabs.nxl;

import com.nextlabs.nxl.CryptoBaseRequest.EncryptionRequest;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface ICrypto {

    public NxlFile encrypt(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException;

    NxlFile refreshNxl(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException, JsonException;
}
