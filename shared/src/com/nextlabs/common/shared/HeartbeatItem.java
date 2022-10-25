package com.nextlabs.common.shared;

public class HeartbeatItem {

    private String name;
    private String serialNumber;
    private Object content;

    public HeartbeatItem() {
    }

    public HeartbeatItem(String name, String serialNumber) {
        this.name = name;
        this.serialNumber = serialNumber;
    }

    public boolean hasNewContent(String serialNumber) {
        return content != null && !this.serialNumber.equals(serialNumber);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public Object getContent() {
        return content;
    }
}
