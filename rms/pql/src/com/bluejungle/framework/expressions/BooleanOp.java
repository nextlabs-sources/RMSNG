/*
 * Created on Dec 30, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */
package com.bluejungle.framework.expressions;

import com.bluejungle.framework.patterns.EnumBase;

import java.util.Collection;

/**
 * BooleanOp is a class of all boolean operators that are essentially enumerations with behavior.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/BooleanOp.java#1 $:
 */

public abstract class BooleanOp extends EnumBase {

    public static final String AND_NAME = "AND";
    public static final int AND_TYPE = 1;

    public static final String OR_NAME = "OR";
    public static final int OR_TYPE = 2;

    public static final String NOT_NAME = "NOT";
    public static final int NOT_TYPE = 3;

    /**
     * OR
     */
    public static final BooleanOp OR = new BooleanOp(OR_NAME, OR_TYPE) {

        public boolean match(Collection<IPredicate> predicates, IArguments request) {
            if (predicates == null || predicates.isEmpty()) {
                return false;
            }
            for (IPredicate predicate : predicates) {
                if (predicate.match(request)) {
                    return true;
                }
            }
            return false;
        }
    };

    /**
     * AND
     */
    public static final BooleanOp AND = new BooleanOp(AND_NAME, AND_TYPE) {

        public boolean match(Collection<IPredicate> predicates, IArguments value) {
            if (predicates == null || predicates.isEmpty()) {
                return false;
            }
            for (IPredicate predicate : predicates) {
                if (!predicate.match(value)) {
                    return false;
                }
            }
            return true;
        }
    };

    /**
     * NOT
     */
    public static final BooleanOp NOT = new BooleanOp(NOT_NAME, NOT_TYPE) {

        public boolean match(Collection<IPredicate> predicates, IArguments request) {
            return !(OR.match(predicates, request));
        }
    };

    /**
     * @param enumName BooleanOp name
     * @return named BooleanOp
     * @throws IllegalArgumentException if named BooleanOp doesn't exist
     */
    public static BooleanOp getElement(String enumName) {
        return getElement(enumName, BooleanOp.class);
    }

    /**
     * @param enumName BooleanOp name
     * @return true if a BooleanOp with a given name exists, false otherwise
     */
    public static boolean existsElement(String enumName) {
        return existsElement(enumName, BooleanOp.class);
    }

    /**
     * @param enumType type of the BooleanOp
     * @return BooleanOp of the given type
     * @throws IllegalArgumentException if a BooleanOp of the given type does not exist
     */
    public static BooleanOp getElement(int enumType) {
        return getElement(enumType, BooleanOp.class);
    }

    /**
     * @param enumType type of the BooleanOp
     * @return true if a BooleanOp with a given type exists, false otherwise
     */
    public static boolean existsElement(int enumType) {
        return existsElement(enumType, BooleanOp.class);
    }

    /**
     * evaluates this boolean operation on a collection of matchables. evaluation is short-circuited whenever possible
     *
     * @param predicates a <code>Collection</code> of <code>IPredicate</code>.
     * @param values The arguments to pass to the predicates.
     * @return result of a boolean operation across all predicates.
     */
    public abstract boolean match(Collection<IPredicate> predicates, IArguments values);

    private BooleanOp(String name, int type) {
        super(name, type, BooleanOp.class);
    }

}
