/*
 * Created on Feb 17, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */
package com.bluejungle.framework.expressions;

import java.util.Date;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/Constant.java#1 $:
 */

public class Constant implements IExpression {

    public static final Constant NULL = new Constant(IEvalValue.NULL);

    public static final Constant EMPTY = new Constant(IEvalValue.EMPTY);

    public static Constant build(IMultivalue mv, String rep) {
        return new Constant(EvalValue.build(mv), rep);
    }

    public static Constant build(String s) {
        return new Constant(EvalValue.build(s));
    }

    public static Constant build(String s, String rep) {
        return new Constant(EvalValue.build(s), rep);
    }

    public static Constant build(long l) {
        return new Constant(EvalValue.build(l));
    }

    public static Constant build(long l, String rep) {
        return new Constant(EvalValue.build(l), rep);
    }

    public static Constant build(Date d) {
        return new Constant(EvalValue.build(d));
    }

    public static Constant build(Date d, String rep) {
        return new Constant(EvalValue.build(d), rep);
    }

    protected IEvalValue val;
    protected String rep;

    protected Constant(IEvalValue val) {
        this(val, toString(val));
    }

    protected Constant(IEvalValue val, String rep) {
        if (val == null) {
            throw new NullPointerException("value");
        }
        if (rep == null) {
            throw new NullPointerException("representation");
        }
        this.rep = rep;
        this.val = val;
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#evaluate(com.bluejungle.framework.expressions.IArguments)
     */
    public IEvalValue evaluate(IArguments arg) {
        return val;
    }

    public IEvalValue getValue() {
        return val;
    }

    public String getRepresentation() {
        return rep;
    }

    public String toString() {
        return val.getType().formatRepresentation(rep);
    }

    private static String toString(IEvalValue val) {
        return (val != null) ? String.valueOf(val.getValue()) : "<NULL>";
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#buildRelation(com.bluejungle.framework.expressions.RelationOp, com.bluejungle.framework.expressions.IExpression)
     */
    public IRelation buildRelation(RelationOp op, IExpression rhs) {
        return new Relation(op, this, rhs);
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#acceptVisitor(com.bluejungle.framework.expressions.IExpressionVisitor)
     */
    public void acceptVisitor(IExpressionVisitor visitor, IExpressionVisitor.Order order) {
        visitor.visit((Constant)this);
    }

}
