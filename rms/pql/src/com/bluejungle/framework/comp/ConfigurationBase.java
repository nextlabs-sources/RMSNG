/*
 * Created on Dec 1, 2004
 * All sources, binaries and HTML pages
 * (C) copyright 2004 by Blue Jungle Inc., Redwood City CA,
 * Ownership remains with Blue Jungle Inc,
 * All rights reserved worldwide.
 */
package com.bluejungle.framework.comp;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for implementing IConfiguration interface.  Provided as a convenience
 * for developers.  Extend at will
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/ConfigurationBase.java#1 $
 */

public abstract class ConfigurationBase implements IConfiguration {

    public ConfigurationBase() {
    }

    public <T> ConfigurationBase(PropertyKey<T> key, T value) {
        this();
        setProperty(key, value);
    }

    /**
     * returns the value that corresponding to the propName.
     * If the value is not found, <code>defValue</code> will be returned
     *
     * @return the value corresponding to the propName.
     * @deprecated replaced by <code>T get(PropertyKey<T> key, T defValue)</code>
     */
    @Deprecated
    public Object get(String propName, Object defValue) {
        Object value = get(propName);
        if (value != null) {
            return value;
        }

        return defValue;
    }

    /**
     * returns the value that corresponding to the propName.
     * If the value is not found, <code>defValue</code> will be returned
     *
     * @return the value corresponding to the propName.
     */
    public <T> T get(PropertyKey<T> key, T defValue) {
        T value = get(key);
        if (value != null) {
            return value;
        }

        return defValue;
    }

    /**
     * @deprecated replaced by <code>T get(PropertyKey<T> propertyKey)</code>
     */
    @Deprecated
    public Object get(String propName) {
        PropertyKey<Object> propertyKey = new PropertyKey<Object>(propName);
        return get(propertyKey);
    }

    public void override(IConfiguration config) {
        Set<PropertyKey> newProps = config.propertyKeySet();

        for (PropertyKey property : newProps) {
            setProperty(property, config.get(property));
        }
    }

    /**
     * @deprecated replaced by <code>Set<PropertyKey> propertyKeySet()</code>
     */
    @Deprecated
    public Set<String> propertySet() {
        Set<PropertyKey> keys = propertyKeySet();
        Set<String> strKeys = new HashSet<String>();
        for (PropertyKey k : keys) {
            strKeys.add(k.toString());
        }
        return strKeys;
    }

    abstract <T> ConfigurationBase setProperty(PropertyKey<T> key, T value);

    /**
     * @deprecated replaced by <code>ConfigurationBase setProperty(PropertyKey<T> key, T value)</code>
     */
    @Deprecated
    public <T> ConfigurationBase setProperty(String propName, Object value) {
        PropertyKey<Object> propertyKey = new PropertyKey<Object>(propName);
        setProperty(propertyKey, value);
        return this;
    }
}
