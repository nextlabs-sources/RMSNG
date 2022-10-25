package com.nextlabs.rms.eval;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.util.MembershipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CentralPoliciesEvaluationHandler {

    public static final String LOCALHOST_IP = "127.0.0.1"; //NOPMD
    private static List<String> actions;

    static {
        actions = new ArrayList<String>(Rights.all().length) {

            private static final long serialVersionUID = 1L;
            {
                for (Rights rights : Rights.all()) {
                    if (rights.equals(Rights.ACCESS_PROJECT)) {
                        continue;
                    }
                    add(rights.toString());
                }
            }
        };
    }

    private CentralPoliciesEvaluationHandler() {

    }

    public static List<EvalResponse> processRequest(List<EvalRequest> req) {
        IEvalAdapter evalAdapter = EvaluationAdapterFactory.getInstance().getAdapter();
        String uniqueAttribute = null;
        for (EvalRequest request : req) {
            User evalUser = new User(request.getUser());
            Map<String, List<String>> userAttributes = evalUser.getAttributes();
            if (userAttributes.containsKey(UserAttributeCacheItem.ADPASS)) {
                uniqueAttribute = userAttributes.remove(UserAttributeCacheItem.ADPASS).get(0);
            }
            if (userAttributes.containsKey(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE)) {
                uniqueAttribute = userAttributes.get(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE).get(0);
            }
            if (uniqueAttribute != null) {
                for (Map.Entry<String, List<String>> entry : userAttributes.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(uniqueAttribute)) {
                        evalUser.setId(entry.getValue().get(0));
                        break;
                    }
                }
            }
            request.setUser(evalUser);
        }
        return evalAdapter.evaluate(req);
    }

    public static Resource getResource(String fileName, String resDimension, String resourceType) {
        return new Resource(resDimension, fileName, resourceType);
    }

    public static List<EvalResponse> evaluatePolicy(String fileName, User user, Map<String, String[]> attribs,
        String membership, List<String> resourceTypes, boolean pdpPerformObligation, String appName) {
        List<EvalRequest> requests = new ArrayList<>();
        Set<String> seenResourceTypes = new HashSet<>();
        for (String resourceType : resourceTypes) {
            if (seenResourceTypes.contains(resourceType)) {
                continue;
            }
            seenResourceTypes.add(resourceType);
            EvalRequest evalRequest = new EvalRequest();
            evalRequest.setAdhocPolicy("");
            evalRequest.setEvalType(0);
            evalRequest.setMembershipId(membership);

            String cacheKey = UserAttributeCacheItem.getKey(Integer.parseInt(user.getId()), user.getClientId());
            UserAttributeCacheItem userAttrItem = RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
            if (userAttrItem != null) {
                if (user.getAttributes() != null) {
                    user.getAttributes().putAll(userAttrItem.getUserAttributes());
                } else {
                    user.setAttributes(userAttrItem.getUserAttributes());
                }
            }

            evalRequest.setHost(new Host(StringUtils.hasText(user.getIpAddress()) ? user.getIpAddress() : LOCALHOST_IP, null));
            Resource resource = getResource(fileName, EvalRequest.ATTRIBVAL_RES_DIMENSION_FROM, resourceType);
            resource.setClassification(attribs);
            Resource[] resources = { resource };
            evalRequest.setResources(resources);
            evalRequest.setUser(user);
            evalRequest.setApplication(new Application(appName));

            NamedAttributes[] envAttrs = new NamedAttributes[1];
            envAttrs[0] = new NamedAttributes(EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME);
            envAttrs[0].addAttribute(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE, "yes");
            evalRequest.setEnvironments(envAttrs);
            evalRequest.setPerformObligations(pdpPerformObligation);
            requests.add(evalRequest);
        }
        return processRequest(requests);
    }

    private static List<EvalResponse> evaluate(InputStream is, String fileName, User user, String membership,
        String parentTokenGroup, boolean pdpPerformObligation, String appName)
            throws PolicyEvalException {
        try {
            final int length = NxlFile.COMPLETE_HEADER_SIZE;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(is, os, 0, (long)length);
            byte[] nxlMetadata = os.toByteArray();
            return NxlFile.isNxl(nxlMetadata) ? evaluate(nxlMetadata, fileName, user, membership, parentTokenGroup, pdpPerformObligation, appName) : Collections.singletonList(getAllRights());
        } catch (IOException | NxlException | GeneralSecurityException e) {
            throw new PolicyEvalException(e.getMessage(), e);
        }
    }

    public static List<EvalResponse> evaluate(File fileToEvaluate, User user, String membership,
        String parentTokenGroup, boolean pdpPerformObligation, String appName) {
        try (InputStream is = new FileInputStream(fileToEvaluate)) {
            return evaluate(is, fileToEvaluate.getName(), user, membership, parentTokenGroup, pdpPerformObligation, appName);
        } catch (IOException | PolicyEvalException e) {
            return Collections.singletonList(new EvalResponse());
        }
    }

    private static List<EvalResponse> evaluate(byte[] metadata, String filename, User user, String membership,
        String parentTokenGroup, boolean pdpPerformObligation, String appName)
            throws IOException,
            NxlException, GeneralSecurityException {
        try (NxlFile nxlMetaData = NxlFile.parse(metadata)) {
            String tokenGroup = MembershipUtil.getTokenGroup(membership);
            TokenGroupCacheManager cacheManager = TokenGroupCacheManager.getInstance();
            String resourceType = cacheManager.getResourceType(tokenGroup);
            String parentTokenGroupResourceType = cacheManager.getResourceType(parentTokenGroup);
            Map<String, String[]> tagMap = DecryptUtil.getTags(nxlMetaData, null);
            return evaluatePolicy(filename, user, tagMap, membership, Arrays.asList(resourceType, parentTokenGroupResourceType), pdpPerformObligation, appName);
        }
    }

    public static EvalResponse evaluateMembershipPolicy(User user, String tenantName, String resourceType) {
        Map<String, String[]> attributes = new HashMap<>();
        attributes.put("tenantId", new String[] { WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT) });

        List<EvalResponse> responses = evaluatePolicy("filenameForMembershipPolicy", user, attributes, tenantName, Collections.singletonList(resourceType), true, EvalRequest.ATTRIBVAL_RMS_APP_NAME);
        return responses.isEmpty() ? new EvalResponse() : responses.get(0);
    }

    public static List<EvalResponse> getCentralPolicyEvaluationResponse(String fileName, String membership,
        String parentTenantName, User user,
        Map<String, String[]> tagMap) {
        List<EvalResponse> evalResponses = new ArrayList<>();
        if (EvaluationAdapterFactory.isInitialized()) {
            List<String> resourceTypes = new ArrayList<>();
            String tokenGroup = MembershipUtil.getTokenGroup(membership);
            resourceTypes.add(TokenGroupCacheManager.getInstance().getResourceType(tokenGroup));
            if (parentTenantName != null && !parentTenantName.isEmpty()) {
                resourceTypes.add(TokenGroupCacheManager.getInstance().getResourceType(parentTenantName));
            }
            evalResponses = CentralPoliciesEvaluationHandler.evaluatePolicy(fileName, user, tagMap, membership, resourceTypes, true, EvalRequest.ATTRIBVAL_RMS_APP_NAME);
        }
        return evalResponses;
    }

    public static List<String> getEvaluatedRights() {
        return actions;
    }

    public static EvalResponse getAllRights() {
        List<String> rightsList = getEvaluatedRights();
        Rights[] rights = Rights.fromStrings(rightsList);
        return new EvalResponse(rights);
    }

    public static int ipAddressToInteger(String dottedNotation) throws PolicyEvalException {
        String[] octets = dottedNotation.split("\\.");
        if (octets.length != 4) {
            throw new PolicyEvalException("Invalid IP address: " + dottedNotation);
        }
        int res = 0;
        for (String octet : octets) {
            try {
                int o = Integer.parseInt(octet);
                if (o < 0 || o > 255) {
                    throw new PolicyEvalException("Invalid IP address: " + dottedNotation);
                }
                res = (res << 8) + o;
            } catch (NumberFormatException nfe) {
                throw new PolicyEvalException("Invalid IP address: " + dottedNotation, nfe);
            }
        }
        return res;
    }

}
