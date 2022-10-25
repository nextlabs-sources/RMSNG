package com.nextlabs.nxl;

import com.google.gson.JsonObject;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.CryptoBaseRequest.EncryptionRequest;
import com.nextlabs.nxl.DecryptUtil.TokenResponse;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Properties;

import javax.crypto.interfaces.DHPublicKey;

import org.apache.commons.lang3.ArrayUtils;

public class RemoteCrypto implements ICrypto {

    public static final RemoteCrypto INSTANCE = new RemoteCrypto();

    @Override
    public NxlFile encrypt(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException {
        validateRequest(request);
        RemoteEncryptionRequest req = (RemoteEncryptionRequest)request;
        KeyPair keyPair = KeyManager.generateDHKeyPair();
        Certificate[] chain = requestMemberCertificate(req, keyPair.getPublic());
        TokenResponse tokenResponse = requestNewTokens(req, chain, keyPair.getPrivate());
        return EncryptUtil.doEncrypt(request, keyPair, chain, tokenResponse);
    }

    @Override
    public NxlFile refreshNxl(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException, JsonException {
        validateRequest(request);
        RemoteEncryptionRequest req = (RemoteEncryptionRequest)request;
        KeyPair keyPair = KeyManager.generateDHKeyPair();
        Certificate[] chain = requestMemberCertificate(req, keyPair.getPublic());
        TokenResponse newTokenResponse = requestNewTokens(req, chain, keyPair.getPrivate());
        TokenResponse oldTokenResponse = getOldTokenResponse(req);
        return EncryptUtil.doRefreshNXL(request, oldTokenResponse, newTokenResponse, keyPair, chain);
    }

    /**
     * This method gets token details for source NXL file. Later this token is used to decrypt the header at destination.
     * @param req this is a remoteencryptionrequest. This request contains the path of source NXL file.
     * @return TokenResponse of the source NXL File
     * @throws IOException
     * @throws NxlException
     * @throws GeneralSecurityException
     * @throws JsonException
     */
    private TokenResponse getOldTokenResponse(RemoteEncryptionRequest req)
            throws IOException, NxlException, GeneralSecurityException, JsonException {
        NxlFile header = NxlFile.parse(new FileInputStream(req.getOriginalFile()));
        byte[] token = DecryptUtil.requestToken(req.getRemoteURL(), req.getUserId(), req.getTicket(), req.getClientId(), req.getPlatformId(), req.getTenantName(), header.getOwner(), header.getDuid(), header.getRootAgreement(), header.getMaintenanceLevel(), header.getProtectionType(), DecryptUtil.getFilePolicyStr(header, null), DecryptUtil.getTagsString(header, null));
        return new TokenResponse(Hex.toByteArray(header.getDuid()), token, header.getMaintenanceLevel());
    }

    protected Certificate[] requestMemberCertificate(RemoteEncryptionRequest request, PublicKey dhPublicKey)
            throws IOException, CertificateException, NoSuchProviderException {
        validateRequest(request);
        String remoteURL = request.getRemoteURL();
        int userId = request.getUserId();
        String ticket = request.getTicket();
        String clientId = request.getClientId();
        String membership = request.getMembership();
        Integer platformId = request.getPlatformId() != null ? request.getPlatformId() : DeviceType.WEB.getLow();
        String path = remoteURL + "/rs/membership";

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(userId));
        prop.setProperty("ticket", ticket);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("userId", String.valueOf(userId));
        req.addParameter("ticket", ticket);
        req.addParameter("membership", membership);
        req.addParameter("publicKey", Base64Codec.encodeAsString(dhPublicKey.getEncoded()));

        String ret = RestClient.put(path, prop, req.toJson());
        JsonResponse resp = JsonResponse.fromJson(ret);

        if (resp.hasError()) {
            throw new IOException("Failed request member certificate:" + resp.getMessage());
        }
        String certs = resp.getResultAsString("certficates");
        return KeyUtils.readCertificateChain(certs);
    }

    protected TokenResponse requestNewTokens(RemoteEncryptionRequest request, Certificate[] chain,
        PrivateKey privateKey)
            throws IOException, GeneralSecurityException {
        validateRequest(request);
        String remoteURL = request.getRemoteURL();
        int userId = request.getUserId();
        String ticket = request.getTicket();
        String clientId = request.getClientId();
        String membership = request.getMembership();
        Integer platformId = request.getPlatformId() != null ? request.getPlatformId() : DeviceType.WEB.getLow();
        String path = remoteURL + "/rs/token";
        Certificate root = chain[chain.length - 1];
        PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
        DHPublicKey rootAgreement = (DHPublicKey)KeyManager.createPartialAgreement(privateKey, rootPubKey);

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(userId));
        prop.setProperty("ticket", ticket);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("userId", String.valueOf(userId));
        req.addParameter("ticket", ticket);
        req.addParameter("membership", membership);
        req.addParameter("agreement", rootAgreement.getY().toString(16));
        req.addParameter("count", "1");
        req.addParameter("protectionType", request.getProtectionType().ordinal());
        req.addParameter("filePolicy", request.getFilePolicy() == null ? "{}" : GsonUtils.GSON.toJson(request.getFilePolicy()));
        req.addParameter("fileTags", request.getTags() == null ? "{}" : GsonUtils.GSON.toJson(request.getTags()));
        String ret = RestClient.put(path, prop, req.toJson());
        JsonResponse resp = JsonResponse.fromJson(ret);

        if (resp.hasError()) {
            throw new IOException("Failed request new tokens:" + resp.getMessage());
        }

        Map<String, JsonWraper> tokens = resp.getResultAsMap("tokens");
        if (tokens.isEmpty()) {
            throw new IOException("No token returned:" + resp.getMessage());
        }
        int tokenMaintenanceLevel = resp.getResultAsInt("ml", 0);
        Map.Entry<String, JsonWraper> entry = tokens.entrySet().iterator().next();
        JsonObject obj = entry.getValue().getAsJsonTree().getAsJsonObject();
        return new TokenResponse(Hex.toByteArray(entry.getKey()), Hex.toByteArray(obj.get("token").getAsString()), tokenMaintenanceLevel);
    }

