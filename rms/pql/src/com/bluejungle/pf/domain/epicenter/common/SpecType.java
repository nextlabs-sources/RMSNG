/*
 * Created on Feb 15, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.epicenter.common;

import com.bluejungle.framework.patterns.EnumBase;

/**
 * Type of a spec.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/common/SpecType.java#2 $:
 */

public class SpecType extends EnumBase {

    private static final long serialVersionUID = 6552951205288145489L;

    public static final SpecType ILLEGAL = new SpecType("<ILLEGAL>") {

        private static final long serialVersionUID = -9148268253840748858L;
    };

    public static final SpecType ALL = new SpecType("<ALL>") {

        private static final long serialVersionUID = 3079373984451853965L;
    };

    public static final SpecType RESOURCE = new SpecType("resource") {

        private static final long serialVersionUID = -5766912751133569407L;
    };

    public static final SpecType ACTION = new SpecType("action") {

        private static final long serialVersionUID = -6142608867130845704L;
    };

    public static final SpecType USER = new SpecType("user") {

        private static final long serialVersionUID = 7767816754800194789L;
    };

    public static final SpecType APPUSER = new SpecType("appuser") {

        private static final long serialVersionUID = -4910440519632241141L;
    };

    public static final SpecType HOST = new SpecType("host") {

        private static final long serialVersionUID = 9186275462226286920L;
    };

    public static final SpecType APPLICATION = new SpecType("application") {

        private static final long serialVersionUID = -14863954114316517L;
    };

    public static final SpecType PORTAL = new SpecType("portal") {

        private static final long serialVersionUID = -14863954114316516L;
    };

    public static final SpecType RECIPIENT = new SpecType("recipient") {

        private static final long serialVersionUID = 3425755968445289212L;
    };

    protected SpecType(String name) {
        super(name, SpecType.class);
    }

    /**
     * Returns an instance of <code>SpecType</code> for the given name.
     * @param name the name of the <code>SpecType</code>.
     * @return an instance of <code>SpecType</code> for the given name.
     */
    public static SpecType forName(String name) {
        return getElement(name, SpecType.class);
    }

    /**
     * Returns an instance of <code>SpecType</code> for the given type.
     * @param enumType the type of the <code>SpecType</code>.
     * @return an instance of <code>SpecType</code> for the given type.
     */
    public static SpecType forType(int enumType) {
        return getElement(enumType, SpecType.class);
    }

    /**
     * Returns the number of distinct <code>SpecType</code>s.
     * @return the number of distinct <code>SpecType</code>s.
     */
    public static int getElementCount() {
        return numElements(SpecType.class);
    }

}
