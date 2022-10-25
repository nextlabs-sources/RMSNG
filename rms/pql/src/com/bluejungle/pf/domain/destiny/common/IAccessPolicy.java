package com.bluejungle.pf.domain.destiny.common;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 */

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.destiny.lifecycle.EntityType;
import com.bluejungle.pf.destiny.parser.PQLException;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.action.IAction;

import java.util.Collection;

/**
 * @author pkeni
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/IAccessPolicy.java#1 $
 */

/**
 * @author pkeni
 */
public interface IAccessPolicy {

    /**
     * Returns policies in effect as a collection.
     *
     */

    Collection<IDPolicy> getAccessControlPolicies();

    /**
     * Return all the actions this group can perform on this object.
     *
     * @return actions that can be performed by a user belonging to the group.
     * @param groupId group to query.
     *
     */

    Collection<IAction> getActionsForGroup(Long groupId);

    /**
     * Return all the actions this user can perform on this object.
     *
     * @return actions that can be performed by the user.
     * @param userId user to query for.
     *
     */

    Collection<IAction> getActionsForUser(Long userId);

    /**
     * Return all the types of objects this role can perform operations on.
     *
     * @return A set of object types on which this role can perform actions.
     * @param role role to query for.
     *
     */

    Collection<EntityType> getAllowedEntities();

    /**
     * Sets the actions this group can perform on this object.
     *
     * @return
     * @param groupId id for the group in question.
     * @param actions actions to set.
     *
     */

    void setActionsForGroup(Long groupId, Collection<? extends IAction> actions) throws PQLException;

    /**
     * Sets the actions this role can perform on this object.
     *
     * @return
     * @param roleSpec IDSpec of the role in question.
     * @param actions actions to set.
     *
     */

    void setActionsForRole(IPredicate roleSpec, Collection<? extends IAction> actions) throws PQLException;

    /**
     * Sets the actions this user can perform on this object.
     *
     * @return
     * @param user user in question.
     * @param actions actions to set.
     *
     */

    void setActionsForUser(Long userId, Collection<? extends IAction> actions) throws PQLException;

    /**
     * Returns a set of User/GroupAccess objects.  From there, caller
     * can enumerate over all the users and groups and get the actions that
     * user or group may perform.
     *
     * @return a set of <code>IRoleAccess</code> objects.
     *
     */

    Collection<IAccess> getAllUserGroupActions();

    /**
     * Sets the types of objects this role can perform operations on.
     *
     * @return
     * @param allowedEntities set of entities for which to allow this user access.
     *
     */

    void setAllowedEntities(Collection<EntityType> allowedEntities);

    /**
     * Returns true if user is allowed the action
     *
     * @param resource the object describing the owner
     * of the item to which this policy is attached.
     * @param subject an object identifying the user
     * performing the action.
     * @param action the action.
     *
     * @return true if action is allowed false otherwise.
     */

    boolean checkAccess(IPResource resource, IDSubject subject, IAction action);

    /**
     * Returns true if the role is allowed the action.  Used to
     * evaluate role's access to apps.
     *
     * @return true if action is allowed false otherwise.
     */

    boolean checkRoleAccess(IDSpec spec, IAction action);

}
