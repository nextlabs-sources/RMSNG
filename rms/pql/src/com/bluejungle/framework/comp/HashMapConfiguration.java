/*
 * Created on Dec 1, 2004
 * All sources, binaries and HTML pages
 * (C) copyright 2004 by Blue Jungle Inc., Redwood City CA,
 * Ownership remains with Blue Jungle Inc,
 * All rights reserved worldwide.
 */
package com.bluejungle.framework.comp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * simple implementation of configuration based on a HashMap
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/HashMapConfiguration.java#1 $
 */

public class HashMapConfiguration extends ConfigurationBase {

    private final HashMap<PropertyKey, Object> map;

    public HashMapConfiguration() {
        super();
        map = new HashMap<PropertyKey, Object>();
    }

    @SuppressWarnings("deprecation")
    public HashMapConfiguration(Map<?, Object> properties) {
        this(properties.size());

        for (Map.Entry<?, Object> e : properties.entrySet()) {
            setProperty(e.getKey().toString(), e.getValue());
        }
    }

    public HashMapConfiguration(int initialSize) {
        super();
        map = new HashMap<PropertyKey, Object>(initialSize);
    }

    public <T> T get(PropertyKey<T> propertyKey) {
        return (T)map.get(propertyKey);
    }

    public Set<PropertyKey> propertyKeySet() {
        return map.keySet();
    }

    public <T> HashMapConfiguration setProperty(PropertyKey<T> name, T value) {
        map.put(name, value);
        return this;
    }

    public boolean contains(PropertyKey propertyKey) {
        return map.containsKey(propertyKey);
    }
}
