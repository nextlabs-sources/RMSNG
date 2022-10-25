/*
 * Created on Jan 5, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.epicenter.subject;

/**
 * Any subject attribute
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/subject/ISubjectAttribute.java#1 $:
 */

public interface ISubjectAttribute {

    public String getName();

    /**
     * @return true if this attribute is dynamic in nature, i.e., its value can change within
     * some period of time, such as 24 hours
     *
     * @deprecated This is only used by MapBuilder and we now determine if the attribute is dynamic
     * by looking it up in the enrolled information.
     */

    @Deprecated
    public boolean isDynamic();

}
