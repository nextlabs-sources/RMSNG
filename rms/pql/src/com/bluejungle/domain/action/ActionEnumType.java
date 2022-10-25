/*
 * Created on Mar 7, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.domain.action;

import com.bluejungle.framework.patterns.EnumBase;

import java.util.Set;

/**
 * This is the enumeration class for the action type.
 *
 * @author ihanen
 * @version $Id:
 *          //depot/main/alpha_demo/Destiny/main/src/common/domain/src/java/main/com/bluejungle/domain/action/ActionEnumType.java#1 $
 */

public class ActionEnumType extends EnumBase {

    public static final ActionEnumType ACTION_CHANGE_ATTRIBUTES = new ActionEnumType("CHANGE_ATTRIBUTES");
    public static final ActionEnumType ACTION_CHANGE_SECURITY = new ActionEnumType("CHANGE_SECURITY");
    public static final ActionEnumType ACTION_COPY = new ActionEnumType("COPY");
    public static final ActionEnumType ACTION_PASTE = new ActionEnumType("PASTE");
    public static final ActionEnumType ACTION_DELETE = new ActionEnumType("DELETE");
    public static final ActionEnumType ACTION_EMBED = new ActionEnumType("EMBED");
    public static final ActionEnumType ACTION_MOVE = new ActionEnumType("MOVE");
    public static final ActionEnumType ACTION_PRINT = new ActionEnumType("PRINT");
    public static final ActionEnumType ACTION_OPEN = new ActionEnumType("OPEN");
    public static final ActionEnumType ACTION_EDIT = new ActionEnumType("EDIT");
    public static final ActionEnumType ACTION_RENAME = new ActionEnumType("RENAME");
    public static final ActionEnumType ACTION_SEND_EMAIL = new ActionEnumType("EMAIL");
    public static final ActionEnumType ACTION_SEND_IM = new ActionEnumType("IM");
    public static final ActionEnumType ACTION_START_AGENT = new ActionEnumType("START_AGENT");
    public static final ActionEnumType ACTION_STOP_AGENT = new ActionEnumType("STOP_AGENT");
    public static final ActionEnumType ACTION_AGENT_USER_LOGIN = new ActionEnumType("AGENT_USER_LOGIN");
    public static final ActionEnumType ACTION_AGENT_USER_LOGOUT = new ActionEnumType("AGENT_USER_LOGOUT");
    public static final ActionEnumType ACTION_ACCESS_AGENT_CONFIG = new ActionEnumType("ACCESS_AGENT_CONFIG");
    public static final ActionEnumType ACTION_ACCESS_AGENT_LOGS = new ActionEnumType("ACCESS_AGENT_LOGS");
    public static final ActionEnumType ACTION_ACCESS_AGENT_BINARIES = new ActionEnumType("ACCESS_AGENT_BINARIES");
    public static final ActionEnumType ACTION_ACCESS_AGENT_BUNDLE = new ActionEnumType("ACCESS_AGENT_BUNDLE");
    public static final ActionEnumType ACTION_ABNORMAL_AGENT_SHUTDOWN = new ActionEnumType("ABNORMAL_AGENT_SHUTDOWN");
    public static final ActionEnumType ACTION_INVALID_BUNDLE = new ActionEnumType("INVALID_BUNDLE");
    public static final ActionEnumType ACTION_BUNDLE_RECEIVED = new ActionEnumType("BUNDLE_RECEIVED");
    public static final ActionEnumType ACTION_EXPORT = new ActionEnumType("EXPORT");
    public static final ActionEnumType ACTION_ATTACH = new ActionEnumType("ATTACH");
    public static final ActionEnumType ACTION_RUN = new ActionEnumType("RUN");
    public static final ActionEnumType ACTION_AVD = new ActionEnumType("AVDCALL");
    public static final ActionEnumType ACTION_MEETING = new ActionEnumType("MEETING");
    public static final ActionEnumType ACTION_PRESENCE = new ActionEnumType("PRESENCE");
    public static final ActionEnumType ACTION_SHARE = new ActionEnumType("SHARE");
    public static final ActionEnumType ACTION_RECORD = new ActionEnumType("RECORD");
    public static final ActionEnumType ACTION_QUESTION = new ActionEnumType("QUESTION");
    public static final ActionEnumType ACTION_VOICE = new ActionEnumType("VOICE");
    public static final ActionEnumType ACTION_VIDEO = new ActionEnumType("VIDEO");
    public static final ActionEnumType ACTION_JOIN = new ActionEnumType("JOIN");

    /**
     * The constructor is private to prevent unwanted instantiations from the
     * outside.
     *
     * @param name
     *            is passed through to the constructor of the superclass.
     */
    private ActionEnumType(String name) {
        super(name);
    }

    /**
     * Retrieve an ActionEnumType instance by name
     *
     * @param name
     *            the name of the ActionEnumType
     * @return the ActionEnumType associated with the provided name
     * @throws IllegalArgumentException
     *             if no ActionEnumType exists with the specified name
     */
    public static ActionEnumType getActionEnum(final String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null.");
        }

        synchronized (ActionEnumType.class) {
            if (!existsAction(name)) {
                ActionEnumType dummy = new ActionEnumType(name);
            }
        }

        return getElement(name, ActionEnumType.class);
    }

    private static boolean existsAction(String name) {
        return existsElement(name, ActionEnumType.class);
    }

    /**
     * Retrieve an ActionEnumType instance by its int type
     *
     * @param type the int type of the ActionEnumType
     * @return the ActionEnumType associated with the provided int type
     */
    public static ActionEnumType getActionEnum(int type) {
        return getElement(type, ActionEnumType.class);
    }

    /**
     * Returns all the ActionEnumType enums
     *
     * @return set of enums
     */
    public static Set<ActionEnumType> elements() {
        return EnumBase.elements(ActionEnumType.class);
    }

    /**
     *
     * @return the number of elements in this enumeration
     */
    public static int numElements() {
        return numElements(ActionEnumType.class);
    }
}
