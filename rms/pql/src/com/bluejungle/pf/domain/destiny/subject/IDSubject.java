/*
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IDSubject.java#1 $:
 */
package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.pf.domain.epicenter.subject.ISubject;

import java.util.Map;
import java.util.Set;

/**
 * This interface defines the contract for destiny subjects.
 */

public interface IDSubject extends ISubject, IArguments {

    /**
     * @return a unique user id
     */
    String getUid();

    /**
     * @return a unique user name (principalName)
     */
    String getUniqueName();

    /**
     * Returns an <code>IMultiValue</code> with group numbers
     * to which this subject belongs.
     * @return an <code>IMultiValue</code> with group numbers
     * to which this subject belongs.
     */
    public IEvalValue getGroups();

    /**
     * Accesses a dynamic attribute of this subject.
     * @param name the name of the dynamic attribute to access.
     * @return the value of the dynamic attribute.
     */
    public IEvalValue getAttribute(String name);

    /**
     * Get a set of all the key/value pairs in the map
     * @return a set of the key/value pairs
     */
    public Set<Map.Entry<String, IEvalValue>> getEntrySet();

    public boolean isCacheable();

}
