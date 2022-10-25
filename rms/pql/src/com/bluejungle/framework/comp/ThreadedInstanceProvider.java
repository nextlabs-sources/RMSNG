package com.bluejungle.framework.comp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

// Copyright Blue Jungle, Inc.

/**
 * Threaded instance provider manages components that chose THREADED
 * lifestyle, meaning that every thread gets a singleton.
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/ThreadedInstanceProvider.java#1 $
 */
public class ThreadedInstanceProvider
        extends InstanceProviderBase
        implements IInstanceProvider
{

    private ThreadLocal components;
    private ArrayList allComponents;

    protected ThreadedInstanceProvider(ComponentManagerImpl mgr)
    {
        super(mgr);
        allComponents = new ArrayList();
        components = new ThreadLocal() {

            protected synchronized Object initialValue()
            {
                return new HashMap();
            }
        };
    }

    public Object getComponent(ComponentInfo info, Object instance)
    {
        try
        {
            String name = info.getName();
            HashMap map = (HashMap)components.get();
            Object rv = map.get(name);
            if (rv == null)
            {
                Class implClass = Class.forName(info.getClassName());
                rv = implClass.newInstance();

                doFullInit(rv, info);

                map.put(name, rv);
                allComponents.add(rv);
                mgr.registerInstance(rv, info);
            }
            return rv;
        } catch (Exception e)
        {
            // TODO: construct reasonable exception
            throw new RuntimeException(e);
        }

    }

    public void release(Object comp)
    {
        // per-thread singleton, nothing to release
    }

    protected Collection getInstantiatedComponents() {
        ArrayList list = new ArrayList(allComponents);
        Collections.reverse(list);
        return list;
    }
}
