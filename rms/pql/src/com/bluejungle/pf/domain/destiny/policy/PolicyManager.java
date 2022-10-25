package com.bluejungle.pf.domain.destiny.policy;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.IComponentManager;
import com.bluejungle.framework.comp.IHasComponentInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Copyright Blue Jungle, Inc.

/**
 * Implements crud and caching operations for policy objects.
 *
 * @author Sasha Vladimirov
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/destiny/policy/PolicyManager.java#1 $
 */

public class PolicyManager implements IHasComponentInfo<PolicyManager>, IDPolicyManager {

    private static final String CLASSNAME = PolicyManager.class.getName();
    private static final Map actionMap = new HashMap();

    private final Set allPolicies = new HashSet();
    private IComponentManager manager;

    public ComponentInfo<PolicyManager> getComponentInfo() {
        return COMP_INFO;
    }

    /**
     * Creates a new named policy.
     *
     * @requires name is not null.
     * @param name the name of the new policy.
     */
    public IDPolicy newPolicy(Long id, String name) {
        return new Policy(id, name);
    }

    /**
     * Deletes an policy from persistent storage.
     *
     * @param id
     *            id of the policy to be deleted.
     */
    public void deletePolicy(Long id) {
    }

    /**
     * Updates an policy to persistent storage.
     *
     * @requires deploymentSpec is not null.
     * @param deploymentSpec
     *            policy to be updated.
     */
    public void updatePolicy(IDPolicy policy) {
    }

    /**
     * Returns a policy object given the policy name.
     *
     * @requires name is not null.
     * @param name
     *            name for this policy.
     */
    public IDPolicy getPolicy(String name) {
        return null;
    }

    /**
     * Returns an array of all known policys.
     */
    public void setPolicies(IDPolicy[] policies) {
        allPolicies.clear();
        for (int i = 0; i < policies.length; i++) {
            allPolicies.add(policies[i]);
        }
    } // all policies

    public IDPolicy createPolicy(Long id, String name) {
        Policy rv = new Policy(id, name);
        allPolicies.add(rv);
        return rv;
    }

    /**
     * @see com.bluejungle.pf.domain.destiny.policy.IDPolicyManager#getPolicies(java.util.Set)
     */
    public Set getPolicies(Collection ids) {
        Set rv = new HashSet();

        for (Iterator iter1 = ids.iterator(); iter1.hasNext();) {
            long id = ((Long)iter1.next()).longValue();
            for (Iterator iter2 = allPolicies.iterator(); iter2.hasNext();) {
                IDPolicy policy = ((IDPolicy)iter2.next());
                long pid = policy.getId().longValue();
                if (pid == id) {
                    rv.add(policy);
                    break;
                }
            }
        }

        return rv;
    }

}
