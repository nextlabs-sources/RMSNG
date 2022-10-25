package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/**
 * IComponentManager is used to manage components inside the component container. All methods are safe to use in a multi-threaded environment.
 *
 * TODO: exceptions
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/IComponentManager.java#1 $
 */
public interface IComponentManager {

    /**
     * checks whether component with the given name has been registered, i.e. is known to
     * the component manager
     *
     * @param name name of the component in question
     * @return true if this component has been registered, false otherwise
     */
    boolean isComponentRegistered(String name);

    /**
     * Registers component with the manager.  If component with the same name is already registered then that component's info can
     * be left as is, or overriden, depending on the value of the override flag.
     *
     * @param info info of the component to register
     * @param override if this is true, then current info for the component with the same name will be overriden with the new info.
     */
    void registerComponent(ComponentInfo<?> info, boolean override);

    /**
     * Unregisters the component for the specified componentInfo if present.
     * @param info
     */
    void unregisterComponent(ComponentInfo<?> info);

    /**
     * returns a component with the given name in a completely usable state. This method will never return null, if it cannot find the requested component it'll throw an exception.
     *
     * @requires named component has been registered with the manager
     * @param name name of the component
     * @return requested component
     * @deprecated recommand to use <code>T getComponent(Class<? extends IHasComponentInfo<T>> clazz)</code>.
                   Although this method is marked as deprecated, this method won't be removed. It only gives a warning.
     */
    @Deprecated
    Object getComponent(String name);

    /**
     * Performs registration and retrieval of the component in a single shot.  The requested component in returned in a completely usable state.
     * This will not override the current registration of the component.  To do that, use registerComponent followed by getComponent(name).
     * This method will never return null, if it cannot find the requested component it'll throw an exception.
     *
     * @param clazz class that implements IHasComponentInfo
     * @return requested component
     */
    <T> T getComponent(Class<? extends IHasComponentInfo<T>> clazz);

    /**
     * Performs registration and retrieval of the component in a single shot.  The requested component in returned in a completely usable state.
     * This will not override the current registration of the component.  To do that, use registerComponent followed by getComponent(name).
     * This will override default configuration of the component.
     * This method will never return null, if it cannot find the requested component it'll throw an exception.
     *
     * @param clazz class that implements IHasComponentInfo
     * @param config overriding configuration
     * @return requested component
     */
    <T> T getComponent(Class<? extends IHasComponentInfo<T>> clazz, IConfiguration config);

    /**
     * Performs registration and retrieval of the component in a single shot.  The requested component in returned in a completely usable state.
     * This will not override the current registration of the component.  To do that, use registerComponent followed by getComponent(name).
     * This method will never return null, if it cannot find the requested component it'll throw an exception.
     *
     * @requires info is not null
     * @param info component info for the component to return
     * @return a component
     */
    <T> T getComponent(ComponentInfo<T> info);

    /**
     * Performs registration and retrieval of the component in a single shot.  The requested component in returned in a completely usable state.
     * This will not override the current registration of the component.  To do that, use registerComponent followed by getComponent(name).
     * This will override default configuration of the component the first time it's retrieved.
     * To override default configuration for subsequent retrievals, first override the current
     * info with registerComponent().
     * This method will never return null, if it cannot find the requested component it'll throw an exception.
     *
     * @param info component info
     * @param config overriding configuration
     * @return a component
     */
    <T> T getComponent(ComponentInfo<T> info, IConfiguration config);

    /**
     * effects: getComponentName returns the name of the given component
     *
     * @param comp Component whose name must is requested
     *
     * @return name of the given component
     */
    String getComponentName(Object comp);

    /**
     * effects: notifies the container that a given component is no longer needed by the client. The component may be destroyed, or returned to the pool, or nothing may be done at all depending on the components lifestyle.
     *
     * @param comp component that is to be releases
     */
    void releaseComponent(Object comp);

    /**
     * effects: stops running components, disposes of disposable components, any
     * subsequent attempts to use this component manager will result in a runtime
     * exception.
     */
    void shutdown();

    /**
     *
     * @return true if the component manager is shut down, false otherwise. Any
     * attempts to use a component manager that is shut down will result in a runtime
     * exception.
     */
    boolean isShutdown();
}
