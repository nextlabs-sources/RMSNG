package com.bluejungle.pf.destiny.lifecycle;

/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/lifecycle/DevelopmentStatus.java#1 $
 */

import com.bluejungle.framework.patterns.EnumBase;

/**
 * This type-safe enumeration represents the type of
 * an entity deployment (production or testing).
 */
public abstract class DevelopmentStatus extends EnumBase {

    public static final DevelopmentStatus NEW = new DevelopmentStatus("NEW") {
    };

    public static final DevelopmentStatus EMPTY = new DevelopmentStatus("EMPTY") {
    };

    public static final DevelopmentStatus DRAFT = new DevelopmentStatus("DRAFT") {
    };

    public static final DevelopmentStatus APPROVED = new DevelopmentStatus("APPROVED") {
    };

    public static final DevelopmentStatus OBSOLETE = new DevelopmentStatus("OBSOLETE") {
    };

    public static final DevelopmentStatus DELETED = new DevelopmentStatus("DELETED") {
    };

    /**
     * This package-private constant is for making queries that ignore the status
     * without re-generating the query dynamically.
     */
    static final DevelopmentStatus ILLEGAL = new DevelopmentStatus("ILLEGAL") {
    };

    /**
     * The constructor is private to prevent unwanted instanciations from the outside.
     * @param name is passed through to the constructor of the superclass.
     */
    private DevelopmentStatus(String name) {
        super(name, DevelopmentStatus.class);
    }

    public static DevelopmentStatus forName(String name) {
        return getElement(name, DevelopmentStatus.class);
    }
}
