package com.nextlabs.rms.cc.pojos;

import java.util.List;

public class ControlCenterTagFilters {

    private ControlCenterTagFilter[] tagsFilters;

    public ControlCenterTagFilter[] getTagsFilters() {
        return tagsFilters;
    }

    public void setTagsFilters(ControlCenterTagFilter[] tagFilters) {
        this.tagsFilters = tagFilters;
    }

    public static class ControlCenterTagFilter {

        private String operator;
        private List<ControlCenterTag> tags;

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public List<ControlCenterTag> getTags() {
            return tags;
        }

        public void setTags(List<ControlCenterTag> tags) {
            this.tags = tags;
        }

    }

}
