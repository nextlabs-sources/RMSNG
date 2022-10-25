package com.nextlabs.rms.repository.sharepoint;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.repository.sharepoint.online.SharePointOnlineOAuthException;
import com.nextlabs.rms.repository.sharepoint.response.SPRestErrorResponse;
import com.nextlabs.rms.repository.sharepoint.response.SPRestResult;
import com.nextlabs.rms.repository.sharepoint.response.SPRestResult.SPRestMessage;
import com.nextlabs.rms.repository.sharepoint.response.SharePointOAuthErrorResponse;
import com.nextlabs.rms.shared.IHTTPResponseHandler;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SharePointResponseHandler<T> implements IHTTPResponseHandler<T> {

    private final Class<T> successResponseClass;
    private final Gson gson;
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public SharePointResponseHandler(Class<T> responseClass) {
        this.successResponseClass = responseClass;
        this.gson = new Gson();
    }

    @Override
    public T handle(HttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        String reasonPhrase = statusLine.getReasonPhrase();
        int code = statusLine.getStatusCode();
        if (code == HttpStatus.SC_OK || code == HttpStatus.SC_PARTIAL_CONTENT) {
            if (InputStream.class.equals(successResponseClass)) {
                return successResponseClass.cast(response.getEntity().getContent());
            } else {
                String responseBody = getResponseBody(response);
                return gson.fromJson(responseBody, successResponseClass);
            }
        } else if (code == HttpStatus.SC_NO_CONTENT || code == HttpStatus.SC_NOT_MODIFIED) {
            return null;
        } else if (code >= HttpStatus.SC_BAD_REQUEST) {
            String responseBody = getResponseBody(response);
            handleErrorResponse(code, reasonPhrase, responseBody);
        }
        return null;
    }

    private String getResponseBody(HttpResponse response) throws IOException {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private void handleErrorResponse(int code, String reasonPhrase, String responseBody) {
        SPRestErrorResponse response;
        try {
            response = gson.fromJson(responseBody, SPRestErrorResponse.class);
        } catch (final Exception ex) {
            try {
                SharePointOAuthErrorResponse authError = gson.fromJson(responseBody, SharePointOAuthErrorResponse.class);
                LOGGER.error("Error code " + code + ", " + reasonPhrase + ", with authError: " + authError + ", and with response:" + responseBody);
                throw new SharePointOnlineOAuthException(code, reasonPhrase, authError); //NOPMD
            } catch (JsonSyntaxException ex1) {
                String msg = responseBody;
                response = new SPRestErrorResponse();
                SPRestResult error = new SPRestResult();
                SPRestMessage errorMsg = new SPRestMessage();
                errorMsg.setValue("Raw error: " + (StringUtils.hasText(msg) ? msg : ex.getMessage()));
                errorMsg.setLang(Locale.ENGLISH.getLanguage());
                error.setCode("-1");
                error.setMessage(errorMsg);
                response.setError(error);
            }
        }
        LOGGER.error("Error code " + code + ", " + reasonPhrase + ", with response: " + responseBody);
        throw new SPRestServiceException(code, reasonPhrase, response);
    }
}
