package com.bluejungle.pf.domain.destiny.subject;

/*
 * All sources, binaries and HTML pages (C) Copyright 2006 by Blue Jungle Inc,
 * Redwood City, CA. Ownership remains with Blue Jungle Inc.
 * All rights reserved worldwide.
 *
 * @author sergey
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IDSubjectAttribute.java#1 $
 */

import com.bluejungle.pf.domain.epicenter.subject.ISubjectAttribute;

public interface IDSubjectAttribute extends ISubjectAttribute {

    /**
     * Returns the subject type of the subject for which this is an attribute.
     * @return the subject type of the subject for which this is an attribute.
     */
    SubjectType getSubjectType();

}
