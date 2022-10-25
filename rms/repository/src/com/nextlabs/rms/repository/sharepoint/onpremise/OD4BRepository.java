package com.nextlabs.rms.repository.sharepoint.onpremise;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.pojo.ServiceProviderSetting;

public class OD4BRepository extends SharePointOnPremiseRepository {

    public OD4BRepository(RMSUserPrincipal userPrincipal, String repoId, ServiceProviderSetting setting) {
        super(userPrincipal, repoId, setting);
        this.repoType = ServiceProviderType.ONEDRIVE_FORBUSINESS;
    }

}
