/*
 * Created on Mar 29, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.domain.agenttype;

import com.bluejungle.framework.patterns.EnumBase;

/**
 * Enumeration for the type of Agents which exist in Destiny
 *
 * @author sasha
 * @version $Id:
 *          //depot/main/Destiny/main/src/common/domain/src/java/main/com/bluejungle/domain/agenttype/AgentTypeEnumType.java#1 $:
 */

public class AgentTypeEnumType extends EnumBase {

    /**
     * The Desktop Agent type
     */
    public static final AgentTypeEnumType DESKTOP = new AgentTypeEnumType("DESKTOP");

    /**
     * The File Server Agent type
     */
    public static final AgentTypeEnumType FILE_SERVER = new AgentTypeEnumType("FILE_SERVER");

    /**
     * The Portal (e.g. SharePoint) Agent type
     */
    public static final AgentTypeEnumType PORTAL = new AgentTypeEnumType("PORTAL");

    /**
     * The Active Directory Agent type
     */
    public static final AgentTypeEnumType ACTIVE_DIRECTORY = new AgentTypeEnumType("ACTIVE_DIRECTORY");

    private AgentTypeEnumType(String name) {
        super(name, AgentTypeEnumType.class);
    }

    /**
     * Returns the AgentTypeEnumType by name
     *
     * @param name
     *            name to retrieve
     * @return the AgentTypeEnumType by name
     */
    public static AgentTypeEnumType getAgentType(String name) {
        return getElement(name, AgentTypeEnumType.class);
    }

    /**
     * Returns whether a given agent type name exists
     *
     * @param name
     *            name to test
     * @return whether a given agent type name exists
     */
    public static boolean existsAgentType(String name) {
        return existsElement(name, AgentTypeEnumType.class);
    }
}
