package com.bluejungle.pf.domain.destiny.subject;

/*
 * All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc, Redwood City, CA. Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/DefaultSubjectManager.java#1 $
 */

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

/**
 * This implementation of the IDSubjectManager provides default implementations for all methods of the interface.
 */
public class DefaultSubjectManager implements IDSubjectManager {

    /**
     * @see IDSubjectManager#getSubject(String, ISubjectType, DynamicAttributes)
     */
    public IDSubject getSubject(String uid, ISubjectType type, DynamicAttributes attributes) {
        return new Subject(uid, uid, uid, IHasId.UNKNOWN_ID, type);
    }

    /**
     * @see IDSubjectManager#getSubject(String, ISubjectType, DynamicAttributes)
     */
    public IDSubject getSubject(String uid, ISubjectType type) {
        return getSubject(uid, type, null);
    }

    /**
     * @see IDSubjectManager#getLocation(String)
     */
    public Location getLocation(String name) {
        return null;
    }

}
