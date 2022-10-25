package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IInitializable should be implemented by those components that wish to be
 * initialized.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IInitializable.java#1 $
 */
public interface IInitializable
{

    /**
     * effects: initializes the component
     */
    void init();

}
