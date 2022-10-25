/*
 * Created on Jan 25, 2006
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.domain.log;

/**
 * This is the "To resource" information class. This class holds the information
 * about the "to resource".
 *
 * @author ihanen
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/domain/src/java/main/com/bluejungle/domain/log/ToResourceInformation.java#1 $
 */

public class ToResourceInformation extends ResourceInformation {

    /**
     * Constructor
     *
     * @param name
     */
    public ToResourceInformation(String name) {
        super(name);
    }

    /**
     * Constructor
     *
     */
    public ToResourceInformation() {
        super();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer rv = new StringBuffer("ToResourceInformation[");
        rv.append("name: " + getName());
        rv.append("]");
        return rv.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ToResourceInformation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ToResourceInformation ri = (ToResourceInformation)obj;
        return ((getName().equals(ri.getName())));
    }
}
