package com.nextlabs.nxl;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.JsonException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

public final class DecryptUtil {

    private DecryptUtil() {
    }

    public static void decrypt(NxlFile nxl, byte[] token, OutputStream os) throws IOException,
            GeneralSecurityException, NxlException {
        long contentLength = nxl.getContentLength();
        if (contentLength <= 0) {
            throw new IllegalArgumentException("Invalid range");
        }
        try (InputStream is = nxl.getContent(token)) {
            IOUtils.copy(is, os);
        }
    }

    private static void decryptPartial(InputStream fis, NxlFile nxl, byte[] token, OutputStream os, long start,
        long length, boolean skip)
            throws IOException,
            GeneralSecurityException, NxlException {
        long contentLength = nxl.getContentLength();
        if (start < 0 || start >= contentLength || length <= 0 || length > contentLength) {
            throw new IllegalArgumentException("Invalid range");
        }
        try (InputStream is = nxl.getContent(fis, token)) {
            if (start == 0 && length == contentLength) {
                IOUtils.copy(is, os);
            } else {
                long bytes = NxlUtils.getEncBlockStart(start);
                int offset = (int)(start - bytes);
                if (skip) {
                    IOUtils.skip(is, bytes);
                    read(is, os, offset, length);
                } else {
                    DecryptInputStream dis = (DecryptInputStream)is;
                    dis.setIvOffset((start / NxlUtils.BLOCK_SIZE) * NxlUtils.BLOCK_SIZE);
                    read(dis, os, offset, length);
                }
            }
        }
    }

    public static void decrypt(String rmsUrl, int userId, String userTicket, String clientId, Integer platformId,
        String userTenant, NxlFile header, File outputPath)
            throws IOException, NxlException, GeneralSecurityException, JsonException {
        String owner = header.getOwner();
        String duid = header.getDuid();
        BigInteger rootAgreement = header.getRootAgreement();
        int maintenanceLevel = header.getMaintenanceLevel();
        byte[] token = requestToken(rmsUrl, userId, userTicket, clientId, platformId, userTenant, owner, duid, rootAgreement, maintenanceLevel, header.getProtectionType(), DecryptUtil.getFilePolicyStr(header, null), DecryptUtil.getTagsString(header, null));
        if (!header.isValid(token)) {
            throw new NxlException("Nxl is tampered.");
        }
        try (OutputStream fos = new FileOutputStream(outputPath)) {
            decrypt(header, token, fos);
        }
    }

