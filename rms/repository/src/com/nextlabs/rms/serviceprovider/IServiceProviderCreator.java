package com.nextlabs.rms.serviceprovider;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.exception.RepositoryException;

public interface IServiceProviderCreator {

    String createServiceProvider(ServiceProviderSetting serviceProviderSetting, RMSUserPrincipal principal)
            throws RepositoryAlreadyExists, DuplicateRepositoryNameException, BadRequestException, RepositoryException;

}
