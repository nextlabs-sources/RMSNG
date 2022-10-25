package com.bluejungle.pf.domain.epicenter.action;

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.patterns.IEnum;

// Copyright Blue Jungle, Inc.

/*
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/action/IAction.java#1 $
 */

public interface IAction extends IEnum, IHasId, IPredicate {

    /**
     * @return name of this action
     */
    String getName();

    /**
     * @return type of this action
     */
    int getType();

}
