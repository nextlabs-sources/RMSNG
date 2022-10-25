package com.nextlabs.nxl;

import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

abstract class CryptoBaseRequest {

    private OutputStream os;

    public OutputStream getOutputStream() {
        return os;
    }

    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

    public static class StreamInfo {

        private InputStream is;
        private long contentLength;
        private String fileName;

        public StreamInfo(InputStream in, long len, String name) {
            is = in;
            contentLength = len;
            fileName = name;
        }

        public InputStream getInputStream() {
            return is;
        }

        public long getContentLength() {
            return contentLength;
        }

        public String getFileName() {
            return fileName;
        }
    }

    abstract static class EncryptionRequest extends CryptoBaseRequest {

        private String membership;
        private Rights[] rights;
        private String watermark;
        private JsonExpiry expiry;
        private FilePolicy filePolicy;
        private Map<String, String[]> tags;
        private File originalFile;
        private StreamInfo streamInfo;
        private ProtectionType protectionType;
        private String fileName;
        private String tenantName;

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }

        public String getMembership() {
            return membership;
        }

        public void setMembership(String membership) {
            this.membership = membership;
        }

        public Rights[] getRights() {
            return rights;
        }

        public void setRights(Rights[] rights) {
            this.rights = rights;
        }

        public String getWatermark() {
            return watermark;
        }

        public void setWatermark(String watermark) {
            this.watermark = watermark;
        }

        public JsonExpiry getExpiry() {
            return expiry;
        }

        public void setExpiry(JsonExpiry expiry) {
            this.expiry = expiry;
        }

        public FilePolicy getFilePolicy() {
            return filePolicy;
        }

        public void setFilePolicy(FilePolicy filePolicy) {
            this.filePolicy = filePolicy;
        }

        public Map<String, String[]> getTags() {
            return tags;
        }

        public void setTags(Map<String, String[]> tags) {
            this.tags = tags;
        }

        public File getOriginalFile() {
            return originalFile;
        }

        public void setOriginalFile(File originalFile) {
            this.originalFile = originalFile;
        }

        public StreamInfo getStreamInfo() {
            return streamInfo;
        }

        public void setStreamInfo(StreamInfo streamInfo) {
            this.streamInfo = streamInfo;
        }

        public ProtectionType getProtectionType() {
            return protectionType;
        }

        public void setProtectionType(ProtectionType protectionType) {
            this.protectionType = protectionType;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
