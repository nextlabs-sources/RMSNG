package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;
    private String expiry;
    private String watermark;
    private String preferences;

    public UserPreferences() {
    }

    @Id
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_id"))
    public User getUser() {
        return user;
    }

    @Column(name = "expiry", nullable = true)
    public String getExpiry() {
        return expiry;
    }

    @Column(name = "watermark", length = 255, nullable = true)
    public String getWatermark() {
        return watermark;
    }

    @Column(name = "preferences", nullable = true)
    public String getPreferences() {
        return preferences;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }
}
