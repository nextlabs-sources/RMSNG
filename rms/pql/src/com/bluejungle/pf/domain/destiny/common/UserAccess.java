package com.bluejungle.pf.domain.destiny.common;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/UserAccess.java#1 $
 */

import com.bluejungle.pf.domain.epicenter.action.IAction;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author pkeni
 */

/**
 * @author pkeni
 */
public class UserAccess implements IAccess {

    private Long userId;
    private Collection<IAction> actions;

    public UserAccess(Long userId, Collection<? extends IAction> actions) {
        super();
        this.userId = userId;
        this.actions = new HashSet<IAction>();
        this.actions.addAll(actions);
    }

    public Collection<IAction> getActions() {
        return actions;
    }

    public Long getUserId() {
        return userId;
    }
}
