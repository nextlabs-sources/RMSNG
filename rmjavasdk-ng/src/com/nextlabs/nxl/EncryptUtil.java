package com.nextlabs.nxl;

import com.google.gson.JsonParseException;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.CryptoBaseRequest.EncryptionRequest;
import com.nextlabs.nxl.DecryptUtil.TokenResponse;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.crypto.interfaces.DHPublicKey;

public final class EncryptUtil {

    private final ICrypto delegate;

    private EncryptUtil(ICrypto delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException(ICrypto.class.getName() + " is required");
        }
        this.delegate = delegate;
    }

    public static EncryptUtil create(ICrypto crypto) {
        return new EncryptUtil(crypto);
    }

    public NxlFile encrypt(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException {
        return delegate.encrypt(request);
    }

    /***
     * This method copies a nxl from source to destination with new DUID
     * @param request this is the encryption request carries all details for performing the operation
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws NxlException
     * @throws TokenGroupNotFoundException
     * @throws JsonException
     */
    public NxlFile refreshNxl(EncryptionRequest request)
            throws IOException, GeneralSecurityException, NxlException, TokenGroupNotFoundException, JsonException {
        return delegate.refreshNxl(request);
    }

    /***
     * This method gets the root and ICA agreement used in request new token. These agreements are later updated in the NXL header
     * @param keyPair
     * @param chain
     * @return
     * @throws GeneralSecurityException
     */
    private static BigInteger[] getAgreements(KeyPair keyPair, Certificate[] chain) throws GeneralSecurityException {
        PrivateKey privateKey = keyPair.getPrivate();
        Certificate root = chain[chain.length - 1];
        PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
        BigInteger[] agreements = new BigInteger[2];
        DHPublicKey agreementKey = (DHPublicKey)KeyManager.createPartialAgreement(privateKey, rootPubKey);
        agreements[0] = agreementKey.getY();
        if (chain.length >= 3) {
            Certificate icaCert = chain[chain.length - 2];
            DHPublicKey icaPubKey = (DHPublicKey)KeyUtils.readPublicKey(icaCert.getPublicKey().getEncoded(), "DH");
            agreementKey = (DHPublicKey)KeyManager.createPartialAgreement(privateKey, icaPubKey);
            agreements[1] = agreementKey.getY();
        }
        return agreements;
    }

    public static NxlFile doEncrypt(EncryptionRequest request, KeyPair keyPair, Certificate[] chain,
        TokenResponse tokenResponse) throws GeneralSecurityException, IOException, NxlException {

        PrivateKey privateKey = keyPair.getPrivate();
        Certificate root = chain[chain.length - 1];
        PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
        BigInteger[] agreements = new BigInteger[2];
        DHPublicKey agreementKey = (DHPublicKey)KeyManager.createPartialAgreement(privateKey, rootPubKey);
        agreements[0] = agreementKey.getY();
        if (chain.length >= 3) {
            Certificate icaCert = chain[chain.length - 2];
            DHPublicKey icaPubKey = (DHPublicKey)KeyUtils.readPublicKey(icaCert.getPublicKey().getEncoded(), "DH");
            agreementKey = (DHPublicKey)KeyManager.createPartialAgreement(privateKey, icaPubKey);
            agreements[1] = agreementKey.getY();
        }

        if (request.getOriginalFile() != null) {
            try (InputStream is = new FileInputStream(request.getOriginalFile())) {
                return buildNxl(request, tokenResponse, agreements, is, request.getOriginalFile().length(), new FileInfo(request.getOriginalFile(), request.getMembership()));
            }
        } else if (request.getStreamInfo() != null) {
            return buildNxl(request, tokenResponse, agreements, request.getStreamInfo().getInputStream(), request.getStreamInfo().getContentLength(), new FileInfo(request.getStreamInfo().getFileName(), request.getMembership()));
        } else {
            throw new NxlException("No file or input stream is provided for encryption.");
        }
    }

    /***
     * This method helps to copy a nxl from source to destination with new DUID
     * @param request  the encryption request with all source and detination detials
     * @param oldtokenResponse token details of source nxl file
     * @param newtokenResponse new token details for the destination copy
     * @param keyPair key pair used to request new tokens
     * @param chain certificate chain used for request new tokens
     * @return
     * @throws GeneralSecurityException
     * @throws NxlException
     * @throws IOException
     */
    public static NxlFile doRefreshNXL(EncryptionRequest request, TokenResponse oldtokenResponse,
        TokenResponse newtokenResponse, KeyPair keyPair, Certificate[] chain)
            throws GeneralSecurityException, NxlException, IOException {
        OutputStream os = request.getOutputStream();
        NxlFile nxlFile = NxlFile.parse(new FileInputStream(request.getOriginalFile()));

        FileInfo fileInfo = getFileInfo(request);
        List<String> policyAndTags = getPolicyAndTags(request);
        BigInteger[] agreements = getAgreements(keyPair, chain);

        NxlFile.Builder builder = getNxlBuilder(newtokenResponse.getDuid(), fileInfo, agreements, policyAndTags, request.getMembership());
        builder.setContent(nxlFile.getContent(), nxlFile.getContentLength());

        NxlFile newCopy = builder.refreshHeader(oldtokenResponse, newtokenResponse, nxlFile);
        newCopy.writeHeader(os, newtokenResponse.getToken(), false);
        newCopy.writeSectionData(os);
        newCopy.writeContent(os);
        return newCopy;

    }

    /**
     * Creates a new NXLbuilder with parameters duid, fileinfo, owner, agreements, policy and tags
     * @param duid  duid of a nxl file
     * @param fileInfo fileinfo of nxl file
     * @param agreements root and ica agreements for a nxl token on the file
     * @param policyAndTags policy and tags to be added to nxl header
     * @param owner
     * @return
     */
    private static NxlFile.Builder getNxlBuilder(byte[] duid, FileInfo fileInfo, BigInteger[] agreements,
        List<String> policyAndTags, String owner) {
        NxlFile.Builder builder = new NxlFile.Builder();
        builder.setDuid(duid);
        builder.setFileInfo(GsonUtils.GSON.toJson(fileInfo));
        builder.setOwner(owner);
        builder.setRootAgreement(agreements[0]);
        if (agreements[1] != null) {
            builder.setIcaAgreement(agreements[1]);
        }
        builder.setFilePolicy(policyAndTags.get(0));
        builder.setFileTags(policyAndTags.get(1));
        return builder;
    }

    /**
     * This method creates a list of string containing policy at position 0 and tags at position 1
     * @param request
     * @return
     */
    private static List<String> getPolicyAndTags(EncryptionRequest request) {
        String policy = (request.getProtectionType() == ProtectionType.ADHOC) ? GsonUtils.GSON.toJson(request.getFilePolicy()) : "{}";
        String fileTags = "{}";
        if (ProtectionType.CENTRAL == request.getProtectionType() && request.getTags() != null) {
            fileTags = GsonUtils.GSON.toJson(request.getTags());
        }
        List<String> policyAndTags = new ArrayList<String>();
        policyAndTags.add(policy);
        policyAndTags.add(fileTags);
        return policyAndTags;
    }

    /**
     * Generates a file info from the encryption request
     * @param request
     * @return
     */
    private static FileInfo getFileInfo(EncryptionRequest request) {
        FileInfo fileInfo = null;
        if (request.getFileName() != null) {
            fileInfo = new FileInfo(request.getFileName(), request.getMembership());
        } else {
            fileInfo = new FileInfo(request.getOriginalFile(), request.getMembership());
        }
        return fileInfo;
    }

    private static NxlFile buildNxl(EncryptionRequest request, TokenResponse tokenResponse, BigInteger[] agreements,
        InputStream is, long contentLength, FileInfo info) throws GeneralSecurityException, IOException, NxlException {

        List<String> policyAndTags = getPolicyAndTags(request);
        OutputStream os = request.getOutputStream();

        NxlFile.Builder builder = getNxlBuilder(tokenResponse.getDuid(), info, agreements, policyAndTags, request.getMembership());
        builder.setContent(is, contentLength);
        byte[] tokenBuf = tokenResponse.getToken();

        NxlFile nxl = builder.build(tokenBuf, tokenResponse.getMaintenanceLevel());
        nxl.writeHeader(os, tokenBuf, false);
        nxl.writeSectionData(os);
        nxl.writeContent(os, tokenBuf);
        return nxl;
    }

    public static void updateTags(NxlFile nxl, byte[] token, Map<String, String[]> tags, OutputStream os)
            throws GeneralSecurityException, IOException, NxlException {
        String fileTags = "{}";
        if (tags != null) {
            for (String[] tagArr : tags.values()) {
                for (String tag : tagArr) {
                    validateTag(tag);
                }
            }
            fileTags = GsonUtils.GSON.toJson(tags);
        }
        updateTags(nxl, token, fileTags, os);
    }

    public static void updateTags(NxlFile nxl, byte[] token, String fileTags, OutputStream os)
            throws GeneralSecurityException, IOException, NxlException {
        nxl.updateSection(2, NxlFile.SECTION_FILE_TAGS, fileTags, token, false);
        nxl.writeHeader(os, token, false);
        nxl.writeSectionData(os);
    }

    public static void updateTags(NxlFile nxl, String fileTags, byte[] tagChecksum, byte[] basicHeaderChecksum,
        OutputStream os) throws GeneralSecurityException, IOException, NxlException {
        nxl.updateSection(2, NxlFile.SECTION_FILE_TAGS, fileTags, tagChecksum, true);
        nxl.writeHeader(os, basicHeaderChecksum, true);
        nxl.writeSectionData(os);
    }

    public static void updateFileInfo(NxlFile nxl, byte[] token, String fileInfo, OutputStream os)
            throws GeneralSecurityException, IOException, NxlException {
        nxl.updateSection(0, NxlFile.SECTION_FILE_INFO, fileInfo, token, false);
        nxl.writeHeader(os, token, false);
        nxl.writeSectionData(os);
    }

    public static void updateFileInfo(NxlFile nxl, String updatedFileInfo, byte[] fileinfoChecksum,
        byte[] basicHeaderChecksum, OutputStream os) throws GeneralSecurityException, IOException, NxlException {
        nxl.updateSection(0, NxlFile.SECTION_FILE_INFO, updatedFileInfo, fileinfoChecksum, true);
        nxl.writeHeader(os, basicHeaderChecksum, true);
        nxl.writeSectionData(os);
    }

    public static void validateTag(String tag) {
        String pattern = "^[^%'\"]+$";
        if (tag != null && !tag.isEmpty()) {
            if (!tag.matches(pattern)) {
                throw new IllegalArgumentException("The TAG cannot contain quotes or percentage characters");
            }
            if (tag.length() > 60) {
                throw new IllegalArgumentException("The TAG cannot exceed maximum 60 characters");
            }
        }
    }

    public static boolean validateTags(String fileTags) {
        boolean validSyntax = false;
        try {
            if (!StringUtils.hasText(fileTags)) {
                return validSyntax;
            }
            Map tags = GsonUtils.GSON.fromJson(fileTags, GsonUtils.STRING_ARRAY_MAP_TYPE);
            EncryptUtil.validateTags(tags);
            validSyntax = true;
        } catch (JsonParseException | IllegalArgumentException e) {
            validSyntax = false;
        }
        return validSyntax;
    }

    public static boolean validFilePolicy(String policy) {
        boolean validPolicy = false;
        try {
            if (!StringUtils.hasText(policy)) {
                return validPolicy;
            }
            GsonUtils.GSON.fromJson(policy, FilePolicy.class);
            validPolicy = true;
        } catch (JsonParseException e) {
            validPolicy = false;
        }
        return validPolicy;
    }

    public static void validateTags(Map tags) {
        if (tags != null && !tags.isEmpty()) {
            for (Object tag : tags.values()) {
                if (tag == null) {
                    throw new IllegalArgumentException("The TAG cannot be null");
                }
                if (tag.getClass().isArray()) {
                    for (String str : (String[])tag) {
                        EncryptUtil.validateTag(str);
                    }
                } else {
                    for (String str : (Collection<String>)tag) {
                        EncryptUtil.validateTag(str);
                    }
                }
            }
        }
    }
}
