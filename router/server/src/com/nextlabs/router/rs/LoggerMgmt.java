package com.nextlabs.router.rs;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.router.servlet.LogConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/logger")
public class LoggerMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLogger() {
        try {
            String loggerURL = WebConfig.getInstance().getProperty(WebConfig.LOGGER_URL);
            if (loggerURL == null) {
                return new JsonResponse(400, "Logger Service is not configured").toJson();
            }
            JsonResponse response = new JsonResponse("OK");
            response.putResult("logger", loggerURL);
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        }
    }
}
