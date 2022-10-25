/*
 * Created on Feb 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

import java.util.Date;

/**
 * EvalValue represents a value that can be used in expression
 * evaluation.
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/EvalValue.java#1 $:
 */

public class EvalValue implements IEvalValue {

    private static final long serialVersionUID = 1L;

    public static IEvalValue build(String val) {
        return val == null ? NULL : new EvalValue(ValueType.STRING, val);
    }

    public static IEvalValue build(long val) {
        return new EvalValue(ValueType.LONG, new Long(val));
    }

    public static IEvalValue build(IMultivalue val) {
        return new EvalValue(ValueType.MULTIVAL, val);
    }

    public static IEvalValue build(Date d) {
        return new EvalValue(ValueType.DATE, new Long(d.getTime()));
    }

    private final ValueType type;
    private final Object value;

    /**
     * Constructor
     * @param type type of this value, may not be null.
     * @param val actual value
     */
    public EvalValue(ValueType type, Object val) {
        if (val == null) {
            throw new NullPointerException("value");
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
        this.value = val;
    }

    /**
     * @see IEvalValue#getValue()
     */
    public Object getValue() {
        return value;
    }

    /**
     * @see IEvalValue#getType()
     */
    public final ValueType getType() {
        return type;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer();
        rv.append(type).append(" ").append(value);
        return rv.toString();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof EvalValue)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (this == NULL) {
            return false;
        }

        IEvalValue e = (IEvalValue)o;

        return (this.type.equals(e.getType()) && this.value.equals(e.getValue()));
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        if (this == NULL) {
            return 0;
        }
        return value.hashCode() ^ type.hashCode();
    }
}
