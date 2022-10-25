package com.nextlabs.common.util;

import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.SimpleSSLSocketFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

public final class RestClient {

    private static int connectionTimeout = 20000;
    private static int readTimeout = 20000;

    private RestClient() {
    }

    public static int getConnectionTimeout() {
        return connectionTimeout;
    }

    public static int getReadTimeout() {
        return readTimeout;
    }

    public static void setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
    }

    public static void setReadTimeout(int timeout) {
        readTimeout = timeout;
    }

    public static String getBaseUrl(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    public static String get(String path) throws IOException {
        return get(path, false);
    }

    public static String get(String path, boolean expectRedir) throws IOException {
        return get(path, null, expectRedir);
    }

    public static String get(String path, Properties prop, boolean expectRedir) throws IOException {
        return sendRequest(path, "GET", prop, null, expectRedir, connectionTimeout, readTimeout);
    }

    public static String delete(String path) throws IOException {
        return sendRequest(path, "DELETE", null, null, false, connectionTimeout, readTimeout);
    }

    public static String delete(String path, boolean expectRedir) throws IOException {
        return sendRequest(path, "DELETE", null, null, expectRedir, connectionTimeout, readTimeout);
    }

    public static String delete(String path, String json) throws IOException {
        return sendRequest(path, "DELETE", null, json, false, connectionTimeout, readTimeout);
    }

    public static String post(String path, String json) throws IOException {
        return post(path, null, json);
    }

    public static String post(String path, Properties prop, String json) throws IOException {
        return sendRequest(path, "POST", prop, json, false, connectionTimeout, readTimeout);
    }

    public static String post(String path, String json, boolean expectRedir) throws IOException {
        return sendRequest(path, "POST", null, json, expectRedir, connectionTimeout, readTimeout);
    }

    public static String post(String path, Properties prop, String json, int connectionTimeout, int readTimeout)
            throws IOException {
        return sendRequest(path, "POST", prop, json, false, connectionTimeout, readTimeout);
    }

    public static String post(String path, String json, int connectionTimeout, int readTimeout) throws IOException {
        return post(path, null, json, connectionTimeout, readTimeout);
    }

    public static String put(String path, Properties prop, String json, int connectionTimeout, int readTimeout)
            throws IOException {
        return sendRequest(path, "PUT", prop, json, false, connectionTimeout, readTimeout);
    }

    public static String put(String path, String json, int connectionTimeout, int readTimeout)
            throws IOException {
        return put(path, null, json, connectionTimeout, readTimeout);
    }

    public static String put(String path, Properties prop, String json) throws IOException {
        return put(path, prop, json, connectionTimeout, readTimeout);
    }

    public static String put(String path, String json) throws IOException {
        return put(path, null, json);
    }

    public static String put(String path, String json, boolean expectRedir) throws IOException {
        return sendRequest(path, "PUT", null, json, expectRedir, connectionTimeout, readTimeout);
    }

    public static String sendRequest(String path, String method, Properties prop, String data, boolean expectRedir,
        int connectionTimeout, int readTimeout)
            throws IOException {
        HttpURLConnection conn = null;
        OutputStream os = null;
        try {
            URL url = new URL(path);
            HttpURLConnection.setFollowRedirects(false);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);

            if (prop != null && !prop.isEmpty()) {
                for (String key : prop.stringPropertyNames()) {
                    conn.setRequestProperty(key, prop.getProperty(key));
                }
            }

            if (data != null) {
                if (prop == null || !prop.containsKey("Content-Type")) {
                    conn.setRequestProperty("Content-Type", "application/json");
                }
                conn.setDoOutput(true);
                os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            if (code < HttpURLConnection.HTTP_OK || code >= HttpURLConnection.HTTP_MULT_CHOICE) {
                if (expectRedir && code < HttpURLConnection.HTTP_BAD_REQUEST) {
                    return conn.getHeaderField("Location");
                }
                throw new IOException(String.format("rest request failed - status: %d, %s", code, conn.getResponseMessage()));
            }
            return IOUtils.toString(conn.getInputStream());
        } finally {
            /*
             * https://scotte.org/2015/01/httpurlconnection-socket-leak/
             */
            if (conn != null) {
                IOUtils.skipAll(conn.getErrorStream());
                IOUtils.closeQuietly(conn.getErrorStream());
            }
            IOUtils.closeQuietly(os);
            IOUtils.close(conn);
        }
    }

    public static void disableSSLCertificateChecking() throws GeneralSecurityException {
        if (BuildConfig.DEBUG) {
            SimpleSSLSocketFactory sf = new SimpleSSLSocketFactory(SimpleSSLSocketFactory.TLS_V1_2);
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
            HttpsURLConnection.setDefaultHostnameVerifier(SimpleSSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        }
    }
}
