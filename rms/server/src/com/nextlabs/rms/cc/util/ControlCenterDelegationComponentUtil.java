package com.nextlabs.rms.cc.util;

import com.google.gson.Gson;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest.ControlCenterIdWrapper;
import com.nextlabs.rms.cc.pojos.ControlCenterCondition;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationSubject;
import com.nextlabs.rms.cc.pojos.ControlCenterObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterObligationParameters;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;
import com.nextlabs.rms.cc.pojos.ControlCenterTagFilters;
import com.nextlabs.rms.cc.service.ControlCenterComponentService;
import com.nextlabs.rms.cc.service.ControlCenterConstants;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ControlCenterDelegationComponentUtil {

    INSTANCE;

    private String[] delegationActionPolicyArray = { "Create Policy", "Delete Policy", "Deploy Policy",
        "Edit Policy", "View Policy", "Move Policy" };
    private String[] delegationActionComponentArray = { "Create Component", "Delete Component",
        "Deploy Component", "Edit Component", "View Component", "Move Component" };
    private String[] delegationActionPolicyModelArray = { "Create Policy Model", "Delete Policy Model",
        "Edit Policy Model", "View Policy Model" };
    private String[] delegationActionTagMgmtArray = { "Create Component Tags",
        "Create Policy Model Tags", "Create Policy Tags" };

    public ControlCenterComponentRequest[] getDefaultActionComponents(ControlCenterDelegationComponent[] results) {
        List<ControlCenterIdWrapper> componentIds = new ArrayList<>();
        List<String> delegatePolicyList = Arrays.asList(delegationActionPolicyArray);
        List<String> delegateComponentList = Arrays.asList(delegationActionComponentArray);
        List<String> delegatePolicyModelList = Arrays.asList(delegationActionPolicyModelArray);
        List<String> delegateTagMgmtList = Arrays.asList(delegationActionTagMgmtArray);
        for (ControlCenterDelegationComponent result : results) {
            if (delegatePolicyList.contains(result.getName()) || delegateComponentList.contains(result.getName()) || delegatePolicyModelList.contains(result.getName()) || delegateTagMgmtList.contains(result.getName())) {
                componentIds.add(new ControlCenterIdWrapper(result.getId()));
            }
        }
        return ControlCenterComponentService.buildComponentRequest(componentIds);
    }

    public ControlCenterComponentRequest[] getDefaultResourceComponents(ControlCenterDelegationComponent[] results,
        ControlCenterPolicyModel[] models) throws ControlCenterServiceException {
        List<ControlCenterIdWrapper> componentIds = new ArrayList<>();
        ControlCenterPolicyModel psPolicy = getModelFromShortName(models, ControlCenterConstants.PS_POLICY);
        ControlCenterPolicyModel psComponent = getModelFromShortName(models, ControlCenterConstants.PS_COMPONENT);
        ControlCenterPolicyModel psPolicyModel = getModelFromShortName(models, ControlCenterConstants.PS_POLICY_MODEL);
        ControlCenterPolicyModel tagMgmt = getModelFromShortName(models, ControlCenterConstants.TAG_MANAGEMENT);
        if (psPolicy == null || psComponent == null || psPolicyModel == null || tagMgmt == null) {
            throw new ControlCenterServiceException("Unable to find models required to create delegation components.");

        }
        for (ControlCenterDelegationComponent result : results) {
            long id = Long.parseLong(result.getData().get(ControlCenterDelegationComponent.KEY_ID));
            if (id == psPolicy.getId() || id == psComponent.getId() || id == psPolicyModel.getId() || id == tagMgmt.getId()) {
                componentIds.add(new ControlCenterIdWrapper(result.getId()));
            }
        }
        return ControlCenterComponentService.buildComponentRequest(componentIds);
    }

    public ControlCenterDelegationObligation[] getDefaultObligations(ControlCenterPolicyModel[] models)
            throws ControlCenterServiceException {

        ControlCenterPolicyModel psPolicy = getModelFromShortName(models, ControlCenterConstants.PS_POLICY);
        ControlCenterPolicyModel psComponent = getModelFromShortName(models, ControlCenterConstants.PS_COMPONENT);
        ControlCenterPolicyModel psPolicyModel = getModelFromShortName(models, ControlCenterConstants.PS_POLICY_MODEL);
        if (psPolicy == null || psComponent == null || psPolicyModel == null) {
            throw new ControlCenterServiceException("Unable to find models required to create delegation obligations.");
        }
        List<ControlCenterTag> tags = new ArrayList<>();
        tags.add(new ControlCenterTag(ControlCenterConstants.ALL_TAGS_KEY, ControlCenterConstants.ALL_TAGS_LABEL));
        String allTagFilter = new Gson().toJson(constructControlCenterTagFilters(tags));

        ControlCenterObligation policyObligation = psPolicy.getObligations().get(0);
        Map<String, String> policyParams = constructObligationParams(policyObligation);
        policyParams.put(ControlCenterConstants.VIEW_TAG_FILTERS, allTagFilter);
        policyParams.put(ControlCenterConstants.EDIT_TAG_FILTERS, allTagFilter);
        policyParams.put(ControlCenterConstants.DEPLOY_TAG_FILTERS, allTagFilter);
        policyParams.put(ControlCenterConstants.MOVE_TAG_FILTERS, allTagFilter);
        policyParams.put(ControlCenterConstants.DELETE_TAG_FILTERS, allTagFilter);
        policyParams.put(ControlCenterConstants.INSERT_TAG_FILTERS, allTagFilter);

        ControlCenterObligation componentObligation = psComponent.getObligations().get(0);
        Map<String, String> componentParams = constructObligationParams(componentObligation);
        componentParams.put(ControlCenterConstants.VIEW_TAG_FILTERS, allTagFilter);
        componentParams.put(ControlCenterConstants.EDIT_TAG_FILTERS, allTagFilter);
        componentParams.put(ControlCenterConstants.DEPLOY_TAG_FILTERS, allTagFilter);
        componentParams.put(ControlCenterConstants.MOVE_TAG_FILTERS, allTagFilter);
        componentParams.put(ControlCenterConstants.DELETE_TAG_FILTERS, allTagFilter);
        componentParams.put(ControlCenterConstants.INSERT_TAG_FILTERS, allTagFilter);

        ControlCenterObligation modelObligation = psPolicyModel.getObligations().get(0);
        Map<String, String> modelParams = constructObligationParams(modelObligation);
        modelParams.put(ControlCenterConstants.VIEW_TAG_FILTERS, allTagFilter);
        modelParams.put(ControlCenterConstants.EDIT_TAG_FILTERS, allTagFilter);
        modelParams.put(ControlCenterConstants.DEPLOY_TAG_FILTERS, allTagFilter);
        modelParams.put(ControlCenterConstants.MOVE_TAG_FILTERS, allTagFilter);
        modelParams.put(ControlCenterConstants.DELETE_TAG_FILTERS, allTagFilter);
        modelParams.put(ControlCenterConstants.INSERT_TAG_FILTERS, allTagFilter);

        ControlCenterDelegationObligation[] obligations = new ControlCenterDelegationObligation[3];
        // POLICY_ACCESS_TAGS
        obligations[0] = new ControlCenterDelegationObligation();
        obligations[0].setName(policyObligation.getShortName());
        obligations[0].setPolicyModelId(psPolicy.getId());
        obligations[0].setParams(policyParams);
        // COMPONENT_ACCESS_TAGS
        obligations[1] = new ControlCenterDelegationObligation();
        obligations[1].setName(componentObligation.getShortName());
        obligations[1].setPolicyModelId(psComponent.getId());
        obligations[1].setParams(componentParams);
        // POLICY_MODEL_ACCESS_TAGS
        obligations[2] = new ControlCenterDelegationObligation();
        obligations[2].setName(modelObligation.getShortName());
        obligations[2].setPolicyModelId(psPolicyModel.getId());
        obligations[2].setParams(modelParams);
        return obligations;
    }

    public ControlCenterTagFilters constructControlCenterTagFilters(List<ControlCenterTag> tags) {
        ControlCenterTagFilters.ControlCenterTagFilter[] filters = new ControlCenterTagFilters.ControlCenterTagFilter[2];
        ControlCenterTagFilters.ControlCenterTagFilter filter = new ControlCenterTagFilters.ControlCenterTagFilter();
        ControlCenterTagFilters.ControlCenterTagFilter folderFilter = new ControlCenterTagFilters.ControlCenterTagFilter();
        folderFilter.setOperator("IN");
        filter.setOperator("IN");
        List<ControlCenterTag> folderTags = new ArrayList<>();
        if (!tags.isEmpty()) {
            ControlCenterTag folderTag = new ControlCenterTag();
            folderTag.setKey("all_folders");
            folderTag.setLabel("All Folders");
            folderTag.setType("FOLDER_TAG");
            folderTags.add(folderTag);
        } else {
            ControlCenterTag tag = new ControlCenterTag();
            tag.setKey("all_tags");
            tag.setLabel("All Tags");
            tags.add(tag);
        }
        filter.setTags(tags);
        filters[0] = filter;
        folderFilter.setTags(folderTags);
        filters[1] = folderFilter;

        ControlCenterTagFilters filterWrapper = new ControlCenterTagFilters();
        filterWrapper.setTagsFilters(filters);
        return filterWrapper;
    }

    public ControlCenterDelegationSubject getDefaultSubjectComponent() {
        ControlCenterDelegationSubject subject = new ControlCenterDelegationSubject();
        subject.setConditions(new ControlCenterCondition[0]);
        return subject;
    }

    ControlCenterPolicyModel getModelFromShortName(ControlCenterPolicyModel[] models, String shortName) {
        for (ControlCenterPolicyModel model : models) {
            if (shortName.equals(model.getShortName())) {
                return model;
            }
        }
        return null;
    }

    public Map<String, String> constructObligationParams(ControlCenterObligation obligation) {
        ControlCenterTagFilters filters = constructControlCenterTagFilters(new ArrayList<>());
        Map<String, String> params = new HashMap<>();
        for (ControlCenterObligationParameters parameter : obligation.getParameters()) {
            params.put(parameter.getShortName(), new Gson().toJson(filters));
        }
        return params;
    }
}
