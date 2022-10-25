package com.nextlabs.rms.viewer.cache;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.repository.CachedFile;

import java.io.File;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.commons.io.FileUtils;

final class ViewerFileCacheEventListener implements CacheEventListener {

    public static final CacheEventListener INSTANCE = new ViewerFileCacheEventListener();

    private void deleteUploadedFile(final Ehcache cache, final Element element) {
        Object objectValue = element.getObjectValue();
        if (objectValue instanceof CachedFile) {
            CachedFile cachedFile = (CachedFile)objectValue;
            String fileId = cachedFile.getEfsId();
            if (StringUtils.equals(ViewerCacheManager.CACHEID_FILECONTENT, cache.getName()) && StringUtils.hasText(fileId)) {
                File tmpFolder = new File(ViewerConfigManager.getInstance().getCommonSharedTempDir(), fileId);
                FileUtils.deleteQuietly(tmpFolder);
            }
        }
    }

    @Override
    public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
        deleteUploadedFile(cache, element);
    }

    @Override
    public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
    }

    @Override
    public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
    }

    @Override
    public void notifyElementExpired(final Ehcache cache, final Element element) {
        deleteUploadedFile(cache, element);
    }

    @Override
    public void notifyElementEvicted(final Ehcache cache, final Element element) {
    }

    @Override
    public void notifyRemoveAll(final Ehcache cache) {
    }

    @Override
    public void dispose() {
    }

    //CHECKSTYLE:OFF
    @Override
    public Object clone() throws CloneNotSupportedException { //NOPMD
        throw new CloneNotSupportedException("Singleton instance");
    }
}
