package com.bluejungle.pf.domain.destiny.policy;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.utils.IPair;
import com.bluejungle.framework.utils.ObjectHelper;
import com.bluejungle.framework.utils.Pair;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.destiny.exceptions.PolicyExceptions;
import com.bluejungle.pf.domain.destiny.misc.EffectType;
import com.bluejungle.pf.domain.destiny.misc.IDTarget;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluatableNode;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyExceptions;
import com.bluejungle.pf.domain.epicenter.misc.IEffectType;
import com.bluejungle.pf.domain.epicenter.misc.IObligation;
import com.bluejungle.pf.domain.epicenter.misc.ITarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Sasha Vladimirov
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/policy/Policy.java#1 $
 */

public class Policy extends PolicyObject implements IDPolicy {

    private static final long serialVersionUID = 1L;

    private ITarget target;
    private IPredicate conditions;
    private IPredicate deploymentTarget;
    private boolean hidden = false;
    private int severity = 0;

    private IEffectType mainEffect;
    private IEffectType otherwiseEffect;
    private SortedSet<String> attributes = new TreeSet<String>();
    private ArrayList<IPair<String, String>> tags = new ArrayList<IPair<String, String>>();
    private DevelopmentStatus status = DevelopmentStatus.NEW;

    private Collection<IObligation>[] obligations;

    private IPolicyExceptions policyExceptions = new PolicyExceptions();

    Policy(Long id, String name) {
        super(id, name);

        // types are 1-based, so array has an empty element at index 0
        obligations = new Collection[EffectType.numElements() + 1];
        for (Iterator iter = EffectType.elements().iterator(); iter.hasNext();) {
            EffectType type = (EffectType)iter.next();
            obligations[type.getType()] = new ArrayList<IObligation>();
        }
    }

    /**
     * Returns the rules for this policy as an array of evaluatable nodes.
     *
     */

    public IEvaluatableNode[] getChildren() {
        return new IEvaluatableNode[0];
    }

    /**
     * returns name, which is the string representation for this object.
     */
    public String toString() {
        return "POLICY " + getName() + " ID " + getId();
    }

    public boolean isLeafNode() {
        return true;
    }

    /**
     * @see IPolicy#getTarget()
     */
    public ITarget getTarget() {
        return target;
    }

    /**
     * Returns the deploymentTarget.
     * @return the deploymentTarget.
     */
    public IPredicate getDeploymentTarget() {
        return this.deploymentTarget;
    }

    /**
     * Sets the deploymentTarget
     * @param deploymentTarget The deploymentTarget to set.
     */
    public void setDeploymentTarget(IPredicate deploymentTarget) {
        this.deploymentTarget = deploymentTarget;
    }

    /**
     * @see IPolicy#setTarget(ITarget)
     */
    public void setTarget(ITarget target) {
        this.target = target;
    }

    /**
     * @see IPolicy#getObligationArray(IEffectType)
     */
    public IObligation[] getObligationArray(IEffectType effectType) {
        Collection<IObligation> returnSet = getObligations(effectType);
        return returnSet.toArray(new IObligation[returnSet.size()]);
    }

    public Collection<IObligation> getObligations(IEffectType effectType) {
        return obligations[effectType.getType()];
    }

    /**
     * @see IPolicy#getObligationArray(IEffect)
     */
    public void resetObligationArray(IEffectType effectType) {
        obligations[effectType.getType()] = new ArrayList<IObligation>();
    }

    /**
     * @see IPolicy#addObligation(IObligation, IEffectType)
     */
    public void addObligation(IObligation obligation, IEffectType effectType) {
        obligations[effectType.getType()].add(obligation);
        obligation.setPolicy(this);
    }

    /**
     * @see IPolicy#deleteObligation(IObligation, IEffectType)
     */
    public void deleteObligation(IObligation obligation, IEffectType effectType) {
        obligations[effectType.getType()].remove(obligation);
        obligation.removePolicy();
    }

