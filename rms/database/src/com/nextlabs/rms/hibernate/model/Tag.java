package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "tag", uniqueConstraints = {
    @UniqueConstraint(name = "u_tag_name", columnNames = { "name", "tenant_id", "type" }),
    @UniqueConstraint(name = "u_tag_order", columnNames = { "tenant_id", "order_id", "type" }) })
@NamedQueries(value = {
    @NamedQuery(name = "deleteTagsByTenant", query = "delete from Tag where tenant.id = :tenantId") })
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "tag_id_seq")
    @SequenceGenerator(name = "tag_id_seq", sequenceName = "tag_id_seq")
    @Column(unique = true, nullable = false)
    private Integer id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_time", nullable = false)
    private Date creationTime;

    @Column(length = 2000)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified", nullable = false)
    private Date lastModified;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column
    private Integer type;

    //bi-directional many-to-one association to Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tag_tenant"))
    private Tenant tenant;

    public Tag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Tag() {
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreationTime() {
        return this.creationTime;
    }

    public void setCreationTime(Date date) {
        this.creationTime = date;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(Date date) {
        this.lastModified = date;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrderId() {
        return this.orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Tenant getTenant() {
        return this.tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public static enum TagType {
        PROJECT
    }

}
