package com.bluejungle.pf.domain.destiny.action;

/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/action/ActionAttribute.java#1 $
 */

import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.expressions.ValueType;
import com.bluejungle.framework.utils.MultipartKey;
import com.bluejungle.pf.domain.destiny.common.SpecAttribute;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.common.SpecType;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class ActionAttribute extends SpecAttribute {

    private static final Set<RelationOp> STRING_OPS;

    static {
        Set<RelationOp> stringOps = new HashSet<RelationOp>(3);
        stringOps.add(RelationOp.EQUALS);
        stringOps.add(RelationOp.NOT_EQUALS);
        STRING_OPS = Collections.unmodifiableSet(stringOps);
    }

    private ActionAttribute(String name) {
        super(name, key(name));
    }

    public static final ActionAttribute NAME = new ActionAttribute("name") {

        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof IEvaluationRequest)) {
                return null;
            }
            IAction action = ((IEvaluationRequest)args).getAction();
            if (!(action instanceof IDAction)) {
                return null;
            }
            return EvalValue.build(action.getName());
        }

        public Constant build(String pattern) {
            return Constant.build(pattern);
        }

        /**
         * @see SpecAttribute#validOperators()
         */
        public Set<RelationOp> validOperators() {
            return STRING_OPS;
        }

        /**
         * @see SpecAttribute#validTypes()
         */
        public ValueType getValueType() {
            return ValueType.STRING;
        }
    };

    public SpecType getSpecType() {
        return SpecType.ACTION;
    }

    /**
     * @see IAttribute#getObjectSubTypeName()
     */
    public String getObjectSubTypeName() {
        return null;
    }

    public static boolean existsElement(String name) {
        return isRegistered(key(name));
    }

    public static ActionAttribute getElement(String name) {
        return (ActionAttribute)getRegistered(key(name));
    }

    private static MultipartKey key(String name) {
        return new MultipartKey(name, ActionAttribute.class);
    }

}
