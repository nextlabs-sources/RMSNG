package com.nextlabs.rms.cc.factory;

import com.nextlabs.rms.cc.service.ControlCenterRestClient;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.cc.service.ControlCenterTagService;

public final class ControlCenterTagServiceFactory {

    public static ControlCenterTagService createControlCenterTagService(ControlCenterRestClient rsClient)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        return new ControlCenterTagService(rsClient, "/console/api/v1/config/tags/add/POLICY_TAG");
    }

    private ControlCenterTagServiceFactory() {
    }
}
