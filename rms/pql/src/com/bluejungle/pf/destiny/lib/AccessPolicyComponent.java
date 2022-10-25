package com.bluejungle.pf.destiny.lib;

/*
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/lib/AccessPolicyComponent.java#1 $
 */

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.expressions.BooleanOp;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.DefaultPredicateVisitor;
import com.bluejungle.framework.expressions.ICompositePredicate;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IPredicateReference;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.PredicateConstants;
import com.bluejungle.framework.expressions.Predicates;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.pf.destiny.formatter.DomainObjectFormatter;
import com.bluejungle.pf.destiny.parser.DefaultPQLVisitor;
import com.bluejungle.pf.destiny.parser.DomainObjectBuilder;
import com.bluejungle.pf.destiny.parser.DomainObjectDescriptor;
import com.bluejungle.pf.destiny.parser.PQLException;
import com.bluejungle.pf.destiny.parser.PQLLexer;
import com.bluejungle.pf.destiny.parser.PQLParser;
import com.bluejungle.pf.destiny.parser.PQLTreeWalker;
import com.bluejungle.pf.domain.destiny.action.IDAction;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy.SubjectDeleter;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy.SubjectDetector;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy.SubjectInserter;
import com.bluejungle.pf.domain.destiny.common.BuiltInSpec;
import com.bluejungle.pf.domain.destiny.common.GroupAccess;
import com.bluejungle.pf.domain.destiny.common.IAccess;
import com.bluejungle.pf.domain.destiny.common.IDSpec;
import com.bluejungle.pf.domain.destiny.common.IDSpecManager;
import com.bluejungle.pf.domain.destiny.common.SpecBase;
import com.bluejungle.pf.domain.destiny.common.SpecReference;
import com.bluejungle.pf.domain.destiny.common.UserAccess;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;
import com.bluejungle.pf.domain.destiny.misc.IDTarget;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.policy.PolicyObject;
import com.bluejungle.pf.domain.destiny.resource.IPResource;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.destiny.subject.IDSubjectAttribute;
import com.bluejungle.pf.domain.destiny.subject.SubjectAttribute;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;
import com.bluejungle.pf.domain.epicenter.resource.IResource;
import com.bluejungle.pf.engine.destiny.EngineResourceInformation;
import com.bluejungle.pf.engine.destiny.IClientInformationManager;
import com.bluejungle.pf.engine.destiny.IContentAnalysisManager;
import com.bluejungle.pf.engine.destiny.IEngineSubjectResolver;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created on Oct 10, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/lib/AccessPolicyComponent.java#1 $
 */

/**
 * These methods were created for use by the Administrator application backend.
 *
 * AccessPolicyComponent object can represent either the ACCESS_CONTROL or
 * DEFAULT_ACCESS_CONTROL clause of the pql ACCESS_POLICY non-terminal.
 *
 */

public class AccessPolicyComponent {

    private Set<IDPolicy> component = new HashSet<IDPolicy>();

    /**
     *
     * Create an instance of AccessPolicyComponent with no policies
     */
    public AccessPolicyComponent() {
    }

    public AccessPolicyComponent(Collection<IDPolicy> policies) {
        component.addAll(policies);
    }

    public AccessPolicyComponent(String accessPolicyComponent) {
        PQLLexer lexer = null;
        PQLParser parser = null;
        PQLTreeWalker walker = null;
        AST ast = null;

        if (accessPolicyComponent == null) {
            throw new NullPointerException("accessPolicyComponent");
        }
        lexer = new PQLLexer(new StringReader(accessPolicyComponent));
        parser = new PQLParser(lexer);
        ast = parser.getAST();

        try {
            parser.access_control_body();
            ast = parser.getAST();
            walker = new PQLTreeWalker(new DefaultPQLVisitor());
            component.addAll(walker.access_control_body(ast));
        } catch (RecognitionException re) {
            // PKENI
            ;
        } catch (TokenStreamException te) {
            // PKENI
            ;
        }
    }

    public String toPQL() {
        DomainObjectFormatter dof = new DomainObjectFormatter();
        StringBuffer ret = new StringBuffer("access_control ");

        if (component.isEmpty() == false) {
            for (IDPolicy policy : component) {
                ret.append("PBAC");
                dof.formatPolicyBody(null, policy);
                ret.append(dof.getPQL());
                ret.append("\n");
                dof.reset();
            }
        }
        return ret.toString();
    }

