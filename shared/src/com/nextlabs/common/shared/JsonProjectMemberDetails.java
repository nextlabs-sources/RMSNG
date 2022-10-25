package com.nextlabs.common.shared;

public class JsonProjectMemberDetails extends JsonProjectMember {

    String inviterDisplayName;
    String inviterEmail;

    public String getInviterDisplayName() {
        return inviterDisplayName;
    }

    public void setInviterDisplayName(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public String getInviterEmail() {
        return inviterEmail;
    }

    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }
}
