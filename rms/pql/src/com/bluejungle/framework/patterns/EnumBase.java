/*
 * Created on Dec 30, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */
package com.bluejungle.framework.patterns;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EnumBase is the base class that should be subclassed whenever an enumeration is
 * needed.
 * <p>
 * It provides a mechanism for automatically assigning unique ordinal (enumType) to
 * each element of the enumeration, or of accepting an ordinal already assigned to
 * each element.  Ordinals are assigned starting with 0.
 * It also provides a mechanism for getting the right enumeration
 * element by ordinal and by name.  If an implementing subclass wishes to use those
 * mechanisms, it needs to expose them and delegate the implementation to this class.
 * EnumBase uses a map that keeps track of all the enumeration elements for every class.
 * Each enumeration's namespace is separate, and the separation is done using the class
 * of the enumeration subclass.
 * <p>
 * If the subclasses wishes to use any of the base class mechanisms, it should add
 * each enumeration element to the map using one of the two addElement methods.
 * This can be easily done in the subclass constructor.   Keep in mind that if
 * anonymous inner classes are used to implement enumerations, those
 * classes cannot be used as a key into the map, because they are unique.  Instead
 * use the parent's class as the key.  For an example of this and other usage see
 * BooleanOp and PredicateConstants.
 * <p>
 * EnumBase is safe for use in a multithreaded environment as long as the registration
 * of elements for any particular enumeration class is serialized.  That is, two different
 * enumeration classes, EnumOne and EnumTwo can be registering their elements at the
 * same time, but within EnumOne and EnumTwo that registration has to be serialized.  One easy
 * way to achieve this serialization is by initializing and registering <b>all</b> the
 * elements of an enumeration as static members of the enumeration class.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/patterns/EnumBase.java#1 $:
 */

public abstract class EnumBase implements IEnum, Serializable {

    protected int enumType;
    protected String enumName;

    private static final Map<Class, EnumDescription> CLASSMAP = new HashMap<Class, EnumDescription>();

    /**
     * @param clazz enumeration's class
     * @return number of elements in the enumeration
     */
    protected static int numElements(Class clazz) {
        EnumDescription desc = getEnumDescription(clazz);
        return desc.numElements();
    }

    /**
     * Adds an enumeration element to the map.  This method should be used
     * if the subclass wishes to use automatic int enumType generation.
     * @param elem element to add
     * @param clazz enumeration's class
     * @throws IllegalArgumentException if the name assigned to elem is
     * a duplicate.
     */
    protected static <T extends EnumBase> void addElement(T elem, Class clazz) {
        EnumDescription<T> desc = getOrCreateEnumDescription(clazz);
        desc.addElement(elem);
    }

    /**
     * Adds an enumeration element to the map.  This method should be used
     * if the subclass wishes to handle the assignment of int enumType by
     * itself.
     *
     * @param elem element to add
     * @param enumType enumType assigned to the element
     * @param clazz enumeration's class
     * @throws IllegalArgumentException if enumType or name assigned to elem
     * is a duplicate.
     */
    protected static <T extends EnumBase> void addElement(T elem, int enumType, Class clazz) {
        EnumDescription<T> desc = getOrCreateEnumDescription(clazz);
        desc.addElement(elem, enumType);
    }

    /**
     * gets an enumeration element by type
     * @param enumType type of the enumeration element
     * @param clazz class of the enumeration
     * @return  enumeration element
     * @throws IllegalArgumentException if there is no
     * enumeration element of type enumType, or there are no
     * enumeration elements added for class clazz
     */
    protected static <T extends EnumBase> T getElement(int enumType, Class<T> clazz) {
        EnumDescription<T> desc = getEnumDescription(clazz);
        return desc.get(enumType);
    }

    /**
     * gets an enumeration element by name
     * @param enumName name of the enumeration element
     * @param clazz class of the enumeration
     * @return enumeration element
     * @throws IllegalArgumentException if there is no
     * enumeration element with name enumName, or there are no
     * enumeration elements added for class clazz
     */
    protected static <T extends EnumBase> T getElement(String enumName, Class<T> clazz) {
        EnumDescription<T> desc = getEnumDescription(clazz);
        return desc.get(enumName);
    }

    /**
     * Checks whether the element of a given type exists.
     *
     * @param enumType type of the enumeration element
     * @param clazz class of the enumeration
     * @return true if the element with the given type exists,
     * false otherwise
     */
    protected static boolean existsElement(int enumType, Class clazz) {
        EnumDescription desc;

        synchronized (EnumBase.class) {
            desc = CLASSMAP.get(clazz);
        }
        if (desc == null) {
            return false;
        }

        return desc.exists(enumType);
    }

    /**
     * Checks whether  the element withe the given name exists.
     *
     * @param enumName name of the enumeration element
     * @param clazz class of the enumeration
     * @return true if the element with the given type exists,
     * false otherwise
     */
    protected static boolean existsElement(String enumName, Class clazz) {
        EnumDescription desc;
        synchronized (EnumBase.class) {
            desc = CLASSMAP.get(clazz);
        }
        if (desc == null) {
            return false;
        }
        return desc.exists(enumName);
    }

    /**
     * Retuns all the elements of an enumeration
     *
     * @param enumClass class of the enumeration
     * @return all elements of the enumeration
     * @throws IllegalArgumentException if there are no elements for enumClass
     */
    protected static <T extends EnumBase> Set<T> elements(Class<T> enumClass) {
        EnumDescription<T> desc = getEnumDescription(enumClass);
        return desc.elements();
    }

