package com.bluejungle.framework.expressions;

import java.util.Iterator;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $ $
 */

/**
 * Objects of this class represent multi-valued sets used in inclusion operations.
 * Generic IN relation wraps IMultivalue and presents it as an IPredicate.
 */
public interface IMultivalue extends Iterable<IEvalValue> {

    /**
     * This constant represents an empty <code>IMultivalue</code>.
     */
    IMultivalue EMPTY = new IMultivalue() {

        public Iterator<IEvalValue> iterator() {
            return new Iterator<IEvalValue>() {

                public boolean hasNext() {
                    return false;
                }

                public EvalValue next() {
                    throw new IllegalStateException();
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }

        public boolean includes(IEvalValue val) {
            return false;
        }

        public boolean intersects(IMultivalue multival) {
            return false;
        }

        public boolean intersects(IMultivalue multival, IMultivalueEqual eq) {
            return false;
        }

        public boolean includes(IMultivalue multival) {
            return (multival == IMultivalue.EMPTY);
        }

        public boolean includes(IMultivalue multival, IMultivalueEqual eq) {
            return (multival == IMultivalue.EMPTY);
        }

        public boolean includes(IEvalValue multival, IMultivalueEqual eq) {
            return false;
        }

        public ValueType getType() {
            return ValueType.NULL;
        }

        public boolean isEmpty() {
            return true;
        }

        public int size() {
            return 0;
        }

        public <T> T[] toArray(T[] array) {
            if (array == null) {
                throw new NullPointerException("array");
            }
            return array;
        }

        public Object get(int index) {
            throw new UnsupportedOperationException("get(int)");
        }
    };

    /**
     * Performs the containment check on the specified value,
     * and returns true or false depending on the result.
     * @param val the value to be checked.
     * @return true if the val is included in the set; false otherwise.
     */
    boolean includes(IEvalValue val);

    /**
     * Determines if the two multivalues have one or more value in common.
     *
     * @param multival the multivalue to be checked.
     * @return true if the multival has one or more value in common
     * with this <code>IMultivalue</code>; false otherwise.
     */
    boolean intersects(IMultivalue multival);

    /**
     * Determines if the two multivalues have one or more values in common using
     * the supplied object to determine equality (rather than .equals())
     *
     * @param multival the multivalue to be checked
     * @param eq the equality tester
     * @return true if the multival has one or more value in common
     * with this <code>IMultivalue</code>; false otherwise.
     */
    boolean intersects(IMultivalue multival, IMultivalueEqual eq);

    /**
     * Determines if one multivalue is completely contained within another
     *
     * @param multival the candidate
     * @return true if every member of multival is included in this object,
     * false otherwise
     */
    boolean includes(IMultivalue multival);

    /**
     * Determines if one multivalue is completely contained within another
     * using the supplied class to determine equality of members
     *
     * @param multival the candidate
     * @param eq the equality tester
     * @return true if every member of multival is included in this object,
     * false otherwise
     */
    boolean includes(IMultivalue multival, IMultivalueEqual eq);

    /**
     * Performs a containment check using the supplied class to determine equality
     *
     * @param val the candidate
     * @param eq the equality tester
     * @return true if val is contained in this object, false otherwise
     */
    boolean includes(IEvalValue val, IMultivalueEqual eq);

    /**
     * @return an iterator with all values.
     */
    Iterator<IEvalValue> iterator();

    /**
     * Returns the element type of values in this <code>IMultivalue</code>.
     * @return the element type of values in this <code>IMultivalue</code>.
     */
    ValueType getType();

    Object get(int index);

    /**
     * Returns <code>true</code> if this multivalue is empty;
     * returns <code>false</code> otherwise.
     * @return <code>true</code> if this multivalue is empty;
     * returns <code>false</code> otherwise.
     */
    boolean isEmpty();

    int size();

    <T> T[] toArray(T[] array);

    public interface IMultivalueEqual {

        public boolean equal(IEvalValue x, IEvalValue y);
    }
}
