/*
 * Created on Feb 24, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by NextLabs Inc.,
 * Redwood City CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IAttribute;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.expressions.ValueType;
import com.bluejungle.framework.utils.MultipartKey;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/SpecAttribute.java#1 $:
 */

public abstract class SpecAttribute implements IAttribute {

    // These strings should all be lower-case
    public static final String DESTINYTYPE_ATTR_NAME = "ce::destinytype";
    public static final String NAME_ATTR_NAME = "ce::name";
    public static final String RESOLVEDNAME_ATTR_NAME = "ce::resolved_name";
    public static final String ID_ATTR_NAME = "ce::id";
    public static final String FSCHECK_ATTR_NAME = "ce::filesystemcheck";
    public static final String FSO_ATTR_INCLUDED_ATTR_NAME = "ce::file_custom_attributes_included";
    public static final String NOCACHE_NAME = "ce::nocache";
    public static final String NATIVE_RESOURCE_NAME = "ce::nativeresname";
    public static final String REQUEST_CACHE_HINT = "ce::request_cache_hint";
    public static final String CONTENT_NAME = "content";
    // Used by Linux.  The original file name, uncoverted to lower case, canonicalized with "file://"
    public static final String ORIG_ATTR_NAME = "ce::orig";
    public static final String GET_EQUIVALENT_HOST_NAMES = "ce::get_equivalent_host_names";

    protected final String name;

    protected SpecAttribute(String name, MultipartKey key) {
        this.name = name;
        register(key, this);
    }

    public final String getName() {
        return name;
    }

    /**
     * @see IExpression#acceptVisitor(IExpressionVisitor, IExpressionVisitor.Order)
     */
    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit((IAttribute)this);
    }

    public String getObjectTypeName() {
        return getSpecType().getName();
    }

    /**
     * Computes the value of this attribute for the given
     * arguments
     *
     * @param args arguments
     * @return value of the attribute
     */
    public abstract IEvalValue evaluate(IArguments args);

    public IRelation buildRelation(RelationOp op, String val) {
        return new Relation(op, this, this.build(val));
    }

    public IRelation buildRelation(RelationOp op, IExpression rhs) {
        if (rhs instanceof Constant && rhs != Constant.NULL && rhs != Constant.EMPTY) {
            return buildRelation(op, ((Constant)rhs).getRepresentation());
        } else {
            return new Relation(op, this, rhs);
        }
    }

    public String toString() {
        return getName();
    }

    /**
     * Builds a constant of the right type for this attribute
     * based on a string value
     *
     * @param str value to build a constant from
     * @return Constant of the right type
     */
    public Constant build(String str) {
        return Constant.build(str);
    }

    /**
     * @return a set of RelationOps that are valid for this
     * attribute.  All other operations are invalid
     */
    public abstract Set<RelationOp> validOperators();

    /**
     * @return ValueType of this attribute
     */
    public abstract ValueType getValueType();

    public abstract SpecType getSpecType();

    protected static synchronized SpecAttribute getRegistered(MultipartKey key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!instances.containsKey(key)) {
            throw new IllegalArgumentException("Attribute does not exist: " + key);
        }
        return (SpecAttribute)instances.get(key);
    }

    protected static synchronized boolean isRegistered(MultipartKey key) {
        return instances.containsKey(key);
    }

    protected static synchronized void register(MultipartKey key, SpecAttribute attr) {
        if (!instances.containsKey(key)) {
            instances.put(key, attr);
        }
    }

    private static Map<MultipartKey, SpecAttribute> instances = new HashMap<MultipartKey, SpecAttribute>();

}
