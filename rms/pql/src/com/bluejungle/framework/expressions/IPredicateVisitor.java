package com.bluejungle.framework.expressions;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $ $
 */

import com.bluejungle.framework.patterns.EnumBase;

/**
 * Implementat this interface to code double-dispatch
 * on subclasses of IPredicate.
 */
public interface IPredicateVisitor {

    /**
     * Visits a composite predicate.
     * @param pred the composite predicate to visit.
     * @param preorder when true, indicates that this visit preceeds the visits of the components;
     * when false, indicates that the components have already been visited.
     */
    void visit(ICompositePredicate pred, boolean preorder);

    /**
     * Visits a predicate reference.
     * @param pred the predicate reference to visit.
     */
    void visit(IPredicateReference pred);

    /**
     * Visits a relation.
     * @param pred the relation to visit.
     */
    void visit(IRelation pred);

    /**
     * Visits a leaf or an externally defined predicate.
     * @param pred the leaf predicate to visit.
     */
    void visit(IPredicate pred);

    /**
     * An enumeration to define the visiting order.
     */
    public static abstract class Order extends EnumBase {

        private Order(String name) {
            super(name, Order.class);
        }

        public abstract boolean isPreOrder();

        public abstract boolean isPostOrder();
    }

    /** Pre-order visiting oreder. */
    public static final Order PREORDER = new Order("preorder") {

        private static final long serialVersionUID = 1L;

        public boolean isPreOrder() {
            return true;
        }

        public boolean isPostOrder() {
            return false;
        }
    };

    /** Post-order visiting oreder. */
    public static final Order POSTORDER = new Order("postorder") {

        private static final long serialVersionUID = 1L;

        public boolean isPreOrder() {
            return false;
        }

        public boolean isPostOrder() {
            return true;
        }
    };

    /** Pre-and-post-order visiting oreder. */
    public static final Order PREPOSTORDER = new Order("prepostorder") {

        private static final long serialVersionUID = 1L;

        public boolean isPreOrder() {
            return true;
        }

        public boolean isPostOrder() {
            return true;
        }
    };

}
