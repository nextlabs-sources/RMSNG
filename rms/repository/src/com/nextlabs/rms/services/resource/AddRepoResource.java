package com.nextlabs.rms.services.resource;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.UnSupportedStorageProviderException;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.rmc.AddRepositoryRequestDocument;
import com.nextlabs.rms.rmc.AddRepositoryResponseDocument;
import com.nextlabs.rms.rmc.AddRepositoryResponseDocument.AddRepositoryResponse;
import com.nextlabs.rms.services.manager.RepositorySvcManager;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

public class AddRepoResource extends AbstractSyncResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    protected Representation handlePost(Representation entity)
            throws IOException, XmlException, UserNotFoundException, RepositoryAlreadyExists,
            DuplicateRepositoryNameException, UnSupportedStorageProviderException, BadRequestException {
        RepositorySvcManager theManager = RepositorySvcManager.getInstance();
        String xml = entity.getText();

        RMSUserPrincipal user = authenticateRequest();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AddRepositoryRequest: {}", xml);
        }
        AddRepositoryRequestDocument doc = AddRepositoryRequestDocument.Factory.parse(xml);
        AddRepositoryRequestDocument.AddRepositoryRequest request = doc.getAddRepositoryRequest();
        AddRepositoryResponseDocument addRepositoryResponse = theManager.addRepositoryRequest(user, request);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AddRepositoryResponse: {}", addRepositoryResponse);
        }
        return getStringRepresentation(addRepositoryResponse);
    }

    protected StringRepresentation getErrorResponse(int errCode, String errMsg) throws IOException {
        AddRepositoryResponseDocument doc = AddRepositoryResponseDocument.Factory.newInstance();
        AddRepositoryResponse response = doc.addNewAddRepositoryResponse();
        response.setStatus(getStatus(errCode, errMsg));
        return getStringRepresentation(doc);
    }

}
