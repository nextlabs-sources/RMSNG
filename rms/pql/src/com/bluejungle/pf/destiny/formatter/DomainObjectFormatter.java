package com.bluejungle.pf.destiny.formatter;

/*
 * All sources, binaries and HTML pages (C) Copyright 2007 by extLabs Inc,
 * San Mateo, CA. Ownership remains with NextLabs Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/formatter/DomainObjectFormatter.java#2 $
 */

import com.bluejungle.framework.expressions.BooleanOp;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.IAttribute;
import com.bluejungle.framework.expressions.ICompositePredicate;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionReference;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IFunctionApplication;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IPredicateReference;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.utils.IPair;
import com.bluejungle.framework.utils.StringUtils;
import com.bluejungle.framework.utils.UnmodifiableDate;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.destiny.lifecycle.EntityType;
import com.bluejungle.pf.destiny.parser.DomainObjectDescriptor;
import com.bluejungle.pf.destiny.parser.PQLParser;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IDSpec;
import com.bluejungle.pf.domain.destiny.common.IDSpecRef;
import com.bluejungle.pf.domain.destiny.misc.EffectType;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;
import com.bluejungle.pf.domain.destiny.obligation.IDObligation;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.policy.PolicyFolder;
import com.bluejungle.pf.domain.destiny.subject.Location;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyExceptions;
import com.bluejungle.pf.domain.epicenter.exceptions.IPolicyReference;
import com.bluejungle.pf.domain.epicenter.misc.IEffectType;
import com.bluejungle.pf.domain.epicenter.misc.IObligation;
import com.bluejungle.pf.domain.epicenter.misc.ITarget;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Formats domain objects as PQL.
 */
public class DomainObjectFormatter {

    private StringBuilder out = new StringBuilder();
    private int indent;
    private final int spi; // Spaces Per Unit of Indentation.
    private static Set<String> UNQUOTED_ATTRIBUTES = new HashSet<String>();
    private static List<IEffectType> sortedEffects = new ArrayList<IEffectType>(EffectType.elements());

    static {
        UNQUOTED_ATTRIBUTES.add("id");
        UNQUOTED_ATTRIBUTES.add("location");
        UNQUOTED_ATTRIBUTES.add("email");
        UNQUOTED_ATTRIBUTES.add("user");
        UNQUOTED_ATTRIBUTES.add("host");
        UNQUOTED_ATTRIBUTES.add("action");
        UNQUOTED_ATTRIBUTES.add("resource");
        UNQUOTED_ATTRIBUTES.add("principal");
        UNQUOTED_ATTRIBUTES.add("application");
        UNQUOTED_ATTRIBUTES.add("group");
        UNQUOTED_ATTRIBUTES.add("recipient");

        // Get them in canonical order to help with unit testing
        Collections.sort(sortedEffects, new Comparator<IEffectType>() {

            @Override
            public int compare(IEffectType e1, IEffectType e2) {
                return e1.toString().compareTo(e2.toString());
            }
        });

    }

    private final PredicateVisitor visitor = new PredicateVisitor();

    /**
     * Creates new DomainObjectFormatter.
     */
    public DomainObjectFormatter() {
        this(4);
    }

    /**
     * Creates new DomainObjectFormatter;
     *
     * @param spi spaces per unit of indentation
     */
    public DomainObjectFormatter(int spi) {
        this.spi = spi;
    }

    /**
     * Accesses the PQL built in the formatter.
     *
     * @return the PQL built in the formatter.
     */
    public String getPQL() {
        String ret = out.toString();
        out.setLength(0);
        return ret;
    }

    /**
     * Formats a definition of a policy or a spec.
     *
     * @param def a policy or a spec.
     */
    public void formatDef(Object def) {
        if (def instanceof IDPolicy) {
            IDPolicy policy = (IDPolicy)def;
            formatPolicyDef(new DomainObjectDescriptor(
                    policy.getId()
                    , policy.getName()
                    , (policy.getOwner() == null) ? null : policy.getOwner().getId()
                    , policy.getAccessPolicy()
                    , EntityType.POLICY
                    , policy.getDescription()
                    , policy.getStatus()
                    , -1
                    , UnmodifiableDate.START_OF_TIME
                    , UnmodifiableDate.START_OF_TIME
                    , null
                    , null
                    , null
                    , null
                    , policy.isHidden()
                    , true
                    , false)
                    , policy);
        } else if (def instanceof IDSpec) {
            formatDef((IDSpec)def);
        } else if (def instanceof PolicyFolder) {
            formatFolder((PolicyFolder)def);
        }
    }

