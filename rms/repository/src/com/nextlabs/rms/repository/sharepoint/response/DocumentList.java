package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DocumentList {

    @SerializedName("value")
    private List<Results> results;

    public List<Results> getResults() {
        return results;
    }

    public void setResults(List<Results> results) {
        this.results = results;
    }

    public static class Results {

        @SerializedName("Name")
        private String name;
        @SerializedName("TimeLastModified")
        private String lastModifiedTime;
        @SerializedName("TimeCreated")
        private String createdTime;
        @SerializedName("Length")
        private Long size;
        @SerializedName("ItemCount")
        private Integer childrenCount;
        @SerializedName("UniqueId")
        private String id;
        @SerializedName("odata.type")
        private String dataType;
        @SerializedName("ServerRelativeUrl")
        private String serverRelativeUrl;
        @SerializedName("__metadata")
        private Metadata metadata;

        public String getServerRelativeUrl() {
            return serverRelativeUrl;
        }

        public void setServerRelativeUrl(String serverRelativeUrl) {
            this.serverRelativeUrl = serverRelativeUrl;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public boolean isFile() {
            return DataType.FILE.getMetadata().equals(getDataType()) || (metadata != null && DataType.FILE.getMetadata().equals(metadata.getType()));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLastModifiedTime() {
            return lastModifiedTime;
        }

        public void setLastModifiedTime(String lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
        }

        public String getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Integer getChildrenCount() {
            return childrenCount;
        }

        public void setChildrenCount(Integer childrenCount) {
            this.childrenCount = childrenCount;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static class Metadata {

        @SerializedName("type")
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static enum DataType {
        FILE("SP.File"),
        FOLDER("SP.Folder"),
        LIST("SP.List");

        private String metadata;

        private DataType(String metadata) {
            this.metadata = metadata;
        }

        public String getMetadata() {
            return metadata;
        }
    }
}
