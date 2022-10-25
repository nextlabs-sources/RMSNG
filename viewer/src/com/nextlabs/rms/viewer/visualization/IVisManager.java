package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.rms.viewer.exception.RMSException;

import java.io.File;

public interface IVisManager {

    public String getVisURL(String user, int offset, String domain, String sessionId, byte[] fileContent,
        String displayName, String cacheId) throws RMSException;

    public String getVisURL(String user, int offset, String domain, String sessionId, File folderpath,
        String displayName, String cacheId) throws RMSException;
}
