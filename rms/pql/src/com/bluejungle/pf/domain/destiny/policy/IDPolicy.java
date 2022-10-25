/*
 * Created on Dec 28, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.policy;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.destiny.misc.IDTarget;
import com.bluejungle.pf.domain.epicenter.policy.IPolicy;

/**
 * Destiny policy
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/policy/IDPolicy.java#1 $:
 */

public interface IDPolicy extends IPolicy {

    IDTarget getEvaluationTarget();

    DevelopmentStatus getStatus();

    void setStatus(DevelopmentStatus status);

    /**
     * Deployment target is a predicate that may evalauate to true
     * or false for any given host/agent combination.
     *
     * @return deployment target of this policy, which may be null
     * if policy is to be deployed everywhere
     */
    IPredicate getDeploymentTarget();

    /**
     * Sets the deployment target for this policy, overriding the
     * current deployment target, if any.
     *
     * @param dt deployment target
     */
    void setDeploymentTarget(IPredicate dt);

    boolean isHidden();

}
