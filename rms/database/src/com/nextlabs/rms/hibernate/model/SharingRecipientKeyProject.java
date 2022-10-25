package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SharingRecipientKeyProject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duid;
    private int projectId;

    public SharingRecipientKeyProject() {
    }

    @Column(name = "duid", nullable = false, length = 36)
    public String getDuid() {
        return duid;
    }

    @Column(name = "project_id", nullable = false)
    public int getProjectId() {
        return projectId;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SharingRecipientKeyProject) {
            SharingRecipientKeyProject oth = (SharingRecipientKeyProject)obj;
            return StringUtils.equals(getDuid(), oth.getDuid()) && getProjectId() == oth.getProjectId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(getDuid());
        hash = hash * 7 + getProjectId();
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("duid:").append(duid);
        sb.append(", projectId:").append(projectId);
        return sb.toString();
    }
}
