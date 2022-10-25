package com.nextlabs.common.shared;

public class JsonABACMembershipObligation {

    private int[] tagIds;
    private int[] projectIds;
    private boolean allowAll;

    public int[] getTagIds() {
        return tagIds;
    }

    public void setTagIds(int[] tagIds) {
        this.tagIds = tagIds;
    }

    public int[] getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(int[] projectIds) {
        this.projectIds = projectIds;
    }

    public boolean isAllowAll() {
        return allowAll;
    }

    public void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }
}
