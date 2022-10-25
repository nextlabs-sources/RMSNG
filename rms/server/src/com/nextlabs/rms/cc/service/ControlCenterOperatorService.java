package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.googlecode.flyway.core.util.StringUtils;
import com.nextlabs.rms.cc.pojos.ControlCenterOperator;

public class ControlCenterOperatorService extends ControlCenterRestClient {

    public ControlCenterOperatorService(ControlCenterRestClient rs) {
        super(rs);
    }

    public ControlCenterOperator[] getMultiValOperators() throws ControlCenterServiceException,
            ControlCenterRestClientException {
        return getOperators("MULTIVAL");
    }

    public ControlCenterOperator[] getStringOperators() throws ControlCenterServiceException,
            ControlCenterRestClientException {
        return getOperators("STRING");
    }

    public ControlCenterOperator[] getAllOperators() throws ControlCenterServiceException,
            ControlCenterRestClientException {
        return getOperators("");
    }

    public ControlCenterOperator[] getOperators(String type)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        StringBuilder serviceUrl = new StringBuilder(getConsoleUrl()).append("/console/api/v1/config/dataType/list");
        if (StringUtils.hasText(type)) {
            serviceUrl.append("/" + type);
        }
        ControlCenterResponse psSearchResponse = doGet(serviceUrl.toString(), ControlCenterResponse.class);
        if (!CODE_1003.equals(psSearchResponse.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while getting operators: ").append(psSearchResponse.getStatusCode()).append(" - ").append(psSearchResponse.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }
        JsonElement data = psSearchResponse.getData();
        return new Gson().fromJson(data, ControlCenterOperator[].class);
    }
}
