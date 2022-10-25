package com.bluejungle.pf.domain.destiny.common;

/*
 * Created on Feb 15, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.destiny.action.DAction;
import com.bluejungle.pf.domain.epicenter.common.ISpec;
import com.bluejungle.pf.domain.epicenter.misc.IAccessControlled;

/**
 * IDSpec represents an abstract specification of some set of
 * destiny entities.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/IDSpec.java#1 $:
 */

public interface IDSpec extends ISpec, IAccessControlled {

    /**
     * Changes the <code>IPredicate</code> of this spec.
     * @param pred the new <code>IPredicate</code> object.
     */
    void setPredicate(IPredicate pred);

    void setDescription(String description);

    DevelopmentStatus getStatus();

    void setStatus(DevelopmentStatus status);

    boolean isHidden();

    boolean checkRoleAccess(IDSpec spec, DAction action);

}