    public void formatPolicyDef(DomainObjectDescriptor descr, IDPolicy p) {
        put("ID ", descr.getId());
        if (descr.getStatus() != DevelopmentStatus.NEW) {
            put(" STATUS ", descr.getStatus(), " ");
        } else {
            put(" ");
        }
        Long owner = descr.getOwner();
        if (owner != null) {
            put("CREATOR \"", owner.toString(), "\" ");
        }
        if (descr.getAccessPolicy() != null) {
            formatAccessPolicy(descr.getAccessPolicy());
        }
        iput(descr.isHidden() ? "HIDDEN POLICY " : "POLICY ", PQLParser.quoteName(descr.getName()));
        try {
            indent++;
            formatPolicyBody(descr.getDescription(), p);
            IPredicate dt = p.getDeploymentTarget();
            if (dt != null) {
                nli("DEPLOYED TO ");
                IPredicateVisitor pv = new IPredicateVisitor() {

                    private int level = 0;
                    private boolean firstLevelOr = false;
                    private int numPredicates = 0;
                    private IdentityHashMap<IPredicate, Object> seen = new IdentityHashMap<IPredicate, Object>();

                    public void visit(ICompositePredicate pred, boolean preorder) {
                        if (preorder) {
                            if (seen.containsKey(pred)) {
                                throw new IllegalStateException("Objects with circular references cannot be formated AS PQL.");
                            }
                            seen.put(pred, null);
                            numPredicates = pred.predicateCount();
                            level++;
                            BooleanOp op = pred.getOp();

                            if (level == 1) {
                                if (BooleanOp.AND.equals(op)) {
                                    visitor.visit(pred, "WITH", preorder);
                                } else if (BooleanOp.OR.equals(op)) {
                                    firstLevelOr = true;
                                    visitor.visit(pred, ",", preorder);
                                }
                            } else if (level == 2) {
                                if (BooleanOp.AND.equals(op) && firstLevelOr) {
                                    visitor.visit(pred, "WITH", preorder);
                                } else {
                                    visitor.visit(pred, true);
                                }
                            } else {
                                visitor.visit(pred, true);
                            }
                        } else {
                            seen.remove(pred);
                            visitor.visit(pred, false);
                        }
                    }

                    public void visit(IPredicateReference pred) {
                        visitor.visit(pred);
                        updateLevel();
                    }

                    public void visit(IRelation pred) {
                        visitor.visit(pred);
                        updateLevel();
                    }

                    public void visit(IPredicate pred) {
                        visitor.visit(pred);
                        updateLevel();
                    }

                    private void updateLevel() {
                        if (numPredicates > 1) {
                            numPredicates--;
                        } else {
                            level--;
                        }

                    }

                };
                dt.accept(pv, IPredicateVisitor.PREPOSTORDER);
            }
        } finally {
            indent--;
            nl();
        }
    }

