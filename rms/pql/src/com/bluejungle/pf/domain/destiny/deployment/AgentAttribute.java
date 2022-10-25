/*
 * Created on May 6, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.deployment;

import com.bluejungle.domain.agenttype.AgentTypeEnumType;
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
import com.bluejungle.framework.patterns.EnumBase;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/deployment/AgentAttribute.java#1 $:
 */

public abstract class AgentAttribute extends EnumBase implements IAttribute {

    public static final AgentAttribute ID = new AgentAttribute("ID") {

        private static final long serialVersionUID = 1L;

        protected IExpression build(String constant) {
            try {
                long val = Long.parseLong(constant);
                return Constant.build(val, constant);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid id specified: " + constant, e);
            }
        }

        public IEvalValue evaluate(IArguments arg) {
            if (!(arg instanceof AgentRequest)) {
                return IEvalValue.NULL;
            }
            long agentId = ((AgentRequest)arg).getAgentId();
            return EvalValue.build(agentId);
        }

    };

    public static final AgentAttribute TYPE = new AgentAttribute("TYPE") {

        private static final long serialVersionUID = 1L;

        protected IExpression build(String typeName) {
            AgentTypeEnumType type = AgentTypeEnumType.getAgentType(typeName.toUpperCase());
            if (type == null) {
                throw new IllegalArgumentException("type");
            }
            return Constant.build(type.getName());
        }

        public IEvalValue evaluate(IArguments arg) {
            if (!(arg instanceof AgentRequest)) {
                return IEvalValue.NULL;
            }
            AgentTypeEnumType type = ((AgentRequest)arg).getAgentType();
            return EvalValue.build(type.getName());
        }

    };

    public String getObjectTypeName() {
        return "AGENT";
    }

    public String getObjectSubTypeName() {
        return null;
    }

    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit((IAttribute)this);
    }

    public IRelation buildRelation(RelationOp op, IExpression rhs) {
        if (rhs instanceof Constant) {
            rhs = build(((Constant)rhs).getRepresentation());
        }
        return new Relation(op, this, rhs);
    }

    public static AgentAttribute getAttribute(String name) {
        return getElement(name.toUpperCase(), AgentAttribute.class);
    }

    protected AgentAttribute(String name) {
        super(name, AgentAttribute.class);
    }

    protected abstract IExpression build(String constant);
}
