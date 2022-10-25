/*
 * Created on Jan 6, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */
package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IDSubjectManager.java#1 $:
 */

public interface IDSubjectManager {

    ComponentInfo<DefaultSubjectManager> COMP_INFO = new ComponentInfo<DefaultSubjectManager>(
            IDSubjectManager.class.getName(),
            DefaultSubjectManager.class,
            IDSubjectManager.class,
            LifestyleType.SINGLETON_TYPE);

    /**
     * Retrieves the given subjects and gives it
     * the <code>DynamicAttributes</code> passed in.
     * @param uid unique id of the subject
     * @param type type of the subject
     * @param attributes The dynamic attributes of this subject, or <code>null</code>
     * if the subject does not have dynamic attributes.
     * @return subject
     */
    IDSubject getSubject(String uid, ISubjectType type, DynamicAttributes attributes);

    /**
     * Retrieves the given subject with no dynamic attributes.
     * @param uid unique id of the subject
     * @param type type of the subject
     * @return subject
     */
    IDSubject getSubject(String uid, ISubjectType type);

    /**
     * Returns location with the given name
     * @param name location name
     * @return location
     * @throws IllegaArgumentException if location with a given name doesn't exist.
     */
    Location getLocation(String name);

}
