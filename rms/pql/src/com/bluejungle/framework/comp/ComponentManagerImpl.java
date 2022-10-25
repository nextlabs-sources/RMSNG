package com.bluejungle.framework.comp;

import java.util.HashMap;

// Copyright Blue Jungle, Inc.

/**
 * Default implementation for a component manager.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/ComponentManagerImpl.java#1 $
 */
public class ComponentManagerImpl implements IComponentManager {

    private final HashMap<Object, ComponentInfo> instanceDefMap;
    private final HashMap<String, ComponentInfo> nameDefMap;
    private final HashMap classDefMap;

    IInstanceProvider singletonProvider;
    IInstanceProvider transientProvider;
    IInstanceProvider pooledProvider;
    IInstanceProvider threadedProvider;

    private boolean shutdown = false;

    ComponentManagerImpl() {

        singletonProvider = new SingletonInstanceProvider(this);
        transientProvider = new TransientInstanceProvider(this);
        pooledProvider = new PooledInstanceProvider(this);
        threadedProvider = new ThreadedInstanceProvider(this);

        instanceDefMap = new HashMap<Object, ComponentInfo>();
        nameDefMap = new HashMap<String, ComponentInfo>();
        classDefMap = new HashMap();
    }

    public <T> T getComponent(Class<? extends IHasComponentInfo<T>> clazz) {
        return getComponent(clazz, null);
    }

    public synchronized <T> T getComponent(Class<? extends IHasComponentInfo<T>> clazz, IConfiguration config) {
        checkShutdown();
        ComponentInfo<T> info = (ComponentInfo<T>)classDefMap.get(clazz);
        IHasComponentInfo<T> hasInfo = null;
        if (info == null) {
            try {
                hasInfo = clazz.newInstance();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            info = hasInfo.getComponentInfo();
            classDefMap.put(clazz, info);
        }

        return getComponent(info, config, hasInfo);
    }

    /**
     * @deprecated replaced by <code>T getComponent(Class<? extends IHasComponentInfo<T>> clazz)</code>
     */
    @Deprecated
    public synchronized Object getComponent(String name) {
        checkShutdown();
        ComponentInfo<?> info = nameDefMap.get(name);
        if (info == null) {
            throw new NullPointerException("Unknown component: " + name);
        }
        return getComponent(info);
    }

    public <T> T getComponent(ComponentInfo<T> info) {
        return getComponent(info, null, null);
    }

    public <T> T getComponent(ComponentInfo<T> info, IConfiguration config) {
        return getComponent(info, config, null);
    }

    public synchronized <T> T getComponent(ComponentInfo<T> info, IConfiguration config, Object instance) {
        checkShutdown();
        if (info == null) {
            // TODO: exception
            throw new NullPointerException("Component info is null.");
        }
        // the first reference to a component by name always wins
        if (nameDefMap.containsKey(info.getName())) {
            info = nameDefMap.get(info.getName());
        } else {
            nameDefMap.put(info.getName(), info);
            if (config != null) {
                info.overrideConfiguration(config);
            }
        }

        IInstanceProvider instanceProvider = info.getLifestyleType().getProvider(this);
        T rv = instanceProvider.getComponent(info, instance);
        if (rv == null) {
            // TODO: reasonable error number
            throw new RuntimeException("Failed to create component.");
        }

        return rv;
    }

    synchronized void registerInstance(Object instance, ComponentInfo info) {
        instanceDefMap.put(instance, info);
    }

    public String getComponentName(Object comp) {
        checkShutdown();
        return getComponentDefinition(comp).getName();
    }

    public synchronized void releaseComponent(Object comp) {
        checkShutdown();
        ComponentInfo info = getComponentDefinition(comp);
        instanceDefMap.remove(comp);

        IInstanceProvider provider = info.getLifestyleType().getProvider(this);
        provider.release(comp);

    }

    private synchronized ComponentInfo getComponentDefinition(Object comp) {
        ComponentInfo info = instanceDefMap.get(comp);
        if (info == null) {
            // TODO: reasonable error number
            throw new RuntimeException();
        }
        return info;
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        //the order is important
        // closeing the singletion at the end
        transientProvider.shutdown();
        threadedProvider.shutdown();
        pooledProvider.shutdown();
        singletonProvider.shutdown();

        nameDefMap.clear();
        instanceDefMap.clear();
        classDefMap.clear();
    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    /**
     * @see com.bluejungle.framework.comp.IComponentManager#isComponentRegistered(java.lang.String)
     */
    public synchronized boolean isComponentRegistered(String name) {
        checkShutdown();
        return nameDefMap.containsKey(name);
    }

    /**
     * @see com.bluejungle.framework.comp.IComponentManager#registerComponent(com.bluejungle.framework.comp.ComponentInfo, boolean)
     */
    public synchronized void registerComponent(ComponentInfo info, boolean override) {
        checkShutdown();
        String name = info.getName();
        if (override || !nameDefMap.containsKey(name)) {
            nameDefMap.put(name, info);
        }
    }

    public synchronized void unregisterComponent(ComponentInfo info) {
        checkShutdown();
        String name = info.getName();
        nameDefMap.remove(name);
    }

    private synchronized void checkShutdown() {
        if (shutdown) {
            throw new RuntimeException("Component Manager is shut down!");
        }
    }
}
