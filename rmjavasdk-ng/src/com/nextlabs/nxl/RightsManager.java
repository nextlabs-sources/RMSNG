/**
 * This is the wrapper class to be used for encrypting and decrypting with NextLabs RMS.
 */
package com.nextlabs.nxl;

import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.SimpleSSLSocketFactory;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonClassificationCategory;
import com.nextlabs.common.shared.JsonClassificationCategory.Label;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonProject;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonTag;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.HTTPUtil;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.CryptoBaseRequest.StreamInfo;
import com.nextlabs.nxl.exception.JsonException;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsStatus;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

/**
 * This class contains methods to encrypt an unprotected file, to decrypt an NXL file and to read or update the metadata
 * of an encrypted NXL file. Only one instance of this class should be created by the calling application as creating an
 * instance of this class is an expensive and time consuming task. The calling application can perform multiple
 * encryption/decryption operations on the same instance. Even in multithreaded scenarios, create a single instance of
 * RightsManager and share it among different threads. This class is implicitly thread safe (as a consequence of Immutability) 
 * but method(s) receiving mutable parameters may not be in some scenarios.
 *
 * @author RMS-DEV-TEAM@nextlabs.com
 * @version 1.0.0
 *
 */
public class RightsManager {

    private static Logger logger = LogManager.getLogger("RightsManager");
    static {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
        if (!setFIPSContext()) {
            logger.fatal("FIPS compliant provider not initialized/available");
            System.exit(-1);
        }
    }
    private String watermark;
    private JsonExpiry expiry;
    private String routerUrl;
    private int appId;
    private String appKey;
    private String clientId;
    private Integer platformId;
    private Map<CacheKey, TokenGroupDetails> detailKeyedTokenGroupCache;
    private Map<String, TokenGroupDetails> nameKeyedTokenGroupCache;
    private static final Type CATEGORY_LIST_TYPE = new TypeToken<List<JsonClassificationCategory>>() {
    }.getType();