    protected void validateRequest(EncryptionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (!(request instanceof RemoteEncryptionRequest)) {
            throw new IllegalArgumentException("Remote request is required");
        }
        if (!StringUtils.hasText(request.getMembership())) {
            throw new IllegalArgumentException("Membership is required");
        }
        File file = request.getOriginalFile();
        if (request.getStreamInfo() == null && (file == null || !file.exists() || !file.isFile())) {
            throw new IllegalArgumentException("Original file/input stream is required");
        }
        OutputStream os = request.getOutputStream();
        if (os == null) {
            throw new IllegalArgumentException("Output stream is required");
        }
        Map tags = request.getTags();
        EncryptUtil.validateTags(tags);
    }

    public static class RemoteEncryptionRequest extends EncryptionRequest {

        private final int userId;
        private final String ticket;
        private final String clientId;
        private final Integer platformId;
        private final String remoteURL;

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, Rights[] rights, String watermark, JsonExpiry expiry, Map<String, String[]> tags,
            OutputStream os) {
            this.userId = userId;
            this.ticket = ticket;
            this.clientId = clientId;
            this.platformId = platformId;
            this.remoteURL = remoteURL;
            setMembership(membership);
            setRights(rights);
            setWatermark(watermark);
            setExpiry(expiry);
            setTags(tags);
            setOutputStream(os);
        }

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, Rights[] rights, String watermark, JsonExpiry expiry, Map<String, String[]> tags,
            OutputStream os, boolean setProtectionType) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, rights, watermark, expiry, tags, os);
            if (setProtectionType) {
                if ((tags == null || tags.isEmpty()) && ArrayUtils.isNotEmpty(rights)) {
                    setProtectionType(ProtectionType.ADHOC);
                    setFilePolicy(new FilePolicy(membership, 0, "Ad-hoc", rights, watermark, expiry));
                } else {
                    setProtectionType(ProtectionType.CENTRAL);
                }
            }
        }

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, Rights[] rights, String watermark, JsonExpiry expiry, Map<String, String[]> tags,
            File originalFile, OutputStream os) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, rights, watermark, expiry, tags, os, true);
            setOriginalFile(originalFile);
        }

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, Rights[] rights, String watermark, JsonExpiry expiry, Map<String, String[]> tags,
            StreamInfo streamInfo, OutputStream os) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, rights, watermark, expiry, tags, os, true);
            setStreamInfo(streamInfo);
        }

        /**
         *  This constructor is used to intialze a remote request for CopyNXL file to destination with new duid
         * @param userId
         * @param ticket
         * @param clientId
         * @param platformId
         * @param remoteURL
         * @param membership
         * @param tenantName
         * @param rights
         * @param watermark
         * @param expiry
         * @param tags
         * @param originalFile
         * @param os
         */
        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, String tenantName, Rights[] rights, String watermark, JsonExpiry expiry,
            Map<String, String[]> tags,
            File originalFile, OutputStream os) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, rights, watermark, expiry, tags, os, true);
            setTenantName(tenantName);
            setOriginalFile(originalFile);
        }

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, Rights[] rights, String watermark, JsonExpiry expiry, Map<String, String[]> tags,
            File originalFile, OutputStream os, int protectionType) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, rights, watermark, expiry, tags, os);
            setOriginalFile(originalFile);
            if (protectionType == ProtectionType.ADHOC.ordinal()) {
                setProtectionType(ProtectionType.ADHOC);
                setFilePolicy(new FilePolicy(membership, 0, "Ad-hoc", rights, watermark, expiry));
            } else if (protectionType == ProtectionType.CENTRAL.ordinal()) {
                setProtectionType(ProtectionType.CENTRAL);
            }
        }

        public RemoteEncryptionRequest(int userId, String ticket, String clientId, Integer platformId, String remoteURL,
            String membership, FilePolicy filePolicy, Map<String, String[]> tags,
            File originalFile, OutputStream os, boolean isAdhoc) {
            this(userId, ticket, clientId, platformId, remoteURL, membership, null, null, null, tags, os);
            setOriginalFile(originalFile);
            if (isAdhoc) {
                setProtectionType(ProtectionType.ADHOC);
                setFilePolicy(filePolicy);
            } else {
                setProtectionType(ProtectionType.CENTRAL);
            }
        }

        public String getRemoteURL() {
            return remoteURL;
        }

        public String getTicket() {
            return ticket;
        }

        public int getUserId() {
            return userId;
        }

        public String getClientId() {
            return clientId;
        }

        public Integer getPlatformId() {
            return platformId;
        }
    }
}
