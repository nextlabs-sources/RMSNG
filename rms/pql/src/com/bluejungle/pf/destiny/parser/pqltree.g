header {
package com.bluejungle.pf.destiny.parser;

/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: $
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;

import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.comp.IComponentManager;
import com.bluejungle.framework.expressions.BooleanOp;
import com.bluejungle.framework.expressions.CompositePredicate;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.PredicateConstants;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.utils.UnmodifiableDate;
import com.bluejungle.pf.destiny.lib.AccessPolicyComponent;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.destiny.lifecycle.EntityType;
import com.bluejungle.pf.domain.destiny.action.ActionAttribute;
import com.bluejungle.pf.domain.destiny.action.IDActionManager;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.common.SpecReference;
import com.bluejungle.pf.domain.destiny.deployment.AgentAttribute;
import com.bluejungle.pf.domain.destiny.environment.EnvironmentAttribute;
import com.bluejungle.pf.domain.destiny.environment.HeartbeatAttribute;
import com.bluejungle.pf.domain.destiny.environment.TimeAttribute;
import com.bluejungle.pf.domain.destiny.environment.RemoteAccessAttribute;
import com.bluejungle.pf.domain.destiny.exceptions.CombiningAlgorithm;
import com.bluejungle.pf.domain.destiny.exceptions.PolicyExceptions;
import com.bluejungle.pf.domain.destiny.exceptions.PolicyReference;
import com.bluejungle.pf.domain.destiny.function.FunctionApplication;
import com.bluejungle.pf.domain.destiny.misc.EffectType;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;
import com.bluejungle.pf.domain.destiny.misc.Target;
import com.bluejungle.pf.domain.destiny.obligation.DObligationManager;
import com.bluejungle.pf.domain.destiny.obligation.IDObligationManager;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.policy.IDPolicyManager;
import com.bluejungle.pf.domain.destiny.policy.Policy;
import com.bluejungle.pf.domain.destiny.policy.PolicyManager;
import com.bluejungle.pf.domain.destiny.resource.ResourceAttribute;
import com.bluejungle.pf.domain.destiny.subject.IDSubjectSpec;
import com.bluejungle.pf.domain.destiny.subject.Location;
import com.bluejungle.pf.domain.destiny.subject.SubjectAttribute;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyExceptions;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyReference;
import com.bluejungle.pf.domain.epicenter.misc.IObligation;
import com.bluejungle.pf.domain.epicenter.misc.ITarget;
import com.bluejungle.pf.domain.epicenter.policy.IPolicy;
}

class PQLTreeWalker extends TreeParser;

options {
    k = 2;
    importVocab=PQLTokens;
    defaultErrorHandler=false;
}

