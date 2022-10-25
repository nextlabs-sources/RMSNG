package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IInstanceProvider is an interface that represents a provider of
 * component instances.  This interface should be implemented by any
 * such provider.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IInstanceProvider.java#1 $
 */
public interface IInstanceProvider
{

    /**
     * @requires: info, log is not null
     * @effects: returns a component by the given name, initialized as necessary
     *
     * @param info    definition of the component to retrieve
     * @param instance if the component has been instantiated during the info discovery process
     * then this is the instance used
     * @return instance of a component
     */
    <T> T getComponent(ComponentInfo<T> info, Object instance);

    /**
     * requires: comp is not null
     * effects: releases the component from management
     *
     * @param comp the component to release
     */
    void release(Object comp);

    /**
     * effects: stops running components, disposes of disposable components
     *
     */
    void shutdown();
}
