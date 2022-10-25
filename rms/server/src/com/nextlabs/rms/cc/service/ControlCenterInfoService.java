package com.nextlabs.rms.cc.service;

import com.google.gson.JsonElement;

public final class ControlCenterInfoService extends ControlCenterRestClient {

    private static final String CREATE_ENDPOINT = "/console/api/v1/system/version";

    public ControlCenterInfoService(ControlCenterRestClient rs) {
        super(rs);
    }

    public String getControlCenterVersion() throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceURL = getConsoleUrl() + CREATE_ENDPOINT;
        ControlCenterResponse psResponse = doGet(serviceURL, ControlCenterResponse.class);
        if (!CODE_1004.equals(psResponse.getStatusCode())) {
            throw new ControlCenterServiceException("Error occured while fetching ControlCenter version details" + psResponse.getStatusCode() + " -" + psResponse.getMessage());
        }
        JsonElement data = psResponse.getData();
        String[] versionDigits = data.getAsString().split("\\.");
        return versionDigits[0] + "." + versionDigits[1];
    }

}
