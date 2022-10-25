package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.AuthUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ControlCenterRestClient {

    protected static final String CC_CONSOLE_AUTH_ENDPOINT = "/console/login/cas";
    protected static final String CONSOLE_COOKIE = "JSESSIONID";
    protected static final String CODE_1000 = "1000";
    protected static final String CODE_1001 = "1001";
    protected static final String CODE_1002 = "1002";
    protected static final String CODE_1003 = "1003";
    protected static final String CODE_1004 = "1004";
    protected static final String CODE_1006 = "1006";
    protected static final String CODE_1007 = "1007";
    protected static final String CODE_1008 = "1008";
    protected static final String CODE_5000 = "5000";
    protected static final String CODE_6003 = "6003";
    protected static final String CODE_6006 = "6006";
    protected static final String CONSOLE_URL;
    protected static final HttpClient HTTPCLIENT;
    protected static final int CC_SESSION_DURATION;

    private static SSLContext context;
    private static SSLConnectionSocketFactory sslsf;
    private static byte[] defaultKey;
    protected static final String CSRF_HEADER = "X-CSRF-TOKEN";

    protected HttpClientContext clientContext;
    protected CookieStore cookieStore;
    protected long lastSuccessfulTS;
    protected String tokenGroupName;
    protected String csrfToken;

    protected static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final ConcurrentMap<String, TokenGroupHttpClient> CC_CLIENT_CACHE;
    private static final long LOGIN_BUFFER_MS = 180000;

    static {
        try {
            context = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
            sslsf = new SSLConnectionSocketFactory(context);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Error occurred while building SSLConnectionSocketFactory", e);
        }

        try {
            String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
            KeyManager km = new KeyManager(new KeyStoreManagerImpl());
            String icaAlias = IKeyStoreManager.PREFIX_ICA + defaultTenant;
            defaultKey = km.getKey(defaultTenant, icaAlias).getEncoded();
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error occurred while reading the ICA private key", e);
        }

        CONSOLE_URL = WebConfig.getInstance().getProperty(WebConfig.CC_CONSOLE_URL);
        if (!StringUtils.hasText(CONSOLE_URL)) {
            LOGGER.error("CC Console URL is not specified");
        }
        HTTPCLIENT = HttpClients.custom().disableRedirectHandling().setSSLSocketFactory(sslsf).build();
        CC_CLIENT_CACHE = new ConcurrentHashMap<>();
        CC_SESSION_DURATION = WebConfig.getInstance().getIntProperty(WebConfig.CC_SESSION_TIMEOUT) == -1 ? com.nextlabs.rms.config.Constants.DEFAULT_CC_SESSION_DURATION : WebConfig.getInstance().getIntProperty(WebConfig.CC_SESSION_TIMEOUT) * 60 * 1000;
    }

    public ControlCenterRestClient() throws ControlCenterServiceException, ControlCenterRestClientException {
        this(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID), WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET));
    }

    public ControlCenterRestClient(String tokenGroupName)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        this(tokenGroupName, getPassword(tokenGroupName));
    }

    private ControlCenterRestClient(String tokenGroupName, String password)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        if (CC_CLIENT_CACHE.containsKey(tokenGroupName)) {
            TokenGroupHttpClient tokenGroupHttpClient = CC_CLIENT_CACHE.get(tokenGroupName);
            this.cookieStore = tokenGroupHttpClient.getCookieStore();
            this.clientContext = HttpClientContext.create();
            this.clientContext.setCookieStore(this.cookieStore);
            this.lastSuccessfulTS = tokenGroupHttpClient.getLastSuccessfulTS();
            this.tokenGroupName = tokenGroupHttpClient.getTokenGroupName();
            this.csrfToken = tokenGroupHttpClient.getCsrfToken();
            LOGGER.debug("Using cached configuration for token group {}, last successful auth timestamp: {}", this.tokenGroupName, this.lastSuccessfulTS);
        } else {
            LOGGER.debug("Logging into Control Center while instantiating ControlCenterRestClient");
            reLoginCAS(tokenGroupName, password);
        }
    }

    private void reLoginCAS(String tokenGroupName, String password)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        LOGGER.debug("Logging in into Control Center for token group {}", tokenGroupName);
        this.cookieStore = new BasicCookieStore();
        this.clientContext = HttpClientContext.create();
        this.clientContext.setCookieStore(cookieStore);
        // CC's username rule: Username must begin with a letter followed by letters, numbers, periods (.), or underscores (_)
        // tokenGroupName may contain special characters, Use keystore_id for username if user is not admin
        String username;
        if (tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID))) {
            username = tokenGroupName;
        } else if (tokenGroupName.endsWith(Constants.MEMBERSHIP_MODEL_SUFFIX)) {
            String keyStoreId = new KeyStoreManagerImpl().getKeyStore(tokenGroupName.substring(0, tokenGroupName.length() - Constants.MEMBERSHIP_MODEL_SUFFIX.length())).getId();
            username = ControlCenterManager.escapeIllegalChars(keyStoreId) + Constants.MEMBERSHIP_MODEL_SUFFIX;
        } else {
            username = ControlCenterManager.escapeIllegalChars(new KeyStoreManagerImpl().getKeyStore(tokenGroupName).getId());
        }
        String serviceTicketEndpoint = getTGTEndpoint(username, password);
        String serviceTicket = getServiceTicketFromTGT(serviceTicketEndpoint);

        this.lastSuccessfulTS = org.joda.time.DateTime.now().getMillis();
        Cookie sessionCookie = getSessionCookieWithServiceTicket(serviceTicket);
        this.tokenGroupName = tokenGroupName;
        this.csrfToken = getCsrfTokenWithSessionId(sessionCookie);
        this.cookieStore.addCookie(sessionCookie);
        CC_CLIENT_CACHE.put(tokenGroupName, new TokenGroupHttpClient(tokenGroupName, cookieStore, lastSuccessfulTS, csrfToken));
        LOGGER.debug("Login into Control Center success, new successful auth timestamp: {}", lastSuccessfulTS);
    }

    protected ControlCenterRestClient(ControlCenterRestClient rs) {
        this.clientContext = rs.clientContext;
        this.cookieStore = rs.cookieStore;
        this.lastSuccessfulTS = rs.lastSuccessfulTS;
        this.tokenGroupName = rs.tokenGroupName;
        this.csrfToken = rs.csrfToken;
    }

    public String getConsoleUrl() {
        return CONSOLE_URL;
    }

    static String getPassword(String tokenGroupName) {
        try {
            //CC 9.1 onwards the password must be between 10 and 128 non-whitespace characters, and must contain at least one number, one lowercase letter, one uppercase letter,
            // one non-alphanumeric character, and contain no more than two identical consecutive characters. There is no password expiry rule.
            String hex = Hex.toHexString(AuthUtils.hmac(tokenGroupName.getBytes(StandardCharsets.UTF_8), defaultKey, null));
            int m = hex.charAt(5) % 8;
            StringBuilder password = new StringBuilder(11).append(hex, m * 8, m * 8 + 3).append((char)(0x23 + m)).append(hex, m * 8 + 3, m * 8 + 5).append((char)(0x39 - m)).append(hex, m * 8 + 5, m * 8 + 8).append((char)(0x71 + m));
            if (!validatePassword(password.toString())) {
                password = new StringBuilder(14).append(hex, m * 8, m * 8 + 2).append((char)(0x23 + m)).append(hex, m * 8 + 2, m * 8 + 4).append((char)(0x23 + m)).append(hex, m * 8 + 4, m * 8 + 6).append((char)(0x71 + m)).append(hex, m * 8 + 6, m * 8 + 8).append((char)(0x71 + m)).append((char)(0x39 - m)).append((char)(0x41 + m));
            }
            boolean debugMode = Boolean.parseBoolean(WebConfig.getInstance().getProperty(WebConfig.DEBUG, "false"));
            if (debugMode) {
                LOGGER.debug("Password for token group {} : {}", tokenGroupName, password.toString());
            }
            return password.toString();
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private static boolean validatePassword(String password) {
        //This pattern is used in CC to validate password
        final String ccPattern = "^(?:(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]))(?!.*(.)\\1{2,})[A-Za-z0-9!~`<>,;:_=?*+#.'\\\\\"&%()|\\[\\]{}\\-$^@/]{10,128}";
        Pattern pattern = Pattern.compile(ccPattern);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private String getTGTEndpoint(String username, String password)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String url = CONSOLE_URL + "/cas/v1/tickets";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("username", username));
        urlParameters.add(new BasicNameValuePair("password", password));
        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters, Constants.ENCODE_CHAR));
            HttpResponse response = HTTPCLIENT.execute(post, HttpClientContext.create());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_CREATED) {
                LOGGER.error("Error occurred while getting TGT endpoint." + username);
                throw new ControlCenterRestClientException(statusCode, EntityUtils.toString(response.getEntity()));
            }
            return response.getFirstHeader("Location").getValue();
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }

    private String getServiceTicketFromTGT(String ticketGrantingEndpoint)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String consoleUrl;
        URI uri;
        try {
            uri = new URI(CONSOLE_URL + CC_CONSOLE_AUTH_ENDPOINT);
            consoleUrl = UriBuilder.fromUri(uri).toString();
        } catch (URISyntaxException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        }
        HttpPost post = new HttpPost(ticketGrantingEndpoint);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("service", consoleUrl));

        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters, Constants.ENCODE_CHAR));
            HttpResponse response = HTTPCLIENT.execute(post, HttpClientContext.create());
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.error("Error occurred while getting service ticket using ticket granting ticket for service url:" + consoleUrl);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
            return responseBody;
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }

    private Cookie getSessionCookieWithServiceTicket(String serviceTicket)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String url;
        try {
            url = String.format("%s?ticket=%s", CONSOLE_URL + CC_CONSOLE_AUTH_ENDPOINT, URLEncoder.encode(serviceTicket, Constants.ENCODE_CHAR));
        } catch (UnsupportedEncodingException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        }
        HttpGet httpGet = new HttpGet(url);
        Cookie sessionCookieValue = null;

        try {
            HttpResponse response = HTTPCLIENT.execute(httpGet, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
                LOGGER.error("Error occurred while getting session cookie using service ticket.");
                throw new ControlCenterRestClientException(statusCode, EntityUtils.toString(response.getEntity()));
            }
            for (Cookie cookie : cookieStore.getCookies()) {
                if (cookie.getName().equals(ControlCenterRestClient.CONSOLE_COOKIE)) {
                    sessionCookieValue = cookie;
                    break;
                }
            }
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpGet.releaseConnection();
        }
        return sessionCookieValue;
    }

    private String getCsrfTokenWithSessionId(Cookie sessionCookieValue)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        String serviceUrl = CONSOLE_URL + "/console/api/v1/system/csrfToken";
        HttpGet httpGet = new HttpGet(serviceUrl);
        String consoleCsrfToken = null;
        try {
            CookieStore cookie = new BasicCookieStore();
            cookie.addCookie(sessionCookieValue);
            HttpClientContext httpClientContext = HttpClientContext.create();
            httpClientContext.setCookieStore(cookie);
            HttpResponse response = HTTPCLIENT.execute(httpGet, httpClientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_MOVED_TEMPORARILY && statusCode != HttpStatus.SC_NOT_FOUND && statusCode != HttpStatus.SC_FORBIDDEN) {
                LOGGER.error("Error occurred while getting session cookie using service ticket.");
                throw new ControlCenterRestClientException(statusCode, EntityUtils.toString(response.getEntity()));
            }
            Header csrfTokenHeader = response.getFirstHeader(CSRF_HEADER);
            if (csrfTokenHeader != null) {
                consoleCsrfToken = csrfTokenHeader.getValue();
            }
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpGet.releaseConnection();
        }
        return consoleCsrfToken;
    }

    protected <T> T doPost(String serviceUrl, Object object, Class<T> resultClass)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String responseBody;
        HttpPost httpPost = new HttpPost(serviceUrl);
        LOGGER.debug("Initiating POST request to URL {}", serviceUrl);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(object, object.getClass());
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(new StringEntity(json, Constants.ENCODE_CHAR));
            long currentTime = System.currentTimeMillis();
            LOGGER.debug("Last successful auth timestamp: {}, LOGIN_BUFFER_MS: {}, CC_SESSION_DURATION: {}, current time: {}", lastSuccessfulTS, LOGIN_BUFFER_MS, CC_SESSION_DURATION, currentTime);
            if (currentTime + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
            }
            httpPost.addHeader(CSRF_HEADER, csrfToken);
            HttpResponse response = HTTPCLIENT.execute(httpPost, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                CC_CLIENT_CACHE.remove(tokenGroupName);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
        } catch (ParseException | IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpPost.releaseConnection();
        }

        return new Gson().fromJson(responseBody, resultClass);
    }

    <T> T doPostText(String serviceUrl, String data)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String responseBody;
        HttpPost httpPost = new HttpPost(serviceUrl);
        try {
            httpPost.addHeader("content-type", "text/plain");
            httpPost.setEntity(new StringEntity(data, Constants.ENCODE_CHAR));
            long currentTime = System.currentTimeMillis();
            LOGGER.debug("Last successful auth timestamp: {}, LOGIN_BUFFER_MS: {}, CC_SESSION_DURATION: {}, current time: {}", lastSuccessfulTS, LOGIN_BUFFER_MS, CC_SESSION_DURATION, currentTime);
            if (currentTime + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
            }
            httpPost.addHeader(CSRF_HEADER, csrfToken);
            HttpResponse response = HTTPCLIENT.execute(httpPost, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                CC_CLIENT_CACHE.remove(tokenGroupName);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
        } catch (ParseException | IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpPost.releaseConnection();
        }

        return new Gson().fromJson(responseBody, (Class<T>)ControlCenterResponse.class);
    }

    <T> List<ControlCenterAsyncResponse<T>> doPostAsync(String serviceUrl, List<?> objects)
            throws ControlCenterRestClientException, ControlCenterServiceException, InterruptedException,
            ExecutionException {

        try (CloseableHttpAsyncClient client = createHttpAsynClient()) {
            client.start();
            List<ControlCenterAsyncResponse<T>> responses = new ArrayList<>();
            Gson gson = new Gson();
            for (Object object : objects) {
                HttpPost httpPost = new HttpPost(serviceUrl);
                String json = gson.toJson(object, object.getClass());
                httpPost.addHeader("content-type", "application/json");
                if (System.currentTimeMillis() + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                    reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
                }
                httpPost.addHeader(CSRF_HEADER, csrfToken);
                httpPost.setEntity(new StringEntity(json, Constants.ENCODE_CHAR));

                Future<HttpResponse> future = client.execute(httpPost, null);
                responses.add(new ControlCenterAsyncResponse<>(object, future));
            }

            for (ControlCenterAsyncResponse<T> req : responses) {
                HttpResponse response = req.future.get();
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    ControlCenterRestClientException ex = new ControlCenterRestClientException(statusCode, responseBody);
                    req.setException(ex);
                } else {
                    req.setResponse(new Gson().fromJson(responseBody, (Class<T>)ControlCenterResponse.class));
                }
            }
            return responses;
        } catch (IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        }
    }

    protected <T> T doPut(String serviceUrl, Object object)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String responseBody;
        HttpPut httpPut = new HttpPut(serviceUrl);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(object, object.getClass());
            httpPut.addHeader("content-type", "application/json");
            httpPut.setEntity(new StringEntity(json, Constants.ENCODE_CHAR));
            long currentTime = System.currentTimeMillis();
            LOGGER.debug("Last successful auth timestamp: {}, LOGIN_BUFFER_MS: {}, CC_SESSION_DURATION: {}, current time: {}", lastSuccessfulTS, LOGIN_BUFFER_MS, CC_SESSION_DURATION, currentTime);
            if (currentTime + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
            }
            httpPut.addHeader(CSRF_HEADER, csrfToken);
            HttpResponse response = HTTPCLIENT.execute(httpPut, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                CC_CLIENT_CACHE.remove(tokenGroupName);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
        } catch (ParseException | IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpPut.releaseConnection();
        }
        return new Gson().fromJson(responseBody, (Class<T>)ControlCenterResponse.class);
    }

    protected <T> T doGet(String serviceUrl, Class<T> resultClass)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String responseBody;
        HttpGet httpGet = new HttpGet(serviceUrl);
        try {
            long currentTime = System.currentTimeMillis();
            LOGGER.debug("Last successful auth timestamp: {}, LOGIN_BUFFER_MS: {}, CC_SESSION_DURATION: {}, current time: {}", lastSuccessfulTS, LOGIN_BUFFER_MS, CC_SESSION_DURATION, currentTime);
            if (currentTime + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
            }
            httpGet.addHeader(CSRF_HEADER, csrfToken);
            HttpResponse response = HTTPCLIENT.execute(httpGet, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                CC_CLIENT_CACHE.remove(tokenGroupName);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
        } catch (ParseException | IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpGet.releaseConnection();
        }
        return new Gson().fromJson(responseBody, resultClass);
    }

    <T> T doDelete(String serviceUrl)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String responseBody;
        HttpDelete httpDelete = new HttpDelete(serviceUrl);
        try {
            long currentTime = System.currentTimeMillis();
            LOGGER.debug("Last successful auth timestamp: {}, LOGIN_BUFFER_MS: {}, CC_SESSION_DURATION: {}, current time: {}", lastSuccessfulTS, LOGIN_BUFFER_MS, CC_SESSION_DURATION, currentTime);
            if (currentTime + LOGIN_BUFFER_MS >= lastSuccessfulTS + CC_SESSION_DURATION) {
                reLoginCAS(tokenGroupName, tokenGroupName.equals(WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_ID)) ? WebConfig.getInstance().getProperty(WebConfig.CC_ADMIN_SECRET) : getPassword(tokenGroupName));
            }
            httpDelete.addHeader(CSRF_HEADER, csrfToken);
            HttpResponse response = HTTPCLIENT.execute(httpDelete, clientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                CC_CLIENT_CACHE.remove(tokenGroupName);
                throw new ControlCenterRestClientException(statusCode, responseBody);
            }
        } catch (ParseException | IOException e) {
            throw new ControlCenterServiceException(e.getMessage(), e);
        } finally {
            httpDelete.releaseConnection();
        }
        return new Gson().fromJson(responseBody, (Class<T>)ControlCenterResponse.class);
    }

    private CloseableHttpAsyncClient createHttpAsynClient() {
        return HttpAsyncClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setDefaultCookieStore(cookieStore).setSSLContext(context).build();
    }

    public static class ControlCenterAsyncResponse<T> {

        private final Object request;
        Future<HttpResponse> future;
        private T response;
        private Exception exception;

        ControlCenterAsyncResponse(Object request, Future<HttpResponse> future) {
            this.request = request;
            this.future = future;
        }

        public void setResponse(T response) {
            this.response = response;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        public Object getRequest() {
            return request;
        }

        public T getResponse() {
            return response;
        }
    }

    public static class ControlCenterResponse {

        private String statusCode;
        private String message;
        private JsonElement data;

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public JsonElement getData() {
            return data;
        }

        public void setData(JsonElement data) {
            this.data = data;
        }

    }

    public static class ControlCenterRestClientException extends Exception {

        private static final long serialVersionUID = 1L;
        private final int code;
        private final String msg;

        public ControlCenterRestClientException(int code, String msg) {
            super(code + ": " + msg);
            this.code = code;
            this.msg = msg;
        }

        public ControlCenterRestClientException(int code, String msg, Throwable cause) {
            super(code + ": " + msg);
            super.initCause(cause);
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    public static class ControlCenterServiceException extends Exception {

        private static final long serialVersionUID = 1L;

        public ControlCenterServiceException(String message) {
            super(message);
        }

        public ControlCenterServiceException(String message, Throwable t) {
            super(message, t);
        }
    }

    public static class ResourceAlreadyExistsException extends ControlCenterServiceException {

        private static final long serialVersionUID = 1L;

        public ResourceAlreadyExistsException(String message) {
            super(message);
        }

    }

    private static final class TokenGroupHttpClient {

        private final String tokenGroupName;
        private final String csrfToken;
        private final CookieStore cookieStore;
        private final long lastSuccessfulTS;

        TokenGroupHttpClient(String tokenGroupName, CookieStore cookieStore, long lastSuccessfulTS,
            String csrfToken) {
            this.tokenGroupName = tokenGroupName;
            this.cookieStore = cookieStore;
            this.lastSuccessfulTS = lastSuccessfulTS;
            this.csrfToken = csrfToken;
        }

        public String getTokenGroupName() {
            return tokenGroupName;
        }

        CookieStore getCookieStore() {
            return cookieStore;
        }

        long getLastSuccessfulTS() {
            return lastSuccessfulTS;
        }

        String getCsrfToken() {
            return csrfToken;
        }

    }
}
