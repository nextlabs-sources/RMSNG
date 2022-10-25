/*
 * Created on Nov 17, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

package com.bluejungle.framework.comp;

import java.util.Collection;
import java.util.Collections;

// Copyright Blue Jungle, Inc.

/**
 *
 * TransientInstanceProvider is an instance provider that managers components
 * with a transient lifestyle.
 *
 * @author hfriedland
 * @version "$Id:
 *          //depot/main/Destiny/main/src/common/framework/com/bluejungle/framework/comp/TransientInstanceProvider.java#4 $"
 */

public class TransientInstanceProvider extends InstanceProviderBase implements IInstanceProvider {

    /**
     * Create an instance of TransientInstanceProvider
     *
     * @param manager --
     *            an instance of the component manager
     */
    protected TransientInstanceProvider(ComponentManagerImpl mgr) {
        super(mgr);
    }

    /**
     * effects: Returns a component instance requires: None of the arguments can
     * be null
     *
     * @param info --
     *            an applicable instance of ComponentDefinition
     * @param log --
     *            an applicable instance of Log
     * @return -- a component
     */
    public Object getComponent(ComponentInfo info, Object instance)

    {
        try {
            Class implClass = Class.forName(info.getClassName());
            Object rv = implClass.newInstance();
            doFullInit(rv, info);
            return rv;
        }

        catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * does not do much at all
     *
     * @param component
     */
    public void release(Object component) {
        // because this is a transient container, there is no releasing
    }

    protected Collection getInstantiatedComponents() {
        return Collections.EMPTY_LIST;
    }
}
