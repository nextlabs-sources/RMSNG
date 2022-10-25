package com.nextlabs.rms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextlabs.nxl.exception.FIPSError;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class JWTVerifier {

    private final String secret;
    private final String audience;
    private final String issuer;

    private ObjectMapper mapper;

    private Map<String, String> algorithms;

    public JWTVerifier(String secret, String audience, String issuer) {
        if (secret == null || "".equals(secret)) {
            throw new IllegalArgumentException("Secret cannot be null or empty");
        }
        mapper = new ObjectMapper();
        algorithms = new HashMap<String, String>();
        algorithms.put("HS256", "HmacSHA256");
        algorithms.put("HS384", "HmacSHA384");
        algorithms.put("HS512", "HmacSHA512");

        this.secret = secret;
        this.audience = audience;
        this.issuer = issuer;
    }

    public JWTVerifier(String secret, String audience) {
        this(secret, audience, null);
    }

    public JWTVerifier(String secret) {
        this(secret, null, null);
    }

    /**
     * Performs JWT validation
     *
     * @param token token to verify
     * @throws SignatureException    when signature is invalid
     * @throws IllegalStateException when token's structure, expiration, issuer or audience are invalid
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String token)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException,
            IOException, SignatureException {
        if (token == null || "".equals(token)) {
            throw new IllegalStateException("token not set");
        }

        String[] pieces = token.split("\\.");

        // check number of segments
        if (pieces.length != 3) {
            throw new IllegalStateException("Wrong number of segments: " + pieces.length);
        }

        // get JWTHeader JSON object. Extract algorithm
        JsonNode jwtHeader = decodeAndParse(pieces[0]);

        String algorithm = getAlgorithm(jwtHeader);

        // get JWTClaims JSON object
        JsonNode jwtPayload = decodeAndParse(pieces[1]);

        // check signature
        verifySignature(pieces, algorithm);

        // additional JWTClaims checks
        verifyExpiration(jwtPayload);
        verifyIssuer(jwtPayload);
        //verifyAudience(jwtPayload);

        return mapper.treeToValue(jwtPayload, Map.class);
    }

    void verifySignature(String[] pieces, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, UnsupportedEncodingException {
        Mac hmac;
        try {
            hmac = Mac.getInstance(algorithm, "BCFIPS");
            hmac.init(new SecretKeySpec(Base64.decodeBase64(secret), algorithm));
            byte[] sig = hmac.doFinal(new StringBuilder(pieces[0]).append(".").append(pieces[1]).toString().getBytes("UTF-8"));

            if (!Arrays.equals(sig, Base64.decodeBase64(pieces[2]))) {
                throw new SignatureException("signature verification failed");
            }
        } catch (NoSuchProviderException e) {
            throw new FIPSError("BCFIPS provider not available", e);
        }
    }

    void verifyExpiration(JsonNode jwtClaims) {
        final long expiration = jwtClaims.has("exp") ? jwtClaims.get("exp").asLong(0) : 0;

        if (expiration != 0 && System.currentTimeMillis() / 1000L >= expiration) {
            throw new IllegalStateException("jwt expired");
        }
    }

    void verifyIssuer(JsonNode jwtClaims) {
        final String issuerFromToken = jwtClaims.has("iss") ? jwtClaims.get("iss").asText() : null;

        if (issuerFromToken != null && issuer != null && !issuer.equals(issuerFromToken)) {
            throw new IllegalStateException("jwt issuer invalid");
        }
    }

    void verifyAudience(JsonNode jwtClaims) {
        final String audienceFromToken = jwtClaims.has("aud") ? jwtClaims.get("aud").asText() : null;

        if (audienceFromToken != null && !audience.equals(audienceFromToken)) {
            throw new IllegalStateException("jwt audience invalid");
        }
    }

    String getAlgorithm(JsonNode jwtHeader) {
        final String algorithmName = jwtHeader.has("alg") ? jwtHeader.get("alg").asText() : null;

        if (jwtHeader.get("alg") == null) {
            throw new IllegalStateException("algorithm not set");
        }

        if (algorithms.get(algorithmName) == null) {
            throw new IllegalStateException("unsupported algorithm");
        }

        return algorithms.get(algorithmName);
    }

    JsonNode decodeAndParse(String b64String) throws IOException {
        String jsonString = new String(Base64.decodeBase64(b64String), "UTF-8");
        return mapper.readValue(jsonString, JsonNode.class);
    }
}
