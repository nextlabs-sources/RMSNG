/*
 * Created on Feb 4, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

package com.bluejungle.pf.domain.destiny.misc;

import com.bluejungle.framework.patterns.EnumBase;

import java.util.Set;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/misc/EffectType.java#1 $:
 */

public abstract class EffectType extends EnumBase implements IDEffectType {

    public static final IDEffectType ALLOW = new EffectType(ALLOW_NAME, ALLOW_TYPE) {

        private static final long serialVersionUID = 1L;
    };

    public static final IDEffectType DENY = new EffectType(DENY_NAME, DENY_TYPE) {

        private static final long serialVersionUID = 1L;
    };

    public static final IDEffectType DONT_CARE = new EffectType(DONT_CARE_NAME, DONT_CARE_TYPE) {

        private static final long serialVersionUID = 1L;
    };

    private EffectType(String name, int type) {
        super(name, type, EffectType.class);
    }

    public static Set<EffectType> elements() {
        return EnumBase.elements(EffectType.class);
    }

    public static EffectType getElement(String name) {
        return getElement(name, EffectType.class);
    }

    public static int numElements() {
        return numElements(EffectType.class);
    }

}
