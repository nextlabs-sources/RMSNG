package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SharePointSearchJson {

    @SerializedName("PrimaryQueryResult")
    private PrimaryQueryResult primaryQueryResult;

    public PrimaryQueryResult getPrimaryQueryResult() {
        return primaryQueryResult;
    }

    public void setPrimaryQueryResult(PrimaryQueryResult primaryQueryResult) {
        this.primaryQueryResult = primaryQueryResult;
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
        private List<Rows> rows;

        public List<Rows> getRows() {
            return rows;
        }

        public void setRows(List<Rows> rows) {
            this.rows = rows;
        }

    }

    public static class Rows {

        @SerializedName("Cells")
        private List<Cells> cells;

        public Rows(List<Cells> cells) {
            this.cells = cells;
        }

        public List<Cells> getCells() {
            return cells;
        }

        public void setCells(List<Cells> cells) {
            this.cells = cells;
        }
    }

    public static class Cells {

        @SerializedName("Key")
        private String key;
        @SerializedName("Value")
        private String value;
        @SerializedName("ValueType")
        private String valueType;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValueType() {
            return valueType;
        }

        public void setValueType(String valueType) {
            this.valueType = valueType;
        }
    }
}
