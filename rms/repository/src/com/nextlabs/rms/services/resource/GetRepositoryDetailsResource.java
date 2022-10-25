package com.nextlabs.rms.services.resource;

import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.rmc.GetRepositoryDetailsRequestDocument;
import com.nextlabs.rms.rmc.GetRepositoryDetailsResponseDocument;
import com.nextlabs.rms.services.manager.RepositorySvcManager;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

public class GetRepositoryDetailsResource extends AbstractSyncResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    protected Representation handlePost(Representation entity) throws IOException, XmlException, UserNotFoundException {
        RepositorySvcManager theManager = RepositorySvcManager.getInstance();

        String xml = entity.getText();

        RMSUserPrincipal user = authenticateRequest();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("GetRepositoryDetailsRequest: {}", xml);
        }

        GetRepositoryDetailsRequestDocument doc = GetRepositoryDetailsRequestDocument.Factory.parse(xml);
        GetRepositoryDetailsRequestDocument.GetRepositoryDetailsRequest request = doc.getGetRepositoryDetailsRequest();
        GetRepositoryDetailsResponseDocument getRepositoryDetailsResponse = theManager.getRepositoryDetailsRequest(user, request);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("GetRepositoryDetailsResponse: {}", getRepositoryDetailsResponse);
        }
        return getStringRepresentation(getRepositoryDetailsResponse);
    }

    protected StringRepresentation getErrorResponse(int errCode, String errMsg) throws IOException {
        GetRepositoryDetailsResponseDocument doc = GetRepositoryDetailsResponseDocument.Factory.newInstance();
        GetRepositoryDetailsResponseDocument.GetRepositoryDetailsResponse response = doc.addNewGetRepositoryDetailsResponse();
        response.setStatus(getStatus(errCode, errMsg));
        return getStringRepresentation(doc);
    }
}
