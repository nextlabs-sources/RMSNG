package com.bluejungle.pf.domain.destiny.common;

/*
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * San Mateo CA, Ownership remains with Blue Jungle Inc,
 * All rights reserved worldwide.
 *
 * @author pkeni
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/AccessPolicy.java#1 $
 */

import com.bluejungle.framework.expressions.BooleanOp;
import com.bluejungle.framework.expressions.CompositePredicate;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.ICompositePredicate;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IPredicateReference;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Predicates;
import com.bluejungle.pf.destiny.lib.AccessPolicyComponent;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.destiny.lifecycle.EntityType;
import com.bluejungle.pf.destiny.parser.DomainObjectBuilder;
import com.bluejungle.pf.destiny.parser.PQLException;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pkeni
 *
 * Implements policy based access control for Destiny objects.
 *
 */
public class AccessPolicy implements IAccessPolicy {

    private AccessPolicyComponent accessControlPQL;
    private Set<EntityType> allowedEntities = new HashSet<EntityType>(); /* All EntityType objects */

    private static final String defaultAccessPolicyTemplate =
            "  PBAC FOR * ON DELETE BY appuser.did = # DO allow"
                    + "  PBAC FOR * ON WRITE BY appuser.did = # DO allow"
                    + "  PBAC FOR * ON APPROVE BY appuser.did = # DO allow"
                    + "  PBAC FOR * ON DEPLOY BY appuser.did = # DO allow"
                    + "  PBAC FOR * ON READ BY appuser.did = # DO allow"
                    + "  PBAC FOR * ON ADMIN BY appuser.did = # DO allow ";

    private static final Map<String, IDSpec> roleByName = new HashMap<String, IDSpec>();

    static {
        List<IDSpec> builtInRoles = new ArrayList<IDSpec>();
        builtInRoles.add(new SpecBase(null, SpecType.USER, new Long(0), "Policy Administrator", "", DevelopmentStatus.EMPTY, null, true));
        builtInRoles.add(new SpecBase(null, SpecType.USER, new Long(0), "Policy Analyst", "", DevelopmentStatus.EMPTY, null, true));
        builtInRoles.add(new SpecBase(null, SpecType.USER, new Long(0), "Business Analyst", "", DevelopmentStatus.EMPTY, null, true));
        builtInRoles.add(new SpecBase(null, SpecType.USER, new Long(0), "Report Administrator", "", DevelopmentStatus.EMPTY, null, true));

        for (IDSpec roleSpec : builtInRoles) {
            roleByName.put(roleSpec.getName(), roleSpec);
        }
    }

    public static class SubjectDetector extends Predicates.DefaultDetector {

        private final String subjName;

        public SubjectDetector(Object subj) {
            subjName = forObjectGetName(subj);
        }

        public String getSubjectName() {
            return subjName;
        }

        public boolean checkReference(IPredicateReference pred) {
            String stringVal = ((SpecReference)pred).getReferencedName();
            return stringVal != null && stringVal.equals(subjName);
        }

        public boolean checkRelation(IRelation pred) {
            String stringVal = null;
            Object obj = null;
            if (pred.getLHS() instanceof Constant) {
                obj = ((Constant)pred.getLHS()).getValue().getValue();
                if (obj instanceof Long) {
                    stringVal = ((Long)obj).toString();
                } else {
                    stringVal = (String)obj;
                }
            }
            if (pred.getRHS() instanceof Constant) {
                obj = ((Constant)pred.getRHS()).getValue().getValue();
                if (obj instanceof Long) {
                    stringVal = ((Long)(((Constant)pred.getRHS()).getValue().getValue())).toString();
                } else {
                    stringVal = (String)obj;
                }
            }
            return stringVal != null && stringVal.equals(subjName);
        }

        public boolean check(IPredicate pred) {
            if (pred instanceof SpecBase) {
                String stringVal = ((SpecBase)pred).getName();
                return stringVal != null && stringVal.equals(subjName);
            } else {
                return false;
            }
        }
    }

    public static class SubjectDeleter extends Predicates.DefaultTransformer {

        private final SubjectDetector detector;

        public SubjectDeleter(Object subj) {
            detector = new SubjectDetector(subj);
        }

        public IPredicate transform(IPredicate pred) {
            return detector.check(pred) ? null : pred;
        }

        public IPredicate transformReference(IPredicateReference pred) {
            return detector.checkReference(pred) ? null : pred;
        }

        public IPredicate transformRelation(IRelation pred) {
            return detector.checkRelation(pred) ? null : pred;
        }
    }

    public static class SubjectInserter extends Predicates.DefaultTransformer {

        private final SubjectDetector detector;
        private final IPredicate toInsert;
        private int compositeLevel = 0;
        private boolean found = false;

        public SubjectInserter(Object subj, IPredicate toInsert) {
            detector = new SubjectDetector(subj);
            this.toInsert = toInsert;
        }

        public void transformCompositeStart(ICompositePredicate pred) {
            compositeLevel++;
        }

        public IPredicate transformCompositeEnd(ICompositePredicate orig, IPredicate res) {
            compositeLevel--;
            return insertIfNotFound(orig, res);
        }

        public IPredicate transform(IPredicate pred) {
            if (!found) {
                found = detector.check(pred);
            }
            return insertIfNotFound(pred, pred);
        }

