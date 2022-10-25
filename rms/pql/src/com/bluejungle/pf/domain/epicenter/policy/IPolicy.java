/*
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

package com.bluejungle.pf.domain.epicenter.policy;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.utils.IPair;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluatableLeaf;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyExceptions;
import com.bluejungle.pf.domain.epicenter.misc.IEffectType;
import com.bluejungle.pf.domain.epicenter.misc.IObligation;
import com.bluejungle.pf.domain.epicenter.misc.ITarget;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * IPolicy represents a policy.
 *
 * @author Sasha Vladimirov
 *
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/epicenter/IPolicy.java#11 $
 *
 */

public interface IPolicy extends Serializable, IPolicyObject, IEvaluatableLeaf {

    // This attribute should be used on all policies that are policy exceptions
    public static final String EXCEPTION_ATTRIBUTE = "POLICY_EXCEPTION";

    /**
     * @return the target of this policy
     */
    ITarget getTarget();

    /**
     * @param target target this policy applies to
     */
    void setTarget(ITarget target);

    /**
     * gets obligations on this policy that are associated with a particular effect type
     * @param effectType effect type, if null all obligations are returned
     * @return obligations that get executed when policy evaluates to effect
     */
    Collection<IObligation> getObligations(IEffectType effectType);

    /**
     * gets obligations on this policy that are associated with a particular effect type
     * @param effectType effect type, if null all obligations are returned
     * @return obligations that get executed when policy evaluates to effect
     */
    IObligation[] getObligationArray(IEffectType effectType);

    /**
     * resets obligations on this policy that are associated with a particular effect type
     * @param effectType effect type, if null all obligations are returned
     */
    void resetObligationArray(IEffectType effectType);

    /**
     * adds an obligation that should be executed for the specified effect
     * type
     *
     * @requires effectType is not null
     * @param obligation obligation to execute
     * @param effectType effect type to execute the obligation on
     */
    void addObligation(IObligation obligation, IEffectType effectType);

    /**
     * deletes a given obligation for a given effect type
     * @param obligation obligation to delete
     * @param effectType effect type which the obligation gets executed on
     */
    void deleteObligation(IObligation obligation, IEffectType effectType);

    /**
     * set/unset the named attribute in the policy
     * @param name the attribute name
     * @param value true to set the attribute, false to unset
     */
    void setAttribute(String name, boolean value);

    /**
     * returns true if the named attribute has been set in this policy
     * @param name the attribute name
     * @return whether the attribute exists
     */
    boolean hasAttribute(String name);

    /**
     * get all the set attributes in the policy
     * @return a Set of all attributes in this policy
     */
    Set<String> getAttributes();

    /**
     * Adds the tag name/value to the current list of tags
     * @param name the name of the tag
     * @param value the value of the tag
     */
    void addTag(String name, String value);

    /**
     * get all the tags in the policy
     * @return a map with the key being the tag name and value being the set of values
     */
    Collection<IPair<String, String>> getTags();

    /**
     * removes all of the mappings from the tag map
     */
    void clearTags();

    /**
     * Removes the mapping for the specified name and value from the list of tags, if present
     * It is not an error to remove a name/value that doesn't exist
     * @param name the name of the tag
     * @param value the value of the tag
     */
    void removeTag(String name, String value);

    /**
     * Sets the severity level of this policy.
     * @param level the severity level of this policy.
     */
    void setSeverity(int level);

    /**
     * Gets the severity level of this policy.
     * @return the severity level of this policy.
     */
    int getSeverity();

    /**
     *
     * @return effect of this policy if the request is applicable to subject,
     *  action, and resource
     */
    public IEffectType getMainEffect();

    /**
     * sets the effect of this policy when the request is  applicable to subject,
     * action, and resource
     * @param mainEffect effect
     */
    public void setMainEffect(IEffectType mainEffect);

    /**
     *
     * @return effect of this policy if the request is applicable to action and
     * resource but not subject
     */
    public IEffectType getOtherwiseEffect();

    /**
     * sets the effect of this policy when the request is applicalbe so action and
     * resource but not subject.
     *
     * @param otherwiseEffect effect
     */
    public void setOtherwiseEffect(IEffectType otherwiseEffect);

    /**
     * removes the otherwise clause of this policy
     */
    void removeOtherwise();

    /**
     * sets the conditions for this policy
     *
     * @param conditions
     */
    void setConditions(IPredicate conditions);

    /**
     * @return conditions of this policy, which may be null
     */
    IPredicate getConditions();

    void setAccessPolicy(AccessPolicy accessPolicy);

    /**
     * Set exceptions to this policy
     */
    void setPolicyExceptions(IPolicyExceptions exceptions);

    /**
     * Return exceptions to this policy
     */
    IPolicyExceptions getPolicyExceptions();
}
