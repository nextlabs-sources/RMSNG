/*
 * Created on Dec 1, 2004 All sources, binaries and HTML pages (C) copyright
 * 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle
 * Inc, All rights reserved worldwide.
 */
package com.bluejungle.framework.comp;

import java.util.Set;

/**
 * Represents a configuration of key-value property pairs
 *
 * @author sasha
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/framework/com/bluejungle/framework/comp/IConfiguration.java#1 $
 */

public interface IConfiguration {

    /**
     * returns the value associated with the given property in this
     * configuration, or defValue if this configuration does not have a value
     * for the specified property.
     *
     * @param propName
     *            property name
     * @param defValue
     *            property
     * @return value associated with propName, if any, defValue otherwise
     * @deprecated replaced by <code>T get(PropertyKey<T> propertyKey, T defValue)</code>
     */
    @Deprecated
    Object get(String propName, Object defValue);

    <T> T get(PropertyKey<T> propertyKey, T defValue);

    /**
     * returns the value associated with the given property in this
     * configuration, or null if this configuration does not have a value for
     * the specified property.
     *
     * @param propName
     *            property name
     * @return value associated with propName, if any, null otherwise
     * @deprecated replaced by <code>T get(PropertyKey<T> propertyKey)</code>
     */
    @Deprecated
    Object get(String propName);

    <T> T get(PropertyKey<T> propertyKey);

    /**
     * This overrides this configuration with the supplied one. Properties that
     * exist in this configuration but do not exist in the overriding one
     * are left untouched.
     *
     * @param config
     *            overriding configuration
     */
    void override(IConfiguration config);

    /**
     * @return set containing all the properties this configuration contains,
     * 			change this set will not effect the configuration
     * @deprecated replaced by <code>Set<PropertyKey> propertyKeySet()</code>
     */
    @Deprecated
    Set<String> propertySet();

    Set<PropertyKey> propertyKeySet();

    boolean contains(PropertyKey propertyKey);
}
