package com.bluejungle.framework.expressions;

import com.bluejungle.framework.patterns.EnumBase;

import java.util.Set;

//All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
//Redwood City CA,
//Ownership remains with Blue Jungle Inc, All rights reserved worldwide.

/**
 * Operators used in the Epicenter/Destiny policy framework.
 *
 * @author pkeni
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/RelationOp.java#1 $
 */

public class RelationOp extends EnumBase {

    private static final long serialVersionUID = 1L;

    public static final int verboseNames = 1;

    public static final RelationOp EQUALS = new RelationOp("equals", "=") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp NOT_EQUALS = new RelationOp("not_equals", "!=") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp GREATER_THAN = new RelationOp("greater_than", ">") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp LESS_THAN = new RelationOp("less_than", "<") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp GREATER_THAN_EQUALS = new RelationOp("greater_than_equals", ">=") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp LESS_THAN_EQUALS = new RelationOp("less_than_equals", "<=") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp HAS = new RelationOp("has", "has") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp DOES_NOT_HAVE = new RelationOp("does_not_have", "does_not_have") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp INCLUDES = new RelationOp("includes", "includes") {

        private static final long serialVersionUID = 1L;
    };
    public static final RelationOp EQUALS_UNORDERED = new RelationOp("equals_unordered", "equals_unordered") {

        private static final long serialVersionUID = 1L;
    };
    private final String altName; // non-verbose name

    private RelationOp(String altName, String name) {
        super(name, RelationOp.class);
        this.altName = altName;
    }

    public String toString() {
        return (RelationOp.verboseNames == 0) ? this.altName : this.enumName;
    }

    public static RelationOp getElement(String name) {
        return getElement(name, RelationOp.class);
    }

    public static Set<RelationOp> elements() {
        return elements(RelationOp.class);
    }

    public static int numElements() {
        return numElements(RelationOp.class);
    }
}
