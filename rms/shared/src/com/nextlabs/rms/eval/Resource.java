package com.nextlabs.rms.eval;

import java.util.HashMap;
import java.util.Map;

public class Resource {

    private final String dimensionName;
    private final String resourceName;
    private String resourceType;

    private String duid;
    private Map<String, String[]> classification = new HashMap<String, String[]>();
    private Map<String, String[]> attributes = new HashMap<String, String[]>();

    public Resource(String dimensionName, String resourceName, String resourceType) {
        if (dimensionName == null) {
            throw new IllegalArgumentException("Dimension name is null");
        }
        if (resourceName == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type is null");
        }
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.dimensionName = dimensionName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setClassification(Map<String, String[]> classification) {
        this.classification = classification;
    }

    public void setAttributes(Map<String, String[]> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Resource) {
            Resource oth = (Resource)obj;
            return getResourceName().equals(oth.getResourceName()) && getResourceType().equals(oth.getResourceType()) && getAttributes().equals(oth.getAttributes());
        }
        return false;
    }

    public Map<String, String[]> getAttributes() {
        return attributes;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public Map<String, String[]> getClassification() {
        return classification;
    }

    @Override
    public int hashCode() {
        int hash = getResourceName().hashCode();
        hash = 31 * hash + getResourceType().hashCode();
        hash = 31 * hash + getAttributes().hashCode();
        return hash;
    }
}
