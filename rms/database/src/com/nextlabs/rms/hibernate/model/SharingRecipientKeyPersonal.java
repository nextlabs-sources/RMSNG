package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SharingRecipientKeyPersonal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private String email;

    public SharingRecipientKeyPersonal() {
    }

    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "email", nullable = false, length = 255)
    public String getEmail() {
        return email;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SharingRecipientKeyPersonal) {
            SharingRecipientKeyPersonal oth = (SharingRecipientKeyPersonal)obj;
            return StringUtils.equals(getDuid(), oth.getDuid()) && StringUtils.equals(getEmail(), oth.getEmail());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(getDuid());
        hash = hash * 7 + Objects.hash(getEmail());
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("duid:").append(duid);
        sb.append(", email:").append(email);
        return sb.toString();
    }
}
