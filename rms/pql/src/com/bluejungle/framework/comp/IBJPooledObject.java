package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 *
 * any class that wants to be poolable by BJPool must implement
 * IBJPooledObject
 *
 * @author hfriedland
 * @version "$Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IBJPooledObject.java#1 $"
 */
public interface IBJPooledObject
{

    // this method is called when the BJPool is populated
    // with objects
    public void init();

    // this method is called when the object is released back
    // into the pool
    public void reset();
}
