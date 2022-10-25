/*
 * Created on Feb 27, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/exceptions/IPolicyReference.java#1 $:
 */

package com.bluejungle.pf.domain.epicenter.exceptions;

public interface IPolicyReference {

    /**
     * Returns the name of the policy to which this reference points
     */
    String getReferencedName();
}
