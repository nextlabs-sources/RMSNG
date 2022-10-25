/*
 * Created on Dec 21, 2006
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

package com.bluejungle.pf.domain.destiny.obligation;

import com.bluejungle.domain.log.PolicyActivityInfoV5;
import com.bluejungle.framework.utils.StringUtils;
import com.bluejungle.pf.engine.destiny.EvaluationResult;

/**
 * Instances of this class represent display obligations.
 *
 * @author sergey
 */
public class DisplayObligation extends DObligation {

    private static final long serialVersionUID = 1L;

    private String message;

    public static final String OBLIGATION_NAME = "display";

    public DisplayObligation(String message) {
        this.message = message;
    }

    public String toPQL() {
        return getType() + "( \"" + StringUtils.escape(message) + "\" )";
    }

    public String getType() {
        return OBLIGATION_NAME;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isActivityAcceptable(EvaluationResult res, PolicyActivityInfoV5 args) {
        return true;
    }
}
