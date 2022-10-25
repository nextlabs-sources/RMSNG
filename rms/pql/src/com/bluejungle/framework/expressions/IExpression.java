/*
 * Created on Feb 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IExpression.java#1 $:
 */

public interface IExpression {

    IEvalValue evaluate(IArguments arg);

    IRelation buildRelation(RelationOp op, IExpression rhs);

    void acceptVisitor(IExpressionVisitor visitor, IExpressionVisitor.Order order);
}
