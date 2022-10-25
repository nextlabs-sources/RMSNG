package com.bluejungle.pf.domain.epicenter.subject;

import com.bluejungle.framework.domain.IHasId;

// Copyright Blue Jungle, Inc.

/*
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/subject/ISubject.java#1 $
 */

public interface ISubject extends IHasId {

    /**
     * @return canonical name of this subject
     */
    String getName();

    /**
     * @return a type of this subject
     */
    ISubjectType getSubjectType();

}
