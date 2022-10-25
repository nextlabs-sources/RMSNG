package com.bluejungle.framework.comp;

import com.bluejungle.framework.patterns.EnumBase;

// Copyright Blue Jungle, Inc.

/**
 * LifestyleType is an enumeration representing possible component lifestyle types.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/LifestyleType.java#1 $
 */
public abstract class LifestyleType extends EnumBase {

    /**
     * SINGLETON_TYPE is a singleton lifestyle
     */
    public static final LifestyleType SINGLETON_TYPE = new LifestyleType("singleton") {

        public IInstanceProvider getProvider(ComponentManagerImpl mgr) {
            return mgr.singletonProvider;
        }
    };
    /**
     * TRANSIENT_TYPE is a lifestyle where a new instance is created every time a component instance is requested. Components implementing this lifestyle are not stoped or disposed of during shutdown.
     */
    public static final LifestyleType TRANSIENT_TYPE = new LifestyleType("transient") {

        IInstanceProvider getProvider(ComponentManagerImpl mgr) {
            return mgr.transientProvider;
        }

    };
    /**
     * THREADED_TYPE is a lifestyle where there's only one instance of a component for every existing thread. This is a simple way of making a thread-safe component.
     */
    public static final LifestyleType THREADED_TYPE = new LifestyleType("threaded") {

        IInstanceProvider getProvider(ComponentManagerImpl mgr) {
            return mgr.threadedProvider;
        }

    };
    /**
     * POOLED_TYPE is a lifestyle where instances of the component are pooled.
     */
    public static final LifestyleType POOLED_TYPE = new LifestyleType("pooled") {

        IInstanceProvider getProvider(ComponentManagerImpl mgr) {
            return mgr.pooledProvider;
        }

    };

    private LifestyleType(String name) {
        super(name, LifestyleType.class);
    }

    abstract IInstanceProvider getProvider(ComponentManagerImpl mgr);

}
