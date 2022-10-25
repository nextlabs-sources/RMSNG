package com.bluejungle.pf.domain.destiny.misc;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.PredicateConstants;

import java.io.Serializable;

// Copyright Blue Jungle, Inc.

/*
 * @author Sasha Vladimirov
 *
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/destiny/misc/Target.java#2 $
 */

public class Target implements IDTarget, Serializable {

    private static final long serialVersionUID = 1L;

    private IPredicate action;
    private IPredicate fromResource;
    private IPredicate toResource;
    private IPredicate subject;
    private IPredicate toSubject;

    public static final Target EMPTY_TARGET = new Target(
            PredicateConstants.FALSE
            , null
            , PredicateConstants.FALSE
            , PredicateConstants.FALSE
            , null
            );

    /**
     * Returns the from resource Predicate.
     *
     */
    public IPredicate getFromResourcePred() {
        return fromResource;
    }

    /**
     * Sets from resource.
     *
     * @param fromResourcePredicateource predicate.
     */

    public void setFromResourcePred(IPredicate fromResource) {
        this.fromResource = fromResource;
    }

    /**
     * @return the to resource spec.
     */
    public IPredicate getToResourcePred() {
        return toResource;
    }

    /**
     * Sets to resource spec.
     *
     * @param toResourceSpec
     *            to resource predicate.
     */

    public void setToResourcePred(IPredicate toResource) {
        this.toResource = toResource;
    }

    /**
     * Returns the from subject spec.
     *
     */
    public IPredicate getSubjectPred() {
        return subject;
    }

    /**
     * Sets the from subject.
     *
     * @param subjectSpec
     *            subject spec.
     */
    public void setSubjectPred(IPredicate subjectSpec) {
        this.subject = subjectSpec;
    }

    /**
     * Returns the to subject spec.
     *
     */
    public IPredicate getToSubjectPred() {
        return toSubject;
    }

    /**
     * Sets the to subject.
     *
     * @param subjectSpec
     *            subject spec.
     */
    public void setToSubjectPred(IPredicate toSubject) {
        this.toSubject = toSubject;
    }

    /**
     * Returns the action spec for the rule.
     *
     */
    public IPredicate getActionPred() {
        return action;
    }

    /**
     * Sets the action.
     *
     * @param actionSpec
     *            action spec.
     */
    public void setActionPred(IPredicate actionSpec) {
        this.action = actionSpec;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer("for ");
        rv.append(fromResource);
        rv.append(" on ");
        rv.append(action);
        if (toResource != null) {
            rv.append(" to ");
            rv.append(toResource);
        }
        if (toSubject != null) {
            rv.append(" to_subject ");
            rv.append(toSubject);
        }
        rv.append(" by ");
        rv.append(subject);

        return rv.toString();
    }

    private Target(
        IPredicate fromResource
        , IPredicate toResource
        , IPredicate action
        , IPredicate subject
        , IPredicate toSubject) {
        this.action = action;
        this.fromResource = fromResource;
        this.toResource = toResource;
        this.subject = subject;
        this.toSubject = toSubject;
    }

    public static Target forFileAction(
        IPredicate fromResource
        , IPredicate action
        , IPredicate subject) {
        return new Target(fromResource, null, action, subject, null);
    }

    public static Target forFileActionWithTo(
        IPredicate fromResource
        , IPredicate toResource
        , IPredicate action
        , IPredicate subject) {
        return new Target(fromResource, toResource, action, subject, null);
    }

    public static Target forEmailAction(
        IPredicate fromResource
        , IPredicate action
        , IPredicate subject
        , IPredicate toSubject) {
        return new Target(fromResource, null, action, subject, toSubject);
    }

    public Target() {
        super();
    }
}
