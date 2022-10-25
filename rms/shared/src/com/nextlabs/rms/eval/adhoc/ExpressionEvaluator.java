package com.nextlabs.rms.eval.adhoc;

import com.nextlabs.nxl.FilePolicy.Operator;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {

    }

    public static boolean evaluate(Operator op, Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            return evaluate(op, ((Boolean)left).booleanValue(), ((Boolean)right).booleanValue());
        } else if (left instanceof String && right instanceof String) {
            return evaluate(op, (String)left, (String)right);
        } else if (left instanceof Number && right instanceof Number) {
            return evaluate(op, ((Number)left).doubleValue(), ((Number)right).doubleValue());
        }
        return false;
    }

    private static boolean evaluate(Operator op, boolean left, boolean right) {
        switch (op) {
            case EQ:
                return left == right;
            case NE:
                return left != right;
            default:
                return false;
        }
    }

    private static boolean evaluate(Operator op, String left, String right) {
        switch (op) {
            case EQ:
                return left.equals(right);
            case NE:
                return !left.equals(right);
            default:
                return false;
        }
    }

    private static boolean evaluate(Operator op, double left, double right) {
        switch (op) {
            case EQ:
                return left == right;
            case NE:
                return left != right;
            case GT:
                return left > right;
            case GE:
                return left >= right;
            case LT:
                return left < right;
            case LE:
                return left <= right;
            default:
                return false;
        }
    }
}
