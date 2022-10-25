package com.bluejungle.pf.domain.destiny.common;

/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.domain.DOBase;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.framework.utils.ObjectHelper;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.destiny.action.DAction;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/SpecBase.java#1 $
 */

public class SpecBase extends DOBase implements IDSpec {

    protected String name;
    protected String description;
    protected final IDSpecManager manager;
    private final SpecType specType;
    private IPredicate pred;
    private DevelopmentStatus status;
    protected IDSubject owner;
    protected AccessPolicy accessPolicy;
    protected boolean hidden;

    /**
     * Constructor
     * @param manager
     * @param specType
     * @param id
     * @param name
     * @param description
     * @param status
     * @param pred
     */
    public SpecBase(
        IDSpecManager manager
        , SpecType specType
        , Long id
        , String name
        , String description
        , DevelopmentStatus status
        , IPredicate pred
        , boolean hidden) {
        super(id);
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.description = description;
        this.manager = manager;
        if (specType == null) {
            throw new NullPointerException("specType");
        }
        this.specType = specType;
        if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;
        this.pred = pred;
        this.hidden = hidden;
    }

    /**
     * @see ISpec#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see ISpec#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the predicate embedded in this spec.
     * @return the predicate embedded in this spec.
     */
    public IPredicate getPredicate() {
        return pred;
    }

    /**
     * @see IDSpec#getStatus()
     */
    public DevelopmentStatus getStatus() {
        return status;
    }

    /**
     * Sets the description
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the name
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the type of this spec.
     * @return the type of this spec.
     */
    public SpecType getSpecType() {
        return specType;
    }

    /**
     * Returns the manager of this spec.
     * @return the manager of this spec.
     */
    public IDSpecManager getManager() {
        return manager;
    }

    /**
     * Changes the <code>IPredicate</code> of this spec.
     * @param pred the new <code>IPredicate</code> object.
     */
    public void setPredicate(IPredicate pred) {
        this.pred = pred;
    }

    /**
     * @see IDSpec#accept(IPredicateVisitor)
     */
    public void accept(IPredicateVisitor v, IPredicateVisitor.Order order) {
        pred.accept(v, order);
    }

    /**
     * @see IPredicate#match(IArguments)
     * @see IDSpec#setStatus(DevelopmentStatus)
     */
    public final boolean match(IArguments arguments) {
        if (pred == null) {
            throw new IllegalStateException("the spec is empty");
        }
        return pred.match(arguments);
    }

    /**
     * @see IDSpec#setStatus(DevelopmentStatus)
     */
    public void setStatus(DevelopmentStatus status) {
        this.status = status;
    }

    public String toString() {
        return name;
    }

    /**
     * @see IAccessControlled#checkAccess(IDSubject, DAction)
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
     * @see IAccessControlled#checkRoleAccess(IDSpec, DAction)
     */
    public boolean checkRoleAccess(IDSpec spec, DAction action) {
        return accessPolicy == null || accessPolicy.checkRoleAccess(spec, action);
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
        accessPolicy = new AccessPolicy();
    }

    /**
     * @see IDomainObject#getOwner()
     */
    public IDSubject getOwner() {
        return owner;
    }

    public IEvalValue getOwnerValue() {
        if (getOwner() != null) {
            return EvalValue.build(getOwner().getUid());
        } else {
            return IEvalValue.NULL;
        }
    }

    /**
     * @see IDomainObject#setOwner(IDSubject)
     */
    public void setOwner(IDSubject owner) {
        this.owner = owner;
    }

    /**
     * @see IDSpec#isHidden()
     */
    public boolean isHidden() {
        return hidden;
    }

    public int hashCode() {
        return ObjectHelper.nullSafeHashCode(getId());
    }

    public boolean equals(Object other) {
        return (this == other) ||
                ((other instanceof SpecBase)
                        && (getId() != null)
                        && getId().equals(((SpecBase)other).getId())
                );
    }

}
