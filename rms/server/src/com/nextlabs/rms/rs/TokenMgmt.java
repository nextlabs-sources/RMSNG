package com.nextlabs.rms.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.EncryptUtil;
import com.nextlabs.nxl.FileInfo;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalRequest;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.NamedAttributes;
import com.nextlabs.rms.eval.Resource;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.share.SharePersonalMapper;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.MembershipUtil;
import com.nextlabs.rms.util.PolicyEvalUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/token")
public class TokenMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int FILEINFO_SECTION_HEADER_OFFSET = 0x04C8 + 0x0020;
    private static final int FILETAG_SECTION_HEADER_OFFSET = 0x0548 + 0x0020;
    private static final int FILEHEADER_CHECKSUM_SIZE = 0x0020;
    public static final Type FILEINFO_TYPE = new TypeToken<FileInfo>() {
    }.getType();
    private final SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();

    @Secured
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String newTokens(@Context HttpServletRequest request, String json) {
        boolean error = true;
        String tokenGroupName = null;
        UserSession us = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            int count = req.getIntParameter("count", 1);
            count = Math.max(1, Math.min(count, 100));
            String name = req.getParameter("membership");
            String agreement = req.getParameter("agreement");
            us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!StringUtils.hasText(name) || !StringUtils.hasText(agreement)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }
            String filePolicy = Nvl.nvl(req.getParameter("filePolicy"), "");
            String fileTags = Nvl.nvl(req.getParameter("fileTags"), "");
            Boolean prefetch = Nvl.nvl(req.getParameter("prefetch", Boolean.class));

            int protectionType = req.getIntParameter("protectionType", -1);
            if (BooleanUtils.isNotTrue(prefetch)) {
                if (count > 1) {
                    return new JsonResponse(4001, "Generating token for multiple files is not allowed").toJson();
                }
                if ((protectionType != ProtectionType.ADHOC.ordinal() && protectionType != ProtectionType.CENTRAL.ordinal()) || (!StringUtils.hasText(filePolicy) && !StringUtils.hasText(fileTags))) {
                    return new JsonResponse(4002, "Invalid protection type or file policy or file tags").toJson();
                }
            }
            Membership membership = null;
            tokenGroupName = StringUtils.substringAfter(name, "@");
            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(Membership.class);
                criteria.add(Restrictions.eq("name", name));
                criteria.add(Restrictions.eq("status", Membership.Status.ACTIVE));
                membership = (Membership)criteria.uniqueResult();
                if (membership == null) {
                    if (!abacMembershipAllowed(session, tokenGroupName, us)) {
                        return new JsonResponse(403, "Access denied").toJson();
                    }
                } else if (membership.getUser().getId() != us.getUser().getId()) {
                    return new JsonResponse(403, "Access denied").toJson();
                }
            }
            int maintanenceLevel = 0;
            Map<String, JsonObject> tokens = generateToken(tokenGroupName, new BigInteger(agreement, 16), maintanenceLevel, count, prefetch);

            try (DbSession session = DbSession.newSession()) {
                session.beginTransaction();
                for (Map.Entry<String, JsonObject> entry : tokens.entrySet()) {
                    String duid = entry.getKey();
                    JsonObject tokenObj = entry.getValue();
                    final Date now = new Date();
                    if (prefetch) {
                        String otp = tokenObj.get("otp").getAsString();
                        storeNxlMetadata(session, duid, name, "", "", ProtectionType.CENTRAL, prefetch, otp, now);
                    } else {
                        storeNxlMetadata(session, duid, name, filePolicy, fileTags, ProtectionType.values()[protectionType], prefetch, null, now);
                    }
                }
                session.commit();
            }

            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("ml", String.valueOf(maintanenceLevel));
            resp.putResult("tokens", tokens);
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | InvalidKeyException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (TokenGroupNotFoundException e) {
            return new JsonResponse(404, "Tenant is not supported on this server").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "TokenMgmt", "newTokens", error ? 0 : 1, us != null ? us.getUser().getId() : null, tokenGroupName);
        }
    }

    public static Map<String, JsonObject> generateToken(String tokenGroupName, BigInteger agreement,
        int maintanenceLevel,
        int count, boolean prefetch)
            throws GeneralSecurityException, TokenGroupNotFoundException, IOException {
        if (count <= 0) {
            throw new IllegalArgumentException("Invalid count");
        }
        KeyManager km = new KeyManager(new KeyStoreManagerImpl());

        String dhAlias = IKeyStoreManager.PREFIX_DH + tokenGroupName;
        PrivateKey privKey = (PrivateKey)km.getKey(tokenGroupName, dhAlias);
        Certificate[] chain = km.getCertificateChain(tokenGroupName, dhAlias);
        if (privKey == null) {
            throw new TokenGroupNotFoundException(tokenGroupName);
        }

        Certificate root = chain[chain.length - 1];
        // Need force convert root public to DHPublicKey
        PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
        PublicKey agreeKey = KeyManager.createDHPublicKey(agreement);

        SecretKey uek = KeyManager.createAgreement(privKey, rootPubKey, agreeKey);
        SecretKey hsk = km.getSecretKey(tokenGroupName, IKeyStoreManager.PREFIX_HSK + tokenGroupName);
        Map<String, JsonObject> tokens = new HashMap<String, JsonObject>(count);
        try (DbSession session = DbSession.newSession()) {
            for (int i = 0; i < count; ++i) {
                byte[] duid;
                int attempts = 0;
                while (true) {
                    if (attempts == 5) {
                        throw new KeyStoreException("Attempting to generate unique duid but collided for 5 times.");
                    }
                    duid = KeyManager.randomBytes(16);
                    NxlMetadata nxl = session.get(NxlMetadata.class, Hex.toHexString(duid));
                    if (nxl == null) {
                        break;
                    }
                    attempts++;
                }
                byte[] token = KeyManager.createToken(uek, hsk, duid, maintanenceLevel);
                JsonObject obj = new JsonObject();
                if (prefetch) {
                    obj.addProperty("otp", Hex.toHexString(KeyManager.randomBytes(16)));
                }
                obj.addProperty("token", Hex.toHexString(token));
                tokens.put(Hex.toHexString(duid), obj);
            }
        }
        return tokens;
    }

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getToken(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        String tenant = null;
        String duid = null;
        DbSession session = DbSession.newSession();
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            tenant = req.getParameter("tenant");
            String agreement = req.getParameter("agreement");
            int maintanenceLevel = req.getIntParameter("ml", 0);
            duid = req.getParameter("duid");
            String owner = req.getParameter("owner");
            String clientFilePolicy = Nvl.nvl(req.getParameter("filePolicy"), "");
            String clientFileTags = Nvl.nvl(req.getParameter("fileTags"), "");
            int clientProtectionType = req.getIntParameter("protectionType", -1);
            EvalRequest evalReq = Nvl.nvl(req.getParameter("dynamicEvalRequest", EvalRequest.class), new EvalRequest());
            evalReq.setPerformObligations(false);
            String sharedSpaceType = req.getParameter("sharedSpaceType");
            String sharedSpaceId = req.getParameter("sharedSpaceId");
            String sharedSpaceUserMembership = req.getParameter("sharedSpaceUserMembership");

            if (!StringUtils.hasText(tenant) || !StringUtils.hasText(agreement) || !StringUtils.hasText(duid) || !StringUtils.hasText(owner)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            if (evalReq.getHost() != null && evalReq.getHost().getAttributes() == null) {
                evalReq.getHost().setAttributes(Maps.newHashMap());
            }

            // process environment attributes with dont-care-acceptable
            if (evalReq.getEnvironments() == null) {
                evalReq.setEnvironments(new NamedAttributes[0]);
            }
            Optional<NamedAttributes> environment = Arrays.stream(evalReq.getEnvironments()).filter(env -> EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME.equalsIgnoreCase(env.getName())).findFirst();
            if (!environment.isPresent()) {
                NamedAttributes env = new NamedAttributes(EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME);
                env.addAttribute(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE, "yes");
                List<NamedAttributes> currentEnvironments = Lists.newArrayList(evalReq.getEnvironments());
                currentEnvironments.add(env);
                evalReq.setEnvironments(currentEnvironments.toArray(new NamedAttributes[currentEnvironments.size()]));
            } else {
                if (environment.get().getAttributes() == null) {
                    environment.get().setAttributes(Maps.newHashMap());
                }

                if (!environment.get().getAttributes().containsKey(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE)) {
                    environment.get().addAttribute(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE, "yes");
                }
            }

            NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, duid);
            if (nxlMetadataDB == null) {
                return new JsonResponse(4000, "Unverified metadata for duid").toJson();
            }
            if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.INACTIVE)) {
                return new JsonResponse(4001, "duid is inactive").toJson();
            } else if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.REVOKED)) {
                return new JsonResponse(4002, "duid is revoked").toJson();
            }
            if ((clientProtectionType != ProtectionType.ADHOC.ordinal() && clientProtectionType != ProtectionType.CENTRAL.ordinal()) || clientProtectionType != nxlMetadataDB.getProtectionType().ordinal()) {
                return new JsonResponse(4003, "Invalid or incorrect protection type as per saved metadata").toJson();
            }
            if (!nxlMetadataDB.getOwner().equals(owner)) {
                return new JsonResponse(4006, "Invalid owner as per saved metadata").toJson();
            }

            MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
            String clientFilePolicyChecksum = Hex.toHexString(md.digest(clientFilePolicy.getBytes((StandardCharsets.UTF_8))));
            String clientFileTagsChecksum = Hex.toHexString(md.digest(clientFileTags.getBytes((StandardCharsets.UTF_8))));
            if (clientProtectionType == ProtectionType.ADHOC.ordinal()) {
                if (!nxlMetadataDB.getFilePolicyChecksum().equals(clientFilePolicyChecksum)) {
                    return new JsonResponse(4004, "Incorrect file policy as per saved metadata").toJson();
                }
            } else if (!nxlMetadataDB.getFileTagsChecksum().equals(clientFileTagsChecksum)) {
                return new JsonResponse(4005, "Incorrect file tags as per saved metadata").toJson();
            }

            KeyManager km = new KeyManager(new KeyStoreManagerImpl());

            String ownerTokenGroupName = StringUtils.substringAfter(owner, "@");
            String parentTenantName = null;
            TokenGroupManager ownerTokenGroupManager = null;
            try {
                ownerTokenGroupManager = TokenGroupManager.newInstance(session, ownerTokenGroupName, us.getLoginTenant());
                parentTenantName = ownerTokenGroupManager.getParentTenantName();
            } catch (TokenGroupException e) {
                return new JsonResponse(404, e.getMessage()).toJson();
            }

            String dhAlias = IKeyStoreManager.PREFIX_DH + ownerTokenGroupName;
            PrivateKey privKey = (PrivateKey)km.getKey(ownerTokenGroupName, dhAlias);
            if (privKey == null) {
                return new JsonResponse(404, "Invalid token group name").toJson();
            }

            boolean isFileSharedToProject = false;
            boolean isTenantTokenGroup = false;
            boolean isIndividualFileOwner = isOwner(userId, owner);
            if (km.getKey(tenant, IKeyStoreManager.PREFIX_DH + tenant) == null) {
                // user is on different rms server
                try {
                    JsonResponse resp = UserMgmt.getRemoteUserProfile(userId, ticket, tenant, clientId, platformId);
                    if (resp.hasError()) {
                        return resp.toJson();
                    }

                    List<String> emails = resp.getResult("emails", GsonUtils.GENERIC_LIST_TYPE);
                    Membership membership = null;
                    switch (ownerTokenGroupManager.getGroupType()) {

                        case TOKENGROUP_SYSTEMBUCKET:
                            break;
                        case TOKENGROUP_PROJECT:
                            membership = ownerTokenGroupManager.getStaticMembership(session, userId);
                            if (sharedSpaceType != null && sharedSpaceId != null && Integer.parseInt(sharedSpaceType) == 1 && StringUtils.hasText(sharedSpaceUserMembership)) {
                                isFileSharedToProject = SharedFileManager.isRecipientProject(duid, Integer.parseInt(sharedSpaceId), session);
                            }
                            if (membership == null && !abacMembershipAllowed(session, ownerTokenGroupName, us) && !isFileSharedToProject) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Unable to find dynamic membership for user: {}", userId);
                                }
                                return new JsonResponse(404, "Not found.").toJson();
                            }
                            //	Project files both adhoc and central policy
                            if (SharedFileManager.isBlacklisted(session, duid)) {
                                return new JsonResponse(403, "Access denied").toJson();
                            }
                            break;
                        case TOKENGROUP_TENANT:
                            //	MyVault and Shared files
                            isTenantTokenGroup = true;
                            if (!isIndividualFileOwner) {
                                if (SharedFileManager.isBlacklisted(session, duid)) {
                                    return new JsonResponse(403, "Access denied").toJson();
                                } else if (!(new SharePersonalMapper().getRecipientList(session, duid, emails)).isEmpty()) {
                                    return new JsonResponse(403, "Unauthorized").toJson();
                                }
                            }
                            break;
                        default:
                            return new JsonResponse(404, "Invalid or unknown token group name").toJson();
                    }
                } catch (IOException e) {
                    return new JsonResponse(503, "Failed connect to remote rms").toJson();
                }
            } else {
                User user = us.getUser();
                Membership membership = null;
                switch (ownerTokenGroupManager.getGroupType()) {

                    case TOKENGROUP_SYSTEMBUCKET:
                        break;
                    case TOKENGROUP_PROJECT:
                        membership = ownerTokenGroupManager.getStaticMembership(session, userId);
                        if (sharedSpaceType != null && sharedSpaceId != null && Integer.parseInt(sharedSpaceType) == 1 && StringUtils.hasText(sharedSpaceUserMembership)) {
                            isFileSharedToProject = SharedFileManager.isRecipientProject(duid, Integer.parseInt(sharedSpaceId), session);
                        }
                        if (membership == null && !abacMembershipAllowed(session, ownerTokenGroupName, us) && !isFileSharedToProject) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Unable to find dynamic membership for user: {}", userId);
                            }
                            return new JsonResponse(404, "Not found.").toJson();
                        }
                        //	Project files both adhoc and central policy
                        if (SharedFileManager.isBlacklisted(session, duid)) {
                            return new JsonResponse(403, "Access denied").toJson();
                        }
                        break;
                    case TOKENGROUP_TENANT:
                        //	MyVault and Shared files
                        isTenantTokenGroup = true;
                        if (!isIndividualFileOwner) {
                            if (SharedFileManager.isBlacklisted(session, duid)) {
                                return new JsonResponse(403, "Access denied").toJson();
                            }
                            Set<String> emails = UserMgmt.getUserEmails(session, user);
                            if ((new SharePersonalMapper().getRecipientList(session, duid, emails)).isEmpty()) {
                                return new JsonResponse(403, "Unauthorized").toJson();
                            }
                        }
                        break;
                    default:
                        return new JsonResponse(404, "Invalid or unknown token group name").toJson();
                }
            }

            if (!UserService.isAPIUserSession(us)) {
                com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(us.getUser().getId())).ticket(ticket).clientId(clientId).platformId(platformId).email(us.getUser().getEmail()).displayName(us.getUser().getDisplayName()).ipAddress(request.getRemoteAddr()).build();
                boolean isOwner = isTenantTokenGroup && isIndividualFileOwner;
                EvalResponse evalResponse = null;
                if (isFileSharedToProject) {
                    // to evaluate policy for the project token group to which file is shared 
                    evalResponse = evaluateTokenPolicy(clientProtectionType, isOwner, sharedSpaceUserMembership, parentTenantName, clientFilePolicy, clientFileTags, userEval, evalReq);
                } else {
                    evalResponse = evaluateTokenPolicy(clientProtectionType, isOwner, owner, parentTenantName, clientFilePolicy, clientFileTags, userEval, evalReq);
                }

                if (!Arrays.asList(evalResponse.getRights()).contains(Rights.VIEW)) {
                    LOGGER.info("Denied token to decrypt " + duid);
                    return new JsonResponse(403, "Unauthorized").toJson();
                }
            }

            Certificate[] chain = km.getCertificateChain(ownerTokenGroupName, dhAlias);
            Certificate root = chain[chain.length - 1];
            // Need force convert root public to DHPublicKey
            PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
            PublicKey agreeKey = KeyManager.createDHPublicKey(new BigInteger(agreement, 16));

            SecretKey uek = KeyManager.createAgreement(privKey, rootPubKey, agreeKey);
            SecretKey hsk = km.getSecretKey(ownerTokenGroupName, IKeyStoreManager.PREFIX_HSK + ownerTokenGroupName);
            byte[] token = KeyManager.createToken(uek, hsk, Hex.toByteArray(duid), maintanenceLevel);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("token", Hex.toHexString(token));

            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | InvalidKeyException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
            Audit.audit(request, "API", "TokenMgmt", "getToken", error ? 0 : 1, userId, tenant, duid);
        }
    }

    private boolean isOwner(int userId, String owner) {
        boolean isOwner = false;

        try (DbSession session = DbSession.newSession()) {
            Membership membership = session.get(Membership.class, owner);
            if (membership != null && membership.getUser() != null && userId == membership.getUser().getId()) {
                isOwner = true;
            }
        }
        return isOwner;
    }

    private EvalResponse evaluateTokenPolicy(int protectionType, boolean isOwner,
        String owner, String parentTenant,
        String clientFilePolicy, String clientFileTags, com.nextlabs.rms.eval.User user, EvalRequest evalReq) {
        if (protectionType == ProtectionType.ADHOC.ordinal()) {
            return AdhocEvalAdapter.evaluate(GsonUtils.GSON.fromJson(clientFilePolicy, FilePolicy.class), isOwner);
        } else if (EvaluationAdapterFactory.isInitialized()) {
            List<EvalRequest> evalRequests = new ArrayList<>();
            String ownerResourceType = TokenGroupCacheManager.getInstance().getResourceType(MembershipUtil.getTokenGroup(owner));
            String parentResourceType = TokenGroupCacheManager.getInstance().getResourceType(MembershipUtil.getTokenGroup(parentTenant));
            evalRequests.add(constructRequest(evalReq, owner, ownerResourceType, clientFileTags, user));
            evalRequests.add(constructRequest(evalReq, owner, parentResourceType, clientFileTags, user));
            List<EvalResponse> evalResponses = CentralPoliciesEvaluationHandler.processRequest(evalRequests);
            return PolicyEvalUtil.getFirstAllowResponse(evalResponses);
        }
        return new EvalResponse();
    }

    @Secured
    @PUT
    @Path("/{duid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateNxlMetadata(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("duid") String duid, String json) {
        boolean error = true;
        boolean holdingLock = false;
        DbSession session = DbSession.newSession();
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            String otp = Nvl.nvl(req.getParameter("otp"), "");
            int clientProtectionType = req.getIntParameter("protectionType", -1);
            String clientFilePolicy = Nvl.nvl(req.getParameter("filePolicy"), "");
            String clientFileTags = Nvl.nvl(req.getParameter("fileTags"), "");
            String existingFileTags = Nvl.nvl(req.getParameter("existingFileTags"), "");

            String fileinfoCheckSum = null;
            String tagCheckSum = null;
            String headerCheckSum = null;

            if (!StringUtils.hasText(duid)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, duid);
            if (nxlMetadataDB == null) {
                return new JsonResponse(4000, "Unverified metadata for duid").toJson();
            }

            boolean isPrefetched = false;
            if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.REVOKED)) {
                return new JsonResponse(4002, "duid is revoked").toJson();
            } else if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.INACTIVE)) {
                isPrefetched = true;
                if (clientProtectionType != ProtectionType.ADHOC.ordinal() && clientProtectionType != ProtectionType.CENTRAL.ordinal()) {
                    return new JsonResponse(4003, "Invalid protection type for duid").toJson();
                }
                if (!nxlMetadataDB.getOtp().equals(otp)) {
                    return new JsonResponse(4003, "Incorrect otp for duid").toJson();
                }
            } else if (!nxlMetadataDB.getStatus().equals(NxlMetadata.Status.ACTIVE)) {
                return new JsonResponse(4004, "Invalid or incorrect status as per saved metadata").toJson();
            }

            if (!isPrefetched && (clientProtectionType != ProtectionType.CENTRAL.ordinal() || clientProtectionType != nxlMetadataDB.getProtectionType().ordinal())) {
                return new JsonResponse(4003, "Invalid or incorrect protection type as per saved metadata").toJson();
            }

            if (clientProtectionType == ProtectionType.CENTRAL.ordinal() && !EncryptUtil.validateTags(clientFileTags)) {
                return new JsonResponse(4008, "Invalid syntax of proposed file policy or file tags").toJson();
            }

            String owner = nxlMetadataDB.getOwner();
            String userMembershipName = owner;
            String ownerTokenGroupName = StringUtils.substringAfter(owner, "@");
            TokenGroupManager ownerTokenGroupManager = null;
            try {
                ownerTokenGroupManager = TokenGroupManager.newInstance(session, ownerTokenGroupName, us.getLoginTenant());
            } catch (TokenGroupException e) {
                return new JsonResponse(404, e.getMessage()).toJson();
            }
            Membership membership = null;
            boolean isSystemBucket = false;
            switch (ownerTokenGroupManager.getGroupType()) {

                case TOKENGROUP_SYSTEMBUCKET:
                    Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
                    isSystemBucket = sbm.isSystemBucket(ownerTokenGroupName, loginTenant.getName());
                    userMembershipName = UserMgmt.generateDynamicMemberName(userId, ownerTokenGroupName);
                    break;
                case TOKENGROUP_PROJECT:
                    membership = ownerTokenGroupManager.getStaticMembership(session, userId);
                    if (membership == null) {
                        if (!abacMembershipAllowed(session, ownerTokenGroupName, us)) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Unable to find dynamic membership for user: {}", userId);
                            }
                            return new JsonResponse(404, "Not found.").toJson();
                        }
                        membership = new Membership();
                        membership.setName(UserMgmt.generateDynamicMemberName(userId, ownerTokenGroupName));
                    }
                    break;
                case TOKENGROUP_TENANT:
                    break;
                default:
                    return new JsonResponse(404, "Invalid or unknown token group name").toJson();
            }

            Date dateModified = new Date();

            if (!isPrefetched) {
                MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
                String existingFileTagsChecksum = Hex.toHexString(md.digest(existingFileTags.getBytes((StandardCharsets.UTF_8))));
                if (nxlMetadataDB.getProtectionType().equals(ProtectionType.ADHOC)) {
                    return new JsonResponse(4005, "Updating adhoc rights is not allowed").toJson();
                } else if (!nxlMetadataDB.getFileTagsChecksum().equals(existingFileTagsChecksum)) {
                    return new JsonResponse(4006, "Incorrect file tags as per saved metadata").toJson();
                }

                Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
                if (!UserService.isAPIUserSession(us) && ((!isSystemBucket && !loginTenant.isProjectAdmin(us.getUser().getEmail())) || (isSystemBucket && !loginTenant.isAdmin(us.getUser().getEmail())))) {
                    LOGGER.info("Denied token to classify " + duid);
                    return new JsonResponse(403, "Unauthorized to classify").toJson();
                }

                // get header, calculate checksum and return to caller
                String fileHeader = req.getParameter("fileHeader");
                byte[] headerBuf = Base64Codec.decode(fileHeader);
                if (headerBuf.length != NxlFile.COMPLETE_HEADER_SIZE) {
                    return new JsonResponse(4008, "Incorrect header size").toJson();
                }
                if (clientProtectionType == ProtectionType.CENTRAL.ordinal()) {
                    try (NxlFile nxl1 = NxlFile.parse(headerBuf);
                            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                            ByteArrayOutputStream baos2 = new ByteArrayOutputStream()) {
                        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
                        String dhAlias = IKeyStoreManager.PREFIX_DH + ownerTokenGroupName;
                        PrivateKey privKey = (PrivateKey)km.getKey(ownerTokenGroupName, dhAlias);
                        if (privKey == null) {
                            return new JsonResponse(404, "Invalid owner tenant").toJson();
                        }
                        Certificate[] chain = km.getCertificateChain(ownerTokenGroupName, dhAlias);
                        Certificate root = chain[chain.length - 1];
                        PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
                        PublicKey agreeKey = KeyManager.createDHPublicKey(new BigInteger(nxl1.getRootAgreement().toString(16), 16));

                        SecretKey uek = KeyManager.createAgreement(privKey, rootPubKey, agreeKey);
                        SecretKey hsk = km.getSecretKey(ownerTokenGroupName, IKeyStoreManager.PREFIX_HSK + ownerTokenGroupName);
                        byte[] token = KeyManager.createToken(uek, hsk, Hex.toByteArray(duid), nxl1.getMaintenanceLevel());
                        FileInfo fileInfo = GsonUtils.GSON.fromJson(new String(nxl1.getSection(".FileInfo").getData(), "UTF-8"), FILEINFO_TYPE);
                        fileInfo.setLastModified(dateModified.getTime());
                        fileInfo.setModifiedBy(isSystemBucket ? userMembershipName : membership.getName());
                        EncryptUtil.updateTags(nxl1, token, clientFileTags, baos1);

                        byte[] buf = baos1.toByteArray();
                        byte[] tagCheckSumBuf = Arrays.copyOfRange(buf, FILETAG_SECTION_HEADER_OFFSET, FILETAG_SECTION_HEADER_OFFSET + FILEHEADER_CHECKSUM_SIZE);

                        /* 
                         * Changes to different parts of the nxl file header cannot  
                         * be done consecutively, to ensure checksums are calculated 
                         * correctly so need to update and re-parse before updating again.
                         */
                        try (NxlFile nxl2 = NxlFile.parse(buf)) {
                            EncryptUtil.updateFileInfo(nxl2, token, GsonUtils.GSON.toJson(fileInfo), baos2);
                            buf = baos2.toByteArray();
                        }

                        byte[] fileinfoCheckSumBuf = Arrays.copyOfRange(buf, FILEINFO_SECTION_HEADER_OFFSET, FILEINFO_SECTION_HEADER_OFFSET + FILEHEADER_CHECKSUM_SIZE);
                        byte[] headerCheckSumBuf = Arrays.copyOfRange(buf, NxlFile.FIXED_HEADER_SIZE, NxlFile.FIXED_HEADER_SIZE + FILEHEADER_CHECKSUM_SIZE);
                        fileinfoCheckSum = Base64Codec.encodeAsString(fileinfoCheckSumBuf);
                        tagCheckSum = Base64Codec.encodeAsString(tagCheckSumBuf);
                        headerCheckSum = Base64Codec.encodeAsString(headerCheckSumBuf);
                    }
                }
            }

            if (!LockManager.getInstance().acquireLock(duid, TimeUnit.MINUTES.toMillis(5))) {
                return new JsonResponse(4007, "Concurrent modification detected.").toJson();
            }

            holdingLock = true;
            session.beginTransaction();
            storeNxlMetadata(session, duid, isPrefetched ? userMembershipName : owner, clientFilePolicy, clientFileTags, ProtectionType.values()[clientProtectionType], false, null, dateModified);
            session.commit();

            JsonResponse resp = new JsonResponse("OK");
            if (StringUtils.hasText(fileinfoCheckSum)) {
                resp.putResult("fileinfoCheckSum", fileinfoCheckSum);
                resp.putResult("dateModified", dateModified.getTime());
            }
            if (StringUtils.hasText(tagCheckSum)) {
                resp.putResult("tagCheckSum", tagCheckSum);
            }
            if (StringUtils.hasText(headerCheckSum)) {
                resp.putResult("headerCheckSum", headerCheckSum);
            }
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (holdingLock && duid != null) {
                LockManager.getInstance().releaseRemoveLock(duid);
            }
            session.close();
            Audit.audit(request, "API", "TokenMgmt", "updateNxlMetadata", error ? 0 : 1, userId, duid);
        }
    }

    private boolean abacMembershipAllowed(DbSession session, String tokenGroupName, UserSession us) {
        Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
        if (sbm.isSystemBucket(tokenGroupName, loginTenant.getName())) {
            return true;
        }
        KeyStoreEntry keyStoreEntry = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
        if (keyStoreEntry != null) {
            Criteria criteria = session.createCriteria(Project.class);
            criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
            criteria.add(Restrictions.eq("status", Project.Status.ACTIVE));
            @SuppressWarnings("unchecked")
            List<Project> projects = criteria.list();
            return (projects != null && !projects.isEmpty() && MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, projects.get(0).getId()));
        }
        return false;
    }

    private void storeNxlMetadata(DbSession session, String duid, String owner, String filePolicy, String fileTags,
        ProtectionType protectionType, boolean prefetch, String otp, Date now) throws GeneralSecurityException {
        if (duid == null || owner == null || filePolicy == null || fileTags == null) {
            throw new InvalidParameterException("Invalid parameters");
        }
        Tenant tenant = TenantMgmt.getTenantByName(session, StringUtils.substringAfter(owner, "@"));
        NxlMetadata metadata = session.get(NxlMetadata.class, duid);
        if (metadata == null) {
            metadata = new NxlMetadata();
            metadata.setCreationTime(now);
        }
        if (tenant != null) {
            metadata.setTokenGroupName(tenant.getId());
        }
        metadata.setDuid(duid);
        metadata.setOwner(owner);
        MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
        byte[] filePolicyChecksum = md.digest(filePolicy.getBytes((StandardCharsets.UTF_8)));
        byte[] fileTagsChecksum = md.digest(fileTags.getBytes((StandardCharsets.UTF_8)));
        metadata.setProtectionType(protectionType);
        metadata.setFilePolicyChecksum(Hex.toHexString(filePolicyChecksum));
        metadata.setFileTagsChecksum(Hex.toHexString(fileTagsChecksum));

        if (prefetch) {
            metadata.setStatus(NxlMetadata.Status.INACTIVE);
            metadata.setOtp(otp);
        } else {
            metadata.setStatus(NxlMetadata.Status.ACTIVE);
        }
        metadata.setLastModified(now);
        session.saveOrUpdate(metadata);
    }

    public static boolean checkNxlMetadata(String duid, String filePolicy, String fileTags,
        ProtectionType protectionType) throws GeneralSecurityException {
        boolean checkNxlMetadata = true;
        try (DbSession session = DbSession.newSession()) {
            NxlMetadata existingMetadata = session.get(NxlMetadata.class, duid);
            if (existingMetadata == null) {
                checkNxlMetadata = false;
            }
            MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
            String currentFilePolicyChecksum = Hex.toHexString(md.digest(filePolicy.getBytes((StandardCharsets.UTF_8))));
            String currentFileTagsChecksum = Hex.toHexString(md.digest(fileTags.getBytes((StandardCharsets.UTF_8))));

            if (protectionType.equals(ProtectionType.ADHOC)) {
                if (!existingMetadata.getFilePolicyChecksum().equals(currentFilePolicyChecksum)) {
                    checkNxlMetadata = false;
                }
            } else if (!existingMetadata.getFileTagsChecksum().equals(currentFileTagsChecksum)) {
                checkNxlMetadata = false;
            }
        }
        return checkNxlMetadata;
    }

    public String getTokenInternal(UserSession us, int userId,
        String ticket, String clientId,
        Integer platformId, String json) {
        String tenant = null;
        String duid = null;
        DbSession session = DbSession.newSession();
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            tenant = req.getParameter("tenant");
            String agreement = req.getParameter("agreement");
            int maintanenceLevel = req.getIntParameter("ml", 0);
            duid = req.getParameter("duid");
            String owner = req.getParameter("owner");
            String clientFilePolicy = Nvl.nvl(req.getParameter("filePolicy"), "");
            String clientFileTags = Nvl.nvl(req.getParameter("fileTags"), "");
            int clientProtectionType = req.getIntParameter("protectionType", -1);
            String sharedSpaceType = req.getParameter("sharedSpaceType");
            String sharedSpaceId = req.getParameter("sharedSpaceId");
            String sharedSpaceUserMembership = req.getParameter("sharedSpaceUserMembership");

            if (!StringUtils.hasText(tenant) || !StringUtils.hasText(agreement) || !StringUtils.hasText(duid) || !StringUtils.hasText(owner)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, duid);
            if (nxlMetadataDB == null) {
                return new JsonResponse(4000, "Unverified metadata for duid").toJson();
            }
            if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.INACTIVE)) {
                return new JsonResponse(4001, "duid is inactive").toJson();
            } else if (nxlMetadataDB.getStatus().equals(NxlMetadata.Status.REVOKED)) {
                return new JsonResponse(4002, "duid is revoked").toJson();
            }
            if ((clientProtectionType != ProtectionType.ADHOC.ordinal() && clientProtectionType != ProtectionType.CENTRAL.ordinal()) || clientProtectionType != nxlMetadataDB.getProtectionType().ordinal()) {
                return new JsonResponse(4003, "Invalid or incorrect protection type as per saved metadata").toJson();
            }
            if (!nxlMetadataDB.getOwner().equals(owner)) {
                return new JsonResponse(4006, "Invalid owner as per saved metadata").toJson();
            }

            MessageDigest md = MessageDigest.getInstance(IKeyStoreManager.ALG_SHA256, "BCFIPS");
            String clientFilePolicyChecksum = Hex.toHexString(md.digest(clientFilePolicy.getBytes((StandardCharsets.UTF_8))));
            String clientFileTagsChecksum = Hex.toHexString(md.digest(clientFileTags.getBytes((StandardCharsets.UTF_8))));
            if (clientProtectionType == ProtectionType.ADHOC.ordinal()) {
                if (!nxlMetadataDB.getFilePolicyChecksum().equals(clientFilePolicyChecksum)) {
                    return new JsonResponse(4004, "Incorrect file policy as per saved metadata").toJson();
                }
            } else if (!nxlMetadataDB.getFileTagsChecksum().equals(clientFileTagsChecksum)) {
                return new JsonResponse(4005, "Incorrect file tags as per saved metadata").toJson();
            }

            KeyManager km = new KeyManager(new KeyStoreManagerImpl());

            String ownerTokenGroupName = StringUtils.substringAfter(owner, "@");
            TokenGroupManager ownerTokenGroupManager = null;
            try {
                ownerTokenGroupManager = TokenGroupManager.newInstance(session, ownerTokenGroupName, us.getLoginTenant());
            } catch (TokenGroupException e) {
                return new JsonResponse(404, e.getMessage()).toJson();
            }

            String dhAlias = IKeyStoreManager.PREFIX_DH + ownerTokenGroupName;
            PrivateKey privKey = (PrivateKey)km.getKey(ownerTokenGroupName, dhAlias);
            if (privKey == null) {
                return new JsonResponse(404, "Invalid token group name").toJson();
            }

            boolean isFileSharedToProject = false;
            boolean isIndividualFileOwner = isOwner(userId, owner);
            if (km.getKey(tenant, IKeyStoreManager.PREFIX_DH + tenant) == null) {
                // user is on different rms server
                try {
                    JsonResponse resp = UserMgmt.getRemoteUserProfile(userId, ticket, tenant, clientId, platformId);
                    if (resp.hasError()) {
                        return resp.toJson();
                    }

                    List<String> emails = resp.getResult("emails", GsonUtils.GENERIC_LIST_TYPE);
                    Membership membership = null;
                    switch (ownerTokenGroupManager.getGroupType()) {

                        case TOKENGROUP_SYSTEMBUCKET:
                            break;
                        case TOKENGROUP_PROJECT:
                            membership = ownerTokenGroupManager.getStaticMembership(session, userId);
                            if (sharedSpaceType != null && sharedSpaceId != null && Integer.parseInt(sharedSpaceType) == 1 && StringUtils.hasText(sharedSpaceUserMembership)) {
                                isFileSharedToProject = SharedFileManager.isRecipientProject(duid, Integer.parseInt(sharedSpaceId), session);
                            }
                            if (membership == null && !abacMembershipAllowed(session, ownerTokenGroupName, us) && !isFileSharedToProject) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Unable to find dynamic membership for user: {}", userId);
                                }
                                return new JsonResponse(404, "Not found.").toJson();
                            }
                            //	Project files both adhoc and central policy
                            if (SharedFileManager.isBlacklisted(session, duid)) {
                                return new JsonResponse(403, "Access denied").toJson();
                            }
                            break;
                        case TOKENGROUP_TENANT:
                            //	MyVault and Shared files
                            if (!isIndividualFileOwner) {
                                if (SharedFileManager.isBlacklisted(session, duid)) {
                                    return new JsonResponse(403, "Access denied").toJson();
                                } else if (!(new SharePersonalMapper().getRecipientList(session, duid, emails)).isEmpty()) {
                                    return new JsonResponse(403, "Unauthorized").toJson();
                                }
                            }
                            break;
                        default:
                            return new JsonResponse(404, "Invalid or unknown token group name").toJson();
                    }
                } catch (IOException e) {
                    return new JsonResponse(503, "Failed connect to remote rms").toJson();
                }
            } else {
                User user = us.getUser();
                Membership membership = null;
                switch (ownerTokenGroupManager.getGroupType()) {

                    case TOKENGROUP_SYSTEMBUCKET:
                        break;
                    case TOKENGROUP_PROJECT:
                        membership = ownerTokenGroupManager.getStaticMembership(session, userId);
                        if (sharedSpaceType != null && sharedSpaceId != null && Integer.parseInt(sharedSpaceType) == 1 && StringUtils.hasText(sharedSpaceUserMembership)) {
                            isFileSharedToProject = SharedFileManager.isRecipientProject(duid, Integer.parseInt(sharedSpaceId), session);
                        }
                        if (membership == null && !abacMembershipAllowed(session, ownerTokenGroupName, us) && !isFileSharedToProject) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Unable to find dynamic membership for user: {}", userId);
                            }
                            return new JsonResponse(404, "Not found.").toJson();
                        }
                        //	Project files both adhoc and central policy
                        if (SharedFileManager.isBlacklisted(session, duid)) {
                            return new JsonResponse(403, "Access denied").toJson();
                        }
                        break;
                    case TOKENGROUP_TENANT:
                        //	MyVault and Shared files
                        if (!isIndividualFileOwner) {
                            if (SharedFileManager.isBlacklisted(session, duid)) {
                                return new JsonResponse(403, "Access denied").toJson();
                            }
                            Set<String> emails = UserMgmt.getUserEmails(session, user);
                            if ((new SharePersonalMapper().getRecipientList(session, duid, emails)).isEmpty()) {
                                return new JsonResponse(403, "Unauthorized").toJson();
                            }
                        }
                        break;
                    default:
                        return new JsonResponse(404, "Invalid or unknown token group name").toJson();
                }
            }

            Certificate[] chain = km.getCertificateChain(ownerTokenGroupName, dhAlias);
            Certificate root = chain[chain.length - 1];
            // Need force convert root public to DHPublicKey
            PublicKey rootPubKey = KeyUtils.readPublicKey(root.getPublicKey().getEncoded(), "DH");
            PublicKey agreeKey = KeyManager.createDHPublicKey(new BigInteger(agreement, 16));

            SecretKey uek = KeyManager.createAgreement(privKey, rootPubKey, agreeKey);
            SecretKey hsk = km.getSecretKey(ownerTokenGroupName, IKeyStoreManager.PREFIX_HSK + ownerTokenGroupName);
            byte[] token = KeyManager.createToken(uek, hsk, Hex.toByteArray(duid), maintanenceLevel);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("token", Hex.toHexString(token));
            return resp.toJson();
        } catch (IllegalArgumentException | InvalidKeyException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
        }
    }

    private EvalRequest constructRequest(EvalRequest evalReq, String owner, String resourceType, String clientFileTags,
        com.nextlabs.rms.eval.User user) {
        evalReq = new EvalRequest(evalReq);
        @SuppressWarnings("unchecked")
        Map<String, String[]> tags = (Map<String, String[]>)(clientFileTags.isEmpty() ? Collections.<String, String[]> emptyMap() : GsonUtils.GSON.fromJson(clientFileTags, GsonUtils.STRING_ARRAY_MAP_TYPE));

        evalReq.setAdhocPolicy("");
        evalReq.setEvalType(0);
        evalReq.setMembershipId(owner);

        String cacheKey = UserAttributeCacheItem.getKey(Integer.parseInt(user.getId()), user.getClientId());
        UserAttributeCacheItem userAttrItem = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
        if (userAttrItem != null) {
            if (user.getAttributes() != null) {
                user.getAttributes().putAll(userAttrItem.getUserAttributes());
            } else {
                user.setAttributes(userAttrItem.getUserAttributes());
            }
        }
        Resource resource = CentralPoliciesEvaluationHandler.getResource("token", EvalRequest.ATTRIBVAL_RES_DIMENSION_FROM, resourceType);
        resource.setClassification(tags);
        Resource[] resources = { resource };
        evalReq.setResources(resources);
        evalReq.setUser(user);
        return evalReq;
    }
}
