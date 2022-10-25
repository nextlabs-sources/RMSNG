package com.nextlabs.rms.cc.util;

import com.nextlabs.rms.cc.exception.ControlCentreNotSupportedException;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationPolicyRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationSubject;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.service.ControlCenterConstants;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;

public final class ControlCenterDelegationUtil {

    private ControlCenterDelegationUtil() {
    }

    public static ControlCenterDelegationPolicyRequest createDelegationPolicyRequest(ControlCenterPolicyModel[] models,
        ControlCenterDelegationComponent[] actionResults, ControlCenterDelegationComponent[] resourceResults)
            throws ControlCenterRestClientException, ControlCenterServiceException, ControlCentreNotSupportedException {
        String policyName = ControlCenterConstants.RMS_DELEGATION_POLICY_NAME;
        ControlCenterDelegationComponentUtil delegationComponent = ControlCenterDelegationComponentUtil.INSTANCE;
        ControlCenterComponentRequest[] actionComponents = delegationComponent.getDefaultActionComponents(actionResults);
        ControlCenterComponentRequest[] resourceComponents = delegationComponent.getDefaultResourceComponents(resourceResults, models);
        ControlCenterDelegationObligation[] obligations = delegationComponent.getDefaultObligations(models);
        ControlCenterDelegationSubject subjectComponent = delegationComponent.getDefaultSubjectComponent();

        ControlCenterDelegationPolicyRequest policy = new ControlCenterDelegationPolicyRequest();
        policy.setName(policyName);
        policy.setSubjectComponent(subjectComponent);
        policy.setObligations(obligations);
        policy.setResourceComponents(resourceComponents);
        policy.setActionComponents(actionComponents);
        policy.setEffectType(ControlCenterConstants.POLICY_EFFECT_ALLOW);
        policy.setStatus(ControlCenterConstants.POLICY_STATUS_DRAFT);
        return policy;
    }

}
