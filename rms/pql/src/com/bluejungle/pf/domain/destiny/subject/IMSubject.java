/*
 * Created on May 15, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IMSubject.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.expressions.IEvalValue;

public interface IMSubject extends IDSubject {

    /**
     * Set an attribute in the subject
     * @param name the name of the dynamic attribute
     * @param value the value to set
     */
    public void setAttribute(String name, IEvalValue value);
}
