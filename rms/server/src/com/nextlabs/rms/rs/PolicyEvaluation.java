package com.nextlabs.rms.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonMembership;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalRequest;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.NamedAttributes;
import com.nextlabs.rms.eval.Obligation;
import com.nextlabs.rms.eval.Resource;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.MembershipUtil;
import com.nextlabs.rms.util.PolicyEvalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/policyEval")
public class PolicyEvaluation {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final EvalRequest.EvalType[] EVAL_TYPE_VALUES = EvalRequest.EvalType.values();

    @Secured
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String policyEvaluation(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("deviceId") String deviceId, String json) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            EvalRequest evalReq = req.getParameter("evalRequest", EvalRequest.class);
            if (evalReq == null || evalReq.getResources() == null || evalReq.getUser() == null || evalReq.getMembershipId() == null || evalReq.getEvalType() == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            if (evalReq.getEvalType() < 0 || evalReq.getEvalType() >= EVAL_TYPE_VALUES.length) {
                return new JsonResponse(400, "Invalid Evaluation type.").toJson();
            }

            String membershipId = evalReq.getMembershipId();
            List<JsonMembership> memberships = UserMgmt.getMemberships(session, us.getUser().getId(), null, us);
            Set<String> membershipIds = memberships.stream().map(JsonMembership::getId).collect(Collectors.toSet());
            if (!membershipIds.contains(membershipId)) {
                return new JsonResponse(404, "Invalid or unknown token group name").toJson();
            }

            String tokenGroup = MembershipUtil.getTokenGroup(membershipId);
            String resourceType = TokenGroupCacheManager.getInstance().getResourceType(tokenGroup);
            for (Resource resource : evalReq.getResources()) {
                resource.setResourceType(resourceType);
            }

            if (EVAL_TYPE_VALUES[evalReq.getEvalType()] == EvalRequest.EvalType.CENTRAL_ONLY) {
                String cacheKey = UserAttributeCacheItem.getKey(us.getUser().getId(), us.getClientId());
                UserAttributeCacheItem userAttrItem = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
                if (userAttrItem != null) {
                    if (evalReq.getUser().getAttributes() != null) {
                        evalReq.getUser().getAttributes().putAll(userAttrItem.getUserAttributes());
                    } else {
                        evalReq.getUser().setAttributes(userAttrItem.getUserAttributes());
                    }
                }
                if (!StringUtils.hasText(evalReq.getUser().getEmail())) {
                    evalReq.getUser().setEmail(us.getUser().getEmail());
                }
            } else {
                return new JsonResponse(5003, "Evaluation type not supported yet").toJson();
            }

            if (evalReq.getHost() != null && evalReq.getHost().getAttributes() == null) {
                evalReq.getHost().setAttributes(Maps.newHashMap());
            }

            processEnvironments(evalReq);

            List<EvalRequest> evalRequests = Lists.newArrayList(evalReq);

            Tenant parentTenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroup);
            String parentResourceType = tokenGroup.equals(parentTenant.getName()) ? null : TokenGroupCacheManager.getInstance().getResourceType(parentTenant.getName());
            if (parentResourceType != null) {
                EvalRequest parentEvalReq = req.getParameter("evalRequest", EvalRequest.class);

                String cacheKey = UserAttributeCacheItem.getKey(us.getUser().getId(), us.getClientId());
                UserAttributeCacheItem userAttrItem = (UserAttributeCacheItem)RMSCacheManager.getInstance().getUserAttributeCache().get(cacheKey);
                if (userAttrItem != null) {
                    if (parentEvalReq.getUser().getAttributes() != null) {
                        parentEvalReq.getUser().getAttributes().putAll(userAttrItem.getUserAttributes());
                    } else {
                        parentEvalReq.getUser().setAttributes(userAttrItem.getUserAttributes());
                    }
                }
                if (!StringUtils.hasText(parentEvalReq.getUser().getEmail())) {
                    parentEvalReq.getUser().setEmail(us.getUser().getEmail());
                }

                if (parentEvalReq.getHost() != null && parentEvalReq.getHost().getAttributes() == null) {
                    parentEvalReq.getHost().setAttributes(Maps.newHashMap());
                }

                processEnvironments(parentEvalReq);
                for (Resource resource : parentEvalReq.getResources()) {
                    resource.setResourceType(parentResourceType);
                }
                evalRequests.add(parentEvalReq);
            }

            List<EvalResponse> responseList = CentralPoliciesEvaluationHandler.processRequest(evalRequests);
            EvalResponse evalResp = PolicyEvalUtil.getFirstAllowResponse(responseList);

            // filter rights from response that matches the "rights" param in request
            Rights[] validRights = evalResp.getRights();
            JsonElement rightsElement = req.getWrappedParameter("evalRequest").getAsJsonObject().get("rights");
            if (rightsElement != null) {
                Integer rightsFromReq = rightsElement.getAsInt();
                if (rightsFromReq != null) {
                    List<Rights> rights = new ArrayList<>();
                    List<Rights> rightsFromResponse = Lists.newArrayList(PolicyEvalUtil.getUnionRights(responseList));
                    for (Rights rightFromReq : Rights.fromInt(rightsFromReq)) {
                        if (rightsFromResponse.contains(rightFromReq)) {
                            rights.add(rightFromReq);
                        }
                    }
                    validRights = rights.toArray(new Rights[rights.size()]);
                }
            }

            List<Obligation> obligations = new ArrayList<>();
            for (EvalResponse er : responseList) {
                if (er.getObligations() != null && !er.getObligations().isEmpty()) {
                    obligations = er.getObligations();
                    break;
                }
            }

            JsonResponse resp = new JsonResponse("Policy Evaluated");
            resp.putResult("protectionType", ProtectionType.CENTRAL.ordinal());
            resp.putResult("rights", Rights.toInt(validRights));
            resp.putResult("obligations", obligations);
            resp.putResult("adhocObligations", evalResp.getAdhocObligations());
            error = false;
            return resp.toJson();
        } catch (IllegalArgumentException | JsonParseException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "PolicyEval", "evaluate", error ? 0 : 1);
        }
    }

    private void processEnvironments(EvalRequest evalReq) {
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
    }
}
