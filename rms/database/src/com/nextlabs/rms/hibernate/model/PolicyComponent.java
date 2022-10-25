package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.TokenGroupType;

import java.io.Serializable;

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

@Entity
@Table(name = "policy_component")
public class PolicyComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Tenant tenant;
    private Project project;
    private Long policyId;
    private String componentJson;
    private Constants.PolicyComponentType componentType;
    private Constants.PolicyModelType policyType;
    private Status status;
    private TokenGroupType type;

    public PolicyComponent() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "policy_component_id_seq")
    @SequenceGenerator(name = "policy_component_id_seq", sequenceName = "policy_component_id_seq")
    @Column(name = "id", nullable = false)
    public Long getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_policy_component_tenant"))
    public Tenant getTenant() {
        return tenant;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_policy_component_project"))
    public Project getProject() {
        return project;
    }

    @Column(name = "policy_id", nullable = false)
    public Long getPolicyId() {
        return policyId;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "component_type", nullable = false)
    public Constants.PolicyComponentType getComponentType() {
        return componentType;
    }

    @Column(name = "component_json")
    public String getComponentJson() {
        return componentJson;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "policy_model_type", nullable = false)
    public Constants.PolicyModelType getPolicyType() {
        return policyType;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type", nullable = false)
    public TokenGroupType getType() {
        return type;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public void setComponentJson(String componentJson) {
        this.componentJson = componentJson;
    }

    public void setPolicyType(Constants.PolicyModelType policyType) {
        this.policyType = policyType;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setType(TokenGroupType type) {
        this.type = type;
    }

    public void setComponentType(Constants.PolicyComponentType componentType) {
        this.componentType = componentType;
    }

    public static enum Status {
        ACTIVE,
        DELETED
    }
}
