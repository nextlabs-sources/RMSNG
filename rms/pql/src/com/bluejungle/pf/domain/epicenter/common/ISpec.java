package com.bluejungle.pf.domain.epicenter.common;

/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.expressions.IPredicate;

/**
 * ISpec represents an abstract specification of some set of entities
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/common/ISpec.java#1 $
 */

public interface ISpec extends IHasId, IPredicate {

    /**
     * @return the name of this spec
     */
    String getName();

    /**
     * @return description of this spec
     */
    String getDescription();

    /**
     * @return the type of this spec
     */
    SpecType getSpecType();

    /**
     * Returns the predicate embedded in this spec.
     * @return
     */
    IPredicate getPredicate();

}
