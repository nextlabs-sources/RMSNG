package com.bluejungle.pf.domain.destiny.common;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/IAccess.java#1 $
 */

import com.bluejungle.pf.domain.epicenter.action.IAction;

import java.util.Collection;

/**
 * @author sergey
 */
public interface IAccess {

    Collection<IAction> getActions();
}
