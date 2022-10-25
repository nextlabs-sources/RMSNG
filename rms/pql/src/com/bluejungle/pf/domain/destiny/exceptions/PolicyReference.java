/*
 * Created on Feb 25, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/exceptions/PolicyReference.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.exceptions;

import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyReference;

public class PolicyReference implements IPolicyReference {

    private String referencedName = null;

    public PolicyReference(String referencedName) {
        this.referencedName = referencedName;
    }

    /**
     * @see IPolicyReference#getReferencedname();
     */
    public String getReferencedName() {
        return referencedName;
    }
}
