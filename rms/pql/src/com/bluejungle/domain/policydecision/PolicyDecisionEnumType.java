/*
 * Created on Mar 10, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.domain.policydecision;

import com.bluejungle.framework.patterns.EnumBase;

/**
 * This is the policy decision enum type class. This class defines the policy
 * decisions that can be used.
 *
 * @author ihanen
 * @version $Id:
 *          //depot/main/Destiny/main/src/common/domain/src/java/main/com/bluejungle/domain/policydecision/PolicyDecisionEnumType.java#1 $
 */

public class PolicyDecisionEnumType extends EnumBase {

    public static final PolicyDecisionEnumType POLICY_DECISION_ALLOW = new PolicyDecisionEnumType("allow");
    public static final PolicyDecisionEnumType POLICY_DECISION_DENY = new PolicyDecisionEnumType("deny");

    /**
     * Constructor
     *
     * @param name
     */
    public PolicyDecisionEnumType(String name) {
        super(name);
    }

    /**
     * Retrieve an PolicyDecisionEnum instance by name
     *
     * @param name
     *            the name of the PolicyDecisionEnum
     * @return the PolicyDecisionEnum associated with the provided name
     * @throws IllegalArgumentException
     *             if no PolicyDecisionEnum exists with the specified name
     */
    public static PolicyDecisionEnumType getPolicyDecisionEnum(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null.");
        }

        return getElement(name, PolicyDecisionEnumType.class);
    }

    /**
     * Retrieve a PolicyDecisionEnum instance by enumType
     * @param type enumType of the PolicyDecisionEnum
     * @return the PolicyDecisionEnum associated with the provided enumType
     */
    public static PolicyDecisionEnumType getPolicyDecisionEnum(int type) {
        return getElement(type, PolicyDecisionEnumType.class);
    }

    /**
     *
     * @return number of elements in this enumeration
     */
    public static int numElements() {
        return numElements(PolicyDecisionEnumType.class);
    }
}
