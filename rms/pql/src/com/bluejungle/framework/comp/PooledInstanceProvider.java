/*
 * Created on Nov 17, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.comp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

// Copyright Blue Jungle, Inc.

/**
 * PooledInstanceProvider is an instance provider that managers components with
 * a pooled lifestyle.
 *
 * @author hfriedland
 * @version "$Id:
 *          //depot/main/Destiny/main/src/common/framework/com/bluejungle/framework/comp/PooledInstanceProvider.java#3 $"
 */

public class PooledInstanceProvider extends InstanceProviderBase implements IInstanceProvider {

    // a hash map of the components
    // <key, value> is <componentName, componentPool>
    private LinkedHashMap<String, Object> components;

    /**
     * Create an instance of PooledInstanceProvider
     *
     * @param manager --
     *            an instance of the component manager
     */
    protected PooledInstanceProvider(ComponentManagerImpl mgr) {
        super(mgr);
        components = new LinkedHashMap<String, Object>();
    }

    /**
     * effects: Returns a component from some pool If the pool for this class of
     * objects does not exist, create it. If the pool exists, try to acquire an
     * object from that pool. If the acquisition is uncessfull a System
     * exception is thrown.
     *
     * requires: None of the arguments can be null
     *
     * @param info --
     *            an applicable instance of ComponentInfo
     * @param log --
     *            an applicable instance of Log
     * @return -- a component from a pool (never null)
     */
    public Object getComponent(ComponentInfo info, Object instance) {

        String name = info.getName();
        BJPool pool = (BJPool)components.get(name);
        // no pool exists for this object
        // create a new pool and add it to the hash map
        if (pool == null) {
            pool = new BJPool(this, info);
            if (pool == null) {
                //TODO exception
                throw new RuntimeException();
            }
            components.put(name, pool);
        }

        // get an object from the pool
        IBJPooledObject pooledObject = pool.acquireObject();
        mgr.registerInstance(pooledObject, info);
        return pooledObject;
    }

    /**
     * effects: release a component back to the pool requires: component is not
     * null
     *
     * @param component
     */
    public void release(Object component) {
        if (component == null) {
            return;
        }
        // what is the component name for this object?
        String componentName = null;
        componentName = mgr.getComponentName(component);
        // get the pool for this component name
        BJPool pool = (BJPool)components.get(componentName);
        if (pool == null) {
            // the user is trying to release an object
            // that we don't know anything about. just return
            return;
        }

        // it is ok to cast component to IBJPooledObject at this point
        // because if this was not an object derived from IBJPooledObject
        // we would have returned above and not get to this point
        pool.releaseObject((IBJPooledObject)component);
    }

    protected Collection getInstantiatedComponents() {
        ArrayList rv = new ArrayList();
        List pools = new ArrayList(components.values());
        Collections.reverse(pools);

        Iterator iter = pools.iterator();
        while (iter.hasNext()) {
            BJPool pool = (BJPool)iter.next();
            rv.addAll(pool.getInstantiatedComponents());
        }
        return rv;
    }
}
