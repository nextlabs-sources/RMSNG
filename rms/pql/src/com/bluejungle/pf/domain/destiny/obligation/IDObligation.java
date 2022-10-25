/*
 * Created on Dec 28, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.obligation;

import com.bluejungle.domain.log.PolicyActivityInfoV5;
import com.bluejungle.pf.domain.epicenter.misc.IObligation;
import com.bluejungle.pf.engine.destiny.EvaluationResult;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/obligation/IDObligation.java#1 $:
 */

public interface IDObligation extends IObligation {

    boolean isActivityAcceptable(EvaluationResult res, PolicyActivityInfoV5 args);

    String toPQL();
}
