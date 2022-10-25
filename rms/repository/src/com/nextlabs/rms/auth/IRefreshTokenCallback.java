package com.nextlabs.rms.auth;

import com.nextlabs.rms.exception.UnauthorizedApplicationRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;

public interface IRefreshTokenCallback {

    public ITokenResponse execute() throws RepositoryException, UnauthorizedApplicationRepositoryException;
}
