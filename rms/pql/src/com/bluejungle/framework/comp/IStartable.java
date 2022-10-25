package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IStartable should be implemented by those components that need to be started before
 * they're exposed.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IStartable.java#1 $
 */
public interface IStartable
{

    /**
     * effects: starts the component.  The method must return immediately and whatever is
     * started needs to run asynchronously
     */
    void start();

    /**
     * effects: stops the component.
     */
    void stop();
}
