package com.nextlabs.rms.viewer.repository;

import java.io.Serializable;
import java.util.Objects;

public class FileCacheId implements Serializable {

    private static final long serialVersionUID = -6012110325597792380L;

    private String sessionId;

    private String docId;

    public FileCacheId(String sessionId, String cacheId) {
        this.sessionId = sessionId;
        this.docId = cacheId;
    }

    public String toString() {
        return "Session ID: " + sessionId + ", Doc ID: " + docId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDocId() {
        return docId;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FileCacheId) {
            FileCacheId fileCacheId = (FileCacheId)obj;
            if (docId.equalsIgnoreCase(fileCacheId.getDocId()) && sessionId.equalsIgnoreCase(fileCacheId.getSessionId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getDocId());
        result = 29 * result + Objects.hashCode(getSessionId());
        return result;
    }

}
