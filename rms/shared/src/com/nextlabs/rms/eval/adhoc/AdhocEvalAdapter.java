package com.nextlabs.rms.eval.adhoc;

import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Conditions;
import com.nextlabs.nxl.FilePolicy.Expression;
import com.nextlabs.nxl.FilePolicy.Obligation;
import com.nextlabs.nxl.FilePolicy.Operator;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.PolicyEvalException;
import com.nextlabs.rms.shared.LogConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AdhocEvalAdapter {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public static final String USR_ATTR_DOC_STEWARD = "documentSteward";

    private AdhocEvalAdapter() {

    }

    public static boolean isFileExpired(FilePolicy filePolicy) {
        if (filePolicy != null) {
            JsonExpiry expiry = getFirstPolicyExpiry(filePolicy);
            long now = new Date().getTime();
            if (expiry.getEndDate() != null) {
                return now > expiry.getEndDate();
            }
        }
        return false;
    }

    public static boolean isNotYetValid(FilePolicy filePolicy) {
        if (filePolicy != null) {
            JsonExpiry expiry = getFirstPolicyExpiry(filePolicy);
            long now = new Date().getTime();
            if (expiry.getStartDate() != null) {
                return now < expiry.getStartDate();
            }
        }
        return false;
    }

    public static JsonExpiry getFirstPolicyExpiry(FilePolicy filePolicy) {
        List<Policy> policies = filePolicy.getPolicies();
        JsonExpiry expiry = new JsonExpiry();
        if (policies == null || policies.isEmpty()) {
            return expiry;
        }
        Policy policy = filePolicy.getPolicies().get(0);
        Conditions conditions = policy.getConditions();
        if (conditions == null) {
            return expiry;
        }
        Expression environment = conditions.getEnvironment();
        if (environment == null) {
            return expiry;
        }
        return constructJsonExpiry(environment);
    }

    private static JsonExpiry constructJsonExpiry(Expression expression) {
        JsonExpiry expiry = new JsonExpiry();
        if (expression.getType() == Expression.TYPE_PROPERTY && StringUtils.equals(expression.getName(), Expression.NAME_ENV_DATE)) {
            Object value = expression.getValue();
            Long val = value == null ? null : value instanceof Number ? Number.class.cast(value).longValue() : new BigDecimal(value.toString()).longValue();
            if (Operator.GE.getValue().equalsIgnoreCase(expression.getOperator()) || Operator.GT.getValue().equalsIgnoreCase(expression.getOperator())) {
                expiry.setStartDate(val);
            } else if (Operator.LE.getValue().equalsIgnoreCase(expression.getOperator()) || Operator.LT.getValue().equalsIgnoreCase(expression.getOperator())) {
                expiry.setEndDate(val);
            }
        } else if (expression.getType() == Expression.TYPE_LOGIC && Operator.AND.getValue().equalsIgnoreCase(expression.getOperator())) {
            List<JsonExpiry> jsonArr = new ArrayList<>();
            for (Expression subExp : expression.getExpressions()) {
                jsonArr.add(constructJsonExpiry(subExp));
            }
            for (JsonExpiry jsonExpiry : jsonArr) {
                if (expiry.getStartDate() == null || (jsonExpiry.getStartDate() != null && expiry.getStartDate() < jsonExpiry.getStartDate())) {
                    expiry.setStartDate(jsonExpiry.getStartDate());
                }
                if (expiry.getEndDate() == null || (jsonExpiry.getEndDate() != null && expiry.getEndDate() > jsonExpiry.getEndDate())) {
                    expiry.setEndDate(jsonExpiry.getEndDate());
                }
            }
        } else if (expression.getType() == Expression.TYPE_LOGIC && Operator.OR.getValue().equalsIgnoreCase(expression.getOperator())) {
            List<JsonExpiry> jsonArr = new ArrayList<>();
            for (Expression subExp : expression.getExpressions()) {
                jsonArr.add(constructJsonExpiry(subExp));
            }
            for (JsonExpiry jsonExpiry : jsonArr) {
                if (expiry.getStartDate() == null || (jsonExpiry.getStartDate() != null && expiry.getStartDate() > jsonExpiry.getStartDate())) {
                    expiry.setStartDate(jsonExpiry.getStartDate());
                }
                if (expiry.getEndDate() == null || (jsonExpiry.getEndDate() != null && expiry.getEndDate() < jsonExpiry.getEndDate())) {
                    expiry.setEndDate(jsonExpiry.getEndDate());
                }
            }
        }
        return expiry;
    }

    public static EvalResponse evaluate(FilePolicy filePolicy, Map<String, Object> userAttributes) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Evaluating file policy: {}", GsonUtils.GSON.toJson(filePolicy, FilePolicy.class));
        }

        Map<String, Object> envAttributes = new HashMap<>();
        envAttributes.put(Policy.NAME_ENV_DATE, new Date().getTime());

        boolean isDocumentSteward = userAttributes != null && ((Boolean)userAttributes.get(USR_ATTR_DOC_STEWARD)).booleanValue();

        List<Policy> policies = filePolicy.getPolicies();
        if (policies == null) {
            return isDocumentSteward ? new EvalResponse(Rights.all()) : new EvalResponse();
        }

        Set<Rights> grant = new HashSet<>();
        Set<Rights> revoke = new HashSet<>();
        List<Obligation> obligations = new ArrayList<>();

        for (Policy policy : policies) {
            Rights[] rights = policy.getRights();
            if (rights != null) {
                try {
                    boolean matched = isDocumentSteward || evaluatePolicy(policy, envAttributes);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Policy ({}:{}) is evaluated to {}.", policy.getId(), policy.getName(), matched);
                    }
                    if (matched) {
                        if (policy.getAction() == Policy.ACTION_GRANT) {
                            Collections.addAll(grant, rights);
                            List<Obligation> obligationList = policy.getObligations();
                            if (obligationList != null && !obligationList.isEmpty()) {
                                obligations.addAll(policy.getObligations());
                                if (policy.needsWatermark()) {
                                    grant.add(Rights.WATERMARK);
                                }
                            }
                        } else {
                            Collections.addAll(revoke, rights);
                        }
                    } else {
                        Collections.addAll(revoke, rights);
                    }
                } catch (PolicyEvalException e) {
                    LOGGER.error("Error occurred while parsing policy: {}", e.getMessage());
                    grant.clear();
                }
            }
        }

        grant.removeAll(revoke);
        EvalResponse response = new EvalResponse(grant.toArray(new Rights[grant.size()]));
        response.addAdHocObligations(obligations);
        return response;
    }

    public static EvalResponse evaluate(FilePolicy filePolicy, boolean documentSteward) {
        Map<String, Object> usrAttrs = new HashMap<>(1);
        usrAttrs.put(AdhocEvalAdapter.USR_ATTR_DOC_STEWARD, documentSteward);
        return evaluate(filePolicy, usrAttrs);
    }

    private static boolean evaluatePolicy(Policy policy, Map<String, Object> envAttributes) throws PolicyEvalException {
        Conditions conditions = policy.getConditions();
        if (conditions == null) {
            return policy.getAction() == Policy.ACTION_GRANT;
        }
        Expression environment = conditions.getEnvironment();
        if (environment != null) {
            boolean result = evaluateExpression(environment, envAttributes);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Environment Expression is evaluated to {}", result);
            }
            return result;
        }
        return true;
    }

    private static boolean evaluateExpression(Expression expression, Map<String, Object> attributes)
            throws PolicyEvalException {
        if (expression.getType() == Expression.TYPE_PROPERTY) {
            List<Expression> expressions = expression.getExpressions();
            if (expressions != null && !expressions.isEmpty()) {
                throw new PolicyEvalException("Expression Type is TYPE_PROPERTY but sub expressions are present.");
            }
            String name = expression.getName();
            Operator operator = Operator.get(expression.getOperator());
            Object value = expression.getValue();
            if (name == null || operator == null || value == null) {
                throw new PolicyEvalException("Malformed expression.");
            }
            Object attribVal = attributes.get(name.toLowerCase());
            if (attribVal == null) {
                throw new PolicyEvalException("No matching attribute found for name: " + name);
            }
            boolean result = ExpressionEvaluator.evaluate(operator, attribVal, value);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Expression '{} ({}) {} {}' is evaluated to {}", name, attribVal, expression.getOperator(), value, result);
            }
            return result;
        } else {
            List<Expression> expressions = expression.getExpressions();
            if (expressions == null || expressions.isEmpty()) {
                throw new PolicyEvalException("Expression Type is TYPE_LOGIC but sub expressions are absent.");
            }
            Operator operator = Operator.get(expression.getOperator());
            if (Operator.OR != operator && Operator.AND != operator) {
                throw new PolicyEvalException("Invalid Operator " + operator + " for Expression TYPE_LOGIC.");
            }
            boolean lastEvalResult = false;
            for (Expression e : expressions) {
                lastEvalResult = evaluateExpression(e, attributes);
                if ((lastEvalResult && operator == Operator.OR) || (!lastEvalResult && operator == Operator.AND)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Skipping the rest of the expressions since an expression is already evaluated to {}.", lastEvalResult);
                    }
                    return lastEvalResult;
                }
            }
            return lastEvalResult;
        }
    }
}
