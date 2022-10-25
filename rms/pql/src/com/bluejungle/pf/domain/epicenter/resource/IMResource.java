package com.bluejungle.pf.domain.epicenter.resource;

import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.ValueType;

import java.util.Collection;
import java.util.Date;

/*
 * All sources, binaries and HTML pages (C) Copyright 2007 by Blue Jungle Inc,
 * San Mateo, CA. Ownership remains with Blue Jungle Inc. All rights reserved
 * worldwide.
 */

/**
 * This interface defines the contract for modifiable resources,
 * which are different from the "regular" resources in that you can
 * modify their attributes.
 *
 * @author sergey
 */
public interface IMResource extends IResource {

    /**
     * Sets the value of an attribute to the specified
     * <code>IEvalValue</code> value.
     * @param name the name of the attribute to set.
     * @param val the new value of the attribute.
     */
    void setAttribute(String name, IEvalValue val);

    /**
     * Sets the value of an attribute to the specified
     * <code>String</code> value.
     * @param name the name of the attribute to set.
     * @param val the new value of the attribute.
     */
    void setAttribute(String name, String val);

    /**
     * Sets the value of an attribute to the specified
     * <code>long</code> value.
     * @param name the name of the attribute to set.
     * @param val the new value of the attribute.
     */
    void setAttribute(String name, long val);

    /**
     * Sets the value of an attribute to the specified
     * <code>Date</code> value.
     * @param name the name of the attribute to set.
     * @param val the new value of the attribute.
     */
    void setAttribute(String name, Date val);

    /**
     * Sets the value of an attribute to an <code>IMultivalue</code>
     * composed of the elements of the vals <code>Collection</code>.
     * @param name the name of the attribute to set.
     * @param vals the <code>Collection</code> from which to construct
     * an <code>IMultivalue</code>.
     */
    <T> void setAttribute(String name, Collection<T> vals);

    /**
     * Sets the value of an attribute to an <code>IMultivalue</code>
     * composed of the elements of the vals <code>Collection</code>,
     * using the specified type.
     * @param name the name of the attribute to set.
     * @param vals the <code>Collection</code> from which to construct
     * an <code>IMultivalue</code>.
     * @param type the <code>ValueType</code> of the <code>IMultivalue</code>
     * to create.
     */
    <T> void setAttribute(String name, Collection<T> vals, ValueType type);

    /**
     * Clears the specified attribute.
     * @param name the name of the attribute to clear.
     */
    void clearAttribute(String name);

    /**
     * Set the process token the resource should use in the lazy attribute loader
     * @ param processToken the process token (supplied by SDK)
     */
    void setProcessToken(Long processToken);
}
