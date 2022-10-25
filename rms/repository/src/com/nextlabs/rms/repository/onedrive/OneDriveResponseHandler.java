package com.nextlabs.rms.repository.onedrive;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveInnerResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthErrorResponse;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveOAuthException;
import com.nextlabs.rms.repository.onedrive.exception.OneDriveServiceException;
import com.nextlabs.rms.shared.IHTTPResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

public class OneDriveResponseHandler<T> implements IHTTPResponseHandler<T> {

    public static final JsonDeserializer<Date> DATE_JSON_DESERIALIZER = new JsonDeserializer<Date>() {

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null) {
                return null;
            }
            try {
                return DateUtils.parseISO8601(json.getAsString());
            } catch (ParseException e) {
                return null;
            }
        }
    };
    private final Class<T> successResponseClass;
    private final Gson gson;

    public OneDriveResponseHandler(Class<T> responseClass) {
        this.successResponseClass = responseClass;
        this.gson = new GsonBuilder().registerTypeAdapter(Date.class, DATE_JSON_DESERIALIZER).create();
    }

    private String getResponseBody(HttpResponse response) throws IOException {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (org.apache.http.ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
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

    private void handleErrorResponse(final int code, final String reasonPhrase, final String responseBody) {
        final String errorMessage = reasonPhrase;
        OneDriveErrorResponse error = null;
        try {
            error = gson.fromJson(responseBody, OneDriveErrorResponse.class);
        } catch (final Exception ex) {
            try {
                OneDriveOAuthErrorResponse authError = gson.fromJson(responseBody, OneDriveOAuthErrorResponse.class);
                throw new OneDriveOAuthException(code, reasonPhrase, authError); //NOPMD
            } catch (JsonSyntaxException ex1) {
                String msg = responseBody;
                error = new OneDriveErrorResponse();
                OneDriveOAuthException.OneDriveResponse e = new OneDriveOAuthException.OneDriveResponse();
                OneDriveInnerResponse innerError = new OneDriveInnerResponse();
                innerError.setCode(ex.getMessage());
                e.setCode(String.valueOf(code));
                e.setMessage("Raw error: " + (StringUtils.hasText(msg) ? msg : ex.getMessage()));
                e.setInnerError(innerError);
                error.setError(e);
                throw new OneDriveServiceException(code, reasonPhrase, error); //NOPMD
            }
        }
        throw new OneDriveServiceException(code, errorMessage, error);
    }
}
