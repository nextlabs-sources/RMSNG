package com.bluejungle.framework.expressions;

/*
 * Created on Feb 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/CompositePredicate.java#1 $:
 */

public class CompositePredicate implements ICompositePredicate {

    private final List<IPredicate> predicates = new ArrayList<IPredicate>();
    private BooleanOp op;

    public CompositePredicate(BooleanOp op, Collection<? extends IPredicate> predicates) {
        if (op == null) {
            throw new NullPointerException("op");
        }
        this.op = op;
        this.predicates.addAll(predicates);
    }

    public CompositePredicate(BooleanOp op, IPredicate... predicates) {
        if (op == null) {
            throw new NullPointerException("op");
        }

        this.op = op;

        for (IPredicate predicate : predicates) {
            this.predicates.add(predicate);
        }
    }

    public CompositePredicate(BooleanOp op, IPredicate predicate) {
        if (op == null) {
            throw new NullPointerException("op");
        }
        this.op = op;
        predicates.add(predicate);
    }

    /**
     * @see IPredicate#match(IArguments)
     */
    public boolean match(IArguments request) {
        return op.match(predicates, request);
    }

    public void accept(IPredicateVisitor visitor, IPredicateVisitor.Order order) {
        if (order.isPreOrder()) {
            visitor.visit((ICompositePredicate)this, true);
        }
        for (IPredicate element : predicates) {
            element.accept(visitor, order);
        }
        if (order.isPostOrder()) {
            visitor.visit((ICompositePredicate)this, false);
        }
    }

    public void addPredicate(IPredicate predicate) {
        predicates.add(predicate);
    }

    public IPredicate predicateAt(int i) {
        return (IPredicate)predicates.get(i);
    }

    public boolean removePredicate(IPredicate predicate) {
        return predicates.remove(predicate);
    }

    public IPredicate removePredicate(int i) {
        return (IPredicate)predicates.remove(i);
    }

    public BooleanOp getOp() {
        return op;
    }

    public int predicateCount() {
        return predicates.size();
    }

    public List<IPredicate> predicates() {
        return Collections.unmodifiableList(predicates);
    }

    /**
     * @param element
     * @param i
     */
    public void insertElement(IPredicate element, int i) {
        predicates.add(i, element);
    }

    /**
     * @param op
     */
    public void setOp(BooleanOp op) {
        if (op == null) {
            throw new NullPointerException("op");
        }
        this.op = op;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer();
        if (predicates.size() == 1) {
            // Use prefix notation
            rv.append(op).append(' ').append(predicates.iterator().next());
        } else {
            // Use infix notation
            boolean isFirst = true;
            for (IPredicate predicate : predicates) {
                if (!isFirst) {
                    rv.append(' ').append(op).append(' ');
                } else {
                    isFirst = false;
                }
                rv.append('(').append(predicate).append(')');
            }
        }
        return rv.toString();
    }
}
