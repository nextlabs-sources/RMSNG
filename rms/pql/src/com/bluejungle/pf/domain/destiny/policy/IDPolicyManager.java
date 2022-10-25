package com.bluejungle.pf.domain.destiny.policy;

/*
 * Created on Jan 25, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;

import java.util.Collection;
import java.util.Set;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/policy/IDPolicyManager.java#1 $:
 */
@SuppressWarnings("deprecation")
public interface IDPolicyManager {

    String CLASSNAME = IDPolicyManager.class.getName();

    ComponentInfo<PolicyManager> COMP_INFO =
            new ComponentInfo<PolicyManager>(CLASSNAME, PolicyManager.class.getName(), CLASSNAME, LifestyleType.SINGLETON_TYPE);

    /**
     * Creates a new named policy. This object could later be persisted with a
     * call to updatePolicy.
     *
     * @requires name is not null.
     * @param name
     *            name of the new policy.
     */
    public IDPolicy newPolicy(Long id, String name);

    /**
     * Deletes an policy from persistent storage.
     *
     * @param id
     *            id of the policy to be deleted.
     */
    public void deletePolicy(Long id);

    /**
     * Updates an policy to persistent storage.
     *
     * @requires deploymentSpec is not null.
     * @param deploymentSpec
     *            policy to be updated.
     */
    public void updatePolicy(IDPolicy policy);

    /**
     * Returns a policy object given the policy name.
     *
     * @requires name is not null.
     * @param name
     *            name for this policy.
     */
    public IDPolicy getPolicy(String name);

    /**
     * Returns all the policies with given ids
     * @param ids policy ids
     * @return a set of policies
     */
    public Set getPolicies(Collection ids);

    /**
     *
     */
    public void setPolicies(IDPolicy[] policies);
}
