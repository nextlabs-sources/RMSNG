package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest.ControlCenterIdWrapper;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria.ControlCenterCriteria;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria.ControlCenterCriteria.SearchField;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria.ControlCenterCriteria.SortField;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;
import com.nextlabs.rms.cc.pojos.JsonControlCenterComponent;
import com.nextlabs.rms.cc.pojos.JsonPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ControlCenterPolicyService extends ControlCenterRestClient {

    public ControlCenterPolicyService(ControlCenterRestClient rs) {
        super(rs);
    }

    public ControlCenterResponse saveAndDeploy(String tokenGroupName, JsonPolicy jsonPolicy,
        String parsedAdvancedConditions, ControlCenterTag[] tags, List<ControlCenterIdWrapper> subjectComponentIds,
        List<ControlCenterIdWrapper> actionComponentIds, ControlCenterPolicyModel membershipModel)
            throws ControlCenterRestClientException, ControlCenterServiceException {

        StringBuilder serviceUrl = new StringBuilder(getConsoleUrl());
        boolean isModify = false;
        if (StringUtils.hasText(parsedAdvancedConditions) && !expressionValidate(parsedAdvancedConditions)) {
            throw new ControlCenterServiceException("Parsed to invalid advanced conditions string");
        }
        ControlCenterPolicy controlCenterPolicy = buildPolicy(jsonPolicy, tokenGroupName, parsedAdvancedConditions, tags, actionComponentIds, subjectComponentIds, membershipModel);
        if (jsonPolicy.getId() != null) {
            controlCenterPolicy.setId(jsonPolicy.getId());
            controlCenterPolicy.setVersion(jsonPolicy.getVersion());
            isModify = true;
        }
        if (membershipModel != null) {
            // get existing obligations except the "projects" obligation
            List<ControlCenterDelegationObligation> allowObligations = Arrays.stream(controlCenterPolicy.getAllowObligations()).filter(obligation -> !"projects".equals(obligation.getName())).collect(Collectors.toList());
            if (jsonPolicy.getJsonABACMembershipObligation() != null && jsonPolicy.getJsonABACMembershipObligation().length > 0) {
                allowObligations.add(MembershipPoliciesEvaluationHandler.getABACPolicyObligation(membershipModel.getId(), jsonPolicy.getJsonABACMembershipObligation()[0]));
            }
            controlCenterPolicy.setAllowObligations(allowObligations.toArray(new ControlCenterDelegationObligation[allowObligations.size()]));
        }

        ControlCenterResponse response;
        if (isModify) {
            serviceUrl.append("/console/api/v1/policy/mgmt/modify");
            response = doPut(serviceUrl.toString(), controlCenterPolicy);
        } else {
            serviceUrl.append("/console/api/v1/policy/mgmt/add");
            response = doPost(serviceUrl.toString(), controlCenterPolicy, ControlCenterResponse.class);
        }

        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while creating policy: ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            if (CODE_6003.equals(response.getStatusCode())) {
                throw new ResourceAlreadyExistsException(sb.toString());
            } else {
                throw new ControlCenterServiceException(sb.toString());
            }
        }
        String policyId = response.getData().getAsString();
        Set<String> ids = new HashSet<>();
        ids.add(policyId);
        deploy(ids.toArray(new String[ids.size()]));
        return response;
    }

    public ControlCenterResponse save(String tokenGroupName, JsonPolicy jsonPolicy, String parsedAdvancedConditions,
        ControlCenterTag[] tags, List<ControlCenterIdWrapper> subjectComponentIds,
        List<ControlCenterIdWrapper> actionComponentIds, ControlCenterPolicyModel membershipModel)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        if (StringUtils.hasText(parsedAdvancedConditions) && !expressionValidate(parsedAdvancedConditions)) {
            throw new ControlCenterServiceException("Parsed to invalid advanced conditions string");
        }
        ControlCenterPolicy controlCenterPolicy = buildPolicy(jsonPolicy, tokenGroupName, parsedAdvancedConditions, tags, actionComponentIds, subjectComponentIds, membershipModel);
        if (membershipModel != null) {
            // get existing obligations except the "projects" obligation
            List<ControlCenterDelegationObligation> allowObligations = Arrays.stream(controlCenterPolicy.getAllowObligations()).filter(obligation -> !"projects".equals(obligation.getName())).collect(Collectors.toList());
            if (jsonPolicy.getJsonABACMembershipObligation() != null && jsonPolicy.getJsonABACMembershipObligation().length > 0) {
                allowObligations.add(MembershipPoliciesEvaluationHandler.getABACPolicyObligation(membershipModel.getId(), jsonPolicy.getJsonABACMembershipObligation()[0]));
            }
            controlCenterPolicy.setAllowObligations(allowObligations.toArray(new ControlCenterDelegationObligation[allowObligations.size()]));
        }
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/add";
        ControlCenterResponse response = doPost(serviceUrl, controlCenterPolicy, ControlCenterResponse.class);
        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while saving policy: ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            if (CODE_6003.equals(response.getStatusCode())) {
                throw new ResourceAlreadyExistsException(sb.toString());
            } else {
                throw new ControlCenterServiceException(sb.toString());
            }
        }
        return response;
    }

    public ControlCenterResponse modify(ControlCenterPolicy policy)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/modify";
        ControlCenterResponse response = doPut(serviceUrl, policy);
        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while modifying policy: ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            if (CODE_6003.equals(response.getStatusCode())) {
                throw new ResourceAlreadyExistsException(sb.toString());
            } else {
                throw new ControlCenterServiceException(sb.toString());
            }
        }
        return response;
    }

    ControlCenterResponse modify(String tokenGroupName, JsonPolicy jsonPolicy, String parsedAdvancedConditions,
        ControlCenterTag[] tags, List<ControlCenterIdWrapper> subjectComponentIds,
        List<ControlCenterIdWrapper> actionComponentIds, ControlCenterPolicyModel membershipModel)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        if (StringUtils.hasText(parsedAdvancedConditions) && !expressionValidate(parsedAdvancedConditions)) {
            throw new ControlCenterServiceException("Parsed to invalid advanced conditions string");
        }
        ControlCenterPolicy controlCenterPolicy = buildPolicy(jsonPolicy, tokenGroupName, parsedAdvancedConditions, tags, actionComponentIds, subjectComponentIds, membershipModel);
        controlCenterPolicy.setId(jsonPolicy.getId());
        controlCenterPolicy.setVersion(jsonPolicy.getVersion());
        if (membershipModel != null) {
            // get existing obligations except the "projects" obligation
            List<ControlCenterDelegationObligation> allowObligations = Arrays.stream(controlCenterPolicy.getAllowObligations()).filter(obligation -> !"projects".equals(obligation.getName())).collect(Collectors.toList());
            if (jsonPolicy.getJsonABACMembershipObligation() != null && jsonPolicy.getJsonABACMembershipObligation().length > 0) {
                allowObligations.add(MembershipPoliciesEvaluationHandler.getABACPolicyObligation(membershipModel.getId(), jsonPolicy.getJsonABACMembershipObligation()[0]));
            }
            controlCenterPolicy.setAllowObligations(allowObligations.toArray(new ControlCenterDelegationObligation[allowObligations.size()]));
        }
        return modify(controlCenterPolicy);
    }

    public void deploy(String[] ids) throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/deploy";
        List<JsonControlCenterComponent> policies = new ArrayList<>();
        for (String policyId : ids) {
            JsonControlCenterComponent component = new JsonControlCenterComponent(policyId, "POLICY", true, -1);
            policies.add(component);
        }
        ControlCenterResponse response = doPost(serviceUrl, policies, ControlCenterResponse.class);
        if (!CODE_1006.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while deploying policies: " + Arrays.toString(ids) + ": " + response.getStatusCode() + " - " + response.getMessage());
        }
    }

    public void undeploy(String[] ids) throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/unDeploy";
        ControlCenterResponse response = doPost(serviceUrl, ids, ControlCenterResponse.class);
        if (!CODE_1007.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while undeploying policies: " + Arrays.toString(ids) + ": " + response.getStatusCode() + " - " + response.getMessage());
        }
    }

    public void delete(Long id) throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/remove/" + id;
        ControlCenterResponse response = doDelete(serviceUrl);
        if (!CODE_1002.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while deleting policy: " + id + ": " + response.getStatusCode() + " - " + response.getMessage());
        }

    }

    public boolean expressionValidate(String expression)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/expressionValidate";
        ControlCenterResponse response = doPostText(serviceUrl, expression);
        if (!CODE_1008.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while validating expression: " + response.getStatusCode() + " - " + response.getMessage());
        }
        return response.getData().getAsBoolean();
    }

    protected ControlCenterPolicy buildPolicy(JsonPolicy jsonPolicy, String tokenGroupName,
        String parsedAdvancedConditions,
        ControlCenterTag[] tags,
        List<ControlCenterIdWrapper> actionComponentIds, List<ControlCenterIdWrapper> subjectComponentIds,
        ControlCenterPolicyModel membershipModel)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        ControlCenterPolicy policy = new ControlCenterPolicy();
        policy.setName(tokenGroupName + " " + jsonPolicy.getName()); //changing the space will affect how searching in the list api returns results.
        policy.setDescription(jsonPolicy.getDescription());
        policy.setExpression(parsedAdvancedConditions);
        policy.setActionComponents(ControlCenterComponentService.buildComponentRequest(actionComponentIds));
        policy.setSubjectComponents(ControlCenterComponentService.buildComponentRequest(subjectComponentIds));
        policy.setFromResourceComponents(ControlCenterComponentService.buildResourceComponentsRequest(jsonPolicy.getResources()));
        policy.setTags(tags);

        // if this is new policy, enable audit log
        if (jsonPolicy.getId() == null) {
            if (membershipModel == null) {
                ControlCenterDelegationObligation auditLogObligation = new ControlCenterDelegationObligation();
                auditLogObligation.setName("log");
                ControlCenterDelegationObligation[] obligations = new ControlCenterDelegationObligation[1];
                obligations[0] = auditLogObligation;
                policy.setAllowObligations(obligations);
                policy.setDenyObligations(obligations);
            } else {
                policy.setAllowObligations(new ControlCenterDelegationObligation[0]);
                policy.setDenyObligations(new ControlCenterDelegationObligation[0]);
            }
        } else {
            ControlCenterPolicy existingCCPolicy = get(jsonPolicy.getId());
            policy.setAllowObligations(existingCCPolicy.getAllowObligations());
            policy.setDenyObligations(existingCCPolicy.getDenyObligations());
        }

        policy.setSkipAddingTrueAllowAttribute(true);
        policy.setEffectType(ControlCenterConstants.POLICY_EFFECT_ALLOW);
        policy.setStatus(jsonPolicy.getToDeploy() ? ControlCenterConstants.POLICY_STATUS_APPROVED : ControlCenterConstants.POLICY_STATUS_DRAFT);
        policy.setDeployed(true);
        policy.setSkipValidate(true);
        if (jsonPolicy.getScheduleConfig() != null) {
            policy.setScheduleConfig(jsonPolicy.getScheduleConfig());
        }
        return policy;
    }

    public ControlCenterPolicy[] list(String searchVal, String tagLabel, String sortField, Integer page, Integer size)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/search";
        ControlCenterSearchCriteria criteria = buildSearchCriteria(searchVal, tagLabel, sortField, page, size);

        ControlCenterResponse response = doPost(serviceUrl, criteria, ControlCenterResponse.class);
        if (!CODE_1004.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while listing policies: " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterPolicy[].class);
    }

    public ControlCenterPolicy get(Long id) throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policy/mgmt/active/" + id;
        ControlCenterResponse response = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1003.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting policy id (" + id + "): " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterPolicy.class);
    }

    private ControlCenterSearchCriteria buildSearchCriteria(String searchVal, String tagLabel, String sortField,
        Integer page,
        Integer size) {

        SearchField[] searchFields;
        if (StringUtils.hasText(searchVal)) {
            searchFields = new SearchField[3];
        } else {
            searchFields = new SearchField[2];
        }

        ControlCenterCriteria criteria = new ControlCenterCriteria();
        if (StringUtils.hasText(tagLabel)) {
            String[] searchTagValues = { tagLabel, ControlCenterConstants.SKYDRM_TAG_LABEL };
            for (int i = 0; i < searchTagValues.length; i++) {
                String[] searchValues = new String[1];
                searchValues[0] = searchTagValues[i];
                SearchField.StringSearchValue value = new SearchField.StringSearchValue();
                value.setType("String");
                value.setValue(searchValues);

                SearchField field = new SearchField();
                field.setField("tags");
                field.setNestedField("tags.label");
                field.setType("NESTED_MULTI");
                field.setValue(value);
                searchFields[i] = field;
            }
        }

        if (StringUtils.hasText(searchVal)) {
            String[] supportedSearchFields = { "name", "description" };
            SearchField.TextSearchValue value = new SearchField.TextSearchValue();
            value.setType("Text");
            value.setFields(supportedSearchFields);
            value.setValue(searchVal);

            SearchField field = new SearchField();
            field.setField("");
            field.setType("TEXT");
            field.setValue(value);
            searchFields[2] = field;

        }
        criteria.setFields(searchFields);
        if (StringUtils.hasText(sortField)) {
            SortField[] sortFields = new SortField[1];
            sortFields[0] = new SortField(sortField);
            criteria.setSortFields(sortFields);
        }

        page = page != null && page > 0 ? page : 0;
        //Changed the value from Integer max to 10000 because max size has been reduced in cc 9.1
        //Its should not affect cc 8.6
        size = size != null && size > 0 ? size : 10000;
        criteria.setPageNo(page);
        criteria.setPageSize(size);

        ControlCenterSearchCriteria searchCriteria = new ControlCenterSearchCriteria();
        searchCriteria.setCriteria(criteria);
        return searchCriteria;
    }
}
