package com.bluejungle.framework.utils;

/**
 * provide helper methods for java.lang.Object
 * @author hchan
 *
 */
public final class ObjectHelper {

    private ObjectHelper() {
    }

    /**
     * Checks two objects for equality in a null-safe way.
     * @param a the first object.
     * @param b the second object.
     * @return true if the objects are equal, false otherwise.
     */
    public static boolean nullSafeEquals(Object a, Object b) {
        // copy from DevelopmentEntity

        if (a == null && b == null) {
            return true;
        }
        if (a != null && b != null) {
            return a.equals(b);
        } else {
            return false;
        }
    }

    /**
     * Obtains object's hashCode in a null-safe way.
     * @param o the object for which to obtain the hash code.
     * @return the hash code of the object, or 0 if the object is null.
     */
    public static int nullSafeHashCode(Object... objects) {
        // copy from DevelopmentEntity

        int hashCode = 0;
        if (objects != null) {
            for (Object obj : objects) {
                // I think the value should multiple by prime before adding
                hashCode += obj != null ? obj.hashCode() : 0;
            }
        }
        return hashCode;
    }

}
