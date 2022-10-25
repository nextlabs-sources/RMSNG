package com.nextlabs.rms.shared;

import java.io.IOException;

import org.apache.http.HttpResponse;

public interface IHTTPResponseHandler<T> {

    public T handle(HttpResponse response) throws IOException;
}
