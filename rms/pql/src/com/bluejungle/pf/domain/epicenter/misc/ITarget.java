package com.bluejungle.pf.domain.epicenter.misc;

/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.expressions.IPredicate;

/*
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/misc/ITarget.java#1 $
 */

public interface ITarget {

    public IPredicate getActionPred();

    public void setActionPred(IPredicate action);

    public IPredicate getFromResourcePred();

    public void setFromResourcePred(IPredicate res);

    public IPredicate getToResourcePred();

    public void setToResourcePred(IPredicate res);

    public IPredicate getSubjectPred();

    public void setSubjectPred(IPredicate sub);

    public IPredicate getToSubjectPred();

    public void setToSubjectPred(IPredicate sub);
}
