/*
 * Created on Dec 30, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.action;

import com.bluejungle.framework.expressions.PredicateConstants;
import com.bluejungle.pf.domain.destiny.common.BuiltInSpec;
import com.bluejungle.pf.domain.destiny.common.IDSpec;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.io.Serializable;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/action/IDActionSpec.java#1 $:
 */

public interface IDActionSpec extends IDSpec, Serializable {

    /**
     * Empty action spec.
     */
    IDSpec EMPTY = new BuiltInSpec(null, SpecType.ACTION, null, "EMPTY", "Built-in <EMPTY> action", PredicateConstants.FALSE, true) {
    };

    /**
     * All actions.
     */
    IDSpec ALL_ACTIONS = new BuiltInSpec(null, SpecType.ACTION, null, "*", "Built-in <*> action", PredicateConstants.TRUE, true) {
    };

}