    public void formatPolicyBody(String description, IDPolicy p) {
        if (description != null) {
            nli("DESCRIPTION ", "\"", StringUtils.escape(description), "\"");
        }
        for (String attrName : p.getAttributes()) {
            nli("ATTRIBUTE ", attrName);
        }

        for (IPair<String, String> tag : p.getTags()) {
            nli("TAG ", PQLParser.quoteName(tag.first()), "=\"", StringUtils.escape(tag.second()), "\"");
        }

        if (p.getSeverity() != 0) {
            nli("SEVERITY ", p.getSeverity());
        }
        ITarget t = p.getTarget();
        if (t != null) {
            IPredicate fr = t.getFromResourcePred();
            assert fr != null; // Policy's constructor must check this
            nli("FOR ");
            formatRef(fr);
            IPredicate a = t.getActionPred();
            assert a != null; // Policy's constructor must check this
            nli("ON ");
            formatRef(a);
            IPredicate tr = t.getToResourcePred();
            if (tr != null) {
                nli("TO ");
                formatRef(tr);
            }
            IPredicate ts = t.getToSubjectPred();
            if (ts != null) {
                nli("SENT_TO ");
                formatRef(ts);
            }
            IPredicate s = t.getSubjectPred();
            assert s != null; // Policy's constructor must check this
            nli("BY ");
            formatRef(s);
        }
        IPredicate conditions = p.getConditions();
        {
            if (conditions != null) {
                nli("WHERE ");
                formatRef(conditions);
            }
        }

        IPolicyExceptions exceptions = p.getPolicyExceptions();

        if (exceptions != null) {
            formatExceptions(exceptions);
        }

        IDEffectType mainEffectType = (IDEffectType)p.getMainEffect();
        if (mainEffectType != null) {
            nli("DO ", mainEffectType);
        }
        IDEffectType otherwise = (IDEffectType)p.getOtherwiseEffect();
        if (otherwise != null) {
            nli("BY DEFAULT DO ", otherwise);
        }

        for (IEffectType et : sortedEffects) {
            IObligation[] oa = p.getObligationArray(et);
            if (oa != null && oa.length != 0) {
                nli("ON ", et, " DO ");
                for (int i = 0; i != oa.length; i++) {
                    if (i != 0) {
                        put(", ");
                    }
                    format(oa[i]);
                }
            }
        }
    }

    public void formatPolicyDef(Long id, IDPolicy policy) {
        if (policy == null) {
            throw new NullPointerException("policy");
        }
        formatPolicyDef(
                new DomainObjectDescriptor(
                        id
                        , policy.getName()
                        , (policy.getOwner() == null) ? null : policy.getOwner().getId()
                        , policy.getAccessPolicy()
                        , EntityType.POLICY
                        , policy.getDescription()
                        , policy.getStatus()
                        , -1
                        , UnmodifiableDate.START_OF_TIME
                        , UnmodifiableDate.START_OF_TIME
                        , null
                        , null
                        , null
                        , null
                        , policy.isHidden()
                        , true
                        , false)
                , policy);
    }

    public void formatFolder(PolicyFolder def) {
        formatFolder(new DomainObjectDescriptor(
                def.getId()
                , def.getName()
                , (def.getOwner() == null) ? null : def.getOwner().getId()
                , def.getAccessPolicy()
                , EntityType.FOLDER
                , def.getDescription()
                , def.getStatus()));
    }

    public void formatFolder(DomainObjectDescriptor descr) {
        if (descr == null) {
            throw new NullPointerException("descr");
        }
        if (descr.getType() != EntityType.FOLDER) {
            throw new IllegalArgumentException("Expecting Policy Folder, received " + descr.getType());
        }
        put("ID ", descr.getId());
        if (descr.getStatus() != DevelopmentStatus.NEW) {
            put(" STATUS ", descr.getStatus(), " ");
        } else {
            put(" ");
        }
        if (descr.getOwner() != null) {
            put("CREATOR \"", descr.getOwner().toString(), "\" ");
        }
        if (descr.getAccessPolicy() != null) {
            formatAccessPolicy(descr.getAccessPolicy());
        }
        put(" FOLDER ", PQLParser.quoteName(descr.getName()));
        if (descr.getDescription() != null && descr.getDescription().length() != 0) {
            put(" ", "DESCRIPTION \"", StringUtils.escape(descr.getDescription()), "\"");
        }
    }

    public void formatLocation(DomainObjectDescriptor descr, Location location) {
        if (descr == null) {
            throw new NullPointerException("descr");
        }
        if (descr.getType() != EntityType.LOCATION) {
            throw new IllegalArgumentException("Expecting Location, received " + descr.getType());
        }
        put("ID ", descr.getId());
        if (descr.getStatus() != DevelopmentStatus.NEW) {
            put(" STATUS ", descr.getStatus(), " ");
        } else {
            put(" ");
        }
        if (descr.getOwner() != null) {
            put("CREATOR \"", descr.getOwner().toString(), "\" ");
        }
        if (descr.getAccessPolicy() != null) {
            formatAccessPolicy(descr.getAccessPolicy());
        }
        put(descr.isHidden() ? "HIDDEN LOCATION " : "LOCATION ", PQLParser.quoteName(location.getName()), " = \"", StringUtils.escape(location.getValue()), "\"");
        if (descr.getDescription() != null && descr.getDescription().length() != 0) {
            put(" ", "DESCRIPTION \"", StringUtils.escape(descr.getDescription()), "\"");
        }
    }

