package com.nextlabs.rms.viewer.cache;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.CacheNotFoundException;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.repository.CachedFile;
import com.nextlabs.rms.viewer.repository.FileCacheId;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ViewerCacheManager {

    private static final ViewerCacheManager INSTANCE = new ViewerCacheManager();

    private final Logger logger = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private CacheManager manager;

    private boolean statelessMode;

    public static final String CACHEID_FILECONTENT = "VW_CACHEID_FILECONTENT";

    public static final int DEFAULT_CACHE_TIMEOUT_IN_MINUTES = 30;

    private ViewerCacheManager() {
        statelessMode = ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.STATELESS_MODE);
        Configuration cacheMgrConfig = new Configuration().diskStore(new DiskStoreConfiguration().path(ViewerConfigManager.getInstance().getTempDir().getAbsolutePath()));
        manager = CacheManager.create(cacheMgrConfig);
        createFileContentCache();
    }

    public boolean isStatelessMode() {
        return statelessMode;
    }

    public Object getFromCache(Object key, String cacheName) {
        if (logger.isTraceEnabled()) {
            logger.trace("Get from cache. Key: " + key + " cacheName: " + cacheName);
        }

        Ehcache cache = getCache(cacheName);
        Element element = cache.get(key);

        if (element != null) {
            return element.getObjectValue();
        } else if (CACHEID_FILECONTENT.equals(cacheName) && statelessMode) {

            CachedFile cachedFile;
            try {
                cachedFile = loadCachedFileFromDisk(key);
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Error occurred while loading cached data from disk", e);
                return null;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Put in cache. Key: " + key + "value: " + cachedFile + " cacheName: " + CACHEID_FILECONTENT);
            }
            cache.put(new Element(key, cachedFile));
            return cachedFile;
        }
        return null;
    }

    private void persistCachedFileToDisk(Object key, Object value) throws IOException {
        ObjectOutputStream oos = null;
        try {
            File tempFile = new File(ViewerConfigManager.getInstance().getViewerSharedTempDir(), ((FileCacheId)key).getSessionId());
            FileUtils.mkdirParent(tempFile);
            FileOutputStream fout = new FileOutputStream(tempFile.getAbsolutePath(), false);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(value);
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
    }

    private CachedFile loadCachedFileFromDisk(Object key) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        try {
            File tempFile = new File(ViewerConfigManager.getInstance().getViewerSharedTempDir(), ((FileCacheId)key).getSessionId());
            FileInputStream streamIn = new FileInputStream(tempFile.getAbsolutePath());
            ois = new ObjectInputStream(streamIn);
            return (CachedFile)ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    public boolean putInCache(Object key, Object value, String cacheName) {
        if (logger.isTraceEnabled()) {
            logger.trace("Put in cache. Key: " + key + "value: " + value + " cacheName: " + cacheName);
        }
        Ehcache cache = getCache(cacheName);
        cache.put(new Element(key, value));
        if (CACHEID_FILECONTENT.equals(cacheName) && statelessMode) {
            try {
                persistCachedFileToDisk(key, value);
            } catch (IOException e) {
                logger.error("Error occurred while persisting cached data to disk", e);
            }
        }
        return true;
    }

    public Object removeFromCache(Object key, String cacheName) {
        if (logger.isTraceEnabled()) {
            logger.trace("Remove from cache. Key: " + key + " cacheName: " + cacheName);
        }
        Ehcache cache = getCache(cacheName);
        Object value = cache.get(key);
        cache.remove(key);
        return value;
    }

    private void createFileContentCache() {
        ViewerConfigManager viewerConfigManager = ViewerConfigManager.getInstance();
        int cacheTimeoutMins = viewerConfigManager.getIntProperty(ViewerConfigManager.FILECONTENT_CACHE_TIMEOUT_MINS);
        if (cacheTimeoutMins <= 0) {
            logger.debug("Using default value for FileContent Cache Timeout");
            cacheTimeoutMins = DEFAULT_CACHE_TIMEOUT_IN_MINUTES;
        }
        logger.debug("FileContent Cache Timeout set to: " + cacheTimeoutMins + " mins");
        int maxBytesLocalHeap = viewerConfigManager.getIntProperty(ViewerConfigManager.FILECONTENT_CACHE_MAXMEM_MB);
        if (maxBytesLocalHeap <= 0) {
            logger.debug("Using default value for maxBytesLocalHeap");
            maxBytesLocalHeap = 100;
        }
        CacheConfiguration config = new CacheConfiguration();
        config.setName(CACHEID_FILECONTENT);
        Cache fileContentCache = new Cache(config.maxBytesLocalHeap(maxBytesLocalHeap, MemoryUnit.MEGABYTES).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU).eternal(false).timeToIdleSeconds(cacheTimeoutMins * 60L).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));
        fileContentCache.getCacheEventNotificationService().registerListener(ViewerFileCacheEventListener.INSTANCE);
        manager.addCache(fileContentCache);
    }

    public static ViewerCacheManager getInstance() {
        return INSTANCE;
    }

    public Ehcache getCache(String cacheName) {
        return manager.getEhcache(cacheName);
    }

    public void shutdown() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public CachedFile getCachedFile(FileCacheId fileCacheId) throws RMSException {
        logger.debug("About to get documentID:" + fileCacheId.getDocId() + " from cache");
        CachedFile cachedFile = (CachedFile)getFromCache(fileCacheId, CACHEID_FILECONTENT);
        if (cachedFile == null) {// Element not found. Try decoding docId
            try {
                fileCacheId = new FileCacheId(fileCacheId.getSessionId(), URLDecoder.decode(fileCacheId.getDocId(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error("Error occurred while decoding documentId", e);
                throw new RMSException(ViewerMessageHandler.getClientString("err.cache.not.found"), e);
            }
            cachedFile = (CachedFile)getFromCache(fileCacheId, CACHEID_FILECONTENT);
        }
        if (cachedFile == null) {
            logger.error("Error occurred in retrieving documentId from cache with fileCacheId having " + fileCacheId.toString());
            throw new CacheNotFoundException(ViewerMessageHandler.getClientString("err.cache.not.found"));
        }
        return cachedFile;
    }
}
