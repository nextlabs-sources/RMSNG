/*
 * Created on Jul 27, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.expressions;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IExpressionReference.java#1 $:
 */

public interface IExpressionReference extends IExpression {

    /**
     * Returns the refName.
     * @return the refName.
     */
    String getPrintableReference();

    /**
     * Returns true if the reference is by name, and false if it is by ID.
     * @return true if the reference is by name, and false if it is by ID.
     */
    boolean isReferenceByName();

    /**
     * Returns the name of the spec to which this reference points.
     * @return the name of the spec to which this reference points.
     */
    String getReferencedName();

}
