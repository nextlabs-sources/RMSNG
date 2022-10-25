/*
 * Created on May 2, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.domain.DOBase;
import com.bluejungle.framework.domain.IHasId;

/**
 * Currently Location represents a named attached to a range of ip addresses
 * specified as a.b.c.d/m
 *
 * In the future location will represent a logical or physical location of user,
 * host, resource, etc...
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/Location.java#1 $:
 */

public class Location extends DOBase implements IHasId {

    protected String name;
    protected String value;

    /**
     * Constructor
     * @param id
     */
    public Location(Long id, String name, String value) {
        super(id);
        this.name = name;
        this.value = value;
    }

    /**
     * Constructor
     * @param name
     * @param value
     */
    public Location(String name, String value) {
        super();
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the name.
     * @return the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the value.
     * @return the value.
     */
    public String getValue() {
        return this.value;
    }
}
