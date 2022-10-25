package com.bluejungle.framework.expressions;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $ $
 */

/**
 * An interface for immutable relation predicates.
 */
public interface IRelation extends IPredicate {

    /**
     * Returns the relation operation of this relation.
     * @return the relation operation of this relation
     */
    RelationOp getOp();

    /**
     * Returns the left-hand side of this relation.
     * @return the left-hand side of this relation.
     */
    IExpression getLHS();

    /**
     * Returns the right-hand side of this relation.
     * @return the right-hand side of this relation.
     */
    IExpression getRHS();
}
