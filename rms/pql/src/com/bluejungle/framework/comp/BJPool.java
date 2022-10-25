package com.bluejungle.framework.comp;

import java.util.ArrayList;
import java.util.Collection;

// Copyright Blue Jungle, Inc.

/**
 *
 * This class provides pool functionality. At this time it depends on
 * a PooledInstanceProvider object so it cannot exist outside of the
 * ComponentManager framework
 *
 * At the construction time, the pool object is created with the size
 * specified in the preferences (passed in to the constructor). If no
 * size is specified in the prefs, the default pool size is used (2).
 *
 * Only objects that implement IBJPooledObject interface can be pooled.
 * This was done because pooled objects might need to have init() and reset()
 * methods. Note: I am still not sure about init(), it looks like Java people
 * do all initialization in constructors, and don't care if they fail.
 *
 * BJPool API is really simple.
 * BJPool() -- constructs a pool object
 * BJPool.acquireObject() -- returns a IBJPooledObject from the pool
 * or throws an exception if there's none available
 * BJPool.releaseObject() -- releases an IBJPooledObject back to the pool
 *
 * @author hfriedland
 * @version "$Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/BJPool.java#1 $"
 */

public class BJPool
{

    private final static String CLASSNAME = BJPool.class.getName();
    private final static String NOT_POOLED_EXCEPTION = CLASSNAME + ".NOT_POOLED";

    private final static int DEFAULT_SIZE = 2;
    private final static int DEFAULT_INCREMENT_SIZE = 2;
    private final static int DEFAULT_MAX_SIZE = 30;

    private int poolSize = 0;
    private int maxSize = 0;
    private int incrementSize = 0;

    // Pooled objects available and in use.
    private ArrayList freePooledObjects = new ArrayList();
    private ArrayList inusePooledObjects = new ArrayList();

    // Keep all this information in member variables so that
    // we can init objects in the pool at any time
    private PooledInstanceProvider instanceManager = null;
    private ComponentInfo info = null;

    /**
     *
     * effects: creates an instance of the BJPool
     * requires: none of the arguments is null
     *
     * @param mng   -- an instance of the PooledInstanceProvider class
     * @param info  -- an applicable instance of the ComponentInfo class
     * @param log   -- an applicable instance of the Log class
     *
     */
    public BJPool(PooledInstanceProvider mng, ComponentInfo info) {
        poolSize = DEFAULT_SIZE;
        incrementSize = DEFAULT_INCREMENT_SIZE;
        maxSize = DEFAULT_MAX_SIZE;

        // save these values so that we can add more pool objects
        // later (in acquireObject()) if needed
        instanceManager = mng;
        this.info = info;
        // Populate the pool
        initPool(poolSize);
    }

    /**
     * effects: Acquire an object from the pool.
     * If no objects are available and the pool is at its maximum size
     * throw a system exception. If no objects are available and there's
     * room to increase the pool, increase the pool by the "increment" size
     * and return a pooled object.
     *
     * @return IBJPooledObject -- an object from the pool
     */
    public IBJPooledObject acquireObject()
    {
        if (freePooledObjects.size() == 0)
        {
            if ((maxSize < poolSize) && (incrementSize > 0))
            {
                initPool(incrementSize);
            }
            else
            {
                // we cannot increase the pool size and no
                // free objects are available, so throw an exception
                throw new RuntimeException();
            }
        }

        // Remove the first
        IBJPooledObject obj = (IBJPooledObject)freePooledObjects.remove(0);

        // Add it to the in-use list
        inusePooledObjects.add(obj);
        return obj;
    }

    /**
     *
     * effects: Release a IBJPooledObject back into the pool.
     * If the user is trying to release  an object
     * that we know nothing about just return.
     * requires: pooledObject is not null
     *
     * @param pooledObject
     */
    public void releaseObject(IBJPooledObject pooledObject)
    {
        int index = inusePooledObjects.indexOf(pooledObject);
        if (index == -1)
        {
            // we know nothing about this object
            return;
        }

        // before releasing it back into the pool, reset the object
        pooledObject.reset();
        freePooledObjects.add(inusePooledObjects.remove(index));
    }

    Collection getInstantiatedComponents() {
        ArrayList rv = new ArrayList(inusePooledObjects.size() + freePooledObjects.size());
        rv.addAll(inusePooledObjects);
        rv.addAll(freePooledObjects);
        return rv;
    }

    /**
     * This is a private method that performs pool initialization
     * Before calling this method class variables (instanceManager,
     * def, ctx, log, cfg) must be initialized to meaningfull non-null
     * values
     *
     * @param size -- the number of free objects to be added to the pool
     */
    private void initPool(int size)
    {
        for (int i = 0; i < size; i++)
        {

            // pooledObjectClass must implement IBJPooledObject
            Class pooledObjectClass;
            try {
                pooledObjectClass = Class.forName(info.getClassName());
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
            Object rv;
            try {
                rv = pooledObjectClass.newInstance();
            } catch (InstantiationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e1);
            }
            // check to make sure pooledObjectClass implements IBJPooledObject
            if (!(rv instanceof IBJPooledObject))
            {
                throw new IllegalArgumentException(NOT_POOLED_EXCEPTION);
            }

            instanceManager.doFullInit(rv, info);
            // any o	bject specific initialization can be done here
            ((IBJPooledObject)rv).init();
            freePooledObjects.add((IBJPooledObject)rv);
        }
    }
}