    public static void decrypt(String rmsUrl, int userId, String userTicket, String clientId, Integer platformId,
        String userTenant, File nxlFile, File outputPath)
            throws IOException, NxlException, GeneralSecurityException, JsonException {
        try (InputStream fis = new FileInputStream(nxlFile); NxlFile nxl = NxlFile.parse(fis)) {
            String owner = nxl.getOwner();
            String duid = nxl.getDuid();
            BigInteger rootAgreement = nxl.getRootAgreement();
            int maintenanceLevel = nxl.getMaintenanceLevel();
            byte[] token = requestToken(rmsUrl, userId, userTicket, clientId, platformId, userTenant, owner, duid, rootAgreement, maintenanceLevel, nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
            if (!nxl.isValid(token)) {
                throw new NxlException("Nxl is tampered.");
            }
            try (OutputStream fos = new FileOutputStream(outputPath)) {
                decrypt(nxl, token, fos);
            }
        }
    }

    static void prefetchDuidInCache(Logger logger, String rmsUrl, int userId, String userTicket, String clientId,
        Integer platformId,
        String userTenant, NxlFile nxl) throws JsonException, IOException, GeneralSecurityException, NxlException {
        long startPrefetchTime = System.currentTimeMillis();
        if (!TokenCache.INSTANCE.getCache().asMap().containsKey(nxl.getDuid())) {
            byte[] token = requestToken(rmsUrl, userId, userTicket, clientId, platformId, userTenant, nxl.getOwner(), nxl.getDuid(), nxl.getRootAgreement(), nxl.getMaintenanceLevel(), nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
            if (!nxl.isValid(token)) {
                throw new NxlException("Nxl is tampered.");
            }
            TokenCache.INSTANCE.getCache().put(nxl.getDuid(), token);
            logger.debug("Prefetched token into cache for duid {}", nxl.getDuid());
        }
        long stopPrefetchTime = System.currentTimeMillis();
        logger.debug("Prefetching token took time (ms) {}", (stopPrefetchTime - startPrefetchTime));
    }

    static void decryptPartial(Logger logger, String rmsUrl, int userId, String userTicket, String clientId,
        Integer platformId,
        String userTenant,
        InputStream fis, OutputStream fos, NxlFile nxl, long start, long length)
            throws IOException, NxlException, GeneralSecurityException, JsonException {
        long startDecryptTime = System.currentTimeMillis();
        boolean skip = false;
        if (nxl == null) {
            nxl = NxlFile.parse(fis);
            skip = true;
        }
        String owner = nxl.getOwner();
        String duid = nxl.getDuid();
        BigInteger rootAgreement = nxl.getRootAgreement();
        int maintenanceLevel = nxl.getMaintenanceLevel();
        byte[] token = null;
        boolean isTokenCachingEnabled = RightsManager.isTokenCachingEnabled();
        if (isTokenCachingEnabled) {
            token = TokenCache.INSTANCE.getCache().getIfPresent(duid);
        }
        if (token == null) {
            token = requestToken(rmsUrl, userId, userTicket, clientId, platformId, userTenant, owner, duid, rootAgreement, maintenanceLevel, nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
            if (!nxl.isValid(token)) {
                throw new NxlException("Nxl is tampered.");
            }
            if (isTokenCachingEnabled) {
                TokenCache.INSTANCE.getCache().put(duid, token);
                logger.debug("Decrypt token requested into cache for duid {}", nxl.getDuid());
            }
        }
        decryptPartial(fis, nxl, token, fos, start, length, skip);
        long stopDecryptTime = System.currentTimeMillis();
        logger.debug("Decrypt took time (ms) {}", (stopDecryptTime - startDecryptTime));
    }

    public static boolean flushTokenFromCache(String duid) {
        if (RightsManager.isTokenCachingEnabled()) {
            TokenCache.INSTANCE.getCache().invalidate(duid);
            return true;
        }
        return false;
    }

    private static long read(InputStream is, OutputStream os, int offset, long length) throws IOException {
        long total = length + offset;
        boolean firstBlock = true;
        long count = 0;
        while (total >= NxlUtils.BLOCK_SIZE) {
            byte[] buffer = new byte[NxlUtils.BLOCK_SIZE];
            count += is.read(buffer);
            if (firstBlock) {
                byte[] partialBlock = new byte[NxlUtils.BLOCK_SIZE - offset];
                System.arraycopy(buffer, offset, partialBlock, 0, partialBlock.length);
                os.write(partialBlock);
                firstBlock = false;
            } else {
                os.write(buffer);
            }
            total -= NxlUtils.BLOCK_SIZE;
        }

        if (total > 0) {
            byte[] buffer = new byte[NxlUtils.BLOCK_SIZE];
            count += is.read(buffer);
            if (firstBlock) {
                total = length;
            } else {
                offset = 0;
            }
            byte[] partialBlock = new byte[(int)total];
            System.arraycopy(buffer, offset, partialBlock, 0, partialBlock.length);
            os.write(partialBlock);
        }
        return count;
    }

    public static byte[] requestToken(String rmsUrl, int userId, String userTicket, String clientId, Integer platformId,
        String userTenant, String ownerMembership, String duid, BigInteger agreement, int tokenMaintenanceLevel,
        ProtectionType protectionType, String filePolicy, String fileTags)
            throws JsonException, IOException {
        String path = rmsUrl + "/rs/token";
        platformId = platformId != null ? platformId : DeviceType.WEB.getLow();

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(userId));
        prop.setProperty("ticket", userTicket);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("userId", String.valueOf(userId));
        req.addParameter("ticket", userTicket);
        req.addParameter("tenant", userTenant);
        req.addParameter("owner", ownerMembership);
        req.addParameter("agreement", agreement.toString(16));
        req.addParameter("ml", String.valueOf(tokenMaintenanceLevel));
        req.addParameter("duid", duid);
        req.addParameter("protectionType", protectionType.ordinal());
        req.addParameter("filePolicy", filePolicy == null ? "{}" : filePolicy);
        req.addParameter("fileTags", fileTags);
        String ret = RestClient.post(path, prop, req.toJson());

        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }

        String token = resp.getResultAsString("token");
        return Hex.toByteArray(token);
    }

    public static byte[] requestToken(String rmsUrl, int userId, String userTicket, String clientId, Integer platformId,
        String userTenant, String ownerMembership, String duid, BigInteger agreement, int tokenMaintenanceLevel,
        ProtectionType protectionType, String filePolicy, String fileTags, String sharedSpaceType,
        String sharedSpaceId, String sharedSpaceUserMembership)
            throws JsonException, IOException {
        String path = rmsUrl + "/rs/token";
        platformId = platformId != null ? platformId : DeviceType.WEB.getLow();

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(userId));
        prop.setProperty("ticket", userTicket);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("userId", String.valueOf(userId));
        req.addParameter("ticket", userTicket);
        req.addParameter("tenant", userTenant);
        req.addParameter("owner", ownerMembership);
        req.addParameter("agreement", agreement.toString(16));
        req.addParameter("ml", String.valueOf(tokenMaintenanceLevel));
        req.addParameter("duid", duid);
        req.addParameter("protectionType", protectionType.ordinal());
        req.addParameter("filePolicy", filePolicy == null ? "{}" : filePolicy);
        req.addParameter("fileTags", fileTags);
        req.addParameter("sharedSpaceType", sharedSpaceType);
        req.addParameter("sharedSpaceId", sharedSpaceId);
        req.addParameter("sharedSpaceUserMembership", sharedSpaceUserMembership);
        String ret = RestClient.post(path, prop, req.toJson());

        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }

        String token = resp.getResultAsString("token");
        return Hex.toByteArray(token);
    }

    public static FileInfo getInfo(NxlFile nxl, byte[] token) throws GeneralSecurityException,
            IOException, NxlException {
        Section infoSection = nxl.getSection(NxlFile.SECTION_FILE_INFO);
        if (infoSection != null) {
            byte[] iv = nxl.getIv();
            String fileInfoSection = StringUtils.toStringQuietly(infoSection.getDecodedData(token, iv));
            return GsonUtils.GSON.fromJson(fileInfoSection, FileInfo.class);
        }
        return null;
    }

    public static Map<String, String[]> getTags(NxlFile nxl, byte[] token) throws GeneralSecurityException,
            IOException, NxlException {
        Section tagSection = nxl.getSection(NxlFile.SECTION_FILE_TAGS);
        if (tagSection != null) {
            byte[] iv = nxl.getIv();
            String tagMetadata = StringUtils.toStringQuietly(tagSection.getDecodedData(token, iv));
            return GsonUtils.GSON.fromJson(tagMetadata, GsonUtils.STRING_ARRAY_MAP_TYPE);
        }
        return Collections.<String, String[]> emptyMap();
    }

    public static String getTagsString(NxlFile nxl, byte[] token)
            throws GeneralSecurityException, IOException, NxlException {
        Section tagSection = nxl.getSection(NxlFile.SECTION_FILE_TAGS);
        if (tagSection != null) {
            byte[] iv = nxl.getIv();
            return StringUtils.toStringQuietly(tagSection.getDecodedData(token, iv));
        }
        return "{}";
    }

    public static FilePolicy getFilePolicy(NxlFile nxl, byte[] token)
            throws GeneralSecurityException, IOException, NxlException {
        String policyStr = getFilePolicyStr(nxl, token);
        if (StringUtils.hasText(policyStr)) {
            return GsonUtils.GSON.fromJson(policyStr, FilePolicy.class);
        }
        return null;
    }

    public static String getFilePolicyStr(NxlFile nxl, byte[] token)
            throws GeneralSecurityException, IOException, NxlException {
        Section policy = nxl.getSection(NxlFile.SECTION_FILE_POLICY);
        if (policy != null) {
            byte[] iv = nxl.getIv();
            return StringUtils.toStringQuietly(policy.getDecodedData(token, iv));
        }
        return null;
    }

    public static class TokenResponse {

        private byte[] duid;
        private byte[] token;
        private int maintenanceLevel;

        public byte[] getDuid() {
            return duid;
        }

        public byte[] getToken() {
            return token;
        }

        public int getMaintenanceLevel() {
            return maintenanceLevel;
        }

        public TokenResponse(byte[] duid, byte[] token, int maintenanceLevel) {
            this.duid = duid;
            this.token = token;
            this.maintenanceLevel = maintenanceLevel;
        }
    }
}