    public Collection<IAction> getActionsForUser(Long userId) {
        final Collection<IAction> res = new HashSet<IAction>();
        for (IDPolicy policy : component) {
            if (Predicates.find(policy.getEvaluationTarget().getSubjectPred(), new SubjectDetector(userId))) {
                res.addAll(collectIndividualActions(policy.getTarget().getActionPred()));
            }
        }
        return res;
    }

    public void setActionsForUser(Long userId, Collection<? extends IAction> actions) throws PQLException {
        Collection<IPredicate> actionsDealtWith = new HashSet<IPredicate>();

        for (IDPolicy policy : component) {
            if (policy.getTarget().getActionPred() instanceof BuiltInSpec) {
                continue; /* ADMIN role, this never changes */
            }
            actionsDealtWith.add(policy.getTarget().getActionPred());
            if (actionInCollection(actions, policy.getTarget().getActionPred())) {
                insertUser(userId, policy);
            } else {
                deleteUser(userId, policy);
            }
        }

        if (actions == null) {
            return;
        }

        for (IAction action : actions) {
            if (!actionsDealtWith.contains(action)) {
                String pql = "POLICY FOO DESCRIPTION \"BAR\" FOR * ON " + action.getName().toUpperCase() + " BY " + " APPUSER.did = " + userId + " DO ALLOW";
                DomainObjectBuilder dob = new DomainObjectBuilder(pql);
                dob.processPQL(new DefaultPQLVisitor() {

                    public void visitPolicy(DomainObjectDescriptor descr, IDPolicy policy) {
                        ((PolicyObject)policy).setId(null);
                        ((PolicyObject)policy).setName(null);
                        ((PolicyObject)policy).setDescription(null);
                        component.add(policy);
                    }
                });
            }
        }
    }

    public void deleteActionsForUser(Long userId, Collection<? extends IAction> actions) throws PQLException {
        for (IDPolicy policy : component) {
            if (policy.getTarget().getActionPred() instanceof BuiltInSpec) {
                continue; /* ADMIN role, this never changes */
            }
            if (actionInCollection(actions, policy.getTarget().getActionPred())) {
                deleteUser(userId, policy);
            }
        }
    }

    public Collection<IAction> getActionsForGroup(Long groupId) {
        final Collection<IAction> res = new HashSet<IAction>();
        for (IDPolicy policy : component) {
            if (Predicates.find(policy.getEvaluationTarget().getSubjectPred(), new SubjectDetector(groupId))) {
                res.addAll(collectIndividualActions(policy.getTarget().getActionPred()));
            }
        }
        return res;
    }

    public void setActionsForGroup(Long groupId, Collection<? extends IAction> actions) throws PQLException {
        Collection<IPredicate> actionsDealtWith = new HashSet<IPredicate>();

        for (IDPolicy policy : component) {
            if (policy.getTarget().getActionPred() instanceof BuiltInSpec) {
                continue; /* ADMIN role, this never changes */
            }
            actionsDealtWith.add(policy.getTarget().getActionPred());
            if (actionInCollection(actions, policy.getTarget().getActionPred())) {
                insertGroup(groupId, policy);
            } else {
                deleteGroup(groupId, policy);
            }
        }
        if (actions == null) {
            return;
        }
        for (IAction action : actions) {
            if (!actionsDealtWith.contains(action)) {
                String pql = "POLICY FOO DESCRIPTION \"BAR\" FOR * ON " + action.getName().toUpperCase() + " BY appuser.accessgroupid HAS " + groupId + " DO ALLOW";
                DomainObjectBuilder dob = new DomainObjectBuilder(pql);
                dob.processPQL(new DefaultPQLVisitor() {

                    public void visitPolicy(DomainObjectDescriptor descr, IDPolicy policy) {
                        ((PolicyObject)policy).setId(null);
                        ((PolicyObject)policy).setName(null);
                        ((PolicyObject)policy).setDescription(null);
                        component.add(policy);
                    }
                });
            }
        }
    }

