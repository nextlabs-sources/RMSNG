/*
 * Created on Jul 15, 2009
 *
 * All sources, binaries and HTML pages (C) copyright 2004-2008 by NextLabs, Inc.,
 * San Mateo CA, Ownership remains with NextLabs, Inc., All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.comp;

/**
 * @author hchan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/PropertyKey.java#1 $
 */

public class PropertyKey<T> implements java.io.Serializable, Comparable<PropertyKey<?>> {

    private final String name;

    public PropertyKey(String name) {
        this.name = name;
    }

    public PropertyKey(Class clazz, String name) {
        this(clazz.getName() + "." + name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PropertyKey other = (PropertyKey)obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        //don't change this, it may break something such as from PropertyKey to String
        return name;
    }

    public int compareTo(PropertyKey<?> o) {
        return this.name.compareTo(o.name);
    }

    public char charAt(int index) {
        return name.charAt(index);
    }

    public int length() {
        return name.length();
    }

    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }
}
