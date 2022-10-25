package com.bluejungle.framework.comp;

import java.util.Collection;

// Copyright Blue Jungle, Inc.

/**
 * InstanceProviderBase implements some convenience methods that
 * can be used by any class implemented IInstanceProvider
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/InstanceProviderBase.java#1 $
 */
public abstract class InstanceProviderBase {

    protected ComponentManagerImpl mgr;

    protected InstanceProviderBase(ComponentManagerImpl mgr) {
        this.mgr = mgr;
    }

    /**
     * requires: none of the arguments are null
     * modifies: comp
     * effects: performs a full initialization on the given component,
     * depending on which interfaces the component implements.
     *
     * @param comp component to initialize
     * @param log  logger to supply to component
     */
    protected void doFullInit(Object comp, ComponentInfo info) {
        if (comp instanceof IManagerEnabled) {
            ((IManagerEnabled)comp).setManager(mgr);
        }
        if (comp instanceof IConfigurable) {
            ((IConfigurable)comp).setConfiguration(info.getConfiguration());
        }
        if (comp instanceof IInitializable) {
            ((IInitializable)comp).init();
        }
        if (comp instanceof IStartable) {
            ((IStartable)comp).start();
        }
    }

    //TODO make this configurable.
    private static final long SHUTDOWN_TIMEOUT = 30000;

    private class TimeoutThread extends Thread {

        final String name;

        TimeoutThread(String name) {
            super();
            this.name = name;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(SHUTDOWN_TIMEOUT);
            } catch (InterruptedException e) {
                // the job is done before the timeout.
            }
        }
    }

    /**
     * the order of the return is very important. The latest object will be in the first position.
     * @return
     */
    protected abstract Collection<?> getInstantiatedComponents();

    public void shutdown() {
        long threadId = Thread.currentThread().getId();
        Collection<?> components = getInstantiatedComponents();

        for (final Object comp : components) {

            if (comp instanceof IStartable) {
                TimeoutThread timeout = new TimeoutThread(comp.getClass().getName());
                timeout.start();
                try {
                    ((IStartable)comp).stop();
                } catch (RuntimeException e) {
                } finally {
                    timeout.interrupt();
                }
            }
            if (comp instanceof IDisposable) {
                TimeoutThread timeout = new TimeoutThread(comp.getClass().getName());
                timeout.start();
                try {
                    ((IDisposable)comp).dispose();
                } catch (RuntimeException e) {
                } finally {
                    timeout.interrupt();
                }
            }
        }
    }
}
