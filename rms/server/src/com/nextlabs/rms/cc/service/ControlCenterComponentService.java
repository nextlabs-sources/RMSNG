package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.cc.pojos.ControlCenterComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest.ControlCenterIdWrapper;
import com.nextlabs.rms.cc.pojos.ControlCenterCondition;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.ControlCenterSearchCriteria;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;
import com.nextlabs.rms.cc.pojos.ControlCenterVersion;
import com.nextlabs.rms.cc.pojos.JsonControlCenterComponent;
import com.nextlabs.rms.cc.pojos.JsonResourceComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ControlCenterComponentService extends ControlCenterRestClient {

    public ControlCenterComponentService(ControlCenterRestClient rsClient) {
        super(rsClient);
    }

    public void createActionComponent(ControlCenterPolicyModel model, String action)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        ControlCenterComponent component = build(model, action);
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/add";
        ControlCenterResponse response = doPost(serviceUrl, component, ControlCenterResponse.class);
        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while creating action component (").append(action).append("): ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Created action component: {}", action);
        }
        String componentId = response.getData().getAsString();
        Set<String> ids = new HashSet<>();
        ids.add(componentId);
        JsonObject[] dependencies = new Gson().fromJson(findDependencies(ids.toArray(new String[ids.size()])).getData(), JsonObject[].class);
        for (JsonObject dependency : dependencies) {
            if (!ids.contains(dependency.get("id").getAsString())) {
                ids.add(dependency.get("id").getAsString());
            }
        }
        deployComponent(ids.toArray(new String[ids.size()]));
    }

    public void createActionComponents(ControlCenterPolicyModel model, List<String> actions)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        List<ControlCenterComponent> components = new ArrayList<>();
        for (String action : actions) {
            components.add(build(model, action));
        }
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/add";
        try {
            List<ControlCenterAsyncResponse<ControlCenterResponse>> responses = doPostAsync(serviceUrl, components);
            Set<String> componentIds = new HashSet<>();
            for (ControlCenterAsyncResponse<ControlCenterResponse> asyncResponse : responses) {
                ControlCenterComponent request = (ControlCenterComponent)asyncResponse.getRequest();
                String action = request.getName();
                if (asyncResponse.getResponse() == null) {
                    StringBuilder sb = new StringBuilder("Error occurred while creating action component: ").append(action);
                    throw new ControlCenterServiceException(sb.toString());
                } else {
                    ControlCenterResponse response = asyncResponse.getResponse();
                    if (!CODE_1000.equals(response.getStatusCode())) {
                        StringBuilder sb = new StringBuilder("Error occurred while creating action component (").append(action).append("): ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
                        throw new ControlCenterServiceException(sb.toString());
                    }
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Created action component: {}", action);
                    }
                    String componentId = response.getData().getAsString();
                    componentIds.add(componentId);
                }
            }

            JsonObject[] dependencies = new Gson().fromJson(findDependencies(componentIds.toArray(new String[componentIds.size()])).getData(), JsonObject[].class);
            for (JsonObject dependency : dependencies) {
                componentIds.add(dependency.get("id").getAsString());
            }

            if (!componentIds.isEmpty()) {
                deployComponent(componentIds.toArray(new String[componentIds.size()]));
            }
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error(e.getMessage());
            throw new ControlCenterServiceException(e.getMessage(), e);
        }
    }

    public ControlCenterComponent build(ControlCenterPolicyModel model, String action) {
        ControlCenterComponent component = new ControlCenterComponent();
        component.setName(action);
        component.setTags(new ControlCenterTag[0]);
        component.setConditions(new ControlCenterCondition[0]);
        component.setSubComponents(new ControlCenterComponent[0]);
        component.setType("ACTION");
        String[] actions = new String[1];
        actions[0] = Rights.toRightAction(action);
        component.setActions(actions);
        component.setPolicyModel(new ControlCenterPolicyModel(model.getId(), model.getName()));
        component.setStatus(ControlCenterConstants.POLICY_STATUS_APPROVED);
        component.setDeployed(true);
        component.setSkipValidate(false);
        return component;
    }

    public ControlCenterComponent getComponentById(long componentId)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/" + componentId;
        ControlCenterResponse response = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1003.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting component with id " + componentId + " : " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterComponent.class);
    }

    public void modifyComponent(ControlCenterComponent component)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/modify";
        component.setTags(new ControlCenterTag[0]);
        component.setSubComponents(new ControlCenterComponent[0]);
        component.setActions(new String[0]);
        component.setSkipValidate(false);
        component.setStatus(ControlCenterConstants.POLICY_STATUS_DRAFT);
        ControlCenterResponse response = doPut(serviceUrl, component);
        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while modifying component: ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }
        String componentId = response.getData().getAsString();
        Set<String> ids = new HashSet<>();
        ids.add(componentId);
        JsonObject[] dependencies = new Gson().fromJson(findDependencies(ids.toArray(new String[ids.size()])).getData(), JsonObject[].class);
        for (JsonObject dependency : dependencies) {
            if (!ids.contains(dependency.get("id").getAsString())) {
                ids.add(dependency.get("id").getAsString());
            }
        }
        if (component.isDeployed()) {
            deployComponent(ids.toArray(new String[ids.size()]));
            component.setStatus(ControlCenterConstants.POLICY_STATUS_ACTIVE);
        }
        response.getData().getAsLong();
    }

    public void deployComponent(String[] componentIds)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/deploy";
        List<JsonControlCenterComponent> components = new ArrayList<>();
        for (String componentId : componentIds) {
            JsonControlCenterComponent component = new JsonControlCenterComponent(componentId, "COMPONENT", true, -1);
            components.add(component);
        }
        ControlCenterResponse response = doPost(serviceUrl, components, ControlCenterResponse.class);
        if (!CODE_1006.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while deploying component with id " + Arrays.toString(componentIds) + " : ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }
    }

    public ControlCenterComponent[] getActionComponents(String tokenGroupName)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/search";
        ControlCenterSearchCriteria searchCriteria = buildSearchCriteria(tokenGroupName);
        ControlCenterResponse response = doPost(serviceUrl, searchCriteria, ControlCenterResponse.class);
        if (!CODE_1004.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting action components: " + response.getStatusCode() + " - " + response.getMessage());
        }
        JsonElement data = response.getData();
        return new Gson().fromJson(data, ControlCenterComponent[].class);
    }

    private ControlCenterSearchCriteria buildSearchCriteria(String policyModelName) {
        ControlCenterSearchCriteria.ControlCenterCriteria.SearchField[] searchFields = new ControlCenterSearchCriteria.ControlCenterCriteria.SearchField[2];

        ControlCenterSearchCriteria.ControlCenterCriteria criteria = new ControlCenterSearchCriteria.ControlCenterCriteria();
        if (StringUtils.hasText(policyModelName)) {
            String[] searchValues = { policyModelName };
            ControlCenterSearchCriteria.ControlCenterCriteria.SearchField.StringSearchValue value = new ControlCenterSearchCriteria.ControlCenterCriteria.SearchField.StringSearchValue();
            value.setType("String");
            value.setValue(searchValues);

            ControlCenterSearchCriteria.ControlCenterCriteria.SearchField field = new ControlCenterSearchCriteria.ControlCenterCriteria.SearchField();
            field.setField("modelType");
            field.setType("MULTI");
            field.setValue(value);
            searchFields[0] = field;

            ControlCenterSearchCriteria.ControlCenterCriteria.SearchField.TextSearchValue actionValue = new ControlCenterSearchCriteria.ControlCenterCriteria.SearchField.TextSearchValue();
            actionValue.setType("String");
            actionValue.setValue("ACTION");

            ControlCenterSearchCriteria.ControlCenterCriteria.SearchField actionField = new ControlCenterSearchCriteria.ControlCenterCriteria.SearchField();
            actionField.setField("group");
            if (ControlCenterManager.getControlCenterVersion().before(ControlCenterVersion.V_2021_03)) {
                actionField.setType("SINGLE");
            } else {
                actionField.setType("SINGLE_EXACT_MATCH");
            }
            actionField.setValue(actionValue);
            searchFields[1] = actionField;
        }

        criteria.setFields(searchFields);

        ControlCenterSearchCriteria.ControlCenterCriteria.SortField[] sortFields = new ControlCenterSearchCriteria.ControlCenterCriteria.SortField[1];
        sortFields[0] = new ControlCenterSearchCriteria.ControlCenterCriteria.SortField("lastUpdatedDate");
        criteria.setSortFields(sortFields);

        criteria.setPageNo(0);
        criteria.setPageSize(65535);

        ControlCenterSearchCriteria searchCriteria = new ControlCenterSearchCriteria();
        searchCriteria.setCriteria(criteria);
        return searchCriteria;
    }

    public long createComponent(ControlCenterComponent component)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        component.setSubComponents(new ControlCenterComponent[0]);
        component.setActions(new String[0]);
        component.setStatus(ControlCenterConstants.POLICY_STATUS_APPROVED);
        component.setDeployed(true);
        component.setSkipValidate(false);
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/add";
        ControlCenterResponse response = doPost(serviceUrl, component, ControlCenterResponse.class);
        if (!CODE_1000.equals(response.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while creating component: ").append(response.getStatusCode()).append(" - ").append(response.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }

        String componentId = response.getData().getAsString();
        Set<String> ids = new HashSet<>();
        ids.add(componentId);
        JsonObject[] dependencies = new Gson().fromJson(findDependencies(ids.toArray(new String[ids.size()])).getData(), JsonObject[].class);
        for (JsonObject dependency : dependencies) {
            if (!ids.contains(dependency.get("id").getAsString())) {
                ids.add(dependency.get("id").getAsString());
            }
        }
        deployComponent(ids.toArray(new String[ids.size()]));
        return response.getData().getAsLong();

    }

    public void deleteComponent(long id) throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/remove/" + id;
        ControlCenterResponse response = doDelete(serviceUrl);
        if (!CODE_1002.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while deleting component: " + id + ": " + response.getStatusCode() + " - " + response.getMessage());
        }
    }

    public static ControlCenterComponentRequest[] buildComponentRequest(List<ControlCenterIdWrapper> componentIds) {
        if (componentIds.isEmpty()) {
            return new ControlCenterComponentRequest[0];
        }
        ControlCenterComponentRequest[] components = new ControlCenterComponentRequest[1];
        components[0] = new ControlCenterComponentRequest();
        components[0].setOperator("IN");
        components[0].setComponents(componentIds);
        return components;
    }

    public static ControlCenterComponentRequest[] buildResourceComponentsRequest(JsonResourceComponent[] resources) {

        if (resources.length == 0) {
            return new ControlCenterComponentRequest[0];
        }
        List<ControlCenterComponentRequest> components = new ArrayList<>();
        for (JsonResourceComponent resource : resources) {

            ControlCenterComponentRequest component = new ControlCenterComponentRequest();
            List<ControlCenterIdWrapper> componentIds = new ArrayList<>();
            for (ControlCenterComponent ccResource : resource.getComponents()) {
                componentIds.add(new ControlCenterIdWrapper(ccResource.getId()));
            }
            if (!componentIds.isEmpty()) {
                component.setComponents(componentIds);
                component.setOperator(resource.getOperator());
                components.add(component);
            }
        }
        return components.toArray(new ControlCenterComponentRequest[components.size()]);
    }

    private ControlCenterResponse findDependencies(String[] ids)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/component/mgmt/findDependencies";
        ControlCenterResponse response = doPost(serviceUrl, ids, ControlCenterResponse.class);
        if (!CODE_1008.equals(response.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting dependencies for the policy id (" + Arrays.toString(ids) + "): " + response.getStatusCode() + " - " + response.getMessage());
        }
        return response;
    }
}
