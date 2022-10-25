package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.PolicyModelType;
import com.nextlabs.common.shared.JsonABACMembershipObligation;
import com.nextlabs.common.shared.JsonClassificationCategory;
import com.nextlabs.common.shared.JsonClassificationCategory.Label;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonTenantUserAttr;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.cc.pojos.ControlCenterAttribute;
import com.nextlabs.rms.cc.pojos.ControlCenterComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest.ControlCenterIdWrapper;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterOperator;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.JsonAdvancedCondnComponent;
import com.nextlabs.rms.cc.pojos.JsonCondition;
import com.nextlabs.rms.cc.pojos.JsonPolicy;
import com.nextlabs.rms.cc.pojos.JsonPolicyComponent;
import com.nextlabs.rms.cc.pojos.JsonResourceComponent;
import com.nextlabs.rms.cc.service.ControlCenterComponentService;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.cc.service.ControlCenterModelService;
import com.nextlabs.rms.cc.service.ControlCenterPolicyService;
import com.nextlabs.rms.cc.service.ControlCenterRestClient;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterResponse;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ResourceAlreadyExistsException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.PolicyComponent;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/policy")
public class PolicyMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String MODULE_NAME = "PolicyMgmt";

    @Secured
    @GET
    @Path("/{tokenGroupName}/policies/models")
    @Produces(MediaType.APPLICATION_JSON)
    public String getModels(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
            ControlCenterPolicyModel subject = new ControlCenterPolicyModel();
            ControlCenterPolicyModel resource = modelService.getResourceModel(policyClientTgName);

            List<JsonClassificationCategory> categories;
            List<JsonTenantUserAttr> attributes;
            categories = !tenant.getKeystore().getTokenGroupName().equals(tokenGroupName) ? ClassificationMgmt.getProjectClassification(session, tokenGroupName) : ClassificationMgmt.getTenantClassification(session, tokenGroupName);
            attributes = TenantMgmt.getUserAttrList(session, StringUtils.hasText(tenant.getParentId()) ? tenant.getParentId() : tenant.getId(), true);

            if (resource != null) {
                for (ControlCenterAttribute attr : resource.getAttributes()) {
                    // if the attribute data type is string, limit to only string operators
                    if ("STRING".equals(attr.getDataType())) {
                        List<ControlCenterOperator> filteredOperatorConfigs = Arrays.stream(attr.getOperatorConfigs()).filter(operator -> "STRING".equals(operator.getDataType())).collect(Collectors.toList());
                        attr.setOperatorConfigs(filteredOperatorConfigs.toArray(new ControlCenterOperator[filteredOperatorConfigs.size()]));
                    }
                    for (JsonClassificationCategory category : categories) {
                        if (category.getName().equals(attr.getName())) {
                            String[] values = new String[category.getLabels().size()];
                            int i = 0;
                            for (Label label : category.getLabels()) {
                                values[i++] = label.getName();
                            }
                            attr.setValues(values);
                            break;
                        }
                    }
                }
            }

            List<ControlCenterAttribute> userAttributes = new ArrayList<>();
            for (JsonTenantUserAttr attr : attributes) {
                userAttributes.add(ControlCenterManager.toControlCenterAttribute(attr.getName()));
            }
            List<ControlCenterAttribute> userAttributesFromCC = new ArrayList<>(Arrays.asList(ControlCenterManager.getDefaultSubjectAttributes(tokenGroupName)));

            // get attributes where the short name contains "_" from User component in CC - this is to support DAP
            List<ControlCenterAttribute> subjectAttributes = modelService.getSubjectModel().getAttributes();
            for (ControlCenterAttribute attribute : subjectAttributes) {
                if (attribute.getShortName().contains("_")) {
                    userAttributesFromCC.add(attribute);
                }
            }

            for (ControlCenterAttribute userAttribute : userAttributesFromCC) {
                // skip attributes with multival or number datatype
                if ("MULTIVAL".equals(userAttribute.getDataType()) || "NUMBER".equals(userAttribute.getDataType())) {
                    continue;
                }
                userAttributes.add(userAttribute);
            }

            for (ControlCenterAttribute attr : userAttributes) {
                List<ControlCenterOperator> filteredOperatorConfigs = Arrays.stream(attr.getOperatorConfigs()).filter(operator -> "=".equals(operator.getKey()) || "!=".equals(operator.getKey())).collect(Collectors.toList());
                attr.setOperatorConfigs(filteredOperatorConfigs.toArray(new ControlCenterOperator[filteredOperatorConfigs.size()]));
            }

            subject.setAttributes(userAttributes);
            error = false;
            JsonResponse response = new JsonResponse("OK");
            response.putResult("subject", subject);
            response.putResult("resource", resource);
            response.putResult("subjectAttributes", attributes);
            return response.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "getModels", error ? 0 : 1, tokenGroupName);
        }
    }

    @Secured
    @PUT
    @Path("/{tokenGroupName}/policies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName, String json) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            JsonPolicy jsonPolicy = req.getParameter("policy", JsonPolicy.class);
            String name = jsonPolicy.getName();
            if (!validatePolicyParameter(name)) {
                return new JsonResponse(4001, "Policy Name contains illegal special characters").toJson();
            }
            String parsedAdvancedConditions = null;
            JsonAdvancedCondnComponent[] userComponents = jsonPolicy.getUserComponents();
            JsonAdvancedCondnComponent[] applicationComponents = jsonPolicy.getApplicationComponents();
            String advancedConditions = jsonPolicy.getAdvancedConditions();

            if (!StringUtils.hasText(name) || name.length() > 255 || RegularExpressions.POLICY_NAME_INVALIDCHARACTERS_PATTERN.matcher(name).matches()) {
                return new JsonResponse(400, "Missing or invalid required parameters").toJson();
            }

            parsedAdvancedConditions = constructAdvancedConditionsExpression(jsonPolicy);
            boolean forProject = !tenant.getKeystore().getTokenGroupName().equals(tokenGroupName);
            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterResponse response = ControlCenterManager.createPolicy(policyClientTgName, jsonPolicy, parsedAdvancedConditions, membershipPolicy);
            long policyId = getPolicyId(response);
            if (forProject) {
                createPolicyComponents(session, policyId, ProjectService.getProjectByTokenGroupName(session, tokenGroupName), userComponents, applicationComponents, advancedConditions, membershipPolicy);
            } else {
                createPolicyComponents(session, policyId, tenant, userComponents, applicationComponents, advancedConditions, membershipPolicy);
            }
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("response", response);
            return resp.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (ResourceAlreadyExistsException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(409, "Policy with the same name already exists.").toJson();
        } catch (ControlCenterServiceException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(5003, "Unable to propagate to control center").toJson();
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(400, "Invalid request parameters").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "create", error ? 0 : 1, tokenGroupName);
        }
    }

    private long getPolicyId(ControlCenterResponse response) {
        long policyId;
        if (response.getData().isJsonObject()) {
            policyId = response.getData().getAsJsonObject().get("id").getAsLong();
        } else {
            policyId = response.getData().getAsLong();
        }
        return policyId;
    }

    @SuppressWarnings("unchecked")
    @Secured
    @POST
    @Path("/{tokenGroupName}/policies/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName, String json) {
        boolean error = true;
        DbSession session = DbSession.newSession();
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            JsonPolicy jsonPolicy = req.getParameter("policy", JsonPolicy.class);
            String name = jsonPolicy.getName();
            if (!validatePolicyParameter(name)) {
                return new JsonResponse(4001, "Policy Name contains illegal special characters").toJson();
            }
            String parsedAdvancedConditions = null;
            JsonAdvancedCondnComponent[] userComponents = jsonPolicy.getUserComponents();
            JsonAdvancedCondnComponent[] applicationComponents = jsonPolicy.getApplicationComponents();
            String advancedConditions = jsonPolicy.getAdvancedConditions();

            if (jsonPolicy.getId() == null || jsonPolicy.getVersion() == null || !StringUtils.hasText(name) || name.length() > 255 || RegularExpressions.POLICY_NAME_INVALIDCHARACTERS_PATTERN.matcher(name).matches()) {
                return new JsonResponse(400, "Missing or invalid required parameters").toJson();
            }

            parsedAdvancedConditions = constructAdvancedConditionsExpression(jsonPolicy);

            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterResponse response = ControlCenterManager.updatePolicy(policyClientTgName, jsonPolicy, parsedAdvancedConditions, membershipPolicy);
            long policyId = getPolicyId(response);

            Criteria criteria = session.createCriteria(PolicyComponent.class);
            criteria.add(Restrictions.eq("policyId", policyId));
            List<PolicyComponent> policyComponents = criteria.list();
            session.beginTransaction();
            for (PolicyComponent policyComponent : policyComponents) {
                switch (policyComponent.getComponentType()) {

                    case USER:
                        policyComponent.setComponentType(Constants.PolicyComponentType.USER);
                        policyComponent.setComponentJson(userComponents == null ? "[]" : new Gson().toJson(userComponents));
                        session.update(policyComponent);
                        break;

                    case APPLICATION:
                        policyComponent.setComponentType(Constants.PolicyComponentType.APPLICATION);
                        policyComponent.setComponentJson(applicationComponents == null ? "[]" : new Gson().toJson(applicationComponents));
                        session.update(policyComponent);
                        break;

                    case ADVANCED_CONDITION:
                        policyComponent.setComponentType(Constants.PolicyComponentType.ADVANCED_CONDITION);
                        policyComponent.setComponentJson(advancedConditions == null ? "" : advancedConditions.trim());
                        session.update(policyComponent);
                        break;

                    default:
                        LOGGER.error("Internal Data Error");
                        return new JsonResponse(5004, "Internal Data Error").toJson();

                }
            }
            session.commit();
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("response", response);
            return resp.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (ControlCenterServiceException e) {
            LOGGER.error(e.getMessage(), e);
            if (e instanceof ResourceAlreadyExistsException) {
                return new JsonResponse(409, "Policy with the same name already exists.").toJson();
            } else {
                return new JsonResponse(5003, "Unable to propagate to control center").toJson();
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(400, "Invalid request parameters").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
            Audit.audit(request, "API", MODULE_NAME, "create", error ? 0 : 1, tokenGroupName);
        }
    }

    private boolean validatePolicyParameter(String policy) throws UnsupportedEncodingException {
        if (policy.length() == policy.getBytes(StandardCharsets.UTF_8.name()).length) {
            Matcher matcher = RegularExpressions.CC_POLICY_SUPPORT_CHAR_PATTERN.matcher(policy);
            return matcher.matches();
        }
        return false;
    }

    private String constructAdvancedConditionsExpression(JsonPolicy jsonPolicy) {

        StringBuilder combinedAdvancedConditions = new StringBuilder();
        StringBuilder parsedUserComponents = parseToAdvancedCondition("user.", jsonPolicy.getUserComponents());
        if (StringUtils.hasText(parsedUserComponents)) {
            combinedAdvancedConditions.append('(');
            combinedAdvancedConditions.append(parsedUserComponents);
            combinedAdvancedConditions.append(')');
        }
        StringBuilder parsedApplicationComponents = parseToAdvancedCondition("application.", jsonPolicy.getApplicationComponents());
        if (StringUtils.hasText(parsedApplicationComponents)) {
            if (StringUtils.hasText(combinedAdvancedConditions)) {
                combinedAdvancedConditions.append(" AND ");
            }
            combinedAdvancedConditions.append('(');
            combinedAdvancedConditions.append(parsedApplicationComponents);
            combinedAdvancedConditions.append(')');
        }
        if (StringUtils.hasText(jsonPolicy.getAdvancedConditions())) {
            if (StringUtils.hasText(combinedAdvancedConditions)) {
                combinedAdvancedConditions.append(" AND ");
            }
            combinedAdvancedConditions.append('(');
            combinedAdvancedConditions.append(jsonPolicy.getAdvancedConditions());
            combinedAdvancedConditions.append(')');
        }
        return combinedAdvancedConditions.toString().trim();
    }

    private StringBuilder parseToAdvancedCondition(String lValuePrefix,
        JsonAdvancedCondnComponent[] advancedComponents) {

        if (advancedComponents == null || advancedComponents.length == 0) {
            return new StringBuilder();
        }
        StringBuilder parsedAdvancedComponents = new StringBuilder();
        parsedAdvancedComponents.append('(');
        int advancedComponentsCount = 0;

        for (JsonAdvancedCondnComponent jsonAdvancedCondnComponent : advancedComponents) {

            if (!jsonAdvancedCondnComponent.hasValidContent()) {
                continue;
            }

            if (advancedComponentsCount > 0) {
                parsedAdvancedComponents.append(" AND ");
            }

            StringBuilder parsedJsonComponents = new StringBuilder();
            if ("NOT".equals(jsonAdvancedCondnComponent.getOperator())) {
                parsedJsonComponents.append('!');
            }
            parsedJsonComponents.append('(');
            int componentsCount = 0;

            for (JsonPolicyComponent jsonPolicyComponent : jsonAdvancedCondnComponent.getComponents()) {

                if (!jsonPolicyComponent.hasValidContent()) {
                    continue;
                }

                if (componentsCount > 0) {
                    parsedJsonComponents.append(" OR ");
                }

                StringBuilder parsedConditions = new StringBuilder();
                parsedConditions.append('(');
                int conditionsCount = 0;
                for (JsonCondition jsonCondition : jsonPolicyComponent.getConditions()) {

                    if (!jsonCondition.hasValidContent()) {
                        continue;
                    }

                    if (conditionsCount > 0) {
                        parsedConditions.append(" AND ");
                    }

                    StringBuilder condition = new StringBuilder(1024);
                    condition.append('(');
                    int valuesCount = 0;
                    if (jsonCondition.getValue().length == 0) {
                        condition.append(lValuePrefix);
                        condition.append('"');
                        condition.append(jsonCondition.getAttribute());
                        condition.append("\" ");
                        condition.append(jsonCondition.getOperator());
                        condition.append(" \"\"");
                    }
                    for (String value : jsonCondition.getValue()) {

                        if (valuesCount > 0) {
                            if ("or".equalsIgnoreCase(jsonCondition.getCombiner())) {
                                condition.append(" OR ");
                            }
                            if ("and".equalsIgnoreCase(jsonCondition.getCombiner())) {
                                condition.append(" AND ");
                            }
                        }
                        condition.append(lValuePrefix);
                        condition.append('"');
                        condition.append(jsonCondition.getAttribute());
                        condition.append("\" ");
                        condition.append(jsonCondition.getOperator());
                        condition.append(" \"");
                        condition.append(value);
                        condition.append('"');
                        valuesCount++;
                    }
                    condition.append(')');

                    parsedConditions.append(condition);
                    conditionsCount++;
                }
                parsedConditions.append(')');

                parsedJsonComponents.append(parsedConditions);
                componentsCount++;
            }

            parsedJsonComponents.append(')');
            parsedAdvancedComponents.append(parsedJsonComponents);
            advancedComponentsCount++;
        }

        parsedAdvancedComponents.append(')');
        if (RegularExpressions.EMPTY_ADVANCED_CONDITION_PATTERN.matcher(parsedAdvancedComponents.toString()).matches()) {
            return new StringBuilder();
        }
        return parsedAdvancedComponents;
    }

    private Optional<String> validate(boolean membershipPolicy, Tenant loginTenant, String tokenGroupName,
        Tenant tenant, UserSession us, DbSession session, int userId) {
        if (membershipPolicy) {
            if (!tenant.getId().equals(loginTenant.getId())) {
                return Optional.of(new JsonResponse(403, "Membership policies can be deployed only to the login tenant").toJson());
            }
            if (!tenant.isAdmin(us.getUser().getEmail())) {
                return Optional.of(new JsonResponse(403, "Only tenant admin can define membership policies").toJson());
            }
        } else {
            TokenGroupManager tokenGroupMgr;
            try {
                tokenGroupMgr = TokenGroupManager.newInstance(session, tokenGroupName, us.getLoginTenant());
            } catch (TokenGroupException e) {
                return Optional.of(new JsonResponse(400, e.getMessage()).toJson());
            }
            Membership membership = tokenGroupMgr.getStaticMembership(session, userId);
            if (membership == null && !MembershipPoliciesEvaluationHandler.isDynamicallyAccessible(session, us, tokenGroupName)) {
                return Optional.of(new JsonResponse(403, "User not part of token group").toJson());
            }
            boolean isSaasMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.SAAS, "false"));
            if ((isSaasMode && !(tenant.isAdmin(us.getUser().getEmail()) || ProjectService.isMemberProjectOwner(membership))) || (!isSaasMode && !(tenant.isAdmin(us.getUser().getEmail()) || tenant.isProjectAdmin(us.getUser().getEmail())))) {
                return Optional.of(new JsonResponse(403, "Only tenant admin or project admin can interact with policies.").toJson());
            }
        }
        return Optional.empty();
    }

    @Secured
    @GET
    @Path("/{tokenGroupName}/policies")
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy,
        @QueryParam("searchString") String searchString) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);

            ControlCenterPolicy[] policies = policyService.list(searchString, policyClientTgName, orderBy, page, size);
            for (ControlCenterPolicy policy : policies) {
                policy.setName(policy.getName().replaceFirst(policyClientTgName + " ", ""));
            }

            error = false;
            List<JsonClassificationCategory> classification = !tenant.getKeystore().getTokenGroupName().equals(tokenGroupName) ? ClassificationMgmt.getProjectClassification(session, tokenGroupName) : ClassificationMgmt.getTenantClassification(session, tokenGroupName);
            JsonResponse response = new JsonResponse("OK");
            response.putResult("policies", policies);
            if (classification.isEmpty()) {
                response.putResult("hasClassification", false);
                response.putResult("message", RMSMessageHandler.getClientString("policy.classification.complete.pending"));
            } else {
                response.putResult("hasClassification", true);
            }
            return response.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "list", error ? 0 : 1, tokenGroupName);
        }
    }

    @SuppressWarnings("unchecked")
    @Secured
    @GET
    @Path("/{tokenGroupName}/policies/{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName,
        @PathParam("policyId") Long policyId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }
            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
            ControlCenterPolicy policy = policyService.get(policyId);

            JsonPolicy jsonPolicy = new JsonPolicy();
            jsonPolicy.setId(policy.getId());
            jsonPolicy.setName(policy.getName().replaceFirst(policyClientTgName + " ", ""));
            jsonPolicy.setDescription(policy.getDescription());
            jsonPolicy.setEffectType(policy.getEffectType());
            jsonPolicy.setVersion(policy.getVersion());
            jsonPolicy.setStatus(policy.getStatus());
            jsonPolicy.setScheduleConfig(policy.getScheduleConfig());
            if (membershipPolicy) {
                Optional<ControlCenterDelegationObligation> projectObligation = Arrays.stream(policy.getAllowObligations()).filter(obligation -> "projects".equals(obligation.getName())).findFirst();
                if (projectObligation.isPresent()) {
                    jsonPolicy.setJsonABACMembershipObligation(new JsonABACMembershipObligation[] {
                        MembershipPoliciesEvaluationHandler.getJsonABACMembershipObligation(projectObligation.get()) });
                }
            }

            List<JsonResourceComponent> jsonResourceList = new ArrayList<>();
            for (ControlCenterComponentRequest controlCenterComponentRequest : policy.getFromResourceComponents()) {

                JsonResourceComponent jsonResourceComponent = new JsonResourceComponent();
                jsonResourceComponent.setOperator(controlCenterComponentRequest.getOperator());
                List<ControlCenterIdWrapper> ccComponents = controlCenterComponentRequest.getComponents();
                List<ControlCenterComponent> jsonComponents = new ArrayList<>();

                for (ControlCenterIdWrapper controlCenterIdWrapper : ccComponents) {
                    ControlCenterComponent resourceComponent = componentService.getComponentById(controlCenterIdWrapper.getId());
                    jsonComponents.add(resourceComponent);
                }
                jsonResourceComponent.setComponents(jsonComponents.toArray(new ControlCenterComponent[jsonComponents.size()]));
                jsonResourceList.add(jsonResourceComponent);
            }
            jsonPolicy.setResources(jsonResourceList.toArray(new JsonResourceComponent[jsonResourceList.size()]));

            List<String> policyActions = new ArrayList<>();
            if (policy.getActionComponents().length > 0) {
                for (ControlCenterIdWrapper actionIdWrapper : policy.getActionComponents()[0].getComponents()) {
                    policyActions.add(componentService.getComponentById(actionIdWrapper.getId()).getName());
                }
            }
            jsonPolicy.setActions(policyActions.toArray(new String[policyActions.size()]));

            Criteria criteria = session.createCriteria(PolicyComponent.class);
            criteria.add(Restrictions.eq("policyId", policyId));
            List<PolicyComponent> policyComponents = criteria.list();
            for (PolicyComponent policyComponent : policyComponents) {
                switch (policyComponent.getComponentType()) {

                    case USER:
                        if (policyComponent.getComponentJson().isEmpty()) {
                            jsonPolicy.setUserComponents(new JsonAdvancedCondnComponent[0]);
                        } else {
                            jsonPolicy.setUserComponents(new Gson().fromJson(policyComponent.getComponentJson(), JsonAdvancedCondnComponent[].class));
                        }
                        break;

                    case APPLICATION:
                        if (policyComponent.getComponentJson().isEmpty()) {
                            jsonPolicy.setApplicationComponents(new JsonAdvancedCondnComponent[0]);
                        } else {
                            jsonPolicy.setApplicationComponents(new Gson().fromJson(policyComponent.getComponentJson(), JsonAdvancedCondnComponent[].class));
                        }
                        break;

                    case ADVANCED_CONDITION:
                        jsonPolicy.setAdvancedConditions(policyComponent.getComponentJson());
                        break;

                    default:
                        LOGGER.error("Internal Data Error");
                        return new JsonResponse(5004, "Internal Data Error").toJson();

                }
            }
            error = false;
            JsonResponse response = new JsonResponse("OK");
            response.putResult("policy", jsonPolicy);
            return response.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "get", error ? 0 : 1, tokenGroupName);
        }
    }

    @SuppressWarnings("unchecked")
    @Secured
    @DELETE
    @Path("/{tokenGroupName}/policies/{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName,
        @PathParam("policyId") Long policyId) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            ControlCenterPolicy policy = policyService.get(policyId);
            policyService.delete(policyId);

            ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
            try {
                for (ControlCenterComponentRequest sub : policy.getSubjectComponents()) {
                    for (ControlCenterIdWrapper comp : sub.getComponents()) {
                        componentService.deleteComponent(comp.getId());
                    }
                }
                for (ControlCenterComponentRequest rsc : policy.getFromResourceComponents()) {
                    for (ControlCenterIdWrapper comp : rsc.getComponents()) {
                        componentService.deleteComponent(comp.getId());
                    }
                }
            } catch (ControlCenterRestClientException | ControlCenterServiceException e) {
                LOGGER.error("Error occurred while trying to delete unused components.", e);
            }

            Criteria criteria = session.createCriteria(PolicyComponent.class);
            criteria.add(Restrictions.eq("policyId", policyId));
            List<PolicyComponent> policyComponents = criteria.list();
            session.beginTransaction();
            for (PolicyComponent policyComponent : policyComponents) {
                policyComponent.setStatus(PolicyComponent.Status.DELETED);
                session.update(policyComponent);
            }

            session.commit();
            error = false;
            JsonResponse response = new JsonResponse("OK");
            return response.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "delete", error ? 0 : 1, tokenGroupName);
        }
    }

    @Secured
    @POST
    @Path("/{tokenGroupName}/policies/deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deployPolicies(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName, String json) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            String[] ids = req.getParameter("ids", String[].class);
            if (ids == null || ids.length == 0) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            ControlCenterComponentService controlCenterComponentService = new ControlCenterComponentService(rsClient);
            for (String id : ids) {
                ControlCenterPolicy policy = policyService.get(Long.valueOf(id));
                for (ControlCenterComponentRequest componentRequest : policy.getFromResourceComponents()) {
                    for (ControlCenterIdWrapper idWrapper : componentRequest.getComponents()) {
                        controlCenterComponentService.deployComponent(new String[] {
                            String.valueOf(idWrapper.getId()) });
                    }
                }
            }
            policyService.deploy(ids);
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            return resp.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", MODULE_NAME, "deployPolicies", error ? 0 : 1, tokenGroupName);
        }
    }

    @Secured
    @POST
    @Path("/{tokenGroupName}/policies/undeploy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String undeployPolicies(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @HeaderParam("membershipPolicy") boolean membershipPolicy,
        @PathParam("tokenGroupName") String tokenGroupName, String json) {
        boolean error = true;
        DbSession session = DbSession.newSession();
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(membershipPolicy, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }

            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            String[] ids = req.getParameter("ids", String[].class);
            if (ids == null || ids.length == 0) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            String policyClientTgName = membershipPolicy ? tokenGroupName + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : tokenGroupName;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(policyClientTgName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            policyService.undeploy(ids);
            error = false;
            JsonResponse resp = new JsonResponse("OK");
            return resp.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
            Audit.audit(request, "API", MODULE_NAME, "undeployPolicies", error ? 0 : 1, tokenGroupName);
        }
    }

    @Secured
    @POST
    @Path("/{tokenGroupName}/policies/expressionValidate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String expressionValidate(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tokenGroupName") String tokenGroupName,
        String expression) {
        boolean error = true;
        DbSession session = DbSession.newSession();
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
            Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
            Optional<String> validateResult = validate(false, loginTenant, tokenGroupName, tenant, us, session, userId);
            if (validateResult.isPresent()) {
                return validateResult.get();
            }
            if (!StringUtils.hasText(expression)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            ControlCenterRestClient rsClient = new ControlCenterRestClient(tokenGroupName);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            boolean isValid = policyService.expressionValidate(expression);

            error = false;
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("valid", isValid);
            return resp.toJson();
        } catch (ControlCenterRestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return handleException(e).toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
            Audit.audit(request, "API", MODULE_NAME, "deployPolicies", error ? 0 : 1, tokenGroupName);
        }
    }

    private JsonResponse handleException(ControlCenterRestClientException e) {
        if (e.getCode() == 400) {
            return new JsonResponse(5001, RMSMessageHandler.getClientString("policy.classification.complete.pending"));
        } else {
            return new JsonResponse(500, "Internal Server Error");
        }
    }

    private void createPolicyComponents(DbSession session, Long policyId, Object object,
        JsonAdvancedCondnComponent[] userComponents,
        JsonAdvancedCondnComponent[] applicationComponents, String advancedConditions, boolean membershipPolicy) {
        session.beginTransaction();
        PolicyComponent policyComponent = createPolicyComponent(policyId, object, Constants.PolicyComponentType.USER, userComponents == null ? "[]" : new Gson().toJson(userComponents), membershipPolicy ? PolicyModelType.MEMBERSHIP : PolicyModelType.RESOURCE);
        session.save(policyComponent);

        policyComponent = createPolicyComponent(policyId, object, Constants.PolicyComponentType.APPLICATION, applicationComponents == null ? "[]" : new Gson().toJson(applicationComponents), membershipPolicy ? PolicyModelType.MEMBERSHIP : PolicyModelType.RESOURCE);
        session.save(policyComponent);

        policyComponent = createPolicyComponent(policyId, object, Constants.PolicyComponentType.ADVANCED_CONDITION, advancedConditions == null ? "" : advancedConditions.trim(), membershipPolicy ? PolicyModelType.MEMBERSHIP : PolicyModelType.RESOURCE);
        session.save(policyComponent);
        session.commit();
    }

    private PolicyComponent createPolicyComponent(Long policyId, Object object, Constants.PolicyComponentType type,
        String componentJsonStr, PolicyModelType policyModelType) {
        PolicyComponent policyComponent = new PolicyComponent();
        policyComponent.setPolicyId(policyId);
        if (object instanceof Tenant) {
            policyComponent.setTenant((Tenant)object);
            policyComponent.setType(Constants.TokenGroupType.TOKENGROUP_TENANT);
        } else if (object instanceof Project) {
            policyComponent.setProject((Project)object);
            policyComponent.setType(Constants.TokenGroupType.TOKENGROUP_PROJECT);
        }
        policyComponent.setComponentType(type);
        policyComponent.setComponentJson(componentJsonStr);
        policyComponent.setPolicyType(policyModelType);
        policyComponent.setStatus(PolicyComponent.Status.ACTIVE);
        return policyComponent;
    }
}