        public IPredicate transformReference(IPredicateReference pred) {
            if (!found) {
                found = detector.checkReference(pred);
            }
            return insertIfNotFound(pred, pred);
        }

        public IPredicate transformRelation(IRelation pred) {
            if (!found) {
                found = detector.checkRelation(pred);
            }
            return insertIfNotFound(pred, pred);
        }

        private IPredicate insertIfNotFound(IPredicate orig, IPredicate res) {
            if (compositeLevel == 0 && !found) {
                if (res instanceof ICompositePredicate && ((ICompositePredicate)res).getOp() == BooleanOp.OR) {
                    ((CompositePredicate)res).addPredicate(toInsert);
                } else {
                    res = new CompositePredicate(BooleanOp.OR, Arrays.asList(new IPredicate[] { res, toInsert }));
                }
                found = true;
                return res;
            } else {
                return orig;
            }
        }
    }

    public AccessPolicy() {
        super();
    }

    public AccessPolicy(AccessPolicyComponent accessControlPQL, Collection<EntityType> allowedEntities) {
        super();

        this.accessControlPQL = accessControlPQL;
        if (allowedEntities != null) {
            this.allowedEntities.addAll(allowedEntities);
        }
    }

    /**
     * @see IAccessPolicy#getActionsForGroup(IDSpec)
     */
    public Collection<IAction> getActionsForGroup(Long groupId) {
        return accessControlPQL.getActionsForGroup(groupId);
    }

    /**
     * @see IAccessPolicy#getActionsForUser(IDSubject)
     */

    public Collection<IAction> getActionsForUser(Long userId) {
        return accessControlPQL.getActionsForUser(userId);
    }

    /**
     * @see IAccessPolicy#getAllowedEntities()
     */
    public Collection<EntityType> getAllowedEntities() {
        return allowedEntities;
    }

    /**
     * @see IAccessPolicy#setActionsForGroup(Long, Collection)
     */
    public void setActionsForGroup(Long groupId, Collection<? extends IAction> actions) throws PQLException {

        accessControlPQL.setActionsForGroup(groupId, actions);
        return;
    }

    /**
     * @see IAccessPolicy#setActionsForGroup(IDSpec, Collection)
     */
    public void setActionsForRole(IPredicate roleSpec, Collection<? extends IAction> actions) throws PQLException {
        accessControlPQL.setActionsForRole(roleSpec, actions);
        return;
    }

    /**
     * @see IAccessPolicy#deleteActionsForGroup(IDSpec, Collection)
     */
    public void deleteActionsForGroup(Long groupId, Collection<? extends IAction> actions) throws PQLException {
        accessControlPQL.deleteActionsForGroup(groupId, actions);
        return;
    }

    /**
     * @see IAccessPolicy#setActionsForUser(IDSubject, Collection)
     */
    public void setActionsForUser(Long userId, Collection<? extends IAction> actions) throws PQLException {

        accessControlPQL.setActionsForUser(userId, actions);
        return;
    }

    /**
     * @see IAccessPolicy#deleteActionsForUser(IDSubject, Collection)
     */
    public void deleteActionsForUser(Long userId, Collection<? extends IAction> actions) throws PQLException {
        accessControlPQL.deleteActionsForUser(userId, actions);
    }

    /**
     * @see IAccessPolicy#setAllowedEntities(Collection)
     */
    public void setAllowedEntities(Collection<EntityType> allowedEntities) {
        this.allowedEntities = new HashSet<EntityType>(allowedEntities);
    }

    /**
     * @see IAccessPolicy#getAllRoleActions()
     */
    public Collection<IAccess> getAllUserGroupActions() {
        return accessControlPQL.getAllUserGroupActions();
    }

    public boolean checkAccess(IPResource resource, IDSubject subject, IAction action) {
        return accessControlPQL.checkAccess(
                resource
                , subject
                , action
                );
    }

    public boolean checkRoleAccess(IDSpec role, IAction action) {
        return accessControlPQL.checkRoleAccess(role, action);
    }

    private static String forObjectGetName(Object obj) {
        if (obj instanceof IDSubject) {
            return ((IDSubject)obj).getUniqueName();
        }
        if (obj instanceof IDSpecRef) {
            return ((IDSpecRef)obj).getReferencedName();
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        if (obj instanceof IDSpec) {
            return ((IDSpec)obj).getName();
        }
        if (obj instanceof Long) {
            return ((Long)obj).toString();
        }
        return null;
    }

    public static IDSpec roleForName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        return (IDSpec)roleByName.get(name);
    }

    public Collection<IDPolicy> getAccessControlPolicies() {
        return accessControlPQL.getPolicies();
    }

    public static String getDefaultAccessPolicyStr(Long ownerId) {
        return AccessPolicy.defaultAccessPolicyTemplate.replaceAll("#", "\"" + ownerId + "\"");
    }

    public static IAccessPolicy getDefaultAccessPolicy(Long ownerId) {
        String ap = getDefaultAccessPolicyStr(ownerId);
        DomainObjectBuilder dob = new DomainObjectBuilder(
                "ACCESS_POLICY ACCESS_CONTROL "
                        + ap
                        + " ALLOWED_ENTITIES "
                );
        try {
            return dob.processAccessPolicy();
        } catch (PQLException ignored) {
            return null;
        }
    }

}
