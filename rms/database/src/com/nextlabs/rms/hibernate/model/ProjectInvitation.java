/**
 *
 */
package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author nnallagatla
 *
 */
@Entity
@Table(name = "project_invitation")
public class ProjectInvitation implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 4263623790659890005L;

    private long id;
    private Project project;
    private User inviter;
    private String inviteeEmail;
    private Date inviteTime;
    private Date expireDate;
    private Date actionTime;
    private Status status;
    private String comment;
    private String invitationMsg;

    public ProjectInvitation() {

    }

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "project_invitation_id_seq")
    @SequenceGenerator(name = "project_invitation_id_seq", sequenceName = "project_invitation_id_seq")
    public long getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pi_project"))
    public Project getProject() {
        return project;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pi_user"))
    public User getInviter() {
        return inviter;
    }

    @Column(name = "invitee_email", length = 150, nullable = false)
    public String getInviteeEmail() {
        return inviteeEmail;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invite_time", nullable = false)
    public Date getInviteTime() {
        return inviteTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expire_date", nullable = false)
    public Date getExpireDate() {
        return expireDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "action_time")
    public Date getActionTime() {
        return actionTime;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Column(name = "comments", length = 250)
    public String getComment() {
        return comment;
    }

    @Column(name = "invitation_msg", length = 250)
    public String getInvitationMsg() {
        return invitationMsg;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setInviter(User inviter) {
        this.inviter = inviter;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public void setInviteTime(Date inviteTime) {
        this.inviteTime = inviteTime;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setInvitationMsg(String invitationMsg) {
        this.invitationMsg = invitationMsg;
    }

    public static enum Status {
        PENDING,
        SENT,
        ACCEPTED,
        DECLINED,
        REVOKED,
        EXPIRED
    }

    @Override
    public String toString() {
        return new StringBuilder("id:").append(id).append(" projectId:").append(project.getId()).append(" Inviter: ").append(inviter.getId()).append(" Invitee:").append(inviteeEmail).append(" inviteTime:").append(inviteTime).append(" status:").append(status.name()).toString();
    }
}
