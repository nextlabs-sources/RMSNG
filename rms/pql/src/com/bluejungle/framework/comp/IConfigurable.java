/*
 * Created on Dec 1, 2004
 * All sources, binaries and HTML pages
 * (C) copyright 2004 by Blue Jungle Inc., Redwood City CA,
 * Ownership remains with Blue Jungle Inc,
 * All rights reserved worldwide.
 */
package com.bluejungle.framework.comp;

/**
 * represents a component that is configurable
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IConfigurable.java#1 $
 */

public interface IConfigurable {

    /**
     * sets the configuration for the current component.  The component manager
     * calls this before any other initialization.
     *
     * @param config configuration to set
     */
    void setConfiguration(IConfiguration config);

    /**
     * returns components current configuration
     * @return components current configuration
     */
    IConfiguration getConfiguration();

}
