/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/environment/RemoteAccessAttribute.java#1 $
 */
package com.bluejungle.pf.domain.destiny.environment;

import com.bluejungle.framework.expressions.Constant;
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
import com.bluejungle.pf.domain.destiny.subject.SubjectAttribute;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

/**
 * This attribute represents the flag indicating
 * if the process is using a remote access facility.
 *
 * @author sergey
 */
public abstract class RemoteAccessAttribute implements IAttribute {

    private static final IEvalValue LOCAL = EvalValue.build(0);

    private static final IEvalValue REMOTE = EvalValue.build(1);

    /**
     * The instance of the attribute for public use.
     */
    public static final RemoteAccessAttribute REMOTE_ACCESS = new RemoteAccessAttribute() {

        /**
         * @see IAttribute#getName()
         */
        public String getName() {
            return "REMOTE_ACCESS";
        }

        /**
         * @see IExpression#evaluate(IArguments)
         */
        public IEvalValue evaluate(IArguments arg) {
            if (arg instanceof IEvaluationRequest) {
                return ((IEvaluationRequest)arg).getRemoteAddress() != null ? REMOTE : LOCAL;
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
     * The instance of the attribute for public use.
     */
    public static final RemoteAccessAttribute REMOTE_ADDRESS = new RemoteAccessAttribute() {

        /**
         * @see IAttribute#getName()
         */
        public String getName() {
            return "REMOTE_ADDRESS";
        }

        /**
         * @see IExpression#evaluate(IArguments)
         */
        public IEvalValue evaluate(IArguments arg) {
            if (arg instanceof IEvaluationRequest) {
                String addr = ((IEvaluationRequest)arg).getRemoteAddress();
                if (addr != null) {
                    return EvalValue.build(addr);
                }
            }
            return EvalValue.NULL;
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
            if (rhs instanceof Constant) {
                rhs = SubjectAttribute.INET_ADDRESS.build(((Constant)rhs).getRepresentation());
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
