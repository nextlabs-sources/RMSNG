package com.nextlabs.rms.cache;

import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.JsonIdentityProvider;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.LogConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;

public final class RMSCacheManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String INFINISPAN_CLUSTERED_CONFIG_FILE_NAME = "infinispan_clustered.xml";

    private static final String JGROUPS_CONFIG_FILE_NAME = "jgroups.xml";

    private static final String JGROUPS_AWS_CONFIG_FILE_NAME = "jgroups_aws.xml";

    private static final String IDP_CACHE = "IDP_CACHE";

    private static final String USER_CACHE = "USER_CACHE";

    private static final String APPLOGIN_NONCE_CACHE = "APPLOGIN_NONCE_CACHE";

    // cache to store mapping between token group name and keystore id, for easy retrieval for eval in viewer and rms
    private static final String TOKEN_GROUP_KEYSTORE_MAP_CACHE = "TOKEN_GROUP_MAP_CACHE";

    private static final String EAP_CACHE = "EAP_CACHE";

    private static final String EAP_ATTR_CACHE = "EAP_ATTR_CACHE";

    private static final String SESSION_CACHE = "SESSION_CACHE";

    private static RMSCacheManager instance;

    private final Cache<String, List<JsonIdentityProvider>> idpCache;

    private final Cache<String, UserAttributeCacheItem> userAttributeCache;

    private final Cache<String, String> loginNonceCache;

    private final Cache<String, String> tokenGroupCache;

    private final Cache<String, List<Object>> eapCache;

    private final Cache<String, HashMap<String, Integer>> eapAttrCache;

    private final Cache<String, String> sessionCache;

    private final long startedAt;

    private BasicCacheContainer manager;

    public Cache<String, List<Object>> getEapCache() {
        return eapCache;
    }

    public Cache<String, HashMap<String, Integer>> getEapAttrCache() {
        return eapAttrCache;
    }

    public Cache<String, String> getSessionCache() {
        return sessionCache;
    }

    private RMSCacheManager(File confDir, File cacheDir, boolean isCacheServerMode, String cacheHost,
        int connectorPort) throws IOException {

        if (isCacheServerMode) {
            // cache names must match those in infinispan_server.xml
            idpCache = new RESTCache(IDP_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            userAttributeCache = new RESTCache(USER_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            loginNonceCache = new RESTCache(APPLOGIN_NONCE_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            tokenGroupCache = new RESTCache(TOKEN_GROUP_KEYSTORE_MAP_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            eapCache = new RESTCache(EAP_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            eapAttrCache = new RESTCache(EAP_ATTR_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            sessionCache = new RESTCache(SESSION_CACHE, "http://" + cacheHost + ":" + connectorPort + "/rest/");
            if (idpCache == null || userAttributeCache == null || loginNonceCache == null || tokenGroupCache == null || eapCache == null || eapAttrCache == null || sessionCache == null) {
                throw new IOException("Error in initializing Remote Infinispan Client");
            }
            LOGGER.info("Initialized Remote Infinispan REST Client for " + cacheHost + " at port " + connectorPort);
            startedAt = System.currentTimeMillis();

        } else {
            File clusterConfig = new File(confDir, INFINISPAN_CLUSTERED_CONFIG_FILE_NAME);
            File jgroupsConfig = new File(confDir, JGROUPS_CONFIG_FILE_NAME);
            File jgroupsAWSConfig = new File(confDir, JGROUPS_AWS_CONFIG_FILE_NAME);

            if (!cacheDir.exists()) {
                FileUtils.mkdir(cacheDir);
            }
            if (clusterConfig.exists()) {
                if (jgroupsConfig.exists()) {
                    System.setProperty("rms.ispn.jgroups.conf.location", jgroupsConfig.getAbsolutePath());
                    LOGGER.info("Initializing Embedded Infinispan Cache in distributed mode");
                } else if (jgroupsAWSConfig.exists()) {
                    System.setProperty("rms.ispn.jgroups.conf.location", jgroupsAWSConfig.getAbsolutePath());
                    LOGGER.info("Initializing Embedded Infinispan Cache in distributed mode using AWS PING");
                } else {
                    LOGGER.error("Failed to initialize Embedded Infinispan Cache");
                    String error = new StringBuilder(JGROUPS_CONFIG_FILE_NAME).append(" or ").append(JGROUPS_AWS_CONFIG_FILE_NAME).append(" not found in ").append(confDir).toString();
                    throw new FileNotFoundException(error);
                }
                System.setProperty("rms.ispn.file.store", cacheDir.getAbsolutePath());
                manager = new DefaultCacheManager(clusterConfig.getAbsolutePath());
                idpCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(IDP_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                userAttributeCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(USER_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                loginNonceCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(APPLOGIN_NONCE_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                tokenGroupCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(TOKEN_GROUP_KEYSTORE_MAP_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                eapCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(EAP_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                eapAttrCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(EAP_ATTR_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                sessionCache = new Cache(((org.infinispan.Cache<Object, Object>)manager.getCache(SESSION_CACHE)).getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES));
                if (idpCache == null || userAttributeCache == null || loginNonceCache == null || tokenGroupCache == null || eapAttrCache == null || eapCache == null || sessionCache == null) {
                    throw new IOException("Error in initializing Embedded Infinispan Cache");
                }
                LOGGER.info("Initialized Embedded Infinispan Cache");
                startedAt = System.currentTimeMillis();
            } else {
                LOGGER.error("Failed to initialize Embedded Infinispan Cache");
                String error = new StringBuilder(INFINISPAN_CLUSTERED_CONFIG_FILE_NAME).append(" not found in ").append(confDir).toString();
                throw new FileNotFoundException(error);
            }
        }
    }

    public static RMSCacheManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Infinispan Cache Manager is not initialized.");
        }
        return instance;
    }

    public static synchronized void init(File confDir, File cacheDir, boolean isCacheServerMode, String infinispanHost,
        int connectorPort) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Infinispan Cache Manager is already initialized.");
        }
        instance = new RMSCacheManager(confDir, cacheDir, isCacheServerMode, infinispanHost, connectorPort);
    }

    public void shutdown() {
        if (manager != null) {
            manager.stop();
        }
    }

    public long getstartedAt() {
        return startedAt;
    }

    public Cache<String, List<JsonIdentityProvider>> getIdpCache() {
        return idpCache;
    }

    public Cache<String, UserAttributeCacheItem> getUserAttributeCache() {
        return userAttributeCache;
    }

    public Cache<String, String> getAppLoginNonceCache() {
        return loginNonceCache;
    }

    public Cache<String, String> getTokenGroupCache() {
        return tokenGroupCache;
    }

    public static class Cache<K, V> {

        private final BasicCache<K, V> cache;

        public Cache(BasicCache<K, V> cache) {
            this.cache = cache;
        }

        public V get(K key) {
            return cache.get(key);
        }

        public V put(K key, V value) {
            return cache.put(key, value);
        }

        public V put(K key, V value, long lifespan, TimeUnit lifespanUnit) {
            return cache.put(key, value, lifespan, lifespanUnit);
        }

        public Object remove(K key) {
            return cache.remove(key);
        }

        public boolean containsKey(K key) {
            return cache.containsKey(key);
        }

        public boolean containsValue(V value) {
            return cache.containsValue(value);
        }

        public Set<Entry<K, V>> entrySet() {
            return cache.entrySet();
        }

        public Set<K> keySet() {
            return cache.keySet();
        }
    }

    public static class RESTCache<K, V> extends Cache<K, V> {

        private final String basicUrl;

        private static final String GET = "GET";
        private static final String PUT = "PUT";
        private static final String DELETE = "DELETE";

        RESTCache(String cacheName, String restServerURL) {
            super(null);
            this.basicUrl = restServerURL + cacheName;
        }

        private String restOperation(String method, String key, Object value) {
            return restOperation(method, key, value, 0L, null);
        }

        private String restOperation(String method, String key, Object value, long lifespan, TimeUnit lifespanUnit) {
            HttpURLConnection connection = null;
            OutputStream os = null;
            BufferedOutputStream output = null;
            try {
                URL url = key == null ? new URL(basicUrl) : new URL(basicUrl + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(RestClient.getConnectionTimeout());
                connection.setReadTimeout(RestClient.getReadTimeout());
                connection.setRequestProperty("Content-Type", "text/plain");
                int read = 0;
                byte[] buffer = new byte[1024 * 8];

                if (method.equals(PUT)) {
                    if (lifespan > 0) {
                        connection.setRequestProperty("timeToLiveSeconds", String.valueOf(TimeUnit.SECONDS.convert(lifespan, lifespanUnit)));
                    }
                    connection.setDoOutput(true);
                    String payload = Base64Codec.encodeAsString(SerializationUtils.serialize((Serializable)value));
                    os = connection.getOutputStream();
                    output = new BufferedOutputStream(os);
                    output.write(payload.getBytes("UTF-8"));
                    output.close();
                }

                connection.connect();
                InputStream responseBodyStream = connection.getInputStream();
                StringBuffer responseBody = new StringBuffer();
                while ((read = responseBodyStream.read(buffer)) != -1) {
                    responseBody.append(new String(buffer, 0, read, "UTF-8"));
                }
                connection.disconnect();
                return responseBody.toString();
            } catch (FileNotFoundException fnfe) {
                // Could be that the key being queried does not exist. Return null.
                LOGGER.debug("REST cache operation " + method + " " + key + " to server did not find queried key");
                return null;
            } catch (Exception e) {
                LOGGER.error("REST cache operation " + method + " " + key + " to server failed", e);
                return null;
            } finally {
                /*
                 * https://scotte.org/2015/01/httpurlconnection-socket-leak/
                 */
                if (connection != null) {
                    IOUtils.skipAll(connection.getErrorStream());
                    IOUtils.closeQuietly(connection.getErrorStream());
                }
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(output);
                IOUtils.close(connection);
            }
        }

        private String toStringKey(Object key) {
            if (key instanceof String) {
                return (String)key;
            } else {
                throw new UnsupportedOperationException("REST cache connector expects keys to be String as they are Immutable and override equals() and hashCode()");
            }
        }

        private V decode(String s) {
            if (!StringUtils.hasText(s)) {
                return null;
            } else {
                return (V)SerializationUtils.deserialize(Base64Codec.decode(s));
            }
        }

        @Override
        public V get(K key) {
            String stringKey = toStringKey(key);
            String stringValue = restOperation(GET, stringKey, null);
            return decode(stringValue);
        }

        @Override
        public V put(Object key, Object value) {
            String stringKey = toStringKey(key);
            String stringValue = restOperation(PUT, stringKey, value);

            return decode(stringValue);
        }

        @Override
        public V remove(Object key) {
            String stringKey = toStringKey(key);
            String stringValue = restOperation(DELETE, stringKey, null);

            return decode(stringValue);
        }

        @Override
        public V put(Object key, Object value, long lifespan, TimeUnit lifespanUnit) {
            String stringKey = toStringKey(key);
            String stringValue = restOperation(PUT, stringKey, value, lifespan, lifespanUnit);

            return decode(stringValue);
        }

        @Override
        public boolean containsKey(Object key) {
            String stringKey = toStringKey(key);
            String stringValue = restOperation(GET, stringKey, null);
            return decode(stringValue) != null;
        }

        /**
         * This is an expensive operation that should be avoided.
         * The entrySet returned is not backed up by the cache so
         * any changes to it need to be propagated manually. 
         */
        @Override
        public Set<Entry<K, V>> entrySet() {
            /* If need to make the returned entrySet linked to the cache
             * then extend Map and override its methods to make restful calls 
             */
            String stringKey = toStringKey("");
            Map<K, V> map = new HashMap<>();
            Set<K> keys = new HashSet(Arrays.asList((restOperation(GET, stringKey, null).split("\\r?\\n"))));
            for (K key : keys) {
                stringKey = toStringKey(key);
                String stringValue = restOperation(GET, stringKey, null);
                map.put(key, decode(stringValue));
            }
            return map.entrySet();
        }

        /**         
         * The keySet returned is not backed up by the cache so
         * any changes to it need to be propagated manually. 
         */
        @Override
        public Set<K> keySet() {
            /* If need to make the returned entrySet linked to the cache
             * then extend Map and override its methods to make restful calls 
             */
            String stringKey = toStringKey("");
            return new HashSet(Arrays.asList((restOperation(GET, stringKey, null).split("\\r?\\n"))));
        }
    }
}
