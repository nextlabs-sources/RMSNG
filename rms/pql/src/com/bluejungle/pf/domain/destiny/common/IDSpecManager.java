/*
 * Created on Feb 23, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;
import com.bluejungle.framework.expressions.IPredicate;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/IDSpecManager.java#1 $:
 */

public interface IDSpecManager {

    String COMP_NAME = IDSpecManager.class.getName();

    ComponentInfo<IDSpecManager> COMP_INFO =
            new ComponentInfo<IDSpecManager>(
                    COMP_NAME,
                    DefaultSpecManager.class,
                    IDSpecManager.class,
                    LifestyleType.SINGLETON_TYPE);

    /**
     * Returns the spec with the given name and type.
     * @param specName the name of the spec.
     * @return spec with the given name and type.
     */
    IDSpec resolveSpec(String specName);

    /**
     * Returns the spec with the given ID.
     * (there is no type because the IDs are unique across all types).
     * @param id the ID of the spec.
     * @return spec with the given name and type.
     */
    IDSpec resolveSpec(Long id);

    /**
     * Returns a reference to a spec with the specified name and type.
     * @param specName the name of the referenced spec.
     * @return a reference to a spec with the specified name and type.
     */
    IPredicate getSpecReference(String specName);

    /**
     * Returns a reference to a spec with the specified ID
     * (there is no type because the IDs are unique across all types).
     * @param id the ID of the referenced spec.
     * @return a reference to a spec with the specified ID.
     */
    IPredicate getSpecReference(Long id);

    /**
     * Saves the specified spec by name and/or by id.
     *
     * @param spec
     */
    void saveSpec(IDSpec spec);
}
