/*
 * Created on Feb 15, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionReference;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IPredicateReference;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.pf.destiny.parser.PQLParser;

/**
 * A reference to another spec by name or by ID.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/SpecReference.java#1 $:
 */

public class SpecReference implements IDSpecRef, IExpressionReference {

    /** Cached referenced spec. */
    private IDSpec refSpec = null;

    /**
     * This interface defines the behavior of the reference.
     * Separate implementations for this interface exist
     * for references by name and references by ID.
     */
    private interface State {

        String getPrintableReference();

        String getReferencedName();

        Long getReferencedID();

        boolean isReferenceByName();
    }

    /**
     * This member represents the state of the reference
     * as either a reference by name or a reference by ID.
     * Changing the state has the effect of changing the type
     * of this object.
     */
    private State state;

    private class ByID implements State {

        /**
         * The optional ID of the referenced entity.
         * When the ID is set, the reference behaves like
         * a reference by ID, including the formatting of
         * itself as "ID <id>" when converted to PQL.
         * This variable has a value of null by default.
         */
        private Long referencedId = null;

        public ByID(Long id) {
            referencedId = id;
        }

        public String getPrintableReference() {
            return "ID " + referencedId;
        }

        public String getReferencedName() {
            throw new IllegalStateException("Spec reference by ID cannot produce a spec name: " + getPrintableReference());
        }

        public Long getReferencedID() {
            return referencedId;
        }

        public boolean isReferenceByName() {
            return false;
        }

        public String toString() {
            return "ID=" + referencedId;
        }
    }

    private class ByName implements State {

        /**
         * The optional name of the referenced entity.
         * When the ID is set, the reference behaves like
         * a reference by ID, including the formatting of
         * itself as "ID <id>" when converted to PQL.
         * This variable has a value of null by default.
         */
        private String referencedName = null;

        public ByName(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            referencedName = name;
        }

        public String getPrintableReference() {
            String printableName = PQLParser.quoteName(referencedName);
            StringBuffer res = new StringBuffer();
            res.append("GROUP=");
            assert printableName != null && printableName.length() != 0;
            if (printableName.charAt(0) != '"') {
                res.append('"');
            }
            res.append(printableName);
            if (printableName.charAt(0) != '"') {
                res.append('"');
            }
            return res.toString();
        }

        public String getReferencedName() {
            return referencedName;
        }

        public Long getReferencedID() {
            throw new IllegalStateException("Spec reference by name cannot produce an ID: " + getPrintableReference());
        }

        public boolean isReferenceByName() {
            return true;
        }

        public String toString() {
            return "NAME='" + referencedName + "'";
        }
    }

    /**
     * Creates a new SpecRefByName with the specified name and type.
     * @param referencedName the name of the referenced entity.
     * @param specType the type of the referenced entity.
     */
    public SpecReference(String referencedName) {
        if (referencedName == null) {
            throw new NullPointerException("referencedName");
        }
        setReferencedName(referencedName);
    }

    /**
     * Creates a new SpecRefByName with the specified ID and type.
     * @param referencedId the id of the referenced entity.
     * @param specType the type of the referenced entity.
     */
    public SpecReference(Long referencedId) {
        if (referencedId == null) {
            throw new NullPointerException("referencedId");
        }
        setReferencedID(referencedId);
    }

    /**
     * Returns the referencedName formatted for use in PQL.
     * @return the referencedName formatted for use in PQL.
     */
    public String getPrintableReference() {
        return state.getPrintableReference();
    }

    /**
     * Returns true if the reference is by name, and false if it is by ID.
     * @return true if the reference is by name, and false if it is by ID.
     */
    public boolean isReferenceByName() {
        return state.isReferenceByName();
    }

    /**
     * Returns the name of the spec to which this reference points.
     * @return the name of the spec to which this reference points.
     */
    public String getReferencedName() {
        return state.getReferencedName();
    }

    /**
     * Returns the ID of the spec to which this reference points (may be null).
     * @return the ID of the spec to which this reference points (may be null).
     */
    public Long getReferencedID() {
        return state.getReferencedID();
    }

    /**
     * Sets the referenced ID for this reference.
     * @param id the referenced ID for this reference.
     */
    public void setReferencedID(Long id) {
        state = new ByID(id);
    }

    /**
     * Sets the referenced name for this reference.
     * @param name the referenced name for this reference.
     */
    public void setReferencedName(String name) {
        state = new ByName(name);
    }

    /**
     * @see IDSpec#accept(IDSpecVisitor)
     */
    public void accept(IPredicateVisitor v, IPredicateVisitor.Order order) {
        v.visit((IPredicateReference)this);
    }

    /**
     * @see IPredicate#match(com.bluejungle.framework.patterns.IArguments)
     */
    public boolean match(IArguments request) {
        if (refSpec == null)
            return false;
        return refSpec.match(request);
    }

    /**
     * @see IExpression#evaluate(IArguments)
     */
    public IEvalValue evaluate(IArguments arg) {
        return IEvalValue.NULL;
    }

    /**
     * @see IExpression#buildRelation(RelationOp, IExpression)
     */
    public IRelation buildRelation(RelationOp op, IExpression rhs) {
        return new Relation(op, this, rhs);
    }

    /**
     * @see IExpression#acceptVisitor(IExpressionVisitor, IExpressionVisitor.Order)
     */
    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit(this);
    }

    public String toString() {
        return state == null ? "null" : state.toString();
    }

}