{
    private static final IComponentManager cm = ComponentManagerFactory.getComponentManager();
    private static final IDPolicyManager pm = (PolicyManager)cm.getComponent(IDPolicyManager.COMP_INFO);
    private static final IDActionManager am = (IDActionManager)cm.getComponent(IDActionManager.COMP_INFO);
    private static final IDObligationManager om = (IDObligationManager)cm.getComponent(DObligationManager.COMP_INFO);
    private boolean allowIDs = false;
    private IPQLVisitor visitor = new DefaultPQLVisitor();
    private String fileName;

    private static class BooleanReturn {
        private boolean flag = false;
        public void set() {
            flag = true;
        }
        public boolean get() {
            return flag;
        }
    }

    public PQLTreeWalker(IPQLVisitor v) {
        this(v, "<UNKNOWN>");
    }

    public PQLTreeWalker(IPQLVisitor v, String fileName) {
        this();
        this.visitor = v;
        this.fileName = fileName;
    }

    public void entity_def(AST t) throws RecognitionException {
        entity_def( t, null, DevelopmentStatus.NEW.getName(), null, null, false );
    }

    private static DomainObjectDescriptor descr( Long id, String name, Long creator, IAccessPolicy ap, EntityType type, String description, DevelopmentStatus status, boolean hidden ) {
        return new DomainObjectDescriptor( id, name, creator, ap, type, description, status, -1, UnmodifiableDate.START_OF_TIME, UnmodifiableDate.START_OF_TIME, null, null, null, null, hidden, true, false );
    }

    private static Long makeId( String id ) {
        try {
            return Long.valueOf( id );
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private SpecReference makeSpecReference(AST id) throws RecognitionException {
        if ( !allowIDs ) {
            throw new RecognitionException("Unexpected reference by ID", "", id.getLine(), id.getColumn());
        }
        return new SpecReference(makeId(id.getText()));
    }

    private static abstract class AttributeResolver {
        public IExpression resolveAttribute(String name) throws RecognitionException {
            return Constant.build(name, name);
        }
        public abstract IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) throws RecognitionException;
    }

    private static final AttributeResolver DEFAULT_ATTR_RESOLVER = new AttributeResolver() {
        public IExpression resolveAttribute(String name) throws RecognitionException {
            IExpression res = ResourceAttribute.forNameAndType(name.toLowerCase(), ResourceAttribute.FILE_SYSTEM_SUBTYPE, true);
            if (res != null) {
                return res;
            }
            res = SubjectAttribute.forNameAndType(name.toLowerCase(), SubjectType.USER);
            /* Only allow well-known (non-arbitrary) attributes to be used unqualified */
            if (res instanceof SubjectAttribute.ExternalSubjectAttribute) {
                return super.resolveAttribute(name);
            }
            return super.resolveAttribute(name);
        }
        public IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) {
            if ("all_subjects".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_SUBJECTS;
            } else if ("all_users".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_USERS;
            } else if ("all_hosts".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_HOSTS;
            } else if ("all_applications".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_APPS;
            }
            String standardActionName = name.toUpperCase();
            if (resolveStandardSpecs && am.isBasicAction(standardActionName)) {
                return am.getAction(standardActionName);
            } else {
                return new SpecReference(name);
            }
        }
    };

    private static final AttributeResolver ENV_ATTR_RESOLVER = new AttributeResolver() {
        public IExpression resolveAttribute(String name) throws RecognitionException {
            if (name == null) {
                throw new NullPointerException("attribute name is null");
            }
            if (name.equalsIgnoreCase("current_time")) {
                return TimeAttribute.IDENTITY;
            } else if (name.equalsIgnoreCase("remote_access")) {
                return RemoteAccessAttribute.REMOTE_ACCESS;
            } else if (name.equalsIgnoreCase("remote_address")) {
                return RemoteAccessAttribute.REMOTE_ADDRESS;
            } else if (name.equalsIgnoreCase("time_since_last_heartbeat")) {
                return HeartbeatAttribute.TIME_SINCE_LAST_HEARTBEAT;
            } else {
                return super.resolveAttribute(name);
            }
        }
        public IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) throws RecognitionException {
            throw new RecognitionException("Undefined spec type: environment.");
        }
    };

    private static final AttributeResolver ACTION_ATTR_RESOLVER = new AttributeResolver() {
        public IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) {
            String standardActionName = name.toUpperCase();
            if (resolveStandardSpecs && am.isBasicAction(standardActionName)) {
                return am.getAction(standardActionName);
            } else {
                return new SpecReference(name);
            }
        }
    };

    private static final AttributeResolver RESOURCE_ATTR_RESOLVER = new AttributeResolver() {
        public IExpression resolveAttribute(String name) throws RecognitionException {
            IExpression res = ResourceAttribute.forNameAndType(name.toLowerCase(), ResourceAttribute.FILE_SYSTEM_SUBTYPE, true);
            if (res != null) {
                return res;
            } else {
                return super.resolveAttribute(name);
            }
        }
        public IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) {
            return new SpecReference(name);
        }
    };

    private static final AttributeResolver SUBJECT_ATTR_RESOLVER = new AttributeResolver() {
        public IExpression resolveAttribute(String name) throws RecognitionException {
            IExpression res = SubjectAttribute.forNameAndType(name.toLowerCase(), SubjectType.USER);
            /* Only allow well-known (non-arbitrary) attributes to be used unqualified */
            if (res instanceof SubjectAttribute.ExternalSubjectAttribute) {
                return super.resolveAttribute(name);
            } else {
                return res;
            }
        }
        public IPredicate makeSpecReference(String name, boolean resolveStandardSpecs) {
            if ("all_subjects".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_SUBJECTS;
            } else if ("all_users".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_USERS;
            } else if ("all_hosts".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_HOSTS;
            } else if ("all_applications".equalsIgnoreCase(name)) {
                return IDSubjectSpec.ALL_APPS;
            } else {
                return new SpecReference(name);
            }
        }
    };

}

program_def { allowIDs = false; }
    :   (entity_def[ null, DevelopmentStatus.NEW.getName(), null, null, false ])*
    ;

