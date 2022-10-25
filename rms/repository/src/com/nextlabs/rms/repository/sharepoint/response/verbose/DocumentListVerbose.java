package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.DocumentList;

import java.util.List;

public class DocumentListVerbose {

    @SerializedName("d")
    private Data data;

    public List<DocumentList.Results> getResults() {
        return data.getResults();
    }

    public void setResults(List<DocumentList.Results> results) {
        this.data.setResults(results);
    }

    public static class Data {

        private List<DocumentList.Results> results;

        public List<DocumentList.Results> getResults() {
            return results;
        }

        public void setResults(List<DocumentList.Results> results) {
            this.results = results;
        }
    }
}
