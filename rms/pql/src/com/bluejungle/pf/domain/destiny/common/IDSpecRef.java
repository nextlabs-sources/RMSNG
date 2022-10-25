/*
 * Created on Feb 24, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.expressions.IPredicateReference;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/IDSpecRef.java#1 $:
 */

public interface IDSpecRef extends IPredicateReference {

    /**
     * Returns the refName.
     * @return the refName.
     */
    String getPrintableReference();

    /**
     * Returns true if the reference is by name, and false if it is by ID.
     * @return true if the reference is by name, and false if it is by ID.
     */
    boolean isReferenceByName();

    /**
     * Returns the name of the spec to which this reference points.
     * @return the name of the spec to which this reference points.
     */
    String getReferencedName();

    /**
     * Returns the ID of the spec to which this reference points (may be null).
     * @return the ID of the spec to which this reference points (may be null).
     */
    Long getReferencedID();

}
