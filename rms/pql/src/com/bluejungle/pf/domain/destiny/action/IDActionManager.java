/*
 * Created on Jan 25, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.action;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;

import java.util.Collection;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/action/IDActionManager.java#1 $:
 */

public interface IDActionManager {

    String CLASSNAME = IDActionManager.class.getName();

    ComponentInfo<IDActionManager> COMP_INFO =
            new ComponentInfo<IDActionManager>(CLASSNAME,
                    ActionManager.class,
                    IDActionManager.class,
                    LifestyleType.SINGLETON_TYPE);

    /**
     * Checks whether the actions spec with a given name exists
     * in the system.
     *
     * @param name name of the action spec
     * @return true if the action spec with a given name exists,
     * false otherwise
     */
    boolean existsAction(String name);

    /**
     * Checks whether the name could be a basic action or if it is
     * a component (because of dynamic adding of actions, this is
     * not certain to return the same answer as "existsAction", but
     * the general meaning is the same).
     *
     * @param name name of the action spec
     * @return true if this could be a basic action name
     */
    boolean isBasicAction(String name);

    /**
     * Returns an action object given the action name.
     *
     * @requires actionName is not null.
     * @param actionName
     *            name for this action.
     */
    IDAction getAction(String actionName);

    /**
     * Returns all actions.
     *
     */
    Collection allActions();

}
