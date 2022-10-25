package com.nextlabs.rms.hibernate.model;

import com.nextlabs.common.shared.Constants;

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
@Table(name = "identity_provider")
public class IdentityProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private Constants.LoginType type;
    private Tenant tenant;
    private String attributes;
    private String userAttributeMap;

    public IdentityProvider() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "identity_provider_id_seq")
    @SequenceGenerator(name = "identity_provider_id_seq", sequenceName = "identity_provider_id_seq")
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "idp_type", nullable = false)
    public Constants.LoginType getType() {
        return type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_idp_details_tenant"))
    public Tenant getTenant() {
        return tenant;
    }

    @Column(name = "attributes", nullable = true)
    public String getAttributes() {
        return attributes;
    }

    @Column(name = "user_attribute_map", nullable = true, length = 1200)
    public String getUserAttributeMap() {
        return userAttributeMap;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setType(Constants.LoginType idpType) {
        this.type = idpType;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setAttributes(String details) {
        this.attributes = details;
    }

    public void setUserAttributeMap(String userAttributeMap) {
        this.userAttributeMap = userAttributeMap;
    }
}
