package com.nextlabs.rms.cc.service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nextlabs.common.shared.Constants.PolicyModelType;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cc.factory.ControlCenterTagServiceFactory;
import com.nextlabs.rms.cc.pojos.ControlCenterAttribute;
import com.nextlabs.rms.cc.pojos.ControlCenterIdentifier;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria.ControlCenterCriteria.SearchField;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.security.KeyStoreManagerImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ControlCenterModelService extends ControlCenterRestClient {

    private static final String CREATE_ENDPOINT = "/console/api/v1/policyModel/mgmt/add";
    private static final String SEARCH_ENDPOINT = "/console/api/v1/policyModel/search";

    private static List<ControlCenterIdentifier> resourceActions;
    private static List<ControlCenterIdentifier> membershipActions;
    private static List<ControlCenterIdentifier> projectActions;

    static {
        List<String> allActions = CentralPoliciesEvaluationHandler.getEvaluatedRights();
        resourceActions = new ArrayList<>(allActions.size());
        for (String action : allActions) {
            resourceActions.add(new ControlCenterIdentifier(action, Rights.toRightAction(action)));
        }
        List<String> projectRights = Lists.newArrayList(Rights.VIEW.name(), Rights.PRINT.name(), Rights.SHARE.name(), Rights.DOWNLOAD.name(), Rights.EDIT.name(), Rights.DECRYPT.name());
        projectActions = new ArrayList();
        for (String action : projectRights) {
            projectActions.add(new ControlCenterIdentifier(action, Rights.toRightAction(action)));
        }
        membershipActions = Collections.singletonList(new ControlCenterIdentifier(Rights.ACCESS_PROJECT.name(), Rights.toRightAction(Rights.ACCESS_PROJECT.name())));
    }

    public ControlCenterModelService(ControlCenterRestClient rs) {
        super(rs);
    }

    public ControlCenterPolicyModel getPolicyModelById(long id) throws ControlCenterRestClientException,
            ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policyModel/mgmt/active/" + id;
        ControlCenterResponse response = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1003.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while fetching policy model (" + id + "): " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterPolicyModel.class);
    }

    public ControlCenterPolicyModel createResourcePolicyModel(String tokenGroupName,
        List<ControlCenterAttribute> attributes,
        PolicyModelType policyModelType)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + CREATE_ENDPOINT;
        ControlCenterPolicyModel model = new ControlCenterPolicyModel();
        // Policy model short names cannot contain spaces or special characters, use keystore_id for short name
        String shortName;
        if (tokenGroupName.endsWith(Constants.MEMBERSHIP_MODEL_SUFFIX)) {
            String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroupName.substring(0, tokenGroupName.length() - Constants.MEMBERSHIP_MODEL_SUFFIX.length())).getId();
            shortName = ControlCenterManager.escapeIllegalChars(keyStoreId + Constants.MEMBERSHIP_MODEL_SUFFIX);
        } else {
            String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroupName).getId();
            shortName = ControlCenterManager.escapeIllegalChars(keyStoreId);
        }
        model.setName(tokenGroupName);
        model.setShortName(shortName);
        model.setType("RESOURCE");
        model.setStatus(ControlCenterConstants.POLICY_STATUS_ACTIVE);
        String defaultTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
        if (tokenGroupName.startsWith(defaultTenantName)) {
            if (policyModelType.equals(PolicyModelType.RESOURCE)) {
                if (tokenGroupName.equals(defaultTenantName)) {
                    model.setActions(resourceActions);
                } else {
                    model.setActions(projectActions);
                }
            } else if (policyModelType.equals(PolicyModelType.MEMBERSHIP)) {
                model.setActions(membershipActions);
            }
        }
        model.setAttributes(attributes);
        ControlCenterRestClient rsClient = new ControlCenterRestClient(tokenGroupName);
        ControlCenterTagService tagService = ControlCenterTagServiceFactory.createControlCenterTagService(rsClient);
        if (tagService.getSkyDRMTag() == null) {
            tagService.createSkyDRMTag();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created SkyDRM tag");
            }
        }
        ControlCenterTag[] tags = new ControlCenterTag[1];
        tags[0] = tagService.getSkyDRMTag();
        model.setTags(tags);

        ControlCenterResponse response = doPost(serviceUrl, model, ControlCenterResponse.class);
        if (!CODE_1000.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while creating RMS policy model: " + response.getStatusCode() + " - " + response.getMessage());
        }
        model.setId(response.getData().getAsLong());
        TokenGroupCacheManager.getInstance().putResourceType(tokenGroupName, shortName);
        return model;
    }

    public void updateResourcePolicyModel(ControlCenterPolicyModel model,
        List<ControlCenterAttribute> newResourceAttributes,
        PolicyModelType policyModelType, boolean createActions)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policyModel/mgmt/modify";
        List<ControlCenterIdentifier> newActions = new ArrayList<>();
        if (createActions) {
            List<ControlCenterIdentifier> actions = new ArrayList<>();
            if (policyModelType.equals(PolicyModelType.RESOURCE)) {
                actions = resourceActions;
            } else if (policyModelType.equals(PolicyModelType.MEMBERSHIP)) {
                actions = membershipActions;
            }
            for (ControlCenterIdentifier action : actions) {
                boolean exists = false;
                if (model.getActions() != null) {
                    for (ControlCenterIdentifier existing : model.getActions()) {
                        if (existing.getName().equals(action.getName()) && existing.getShortName().equals(action.getShortName())) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    newActions.add(action);
                }
            }
            model.getActions().addAll(newActions);
        }
        model.setAttributes(newResourceAttributes);
        ControlCenterResponse response = doPut(serviceUrl, model);
        if (!CODE_1001.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while updating RMS policy model: " + response.getStatusCode() + " - " + response.getMessage());
        }
        response.getData().getAsLong();
    }

    private SearchField[] buildSearchFields(String name) {
        SearchField.TextSearchValue value = new SearchField.TextSearchValue();
        value.setType("String");
        value.setValue(name);

        SearchField field = new SearchField();
        field.setField("name.untouched");
        field.setType("SINGLE_EXACT_MATCH");
        field.setValue(value);

        SearchField[] searchFields = new SearchField[1];
        searchFields[0] = field;
        return searchFields;
    }

    public ControlCenterPolicyModel getResourceModel(String tokenGroupName) throws ControlCenterServiceException,
            ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + SEARCH_ENDPOINT;
        ControlCenterSearchCriteria criteria = new ControlCenterSearchCriteria();
        criteria.getCriteria().setFields(buildSearchFields(tokenGroupName));
        ControlCenterResponse response = doPost(serviceUrl, criteria, ControlCenterResponse.class);
        if (!CODE_1004.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while fetching RMS policy model: " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        ControlCenterPolicyModel[] models = new Gson().fromJson(data, ControlCenterPolicyModel[].class);
        if (models == null || models.length == 0) {
            return null;
        }
        return getPolicyModelById(models[0].getId());
    }

    public ControlCenterPolicyModel getSubjectModel() throws ControlCenterServiceException,
            ControlCenterRestClientException {
        SearchField.TextSearchValue value = new SearchField.TextSearchValue();
        value.setValue("user");
        value.setType("Text");
        value.setFields(new String[] { "shortName" });
        SearchField shortNameField = new SearchField();
        shortNameField.setField("shortName");
        shortNameField.setType("TEXT");
        shortNameField.setValue(value);

        String serviceUrl = getConsoleUrl() + SEARCH_ENDPOINT;
        ControlCenterSearchCriteria criteria = new ControlCenterSearchCriteria();
        criteria.getCriteria().setFields(new SearchField[] { shortNameField });
        ControlCenterResponse response = doPost(serviceUrl, criteria, ControlCenterResponse.class);
        JsonElement data = response.getData();
        ControlCenterPolicyModel[] policyModels = new Gson().fromJson(data, ControlCenterPolicyModel[].class);

        if (policyModels.length > 0) {
            return getPolicyModelById(policyModels[0].getId());
        }
        return null;
    }

    public ControlCenterAttribute[] getDefaultSubjectAttributes() throws ControlCenterServiceException,
            ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/policyModel/mgmt/extraSubjectAttribs/" + ControlCenterConstants.RMS_DEFAULT_SUBJECT_MODEL_NAME;
        ControlCenterResponse response = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1003.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting default subject attributes: " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterAttribute[].class);
    }
}
