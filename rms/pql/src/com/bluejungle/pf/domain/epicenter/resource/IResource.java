package com.bluejungle.pf.domain.epicenter.resource;

/*
 * All sources, binaries and HTML pages (C) Copyright 2007 by Blue Jungle Inc,
 * San Mateo, CA. Ownership remains with Blue Jungle Inc. All rights reserved
 * worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/resource/IResource.java#1 $
 */

import com.bluejungle.framework.expressions.IEvalValue;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * This interface defines the contract for accessing resource attributes.
 *
 * @author Sasha Vladimirov, sergey
 */

public interface IResource extends Serializable {

    /**
     * This method provides access to the unique ID of the resource.
     * @return the unique ID of the resource. The value this method returns
     * must not be null.
     */
    Serializable getIdentifier();

    /**
     * Given an attribute name, returns the attribute value.
     * @param name
     * @return the attribute value for the specified name.
     */
    IEvalValue getAttribute(String name);

    /**
     * Checks if a resource has the specified attribute.
     *
     * @param name the name of the attribute to check.
     * @return true if the attribute exists; false otherwise.
     */
    boolean hasAttribute(String name);

    /**
     * Get a set of all the key/value pairs in the map
     * @return a set of the key/value pairs
     */
    Set<Map.Entry<String, IEvalValue>> getEntrySet();

    /**
     * Creates a modifiable copy of this resource.
     * @return a modifiable copy of this resource.
     */
    IMResource clone();

}
