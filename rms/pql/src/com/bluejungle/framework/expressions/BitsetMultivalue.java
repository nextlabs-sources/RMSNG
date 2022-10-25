package com.bluejungle.framework.expressions;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/BitsetMultivalue.java#1 $
 */

import java.util.BitSet;
import java.util.Iterator;

/**
 * This implementation of <code>IMultivalue</code> is based on
 * <code>BitSet</code> objects. The implementation has package visibility
 * because it should be accessed only through the corresponding factory method
 * of the Multivalue class.
 *
 * @author sergey
 */
class BitsetMultivalue implements IMultivalue {

    /**
     * This is the representation of the bitset.
     * It is set in the constructor, and does not change after that.
     */
    private final BitSet bits;

    /**
     * This constructor saves the <code>BitSet</code> provided
     * as the input into the internal bits of the class.
     * The value is not cloned to optimize the common case,
     * so changing the BitSet after setting it into a multivalue
     * will result in changing the value of the Multivalue as well.
     *
     * @param bits the bits representing the values
     * to be placed into the <code>IMultivalue</code>.
     */
    public BitsetMultivalue(BitSet bits) {
        if (bits == null) {
            throw new NullPointerException("bits");
        }
        this.bits = bits;
    }

    /**
     * @see IMultivalue#get(int)
     */
    public Object get(int index) {
        throw new UnsupportedOperationException("get");
    }

    /**
     * @see IMultivalue#getType()
     */
    public ValueType getType() {
        return ValueType.LONG;
    }

    /**
     * @see IMultivalue#includes(EvalValue)
     */
    public boolean includes(IEvalValue val) {
        if (val == null || val == IEvalValue.NULL) {
            return false;
        }
        Object value = val.getValue();
        if (value instanceof Number) {
            return bits.get(((Number)value).intValue());
        } else {
            return false;
        }
    }

    /**
     * @see IMultivalue#includes(IMultivalue, IMultivalueEqual)
     */
    public boolean includes(IMultivalue multival, IMultivalueEqual eq) {
        return includes(multival);
    }

    /**
     * @see IMultivalue#includes(IEvalValue, IMultivalueEqual)
     */
    public boolean includes(IEvalValue val, IMultivalueEqual eq) {
        return includes(val);
    }

    /**
     * @see IMultivalue#includes(IEvalValue val)
     */
    public boolean includes(IMultivalue multival) {
        if (multival == null) {
            return false;
        }

        if (multival.getType() != ValueType.LONG) {
            return false;
        }

        for (IEvalValue obj : multival) {
            Long n = (Long)obj.getValue();
            if (n == null) {
                continue;
            }

            if (!bits.get(n.intValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see IMultivalue#intersects(IMultivalue)
     */
    public boolean intersects(IMultivalue multival, IMultivalueEqual eq) {
        return intersects(multival);
    }

    /**
     * @see IMultivalue#intersects(IMultivalue)
     */
    public boolean intersects(IMultivalue multival) {
        if (multival == null) {
            return false;
        }
        if (multival.getType() != ValueType.LONG) {
            return false;
        }
        for (IEvalValue obj : multival) {
            Long n = (Long)obj.getValue();
            if (n == null) {
                continue;
            }
            if (bits.get(n.intValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see IMultivalue#isEmpty()
     */
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    /**
     * @see IMultivalue#iterator()
     */
    public Iterator<IEvalValue> iterator() {
        return new Iterator<IEvalValue>() {

            private int pos = bits.nextSetBit(0);

            public boolean hasNext() {
                return pos != -1;
            }

            public IEvalValue next() {
                IEvalValue res = new IEvalValue() {

                    private static final long serialVersionUID = 1L;
                    final Long val = new Long(pos);

                    public ValueType getType() {
                        return ValueType.LONG;
                    }

                    public Object getValue() {
                        return val;
                    }
                };
                pos = bits.nextSetBit(pos);
                return res;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

        };
    }

    /**
     * @see IMultivalue#size()
     */
    public int size() {
        return bits.cardinality();
    }

    /**
     * @see IMultivalue#toArray(T[])
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] res) {
        if (res != null && !(res instanceof Long[])) {
            throw new IllegalArgumentException("array must be of type Long[]");
        }
        if (res == null || res.length != bits.cardinality()) {
            res = (T[])new Long[bits.cardinality()];
        }
        int i = 0;
        for (int pos = bits.nextSetBit(0); pos >= 0; pos = bits.nextSetBit(pos + 1)) {
            res[i++] = (T)new Long(pos);
        }
        return res;
    }

}
