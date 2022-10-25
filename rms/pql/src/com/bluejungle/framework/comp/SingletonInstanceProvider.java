package com.bluejungle.framework.comp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Copyright Blue Jungle, Inc.

/**
 * SingletonInstanceProvider is an instance provider that manager components
 * with a singleton lifestyle.
 *
 * @author Sasha Vladimirov
 * @version $Id:
 *          //depot/branch/Destiny_1.5.1/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/SingletonInstanceProvider.java#1 $
 */
public class SingletonInstanceProvider extends InstanceProviderBase implements IInstanceProvider {

    private LinkedHashMap<String, Object> components;

    protected SingletonInstanceProvider(ComponentManagerImpl mgr) {
        super(mgr);
        components = new LinkedHashMap<String, Object>();
    }

    public Object getComponent(ComponentInfo info, Object instance) {
        try {
            String name = info.getName();
            Object rv = components.get(name);
            if (rv == null || !rv.getClass().getName().equals(info.getClassName())) {
                if (instance != null && info.getClassName().equals(instance.getClass().getName())) {
                    rv = instance;
                } else {
                    Class implClass = Class.forName(info.getClassName());
                    rv = implClass.newInstance();
                }
                doFullInit(rv, info);
                components.put(name, rv);
                mgr.registerInstance(rv, info);
            }
            return rv;
        } catch (Exception e) {
            // TODO: construct reasonable exception
            throw new RuntimeException(e);
        }
    }

    public void release(Object component) {
        String componentName = null;

        Iterator componentsIterator = this.components.entrySet().iterator();
        while (componentsIterator.hasNext()) {
            Map.Entry nextEntry = (Map.Entry)componentsIterator.next();
            if (nextEntry.getValue() == component) {
                componentName = (String)nextEntry.getKey();
            }
        }
        if (componentName != null) {
            this.components.remove(componentName);
        }
    }

    protected Collection getInstantiatedComponents() {
        // return from the latest one to the earliest one
        List values = new ArrayList(components.values());
        Collections.reverse(values);
        return values;
    }

    /**
     * Cleans up the singleton object instances
     *
     * @see com.bluejungle.framework.comp.InstanceProviderBase#shutdown()
     */
    public void shutdown() {
        super.shutdown();
        this.components.clear();
    }
}