    private void formatDescriptor(DomainObjectDescriptor descr) {
        put("ID ", descr.getId());
        if (descr.getStatus() != DevelopmentStatus.NEW) {
            put(" STATUS ", descr.getStatus(), " ");
        } else {
            put(" ");
        }
        Long owner = descr.getOwner();
        if (owner != null) {
            put("CREATOR \"", owner.toString(), "\" ");
        }
        if (descr.getAccessPolicy() != null) {
            formatAccessPolicy(descr.getAccessPolicy());
        }
        if (descr.getName() != null) {
            put(descr.isHidden() ? "HIDDEN COMPONENT " : "COMPONENT ", PQLParser.quoteName(descr.getName()), " = ");
        }
        String description = descr.getDescription();
        if (description != null && description.length() != 0) {
            nli();
            put("DESCRIPTION \"", StringUtils.escape(description), "\"");
            nli();
        }
    }

    public void formatAccessPolicy(IAccessPolicy ap) {
        AccessPolicy accessPolicy = (AccessPolicy)ap;

        nli("ACCESS_POLICY");
        Collection<IDPolicy> policies = null;
        Collection<EntityType> entities = null;

        nli("ACCESS_CONTROL");
        policies = accessPolicy.getAccessControlPolicies();
        if (policies != null) {
            try {
                indent++;

                // we want to the access policies sorted so we can compare the pql directly.
                List<IDPolicy> sortedByAction = new ArrayList<IDPolicy>(policies);
                Collections.sort(sortedByAction, new Comparator<IDPolicy>() {

                    @Override
                    public int compare(IDPolicy o1, IDPolicy o2) {
                        String action1 = getAction(o1);
                        String action2 = getAction(o2);
                        return action1.compareTo(action2);
                    }

                    /**
                     * never return null, the worst case is empty string
                     * @return
                     */
                    private String getAction(IDPolicy policy) {
                        ITarget t1 = policy.getTarget();
                        if (t1 != null) {
                            IPredicate actionPredicate = t1.getActionPred();
                            if (actionPredicate instanceof IAction) {
                                return ((IAction)actionPredicate).getName();
                            }
                        }
                        return "";
                    }
                });
                for (IDPolicy pol : sortedByAction) {
                    nli("PBAC ");
                    try {
                        indent++;
                        formatPolicyBody(null, pol);
                    } finally {
                        indent--;
                    }
                }
            } finally {
                indent--;
            }
        }

        nli("ALLOWED_ENTITIES");
        entities = accessPolicy.getAllowedEntities();
        if (entities != null) {
            try {
                indent++;
                int i = 0;
                for (EntityType et : entities) {
                    if (i++ != 0) {
                        iput(", ");
                    }
                    iput(et.getName() + "_ENTITY");
                }
                put(" ");
            } finally {
                indent--;
            }
        }
        nl();
    }

    private void formatDef(IDSpec spec) {
        if (spec == null) {
            throw new NullPointerException("spec");
        }
        if (spec.getName() != null) {
            put("ID ", spec.getId());
            if (spec.getStatus() != DevelopmentStatus.NEW) {
                put(" STATUS ", spec.getStatus(), " ");
            } else {
                put(" ");
            }
            if ((spec.getOwner() != null) && (spec.getOwner().getId() != null)) {
                put("CREATOR \"", spec.getOwner().getId().toString(), "\" ");
            }
            if (spec.getAccessPolicy() != null) {
                formatAccessPolicy(spec.getAccessPolicy());
            }
            nli(spec.isHidden() ? "HIDDEN COMPONENT " : "COMPONENT ", PQLParser.quoteName(spec.getName()), " = ");
            String description = spec.getDescription();
            if (description != null && description.length() != 0) {
                nli();
                put("DESCRIPTION \"", StringUtils.escape(description), "\"");
                nli();
            }
            spec.accept(visitor, IPredicateVisitor.PREPOSTORDER);
        }
    }

    public void formatDef(DomainObjectDescriptor descr, IPredicate pred) {
        if (pred == null) {
            throw new NullPointerException("pred");
        }
        formatDescriptor(descr);
        pred.accept(visitor, IPredicateVisitor.PREPOSTORDER);
        nl();
    }

