/*
 * Created on Dec 3, 2004
 * All sources, binaries and HTML pages
 * (C) copyright 2004 by Blue Jungle Inc., Redwood City CA,
 * Ownership remains with Blue Jungle Inc,
 * All rights reserved worldwide.
 */
package com.bluejungle.pf.engine.destiny;

import com.bluejungle.domain.log.PolicyActivityInfoV5;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;
import com.bluejungle.pf.domain.destiny.obligation.IDObligation;

import java.util.ArrayList;
import java.util.List;

/**
 * EvaluationResult contains a complete result of evaluating policies.  The result is
 * one of four possible types: ALLOW, DENY, QUERY, CONFIRM.  If the effectName is
 * QUERY or CONFIRM, then the result also contains EvaluationResultQuery.
 *
 * Each evaluation request is also assigned a globally-unique request id, which is
 * returned in the result.
 *
 * If the result is a DENY, then a list of reasons, one from every applicable DENY policy
 * is also provided.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/EvaluationResult.java#1 $
 */

public class EvaluationResult {

    public final static String ALLOW = IDEffectType.ALLOW_NAME;
    public final static String DENY = IDEffectType.DENY_NAME;
    public final static String DONT_CARE = IDEffectType.DONT_CARE_NAME;

    private String effectName;
    private long requestId;
    private final List<IDObligation> obligations = new ArrayList<IDObligation>();
    private EvaluationRequest req;
    private PolicyActivityInfoV5 paInfo;
    private long logUid = 0;

    /**
     * Constructor
     * @param requestId
     * @param effectName
     */
    public EvaluationResult(EvaluationRequest req, String effectName) {
        super();
        this.req = req;
        this.effectName = effectName;
    }

    /**
     * @return effectName of this evaluation result, which is either ALLOW or DENY.
     */
    public String getEffectName() {
        return effectName;
    }

    /**
     * Returns the requestId.
     * @return the requestId.
     */
    public long getRequestId() {
        return this.req.getRequestId().longValue();
    }

    public void addObligation(IDObligation obl) {
        obligations.add(obl);
    }

    public void setPAInfo(PolicyActivityInfoV5 paInfo) {
        this.paInfo = paInfo;
    }

    public PolicyActivityInfoV5 getPAInfo() {
        return paInfo;
    }

    public List<IDObligation> getObligations() {
        return obligations;
    }

    public EvaluationRequest getEvaluationRequest() {
        return req;
    }

    public void setLogUid(long logUid) {
        // Only set a uid if it hasn't already been set
        if (this.logUid == 0) {
            this.logUid = logUid;
        }
    }

    public long getLogUid() {
        return logUid;
    }
}
