/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/environment/HeartbeatAttribute.java#1 $
 */
package com.bluejungle.pf.domain.destiny.environment;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IAttribute;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

/**
 * This attribute represents the number of seconds since the last
 * successful heartbeat
 *
 * @author amorgan
 */
public abstract class HeartbeatAttribute implements IAttribute {

    public static final HeartbeatAttribute TIME_SINCE_LAST_HEARTBEAT = new HeartbeatAttribute() {

        /**
         * @see IAttribute#getName()
         */
        public String getName() {
            return "TIME_SINCE_LAST_HEARTBEAT";
        }

        /**
         * @see IExpression#evaluate(IArguments)
         */
        public IEvalValue evaluate(IArguments arg) {
            if (arg instanceof IEvaluationRequest) {
                long time = ((IEvaluationRequest)arg).getLastSuccessfulHeartbeat();
                return EvalValue.build(time);
            } else {
                return EvalValue.NULL;
            }
        }

        /**
         * @see IExpression#buildRelation(RelationOp, IExpression)
         */
        public IRelation buildRelation(RelationOp op, IExpression rhs) {
            if (op == null) {
                throw new NullPointerException("operator");
            }
            if (rhs == null) {
                throw new NullPointerException("rhs");
            }
            return new Relation(op, this, rhs);
        }
    };

    /**
     * @see IAttribute#getObjectSubTypeName()
     */
    public String getObjectSubTypeName() {
        return null;
    }

    /**
     * @see IAttribute#getObjectTypeName()
     */
    public String getObjectTypeName() {
        return "ENVIRONMENT";
    }

    /**
     * @see IExpression#acceptVisitor(IExpressionVisitor, IExpressionVisitor.Order)
     */
    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit(this);
    }

}
