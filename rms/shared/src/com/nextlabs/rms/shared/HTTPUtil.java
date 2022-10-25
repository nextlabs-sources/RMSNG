package com.nextlabs.rms.shared;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

public final class HTTPUtil {

    public static final String HEADER_X_FORWARDED_FOR = "X-FORWARDED-FOR";
    public static final SSLContext SSL_CONTEXT;
    public static final SSLConnectionSocketFactory SSL_CONNECTION_SOCKET_FACTORY;

    static {
        try {
            SSL_CONTEXT = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

                @Override
                public boolean isTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                        throws java.security.cert.CertificateException {
                    return true;
                }
            }).build();
            SSL_CONNECTION_SOCKET_FACTORY = new SSLConnectionSocketFactory(SSL_CONTEXT, NoopHostnameVerifier.INSTANCE);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private HTTPUtil() {
    }

    public static String getURI(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        URI uri = URI.create(url.toString());
        url.setLength(0);
        url.append(uri.getScheme()).append("://").append(uri.getAuthority());
        ServletContext sc = request.getServletContext();
        url.append(sc.getContextPath());
        return url.toString();
    }

    public static String getInternalURI(HttpServletRequest request) {
        ServletContext sc = request.getServletContext();
        String contextPath = sc.getContextPath();
        String internalURL = null;
        WebConfig webConfig = WebConfig.getInstance();

        switch (contextPath) {
            case "/rms":
                internalURL = webConfig.getProperty(WebConfig.RMS_INTERNAL_URL);
                break;
            case "/router":
                internalURL = webConfig.getProperty(WebConfig.ROUTER_INTERNAL_URL);
                break;
            case "/viewer":
                internalURL = webConfig.getProperty(WebConfig.VIEWER_INTERNAL_URL);
                break;
            default:
                internalURL = getURI(request);
        }
        if (StringUtils.hasText(internalURL)) {
            return internalURL;
        }
        return getURI(request);
    }

    /**
     * content disposition header value taking care of non-ascii & special characters
     * @param filename
     * @return
     */
    public static String getContentDisposition(String filename) {
        URI uri;
        String attachmentFilename = filename;
        try {
            uri = new URI(null, null, filename, null);
            attachmentFilename = uri.toASCIIString();
            attachmentFilename = attachmentFilename.replace("+", "%2B").replace(";", "%3B").replace("'", "%27").replace(",", "%2C");
        } catch (URISyntaxException e) {
            uri = null;
        }
        StringBuilder contentDisposition = new StringBuilder(50);
        contentDisposition.append("attachment; filename*=UTF-8''").append(attachmentFilename);
        return contentDisposition.toString();
    }

    public static CloseableHttpClient getHTTPClient() throws GeneralSecurityException {
        SSLConnectionSocketFactory sslsf = getTrustAllSocketFactory();
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    public static SSLConnectionSocketFactory getTrustAllSocketFactory()
            throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        return SSL_CONNECTION_SOCKET_FACTORY;
    }

    public static String getRemoteAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (!StringUtils.hasLength(ipAddress) || StringUtils.equalsIgnoreCase("unknown", ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getQueryParameters(HttpServletRequest request, Charset charset) {
        final StringBuilder result = new StringBuilder();
        Map<String, String[]> params = request.getParameterMap();
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                String[] values = request.getParameterValues(key);
                String encodedKey = encode(key, charset);
                values = Nvl.nvl(values, new String[1]);
                for (String val : values) {
                    final String encodedValue = val != null ? encode(val, charset) : "";
                    if (result.length() > 0) {
                        result.append('&');
                    }
                    result.append(encodedKey);
                    result.append('=');
                    result.append(encodedValue);
                }
            }
        }
        return result.toString();
    }

    public static String encode(final String content, final Charset encoding) {
        try {
            return URLEncoder.encode(content, encoding != null ? encoding.name() : StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String encode(final String content) {
        return encode(content, null);
    }
}
