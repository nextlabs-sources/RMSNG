package com.nextlabs.rms.services.resource;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.rmc.UpdateRepositoryRequestDocument;
import com.nextlabs.rms.rmc.UpdateRepositoryResponseDocument;
import com.nextlabs.rms.services.manager.RepositorySvcManager;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.restlet.representation.Representation;

public class UpdateRepoResource extends AbstractSyncResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    protected Representation handlePost(Representation entity) throws IOException, RepositoryNotFoundException,
            UserNotFoundException, XmlException, UnauthorizedOperationException, DuplicateRepositoryNameException,
            BadRequestException, ForbiddenOperationException {

        RMSUserPrincipal user = authenticateRequest();
        RepositorySvcManager theManager = RepositorySvcManager.getInstance();
        String xml = entity.getText();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("UpdateRepositoryRequest: {}", xml);
        }
        UpdateRepositoryRequestDocument doc = UpdateRepositoryRequestDocument.Factory.parse(xml);
        UpdateRepositoryRequestDocument.UpdateRepositoryRequest request = doc.getUpdateRepositoryRequest();
        UpdateRepositoryResponseDocument updateRepositoryResponse = theManager.updateRepositoryRequest(user, request);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("UpdateRepositoryResponse: {}", updateRepositoryResponse);
        }
        return getStringRepresentation(updateRepositoryResponse);
    }

    @Override
    protected Representation getErrorResponse(int errCode, String errMsg) throws IOException {
        UpdateRepositoryResponseDocument doc = UpdateRepositoryResponseDocument.Factory.newInstance();
        UpdateRepositoryResponseDocument.UpdateRepositoryResponse response = doc.addNewUpdateRepositoryResponse();
        response.setStatus(getStatus(errCode, errMsg));
        return getStringRepresentation(doc);
    }
}
