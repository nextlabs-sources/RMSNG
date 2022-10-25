/*
 * Created on Feb 11, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

import com.bluejungle.framework.patterns.EnumBase;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/PredicateConstants.java#1 $:
 */

public abstract class PredicateConstants extends EnumBase implements IPredicate {

    public static final PredicateConstants TRUE = new PredicateConstants("TRUE") {

        public boolean match(IArguments request) {
            return true;
        }
    };

    public static final PredicateConstants FALSE = new PredicateConstants("FALSE") {

        public boolean match(IArguments request) {
            return false;
        }
    };

    public void accept(IPredicateVisitor visitor, IPredicateVisitor.Order order) {
        visitor.visit(this);
    }

    public static boolean existsElement(String enumName) {
        return existsElement(enumName, PredicateConstants.class);
    }

    public static boolean existsElement(int enumType) {
        return existsElement(enumType, PredicateConstants.class);
    }

    public static PredicateConstants getElement(String enumName) {
        return getElement(enumName, PredicateConstants.class);
    }

    public static PredicateConstants getElement(int enumType) {
        return getElement(enumType, PredicateConstants.class);
    }

    private PredicateConstants(String name) {
        super(name, PredicateConstants.class);
    }
}
