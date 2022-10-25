/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.domain;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/domain/DOBase.java#1 $:
 */

public abstract class DOBase implements IDomainObject {

    protected Long id;

    /**
     * Constructor
     *
     */
    public DOBase() {
        super();
    }

    public DOBase(Long id) {
        super();
        this.id = id;
    }

    /**
     * @see com.bluejungle.framework.domain.IHasId#getId()
     */
    public Long getId() {
        return id;
    }

    /**
     * @see com.bluejungle.framework.domain.IHasId#setId(java.lang.Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

}
