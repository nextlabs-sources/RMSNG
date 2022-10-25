package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * ComponentManagerFactory is a factory that returns a default component manager
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/ComponentManagerFactory.java#1 $
 */
public class ComponentManagerFactory {

    private static ComponentManagerImpl compMgr = new ComponentManagerImpl();

    public static synchronized IComponentManager getComponentManager() {
        if (compMgr.isShutdown()) {
            renew();
        }
        return compMgr;
    }

    private static void renew() {
        compMgr = new ComponentManagerImpl();
    }
}