    public void setActionsForRole(IPredicate roleSpec, Collection<? extends IAction> actions) throws PQLException {
        Collection<IPredicate> actionsDealtWith = new HashSet<IPredicate>();

        for (IDPolicy policy : component) {
            if (policy.getTarget().getActionPred() instanceof BuiltInSpec) {
                continue; /* ADMIN role, this never changes */
            }
            actionsDealtWith.add(policy.getTarget().getActionPred());
            if (actionInCollection(actions, policy.getTarget().getActionPred())) {
                insertRole(roleSpec, policy);
            } else {
                deleteRole(roleSpec, policy);
            }
        }

        if (actions == null) {
            return;
        }

        for (IAction action : actions) {
            if (!actionsDealtWith.contains(action)) {
                String pql = "POLICY FOO DESCRIPTION \"BAR\" FOR * ON " + action.getName().toUpperCase() + " BY \"" + ((SpecBase)roleSpec).getName() + "\" DO ALLOW";
                DomainObjectBuilder dob = new DomainObjectBuilder(pql);
                dob.processPQL(new DefaultPQLVisitor() {

                    public void visitPolicy(DomainObjectDescriptor descr, IDPolicy policy) {
                        ((PolicyObject)policy).setId(null);
                        ((PolicyObject)policy).setName(null);
                        ((PolicyObject)policy).setDescription(null);
                        component.add(policy);
                    }
                });
            }
        }
    }

    public void deleteActionsForGroup(Long groupId, Collection<? extends IAction> actions) throws PQLException {
        for (IDPolicy policy : component) {
            if (policy.getTarget().getActionPred() instanceof BuiltInSpec) {
                continue; /* ADMIN role, this never changes */
            }
            if (actionInCollection(actions, policy.getTarget().getActionPred())) {
                deleteGroup(groupId, policy);
            }
        }
    }

