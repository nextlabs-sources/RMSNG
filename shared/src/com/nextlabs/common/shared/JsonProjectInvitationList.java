package com.nextlabs.common.shared;

import java.util.List;

public class JsonProjectInvitationList {

    private Long totalInvitations;
    private List<JsonProjectInvitation> invitations;

    public List<JsonProjectInvitation> getInvitations() {
        return invitations;
    }

    public Long getTotalInvitations() {
        return totalInvitations;
    }

    public void setInvitations(List<JsonProjectInvitation> invitations) {
        this.invitations = invitations;
    }

    public void setTotalInvitations(Long totalInvitations) {
        this.totalInvitations = totalInvitations;
    }
}
