package com.bluejungle.framework.expressions;

/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IPredicate.java#1 $:
 */

public interface IPredicate {

    /**
     * Evaluates the predicate in the context of the given set of arguments,
     * and returns the result.
     * @param arguments the set of arguments for evaluation.
     * @return the result of evaluating the predicate in the context of the
     * given set of arguments.
     */
    boolean match(IArguments arguments);

    /**
     * Accepts the visitor for walking the predicate tree.
     * @param visitor the visitor to walk the predicate tree.
     * @param order the order of visiting (pre- or post-order).
     */
    void accept(IPredicateVisitor visitor, IPredicateVisitor.Order order);

}
