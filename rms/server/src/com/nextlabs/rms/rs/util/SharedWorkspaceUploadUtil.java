package com.nextlabs.rms.rs.util;

import com.nextlabs.common.shared.JsonRepositoryFileEntry;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.application.FileUploadMetadata;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SharedWorkspaceUploadUtil {

    public Map<String, String> createCustomMetaDeta(String duid, Rights[] rights, String createdBy,
        String lastModifiedBy, Long creationTime, Long lastModified) {
        Map<String, String> customMetadata = new HashMap<>();
        customMetadata.put("duid", duid);
        customMetadata.put("rights", String.valueOf(Rights.toInt(rights)));
        customMetadata.put("createdBy", createdBy);
        if (lastModifiedBy != null) {
            customMetadata.put("lastModifiedBy", lastModifiedBy);
        }
        if (creationTime != null) {
            customMetadata.put("creationTime", creationTime.toString());
        }
        if (lastModified != null) {
            customMetadata.put("lastModified", lastModified.toString());
        }
        return customMetadata;
    }

    public static JsonRepositoryFileEntry getJsonRepositoryFileEntry(FileUploadMetadata metadata, File tmpFile) {
        JsonRepositoryFileEntry entry = new JsonRepositoryFileEntry();
        Date lastModifiedTime = metadata.getLastModifiedTime();
        if (lastModifiedTime != null) {
            entry.setLastModified(lastModifiedTime.getTime());
        }
        entry.setFolder(false);
        entry.setName(tmpFile.getName());
        entry.setPathDisplay(metadata.getPathDisplay());
        entry.setPathId(metadata.getPathDisplay());
        entry.setSize(tmpFile.length());
        return entry;
    }

}
