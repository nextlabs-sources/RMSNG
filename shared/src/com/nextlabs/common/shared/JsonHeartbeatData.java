package com.nextlabs.common.shared;

import java.util.List;
import java.util.Map;

public class JsonHeartbeatData {

    private String tokenGroupName;
    private Long configurationModifiedTimeStamp;
    private HeartbeatItem watermarkConfig;
    private List<JsonClassificationCategory> classificationCategories;
    private List<Map<String, String>> tokenGroupResourceTypeMappings;

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }

    public Long getConfigurationModifiedTimeStamp() {
        return configurationModifiedTimeStamp;
    }

    public void setConfigurationModifiedTimeStamp(Long configurationModifiedTimeStamp) {
        this.configurationModifiedTimeStamp = configurationModifiedTimeStamp;
    }

    public HeartbeatItem getWatermarkConfig() {
        return watermarkConfig;
    }

    public void setWatermarkConfig(HeartbeatItem watermarkConfig) {
        this.watermarkConfig = watermarkConfig;
    }

    public List<JsonClassificationCategory> getClassificationCategories() {
        return classificationCategories;
    }

    public void setClassificationCategories(List<JsonClassificationCategory> classificationCategories) {
        this.classificationCategories = classificationCategories;
    }

    public List<Map<String, String>> getTokenGroupResourceTypeMappings() {
        return tokenGroupResourceTypeMappings;
    }

    public void setTokenGroupResourceTypeMappings(List<Map<String, String>> tokenGroupResourceTypeMappings) {
        this.tokenGroupResourceTypeMappings = tokenGroupResourceTypeMappings;
    }
}
