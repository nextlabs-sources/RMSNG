/*
 * Created on Dec 30, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.patterns;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/patterns/IEnum.java#1 $:
 */

public interface IEnum {

    /**
     * @return name of this enum object
     */
    String getName();

    /**
     * @return type of this enum object
     */
    int getType();
}
