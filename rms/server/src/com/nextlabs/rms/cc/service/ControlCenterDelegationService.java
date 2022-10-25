package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nextlabs.rms.cc.exception.ControlCentreNotSupportedException;
import com.nextlabs.rms.cc.exception.DelegationPolicyException;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationPolicyRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.util.ControlCenterDelegationUtil;

final class ControlCenterDelegationService extends ControlCenterRestClient {

    ControlCenterDelegationService(ControlCenterRestClient rs) {
        super(rs);
    }

    void createRMSDelegationPolicy() throws ControlCenterServiceException,
            ControlCenterRestClientException, ControlCentreNotSupportedException, DelegationPolicyException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/delegationAdmin/rule/mgmt/add";
        ControlCenterPolicyModel[] models = getDelegationModels();
        ControlCenterDelegationComponent[] actionResults = searchDelegationComponents("ACTION");
        ControlCenterDelegationComponent[] resourceResults = searchDelegationComponents("RESOURCE");
        ControlCenterDelegationPolicyRequest policy = ControlCenterDelegationUtil.createDelegationPolicyRequest(models, actionResults, resourceResults);
        ControlCenterResponse psResponse = doPost(serviceUrl, policy, ControlCenterResponse.class);
        if (CODE_6003.equals(psResponse.getStatusCode())) {
            throw new DelegationPolicyException("Error occurred while creating delegation policy: " + psResponse.getStatusCode() + " - " + psResponse.getMessage());
        }
        if (!CODE_1000.equals(psResponse.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while creating delegation policy: " + psResponse.getStatusCode() + " - " + psResponse.getMessage());
        }
    }

    private ControlCenterPolicyModel[] getDelegationModels()
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/delegationAdmin/model/mgmt/details/DA_RESOURCE?pageSize=65535";
        ControlCenterResponse psModelResponse = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1004.equals(psModelResponse.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while getting delegation models: " + psModelResponse.getStatusCode() + " - " + psModelResponse.getMessage());
        }
        JsonElement data = psModelResponse.getData();
        return new Gson().fromJson(data, ControlCenterPolicyModel[].class);
    }

    private ControlCenterDelegationComponent[] searchDelegationComponents(String name)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/delegationAdmin/component/search/listNames/" + name + "?pageSize=100";
        ControlCenterResponse psSearchResponse = doGet(serviceUrl, ControlCenterResponse.class);
        if (!CODE_1004.equals(psSearchResponse.getStatusCode())) {
            throw new ControlCenterServiceException("Error occurred while searching delegation components: " + psSearchResponse.getStatusCode() + " - " + psSearchResponse.getMessage());
        }
        JsonElement data = psSearchResponse.getData();
        return new Gson().fromJson(data, ControlCenterDelegationComponent[].class);
    }
}
