/*
 * Created on May 6, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.deployment;

import com.bluejungle.domain.agenttype.AgentTypeEnumType;
import com.bluejungle.framework.expressions.IArguments;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/deployment/AgentRequest.java#1 $:
 */

public class AgentRequest implements IArguments {

    protected long agentId;
    protected AgentTypeEnumType agentType;

    /**
     * Constructor
     *
     */
    public AgentRequest(long agentId, AgentTypeEnumType agentType) {
        super();
        this.agentId = agentId;
        this.agentType = agentType;
    }

    /**
     * Returns the agentId.
     * @return the agentId.
     */
    public long getAgentId() {
        return agentId;
    }

    /**
     * Returns the agentType.
     * @return the agentType.
     */
    public AgentTypeEnumType getAgentType() {
        return agentType;
    }

}