    public void formatRef(IPredicate pred) {
        if (pred == null) {
            throw new NullPointerException("pred");
        }
        pred.accept(visitor, IPredicateVisitor.PREPOSTORDER);
    }

    public void formatExceptions(IPolicyExceptions exceptions) {
        if (exceptions == null) {
            throw new NullPointerException("exceptions");
        }

        List<IPolicyReference> refs = exceptions.getPolicies();

        if (refs == null || refs.isEmpty()) {
            return;
        }

        nli("SUBPOLICY ");

        put(exceptions.getCombiningAlgorithm().getName());

        boolean first = true;
        for (IPolicyReference ref : refs) {
            if (first) {
                first = false;
            } else {
                put(", ");
            }
            nli("\"", ref.getReferencedName(), "\"");
        }
    }

    public void format(IObligation o) {
        if (o == null) {
            throw new NullPointerException("obligation");
        }
        if (o instanceof IDObligation) {
            put(((IDObligation)o).toPQL());
        } else {
            put(o.getType());
        }
    }

    /**
     * Descendants of this class can override this method to format relations differently.
     *
     * @param rel the relation to be formatted.
     */
    protected void formatSingleExpression(final StringBuilder out, IExpression expr) {
        IExpressionVisitor ev = new IExpressionVisitor() {

            public void visit(IAttribute attr) {
                formatAttribute(out, attr);
            }

            public void visit(Constant constant) {
                out.append(constant.toString());
            }

            public void visit(IFunctionApplication func) {
                formatFunction(out, func);
            }

            public void visit(IExpression expression) {
                formatExpression(out, expression);
            }

            public void visit(IExpressionReference ref) {
                formatExpressionReference(out, ref);
            }

        };
        expr.acceptVisitor(ev, IExpressionVisitor.PREORDER);
    }

    protected void formatRelation(final StringBuilder out, IRelation rel) {
        formatSingleExpression(out, rel.getLHS());
        out.append(' ');
        out.append(rel.getOp());
        out.append(' ');
        formatSingleExpression(out, rel.getRHS());
    }

    /**
     * Descendants of this class can override this method to format predicate references differently.
     *
     * @param pred the reference to be formatted.
     */
    protected void formatPredicateReference(StringBuilder out, IPredicateReference pred) {
        IDSpecRef ref = (IDSpecRef)pred;
        out.append(ref.getPrintableReference());
    }

    /**
     * Descendants of this class can override this method to format expression references differently.
     *
     * @param pred the reference to be formatted.
     */
    protected void formatExpressionReference(StringBuilder out, IExpressionReference ref) {
        if (ref.isReferenceByName()) {
            out.append(PQLParser.quoteName(ref.getReferencedName()));
        } else {
            out.append(ref.getPrintableReference());
        }
    }

    /**
     * Descendants of this class can override this method to format attributes differently.
     *
     * @param attr the attribute to be formatted.
     */
    protected void formatAttribute(StringBuilder out, IAttribute attr) {
        out.append(attr.getObjectTypeName());
        if (attr.getObjectSubTypeName() != null
                && !(attr.getObjectSubTypeName().equalsIgnoreCase(attr.getObjectTypeName()))) {
            out.append('.');
            out.append(attr.getObjectSubTypeName());
        }
        out.append('.');
        if (UNQUOTED_ATTRIBUTES.contains(attr.getName().toLowerCase())) {
            out.append(attr.getName());
        } else {
            out.append(PQLParser.quoteName(attr.getName()));
        }
    }

    /**
     * Descendants of this class can override this method to format unknown expressions.
     *
     * @param pred the expression to be formatted.
     */
    protected void formatExpression(StringBuilder out, IExpression expr) {
        out.append(expr.toString());
    }

    /**
     * Descendants of this class can override this method to format functions differently
     *
     * @param func the function to be formatted
     */
    protected void formatFunction(StringBuilder out, IFunctionApplication func) {
        out.append("call_function (\"");
        out.append(func.getServiceName());
        out.append("\", \"");
        out.append(func.getFunctionName());
        out.append('\"');
        for (IExpression exp : func.getArguments()) {
            out.append(", ");
            formatSingleExpression(out, exp);
        }
        out.append(")");
    }

