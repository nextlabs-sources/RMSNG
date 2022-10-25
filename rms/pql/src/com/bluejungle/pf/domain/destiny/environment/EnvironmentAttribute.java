/*
 * Created on May 22, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/environment/EnvironmentAttribute.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.environment;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IAttribute;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.framework.utils.MultipartKey;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class EnvironmentAttribute implements IAttribute {

    private static Map<MultipartKey, EnvironmentAttribute> instances = new HashMap<MultipartKey, EnvironmentAttribute>();
    private static final String REQUEST_ID_NAME = "request_id";
    private static final EnvironmentAttribute REQUEST_ID = new EnvironmentAttribute(REQUEST_ID_NAME) {
    };

    private final String name;

    private EnvironmentAttribute(String name) {
        this.name = name;
        register(makeKey(name), this);
    }

    public String getName() {
        return name;
    }

    public String getObjectTypeName() {
        return "environment";
    }

    public String getObjectSubTypeName() {
        return null;
    }

    public IEvalValue evaluate(IArguments args) {
        if (!(args instanceof IEvaluationRequest)) {
            return IEvalValue.NULL;
        }

        DynamicAttributes env = ((IEvaluationRequest)args).getEnvironment();

        IEvalValue res = env.get(name);

        return (res == null) ? IEvalValue.NULL : res;
    }

    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit((IAttribute)this);
    }

    public IRelation buildRelation(RelationOp op, IExpression rhs) {
        return new Relation(op, this, rhs);
    }

    private static MultipartKey makeKey(String name) {
        return new MultipartKey(name);
    }

    public static EnvironmentAttribute forName(String name) {
        MultipartKey key = makeKey(name);

        if (isRegistered(key)) {
            return getRegistered(key);
        } else {
            return new EnvironmentAttribute(name) {
            };
        }
    }

    protected static synchronized EnvironmentAttribute getRegistered(MultipartKey key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!instances.containsKey(key)) {
            throw new IllegalArgumentException("Attribute does not exist: " + key);
        }
        return instances.get(key);
    }

    protected static synchronized boolean isRegistered(MultipartKey key) {
        return instances.containsKey(key);
    }

    protected static synchronized void register(MultipartKey key, EnvironmentAttribute attr) {
        if (!instances.containsKey(key)) {
            instances.put(key, attr);
        }
    }
}
