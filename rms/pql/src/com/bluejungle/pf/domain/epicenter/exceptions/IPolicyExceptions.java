/*
 * Created on Feb 27, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/exceptions/IPolicyExceptions.java#1 $:
 */

package com.bluejungle.pf.domain.epicenter.exceptions;

import java.util.List;

public interface IPolicyExceptions {

    /**
     * get the combining algorithm for these exceptions
     * @return the combining algorithm
     */
    ICombiningAlgorithm getCombiningAlgorithm();

    /**
     * set the combining algorithm for these exceptions
     * @param combiningAlgorithm the combining algorithm
     */
    void setCombiningAlgorithm(ICombiningAlgorithm combiningAlgorithm);

    /**
     * get all the policy exceptions
     * @return the policy exceptions as a list of references
     */
    List<IPolicyReference> getPolicies();

    /**
     * add a new exception as a reference
     * @param exception the policy exception as a reference
     */
    void addPolicy(IPolicyReference exception);

    /**
     * set the policy exceptions
     * @param exceptions a list of references
     */
    void setPolicies(List<IPolicyReference> exceptions);
}
