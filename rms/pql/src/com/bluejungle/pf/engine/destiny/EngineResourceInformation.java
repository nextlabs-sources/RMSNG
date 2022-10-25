package com.bluejungle.pf.engine.destiny;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/EngineResourceInformation.java#1 $
 */

/**
 * Instances of this resource information class are managed together
 * with instances of resources. They represent the data the engine associates
 * with a particular resource, such as policy applicability data, the data on
 * which name was matched for what policy, etc.
 *
 * @author sergey
 */
public class EngineResourceInformation implements Serializable {

    private static final long serialVersionUID = 1L;

    // policy applicability caches
    private final BitSet fromMatchingCache = new BitSet();

    private final BitSet fromNonMatchingCache = new BitSet();

    private final BitSet toMatchingCache = new BitSet();

    private final BitSet toNonMatchingCache = new BitSet();

    // Index of the matched resource among the alternates
    private final Map<Integer, Integer> fromMatchingIndex = new HashMap<Integer, Integer>();

    // Index of the matched resource among the alternates
    private final Map<Integer, Integer> toMatchingIndex = new HashMap<Integer, Integer>();

    /**
     * This method checks whether the associated resource is applicable
     * to the given policy's "from" resource predicate.
     *
     * @param policyOrdinal the ordinal of the policy within the evaluating
     * context used to access the policy.
     * @return Boolean.TRUE if policy's "from" resource predicate
     * is applicable to the associated resoure, Boolean.FALSE if it is
     * not applicable, and null if the applicability value is not in the cache.
     */
    public synchronized Boolean isApplicableFrom(int policyOrdinal) {
        if (fromMatchingCache.get(policyOrdinal)) {
            return Boolean.TRUE;
        } else if (fromNonMatchingCache.get(policyOrdinal)) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    /**
     * If the associated resource has matched the policyordinal's
     * "from" predicate in the past, this method retrieves which one
     * of the alternate resource names resulted in a matching.
     *
     * @param policyOrdinal the ordinal of the policy within the evaluating
     * context used to access the policy.
     * @return Integer value which is an index into the alternate names for this
     * resource. (Note, that the array of alternates themselves is kept in
     * the associated resource).
     */
    public synchronized Integer getFromResourceIndex(int policyOrdinal) {
        return fromMatchingIndex.get(policyOrdinal);
    }

    /**
     * This method sets the applicability of the given policy's "from"
     * resource predicate to the associated resource.
     *
     * @param policyOrdinal The ordinal of the policy.
     * @param match true indicates that the policy is applicable;
     * false indicates that it is not applicable.
     * @param index when match is true, index indicates the alternate
     * that matched among the "from" alternates.
     */
    public synchronized void setApplicableFrom(int policyOrdinal, boolean match, int index) {
        if (match) {
            fromMatchingCache.set(policyOrdinal);
            fromMatchingIndex.put(policyOrdinal, index);
        } else {
            fromNonMatchingCache.set(policyOrdinal);
        }
    }

    /**
     * This method checks whether the associated resource is applicable
     * to the given policy's "to" resource predicate.
     *
     * @param policyOrdinal the ordinal of the policy within the evaluating
     * context used to access the policy.
     * @return Boolean.TRUE if policy's "to" resource predicate
     * is applicable to the associated resoure, Boolean.FALSE if it is
     * not applicable, and null if the applicability value is not in the cache.
     */
    public synchronized Boolean isApplicableTo(int policyOrdinal) {
        if (toMatchingCache.get(policyOrdinal)) {
            return Boolean.TRUE;
        }
        if (toNonMatchingCache.get(policyOrdinal)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * If the associated resource has matched the policyordinal's
     * "to" predicate in the past, this method retrieves which one
     * of the alternate resource names resulted in a matching.
     *
     * @param policyOrdinal the ordinal of the policy within the evaluating
     * context used to access the policy.
     * @return Integer value which is an index into the alternate names for this
     * resource. (Note, that the array of alternates themselves is kept in
     * the associated resource).
     */
    public synchronized Integer getToResourceIndex(int policyOrdinal) {
        return toMatchingIndex.get(policyOrdinal);
    }

    /**
     * This method sets the applicability of the given policy's "to"
     * resource predicate to the associated resource.
     *
     * @param policyOrdinal The ordinal of the policy.
     * @param match true indicates that the policy is applicable;
     * false indicates that it is not applicable.
     * @param index when match is true, index indicates the alternate
     * that matched among the "to" alternates.
     */
    public synchronized void setApplicableTo(int policyOrdinal, boolean match, int index) {
        if (match) {
            toMatchingCache.set(policyOrdinal);
            toMatchingIndex.put(policyOrdinal, index);
        } else {
            toNonMatchingCache.set(policyOrdinal);
        }
    }

    /**
     * This method retrieves a <code>BitSet</code> of all policies with the
     * "from" resource predicate <b>not</b> matching the associated resource.
     *
     * @return a <code>BitSet</code> of non-matching policies.
     * If the associated resource does not cache applicability,
     * an empty <code>BitSet</code> is returned.
     */
    public synchronized BitSet getNonMatchingFromPolicies() {
        return fromNonMatchingCache;
    }

    /**
     * This method retrieves a <code>BitSet</code> of all policies with the
     * "to" resource predicate <b>not</b> matching the associated resource.
     *
     * @return a <code>BitSet</code> of non-matching policies.
     * If the associated resource does not cache applicability,
     * an empty <code>BitSet</code> is returned.
     */
    public synchronized BitSet getNonMatchingToPolicies() {
        return toNonMatchingCache;
    }

}
