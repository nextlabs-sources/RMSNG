/*
 * Created on Feb 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

import com.bluejungle.framework.patterns.EnumBase;
import com.bluejungle.framework.utils.StringUtils;

import java.util.Date;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/ValueType.java#1 $:
 */

public class ValueType extends EnumBase {

    private static final long serialVersionUID = 1L;

    public static final ValueType LONG = new ValueType("long") {

        private static final long serialVersionUID = 1L;

        public Object getInternalRepresentation(Object obj) {
            if (obj instanceof Long) {
                return obj;
            } else if (obj instanceof Date) {
                return ((Date)obj).getTime();
            } else if (obj instanceof String) {
                return Long.parseLong((String)obj);
            } else {
                throw new IllegalArgumentException("converting to Date an object of unknown type");
            }
        }
    };

    public static final ValueType STRING = new ValueType("string") {

        private static final long serialVersionUID = 1L;

        @Override
        public String formatRepresentation(String representation) {
            return '"' + StringUtils.escape(representation) + '"';
        }
    };

    public static final ValueType NULL = new ValueType("null") {

        private static final long serialVersionUID = 1L;
    };

    public static final ValueType MULTIVAL = new ValueType("multival") {

        private static final long serialVersionUID = 1L;

        @Override
        public String formatRepresentation(String representation) {
            return '"' + StringUtils.escape(representation) + '"';
        }
    };

    public static final ValueType DATE = new ValueType("date") {

        private static final long serialVersionUID = 1L;

        @Override
        public Object getInternalRepresentation(Object obj) {
            if (obj instanceof Long) {
                return obj;
            } else if (obj instanceof Date) {
                return ((Date)obj).getTime();
            } else {
                throw new IllegalArgumentException("converting to Date an object of unknown type");
            }
        }
    };

    private ValueType(String name) {
        super(name, ValueType.class);
    }

    /**
     * Given a user-readable <code>String</code> representation,
     * provides a representation suitable for PQL.
     * This default version simply returns the original representation.
     * Subclasses may override this to provide different representations.
     * @param representation user-readable representation.
     * @return representation for PQL.
     */
    public String formatRepresentation(String representation) {
        return representation;
    }

    /**
     * Returns the number of elements in this enumeration.
     * @return the number of elements in this enumeration.
     */
    public static int numElements() {
        return numElements(ValueType.class);
    }

    /**
     * Returns an element for its name.
     * @param name the name of the element to return.
     * @return an element specified by its unique name.
     */
    public static ValueType forName(String name) {
        return getElement(name, ValueType.class);
    }

    public static ValueType forObject(Object obj) {
        if (obj == null) {
            return NULL;
        } else if (obj instanceof String) {
            return STRING;
        } else if (obj instanceof Date) {
            return DATE;
        } else if (obj instanceof Long) {
            return LONG;
        } else if (obj instanceof IMultivalue) {
            return MULTIVAL;
        } else {
            throw new IllegalArgumentException("Unsupported type");
        }
    }

    public Object getInternalRepresentation(Object obj) {
        return obj;
    }

}
