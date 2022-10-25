package com.bluejungle.framework.utils;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/utils/IPair.java#1 $
 */

/**
 * This interface defines the contract for pairing objects.
 * @author sergey
 */
public interface IPair<X, Y> {

    /**
     * Returns first element in pair.  Note that, while pair is immutable,
     * this object is not.
     */
    X first();

    /**
     * Returns second element in pair.
     */
    Y second();

}
