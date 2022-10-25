package com.nextlabs.nxl;

import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FilePolicy {

    private String version;
    private String issuer;
    private Date issueTime;
    private List<Policy> policies;

    public FilePolicy(String issuer) {
        this.issuer = issuer;
        version = "1.0";
        issueTime = new Date();
        policies = new ArrayList<Policy>(1);
    }

    public FilePolicy(String issuer, int policyId, String name, Rights[] rights, String watermark, JsonExpiry expiry) {
        this(issuer);
        List<Obligation> obligations = new ArrayList<Obligation>(1);
        Policy policy = new Policy(policyId, name, Policy.ACTION_GRANT, rights, obligations);
        if (watermark != null) {
            watermark = StringUtils.normalize(watermark).trim();
            policy.addWatermark(watermark);
        }
        if (expiry != null) {
            policy.addExpiry(expiry);
        }
        policies.add(policy);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssueTime(Date issueTime) {
        this.issueTime = issueTime;
    }

    public Date getIssueTime() {
        return issueTime;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public static final class Policy {

        public static final int ACTION_REVOKE = 0;
        public static final int ACTION_GRANT = 1;
        public static final String NAME_ENV_DATE = "environment.date";

        private int id;
        private String name;
        private int action;
        private Rights[] rights;
        private Conditions conditions;
        private List<Obligation> obligations;

        public Policy(int id, String name, int action, Rights[] rights, List<Obligation> obligations) {
            this.id = id;
            this.name = name;
            this.action = action;
            this.obligations = obligations;
            setRights(rights);
            conditions = new Conditions();
            Expression subject = new Expression();
            subject.setType(Expression.TYPE_PROPERTY);
            subject.setOperator(Operator.EQ.getValue());
            subject.setName("application.is_associated_app");
            subject.setValue(true);
            conditions.setSubject(subject);
        }

        public boolean needsWatermark() {
            for (Obligation obligation : obligations) {
                if (Obligation.WATERMARK.equals(obligation.getName())) {
                    return true;
                }
            }
            return false;
        }

        public void addWatermark(String watermark) {
            Map<String, Object> value = new HashMap<String, Object>();
            value.put(Obligation.WATERMARK_TEXT_KEY, watermark);
            obligations.add(new Obligation(Obligation.WATERMARK, value));
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setAction(int action) {
            this.action = action;
        }

        public int getAction() {
            return action;
        }

        public void setRights(Rights[] rights) {
            HashSet<Rights> set = new HashSet<>();
            if (rights != null && rights.length > 0) {
                for (Rights right : rights) {
                    if (right == Rights.WATERMARK) {
                        continue;
                    } else {
                        set.add(right);
                    }
                }
            }
            this.rights = set.toArray(new Rights[set.size()]);
        }

        public Rights[] getRights() {
            return rights;
        }

        public void setConditions(FilePolicy.Conditions conditions) {
            this.conditions = conditions;
        }

        public FilePolicy.Conditions getConditions() {
            return conditions;
        }

        public void setObligations(List<FilePolicy.Obligation> obligations) {
            this.obligations = obligations;
        }

        public List<FilePolicy.Obligation> getObligations() {
            return obligations;
        }

        public void addExpiry(JsonExpiry expiry) {
            Expression environment = new Expression();
            int option = expiry.getOption();
            switch (option) {
                case 1:
                case 2:
                    environment = constructEndDateExpression(expiry.getEndDate());
                    break;
                case 3:
                    environment.setType(Expression.TYPE_LOGIC);
                    environment.setOperator(Operator.AND.getValue());
                    Expression endDateExpression = constructEndDateExpression(expiry.getEndDate());
                    Expression startDateExpression = constructStartDateExpression(expiry.getStartDate());
                    List<Expression> expressions = new LinkedList<Expression>();
                    expressions.add(endDateExpression);
                    expressions.add(startDateExpression);
                    environment.setExpressions(expressions);
                    break;
                default:
                    environment = null;
                    break;
            }
            conditions.setEnvironment(environment);
        }

        private Expression constructEndDateExpression(long endDate) {
            return constructDateExpression(endDate, Operator.LE.getValue());
        }

        private Expression constructStartDateExpression(long startDate) {
            return constructDateExpression(startDate, Operator.GE.getValue());
        }

        private Expression constructDateExpression(long date, String operator) {
            Expression expression = new Expression();
            expression.setType(Expression.TYPE_PROPERTY);
            expression.setOperator(operator);
            expression.setName(Expression.NAME_ENV_DATE);
            expression.setValue(date);
            return expression;
        }
    }

    public static final class Conditions {

        private Expression subject;
        private Expression resource;
        private Expression environment;

        public Conditions() {
        }

        public void setSubject(FilePolicy.Expression subject) {
            this.subject = subject;
        }

        public FilePolicy.Expression getSubject() {
            return subject;
        }

        public void setResource(FilePolicy.Expression resource) {
            this.resource = resource;
        }

        public FilePolicy.Expression getResource() {
            return resource;
        }

        public void setEnvironment(FilePolicy.Expression environment) {
            this.environment = environment;
        }

        public FilePolicy.Expression getEnvironment() {
            return environment;
        }
    }

    public static final class Expression {

        public static final int TYPE_LOGIC = 0;
        public static final int TYPE_PROPERTY = 1;

        public static final String NAME_ENV_DATE = "environment.date";

        private int type;
        private String operator;
        private String name;
        private Object value;
        private List<Expression> expressions;

        public Expression() {
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setExpressions(List<FilePolicy.Expression> expressions) {
            this.expressions = expressions;
        }

        public List<FilePolicy.Expression> getExpressions() {
            return expressions;
        }
    }

    public static final class Obligation {

        public static final String WATERMARK = "WATERMARK";
        public static final String WATERMARK_TEXT_KEY = "text";

        private String name;
        private Map<String, Object> value;

        public Obligation() {
        }

        public Obligation(String name, Map<String, Object> value) {
            this.name = name;
            this.value = value;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getValue() {
            return value;
        }

        public void setValue(Map<String, Object> value) {
            this.value = value;
        }
    }

    public enum Operator {
        EQ("="),
        NE("!="),
        GT(">"),
        GE(">="),
        LT("<"),
        LE("<="),
        AND("&&"),
        OR("||");

        private String value;
        private static final Map<String, Operator> LOOKUP = new HashMap<String, Operator>();

        static {
            for (Operator o : values()) {
                LOOKUP.put(o.getValue(), o);
            }
        }

        private Operator(String val) {
            this.value = val;
        }

        public String getValue() {
            return value;
        }

        public static Operator get(String op) {
            return LOOKUP.get(op);
        }

    }
}