    public IEffectType getMainEffect() {
        return this.mainEffect;
    }

    public void setMainEffect(IEffectType mainEffect) {
        this.mainEffect = mainEffect;
    }

    public IEffectType getOtherwiseEffect() {
        return this.otherwiseEffect;
    }

    public void setOtherwiseEffect(IEffectType otherwiseEffect) {
        this.otherwiseEffect = otherwiseEffect;
    }

    public boolean hasOtherwise() {
        return (otherwiseEffect != null);
    }

    /**
     * @see IPolicy#removeOtherwise()
     */
    public void removeOtherwise() {
        otherwiseEffect = null;
    }

    /**
     * @see IDPolicy#getEvaluationTarget()
     */
    public IDTarget getEvaluationTarget() {
        // we know it's a IDTarget
        return (IDTarget)target;
    }

    /**
     * Returns the conditions.
     * @return the conditions.
     */
    public IPredicate getConditions() {
        return this.conditions;
    }

    /**
     * Sets the conditions
     * @param conditions The conditions to set.
     */
    public void setConditions(IPredicate conditions) {
        this.conditions = conditions;
    }

    /**
     * @see IDPolicy#getStatus()
     */
    public DevelopmentStatus getStatus() {
        return status;
    }

    /**
     * @see IDPolicy#setStatus(DevelopmentStatus)
     */
    public void setStatus(DevelopmentStatus status) {
        this.status = status;
    }

    /**
     * @see IPolicy#setSeverity(int)
     */
    public void setSeverity(int level) {
        severity = level;
    }

    /**
     * @see IPolicy#getSeverity()
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * @param effectTypeNum
     * @return
     */
    public Collection<IObligation> getObligations(int effectTypeNum) {
        return obligations[effectTypeNum];
    }

    /**
     * @see IDPolicy#isHidden()
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden indicates that the policy is hidden.
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @see IPolicy#getAttributes()
     */
    public SortedSet<String> getAttributes() {
        return Collections.unmodifiableSortedSet(attributes);
    }

    /**
     * @see IPolicy#hasAttribute(String)
     */
    public boolean hasAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("attribute:name");
        }
        return attributes.contains(name.toUpperCase());
    }

    /**
     * @see IPolicy#setAttribute(String, boolean)
     */
    public void setAttribute(String name, boolean value) {
        if (name == null) {
            throw new NullPointerException("attribute:name");
        }
        if (value) {
            attributes.add(name.toUpperCase());
        } else {
            attributes.remove(name.toUpperCase());
        }
    }

    /**
     * @see IPolicy#addTag(String, String)
     */
    public void addTag(final String name, final String value) {
        if (!tags.contains(new Pair(name, value))) {
            tags.add(new Pair<String, String>(name, value));
        }
    }

    /**
     * @see IPolicy#getTags()
     */
    public Collection<IPair<String, String>> getTags() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * @see IPolicy#clearTags()
     */
    public void clearTags() {
        tags.clear();
    }

    /**
     * @see IPolicy#removeTag(String, String)
     */
    public void removeTag(final String name, final String value) {
        if (name == null) {
            throw new NullPointerException("tag:name");
        }
        if (value == null) {
            throw new NullPointerException("tag:value");
        }

        tags.remove(new Pair(name, value));
    }

    public int hashCode() {
        return ObjectHelper.nullSafeHashCode(getId());
    }

    public boolean equals(Object other) {
        return (this == other) ||
                ((other instanceof Policy)
                        && (getId() != null)
                        && getId().equals(((Policy)other).getId())
                );
    }

    /**
     * @see IPolicy#getPolicyExceptions()
     */
    public IPolicyExceptions getPolicyExceptions() {
        return policyExceptions;
    }

    /**
     * @see IPolicy#setPolicyExceptions(IPolicyExceptions)
     */
    public void setPolicyExceptions(IPolicyExceptions policyExceptions) {
        this.policyExceptions = policyExceptions;
    }
}
