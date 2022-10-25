/*
 * Created on Dec 14, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.epicenter.evaluation;

/**
 * IEvaluatableLeaf represents a leaf node within a graph of evaluatable objects.
 * As such, it has an ability to evaluate itself.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/evaluation/IEvaluatableLeaf.java#1 $:
 */

public interface IEvaluatableLeaf extends IEvaluatableNode {

    /**
     * evaluates this node to determine whether or not
     * the specified action should be allowed. This method is used for both types of
     * operations, those that operate on only one resource, such as delete, and those that
     * operate source and a destination, such as copy.
     *
     * @requires fromResource, action, user, host, app is not null
     * @param fromResource resource being acted upon, in case of a single-resource action
     * or source resource being acted upon in case of a two-resource action, such as a file
     * that is being copied
     * @param toResource destination resource, such as a destination directory for a copy action
     * null means a single-resource action is being performed
     * @param action action being performed on the resource
     * @param user user performin the action
     * @param host host on which the action is being performed
     * @param app application performing the action
     * @return result of the evaluation
     */

    /* this method may not be necessary
    InternalEvaluationResult evaluate(IResource fromResource, IResource toResource, IAction action, ISubject user, ISubject host, ISubject app);
    */

    /**
     *
     * @return true if this leaf has an "otherwise" clause
     */
    public boolean hasOtherwise();

}
