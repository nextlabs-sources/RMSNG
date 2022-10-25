package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "tenant_user_attribute", uniqueConstraints = {
    @UniqueConstraint(name = "u_tenant_attrib_name", columnNames = { "tenant_id", "name" }) })
public class TenantUserAttribute implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private Tenant tenant;
    private String name;
    private boolean custom;
    private boolean selected;

    public TenantUserAttribute() {
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false, length = 36)
    public String getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant_user_attrib_tenant"))
    public Tenant getTenant() {
        return tenant;
    }

    @Column(name = "name", nullable = false, length = 50)
    public String getName() {
        return name;
    }

    @Column(name = "is_custom", nullable = false)
    public boolean isCustom() {
        return custom;
    }

    @Column(name = "is_selected", nullable = false)
    public boolean isSelected() {
        return selected;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
