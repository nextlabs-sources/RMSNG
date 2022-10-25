package com.nextlabs.common.shared;

import java.util.List;

/**
 * @author nnallagatla
 *
 */
public class JsonActivityLogContainer {

    private String name;
    private String duid;
    private List<JsonActivityLogRecord> logRecords;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String fileName) {
        this.name = fileName;
    }

    /**
     * @return the duid
     */
    public String getDuid() {
        return duid;
    }

    /**
     * @param duid the duid to set
     */
    public void setDuid(String duid) {
        this.duid = duid;
    }

    /**
     * @return the logRecords
     */
    public List<JsonActivityLogRecord> getLogRecords() {
        return logRecords;
    }

    /**
     * @param logRecords the logRecords to set
     */
    public void setLogRecords(List<JsonActivityLogRecord> logRecords) {
        this.logRecords = logRecords;
    }

    public static class JsonActivityLogRecord {

        private String email;
        private String operation;
        private String deviceType;
        private String deviceId;
        private String applicationName;
        private String applicationPublisher;
        private long accessTime;
        private String accessResult;
        private String activityData;

        /**
         * @return the email
         */
        public String getEmail() {
            return email;
        }

        /**
         * @param email the email to set
         */
        public void setEmail(String email) {
            this.email = email;
        }

        /**
         * @return the operation
         */
        public String getOperation() {
            return operation;
        }

        /**
         * @param operation the operation to set
         */
        public void setOperation(String operation) {
            this.operation = operation;
        }

        /**
         * @return the deviceType
         */
        public String getDeviceType() {
            return deviceType;
        }

        /**
         * @param deviceType the deviceType to set
         */
        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        /**
         * @return the applicationName
         */
        public String getApplicationName() {
            return applicationName;
        }

        /**
         * @param applicationName the applicationName to set
         */
        public void setApplicationName(String appliationName) {
            this.applicationName = appliationName;
        }

        /**
         * @return the applicationPublisher
         */
        public String getApplicationPublisher() {
            return applicationPublisher;
        }

        /**
         * @param applicationPublisher the applicationPublisher to set
         */
        public void setApplicationPublisher(String applicationPublisher) {
            this.applicationPublisher = applicationPublisher;
        }

        /**
         * @return the accessTime
         */
        public long getAccessTime() {
            return accessTime;
        }

        /**
         * @param accessTime the accessTime to set
         */
        public void setAccessTime(long accessTime) {
            this.accessTime = accessTime;
        }

        /**
         * @return the accessResult
         */
        public String getAccessResult() {
            return accessResult;
        }

        /**
         * @param accessResult the accessResult to set
         */
        public void setAccessResult(String accessResult) {
            this.accessResult = accessResult;
        }

        /**
         * @return the deviceId
         */
        public String getDeviceId() {
            return deviceId;
        }

        /**
         * @param deviceId the deviceId to set
         */
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getActivityData() {
            return activityData;
        }

        public void setActivityData(String activityData) {
            this.activityData = activityData;
        }
    }
}
