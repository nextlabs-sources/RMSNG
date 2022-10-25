package com.bluejungle.framework.utils;

/*
 * Created Jan 25, 2007
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by NextLabs Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

/**
 * Generic pair container class.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/utils/Pair.java#1 $
 */
public class Pair<X, Y> implements IPair<X, Y> {

    private static final long serialVersionUID = 1L;

    private X firstElement;
    private Y secondElement;

    /**
     * Create a new pair containing x and y
     * @param x first element in the pair
     * @param y second element in the pair
     */
    public Pair(X x, Y y) {
        firstElement = x;
        secondElement = y;
    }

    /**
     * @see IPair#first()
     */
    public X first() {
        return firstElement;
    }

    /**
     * @see IPair#second()
     */
    public Y second() {
        return secondElement;
    }

    /**
     * Alternate interfaces for lispniks
     */
    public X car() {
        return first();
    }

    public Y cdr() {
        return second();
    }

    public void setFirst(X x) {
        this.firstElement = x;
    }

    public void setSecond(Y y) {
        this.secondElement = y;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other instanceof Pair) {
            return equals((Pair<X, Y>)other);
        } else {
            return false;
        }
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int res = 0;
        if (firstElement != null) {
            res = firstElement.hashCode();
        }
        if (secondElement != null) {
            res &= secondElement.hashCode();
        }
        return res;
    }

    /**
     * Compares this <code>Pair<X,Y></code> for equality to another one.
     * @param p the other <code>Pair<X,Y></code>.
     * @return true if two pairs are equal; false otherwise.
     */
    public boolean equals(Pair<X, Y> p) {
        return equals(firstElement, p.firstElement) && equals(secondElement, p.secondElement);
    }

    /**
     * Null-safe comparator for two items of the same type.
     * @param <T> the type of the objects to compare.
     * @param a the first object.
     * @param b the second object.
     * @return true if both objects are <code>null</code>
     * or if the objects are equal to each other.
     */
    private static <T> boolean equals(T a, T b) {
        if (a != null && b != null) {
            return a.equals(b);
        } else {
            return a == b;
        }
    }

}
