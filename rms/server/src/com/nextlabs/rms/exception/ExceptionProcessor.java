package com.nextlabs.rms.exception;

import com.google.gson.JsonParseException;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.DataException;

public final class ExceptionProcessor {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private ExceptionProcessor() {

    }

    public static JsonResponse parseToJsonResponse(JsonResponse response, Throwable e) {
        if (e instanceof RMSException && e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof ValidateException) {
            response.setStatusCode(((ValidateException)e).getErrorCode());
            response.setMessage(e.getMessage());
        } else if (e instanceof FileAlreadyRevokedException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("File has been revoked.", e);
            }
            response.setStatusCode(304);
            response.setMessage("File already revoked.");
        } else if (e instanceof IllegalArgumentException || e instanceof ClassCastException || e instanceof DataException || e instanceof JsonParseException) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: ", e);
            }
            response.setStatusCode(400);
            response.setMessage("Malformed request");
        } else if (e instanceof FileNotFoundException) {
            response.setStatusCode(404);
            response.setMessage("File Not Found.");
        } else if (e instanceof SocketTimeoutException) {
            response.setStatusCode(500);
            response.setMessage("Socket timeout.");
        } else if (e instanceof IOException) {
            response.setStatusCode(500);
            response.setMessage("IO Error.");
        } else if (e instanceof FileUploadException) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when uploading file in sharing local ", e);
            }
            response.setStatusCode(500);
            response.setMessage("File Upload Error.");
        } else if (e instanceof RepositoryException) {
            LOGGER.error(e.getMessage(), e);
            response.setStatusCode(502);
            response.setMessage("Bad Gateway");
        } else if (e instanceof VaultStorageExceededException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Vault Storage Exceeded ", e);
            }
            response.setStatusCode(6002);
            response.setMessage("Vault Storage Exceeded.");
        } else if (e instanceof ApplicationRepositoryException) {
            LOGGER.error(e.getMessage(), e);
            response.setStatusCode(6003);
            response.setMessage("Error occured while accessing the the Application Repository ");
        } else {
            LOGGER.error(e.getMessage(), e);
            response.setStatusCode(500);
            response.setMessage("Internal Server Error");
        }
        return response;
    }

    public static Response parseToJAXRSResponse(Throwable e) {
        if (e instanceof RMSException && e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof ValidateException) {
            JsonResponse resp = new JsonResponse();
            resp.setStatusCode(((ValidateException)e).getErrorCode());
            resp.setMessage(e.getMessage());
            return Response.status(resp.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
        } else if (e instanceof FileNotFoundException) {
            JsonResponse resp = new JsonResponse(404, "Missing file.");
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
        } else if (e instanceof IllegalArgumentException || e instanceof JsonParseException || e instanceof ClassCastException || e instanceof IllegalStateException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON ", e);
            }
            JsonResponse resp = new JsonResponse(400, "Malformed request.");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
        } else if (e instanceof InvalidDefaultRepositoryException || e instanceof RepositoryException || e instanceof IOException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            JsonResponse resp = new JsonResponse(500, "Internal Server Error.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();

        } else if (e instanceof UnauthorizedOperationException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage(), e);
            }
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new JsonResponse(403, "Access Denied").toJson()).build();
        } else {
            LOGGER.error(e.getMessage(), e);
            JsonResponse resp = new JsonResponse(500, "Internal Server Error.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(resp.toJson()).build();
        }
    }
}