    /**
     * The RightsManager class connects to a NexLabs Rights Management Server using the {@code appId} and the {@code appKey}. 
     * Refer to the user guide on how to generate an appId and appKey in Rights Management Server.
     * @param routerURL URL of the Rights Management Router
     * @param appId Rights Management Application ID
     * @param appKey Rights Management Application Key
     * @throws NxlException if {@code appId} or {@code appKey} or {@code routerURL} is missing
     */
    public RightsManager(String routerURL, int appId, String appKey) throws NxlException {
        if (!StringUtils.hasText(routerURL) || !StringUtils.hasText(String.valueOf(appId)) || !StringUtils.hasText(appKey)) {
            logger.error("Insufficient values to initialize class");
            throw new NxlException("Insufficient values to initialize class");
        }
        this.routerUrl = routerURL;
        this.appId = appId;
        this.appKey = appKey;
        this.clientId = "RMJavaSDK_CLIENT";
        this.platformId = DeviceType.WEB.getLow();
        this.watermark = "";
        this.expiry = new JsonExpiry();
        this.expiry.setOption(0);
        this.detailKeyedTokenGroupCache = new ConcurrentHashMap<>();
        this.nameKeyedTokenGroupCache = new ConcurrentHashMap<>();
        try {
            SimpleSSLSocketFactory sf = new SimpleSSLSocketFactory(SimpleSSLSocketFactory.TLS_V1_2);
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
            HttpsURLConnection.setDefaultHostnameVerifier(SimpleSSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Initialized RMJavaSDK v{} with token caching {}", getVersion(), isTokenCachingEnabled());
    }

    /**
     * This method can be used to send mass invitation(s) to a project 
     * @param projectId Project ID of the project to whom the members must be invited
     * @param projectName Name of project to whom the members must be invited
     * @param parentTenantName Name of tenant under which the project exists
     * @param emails List of email addresses who will receive the invitation to the project
     * @param projectInvitationMessage Invitation message that will be mailed to the invitees to the project
     * @return JSON response with details	
     * @throws NxlException If there is an {@code IOException} or a NXL specific exception
     */
    public JsonResponse inviteUsersToProject(Integer projectId, String projectName, String parentTenantName,
        List<String> emails, String projectInvitationMessage) throws NxlException {

        emails = Collections.unmodifiableList(emails);
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        String path = details.getRmsUrl() + "/rs/project/" + projectId + "/invite";

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("emails", emails);
        req.addParameter("invitationMsg", projectInvitationMessage);

        JsonResponse resp = null;
        try {
            String ret = RestClient.post(path, prop, req.toJson());
            resp = JsonResponse.fromJson(ret);

            if (resp.hasError()) {
                throw new NxlException("Unable to send invitation emails for " + projectName + " with message " + resp.getMessage());
            }
        } catch (IOException e) {
            logger.error("Unable to send invitation emails for " + projectName, e);
            throw new NxlException("Unable to send invitation emails for " + projectName, e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Sent invitation emails for project {} under parent tenant {}", projectName, parentTenantName);
        }
        return resp;
    }

    /**
     * This method can be used to upload a native or a protected file to a project in SkyDRM
     * @param projectId Project ID of the project where the file will be uploaded
     * @param projectName Name of project where the file will be uploaded 
     * @param parentTenantName Name of tenant under which the project exists
     * @param inputPath The path of the input file to be encrypted
     * @param apiInput JsonRequest of pay load for Project upload API
     * @return JSON response with details	
     * @throws NxlException If there is an {@code IOException} or a NXL specific exception
     */
    public JsonResponse uploadFileToProject(Integer projectId, String projectName, String parentTenantName,
        String inputPath, JsonRequest apiInput) throws NxlException {
        return this.uploadFileToProject(projectId, projectName, parentTenantName, new File(inputPath), apiInput);
    }

    /**
     * This method can be used to upload a native or a protected file to a project in SkyDRM
     * @param projectId Project ID of the project where the file will be uploaded
     * @param projectName Name of project where the file will be uploaded 
     * @param parentTenantName Name of tenant under which the project exists
     * @param inputFile The File object of the file to be uploaded
     * @param apiInput JsonRequest of pay load for Project upload API
     * @return JSON response with details	
     * @throws NxlException If there is an {@code IOException} or a NXL specific exception
     */
    public JsonResponse uploadFileToProject(Integer projectId, String projectName, String parentTenantName,
        File inputFile, JsonRequest apiInput) throws NxlException {

        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        String path = details.getRmsUrl() + "/rs/project/" + projectId + "/upload";

        JsonResponse resp = null;
        try (CloseableHttpClient client = HTTPUtil.getHTTPClient()) {
            HttpPost httpPost = new HttpPost(path);
            httpPost.setHeader("userId", String.valueOf(appId));
            httpPost.setHeader("ticket", appKey);
            httpPost.setHeader("clientId", clientId);
            httpPost.setHeader("platformId", String.valueOf(platformId));

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("API-input", GsonUtils.GSON_SHALLOW.toJson(apiInput)).addBinaryBody("file", inputFile, ContentType.APPLICATION_OCTET_STREAM, inputFile.getName());
            HttpEntity multipart = builder.build();

            httpPost.setEntity(multipart);
            CloseableHttpResponse response = client.execute(httpPost);
            int httpCode = response.getStatusLine().getStatusCode();
            String message = EntityUtils.toString(response.getEntity());

            if (httpCode != HttpURLConnection.HTTP_OK) {
                logger.error("Unable to upload file to project " + projectName + " with HTTP response " + httpCode + " with message " + message);
                throw new NxlException("Unable to upload file to project " + projectName + " with message " + message);
            }
            resp = JsonResponse.fromJson(message);
            if (resp.hasError()) {
                logger.error("Unable to upload file to project " + projectName + " with response " + resp.getStatusCode() + " with message " + resp.getMessage());
                throw new NxlException("Unable to upload file to project " + projectName + " with message " + resp.getMessage());
            }

        } catch (ClientProtocolException e) {
            logger.error("Unable to upload file to project " + projectName, e);
            throw new NxlException("Unable to upload file to project " + projectName + " with message " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Unable to upload file to project " + projectName, e);
            throw new NxlException("Unable to upload file to project " + projectName + " with message " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            logger.error("Unable to upload file to project " + projectName, e);
            throw new NxlException("Unable to upload file to project " + projectName + " with message " + e.getMessage(), e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Uploaded file {} to project {} under parent tenant {}", inputFile.getName(), projectName, parentTenantName);
        }
        return resp;
    }

    /**
     * This method can be used to encrypt files and convert them to NXL format.
     * @param inputPath The path of the input file to be encrypted.
     * @param outputPath The path where the encrypted NXL file is written.
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to encrypt input file 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encrypt(String inputPath, String outputPath, Map<String, List<String>> attributes,
        Rights[] rights, Map<String, String[]> tags, String parentTenantName, String projectName) throws NxlException {

        File inputFile = new File(inputPath);
        if (isNXL(inputPath)) {
            throw new NxlException("Cannot encrypt an NXL file again");
        }
        File outputFile = new File(outputPath);
        this.encrypt(inputFile, outputFile, attributes, rights, tags, parentTenantName, projectName);
    }

    /**
     * This method can be used to encrypt files and convert them to NXL format.
     * @param inputPath The path of the input file to be encrypted.
     * @param outputPath The path where the encrypted NXL file is written.
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param tenantName Name of the tenant whose tokens are to be used to encrypt input file. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encrypt(String inputPath, String outputPath, Map<String, List<String>> attributes,
        Rights[] rights, Map<String, String[]> tags, String tenantName, TokenGroupType tgType) throws NxlException {

        File inputFile = new File(inputPath);
        if (isNXL(inputPath)) {
            throw new NxlException("Cannot encrypt an NXL file again");
        }
        File outputFile = new File(outputPath);
        this.encrypt(inputFile, outputFile, attributes, rights, tags, tenantName, tgType);
    }

    /**
     * This method can be used to encrypt files and convert them to NXL format.
     * @param inputFile The path of the input file to be encrypted.
     * @param outputFile The path where the encrypted NXL file is written.
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to encrypt input file 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encrypt(File inputFile, File outputFile, Map<String, List<String>> attributes,
        Rights[] rights, Map<String, String[]> tags, String parentTenantName, String projectName) throws NxlException {
        if (isNXL(inputFile.getPath())) {
            throw new NxlException("Cannot encrypt an NXL file again");
        }
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        try (OutputStream outputstream = new FileOutputStream(outputFile)) {
            try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(appId, appKey, clientId, platformId, details.getRmsUrl(), details.getMembershipId(), rights, watermark, expiry, tags, inputFile, outputstream))) {
                logger.info("Encrypted file " + inputFile.getName() + " successfully");
            }
        } catch (IOException | GeneralSecurityException | NxlException | TokenGroupNotFoundException e) {
            logger.error("Exception in encrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to encrypt files and convert them to NXL format.
     * @param inputFile The path of the input file to be encrypted.
     * @param outputFile The path where the encrypted NXL file is written.
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param tenantName Name of the tenant whose tokens are to be used to encrypt input file. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encrypt(File inputFile, File outputFile, Map<String, List<String>> attributes,
        Rights[] rights, Map<String, String[]> tags, String tenantName, TokenGroupType tgType) throws NxlException {
        if (isNXL(inputFile.getPath())) {
            throw new NxlException("Cannot encrypt an NXL file again");
        }
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        try (OutputStream outputstream = new FileOutputStream(outputFile)) {
            try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(appId, appKey, clientId, platformId, details.getRmsUrl(), details.getMembershipId(), rights, watermark, expiry, tags, inputFile, outputstream))) {
                logger.info("Encrypted file " + inputFile.getName() + " successfully");
            }
        } catch (IOException | GeneralSecurityException | NxlException | TokenGroupNotFoundException e) {
            logger.error("Exception in encrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputPath The path of the input NXL file to be decrypted.
     * @param outputPath The path where the decrypted file is written.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(String inputPath, String outputPath) throws NxlException {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        this.decrypt(inputFile, outputFile);
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputFile The path of the input NXL file to be decrypted.
     * @param outputFile The path where the decrypted file is written.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(File inputFile, File outputFile) throws NxlException {
        try (InputStream fis = new FileInputStream(inputFile); NxlFile header = NxlFile.parse(fis)) {
            String tokenGroupName = StringUtils.substringAfter(header.getOwner(), "@");
            TokenGroupDetails details = getTokenGroupCache(tokenGroupName);
            if (logger.isDebugEnabled()) {
                logger.debug("Parameters parsed by RightsManager.decrypt: rmsURL: " + details.getRmsUrl() + " appId: " + appId + " appKey: " + appKey + " clientId: " + clientId + " platformId: " + platformId + " tenantName: " + details.getTenantName() + " inputFile: " + inputFile + " outputFile: " + outputFile);
            }
            DecryptUtil.decrypt(details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), header, outputFile);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputPath The path of the input NXL file to be decrypted.
     * @param outputPath The path where the decrypted file is written.
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to decrypt input file
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(String inputPath, String outputPath, String parentTenantName, String projectName)
            throws NxlException {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        this.decrypt(inputFile, outputFile, parentTenantName, projectName);
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputPath The path of the input NXL file to be decrypted.
     * @param outputPath The path where the decrypted file is written.
     * @param tenantName Name of the tenant whose tokens are to be used to decrypt input file. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(String inputPath, String outputPath, String tenantName, TokenGroupType tgType)
            throws NxlException {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        this.decrypt(inputFile, outputFile, tenantName, tgType);
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputFile The path of the input NXL file to be decrypted.
     * @param outputFile The path where the decrypted file is written.
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to decrypt input file
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(File inputFile, File outputFile, String parentTenantName, String projectName)
            throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        try {
            DecryptUtil.decrypt(details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), inputFile, outputFile);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to decrypt NXL files.
     * @param inputFile The path of the input NXL file to be decrypted.
     * @param outputFile The path where the decrypted file is written.
     * @param tenantName Name of the tenant whose tokens are to be used to decrypt input file. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void decrypt(File inputFile, File outputFile, String tenantName, TokenGroupType tgType) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        try {
            DecryptUtil.decrypt(details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), inputFile, outputFile);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to encrypt an input stream to an encrypted output stream.
     * @param in Stream to be encrypted.
     * @param out Stream to write the encrypted content to.
     * @param fileName File name of the input stream (stored as part of the NXL metadata)
     * @param contentLength Length of the input stream
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to encrypt input stream 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encryptStream(InputStream in, OutputStream out, String fileName, long contentLength,
        Map<String, List<String>> attributes, Rights[] rights, Map<String, String[]> tags, String parentTenantName,
        String projectName)
            throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        StreamInfo info = new StreamInfo(in, contentLength, fileName);

        try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(appId, appKey, clientId, platformId, details.getRmsUrl(), details.getMembershipId(), rights, watermark, expiry, tags, info, out))) {
            logger.info("Encrypted input stream successfully");
        } catch (IOException | GeneralSecurityException | TokenGroupNotFoundException e) {
            logger.error("Exception in encrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to encrypt an input stream to an encrypted output stream.
     * @param in Stream to be encrypted.
     * @param out Stream to write the encrypted content to.
     * @param fileName File name of the input stream (stored as part of the NXL metadata)
     * @param contentLength Length of the input stream
     * @param attributes Attributes to be added to the NXL file.
     * @param rights Rights to be added to the NXL file.
     * @param tags Tags to be added to the NXL file.
     * @param tenantName Name of the tenant whose tokens are to be used to encrypt input stream. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET  
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void encryptStream(InputStream in, OutputStream out, String fileName, long contentLength,
        Map<String, List<String>> attributes, Rights[] rights, Map<String, String[]> tags, String tenantName,
        TokenGroupType tgType)
            throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        StreamInfo info = new StreamInfo(in, contentLength, fileName);

        try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).encrypt(new RemoteCrypto.RemoteEncryptionRequest(appId, appKey, clientId, platformId, details.getRmsUrl(), details.getMembershipId(), rights, watermark, expiry, tags, info, out))) {
            logger.info("Encrypted input stream successfully");
        } catch (IOException | GeneralSecurityException | TokenGroupNotFoundException e) {
            logger.error("Exception in encrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * Build a NXL header which can be reused for future partial decryptions. This method returns only the nxl header.
     * @param inputPath The path of the input NXL file to be decrypted.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @return NxlFile header
     * @see #decryptPartial
     * @see #getHeaderSize
     */
    public NxlFile buildNxlHeader(String inputPath) throws NxlException {
        File inputFile = new File(inputPath);
        return this.buildNxlHeader(inputFile);
    }

    /**
     * Build a NXL header which can be reused for future partial decryptions. This method returns only the nxl header.
     * @param inputFile The path of the input NXL file to be decrypted.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @return NxlFile header
     * @see #decryptPartial
     * @see #getHeaderSize
     */
    public NxlFile buildNxlHeader(File inputFile) throws NxlException {
        byte[] buf = new byte[RightsManager.getHeaderSize()];
        try (InputStream is = new FileInputStream(inputFile)) {
            int n = is.read(buf);
            if (n != RightsManager.getHeaderSize()) {
                throw new NxlException("Insufficient bytes to parse header");
            }
        } catch (FileNotFoundException e) {
            logger.error("File not found ", e);
            throw new NxlException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("IO error in reading file's header ", e);
            throw new NxlException(e.getMessage(), e);
        }
        return this.buildNxlHeader(buf);
    }

    /**
     * Build a NXL header which can be reused for future partial decryptions. This method returns only the nxl header.
     * @param header Starting bytes of the NXL file. Must be at least of a standard NXL header length 
     * which can be obtained from {@code getHeaderSize()} 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @return NxlFile header
     * @see #decryptPartial
     * @see #getHeaderSize
     */
    public NxlFile buildNxlHeader(byte[] header) throws NxlException {
        try (NxlFile nxl = NxlFile.parse(header)) {
            String tokenGroupName = StringUtils.substringAfter(nxl.getOwner(), "@");
            TokenGroupDetails details = getTokenGroupCache(tokenGroupName);
            if (isTokenCachingEnabled()) {
                DecryptUtil.prefetchDuidInCache(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), nxl);
            }
            return nxl;
        } catch (GeneralSecurityException | JsonException e) {
            logger.error("Exception in prefetching token ", e);
            throw new NxlException(e.getMessage(), e);
        } catch (IOException | NxlException e) {
            logger.error("Exception in parsing nxl header ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * Build a NXL header which can be reused for future partial decryptions. This method returns only the nxl header.
     * @param header Starting bytes of the NXL file. Must be at least of a standard NXL header length 
     * which can be obtained from {@code getHeaderSize()} 
     * @param parentTenantName Name of the parent tenant. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of the project.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @return NxlFile header
     * @see #decryptPartial
     * @see #getHeaderSize
     */
    public NxlFile buildNxlHeader(byte[] header, String parentTenantName, String projectName) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        try (NxlFile nxl = NxlFile.parse(header)) {
            if (isTokenCachingEnabled()) {
                DecryptUtil.prefetchDuidInCache(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), nxl);
            }
            return nxl;
        } catch (GeneralSecurityException | JsonException e) {
            logger.error("Exception in prefetching token ", e);
            throw new NxlException(e.getMessage(), e);
        } catch (IOException | NxlException e) {
            logger.error("Exception in parsing nxl header ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * Build a NXL header which can be reused for future partial decryptions. This method returns only the nxl header.
     * @param header Starting bytes of the NXL file. Must be at least of a standard NXL header length 
     * which can be obtained from {@code getHeaderSize()} 
     * @param tenantName Name of the tenant. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @return NxlFile header
     * @see #decryptPartial
     * @see #getHeaderSize
     */
    public NxlFile buildNxlHeader(byte[] header, String tenantName, TokenGroupType tgType) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        try (NxlFile nxl = NxlFile.parse(header)) {
            if (isTokenCachingEnabled()) {
                DecryptUtil.prefetchDuidInCache(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), nxl);
            }
            return nxl;
        } catch (GeneralSecurityException | JsonException e) {
            logger.error("Exception in prefetching token ", e);
            throw new NxlException(e.getMessage(), e);
        } catch (IOException | NxlException e) {
            logger.error("Exception in parsing nxl header ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method decrypts an input stream starting from {@code start} for {@code len} bytes and write it to an output stream.
     * If token caching is enabled then please note that at present, the entry eviction/cleanup by Google Guava cache is implemented as "small 
     * amounts of maintenance during write operations [on the cache, not during file I/O], or during occasional read operations if writes are rare". 
     * @param in Stream to be decrypted.
     * @param out Stream to write the encrypted content to.
     * @param header NXL header constructed from {@code buildNxlHeader()} which must be valid and non-null
     * @param start Position to start decryption from
     * @param len Number of bytes to decrypt
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @see #buildNxlHeader
     */
    public void decryptPartial(InputStream in, OutputStream out, NxlFile header, long start, long len)
            throws NxlException {
        if (header == null) {
            logger.warn("Passed Invalid header");
            throw new NxlException("Passed Invalid header");
        }
        try {
            String tokenGroupName = StringUtils.substringAfter(header.getOwner(), "@");
            TokenGroupDetails details = getTokenGroupCache(tokenGroupName);
            DecryptUtil.decryptPartial(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), in, out, header, start, len);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method decrypts an input stream starting from {@code start} for {@code len} bytes and write it to an output stream.
     * If token caching is enabled then please note that at present, the entry eviction/cleanup by Google Guava cache is implemented as "small 
     * amounts of maintenance during write operations [on the cache, not during file I/O], or during occasional read operations if writes are rare". 
     * @param in Stream to be decrypted.
     * @param out Stream to write the encrypted content to.
     * @param header NXL header constructed from {@code buildNxlHeader()}, but if its null then the method will parse it automatically so the inputstream need not "skip" those bytes
     * @param start Position to start decryption from
     * @param len Number of bytes to decrypt
     * @param parentTenantName Name of the tenant project is created under. Default tenant will be used if {@code NULL} is provided.
     * @param projectName Name of project whose tokens are to be used to decrypt input stream 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @see #buildNxlHeader
     */
    public void decryptPartial(InputStream in, OutputStream out, NxlFile header, long start, long len,
        String parentTenantName, String projectName) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        try {
            DecryptUtil.decryptPartial(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), in, out, header, start, len);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method decrypts an input stream starting from {@code start} for {@code len} bytes and write it to an output stream
     * If token caching is enabled then please note that at present, the entry eviction/cleanup by Google Guava cache is implemented as "small
     * amounts of maintenance during write operations [on the cache, not during file I/O], or during occasional read operations if writes are rare".
     * @param in Stream to be decrypted.
     * @param out Stream to write the encrypted content to.
     * @param header NXL header constructed from {@code buildNxlHeader()}, but if its null then the method will parse it automatically so the inputstream need not "skip" those bytes
     * @param start Position to start decryption from
     * @param len Number of bytes to decrypt
     * @param tenantName Name of the tenant whose tokens are to be used to decrypt input stream. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception 
     * @see #buildNxlHeader
     */
    public void decryptPartial(InputStream in, OutputStream out, NxlFile header, long start, long len,
        String tenantName, TokenGroupType tgType) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        try {
            DecryptUtil.decryptPartial(logger, details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), in, out, header, start, len);
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method can be called to invalidate the token in cache if it were present (and caching were enabled, otherwise does not apply) 
     * for a given duid, otherwise it returns false immediately.
     * @param duid The duid of the NXL file which is available in the NxlFile object (returned by buildNxlHeader) by calling header.getDuid()
     * @return true if token caching was enabled and token for given duid was submitted for invalidation. 
     * The actual eviction might happen when the maintenance operation will be performed by Google Guava cache.
     */
    public boolean flushTokenFromCache(String duid) {
        if (isTokenCachingEnabled()) {
            logger.debug("Invalidation of token submitted for duid {}", duid);
            return DecryptUtil.flushTokenFromCache(duid);
        }
        return false;
    }

    /**
     * This method returns the length of the file before encryption.
     * @param header NxlFile header
     * @return length of the original file
     * @see #buildNxlHeader
     */
    public long getOriginalContentLength(NxlFile header) {
        return header.getContentLength();
    }

    /**
     * This method reads the tags in the NXL header.
     * @param inputPath The path of the input file.
     * @return Tags in the NXL file. 
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public Map<String, String[]> readTags(String inputPath) throws NxlException {
        try (InputStream is = new FileInputStream(inputPath)) {
            return readTags(is);
        } catch (IOException e) {
            logger.error("Exception in reading tags ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method reads the tags in the NXL header.
     * @param is InputStream object of the input file.
     * @return Tags in the NXL file.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public Map<String, String[]> readTags(InputStream is) throws NxlException {
        try (NxlFile nxl = NxlFile.parse(is)) {
            return DecryptUtil.getTags(nxl, null);
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Exception in reading tags ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method updates the tags in the NXL header with the new values.
     * @param inputPath The path of the input file to update.
     * @param tags Tags to be added to the NXL file.
     * @param tenantName Name of the tenant. Default tenant will be used if {@code NULL} is provided. 
     * @param tgType token group type - TOKENGROUP_TENANT/TOKENGROUP_PROJECT/TOKENGROUP_SYSTEMBUCKET
     * @param projectName project name if tgType is TOKENGROUP_PROJECT
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void updateTags(String inputPath, Map<String, String[]> tags, String tenantName, TokenGroupType tgType,
        String projectName) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, projectName);
        try (InputStream is = new FileInputStream(inputPath);
                InputStream is1 = new FileInputStream(inputPath);
                NxlFile nxl1 = NxlFile.parse(is1)) {
            byte[] token = DecryptUtil.requestToken(details.getRmsUrl(), appId, appKey, clientId, platformId, details.getTenantName(), nxl1.getOwner(), nxl1.getDuid(), nxl1.getRootAgreement(), nxl1.getMaintenanceLevel(), nxl1.getProtectionType(), DecryptUtil.getFilePolicyStr(nxl1, null), DecryptUtil.getTagsString(nxl1, null));
            if (!nxl1.isValid(token)) {
                throw new NxlException("Invalid token.");
            }
            String existingFileTags = DecryptUtil.getTagsString(nxl1, null);
            byte[] header = new byte[NxlFile.COMPLETE_HEADER_SIZE];
            IOUtils.read(is, header);
            String encodedHeader = Base64Codec.encodeAsString(header);
            String path = details.getRmsUrl() + "/rs/token/" + nxl1.getDuid();

            Properties prop = new Properties();
            prop.setProperty("userId", String.valueOf(appId));
            prop.setProperty("ticket", appKey);
            prop.setProperty("clientId", clientId);
            prop.setProperty("platformId", String.valueOf(platformId));

            JsonRequest req = new JsonRequest();
            req.addParameter("protectionType", nxl1.getProtectionType().ordinal());
            req.addParameter("fileTags", tags == null ? "{}" : GsonUtils.GSON.toJson(tags));
            req.addParameter("existingFileTags", existingFileTags);
            req.addParameter("fileHeader", encodedHeader);

            JsonResponse resp = null;
            try {
                String ret = RestClient.put(path, prop, req.toJson());
                resp = JsonResponse.fromJson(ret);
            } catch (IOException e) {
                logger.error("Unable to update tags for duid " + nxl1.getDuid(), e);
                throw new NxlException("Unable to update tags for duid " + nxl1.getDuid(), e);
            }

            if (resp.hasError()) {
                throw new NxlException("Failed to update tags for duid " + nxl1.getDuid() + " with message " + resp.getMessage());
            }

            String fileinfoCheckSumStr = resp.getResultAsString("fileinfoCheckSum");
            String dateModified = String.valueOf(resp.getResultAsLong("dateModified", new Date().getTime()));
            String tagChecksumStr = resp.getResultAsString("tagCheckSum");
            String headerChecksumStr = resp.getResultAsString("headerCheckSum");

            if (!StringUtils.hasText(fileinfoCheckSumStr) || !StringUtils.hasText(dateModified) || !StringUtils.hasText(tagChecksumStr) || !StringUtils.hasText(headerChecksumStr)) {
                throw new NxlException("Missing checksum");
            }

            byte[] fileinfoChecksum = Base64Codec.decode(fileinfoCheckSumStr);
            byte[] tagChecksum = Base64Codec.decode(tagChecksumStr);
            byte[] headerChecksum = Base64Codec.decode(headerChecksumStr);

            FileInfo updatedFileInfo = GsonUtils.GSON.fromJson(new String(nxl1.getSection(".FileInfo").getData(), "UTF-8"), Constants.FILEINFO_TYPE);
            updatedFileInfo.setLastModified(Long.valueOf(dateModified));
            updatedFileInfo.setModifiedBy(details.getMembershipId());

            /* 
             * Changes to different parts of the nxl file header cannot  
             * be done consecutively, to ensure checksums are calculated 
             * correctly so need to update and re-parse before updating again.
             */
            try (ByteArrayOutputStream baos1 = new ByteArrayOutputStream()) {
                EncryptUtil.updateTags(nxl1, GsonUtils.GSON.toJson(tags), tagChecksum, headerChecksum, baos1);
                try (InputStream is2 = new ByteArrayInputStream(baos1.toByteArray());
                        NxlFile nxl2 = NxlFile.parse(is2)) {
                    try (RandomAccessFile raf = new RandomAccessFile(inputPath, "rw")) {
                        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                        EncryptUtil.updateFileInfo(nxl2, GsonUtils.GSON.toJson(updatedFileInfo), fileinfoChecksum, headerChecksum, baos2);
                        raf.write(baos2.toByteArray());
                    }
                }
            }
        } catch (IOException | GeneralSecurityException | JsonException e) {
            logger.error("Exception in decrypting file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method is created to refresh the DUID on the file. This is to address the Copy file with same DUID issue in Teamcenter for GEPOC
     * @param inputFilePath filePath of the file which needs to copied with new DUID
     * @param outputFilePath where the copy from the input need to be saved
     * @param tenantName SKYDRM Tenant name
     * @param tgType SKYDRM TokenGroupType
     * @throws NxlException
     */
    public void copyNXLwithNewDUID(String inputFilePath, String outputFilePath, String tenantName,
        TokenGroupType tgType) throws NxlException {
        if (!isNXL(inputFilePath)) {
            throw new NxlException("Not able to create a Copy of Non NXL file");
        }

        if (outputFilePath == null || !StringUtils.hasText(outputFilePath)) {
            throw new NxlException("Copy File Destination path is empty");
        }

        if (!outputFilePath.endsWith(".nxl")) {
            throw new NxlException("Copy File Destination path is not a NXL File. It should have extension .nxl");
        }
        TokenGroupDetails details = getTokenGroupCache(tenantName, tgType, null);
        Map<String, String[]> tags = readTags(inputFilePath);

        try (OutputStream outputstream = new FileOutputStream(new File(outputFilePath))) {
            try (NxlFile nxl = EncryptUtil.create(RemoteCrypto.INSTANCE).refreshNxl(new RemoteCrypto.RemoteEncryptionRequest(appId, appKey, clientId, platformId, details.getRmsUrl(), details.getMembershipId(), details.getTenantName(), null, watermark, expiry, tags, new File(inputFilePath), outputstream))) {

                logger.info("Copy of " + inputFilePath + "is created at " + outputFilePath + " successfully");
            }
        } catch (IOException | GeneralSecurityException | NxlException | TokenGroupNotFoundException
                | JsonException e) {
            logger.error("Exception in copyNXLwithNewDUID file ", e);
            throw new NxlException(e.getMessage(), e);
        }
    }

    /**
     * This method checks whether a file is a valid NXL file or not.
     * @param inputPath The path of the input file to validate.
     * @return {@code true} for valid NXL files, {@code false} for non-NXL files or if an {@code Exception} occurs.
     */
    public boolean isNXL(String inputPath) {
        try (InputStream is = new FileInputStream(inputPath)) {
            return isNXL(is);
        } catch (IOException e) {
            logger.error("Exception in parsing file: {} ", e.getMessage(), e);
            return false;
        }
    }

    /**
     * This method checks whether a file is a valid NXL file or not.
     * @param is InputStream object of the input file to validate.
     * @return {@code true} for valid NXL files, {@code false} for non-NXL files or if an {@code Exception} occurs.
     */
    public boolean isNXL(InputStream is) {
        try {
            byte[] header = new byte[NxlFile.BASIC_HEADER_SIZE];
            IOUtils.read(is, header);
            return NxlFile.isNxl(header);
        } catch (IOException e) {
            logger.error("Exception in parsing file: {} ", e.getMessage(), e);
            return false;
        }
    }

    /**
     * This method list projects the user is member of in SkyDRM.
     * @param ownedByMe Get projects owned by me or owned by others, don't care if set to null.
     * @param parentTenant The tenant under which the projects are created. Default tenant will be used if NULL is provided.    
     * @return project list.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public List<JsonProject> listProjects(Boolean ownedByMe, String parentTenant) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenant, TokenGroupType.TOKENGROUP_TENANT, null);
        String path = details.getRmsUrl() + "/rs/project";
        if (ownedByMe != null) {
            StringBuilder sb = new StringBuilder(path);
            if (ownedByMe) {
                sb.append("?ownedByMe=true");
            } else {
                sb.append("?ownedByMe=false");
            }
            path = sb.toString();
        }

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonResponse resp = null;
        try {
            String ret = RestClient.get(path, prop, false);
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Error occured while listing projects for user " + appId, e);
            throw new NxlException("Error occured while listing projects for user " + appId, e);
        }

        if (resp.hasError()) {
            throw new NxlException("Failed to list projects: " + resp.getMessage());
        }

        List<JsonWraper> wraperList = resp.getResultAsList("detail");
        List<JsonProject> projectList = new ArrayList<JsonProject>();
        if (wraperList != null) {
            for (JsonWraper wraper : wraperList) {
                if (wraper != null) {
                    projectList.add(wraper.getAsObject(JsonProject.class));
                }
            }
        }
        return projectList;
    }

    /**
     * This method is used to get project metadata.
     * @param projectId id of project.
     * @param projectName Name of project, projectId will be omitted if projectName is present.
     * @param parentTenant The tenant under which the project is created. Default tenant will be used if NULL is provided.
     * @return project metadata if project exists, null if project doesn't exist.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public JsonProject getProjectMetadata(Integer projectId, String projectName, String parentTenant)
            throws NxlException {
        if (StringUtils.hasText(projectName)) {
            List<JsonProject> plist = listProjects(true, parentTenant);
            for (JsonProject project : plist) {
                if (project.getName().equals(projectName)) {
                    return project;
                }
            }
            return null;
        }
        TokenGroupDetails details = getTokenGroupCache(parentTenant, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        String path = details.getRmsUrl() + "/rs/project/" + projectId;
        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonResponse resp = null;
        try {
            String ret = RestClient.get(path, prop, false);
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Error occured while getting metadata for project " + projectId, e);
            throw new NxlException("Error occured while getting metadata for project " + projectId, e);
        }

        if (resp.hasError()) {
            if (resp.getStatusCode() == 400) {
                return null;
            } else {
                throw new NxlException("Failed to get metadata for project " + projectId + " with message " + resp.getMessage());
            }
        }

        return resp.getResult("detail", JsonProject.class);
    }

    /**
     * This method creates a project in SkyDRM.
     * @param projectName The name of project.
     * @param tags Tags for project, optional.
     * @param parentTenant The tenant under which the project is created. Default tenant will be used if NULL is provided.
     * @return metadata of created project.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public JsonProject createProject(String projectName, String[] tags, String parentTenant) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(parentTenant, TokenGroupType.TOKENGROUP_TENANT, null);
        String path = details.getRmsUrl() + "/rs/project";

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("projectName", projectName);
        req.addParameter("projectDescription", projectName);

        JsonResponse resp = null;
        try {
            String ret = RestClient.put(path, prop, req.toJson());
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Unable to create " + projectName + " project", e);
            throw new NxlException("Unable to create project " + projectName, e);
        }

        if (resp.hasError()) {
            throw new NxlException("Failed to create project: " + resp.getMessage());
        }

        int projectId = resp.getResultAsInt("projectId", -1);

        if (tags != null && tags.length > 0) {
            try {
                updateProjectTags(projectId, tags, parentTenant, projectName);
            } catch (NxlException e) {
                logger.error("Updating project" + projectId + " tags failed ", e);
            }
        }

        StringBuilder sb = new StringBuilder(path);
        sb.append('/').append(projectId);
        path = sb.toString();
        try {
            String ret = RestClient.get(path, prop, false);
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Unable to get metadata for project " + projectId, e);
            throw new NxlException("Unable to get metadata for project " + projectId, e);
        }
        return resp.getResult("detail", JsonProject.class);
    }

    /**
     * This method returns available project tags for a particular tenant.
     * @param tenant tenantName. Default tenant will be used if NULL is provided.
     * @return map of project tags, the key of map is tag name, value is tag details.
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public Map<String, JsonTag> getTenantTags(String tenant) throws NxlException {
        TokenGroupDetails details = getTokenGroupCache(tenant, TokenGroupType.TOKENGROUP_TENANT, null);
        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));
        String path = details.getRmsUrl() + "/rs/tags/tenant/" + details.getTenantId() + "?type=0";
        JsonResponse resp = null;
        try {
            String ret = RestClient.get(path, prop, false);
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Unable to get tags for tenant" + tenant, e);
            throw new NxlException("Unable to get tags for tenant " + tenant, e);
        }

        List<JsonWraper> wraperList = resp.getResultAsList("tags");
        Map<String, JsonTag> tags = new HashMap<>();
        if (wraperList != null) {
            for (JsonWraper wraper : wraperList) {
                if (wraper != null) {
                    JsonTag jsonTag = wraper.getAsObject(JsonTag.class);
                    tags.put(jsonTag.getName(), jsonTag);
                }
            }
        }
        return tags;
    }

    /**
     * This method returns available project tags for a particular tenant.
     * @param projectId the Id of project which need to update tags.
     * @param tags tags assigned to project.
     * @param parentTenant tenantName. Default tenant will be used if NULL is provided.
     * @param projectName name of the project whose tags are to be updated
     * @throws NxlException if there is an {@code IOException} or a NXL specific exception
     */
    public void updateProjectTags(Integer projectId, String[] tags, String parentTenant, String projectName)
            throws NxlException {
        if (tags.length == 0) {
            return;
        }
        Map<String, JsonTag> tenantTags = getTenantTags(parentTenant);
        Integer[] tagIds = new Integer[tags.length];
        for (int i = 0; i < tags.length; i++) {
            if (!tenantTags.containsKey(tags[i])) {
                throw new NxlException("Unable to find tag" + tags[i] + " for tenant " + parentTenant);
            }
            tagIds[i] = tenantTags.get(tags[i]).getId();
        }

        TokenGroupDetails details = getTokenGroupCache(parentTenant, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        String path = details.getRmsUrl() + "/rs/tags/project/" + projectId;

        Properties prop = new Properties();
        prop.setProperty("userId", String.valueOf(appId));
        prop.setProperty("ticket", appKey);
        prop.setProperty("clientId", clientId);
        prop.setProperty("platformId", String.valueOf(platformId));

        JsonRequest req = new JsonRequest();
        req.addParameter("projectTags", tagIds);

        JsonResponse resp = null;
        try {
            String ret = RestClient.post(path, prop, req.toJson());
            resp = JsonResponse.fromJson(ret);
        } catch (IOException e) {
            logger.error("Unable to update tags for project " + projectId, e);
            throw new NxlException("Unable to update tags for project " + projectId, e);
        }

        if (resp.hasError()) {
            throw new NxlException("Failed to update tags for project " + projectId + " with message " + resp.getMessage());
        }
    }

    /**
     * This method returns the classification profile based on TokenGroupType
     * @param tenantName Tenant Name under which we are querying for Classification Profile based on token group type. Default tenant will be used if {@code NULL} is provided.
     * @param tgType TokenGroupType - TOKENGROUP_TENANT or TOKENGROUP_PROJECT or TOKENGROUP_SYSTEMBUCKET
     * @param projectName Name of Project if type is Project
     * @return Classification Profile
     * @throws NxlException If there is an {@code IOException} or a NXL specific exception
     */
    public Map<String, String[]> getClassification(String tenantName, TokenGroupType tgType, String projectName)
            throws NxlException {
        String rmsUrl = getRMSUrl(tenantName);
        StringBuilder path = new StringBuilder(rmsUrl);
        Properties restProp = new Properties();
        restProp.setProperty("userId", String.valueOf(appId));
        restProp.setProperty("ticket", appKey);
        restProp.setProperty("clientId", clientId);
        restProp.setProperty("platformId", String.valueOf(DeviceType.WEB.getLow()));
        String ret;
        String tokenGroupName = "";

        switch (tgType) {
            case TOKENGROUP_PROJECT:
                try {
                    path.append("/rs/project/tokenGroupName?parentTenantName=" + URLEncoder.encode(tenantName, StandardCharsets.UTF_8.name()) + "&projectName=" + URLEncoder.encode(projectName, StandardCharsets.UTF_8.name()));
                    ret = RestClient.get(path.toString(), restProp, false);
                    JsonResponse resp = JsonResponse.fromJson(ret);
                    if (resp.hasError()) {
                        throw new NxlException("Unable to query tokenGroupName of project " + projectName + " under parentTenantName " + tenantName + " with message " + resp.getMessage());
                    }
                    tokenGroupName = resp.getResultAsString("tokenGroupName");
                } catch (IOException e) {
                    logger.error("Unable to query tokenGroupName of project " + projectName + " under parentTenantName " + tenantName, e);
                    throw new NxlException("Unable to query tokenGroupName", e);
                } finally {
                    path = new StringBuilder(rmsUrl);
                    ret = null;
                }
                break;
            case TOKENGROUP_TENANT:
            case TOKENGROUP_SYSTEMBUCKET:
                tokenGroupName = tenantName;
                break;
            default:
                throw new NxlException("Incorrect token group type");
        }

        try {
            path.append("/rs/classification/" + URLEncoder.encode(tokenGroupName, StandardCharsets.UTF_8.name()));
            ret = RestClient.get(path.toString(), restProp, false);
        } catch (IOException e) {
            logger.error("Unable to get classification profile from token group name " + tokenGroupName, e);
            throw new NxlException("Unable to get classification profile from token group name " + tokenGroupName, e);
        }

        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new NxlException("Unable to get classification profile from token group name " + tokenGroupName + " with message " + resp.getMessage());
        }

        List<JsonClassificationCategory> categories = resp.getResult("categories", CATEGORY_LIST_TYPE);
        Map<String, String[]> classifications = new HashMap<>();
        for (JsonClassificationCategory category : categories) {
            Set<String> labelSet = new HashSet<>();
            for (Label label : category.getLabels()) {
                labelSet.add(label.getName());
            }
            if (!labelSet.isEmpty()) {
                classifications.put(category.getName(), labelSet.toArray(new String[labelSet.size()]));
            }
        }
        return Collections.unmodifiableMap(classifications);
    }

    /**
     * This method returns whether token caching is enabled via system property sdk.cache.token=true
     * @return isTokenCachingEnabled
     */
    public static Boolean isTokenCachingEnabled() {
        return Boolean.parseBoolean(System.getProperty("sdk.cache.token"));
    }

    /**
     * This method returns the standard header size of the NXL files.
     * @return headerSize
     */
    public static int getHeaderSize() {
        return NxlFile.COMPLETE_HEADER_SIZE;
    }

    /**
     * This method returns the block size used by the RMJavaSDK for encryption and decryption.
     * @return blockSize
     */
    public static int getBlockSize() {
        return NxlUtils.BLOCK_SIZE;
    }

    /**
     * This method returns the current version of the RMJavaSDK.
     * @return version
     */
    public static String getVersion() {
        return com.nextlabs.common.BuildConfig.VERSION;
    }

    /**
     * This method prints out the current version of the RMJavaSDK.
     * @param args Does not accept any input.
     */
    public static void main(String[] args) {
        System.out.println("RMJavaSDK v" + getVersion()); //NOPMD
    }

    private TokenGroupDetails getTokenGroupCache(String tokenGroupName) throws NxlException {
        TokenGroupDetails rmsCache = nameKeyedTokenGroupCache.get(tokenGroupName);
        if (rmsCache == null) {
            String rmsUrl = getRMSUrl(tokenGroupName);
            StringBuilder path = new StringBuilder(rmsUrl);
            Properties restProp = new Properties();
            restProp.setProperty("userId", String.valueOf(appId));
            restProp.setProperty("ticket", appKey);
            restProp.setProperty("clientId", clientId);
            restProp.setProperty("platformId", String.valueOf(DeviceType.WEB.getLow()));
            String ret = null;

            try {
                path.append("/rs/tokenGroup/details?tokenGroupName=" + URLEncoder.encode(tokenGroupName, StandardCharsets.UTF_8.name()));
                ret = RestClient.get(path.toString(), restProp, false);
            } catch (IOException e) {
                logger.error("Unable to get details from token group name", e);
                throw new NxlException("Unable to get details from token group name", e);
            }

            JsonResponse resp = JsonResponse.fromJson(ret);
            if (resp.hasError()) {
                throw new NxlException("Unable to parse details from parameters: tokenGroupName: " + tokenGroupName + " with message " + resp.getMessage());
            }

            String retTokenGroupName = resp.getResultAsString("tokenGroupName");
            String membership = resp.getResultAsString("membershipName");

            TokenGroupType tgType = TokenGroupType.values()[resp.getResultAsInt("tokenGroupType", -1)];

            String tenant = "";
            String tenantId = "";
            if (tgType == TokenGroupType.TOKENGROUP_TENANT) {
                tenant = resp.getResultAsString("tenantName");
                tenantId = resp.getResultAsString("tenantId");
            } else {
                tenant = resp.getResultAsString("parentTenantName");
                tenantId = resp.getResultAsString("parentTenantId");
            }
            rmsCache = new TokenGroupDetails(rmsUrl, membership, tenant, tenantId);
            nameKeyedTokenGroupCache.put(retTokenGroupName, rmsCache);
        }
        return rmsCache;
    }

    private TokenGroupDetails getTokenGroupCache(String tenantName, TokenGroupType tgType, String projectName)
            throws NxlException {
        CacheKey key = new CacheKey(tenantName, projectName, tgType);
        TokenGroupDetails rmsCache = detailKeyedTokenGroupCache.get(key);
        if (rmsCache == null) {
            String rmsUrl = getRMSUrl(tenantName);
            StringBuilder path = new StringBuilder(rmsUrl);
            Properties restProp = new Properties();
            restProp.setProperty("userId", String.valueOf(appId));
            restProp.setProperty("ticket", appKey);
            restProp.setProperty("clientId", clientId);
            restProp.setProperty("platformId", String.valueOf(DeviceType.WEB.getLow()));

            path.append("/rs/tokenGroup/search?tokenGroupType=" + tgType.ordinal());

            String ret = null;
            try {
                if (StringUtils.hasText(tenantName)) {
                    path.append("&tenantName=" + URLEncoder.encode(tenantName, StandardCharsets.UTF_8.name()));
                }
                if (tgType == TokenGroupType.TOKENGROUP_PROJECT) {
                    if (!StringUtils.hasText(projectName)) {
                        throw new NxlException("Missing project name parameter");
                    }
                    path.append("&projectName=" + URLEncoder.encode(projectName, StandardCharsets.UTF_8.name()));
                }
                ret = RestClient.get(path.toString(), restProp, false);
            } catch (IOException e) {
                logger.error("Unable to get " + Constants.MEMBERSHIP_ID + " from token group name", e);
                throw new NxlException("Unable to get " + Constants.MEMBERSHIP_ID + " from token group name", e);
            }

            JsonResponse resp = JsonResponse.fromJson(ret);
            if (resp.hasError()) {
                throw new NxlException("Unable to parse " + Constants.MEMBERSHIP_ID + " from parameters: tenantName: " + tenantName + ", tgType: " + tgType.name() + ", projectName: " + projectName + " with message " + resp.getMessage());
            }

            String membership = resp.getResultAsString("membershipName");

            String tenant = "";
            String tenantId = "";
            if (tgType == TokenGroupType.TOKENGROUP_TENANT) {
                tenant = resp.getResultAsString("tenantName");
                tenantId = resp.getResultAsString("tenantId");
            } else {
                tenant = resp.getResultAsString("parentTenantName");
                tenantId = resp.getResultAsString("parentTenantId");
            }
            rmsCache = new TokenGroupDetails(rmsUrl, membership, tenant, tenantId);
            detailKeyedTokenGroupCache.put(key, rmsCache);
        }
        return rmsCache;
    }

    private String getRMSUrl(String tokenGroupName) throws NxlException {
        String path;
        Properties restProp = new Properties();
        String ret;
        try {
            if (StringUtils.hasText(tokenGroupName)) {
                path = routerUrl + "/rs/q/tokenGroupName/" + URLEncoder.encode(tokenGroupName, StandardCharsets.UTF_8.name());
            } else {
                path = routerUrl + "/rs/q/defaultTenant";
            }

            ret = RestClient.get(path, restProp, false);
        } catch (IOException e) {
            logger.error("Unable to get " + Constants.RMS_URL + " from token group name", e);
            throw new NxlException("Unable to get " + Constants.RMS_URL + " for token group name " + tokenGroupName + " from router", e);
        }

        JsonResponse resp = JsonResponse.fromJson(ret);
        if (resp.hasError()) {
            throw new NxlException("Unable to get " + Constants.RMS_URL + " for token group name " + tokenGroupName + " from router, with message " + resp.getMessage());
        }
        return resp.getResultAsString("server");
    }

    private static boolean setFIPSContext() {
        boolean approvedOnly = Boolean.parseBoolean(System.getProperty("org.bouncycastle.fips.approved_only"));
        logger.debug("JVM property org.bouncycastle.fips.approved_only: " + approvedOnly);
        if (!approvedOnly) {
            System.setProperty("org.bouncycastle.fips.approved_only", "true");
            approvedOnly = Boolean.parseBoolean(System.getProperty("org.bouncycastle.fips.approved_only"));
            // Note: to be effective this property needs to be set for the JVM or before the CryptoServicesRegistrar class has loaded. 
            logger.info("Set JVM property org.bouncycastle.fips.approved_only: " + approvedOnly);
        }
        boolean approvedMode = CryptoServicesRegistrar.isInApprovedOnlyMode(); // used to check if mode of operation is approved.
        logger.debug("CryptoServicesRegistrar isInApprovedOnlyMode: " + approvedMode);
        if (!approvedMode) {
            CryptoServicesRegistrar.setApprovedOnlyMode(true);
            approvedMode = CryptoServicesRegistrar.isInApprovedOnlyMode();
            logger.info("Set CryptoServicesRegistrar isInApprovedOnlyMode: " + approvedMode);
        }
        boolean fipsReady = FipsStatus.isReady();
        logger.info("FipsStatus isReady: " + fipsReady); // self tests (statics called by JRE) passed and module is ready only
        logger.info("List of installed Security Providers in order of preference: " + Arrays.toString(Security.getProviders()));
        return approvedOnly && approvedMode && fipsReady;
    }

    private static class CacheKey {

        private String tenantName;
        private String projectName;
        private TokenGroupType tgType;

        public CacheKey(String tenantName, String projectName, TokenGroupType tgType) {
            this.tenantName = tenantName;
            this.tgType = tgType;
            this.projectName = tgType == TokenGroupType.TOKENGROUP_PROJECT && StringUtils.hasText(projectName) ? projectName : null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
            result = prime * result + ((tenantName == null) ? 0 : tenantName.hashCode());
            result = prime * result + ((tgType == null) ? 0 : tgType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey)obj;
            if (projectName == null) {
                if (other.projectName != null) {
                    return false;
                }
            } else if (!projectName.equals(other.projectName)) {
                return false;
            }
            if (tenantName == null) {
                if (other.tenantName != null) {
                    return false;
                }
            } else if (!tenantName.equals(other.tenantName)) {
                return false;
            }
            return tgType == other.tgType;
        }

    }

    private static class TokenGroupDetails {

        private String rmsUrl;
        private String membershipId;
        private String tenantName;
        private String tenantId;

        public TokenGroupDetails(String rmsUrl, String membershipId, String tenantName, String tenantId) {
            this.rmsUrl = rmsUrl;
            this.membershipId = membershipId;
            this.tenantName = tenantName;
            this.tenantId = tenantId;
        }

        public String getRmsUrl() {
            return rmsUrl;
        }

        public String getMembershipId() {
            return membershipId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public String getTenantId() {
            return tenantId;
        }
    }
}
