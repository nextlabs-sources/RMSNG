package com.bluejungle.framework.utils;

/*
 * Created Jan 10, 2007
 *
 * All sources, binaries and HTML pages (C) Copyright 2007
 * by Blue Jungle Inc., San Mateo CA, Ownership remains with
 * Blue Jungle Inc, All rights reserved worldwide.
 */

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.framework.expressions.ValueType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author amorgan
 */

public class DynamicAttributes implements Map<String, IEvalValue> {

    public static final DynamicAttributes EMPTY = new DynamicAttributes() {

        /**
         * @see DynamicAttributes#add(String, String)
         */
        @Override
        public void add(String key, String value) {
            throw new UnsupportedOperationException("add");
        }

        /**
         * @see DynamicAttributes#clear()
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }

        /**
         * @see DynamicAttributes#put(String, Collection)
         */
        @Override
        public void put(String key, Collection<String> values) {
            throw new UnsupportedOperationException("put");
        }

        /**
         * @see DynamicAttributes#put(String, com.bluejungle.framework.expressions.IEvalValue)
         */
        @Override
        public IEvalValue put(String key, IEvalValue value) {
            throw new UnsupportedOperationException("put");
        }

        /**
         * @see DynamicAttributes#put(String, long)
         */
        @Override
        public void put(String key, long value) {
            throw new UnsupportedOperationException("put");
        }

        /**
         * @see DynamicAttributes#put(String, String)
         */
        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException("put");
        }

        /**
         * @see DynamicAttributes#put(String, String[])
         */
        @Override
        public void put(String key, String[] values) {
            throw new UnsupportedOperationException("put");
        }

        /**
         * @see DynamicAttributes#putAll(Map)
         */
        @Override
        public void putAll(Map<? extends String, ? extends IEvalValue> t) {
            throw new UnsupportedOperationException("putAll");
        }

        /**
         * @see DynamicAttributes#remove(Object)
         */
        @Override
        public IEvalValue remove(Object key) {
            throw new UnsupportedOperationException("remove");
        }

    };

    private final Map<String, IEvalValue> attrs;

    private static final int DEFAULT_CAPACITY = 10;

    private static final String[] NO_STRINGS = new String[0];

    /**
     * Allocates a <code>DynamicsAttributes</code> object
     * with initial capacity of <code>initialCapacity</code> items
     *
     * @param   initialCapacity  preallocate space for this many items
     */
    public DynamicAttributes(int initialCapacity) {
        attrs = new HashMap<String, IEvalValue>(initialCapacity);
    }

    /**
     * Allocates a <code>DynamicAttributes</code> object with a default
     * size.
     */
    public DynamicAttributes() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Associates the specified value with the specified key
     */
    public void put(String key, String value) {
        attrs.put(key, EvalValue.build(value));
    }

    /**
     * Associates the specified value with the key and all previously
     * assigned keys (creating a Multivalue if necessary).
     *
     * The value will not be added if it is already present.
     */
    public void add(String key, String value) {
        IEvalValue curr = attrs.get(key);

        if (valueAlreadyPresent(value, curr)) {
            return;
        }

        if (curr == null) {
            put(key, value);
        } else if (curr.getType() == ValueType.MULTIVAL) {
            Multivalue mval = (Multivalue)curr.getValue();

            int size = mval.size();
            String[] vals = new String[size + 1];
            mval.toArray(vals);
            vals[size] = value;

            attrs.put(key, EvalValue.build(Multivalue.create(vals)));
        } else {
            attrs.put(key, EvalValue.build(Multivalue.create(new String[] { (String)curr.getValue(), value })));
        }
    }

    private static boolean valueAlreadyPresent(String value, IEvalValue curr) {
        if (curr == null) {
            return false;
        }

        if (curr.getType() == ValueType.MULTIVAL) {
            Multivalue mval = (Multivalue)curr.getValue();

            for (IEvalValue e : mval) {
                if (valueAlreadyPresent(value, e)) {
                    return true;
                }
            }

        } else {
            return ((value == null && curr.getValue() == null) || (value != null && value.equals((String)curr.getValue())));
        }

        return false;

    }

    /**
     * Associatesthe specified long value with the specified key
     */
    public void put(String key, long value) {
        attrs.put(key, EvalValue.build(value));
    }

    /**
     * Associates specified key with multiple values
     */
    public void put(String key, String[] values) {
        attrs.put(key, EvalValue.build(Multivalue.create(values)));
    }

    /**
     * Associates specified key with multiple values
     */
    public void put(String key, Collection<String> values) {
        attrs.put(key, EvalValue.build(Multivalue.create(values)));
    }

    /**
     * Returns the value to which this key is mapped as a string or <code>null</code>
     * if the key does not exist
     */
    public String getString(String key) {
        IEvalValue v = get(key);

        if (v == null) {
            return null;
        }

        return v.getValue().toString();
    }

    /**
     * Returns the value or values to which this key is mapped as an array of strings
     * (which will be empty if this doesn't exist).  This is the best interface if
     * you don't know if the key is mapped to multiple values or just one as it will
     * work correctly under both circumstances.
     */
    public String[] getStrings(String key) {
        IEvalValue v = get(key);

        if (v == null) {
            return NO_STRINGS;
        }

        if (v.getType() == ValueType.MULTIVAL) {
            IMultivalue mval = (IMultivalue)v.getValue();
            return mval.toArray(new String[mval.size()]);
        } else {
            return new String[] { v.getValue().toString() };
        }
    }

    public void clear() {
        attrs.clear();
    }

    /**
     * Returns true if there is an attribute with this key
     */
    public boolean containsKey(Object key) {
        return attrs.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return attrs.containsValue(value);
    }

    public Set<Map.Entry<String, IEvalValue>> entrySet() {
        return attrs.entrySet();
    }

    /**
     * Returns the value to which this key is mapped or <code>null</code>
     * if the key does not exist.
     */
    public IEvalValue get(Object key) {
        return attrs.get(key);
    }

    public boolean isEmpty() {
        return attrs.isEmpty();
    }

    public Set<String> keySet() {
        return attrs.keySet();
    }

    public IEvalValue put(String key, IEvalValue value) {
        return attrs.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends IEvalValue> t) {
        attrs.putAll(t);
    }

    public IEvalValue remove(Object key) {
        return attrs.remove(key);
    }

    public int size() {
        return attrs.size();
    }

    public Collection<IEvalValue> values() {
        return attrs.values();
    }

}
