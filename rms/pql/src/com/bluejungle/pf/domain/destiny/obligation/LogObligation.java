package com.bluejungle.pf.domain.destiny.obligation;

// Copyright Blue Jungle, Inc.

/*
 * Implementation of LogObligation for the server-side, where execution is
 * a nop (for now).
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/obligation/LogObligation.java#1 $
 */

public class LogObligation extends DObligation {

    private static final long serialVersionUID = 1L;

    public static final String CLASSNAME = LogObligation.class.getName();

    public static final String OBLIGATION_NAME = "log";

    protected LogObligation() {
        super();
    }

    /**
     * @returns string name for the obligation
     */
    public String getType() {
        return OBLIGATION_NAME;
    }

    /**
     * @see IDObligation#toPQL()
     */
    public String toPQL() {
        return OBLIGATION_NAME;
    }
}
