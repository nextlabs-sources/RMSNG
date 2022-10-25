package com.bluejungle.pf.domain.destiny.action;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.IHasComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;

import java.util.Collection;

// Copyright Blue Jungle, Inc.

/**
 *
 *
 * @author Sasha Vladimirov
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/destiny/ActionManager.java#4 $
 */

public class ActionManager implements IHasComponentInfo<ActionManager>, IDActionManager {

    public static final String CLASSNAME = ActionManager.class.getName();

    private static final ComponentInfo<ActionManager> COMP_INFO =
            new ComponentInfo<ActionManager>(
                    CLASSNAME,
                    ActionManager.class,
                    null,
                    LifestyleType.SINGLETON_TYPE);

    public ComponentInfo<ActionManager> getComponentInfo() {
        return COMP_INFO;
    }

    /**
     * Deletes an action from persistent storage.
     *
     * @param id
     *            id of the action or action group to be deleted.
     */
    public void deleteActionSpec(Long id) {
    }

    /**
     * Updates an action to persistent storage.
     *
     * @requires action is not null.
     * @param action
     *            action or action group to be updated.
     */
    public void updateActionSpec(IDActionSpec action) {
    }

    /**
     * checks whether an action with the given name exists
     *
     * @param name action name
     * @return true if action with the given name exists, false otherwise
     */
    public boolean existsAction(String name) {
        return DAction.existsAction(name);
    }

    /**
     * Checks whether a name could refer to a basic action or not
     * this generally will return the same value as "existsAction", but
     * because of the dynamic behavior of DAction, this is not guaranteed.
     * Everything other than a basic action has a / in its name
     *
     * @param name action name
     * @return true if this could be a basic action name
     */
    public boolean isBasicAction(String name) {
        return (name != null && name.indexOf("/") == -1);
    }

    /**
     * Returns an action object given the action name.
     *
     * @requires actionName is not null.
     * @param actionName
     *            name for this action.
     * @throws IllegalArgumentException if action with the given name
     * doesn't exist
     */
    public IDAction getAction(String actionName) {
        IDAction ret = null;

        try {
            ret = DAction.getAction(actionName);
        } catch (IllegalArgumentException ie) {
            ret = null;
        }
        return ret;
    }

    /**
     * Returns all action objects.
     *
     */
    public Collection<DAction> allActions() {
        return DAction.allActions();
    }
}
