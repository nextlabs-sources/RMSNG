package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFileMetadata;

import java.util.List;

public class SharePointFoldersMetadataVerbose {

    @SerializedName("d")
    private Data data;

    public List<SharePointFileMetadata> getResults() {
        return data.getResults();
    }

    public void setResults(List<SharePointFileMetadata> results) {
        this.data.setResults(results);
    }

    public static class Data {

        private List<SharePointFileMetadata> results;

        public List<SharePointFileMetadata> getResults() {
            return results;
        }

        public void setResults(List<SharePointFileMetadata> results) {
            this.results = results;
        }
    }
}
