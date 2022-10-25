package com.bluejungle.framework.expressions;

import java.util.List;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $ $
 */

/**
 * An interface for immutable composite predicates.
 */
public interface ICompositePredicate extends IPredicate {

    /**
     * Returns the composition operator of this composite predicate.
     * @return the composition operator of this composite predicate.
     */
    BooleanOp getOp();

    /**
     * Returns the number of predicates inside of this composite predicate.
     * @return the number of predicates inside of this composite predicate.
     */
    int predicateCount();

    /**
     * Returns the predicate at the specified position.
     * @param i the index for which to access the predicate.
     * @return the predicate at the specified position.
     */
    IPredicate predicateAt(int i);

    /**
     * Returns an immutable collection of predicates.
     * @return an immutable collection of predicates.
     */
    List<IPredicate> predicates();
}
