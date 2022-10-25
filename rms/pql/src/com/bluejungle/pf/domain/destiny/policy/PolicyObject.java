/*
 * Created on Dec 23, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */
package com.bluejungle.pf.domain.destiny.policy;

import com.bluejungle.pf.domain.destiny.action.DAction;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.policy.IPolicyObject;

import java.io.Serializable;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/policy/PolicyObject.java#1 $:
 */

public abstract class PolicyObject implements IPolicyObject, Serializable {

    private Long id;
    private String name;
    protected String description;
    protected boolean computedPQL = false;
    protected String pql;

    /* PKENI
     * Policy is the only thing among the domainobjects which does not inherit from
     * DOBase.  The following really belong in DOBase.  Right now this is duplicated
     * in here and SpecBase, unification needed.
     */
    protected IDSubject owner;
    protected AccessPolicy accessPolicy;

    public PolicyObject(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * @see IAccessControlled#checkAccess(IDSubject, DAction)
     */
    public boolean checkAccess(IDSubject subject, DAction action) {
        return accessPolicy == null || accessPolicy.checkAccess(
                new IPResource() {

                    public IDSubject getOwner() {
                        return PolicyObject.this.owner;
                    }
                }
                , subject
                , action
                );
    }

    /**
     * @see IAccessControlled#getAccessPolicy()
     */
    public IAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    /**
     * @see IAccessControlled#setAccessPolicy(IAccessPolicy)
     */
    public void setAccessPolicy(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    /**
     * @see IAccessControlled#removeAccessPolicy()
     */
    public void removeAccessPolicy() {
        this.accessPolicy = new AccessPolicy();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @see IDomainObject#getOwner()
     */
    public IDSubject getOwner() {
        return owner;
    }

    /**
     * @see IDomainObject#setOwner(IDSubject)
     */
    public void setOwner(IDSubject owner) {
        this.owner = owner;
    }

}
