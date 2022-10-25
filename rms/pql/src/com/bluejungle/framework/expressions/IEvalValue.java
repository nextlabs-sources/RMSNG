package com.bluejungle.framework.expressions;

import java.io.Serializable;

/*
 * All sources, binaries and HTML pages (C) Copyright 2007
 * by Blue Jungle Inc., San Mateo CA, Ownership remains with
 * Blue Jungle Inc, All rights reserved worldwide.
 */

public interface IEvalValue extends Serializable {

    public static final IEvalValue NULL = new IEvalValue() {

        private static final long serialVersionUID = 1L;

        public ValueType getType() {
            return ValueType.NULL;
        }

        public Object getValue() {
            return null;
        }
    };

    public static final IEvalValue EMPTY = new EvalValue(ValueType.MULTIVAL, IMultivalue.EMPTY);

    /**
     * Returns the value.
     * @return the val.
     */
    public Object getValue();

    /**
     * Returns the value type.
     * @return the value type.
     */
    public ValueType getType();

}
