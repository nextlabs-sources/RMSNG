package com.bluejungle.framework.expressions;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/DefaultPredicateVisitor.java#1 $
 */

/**
 * Provides default (empty) implementation for the visiting method.
 */
public class DefaultPredicateVisitor implements IPredicateVisitor {

    /**
     * An empty implementation.
     * @see com.bluejungle.framework.expressions.IPredicateVisitor#visit(com.bluejungle.framework.expressions.ICompositePredicate, boolean)
     */
    public void visit(ICompositePredicate pred, boolean preorder) {
    }

    /**
     * An empty implementation.
     * @see com.bluejungle.framework.expressions.IPredicateVisitor#visit(com.bluejungle.framework.expressions.IPredicateReference)
     */
    public void visit(IPredicateReference pred) {
    }

    /**
     * An empty implementation.
     * @see com.bluejungle.framework.expressions.IPredicateVisitor#visit(com.bluejungle.framework.expressions.IRelation)
     */
    public void visit(IRelation pred) {
    }

    /**
     * An empty implementation.
     * @see com.bluejungle.framework.expressions.IPredicateVisitor#visit(com.bluejungle.framework.expressions.IPredicate)
     */
    public void visit(IPredicate pred) {
    }

}