    private static void insertUser(Long userId, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectInserter(
                        userId
                        , SubjectAttribute.APPUSER_ID.buildRelation(
                                RelationOp.EQUALS
                                , Constant.build(userId.toString())
                                )
                )
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private void insertGroup(Long groupId, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectInserter(
                        groupId
                        , new Relation(
                                RelationOp.HAS
                                , SubjectAttribute.APPUSER_ACCESSGROUP_ID
                                , Constant.build(groupId.toString())
                        )
                )
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private void insertRole(IPredicate roleSpec, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectInserter(roleSpec, roleSpec)
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private static void deleteUser(Long userId, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectDeleter(userId)
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private static void deleteGroup(Long groupId, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectDeleter(groupId)
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private static void deleteRole(IPredicate roleSpec, IDPolicy policy) {
        IPredicate transformed = Predicates.transform(
                policy.getEvaluationTarget().getSubjectPred()
                , new SubjectDeleter(roleSpec)
                );
        policy.getEvaluationTarget().setSubjectPred(transformed != null ? transformed : PredicateConstants.FALSE);
    }

    private boolean actionInCollection(Collection<? extends IAction> actions, IPredicate actionSpec) {
        if (actions == null)
            return false;
        for (IAction action : actions) {
            IEvaluationRequest req = new AccessCheckRequest(null, action, null);
            if (actionSpec.match(req)) {
                return true;
            }
        }

        return false;
    }

    public Collection<IAccess> getAllUserGroupActions() {
        Collection<IAccess> res = new HashSet<IAccess>();
        Map<Long, Collection<IAction>> retUserMap = new HashMap<Long, Collection<IAction>>();
        Map<Long, Collection<IAction>> retGroupMap = new HashMap<Long, Collection<IAction>>();

        for (IDPolicy policy : component) {
            final Set<Long> userIds = new HashSet<Long>();
            final Set<Long> groupIds = new HashSet<Long>();
            IPredicate predicate = policy.getEvaluationTarget().getSubjectPred();
            predicate.accept(new DefaultPredicateVisitor() {

                public void visit(IRelation pred) {
                    Long longVal = null;
                    if (pred.getLHS() instanceof Constant) {
                        longVal = (Long)(((Constant)pred.getLHS()).getValue().getValue());
                        if (longVal != null) {
                            if (((IDSubjectAttribute)pred.getRHS()) == SubjectAttribute.APPUSER_ID) {
                                userIds.add(longVal);
                            }
                            if (((IDSubjectAttribute)pred.getRHS()) == SubjectAttribute.APPUSER_ACCESSGROUP_ID) {
                                groupIds.add(longVal);
                            }
                        }
                    }
                    if (pred.getRHS() instanceof Constant) {
                        longVal = (Long)(((Constant)pred.getRHS()).getValue().getValue());
                        if (longVal != null) {
                            if (((IDSubjectAttribute)pred.getLHS()) == SubjectAttribute.APPUSER_ID) {
                                userIds.add(longVal);
                            }
                        }
                        if (((IDSubjectAttribute)pred.getLHS()) == SubjectAttribute.APPUSER_ACCESSGROUP_ID) {
                            groupIds.add(longVal);
                        }
                    }
                }
            }, IPredicateVisitor.POSTORDER);

            Collection<IDAction> policyActions = extractActions(policy.getTarget().getActionPred());

            for (Long uid : userIds) {
                if (retUserMap.get(uid) == null) {
                    retUserMap.put(uid, new HashSet<IAction>());
                }
                retUserMap.get(uid).addAll(policyActions);
            }

            for (Long gid : groupIds) {
                if (retGroupMap.get(gid) == null) {
                    retGroupMap.put(gid, new HashSet<IAction>());
                }
                retGroupMap.get(gid).addAll(policyActions);
            }
        }

        for (Map.Entry<Long, Collection<IAction>> entry : retUserMap.entrySet()) {
            UserAccess ua = new UserAccess(entry.getKey(), entry.getValue());
            res.add(ua);
        }
        for (Map.Entry<Long, Collection<IAction>> entry : retGroupMap.entrySet()) {
            GroupAccess ga = new GroupAccess(entry.getKey(), entry.getValue());
            res.add(ga);
        }

        return res;
    }

    public boolean checkAccess(IPResource resource, IDSubject subj, IAction action) {
        boolean retVal = false;
        final IDSpecManager specManager =
                (IDSpecManager)ComponentManagerFactory
                        .getComponentManager()
                        .getComponent(IDSpecManager.COMP_INFO);

        for (IDPolicy acp : component) {
            IDEffectType effectType = null;
            IDTarget target = acp.getEvaluationTarget();

            final IEvaluationRequest req = new AccessCheckRequest(resource, action, subj);
            /* Resource always matches, since it applies to self */
            if (!target.getActionPred().match(req)) {
                continue;
            }

            final Stack<Boolean> stack = new Stack<Boolean>();
            target.getSubjectPred().accept(new IPredicateVisitor() {

                public void visit(ICompositePredicate pred, boolean preorder) {
                    boolean res = stack.pop();
                    if (pred.getOp() == BooleanOp.NOT) {
                        res = !res;
                    } else {
                        for (int i = 1; i != pred.predicateCount(); i++) {
                            if (pred.getOp() == BooleanOp.AND) {
                                res &= stack.pop();
                            } else if (pred.getOp() == BooleanOp.OR) {
                                res |= stack.pop();
                            } else {
                                throw new IllegalStateException("Operator must be an AND, OR, or NOT; got" + pred.getOp());
                            }
                        }
                    }
                    stack.push(res);
                }

                public void visit(IPredicateReference pred) {
                    if (pred instanceof SpecReference) {
                        SpecReference ref = (SpecReference)pred;
                        IDSpec spec = specManager.resolveSpec(ref.getReferencedName());
                        if (spec != null) {
                            spec.getPredicate().accept(this, IPredicateVisitor.POSTORDER);
                        } else {
                            stack.push(false);
                        }
                    } else {
                        stack.push(false);
                    }
                }

                public void visit(IRelation pred) {
                    stack.push(pred.match(req));
                }

                public void visit(IPredicate pred) {
                    stack.push(pred.match(req));
                }
            }, IPredicateVisitor.POSTORDER);

            if (stack.pop()) {
                effectType = (IDEffectType)acp.getMainEffect();
                if (effectType != null) {
                    retVal = effectType.getType() != IDEffectType.DENY_TYPE;
                    break;
                }
            } else {
                effectType = (IDEffectType)acp.getOtherwiseEffect();
                if (effectType != null) {
                    retVal = effectType.getType() != IDEffectType.DENY_TYPE;
                    break;
                }
            }
        }
        return retVal;
    }

    public boolean checkRoleAccess(IDSpec role, IAction action) {
        IEvaluationRequest req = new AccessCheckRequest(null, action, null);
        for (IDPolicy acp : component) {
            IDTarget target = acp.getEvaluationTarget();
            /* Resource always matches, since it applies to self */
            if (target.getActionPred().match(req)) {
                return Predicates.find(acp.getEvaluationTarget().getSubjectPred(), new SubjectDetector(role));
            }
        }
        return false;
    }

    public Collection<IDPolicy> getPolicies() {
        return component;
    }

    private static Collection<IDAction> extractActions(IPredicate pred) {
        final Set<IDAction> res = new HashSet<IDAction>();
        pred.accept(new DefaultPredicateVisitor() {

            @Override
            public void visit(IPredicate pred) {
                if (pred instanceof IDAction) {
                    res.add((IDAction)pred);
                }
            }
        }, IPredicateVisitor.PREORDER);
        return res;
    }

    private static Collection<IAction> collectIndividualActions(IPredicate pred) {
        final Set<IAction> res = new HashSet<IAction>();
        if (pred != null) {
            pred.accept(new DefaultPredicateVisitor() {

                /**
                 * @see DefaultPredicateVisitor#visit(IPredicate)
                 */
                @Override
                public void visit(IPredicate pred) {
                    if (pred instanceof IAction) {
                        res.add((IAction)pred);
                    }
                }
            }
                    , IPredicateVisitor.PREORDER);
        }
        return res;
    }

    private static class AccessCheckRequest implements IEvaluationRequest, IPResource {

        private final IAction action;
        private final IDSubject user;
        private final IPResource resource;

        public AccessCheckRequest(IPResource resource, IAction action, IDSubject user) {
            this.resource = resource;
            this.action = action;
            this.user = user;
        }

        public IAction getAction() {
            return action;
        }

        public IDSubject getApplication() {
            throw new UnsupportedOperationException("getApplication");
        }

        public IDSubject getOwner() {
            return resource.getOwner();
        }

        public IDSubject getHost() {
            throw new UnsupportedOperationException("getHost");
        }

        public String getHostIPAddress() {
            throw new UnsupportedOperationException("getHostIPAddress");
        }

        public Long getRequestId() {
            throw new UnsupportedOperationException("getRequestId");
        }

        public IResource getFromResource() {
            throw new UnsupportedOperationException("getFromResource");
        }

        public EngineResourceInformation getFromResourceInfo() {
            throw new UnsupportedOperationException("getFromResourceInfo");
        }

        public IResource getToResource() {
            throw new UnsupportedOperationException("getToResource");
        }

        public EngineResourceInformation getToResourceInfo() {
            throw new UnsupportedOperationException("getToResourceInfo");
        }

        public IDSubject getLoggedInUser() {
            throw new UnsupportedOperationException("getLoggedInUser");
        }

        public IDSubject getUser() {
            return user;
        }

        public String getUserName() {
            throw new UnsupportedOperationException("getUserName");
        }

        public void setRequestId(Long requestId) {
            throw new UnsupportedOperationException("setRequestId");
        }

        public void setSentTo(IDSubject[] toSubject) {
            throw new UnsupportedOperationException("setToSubject");
        }

        public IDSubject[] getSentTo() {
            throw new UnsupportedOperationException("getToSubject");
        }

        /**
         * @see IEvaluationRequest#getRemoteAddress()
         */
        public String getRemoteAddress() {
            throw new UnsupportedOperationException("getRemoteAddress");
        }

        /**
         * @see IEvaluationRequest#getSubjectByType(SubjectType)
         */
        public IDSubject getSubjectByType(SubjectType type) {
            if (type == SubjectType.USER || type == SubjectType.APPUSER) {
                return getUser();
            } else {
                throw new IllegalArgumentException("type:" + type);
            }
        }

        public long getTimestamp() {
            throw new UnsupportedOperationException("getTimestamp");
        }

        public long getLastSuccessfulHeartbeat() {
            throw new UnsupportedOperationException("getLastSuccessfulHeartbeat");
        }

        public void setLastSuccessfulHeartbeat(long hb) {
            throw new UnsupportedOperationException("setLastSuccessfulHeartbeat");
        }

        public IContentAnalysisManager getContentAnalysisManager() {
            throw new UnsupportedOperationException("getContentAnalysisManager");
        }

        public int getLevel() {
            throw new UnsupportedOperationException("getLevel");
        }

        public IEngineSubjectResolver getSubjectResolver() {
            throw new UnsupportedOperationException("getSubjectResolver");
        }

        public IClientInformationManager getClientInformationManager() {
            throw new UnsupportedOperationException("getClientInformationManager");
        }

        public IServiceProviderManager getServiceProviderManager() {
            throw new UnsupportedOperationException("getServiceProviderManager");
        }

        public DynamicAttributes getEnvironment() {
            throw new UnsupportedOperationException("getEnvironment");
        }

        public void setEnvironment(DynamicAttributes attr) {
            throw new UnsupportedOperationException("setEnvironment");
        }

        public String getAdditionalPoliciesAsPQL() {
            throw new UnsupportedOperationException("getAdditionalPoliciesAsPQL");
        }

        public boolean getIgnoreBuiltinPolicies() {
            throw new UnsupportedOperationException("getIgnoreBuiltinPolicies");
        }

        public boolean getExecuteObligations() {
            throw new UnsupportedOperationException("getExecuteObligations");
        }
    }

}
