package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SharePointSearchJson;

import java.util.List;

public class SharePointSearchJsonVerbose {

    @SerializedName("d")
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<RowResults> getRowResults() {
        return data.getQuery().getPrimaryQueryResult().getRelevantResults().getTable().getRows().getResults();
    }

    public static class Data {

        @SerializedName("query")
        private Query query;

        public Query getQuery() {
            return query;
        }

        public void setQuery(Query query) {
            this.query = query;
        }

        public static class Query {

            @SerializedName("PrimaryQueryResult")
            PrimaryQueryResult primaryQueryResult;

            public PrimaryQueryResult getPrimaryQueryResult() {
                return primaryQueryResult;
            }

            public void setPrimaryQueryResult(PrimaryQueryResult primaryQueryResult) {
                this.primaryQueryResult = primaryQueryResult;
            }
        }
    }

    public static class PrimaryQueryResult {

        @SerializedName("RelevantResults")
        private RelevantResults relevantResults;

        public RelevantResults getRelevantResults() {
            return relevantResults;
        }

        public void setRelevantResults(RelevantResults relevantResults) {
            this.relevantResults = relevantResults;
        }
    }

    public static class RelevantResults {

        @SerializedName("Table")
        private Table table;

        public Table getTable() {
            return table;
        }

        public void setTable(Table table) {
            this.table = table;
        }
    }

    public static class Table {

        @SerializedName("Rows")
        private Rows rows;

        public Rows getRows() {
            return rows;
        }

        public void setRows(Rows rows) {
            this.rows = rows;
        }
    }

    public static class Rows {

        @SerializedName("results")
        private List<RowResults> results;

        public List<RowResults> getResults() {
            return results;
        }

        public void setResults(List<RowResults> results) {
            this.results = results;
        }
    }

    public static class RowResults {

        @SerializedName("Cells")
        private CellResult cellresult;

        public CellResult getCellresult() {
            return cellresult;
        }

        public void setCellresult(CellResult cellresult) {
            this.cellresult = cellresult;
        }

        public static class CellResult {

            @SerializedName("results")
            private List<SharePointSearchJson.Cells> cells;

            public List<SharePointSearchJson.Cells> getCells() {
                return cells;
            }

            public void setCells(List<SharePointSearchJson.Cells> cells) {
                this.cells = cells;
            }
        }
    }

}
