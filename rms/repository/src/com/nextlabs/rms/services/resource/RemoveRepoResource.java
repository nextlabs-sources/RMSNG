package com.nextlabs.rms.services.resource;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.rmc.RemoveRepositoryRequestDocument;
import com.nextlabs.rms.rmc.RemoveRepositoryResponseDocument;
import com.nextlabs.rms.services.manager.RepositorySvcManager;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

public class RemoveRepoResource extends AbstractSyncResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    protected Representation handlePost(Representation entity)
            throws XmlException, IOException, RepositoryNotFoundException, UnauthorizedOperationException,
            UserNotFoundException, ForbiddenOperationException {
        RepositorySvcManager theManager = RepositorySvcManager.getInstance();
        String xml = entity.getText();

        RMSUserPrincipal user = authenticateRequest();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("RemoveRepositoryRequest: {}", xml);
        }

        RemoveRepositoryRequestDocument doc = RemoveRepositoryRequestDocument.Factory.parse(xml);
        RemoveRepositoryRequestDocument.RemoveRepositoryRequest request = doc.getRemoveRepositoryRequest();
        RemoveRepositoryResponseDocument removeRepositoryResponse = theManager.removeRepositoryRequest(user, request);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("RemoveRepositoryResponse: {}", removeRepositoryResponse.toString());
        }
        return getStringRepresentation(removeRepositoryResponse);
    }

    protected StringRepresentation getErrorResponse(int errCode, String errMsg) throws IOException {
        RemoveRepositoryResponseDocument doc = RemoveRepositoryResponseDocument.Factory.newInstance();
        RemoveRepositoryResponseDocument.RemoveRepositoryResponse response = doc.addNewRemoveRepositoryResponse();
        response.setStatus(getStatus(errCode, errMsg));
        return getStringRepresentation(doc);
    }
}
