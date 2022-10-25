package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IManagerEnabled should be implemented by all components that need to use
 * other components, and therefore need a component manager.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IManagerEnabled.java#1 $
 */
public interface IManagerEnabled
{

    void setManager(IComponentManager manager);

    IComponentManager getManager();
}
