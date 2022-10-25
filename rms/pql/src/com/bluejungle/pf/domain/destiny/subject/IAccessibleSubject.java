package com.bluejungle.pf.domain.destiny.subject;

/*
 * All sources, binaries and HTML pages (C) Copyright 2006 by Blue Jungle Inc,
 * Redwood City, CA. Ownership remains with Blue Jungle Inc.
 * All rights reserved worldwide.
 *
 * @author sergey
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IAccessibleSubject.java#1 $
 */

import com.bluejungle.framework.expressions.IEvalValue;

/**
 * This interface defines the contract for subjects with access groups.
 */

public interface IAccessibleSubject {

    /**
     * @return all the accessgroups this subject is a member of,
     *  captured as an <code>EvalValue</code> of type MULTIVAL.
     */
    IEvalValue getAccessGroups();

}
