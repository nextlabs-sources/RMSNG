/*
 * Created on Dec 22, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.epicenter.misc;

import com.bluejungle.pf.domain.destiny.action.DAction;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;

/**
 * IAccessControlled should be implemented by any entity that needs to provide access
 * control to itself.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/misc/IAccessControlled.java#1 $:
 */

public interface IAccessControlled {

    IDSubject getOwner(); /* Returns owner of this object. */

    void setOwner(IDSubject owner); /* Sets owner of this object if the user has ADMIN privilege. */

    boolean checkAccess(IDSubject subject, DAction action);

    IAccessPolicy getAccessPolicy();

    void setAccessPolicy(AccessPolicy accessPolicy);

    void removeAccessPolicy();

}