internal_program_def { allowIDs = true; }
    :   (entity_def[ null, DevelopmentStatus.NEW.getName(), null, null, false ]
    |   internal_entity_def)*
    ;

policy_body [Long policyId, String policyName, boolean hidden ] returns [IDPolicy p = null] {
        IPredicate from;
        IPredicate to = null;
        IPredicate sendto = null;
        IPredicate act;
        IPredicate subj;
        IPredicate cond;
        IPredicate deploymentTarget;
        List list;
        IDEffectType e;
        Collection acb, dacb, aeb;
        p = pm.newPolicy( policyId, policyName );
        ((Policy)p).setHidden( hidden );
        String attrName = null;
        String tagName = null;
    }
    :   (   ds:DESCRIPTION {
            p.setDescription( #ds.getText() );
        }
        |   #( ATTRIBUTE attrName = identifier ) {
            p.setAttribute(attrName.toLowerCase(), true);
        }
        |   #( TAG tagName=identifier tagval:QUOTED_STRING) {
            p.addTag(tagName, #tagval.getText());
        }
        |   #( SEVERITY sevLevel:UNSIGNED ) {
            try {
                p.setSeverity( Integer.parseInt( #sevLevel.getText() ) );
            } catch ( NumberFormatException nfe ) {
                // TODO: Throw a GOOD exception here: if we're here,
                // the number is too large to fit in a Long
            }
        }
        |   (e = effect_clause) {
            p.setMainEffect(e);
        }
        |   #(TARGET
                #(FROM #(RESOURCE_EXPR from=predicate [RESOURCE_ATTR_RESOLVER]) )
                #(ACTION_EXPR act=predicate [ACTION_ATTR_RESOLVER])
                (#(TO #(RESOURCE_EXPR to=predicate [RESOURCE_ATTR_RESOLVER])))?
                (#(EMAIL_TO #(SUBJECT_EXPR sendto=predicate [SUBJECT_ATTR_RESOLVER])))?
                #(SUBJECT_EXPR subj=predicate [SUBJECT_ATTR_RESOLVER])
            ) {
            ITarget target;
            if ( to != null ) {
                target = Target.forFileActionWithTo(from, to, act, subj);
            } else if (sendto != null) {
                target = Target.forEmailAction(from, act, subj, sendto);
            } else {
                target = Target.forFileAction(from, act, subj);
            }
            p.setTarget( target );
        }
        |   #(OBLIGATION_CLAUSE #(ON (loc:LOCAL)? et:EFFECT_TYPE) list=obligation_list) {
            IDEffectType type = EffectType.getElement( #et.getText().toLowerCase() );
            for ( Iterator iter = list.iterator() ; iter.hasNext() ; ) {
                p.addObligation( (IObligation)iter.next(), type );
            }
        }
        |   #(DEFAULT  e = effect_clause) {
            p.setOtherwiseEffect(e);
        }
        |   #(WHERE cond = predicate [ENV_ATTR_RESOLVER]) {
            p.setConditions( cond );
        }
        |   #(DEPLOYED deploymentTarget = predicate[SUBJECT_ATTR_RESOLVER]) {
            p.setDeploymentTarget(deploymentTarget);
        }
        |   #(EXCEPTIONS ct:combination_type list=policy_exception_list) {
            IPolicyExceptions pe = new PolicyExceptions();

            switch ( #ct.getType() ) {
              case DENY_OVERRIDES:
                pe.setCombiningAlgorithm(CombiningAlgorithm.DENY_OVERRIDES);
                break;
              case ALLOW_OVERRIDES:
                pe.setCombiningAlgorithm(CombiningAlgorithm.ALLOW_OVERRIDES);
                break;
            }
            for ( Iterator iter = list.iterator() ; iter.hasNext() ; ) {
              IPolicyReference ref = new PolicyReference((String)iter.next());
              pe.addPolicy(ref);  
            }

            p.setPolicyExceptions(pe);
        }
        ) *
    ;

policy_exception_list returns [List l = new LinkedList()] {
      String policyName;
    }
    :   (policyName = identifier  { if (policyName != null) l.add(policyName); })+
    ;

combination_type
    :   DENY_OVERRIDES
    |   ALLOW_OVERRIDES
    ;

