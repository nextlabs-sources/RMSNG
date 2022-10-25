/*
 * Created on Feb 4, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.misc;

import com.bluejungle.pf.domain.epicenter.misc.IEffectType;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/misc/IDEffectType.java#1 $:
 */

public interface IDEffectType extends IEffectType {

    String ALLOW_NAME = "allow";
    int ALLOW_TYPE = 1;

    String DENY_NAME = "deny";
    int DENY_TYPE = 2;

    String DONT_CARE_NAME = "dontcare";
    int DONT_CARE_TYPE = 3;

}
