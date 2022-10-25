/*
 * All sources, binaries and HTML pages (C) Copyright 2006 by Blue Jungle Inc,
 * Redwood City, CA. Ownership remains with Blue Jungle Inc.
 * All rights reserved worldwide.
 *
 * @author sergey
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/IEngineSubjectResolver.java#1 $
 */

package com.bluejungle.pf.engine.destiny;

import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

/**
 * This interface defines a contract for resolving subjects.
 */
public interface IEngineSubjectResolver {

    /**
     * Checks whether a given subject exists in the assembly
     * @param uid unique id of the subject
     * @param type subject type
     * @return true if this subject exists in the assembly, false otherwise
     */
    boolean existsSubject(String uid, ISubjectType type);

    /**
     * Gets the <code>IMultiValue</code> representing the group collection
     * of the subject identified by the uid and the type.
     * Returns <code>IEvalValue.EMPTY</code> if the subject does not exist.
     *
     * @param uid unique canonical id of the subject
     * @return subject representing the given uid
     */
    IEvalValue getGroupsForSubject(String uid, ISubjectType type);

}
