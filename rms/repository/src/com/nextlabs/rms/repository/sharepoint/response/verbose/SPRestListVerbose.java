package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SPRestList;

import java.util.List;

public class SPRestListVerbose {

    @SerializedName("d")
    private Data data;

    public List<SPRestList.Results> getResults() {
        return data.getResults();
    }

    public void setResults(List<SPRestList.Results> results) {
        this.data.setResults(results);
    }

    public static class Data {

        private List<SPRestList.Results> results;

        public List<SPRestList.Results> getResults() {
            return results;
        }

        public void setResults(List<SPRestList.Results> results) {
            this.results = results;
        }
    }
}
