package com.nextlabs.rms.viewer.eval;

import com.google.common.collect.Lists;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Constants.SHARESPACE;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalRequest;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.viewer.client.Client;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.FileTypeDetector;
import com.nextlabs.rms.viewer.conversion.WaterMark;
import com.nextlabs.rms.viewer.exception.NotAuthorizedException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.json.SharedFile;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.ViewerUtil;
import com.nextlabs.rms.viewer.util.WatermarkUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

public class EvaluationHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public static String getFileNameWithoutNXL(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return fileName;
        }
        if (!FileUtils.getRealFileExtension(fileName).equals(Constants.NXL_FILE_EXTN)) {
            return fileName;
        }
        int index = fileName.toLowerCase().lastIndexOf(Constants.NXL_FILE_EXTN);
        return fileName.substring(0, index);
    }

    public static void writeContentsToFile(String targetDirPath, String fileName, byte[] fileContent)
            throws IOException {
        try (FileOutputStream ostr = new FileOutputStream(new File(targetDirPath, fileName))) {
            try (ByteArrayInputStream is = new ByteArrayInputStream(fileContent)) {
                IOUtils.copy(is, ostr);
            }
        }
    }

    private byte[] obtainToken(String rmsURL, String userId, String ticket, String clientId, Integer platformId,
        String tenantName, BigInteger agreement, String duid, int maintenanceLevel, String owner, NxlFile nxl)
            throws IOException, NotAuthorizedException, RMSException, NumberFormatException, GeneralSecurityException,
            NxlException {
        try {
            return DecryptUtil.requestToken(rmsURL, Integer.parseInt(userId), ticket, clientId, platformId, tenantName, owner, duid, agreement, maintenanceLevel, nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null));
        } catch (JsonException e) {
            if (e.getStatusCode() == 403) {
                NotAuthorizedException nae = new NotAuthorizedException(e.getMessage(), duid, owner);
                nae.initCause(e);
                throw nae;
            } else if (e.getStatusCode() == 404) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Unable to find membership (name: {}, requested user: {}) for given DUID: {}", owner, userId, duid);
                }
                RMSException re = new RMSException(ViewerMessageHandler.getClientString("err.unauthorized"));
                re.initCause(e);
                throw re;
            }
            RMSException re = new RMSException(e.getMessage());
            re.initCause(e);
            throw re;
        }
    }

    public CachedFile evaluateAndDecrypt(File fileToEvaluate, User user, int offset) throws RMSException, IOException,
            NxlException, NotAuthorizedException, GeneralSecurityException {

        String userId = user.getId();
        String userName = user.getEmail();
        String ticket = user.getTicket();
        String clientId = user.getClientId();
        String tenantName = user.getTenantName();
        Integer platformId = user.getPlatformId();

        byte[] fileContent = null;
        NxlFile nxl = null;

        if (fileToEvaluate.isDirectory()) {
            throw new RMSException("Invalid File format");
        }
        String fileNameWithoutNXL = getFileNameWithoutNXL(fileToEvaluate.getName());
        boolean isNXLFile = false;
        try (InputStream fis = new FileInputStream(fileToEvaluate)) {
            final int length = NxlFile.BASIC_HEADER_SIZE;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(fis, os, 0, length, length);
            byte[] nxlHeader = os.toByteArray();
            isNXLFile = NxlFile.isNxl(nxlHeader);
            if (!isNXLFile && fileToEvaluate.getName().toLowerCase().endsWith(Constants.NXL_FILE_EXTN)) {
                throw new NxlException("Invalid nxl file");
            }
        }

        boolean isOwner = false;
        Rights[] rights = new Rights[] {};
        Map<String, String[]> tags = Collections.<String, String[]> emptyMap();
        String duid = "";
        String membership = "";
        String watermarkText = "";
        String rmsUrl = null;
        String lookupTokenGroupName = null;
        boolean hasWatermarkRights = false;
        JsonExpiry validity = new JsonExpiry();
        final int bufferSize = (int)(fileToEvaluate.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : fileToEvaluate.length());
        boolean isCentralPolicy = false;
        if (isNXLFile) {
            try (InputStream is = new FileInputStream(fileToEvaluate)) {
                nxl = NxlFile.parse(is);
                duid = nxl.getDuid();
                membership = nxl.getOwner();
                lookupTokenGroupName = StringUtils.substringAfter(membership, "@");
                rmsUrl = ViewerUtil.getRMSInternalURL(lookupTokenGroupName);
                isOwner = isOwner(user, membership, rmsUrl);
                BigInteger rootAgreement = nxl.getRootAgreement();
                int maintenanceLevel = nxl.getMaintenanceLevel();
                byte[] token = obtainToken(rmsUrl, userId, ticket, clientId, platformId, tenantName, rootAgreement, duid, maintenanceLevel, membership, nxl);
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FilePolicy policy = DecryptUtil.getFilePolicy(nxl, token);
                List<Policy> adhocPolicies = policy.getPolicies();
                List<EvalResponse> evalResponses = Lists.newArrayList();
                tags = DecryptUtil.getTags(nxl, token);
                if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                    EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, isOwner);
                    validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                    rights = evalResponse.getRights();
                    evalResponses.add(evalResponse);
                } else if (StringUtils.hasText(ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.ICENET_URL))) {
                    isCentralPolicy = true;
                    String parentTenant = Client.getParentTenantName(lookupTokenGroupName, user);
                    evalResponses = CentralPoliciesEvaluationHandler.evaluate(fileToEvaluate, user, membership, parentTenant, true, EvalRequest.ATTRIBVAL_VIEWER_APP_NAME);
                    rights = PolicyEvalUtil.getUnionRights(evalResponses);
                }
                boolean isAuthorized = false;
                for (Rights r : rights) {
                    if (r == Rights.VIEW) {
                        isAuthorized = true;
                    } else if (r == Rights.WATERMARK) {
                        hasWatermarkRights = true;
                    }
                }
                if (!isAuthorized) {
                    throw new NotAuthorizedException("Not authorized", duid, membership);
                }
                watermarkText = getWatermarkText(evalResponses);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize)) {
                    DecryptUtil.decrypt(nxl, token, baos);
                    fileContent = baos.toByteArray();
                }
            } catch (JsonException e) {
                int statusCode = e.getStatusCode();
                if (statusCode == 401) {
                    NotAuthorizedException ex = new NotAuthorizedException("Not authorized", duid, membership);
                    ex.initCause(e);
                    throw ex;
                }
                RMSException ex = new RMSException(e.getMessage());
                ex.initCause(e);
                throw ex;
            } finally {
                IOUtils.closeQuietly(nxl);
            }
        } else {
            lookupTokenGroupName = tenantName;
            rmsUrl = ViewerUtil.getRMSInternalURL(lookupTokenGroupName);
            isOwner = true;
            rights = Rights.values();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            InputStream is = null;
            try {
                is = new FileInputStream(fileToEvaluate);
                IOUtils.copy(is, baos);
                fileContent = baos.toByteArray();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(baos);
            }
        }
        if (ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.WRITE_DECRYPTED_FILE_TO_DISK)) {
            writeContentsToFile(fileToEvaluate.getParent(), fileNameWithoutNXL, fileContent);
        }
        JsonWatermark watermarkConfig = WatermarkConfigManager.getWaterMarkConfig(rmsUrl, lookupTokenGroupName, ticket, WatermarkConfigManager.DEFAULT_PLATFORM_ID, userId, clientId);
        if (watermarkConfig == null) {
            throw new RMSException("Invalid Watermark format");
        }
        WaterMark waterMark = WatermarkUtil.build(null, watermarkText, watermarkConfig);
        if ((isCentralPolicy && watermarkText == null) || (!isCentralPolicy && (isOwner || !hasWatermarkRights))) {
            waterMark.setWaterMarkStr("");
        } else {
            WatermarkUtil.updateWaterMark(waterMark, userName, offset);
        }

        CachedFile cachedFile = new CachedFile(fileContent, fileNameWithoutNXL, fileToEvaluate.getName(), waterMark, duid, membership, Rights.toInt(rights), tags, user, (isCentralPolicy ? ProtectionType.CENTRAL.ordinal() : ProtectionType.ADHOC.ordinal()));
        cachedFile.setFileSize(fileToEvaluate.length());
        cachedFile.setOwner(isOwner);
        cachedFile.setValidity(validity);

        MediaType type = FileTypeDetector.getMimeType(fileContent, fileNameWithoutNXL);
        cachedFile.setMediaType(type);
        LOGGER.debug("Detected Mime type for {} is: {}", fileNameWithoutNXL, type);
        return cachedFile;
    }

    private String getWatermarkText(List<EvalResponse> evalResponses) {
        for (EvalResponse response : evalResponses) {
            if (response.getRights().length > 0) {
                String watermark = response.getEffectiveWatermark();
                if (StringUtils.hasText(watermark)) {
                    return watermark;
                }
            }
        }
        return null;
    }

    public CachedFile evaluateAndDecryptSharedProjectFile(File fileToEvaluate, User user, int offset,
        SHARESPACE fromShareSpace, String spaceId, SharedFile sharedFile)
            throws RMSException, IOException,
            NxlException, NotAuthorizedException, GeneralSecurityException {

        String userId = user.getId();
        String userName = user.getEmail();
        String ticket = user.getTicket();
        String clientId = user.getClientId();
        String tenantName = user.getTenantName();
        Integer platformId = user.getPlatformId();

        byte[] fileContent = null;
        NxlFile nxl = null;

        if (fileToEvaluate.isDirectory()) {
            throw new RMSException("Invalid File format");
        }
        String fileNameWithoutNXL = getFileNameWithoutNXL(fileToEvaluate.getName());
        boolean isNXLFile = false;
        try (InputStream fis = new FileInputStream(fileToEvaluate)) {
            final int length = NxlFile.BASIC_HEADER_SIZE;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(fis, os, 0, length, length);
            byte[] nxlHeader = os.toByteArray();
            isNXLFile = NxlFile.isNxl(nxlHeader);
            if (!isNXLFile && fileToEvaluate.getName().toLowerCase().endsWith(Constants.NXL_FILE_EXTN)) {
                throw new NxlException("Invalid nxl file");
            }
        }

        boolean isOwner = false;
        Rights[] rights;
        Map<String, String[]> tags = Collections.<String, String[]> emptyMap();
        String duid = "";
        String membership = "";
        String watermarkText = "";
        String rmsUrl = null;
        String lookupTokenGroupName = null;
        boolean hasWatermarkRights = false;
        JsonExpiry validity = new JsonExpiry();
        final int bufferSize = (int)(fileToEvaluate.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : fileToEvaluate.length());
        boolean isCentralPolicy = false;
        if (isNXLFile) {
            try (InputStream is = new FileInputStream(fileToEvaluate)) {
                nxl = NxlFile.parse(is);
                duid = nxl.getDuid();
                membership = nxl.getOwner();
                lookupTokenGroupName = StringUtils.substringAfter(membership, "@");
                rmsUrl = ViewerUtil.getRMSInternalURL(lookupTokenGroupName);
                isOwner = isOwner(user, membership, rmsUrl);
                BigInteger rootAgreement = nxl.getRootAgreement();
                int maintenanceLevel = nxl.getMaintenanceLevel();
                String sharedSpaceUserMembership = getSharedSpaceUserMembership(rmsUrl, user, Integer.valueOf(spaceId), fromShareSpace);
                byte[] token = obtainTokenForSharedFile(rmsUrl, userId, ticket, clientId, platformId, tenantName, rootAgreement, duid, maintenanceLevel, membership, nxl, fromShareSpace, spaceId, sharedSpaceUserMembership);
                if (!nxl.isValid(token)) {
                    throw new NxlException("Invalid token.");
                }
                FilePolicy policy = DecryptUtil.getFilePolicy(nxl, token);
                List<Policy> adhocPolicies = policy.getPolicies();
                EvalResponse evalResponse = new EvalResponse();
                tags = DecryptUtil.getTags(nxl, token);
                if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                    evalResponse = AdhocEvalAdapter.evaluate(policy, isOwner);
                    validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                } else if (StringUtils.hasText(ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.ICENET_URL))) {
                    isCentralPolicy = true;
                    evalResponse = getRightsForSharedWithMeFiles(rmsUrl, user, Integer.valueOf(spaceId), fromShareSpace, sharedFile);
                }
                rights = evalResponse.getRights();
                boolean isAuthorized = false;
                for (Rights r : rights) {
                    if (r == Rights.VIEW) {
                        isAuthorized = true;
                    } else if (r == Rights.WATERMARK) {
                        hasWatermarkRights = true;
                    }
                }
                if (!isAuthorized) {
                    throw new NotAuthorizedException("Not authorized", duid, membership);
                }
                watermarkText = evalResponse.getEffectiveWatermark();
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize)) {
                    DecryptUtil.decrypt(nxl, token, baos);
                    fileContent = baos.toByteArray();
                }
            } catch (JsonException e) {
                int statusCode = e.getStatusCode();
                if (statusCode == 401) {
                    NotAuthorizedException ex = new NotAuthorizedException("Not authorized", duid, membership);
                    ex.initCause(e);
                    throw ex;
                }
                RMSException ex = new RMSException(e.getMessage());
                ex.initCause(e);
                throw ex;
            } finally {
                IOUtils.closeQuietly(nxl);
            }
        } else {
            lookupTokenGroupName = tenantName;
            rmsUrl = ViewerUtil.getRMSInternalURL(lookupTokenGroupName);
            isOwner = true;
            rights = Rights.values();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            InputStream is = null;
            try {
                is = new FileInputStream(fileToEvaluate);
                IOUtils.copy(is, baos);
                fileContent = baos.toByteArray();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(baos);
            }
        }
        if (ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.WRITE_DECRYPTED_FILE_TO_DISK)) {
            writeContentsToFile(fileToEvaluate.getParent(), fileNameWithoutNXL, fileContent);
        }
        JsonWatermark watermarkConfig = WatermarkConfigManager.getWaterMarkConfig(rmsUrl, lookupTokenGroupName, ticket, WatermarkConfigManager.DEFAULT_PLATFORM_ID, userId, clientId);
        if (watermarkConfig == null) {
            throw new RMSException("Invalid Watermark format");
        }
        WaterMark waterMark = WatermarkUtil.build(null, watermarkText, watermarkConfig);
        if ((isCentralPolicy && watermarkText == null) || (!isCentralPolicy && (isOwner || !hasWatermarkRights))) {
            waterMark.setWaterMarkStr("");
        } else {
            WatermarkUtil.updateWaterMark(waterMark, userName, offset);
        }

        CachedFile cachedFile = new CachedFile(fileContent, fileNameWithoutNXL, fileToEvaluate.getName(), waterMark, duid, membership, Rights.toInt(rights), tags, user, (isCentralPolicy ? ProtectionType.CENTRAL.ordinal() : ProtectionType.ADHOC.ordinal()));
        cachedFile.setFileSize(fileToEvaluate.length());
        cachedFile.setOwner(isOwner);
        cachedFile.setValidity(validity);

        MediaType type = FileTypeDetector.getMimeType(fileContent, fileNameWithoutNXL);
        cachedFile.setMediaType(type);
        LOGGER.debug("Detected Mime type for {} is: {}", fileNameWithoutNXL, type);
        return cachedFile;
    }

    private EvalResponse getRightsForSharedWithMeFiles(String path, User user, Integer spaceId,
        SHARESPACE fromShareSpace, SharedFile sharedFile) throws JsonException, IOException {
        Properties prop = new Properties();
        prop.setProperty("userId", user.getId());
        prop.setProperty("ticket", user.getTicket());
        String clientId = user.getClientId();
        Integer platformId = user.getPlatformId();
        String deviceId = user.getDeviceId();
        if (StringUtils.hasText(deviceId)) {
            prop.setProperty("deviceId", deviceId);
        }
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null) {
            prop.setProperty("platformId", platformId.toString());
        }
        StringBuffer url = new StringBuffer(180);
        url.append(path).append("/rs/sharedWithMe/metadata/").append(sharedFile.getTransactionId()).append('/').append(sharedFile.getTransactionCode());
        if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
            url.append("?spaceId=").append(spaceId);
        }

        String respString = RestClient.get(url.toString(), prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        JsonSharedWithMeFile sharedWithMeFileMetadata = resp.getResult("detail", JsonSharedWithMeFile.class);
        if (sharedWithMeFileMetadata != null && sharedWithMeFileMetadata.getRights() != null) {
            String[] rightsAsString = sharedWithMeFileMetadata.getRights();
            Rights[] rights = Rights.fromStrings(rightsAsString);
            return new EvalResponse(rights);
        }
        return null;
    }

    private byte[] obtainTokenForSharedFile(String rmsURL, String userId, String ticket, String clientId,
        Integer platformId,
        String tenantName, BigInteger agreement, String duid, int maintenanceLevel, String owner, NxlFile nxl,
        SHARESPACE fromShareSpace, String spaceId, String sharedSpaceUserMembership)
            throws IOException, NotAuthorizedException, RMSException, NumberFormatException, GeneralSecurityException,
            NxlException {
        try {
            return DecryptUtil.requestToken(rmsURL, Integer.parseInt(userId), ticket, clientId, platformId, tenantName, owner, duid, agreement, maintenanceLevel, nxl.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl, null), DecryptUtil.getTagsString(nxl, null), Integer.toString(fromShareSpace.ordinal()), spaceId, sharedSpaceUserMembership);
        } catch (JsonException e) {
            if (e.getStatusCode() == 403) {
                NotAuthorizedException nae = new NotAuthorizedException(e.getMessage(), duid, owner);
                nae.initCause(e);
                throw nae;
            } else if (e.getStatusCode() == 404) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Unable to find membership (name: {}, requested user: {}) for given DUID: {}", owner, userId, duid);
                }
                RMSException re = new RMSException(ViewerMessageHandler.getClientString("err.unauthorized"));
                re.initCause(e);
                throw re;
            }
            RMSException re = new RMSException(e.getMessage());
            re.initCause(e);
            throw re;
        }
    }

    private String getSharedSpaceUserMembership(String path, User user, Integer spaceId, SHARESPACE fromShareSpace)
            throws IOException, JsonException {
        String tenantName = user.getTenantName();
        Properties prop = new Properties();
        prop.setProperty("userId", user.getId());
        prop.setProperty("ticket", user.getTicket());
        String clientId = user.getClientId();
        Integer platformId = user.getPlatformId();
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null) {
            prop.setProperty("platformId", platformId.toString());
        }
        StringBuffer url = new StringBuffer(120);
        url.append(path).append("/rs/usr/memberships?tokenGroupName=").append(URLEncoder.encode(tenantName, StandardCharsets.UTF_8.name()));
        if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace)) {
            url.append("&q=").append(spaceId);
        }
        String respString = RestClient.get(url.toString(), prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        List<JsonWraper> list = resp.getResultAsList("memberships");
        List<JsonMembership> memberships = Collections.emptyList();
        if (list != null && !list.isEmpty()) {
            memberships = new ArrayList<>(list.size());
            for (JsonWraper wraper : list) {
                JsonMembership membership = wraper.getAsObject(JsonMembership.class);
                if (membership != null) {
                    if (SHARESPACE.PROJECTSPACE.equals(fromShareSpace) && membership.getType() == 1) {
                        memberships.add(membership);
                    }
                    if (SHARESPACE.ENTERPRISESPACE.equals(fromShareSpace) && membership.getType() == 2) {
                        memberships.add(membership);
                    }
                }
            }
        }
        if (memberships != null && memberships.get(0) != null) {
            return memberships.get(0).getId();
        }
        return null;
    }

    private List<JsonMembership> getOwner(String path, User user) throws IOException, JsonException {
        String tenantName = user.getTenantName();
        Properties prop = new Properties();
        prop.setProperty("userId", user.getId());
        prop.setProperty("ticket", user.getTicket());
        String clientId = user.getClientId();
        Integer platformId = user.getPlatformId();
        if (StringUtils.hasText(clientId)) {
            prop.setProperty("clientId", clientId);
        }
        if (platformId != null) {
            prop.setProperty("platformId", platformId.toString());
        }
        String respString = RestClient.get(path + "/rs/usr/memberships?tokenGroup=" + URLEncoder.encode(tenantName, StandardCharsets.UTF_8.name()), prop, false);
        JsonResponse resp = JsonResponse.fromJson(respString);
        if (resp.hasError()) {
            throw new JsonException(resp.getStatusCode(), resp.getMessage());
        }
        List<JsonWraper> list = resp.getResultAsList("memberships");
        List<JsonMembership> memberships = Collections.emptyList();
        if (list != null && !list.isEmpty()) {
            memberships = new ArrayList<>(list.size());
            for (JsonWraper wraper : list) {
                JsonMembership membership = wraper.getAsObject(JsonMembership.class);
                if (membership != null) {
                    memberships.add(membership);
                }
            }
        }
        return memberships;
    }

    private boolean isOwner(User user, String membership, String rmsURL) throws IOException, JsonException {
        List<JsonMembership> memberships = getOwner(rmsURL, user);
        if (memberships != null && !memberships.isEmpty()) {
            for (JsonMembership member : memberships) {
                if (StringUtils.equals(member.getId(), membership) && member.getType() == 0) { // 0 - tenant ; 1 - Project ; 2 - SysBucket TG_membership 
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasDocConversionJarFiles() {
        String perceptiveJarPath = ViewerConfigManager.getInstance().getDocConverterDir() + File.separator + ViewerConfigManager.ISYS11DF_JAR;
        String memoryStreamJarPath = ViewerConfigManager.getInstance().getDocConverterDir() + File.separator + ViewerConfigManager.MEMORY_STREAM_JAR;
        File perceptiveJar = new File(perceptiveJarPath);
        File memoryStreamJar = new File(memoryStreamJarPath);

        return perceptiveJar.exists() && memoryStreamJar.exists();
    }

}
