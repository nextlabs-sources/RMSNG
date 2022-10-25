/*
 * Created on May 3, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionReference;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.utils.StringUtils;

/**
 * Represents a reference to a location
 * For now, all location references are by name
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/LocationReference.java#1 $:
 */

public class LocationReference implements IExpressionReference {

    private String refLocationName;

    LocationReference(String refName) {
        if (refName == null) {
            throw new NullPointerException("refName");
        }

        this.refLocationName = refName;
    }

    /**
     * Returns the refLocationName.
     * @return the refLocationName.
     */
    public String getRefLocationName() {
        return this.refLocationName;
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#evaluate(com.bluejungle.framework.expressions.IArguments)
     */
    public IEvalValue evaluate(IArguments arg) {
        // TODO: Figure out where locations should be coming from, SubjectManager is a temporary solution
        IDSubjectManager manager = (IDSubjectManager)ComponentManagerFactory.getComponentManager().getComponent(IDSubjectManager.COMP_INFO);
        Location location = manager.getLocation(refLocationName);
        if (location == null) {
            throw new RuntimeException("Cannot find definition for location " + refLocationName);
        }

        return SubjectAttribute.INET_ADDRESS.build(location.getValue()).getValue();
    }

    public String toString() {
        return '"' + StringUtils.escape(refLocationName) + '"';
    }

    public Constant build(String str) {
        return SubjectAttribute.INET_ADDRESS.build(str);
    }

    public IRelation buildRelation(RelationOp op, String value) {
        IExpression rhs = build(value);
        return new Relation(op, this, rhs);
    }

    public IRelation buildRelation(RelationOp op, IExpression expr) {
        if (expr instanceof Constant) {
            return buildRelation(op, ((Constant)expr).getRepresentation());
        } else {
            return new Relation(op, this, expr);
        }
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#acceptVisitor(com.bluejungle.framework.expressions.IExpressionVisitor, com.bluejungle.framework.expressions.IExpressionVisitor.Order)
     */
    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit(this);
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpressionReference#getPrintableReference()
     */
    public String getPrintableReference() {
        return toString();
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpressionReference#isReferenceByName()
     */
    public boolean isReferenceByName() {
        return true;
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpressionReference#getReferencedName()
     */
    public String getReferencedName() {
        return refLocationName;
    }
}