effect_clause returns [IDEffectType et = null]
    :   (#(EFFECT_CLAUSE EFFECT_TYPE QUOTED_STRING)) => #(EFFECT_CLAUSE et1:EFFECT_TYPE qs:QUOTED_STRING) {
        et = EffectType.getElement( #et1.getText().toLowerCase() );
    }
    |   #(EFFECT_CLAUSE et2:EFFECT_TYPE) {
        et = EffectType.getElement( #et2.getText().toLowerCase() );
    }
    ;

idlist returns [List l = new LinkedList()]
    :   (id:IDENTIFIER { #l.add( id.getText() ); } )+
    ;

obligation_list returns [List l = new LinkedList()] {
        IObligation obl;
    }
    :   (#(OBLIGATION (obl = obligation { if ( obl != null ) l.add(obl); })))+
    ;

obligation returns [IObligation obl = null] {
        List args = null;
    }
    :   #(LOG (QUOTED_STRING (IDENTIFIER)?)?) {
        obl = om.createLogObligation();
    }
    |   #(NOTIFY dest:QUOTED_STRING body:QUOTED_STRING (EMAIL|IM)?) {
        obl = om.createNotifyObligation( dest.getText(), body.getText() );
    }
    |   #(CUSTOM_OBLIGATION co:IDENTIFIER (args = arg_list)) {
        if ("display".equalsIgnoreCase(co.getText())) {
            String message;
            if (args != null && args.size() > 0) {
                message = (String)args.get(0);
            } else {
                message = "";
            }
            obl = om.createDisplayObligation(message);
        } else {
            obl = om.createCustomObligation(co.getText(), args);
        }
    }
    // TODO: Add support for other obligations
    |   DONTLOG
    ;

arg_list returns [List res = new LinkedList()] {
	    Double tmp;
    }
    : ( ( s:QUOTED_STRING {
            res.add(s.getText());
        }
        | n:INTEGER {
            try {
                res.add(Integer.valueOf(n.getText()));
            } catch (Exception ignored) {
                try {
                    res.add(Long.valueOf(n.getText()));
                } catch (Exception alsoIgnored) {
                    res.add(null);
                }
            }
        }
        | tmp = floating_point {
        	res.add(tmp);
        }
        )
    ) *
    ;

identifier returns [String res = null]
    :   i:IDENTIFIER {
        res = #i.getText();
    }
    |   q:QUOTED_STRING {
        res = #q.getText();
    }
    ;

floating_point returns [Double res = new Double(0)] 
    :   #(FLOATING_POINT (i:INTEGER f:INTEGER)) {
    	String fp = i.getText()+"."+f.getText();
        try {
            res = Double.valueOf(fp);
        } catch (Exception ignored) {
        }
    }
    ;

internal_entity_def {
        IAccessPolicy ap = null;
        Long c = null;
    }
    :   #( ANNOTATED_DEFINITION #( ANNOTATIONS id:IDNUMBER es:STATUS c = creator_attribute ap = access_policy (h:HIDDEN)?) entity_def[makeId( #id.getText() ), #es.getText().toUpperCase(), c, ap, h != null] );

