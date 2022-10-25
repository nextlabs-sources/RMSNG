/*
 * Created on May 9, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

import com.bluejungle.framework.patterns.EnumBase;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IExpressionVisitor.java#1 $:
 */

public interface IExpressionVisitor {

    /**
     * Visits an attribute expression
     * @param attribute
     */
    void visit(IAttribute attribute);

    /**
     * Visits a constant expression
     * @param constant
     */
    void visit(Constant constant);

    /**
     * Visits a reference to an expression
     * @param ref
     */
    void visit(IExpressionReference ref);

    /**
     * Visit a function expression
     * @param func
     */
    void visit(IFunctionApplication func);

    /**
     * Visit a leaf-level externally-defined expression
     * @param expression
     */
    void visit(IExpression expression);

    /**
     * An enumeration to define the visiting order.
     */
    public static class Order extends EnumBase {

        private static final long serialVersionUID = 1L;

        private Order(String name) {
            super(name, Order.class);
        }
    }

    /** Pre-order visiting oreder. */
    public static final Order PREORDER = new Order("preorder") {

        private static final long serialVersionUID = 1L;
    };

    /** Post-order visiting oreder. */
    public static final Order POSTORDER = new Order("postorder") {

        private static final long serialVersionUID = 1L;
    };

}
