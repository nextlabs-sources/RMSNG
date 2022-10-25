package com.nextlabs.rms.cc.pojos;

import java.util.List;

public class ControlCenterComponentRequest {

    private String operator;
    private List<ControlCenterIdWrapper> components;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<ControlCenterIdWrapper> getComponents() {
        return components;
    }

    public void setComponents(List<ControlCenterIdWrapper> components) {
        this.components = components;
    }

    public static class ControlCenterIdWrapper {

        private long id;

        public ControlCenterIdWrapper(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

    }
}
