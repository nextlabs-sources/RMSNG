/*
 * Created on Feb 27, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/exceptions/PolicyExceptions.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.exceptions;

import com.bluejungle.pf.domain.epicenter.exceptions.ICombiningAlgorithm;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyExceptions;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyReference;

import java.util.ArrayList;
import java.util.List;

public class PolicyExceptions implements IPolicyExceptions {

    private ICombiningAlgorithm combiningAlgorithm = CombiningAlgorithm.ALLOW_OVERRIDES;
    private List<IPolicyReference> exceptions = new ArrayList<IPolicyReference>();

    public PolicyExceptions() {
    }

    public PolicyExceptions(List<IPolicyReference> exceptions, ICombiningAlgorithm combiningAlgorithm) {
        this.exceptions = exceptions;
        this.combiningAlgorithm = combiningAlgorithm;
    }

    public void setCombiningAlgorithm(ICombiningAlgorithm combiningAlgorithm) {
        this.combiningAlgorithm = combiningAlgorithm;
    }

    public ICombiningAlgorithm getCombiningAlgorithm() {
        return combiningAlgorithm;
    }

    public void setPolicies(List<IPolicyReference> exceptions) {
        this.exceptions = exceptions;
    }

    public void addPolicy(IPolicyReference exception) {
        exceptions.add(exception);
    }

    public List<IPolicyReference> getPolicies() {
        return exceptions;
    }
}
