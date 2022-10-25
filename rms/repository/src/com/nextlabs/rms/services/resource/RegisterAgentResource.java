package com.nextlabs.rms.services.resource;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.joda.time.Instant;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class RegisterAgentResource extends ServerResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String REGISTER_AGENT_RESPONSE;
    static {

        try (InputStream is = RegisterAgentResource.class.getResourceAsStream("RegisterAgent.txt")) {
            REGISTER_AGENT_RESPONSE = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Post
    public Representation doPost(Representation entity) throws XmlException, IOException {
        StringRepresentation response = null;
        try {
            String xml = entity.getText();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("RegisterAgentRequest: {}", xml);
            }
            String registerAgentResponse = REGISTER_AGENT_RESPONSE;
            String now = Instant.now().toString();
            registerAgentResponse = registerAgentResponse.replace("{{now}}", now);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("RegisterAgentResponse: {}", registerAgentResponse);
            }
            response = new StringRepresentation(registerAgentResponse, MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            LOGGER.error("Error occurred when handling POST request for register agent", e);
        }
        return response;
    }
}
