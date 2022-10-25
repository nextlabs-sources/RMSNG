package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IDisposable should be implemented by those components that need to release
 * resources before they are destroyed.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IDisposable.java#1 $
 */
public interface IDisposable
{

    /**
     * effects: releases resources used by the component
     */
    void dispose();
}
