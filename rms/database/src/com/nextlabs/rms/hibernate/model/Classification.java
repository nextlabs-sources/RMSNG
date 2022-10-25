package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants.TokenGroupType;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "classification", uniqueConstraints = {
    @UniqueConstraint(name = "u_tenant_clf_name", columnNames = { "name", "tenant_id" }) })
@NamedQueries(value = {
    @NamedQuery(name = "deleteClassificationsByTenant", query = "delete from Classification where tenant.id = :tenantId"),
    @NamedQuery(name = "deleteClassificationsByProject", query = "delete from Classification where project.id = :projectId") })
public class Classification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Tenant tenant;
    private Project project;
    private boolean multiSel;
    private boolean mandatory;
    private String labels;
    private String parentId;
    private int orderId;
    private TokenGroupType type;

    public Classification() {

    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @Column(name = "name", nullable = false, length = 60)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_clf_tenant"))
    public Tenant getTenant() {
        return tenant;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_clf_project"))
    public Project getProject() {
        return project;
    }

    @Column(name = "is_multi_sel", nullable = false)
    public boolean isMultiSel() {
        return multiSel;
    }

    @Column(name = "is_mandatory", nullable = false)
    public boolean isMandatory() {
        return mandatory;
    }

    @Column(name = "labels", nullable = false, length = 1200)
    public String getLabels() {
        return labels;
    }

    @Column(name = "parent_id", nullable = true, length = 36)
    public String getParentId() {
        return parentId;
    }

    @Column(name = "order_id", nullable = false)
    public int getOrderId() {
        return orderId;
    }

    @Column(name = "type", nullable = false)
    public TokenGroupType getType() {
        return type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setMultiSel(boolean multiSel) {
        this.multiSel = multiSel;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setType(TokenGroupType type) {
        this.type = type;
    }
}