    /**
     * Descendants of this class can override this method to format unknown predicates.
     *
     * @param pred the predicate to be formatted.
     */
    protected void formatPredicate(StringBuilder out, IPredicate pred) {
        // Not much to do here - we're accepting something
        // we don't know, so using its toString looks like
        // our best bet. Subclasses may have a better idea, so they
        // can provide more meaningful printing here.
        put(pred);
    }

    /**
     * Takes a collection of objects to format, and returns an array of <code>String</code>
     * objects with the results of formatting.
     *
     * @param collection
     * @return
     */
    public static String[] format(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException("collection");
        }
        DomainObjectFormatter f = new DomainObjectFormatter();
        List<String> res = new ArrayList<String>(collection.size());
        for (Object i : collection) {
            f.reset();
            f.formatDef(i);
            res.add(f.getPQL());
        }
        return res.toArray(new String[res.size()]);
    }

    /**
     * Resets the formatter, prepearing it for the next object.
     */
    public void reset() {
        out = new StringBuilder();
    }

    // =============== PRIVATE UTILITY METHODS ===================
    private void put(Object... o) {
        for (Object obj : o) {
            out.append(obj);
        }
    }

    private void iput(Object... o) {
        indent();
        for (Object obj : o) {
            out.append(obj);
        }
    }

    private void nl() {
        out.append(lineSeparator);
    }

    private void nli() {
        nl();
        indent();
    }

    private void nli(Object... o) {
        nl();
        indent();
        for (Object obj : o) {
            out.append(obj);
        }
    }

    private void indent() {
        for (int i = 0; i != indent * spi; i++) {
            out.append(' ');
        }
    }

    private static String getLineSeparator() {
        try {
            return AccessController.doPrivileged(
                    new PrivilegedAction<String>() {

                        public String run() {
                            return System.getProperty("line.separator");
                        }
                    }
                    );
        } catch (Exception ex) {
            // Ignore the exception and use the default separator.
            return "\n";
        }
    }

    private static final String lineSeparator = getLineSeparator();

    private class PredicateVisitor implements IPredicateVisitor {

        private final Stack<String> opStack = new Stack<String>();
        private final Stack<Boolean> separatorStack = new Stack<Boolean>();
        private final IdentityHashMap<IPredicate, Object> seen = new IdentityHashMap<IPredicate, Object>();

        /**
         * Formats a composite predicate.
         *
         * @param pred the composite predicate to format.
         * @param preorder true when it's a pre-order visit call, false otherwise.
         */
        public void visit(ICompositePredicate pred, boolean preorder) {
            visit(pred, pred.getOp().toString(), preorder);
        }

        public void visit(ICompositePredicate pred, String opStr, boolean preorder) {
            if (preorder) {
                addInfixOps();
                if (seen.containsKey(pred)) {
                    throw new IllegalStateException("Objects with circular references cannot be formated AS PQL.");
                }
                seen.put(pred, null);
                if (pred.predicateCount() == 1) {
                    put(opStr, " (");
                } else {
                    put("(");
                }
                opStack.push(opStr);
                separatorStack.push(true);
            } else {
                seen.remove(pred);
                put(")");
                opStack.pop();
                separatorStack.pop();
            }
        }

        /**
         * Formats a spec reference.
         *
         * @param pred the predicate reference to format.
         */
        public void visit(IPredicateReference pred) {
            addInfixOps();
            formatPredicateReference(out, pred);
        }

        /**
         * Formats a relation spec.
         *
         * @param spec the relation spec to format.
         */
        public void visit(IRelation rel) {
            addInfixOps();
            formatRelation(out, rel);
        }

        /**
         * Formats a generic predicate.
         *
         * @param pred the generic predicate to format.
         */
        public void visit(IPredicate pred) {
            addInfixOps();
            formatPredicate(out, pred);
        }

        private void addInfixOps() {
            if (separatorStack.isEmpty()) {
                return;
            }
            if (!separatorStack.peek()) {
                // The two stacks grow and shrink at the same time:
                assert !opStack.empty();
                put(" ", opStack.peek(), " ");
            } else {
                separatorStack.pop();
                separatorStack.push(false);
            }
        }

    };

}
