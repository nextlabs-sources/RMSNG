package com.nextlabs.common.shared;

public class JsonProjectInvitation {

    private Long invitationId;
    private String inviteeEmail;
    private String inviterDisplayName;
    private String inviterEmail;
    private Long inviteTime;
    private String code;
    private String invitationMsg;
    private JsonProject project;

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public String getInviterDisplay() {
        return inviterDisplayName;
    }

    public void setInviterDisplay(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public String getInviterEmail() {
        return inviterEmail;
    }

    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }

    public Long getInviteTime() {
        return inviteTime;
    }

    public void setInviteTime(Long inviteTime) {
        this.inviteTime = inviteTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public JsonProject getProject() {
        return project;
    }

    public void setProject(JsonProject project) {
        this.project = project;
    }

    public String getInvitationMsg() {
        return invitationMsg;
    }

    public void setInvitationMsg(String invitationMsg) {
        this.invitationMsg = invitationMsg;
    }
}
