package com.bluejungle.pf.domain.destiny.policy;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/policy/PolicyFolder.java#1 $
 */

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.utils.ObjectHelper;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.destiny.action.DAction;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.misc.IAccessControlled;

/**
 * Instances of this class represent policy folders.
 */
public class PolicyFolder implements IHasId, IAccessControlled {

    /** Represents the ID of this policy folder. */
    private Long id;

    /** Represents the name of this policy folder. */
    private String name;

    /** Represents the description of this policy folder. */
    private String description;

    /** Represents the development status of this policy folder. */
    private DevelopmentStatus status;

    /** Represents the owner of this policy folder. */
    private IDSubject owner;

    /** The access policy for this policy folder. */
    private AccessPolicy accessPolicy;

    /**
     * Creates a new policy folder with the given ID.
     * @param id the ID of the policy folder.
     */
    public PolicyFolder(Long id, String name, String description, DevelopmentStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    /**
     * @see com.bluejungle.framework.domain.IHasId#getId()
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the new ID for this policy folder.
     * @param id the new ID for this policy folder.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Accesses the description of this policy folder.
     * @return the description of this policy folder.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description of this policy folder.
     * @param description a new description of this policy folder.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Accesses the name of this policy folder.
     * @return the name of this policy folder.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this policy folder.
     * @param name a new name of this policy folder.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Accesses the development status of this policy folder.
     * @return the development status of this policy folder.
     */
    public DevelopmentStatus getStatus() {
        return status;
    }

    /**
     * Sets the development status of this policy folder.
     * @param status a new development status of this policy folder.
     */
    public void setStatus(DevelopmentStatus status) {
        this.status = status;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#getOwner()
     */
    public IDSubject getOwner() {
        return owner;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#setOwner(com.bluejungle.pf.domain.destiny.subject.IDSubject)
     */
    public void setOwner(IDSubject owner) {
        this.owner = owner;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#checkAccess(com.bluejungle.pf.domain.destiny.subject.IDSubject, com.bluejungle.pf.domain.destiny.action.DAction)
     */
    public boolean checkAccess(IDSubject subject, DAction action) {
        return accessPolicy == null || accessPolicy.checkAccess(
                new IPResource() {

                    public IDSubject getOwner() {
                        return owner;
                    }
                }
                , subject
                , action
                );
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#getAccessPolicy()
     */
    public IAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#setAccessPolicy(com.bluejungle.pf.domain.destiny.common.AccessPolicy)
     */
    public void setAccessPolicy(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IAccessControlled#removeAccessPolicy()
     */
    public void removeAccessPolicy() {
        this.accessPolicy = new AccessPolicy();
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.resource.IResource#getOwnerValue()
     */
    public IEvalValue getOwnerValue() {
        if (getOwner() != null) {
            return EvalValue.build(getOwner().getUid());
        } else {
            return IEvalValue.NULL;
        }
    }

    public int hashCode() {
        return ObjectHelper.nullSafeHashCode(getId());
    }

    public boolean equals(Object other) {
        return (this == other) ||
                ((other instanceof PolicyFolder)
                        && (getId() != null)
                        && getId().equals(((PolicyFolder)other).getId())
                );
    }

}
