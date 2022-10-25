/*
 * Created on May 9, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

/**
 * IAttribute represents a named attribute of any object
 * with attributes.  For example, the system may be dealing with objects
 * whose logical type (not Java type) is called "order", and objects of that
 * type have an attribute called "total".
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IAttribute.java#1 $:
 */

public interface IAttribute extends IExpression {

    /**
     * @return The name of this attribute
     */
    String getName();

    /**
     * @return The name of the type of object this attribute is for.
     */
    String getObjectTypeName();

    /**
     * @return The name of the subtype of object this attribute is for.
     */
    String getObjectSubTypeName();
}
