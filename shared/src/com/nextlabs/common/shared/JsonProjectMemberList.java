package com.nextlabs.common.shared;

import java.util.List;

public class JsonProjectMemberList {

    private Long totalMembers;
    private List<JsonProjectMember> members;

    public List<JsonProjectMember> getMembers() {
        return members;
    }

    public Long getTotalMembers() {
        return totalMembers;
    }

    public void setMembers(List<JsonProjectMember> members) {
        this.members = members;
    }

    public void setTotalMembers(Long totalMembers) {
        this.totalMembers = totalMembers;
    }
}