entity_def [Long id, String status, Long creator, IAccessPolicy ap, boolean h] {
        IPredicate pred;
        IDPolicy policy;
        IAccessPolicy tap;
        String name;
    }
    :   #( RESOURCE name=identifier (rd:DESCRIPTION)? #( RESOURCE_EXPR pred = predicate [RESOURCE_ATTR_RESOLVER] ) ) {
        String description = ( #rd != null ) ? description = #rd.getText() : null;
        if ( pred != null ) {
            visitor.visitComponent( descr( id, name, creator, ap, EntityType.COMPONENT, description, DevelopmentStatus.forName(status), h ), pred );
        }
    }
    |   #( COMPONENT name=identifier  (cd:DESCRIPTION)? #( COMPONENT_EXPR pred = predicate [DEFAULT_ATTR_RESOLVER] ) ) {
        String description = ( #cd != null ) ? description = #cd.getText() : null;
        if ( pred != null ) {
            visitor.visitComponent( descr( id, name, creator, ap, EntityType.COMPONENT, description, DevelopmentStatus.forName(status), h ), pred);
        }
    }
    |   #( ACTION name=identifier  (ad:DESCRIPTION)? #( ACTION_EXPR pred = predicate [ACTION_ATTR_RESOLVER] ) ) {
        String description = ( #ad != null ) ? description = #ad.getText() : null;
        if ( pred != null ) {
            visitor.visitComponent( descr( id, name, creator, ap, EntityType.COMPONENT, description, DevelopmentStatus.forName(status), h ), pred);
        }
    }
    |   #( pk:PRINCIPAL name = identifier (sd:DESCRIPTION)? #( SUBJECT_EXPR pred = predicate [SUBJECT_ATTR_RESOLVER]) ) {
        String description = ( #sd != null ) ? description = #sd.getText() : null;
        if ( pred != null ) {
            SubjectType subjectType = SubjectType.forName(#pk.getText());
            visitor.visitComponent( descr( id, name, creator, ap, EntityType.COMPONENT, description, DevelopmentStatus.forName(status), h ), pred);
        }
    }
    |   #( POLICY name=identifier policy = policy_body[ id, name, h ] ) {
        if ( policy != null ) {
            DevelopmentStatus ds = DevelopmentStatus.forName(status);
            policy.setStatus( ds );
            visitor.visitPolicy( descr( id, policy.getName(), creator, ap, EntityType.POLICY, policy.getDescription(), ds, h ), policy );
        }
    }
    |   #( FOLDER name=identifier  (pfd:DESCRIPTION)? ) {
        visitor.visitFolder( descr( id, name, creator, ap, EntityType.FOLDER, #pfd != null ? #pfd.getText() : null, DevelopmentStatus.forName(status), h ) );
    }
    |   #( LOCATION name=identifier lv:QUOTED_STRING ) {
        Location location = new Location( id, name, #lv.getText() );
        visitor.visitLocation( descr( id, name, creator, ap, EntityType.LOCATION, "", DevelopmentStatus.forName(status), h ), location );
    }
    |   tap = access_policy {
        visitor.visitAccessPolicy (descr (null, new String ("dummy"), null, null, EntityType.ILLEGAL, "", DevelopmentStatus.forName("ILLEGAL"), h), tap);
    }
    ;

predicate [AttributeResolver attrResolver] returns [IPredicate s = null] {
        List list;
        RelationOp op;
        IExpression lhs, rhs;
        BooleanReturn groupSeenLeft = new BooleanReturn();
        BooleanReturn groupSeenRight = new BooleanReturn();
        String name;
    }
    :   #(OR list = predicate_list [attrResolver] ) {
        s = new CompositePredicate( BooleanOp.OR, list );
    }
    |   #(AND list = predicate_list [attrResolver] ) {
        s = new CompositePredicate( BooleanOp.AND, list );
    }
    |   #(NOT s = predicate [attrResolver] ) {
        s = new CompositePredicate( BooleanOp.NOT,  s );
    }
    |   #(relopn:RELATION_OP lhs = expression [attrResolver, groupSeenLeft] rhs = expression [attrResolver, groupSeenRight]) {
        op = RelationOp.getElement (relopn.getText ().toLowerCase ());
        if (groupSeenLeft.get() && groupSeenRight.get()) {
            System.out.println ("Cannot have group on both left and right side of a predicate");
            s = null;
        } else if (groupSeenLeft.get() || groupSeenRight.get()) {
            if (!(lhs instanceof Constant || rhs instanceof Constant)) {
                throw new RecognitionException("Groups must be compared only to constants", fileName, #relopn.getLine(), #relopn.getColumn());
            } else if (op == RelationOp.EQUALS || op == RelationOp.NOT_EQUALS) {
                if (!(rhs instanceof Constant)) {
                    String strVal = ((Constant)lhs).getRepresentation();
                    if (rhs != null) {
                        s = new SpecReference(strVal);
                    } else {
                        s = attrResolver.makeSpecReference(strVal, false);
                    }
                } else {
                    String strVal = ((Constant)rhs).getRepresentation();
                    if (lhs != null) {
                        s = new SpecReference(strVal);
                    } else {
                        s = attrResolver.makeSpecReference(strVal, false);
                    }
                }
            } else if (op == RelationOp.HAS || op == RelationOp.DOES_NOT_HAVE) {
                s = lhs.buildRelation(op, rhs);
            } else {
                throw new RecognitionException("Groups must be compared only for equality, inequality, or containment", fileName, #relopn.getLine(), #relopn.getColumn());
            }
        } else if (lhs != null) {
            if (lhs instanceof Constant) {
                IRelation tmp = rhs.buildRelation(op, lhs);
                s = new Relation(op, tmp.getRHS(), tmp.getLHS());
            } else {
                s = lhs.buildRelation(op, rhs);
            }
        } else {
            s = PredicateConstants.FALSE;
        }
    }
    |   name = identifier {
        s = attrResolver.makeSpecReference(name, true);
    }
    |   sID:IDNUMBER {
        s = makeSpecReference(#sID);
    }
    |   TRUE             { s = PredicateConstants.TRUE; }
    |   FALSE            { s = PredicateConstants.FALSE; }
    |   STAR             { s = PredicateConstants.TRUE; }
    |   EMPTY            { s = PredicateConstants.FALSE; }
    ;

predicate_list [AttributeResolver attrResolver] returns [List list = new LinkedList()] {
        IPredicate s;
    }
    :   (s = predicate[attrResolver] { list.add( s ); } )+
    ;

expression [AttributeResolver attrResolver, BooleanReturn groupSeen] returns [IExpression expr = null] {
        String attribute = null;
        String[] parts = null;
        List arguments = new LinkedList();
        IExpression tempExp = null;
    }
    :   parts = dot_expression {
        if (parts == null || parts.length < 2 || parts.length > 3) {
            throw new RecognitionException("Detected an invalid dot expression.");
        }
        for ( int i = 0 ; i != parts.length ; i++ ) {
            if (parts[i] == null || parts[i].length() == 0) {
                throw new RecognitionException("Detected an empty dot expression element: "+i);
            }
        }
        String prop = null;
        String type = parts[0];
        String subtype = null;
        if (parts.length == 2) {
            prop = parts[1];
        } else if (parts.length == 3) {
            subtype = parts[1];
            prop = parts[2];
        }
        if (type.equals("user")||type.equals("host")||type.equals("application")||type.equals("recipient")) {
            subtype = type;
            type = "subject";
        }
        if (prop.equals("group")) {
            groupSeen.set();
        }
        if (type.equals("principal") || type.equals("subject")) {
            SubjectType subjType;
            if (subtype != null) {
                subjType = SubjectType.forName(subtype);
            } else {
                subjType = SubjectType.USER;
            }
            expr = SubjectAttribute.forNameAndType(prop, subjType);
        } else if (type.equals("resource")) {
            if (subtype == null) {
                subtype = "fso";
            }
            if (prop.equals("group")) {
                expr = null;
            } else {
                expr = ResourceAttribute.forNameAndType(prop, subtype);
            }
        } else if (type.equals("action")) {
            if (prop.equals("group")) {
                expr = null;
            } else {
                expr = ActionAttribute.getElement(prop);
            }
        } else if (type.equals("appuser")) {
            expr = SubjectAttribute.forNameAndType(prop, SubjectType.APPUSER);
        } else if (type.equals("environment")) {
            if (subtype == null) {
                subtype = prop;
                prop = "identity";
            }
            if (subtype.equals("current_time")) {
                if ( TimeAttribute.existsElement(prop) ) {
                    expr = TimeAttribute.getElement(prop);
                } else {
                    throw new RecognitionException("Unsupported time attribute: " + prop);
                }
            } else if (subtype.equals("remote_access")) {
                expr = RemoteAccessAttribute.REMOTE_ACCESS;
            } else if (subtype.equals("remote_address")) {
                expr = RemoteAccessAttribute.REMOTE_ADDRESS;
            } else if (subtype.equals("time_since_last_heartbeat")) {
                expr = HeartbeatAttribute.TIME_SINCE_LAST_HEARTBEAT;
            } else {
                expr = EnvironmentAttribute.forName(subtype);
            }
        } else if (type.equals("current_time")) {
            if ( TimeAttribute.existsElement(prop) ) {
                expr = TimeAttribute.getElement(prop);
            } else {
                throw new RecognitionException("Unsupported time attribute: " + prop);
            }
        } else if (type.equals("remote_access")) {
            expr = RemoteAccessAttribute.REMOTE_ACCESS;
        } else if (type.equals("remote_address")) {
            expr = RemoteAccessAttribute.REMOTE_ADDRESS;
        } else if (type.equals("agent")) {
            expr = AgentAttribute.getAttribute(prop);
        } else if (subtype.equals("time_since_last_heartbeat")) {
            expr = HeartbeatAttribute.TIME_SINCE_LAST_HEARTBEAT;
        } else {
            throw new RecognitionException("Unknown attribute: "+type+"."+subtype+"."+prop);
        }
    }
    |   #(CALL_FUNCTION sn:QUOTED_STRING fn:QUOTED_STRING ( tempExp = expression [attrResolver, groupSeen] { arguments.add(tempExp); }  )* ) {
        expr = new FunctionApplication(#sn.getText(), #fn.getText(), arguments);
    }
    |   nm:IDENTIFIER {
        String s = #nm.getText().toLowerCase();
        if (s.equals("group")) {
           groupSeen.set();
            expr = null; // Caller checks value and converts this to SpecReference based on context
        } else if (s.equals("action")) {
            expr = ActionAttribute.NAME;
        } else if (s.equals("resource")) {
            expr = ResourceAttribute.NAME;
        } else if (s.equals("user") || s.equals("principal") || s.equals("subject")) {
            expr = SubjectAttribute.USER_NAME;
        } else if (s.equals("host")) {
            expr = SubjectAttribute.HOST_NAME;
        } else if (s.equals("application")) {
            expr = SubjectAttribute.APP_NAME;
        } else if (s.equals("recipient")) {
            expr = SubjectAttribute.RECIPIENT_USER_NAME;
        } else {
            expr = attrResolver.resolveAttribute(nm.getText());
        }
    }
    |   qs:QUOTED_STRING {
        expr = Constant.build(#qs.getText(), #qs.getText());
    }
    |   num:INTEGER {
        String text = #num.getText();
        expr = Constant.build(Long.parseLong(text), text);
    }
    |   fp:FLOATING_POINT {
        throw new RecognitionException("Floating point constants unsupported in pql tree walker", fileName, #fp.getLine(), #fp.getColumn());
    }
    |   id:IDNUMBER {
        expr = makeSpecReference(#id);
    }
    |   NULL {
        expr = Constant.NULL;
    }
    ;

dot_expression returns [String[] parts = null]
    :   #(DOT_EXPRESSION parts = dot_expression_list)
    ;

dot_expression_list returns [String[] parts = null] {
    List res = new ArrayList(3);
    }
    :   ( q:IDENTIFIER { res.add(#q.getText().toLowerCase()); } )* {
        parts = (String[])res.toArray(new String[res.size()]);
    }
    ;

access_policy returns [IAccessPolicy ap = null] {
     Collection acb;
     Collection aeb;
     AccessPolicyComponent apc;
     }
     :   #(ACCESS_CONTROL_POLICY acb=access_control_body aeb=allowed_entities_body) {
         if (acb != null || aeb != null) {
             apc = new AccessPolicyComponent(acb);
             ap = new AccessPolicy (apc, aeb);
         } else {
             ap = null;
         }
     }
     ;

access_control_body returns [Collection coll = null]
    :   #(ACCESS_CONTROL coll=policy_body_list)
    |   EMPTY
    ;

allowed_entities_body returns [Collection coll = null]
    :   #(ALLOWED_ENTITIES coll=term_list)
    |   EMPTY
    ;

policy_body_list returns [List res = new LinkedList()] {
        IPolicy pb;
    }
    :   (PBAC  pb=policy_body [null, null, false] {
            res.add(pb);
        }
        )*
    ;

term_list returns [List res = new LinkedList()]
    :   (
            POLICY_ENTITY      {res.add(EntityType.forName("POLICY"));}
        |   USER_ENTITY        {res.add(EntityType.forName("USER"));}
        |   HOST_ENTITY        {res.add(EntityType.forName("HOST"));}
        |   RESOURCE_ENTITY    {res.add(EntityType.forName("RESOURCE"));}
        |   PORTAL_ENTITY      {res.add(EntityType.forName("PORTAL"));}
        |   LOCATION_ENTITY    {res.add(EntityType.forName("LOCATION"));}
        |   APPLICATION_ENTITY {res.add(EntityType.forName("APPLICATION"));}
        |   ACTION_ENTITY      {res.add(EntityType.forName("ACTION"));}
        |   DEVICE_ENTITY      {res.add(EntityType.forName("DEVICE"));}   
        |   SAP_ENTITY         {res.add(EntityType.forName("SAP"));}     
        |   ENOVIA_ENTITY      {res.add(EntityType.forName("ENOVIA"));}  
        )*
    ;

creator_attribute returns [Long s = null] {
    }
    :   (ca:CREATOR_ATTRIBUTE) {
        if (#ca.getText().compareTo("empty") == 0) {
            s = null;
        } else {
            s = Long.valueOf ( #ca.getText() );
        }
    }
    ;