    /**
     * returns an EnumDescription for the given enumeration class.  If a
     * description does not exist for this class, a new one is created.
     *
     * @param enumClass enumeration's class
     * @return EnumDescription for the given enumeration class
     */
    private static synchronized <T extends EnumBase> EnumDescription<T> getOrCreateEnumDescription(Class enumClass) {
        EnumDescription<T> desc;
        synchronized (EnumBase.class) {
            desc = CLASSMAP.get(enumClass);
            if (desc == null) {
                desc = new EnumDescription<T>();
                CLASSMAP.put(enumClass, desc);
            }
        }
        return desc;
    }

    /**
     * @param clazz class of the enumeration
     * @return EnumDescription for the given class, never null
     * @throws IllegalArgumentException if there is no description for this
     * class
     */
    private static <T extends EnumBase> EnumDescription<T> getEnumDescription(Class clazz) {
        EnumDescription<T> desc;
        desc = CLASSMAP.get(clazz);
        if (desc == null) {
            throw new IllegalArgumentException("Unkown enum class: " + clazz);
        }
        return desc;
    }

    /**
     *
     * Constructor.  Use this constructor if you wish to supply the enumType and
     * your own class as a key into the enumeration map
     *
     * @param name enumeration element name
     * @param type enumeration element type
     * @param clazz key into the enumeration map
     */
    protected EnumBase(String name, int type, Class clazz) {
        this.enumName = name;
        this.enumType = type;
        addElement(this, type, clazz);
    }

    /**
     *
     * Constructor.  Use this constructor if you wish to supply your own
     * class as a key into the enumeration map and have EnumBase generate
     * a unique enumType for you.
     *
     *
     * @param name enumeration element name
     * @param clazz key into the enumeration map
     */
    protected EnumBase(String name, Class clazz) {
        this.enumName = name;
        addElement(this, clazz);
    }

    /**
     * Constructor.  Use this constructor if you wish to supply the enumType and
     * class of the enumeration implementation as a key into the enumeration map
     *
     * @param name enumeration element name
     * @param type enumeration element type
     */
    protected EnumBase(String name, int type) {
        this.enumName = name;
        this.enumType = type;
        addElement(this, type, this.getClass());
    }

    /**
     * Constructor.  Use this constructor if you wish to have EnumBase generate
     * a unique enumType for you and use class of the enumeration implementation
     * as a key into the enumeration map
     *
     * @param name enumeration element name
     */
    protected EnumBase(String name) {
        this.enumName = name;
        addElement(this, this.getClass());
    }

    /**
     * @see com.bluejungle.framework.patterns.IEnum#getName()
     */
    public String getName() {
        return enumName;
    }

    /**
     * @see com.bluejungle.framework.patterns.IEnum#getType()
     */
    public final int getType() {
        return enumType;
    }

    public String toString() {
        return enumName;
    }

    /**
     * compares based on class and enumType
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this.getClass().equals(o.getClass())) {
            return (enumType == ((IEnum)o).getType());
        }
        return false;
    }

    /**
     * hashCode is enumType
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return enumType;
    }

    /**
     * sets the enumeration type
     * @param type enumeration type
     */
    private void setType(int type) {
        this.enumType = type;
    }

    private EnumBase readResolve() {
        Class thisClass = this.getClass();
        do {
            if (existsElement(enumType, thisClass)) {
                return getElement(enumType, thisClass);
            }
            thisClass = thisClass.getSuperclass();
        } while (thisClass != EnumBase.class);

        return this;
    }

    private static final long serialVersionUID = 1;

    /**
     * Description of all enumeration elements for a single
     * enumeration type.
     *
     * @author sasha
     */
    private static class EnumDescription<T extends EnumBase> {

        private int curOrdinal = 0;
        private Map<Integer, T> ordinalMap = new HashMap<Integer, T>();
        private Map<String, T> nameMap = new HashMap<String, T>();

        void addElement(T elem) {
            ((EnumBase)elem).setType(curOrdinal);
            addElementInternal(elem, curOrdinal++);
        }

        void addElement(T elem, int ordinal) {
            T curr = ordinalMap.get(ordinal);
            if (curr != null && curr != elem) {
                throw new IllegalArgumentException("duplicate enumType: " + ordinal);
            }
            addElementInternal(elem, ordinal);

        }

        void addElementInternal(T elem, int ordinal) {

            ordinalMap.put(ordinal, elem);
            EnumBase curr = nameMap.get(elem.getName());
            if (curr != null && curr != elem) {
                throw new IllegalArgumentException("duplicate enumName: " + elem.getName());
            }
            nameMap.put(elem.getName(), elem);
        }

        Set<T> elements() {
            return Collections.unmodifiableSet(new HashSet<T>(ordinalMap.values()));
        }

        T get(int i) {
            T rv = ordinalMap.get(i);
            if (rv == null) {
                throw new IllegalArgumentException("unknown enumType: " + i);
            }
            return rv;
        }

        boolean exists(int i) {
            return ordinalMap.containsKey(i);
        }

        T get(String enumName) {
            T rv = nameMap.get(enumName);
            if (rv == null) {
                throw new IllegalArgumentException("unkown enumName: " + enumName);
            }
            return rv;
        }

        boolean exists(String enumName) {
            return nameMap.containsKey(enumName);
        }

        int numElements() {
            return ordinalMap.size();
        }

    }

}
