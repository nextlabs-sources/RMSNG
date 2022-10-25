package com.nextlabs.rms.application.sharepoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.application.sharepoint.exception.SharePointErrorResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointInnerResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointOAuthErrorResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointOAuthException;
import com.nextlabs.rms.application.sharepoint.exception.SharePointResponse;
import com.nextlabs.rms.application.sharepoint.exception.SharePointServiceException;
import com.nextlabs.rms.shared.IHTTPResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

public class SharePointResponseHandler<T> implements IHTTPResponseHandler<T> {

    public static final JsonDeserializer<Date> DATE_JSON_DESERIALIZER = (jsonElement, type,
        jsonDeserializationContext) -> {
        if (jsonElement == null) {
            return null;
        }
        try {
            return DateUtils.parseISO8601(jsonElement.getAsString());
        } catch (java.text.ParseException e) {
            return null;
        }
    };
    protected final Class<T> successResponseClass;
    protected final Gson gson;

    public SharePointResponseHandler(Class<T> responseClass) {
        this.successResponseClass = responseClass;
        this.gson = new GsonBuilder().registerTypeAdapter(Date.class, DATE_JSON_DESERIALIZER).create();
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

    protected static String getResponseBody(HttpResponse response) throws IOException {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected void handleErrorResponse(int code, String reasonPhrase, String responseBody) throws IOException {
        SharePointErrorResponse error = null;
        try {
            error = gson.fromJson(responseBody, SharePointErrorResponse.class);
        } catch (final Exception ex) {
            try {
                SharePointOAuthErrorResponse authError = gson.fromJson(responseBody, SharePointOAuthErrorResponse.class);
                throw new SharePointOAuthException(code, reasonPhrase, authError); //NOPMD
            } catch (JsonSyntaxException ex1) {
                error = new SharePointErrorResponse();
                SharePointResponse e = new SharePointResponse();
                SharePointInnerResponse innerError = new SharePointInnerResponse();
                innerError.setCode(ex.getMessage());
                e.setCode(String.valueOf(code));
                e.setMessage("Raw error: " + (StringUtils.hasText(responseBody) ? responseBody : ex.getMessage()));
                e.setInnerError(innerError);
                error.setError(e);
                throw new SharePointServiceException(code, reasonPhrase, error); //NOPMD
            }
        }
        throw new SharePointServiceException(code, reasonPhrase, error);
    }
}
