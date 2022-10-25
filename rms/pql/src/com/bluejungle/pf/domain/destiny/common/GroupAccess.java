package com.bluejungle.pf.domain.destiny.common;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/GroupAccess.java#1 $
 */

import com.bluejungle.pf.domain.epicenter.action.IAction;

import java.util.Collection;
import java.util.HashSet;

/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class GroupAccess implements IAccess {

    private Long groupId;
    private Collection<IAction> actions;

    public GroupAccess(Long groupId, Collection<? extends IAction> actions) {
        super();
        this.groupId = groupId;
        this.actions = new HashSet<IAction>();
        this.actions.addAll(actions);
    }

    public Collection<IAction> getActions() {
        return actions;
    }

    public Long getGroupId() {
        return groupId;
    }
}
