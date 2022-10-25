package com.bluejungle.framework.expressions;

/*
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A <code>List</code>-based multivalue.
 * Instances of this class are immutable and therefore thread-safe.
 *
 * @author sasha, sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/Multivalue.java#1 $:
 */

public class Multivalue implements IMultivalue, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Object> values;
    private final ValueType type;
    private static final int EMPTY_HASHCODE = -1;
    private int cachedHashCode = EMPTY_HASHCODE;

    /**
     * Constructor, creates multivalue that contains
     * all the specified values. All values will be
     * of the same type.
     */
    protected <T> Multivalue(Collection<T> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.values = new ArrayList<Object>(values.size());
        Iterator<T> iter = values.iterator();
        ValueType vt;
        do {
            vt = ValueType.forObject(iter.next());
        } while (iter.hasNext() && vt == ValueType.NULL);
        type = vt;
        for (T val : values) {
            ValueType objType = ValueType.forObject(val);
            if (objType != ValueType.NULL) {
                if (objType != type) {
                    throw new IllegalArgumentException("values");
                }
                this.values.add(type.getInternalRepresentation(val));
            } else {
                this.values.add(null);
            }
        }
    }

    /**
     * Constructor, creates multivalue that contains
     * all the specified values. All values will be
     * of the same type.
     */
    protected <T> Multivalue(Collection<T> values, ValueType type) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.values = new ArrayList<Object>(values.size());
        this.type = type;
        for (T val : values) {
            this.values.add(type.getInternalRepresentation(val));
        }
    }

    public static <T> IMultivalue create(Collection<T> values) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        if (values.isEmpty()) {
            return IMultivalue.EMPTY;
        } else {
            return new Multivalue(values);
        }
    }

    public static <T> IMultivalue create(T... values) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        if (values.length == 0) {
            return IMultivalue.EMPTY;
        } else {
            return new Multivalue(Arrays.asList(values));
        }
    }

    public static <T> IMultivalue create(Collection<T> values, ValueType type) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        if (values.isEmpty()) {
            return IMultivalue.EMPTY;
        } else {
            return new Multivalue(values, type);
        }
    }

    public static <T> IMultivalue create(T[] values, ValueType type) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        if (values.length == 0) {
            return IMultivalue.EMPTY;
        } else {
            return new Multivalue(Arrays.asList(values), type);
        }
    }

    public static IMultivalue create(BitSet bits) {
        return new BitsetMultivalue(bits);
    }

    /**
     * @see IMultivalue#includes(EvalValue)
     */
    public boolean includes(IEvalValue val) {
        if (val == null) {
            return false;
        }
        if (val.getType() == ValueType.NULL) {
            return values.contains(null);
        }
        if (val.getType() != type) {
            return false;
        }
        return values.contains(val.getValue());
    }

    /**
     * @see IMultivalue#intersects(IMultivalue)
     */
    public boolean intersects(IMultivalue multival) {
        if (multival == null) {
            return false;
        }
        Set<Object> seen = new HashSet<Object>(values);
        for (IEvalValue o : multival) {
            if (seen.contains(o.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see IMultivalue#intersects(IMultivalue, IMultivalueEqual)
     */
    public boolean intersects(IMultivalue multival, IMultivalueEqual eq) {
        if (multival == null) {
            return false;
        }

        for (IEvalValue arg : multival) {
            for (IEvalValue me : this) {
                if (eq.equal(arg, me)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @see IMultivalue#includes(IMultivalue)
     */

    public boolean includes(IMultivalue multival) {
        if (multival == null) {
            // null is not the same as an empty multival
            return false;
        }
        Set<Object> seen = new HashSet<Object>(values);
        for (IEvalValue o : multival) {
            if (!seen.contains(o.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see IMultivalue#includes(IEvalValue, IMultivalueEqual)
     */
    public boolean includes(IEvalValue val, IMultivalueEqual eq) {
        if (val == null) {
            return false;
        }

        for (IEvalValue myVal : this) {
            if (eq.equal(val, myVal)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see IMultivalue#includes(IMultivalue, IMultivalueEqual)
     */
    public boolean includes(IMultivalue multival, IMultivalueEqual eq) {
        if (multival == null) {
            // null is not the same as an empty multival
            return false;
        }

        for (IEvalValue arg : multival) {
            boolean found = false;
            for (IEvalValue me : this) {
                if (eq.equal(arg, me)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        return type + ":" + values;
    }

    public ValueType getType() {
        return type;
    }

    public boolean isEmpty() {
        return false;
    }

    public Iterator<IEvalValue> iterator() {
        return new Iterator<IEvalValue>() {

            private final Iterator<?> iter = values.iterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public IEvalValue next() {
                return new IEvalValue() {

                    private static final long serialVersionUID = 1L;
                    Object val = iter.next();

                    public ValueType getType() {
                        return val != null ? type : ValueType.NULL;
                    }

                    public Object getValue() {
                        return val;
                    }

                    @Override
                    public int hashCode() {
                        return type.hashCode() ^ val.hashCode();
                    }

                    @Override
                    public boolean equals(Object otherObj) {
                        if (otherObj == this) {
                            return true;
                        }
                        if (!(otherObj instanceof IEvalValue)) {
                            return false;
                        }
                        IEvalValue other = (IEvalValue)otherObj;
                        return type.equals(other.getType()) && val.equals(other.getValue());

                    }
                };
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    public int size() {
        return values.size();
    }

    public <T> T[] toArray(T[] array) {
        return values.toArray(array);
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == EMPTY_HASHCODE) {
            int res = 0;
            for (IEvalValue ev : this) {
                res ^= ev.hashCode();
            }
            cachedHashCode = res;
        }
        return cachedHashCode;
    }

    @Override
    public boolean equals(Object otherObj) {
        if (this == otherObj) {
            return true;
        }
        if (!(otherObj instanceof IMultivalue)) {
            return false;
        }
        IMultivalue other = (IMultivalue)otherObj;
        Iterator<IEvalValue> a = this.iterator();
        Iterator<IEvalValue> b = other.iterator();
        while (a.hasNext() || b.hasNext()) {
            if (!a.next().equals(b.next())) {
                return false;
            }
        }
        return a.hasNext() == b.hasNext();
    }

    public Object get(int index) {
        return values.get(index);
    }
}
