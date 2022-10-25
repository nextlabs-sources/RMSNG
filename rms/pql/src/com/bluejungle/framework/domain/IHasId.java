package com.bluejungle.framework.domain;

/*
 * Created on Dec 8, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

/**
 * IHasId is an interface that should be implemented by all objects that have a
 * unique id. For example, an IPolicy domain object has a unique id, and
 * therefore should implement this interface.
 *
 * @author sasha
 * @version $Id:
 *          //depot/main/Destiny/main/src/common/framework/src/java/main/com/bluejungle/framework/domain/IHasId.java#2 $:
 */

public interface IHasId {

    /**
     * @return id of this object
     */
    Long getId();

    /**
     * Unknow constants
     */
    Long UNKNOWN_ID = new Long(-1);
    String UNKNOWN_NAME = "Unknown";
    /**
     * System user constants
     */
    Long SYSTEM_USER_ID = new Long(-2);
    String SYSTEM_USER_NAME = "LocalSystem@localhost";
}
