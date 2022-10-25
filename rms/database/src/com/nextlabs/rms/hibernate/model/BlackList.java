package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "black_list")
public class BlackList implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private User user;
    private Date creationTime;
    private Date expiration;

    public BlackList() {
    }

    @Id
    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_black_list_user"))
    public User getUser() {
        return user;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    public Date getCreationTime() {
        return creationTime;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration", nullable = false)
    public Date getExpiration() {
        return expiration;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
}
