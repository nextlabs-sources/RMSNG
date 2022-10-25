package com.bluejungle.framework.expressions;

/*
 * Created on Feb 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc., Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved worldwide.
 */

import com.bluejungle.framework.utils.StringUtils;

/**
 * @author sasha, sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/Relation.java#1 $:
 */

public class Relation implements IRelation {

    private IExpression lhs;
    private IExpression rhs;
    private RelationOp op;

    private static RelFunc BAD_TYPE = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            throw new RuntimeException("Incompatible types for operation.  LHS type: " + lhs.getType() + " value " + lhs.getValue() + " RHS type: " + rhs.getType() + " value: " + rhs.getValue());
        }
    };

    private static RelFunc TRUE_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return true;
        }
    };

    private static RelFunc FALSE_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return false;
        }
    };

    private static RelFunc EQUALS_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return lhs.getValue().equals(rhs.getValue());
        }
    };

    private static RelFunc NOT_EQUALS_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return !(lhs.getValue().equals(rhs.getValue()));
        }
    };

    // This code will do String matching if both values are strings and call .equals()
    // otherwise
    private static IMultivalue.IMultivalueEqual GENERIC_MATCHER = new IMultivalue.IMultivalueEqual() {

        public boolean equal(IEvalValue x, IEvalValue y) {
            if (x == null || y == null) {
                return x == y;
            }

            if (x.getType() == ValueType.STRING && y.getType() == ValueType.STRING) {
                return StringUtils.isMatch((String)x.getValue(), (String)y.getValue());
            } else {
                return y.equals(x);
            }
        }
    };

    private static RelFunc MULTIVAL_INCLUSION = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)lhs.getValue();
            return mval.includes(rhs, GENERIC_MATCHER);
        }
    };

    private static RelFunc VAL_INCLUSION = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = Multivalue.create(lhs.getValue());
            return mval.includes((IMultivalue)rhs.getValue(), GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_HAS_L = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)lhs.getValue();
            return mval.includes(rhs);
        }
    };

    private static RelFunc MULTIVAL_HAS_R = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)rhs.getValue();
            return mval.includes(lhs);
        }
    };

    private static RelFunc MULTIVAL_HAS_NOT_L = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)lhs.getValue();
            return !mval.includes(rhs);
        }
    };

    private static RelFunc MULTIVAL_HAS_NOT_R = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)rhs.getValue();
            return !mval.includes(lhs);
        }
    };

    private static RelFunc MULTIVAL_INCLUDES_MULTIVAL_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)lhs.getValue();
            IMultivalue r_mval = (IMultivalue)rhs.getValue();
            return mval.includes(r_mval, GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_EQUIV_MULTIVAL = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue l_mval = (IMultivalue)lhs.getValue();
            IMultivalue r_mval = (IMultivalue)rhs.getValue();
            return l_mval.includes(r_mval, GENERIC_MATCHER) && r_mval.includes(l_mval, GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_EQUIV_SINGLEVAL_L = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)lhs.getValue();

            return mval.size() == 1 && mval.includes(rhs, GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_EQUIV_SINGLEVAL_R = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            IMultivalue mval = (IMultivalue)rhs.getValue();

            return mval.size() == 1 && mval.includes(lhs, GENERIC_MATCHER);
        }
    };

    private static boolean stringMatchesMultival(String pattern, IMultivalue mval) {
        if (pattern == null) {
            return mval.includes(IEvalValue.NULL);
        }
        if (mval.getType() != ValueType.STRING) {
            return false;
        }
        for (IEvalValue val : mval) {
            String strVal = (String)val.getValue();
            if (strVal != null && StringUtils.isMatch(pattern, strVal)) {
                return true;
            }
        }
        return false;
    }

    private static RelFunc MULTIVAL_EQ_MULTIVAL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            IMultivalue lhs = (IMultivalue)lhsVal.getValue();
            IMultivalue rhs = (IMultivalue)rhsVal.getValue();
            return lhs.intersects(rhs, GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_NEQ_MULTIVAL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            IMultivalue lhs = (IMultivalue)lhsVal.getValue();
            IMultivalue rhs = (IMultivalue)rhsVal.getValue();
            return !lhs.intersects(rhs, GENERIC_MATCHER);
        }
    };

    private static RelFunc MULTIVAL_EQ_NULL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            return multivalRepresentsNull((IMultivalue)lhsVal.getValue());
        }
    };

    private static RelFunc MULTIVAL_NEQ_NULL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            return !multivalRepresentsNull((IMultivalue)lhsVal.getValue());
        }
    };

    private static RelFunc NULL_EQ_MULTIVAL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            return multivalRepresentsNull((IMultivalue)rhsVal.getValue());
        }
    };

    private static RelFunc NULL_NEQ_MULTIVAL = new RelFunc() {

        public boolean evaluate(IEvalValue lhsVal, IEvalValue rhsVal) {
            return !multivalRepresentsNull((IMultivalue)rhsVal.getValue());
        }
    };

    private static boolean multivalRepresentsNull(IMultivalue mv) {
        return mv.isEmpty() || (mv.size() == 1 && mv.includes(IEvalValue.NULL));
    }

    private static RelFunc MULTIVAL_MATCH_L = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return stringMatchesMultival((String)rhs.getValue(), (IMultivalue)lhs.getValue());
        }
    };

    private static RelFunc MULTIVAL_MATCH_R = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return stringMatchesMultival((String)lhs.getValue(), (IMultivalue)rhs.getValue());
        }
    };

    private static RelFunc MULTIVAL_NOT_MATCH_L = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return !stringMatchesMultival((String)rhs.getValue(), (IMultivalue)lhs.getValue());
        }
    };

    private static RelFunc MULTIVAL_NOT_MATCH_R = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return !stringMatchesMultival((String)lhs.getValue(), (IMultivalue)rhs.getValue());
        }
    };

    private static RelFunc STR_MATCH_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            String pat = (String)rhs.getValue();
            String str = (String)lhs.getValue();
            return StringUtils.isMatch(pat, str);
        }

    };

    private static RelFunc STR_NOT_MATCH_F = new RelFunc() {

        public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
            return !(STR_MATCH_F.evaluate(lhs, rhs));
        }
    };

    // dispatch table
    private static final RelFunc[][][] functions = new RelFunc[RelationOp.numElements()][ValueType.numElements()][ValueType.numElements()];
    static {

        // initialize everything to BAD_TYPE for convenience
        for (int i = 0; i < functions.length; i++) {
            for (int j = 0; j < functions[0].length; j++) {
                for (int k = 0; k < functions[0][0].length; k++) {
                    functions[i][j][k] = BAD_TYPE;
                }
            }
        }

        //*****************
        // EQUALS
        //*****************
        RelFunc[][] equals = functions[RelationOp.EQUALS.getType()];
        RelFunc[] equalsNull = equals[ValueType.NULL.getType()];
        equalsNull[ValueType.NULL.getType()] = TRUE_F;
        for (int i = 0; i < equalsNull.length; i++) {
            if (i != ValueType.NULL.getType()) {
                equalsNull[i] = FALSE_F;
            }
        }

        for (int lhsIdx = 0; lhsIdx < equals.length; lhsIdx++) {
            if (lhsIdx != ValueType.NULL.getType()) {
                RelFunc[] equalsLHS = equals[lhsIdx];
                for (int rhsIdx = 0; rhsIdx < equalsLHS.length; rhsIdx++) {
                    equalsLHS[rhsIdx] = EQUALS_F;
                }
            }
        }

        equals[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = MULTIVAL_EQ_NULL;
        equals[ValueType.NULL.getType()][ValueType.MULTIVAL.getType()] = NULL_EQ_MULTIVAL;
        equals[ValueType.MULTIVAL.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_EQ_MULTIVAL;
        equals[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_MATCH_L;
        equals[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_MATCH_R;
        equals[ValueType.STRING.getType()][ValueType.STRING.getType()] = STR_MATCH_F;
        equals[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() == Long.parseLong((String)(rhs.getValue()));
            }
        };
        equals[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) == (Long)rhs.getValue();
            }
        };
        equals[ValueType.STRING.getType()][ValueType.DATE.getType()] = equals[ValueType.STRING.getType()][ValueType.LONG.getType()];

        //*****************
        // NOT_EQUALS
        //*****************
        RelFunc[][] neq = functions[RelationOp.NOT_EQUALS.getType()];
        RelFunc[] neqNull = neq[ValueType.NULL.getType()];
        neqNull[ValueType.NULL.getType()] = FALSE_F;
        for (int i = 0; i < neqNull.length; i++) {
            if (i != ValueType.NULL.getType()) {
                neqNull[i] = TRUE_F;
            }
        }

        for (int lhsIdx = 0; lhsIdx < neq.length; lhsIdx++) {
            if (lhsIdx != ValueType.NULL.getType()) {
                RelFunc[] neqLHS = neq[lhsIdx];
                for (int rhsIdx = 0; rhsIdx < neqLHS.length; rhsIdx++) {
                    neqLHS[rhsIdx] = NOT_EQUALS_F;
                }
            }
        }

        neq[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = MULTIVAL_NEQ_NULL;
        neq[ValueType.NULL.getType()][ValueType.MULTIVAL.getType()] = NULL_NEQ_MULTIVAL;
        neq[ValueType.MULTIVAL.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_NEQ_MULTIVAL;
        neq[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_NOT_MATCH_L;
        neq[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_NOT_MATCH_R;
        neq[ValueType.STRING.getType()][ValueType.STRING.getType()] = STR_NOT_MATCH_F;
        neq[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (((Long)lhs.getValue()) != Long.parseLong((String)(rhs.getValue())));
            }
        };
        neq[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) != (Long)rhs.getValue();
            }
        };
        neq[ValueType.STRING.getType()][ValueType.DATE.getType()] = neq[ValueType.STRING.getType()][ValueType.LONG.getType()];

        //**************
        // LT
        //**************
        RelFunc[][] lt = functions[RelationOp.LESS_THAN.getType()];
        lt[ValueType.NULL.getType()][ValueType.NULL.getType()] = FALSE_F;
        lt[ValueType.NULL.getType()][ValueType.LONG.getType()] = FALSE_F;
        lt[ValueType.NULL.getType()][ValueType.STRING.getType()] = FALSE_F;
        lt[ValueType.NULL.getType()][ValueType.DATE.getType()] = FALSE_F;
        lt[ValueType.LONG.getType()][ValueType.NULL.getType()] = FALSE_F;
        lt[ValueType.STRING.getType()][ValueType.NULL.getType()] = FALSE_F;
        lt[ValueType.DATE.getType()][ValueType.NULL.getType()] = FALSE_F;
        lt[ValueType.LONG.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() < (Long)rhs.getValue();
            }
        };
        lt[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (((Long)lhs.getValue()).longValue() <
                Long.parseLong((String)(rhs.getValue())));
            }
        };
        lt[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long.parseLong((String)(lhs.getValue())) <
                ((Long)rhs.getValue()).longValue());
            }
        };
        lt[ValueType.STRING.getType()][ValueType.DATE.getType()] = lt[ValueType.STRING.getType()][ValueType.LONG.getType()];
        lt[ValueType.STRING.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long.parseLong((String)(lhs.getValue())) <
                Long.parseLong((String)(rhs.getValue())));
            }
        };
        lt[ValueType.DATE.getType()][ValueType.DATE.getType()] = lt[ValueType.LONG.getType()][ValueType.LONG.getType()];

        //************
        // LTE
        //************
        RelFunc[][] lte = functions[RelationOp.LESS_THAN_EQUALS.getType()];
        lte[ValueType.NULL.getType()][ValueType.NULL.getType()] = TRUE_F;
        lte[ValueType.NULL.getType()][ValueType.LONG.getType()] = FALSE_F;
        lte[ValueType.NULL.getType()][ValueType.STRING.getType()] = FALSE_F;
        lte[ValueType.NULL.getType()][ValueType.DATE.getType()] = FALSE_F;
        lte[ValueType.LONG.getType()][ValueType.NULL.getType()] = FALSE_F;
        lte[ValueType.STRING.getType()][ValueType.NULL.getType()] = FALSE_F;
        lte[ValueType.DATE.getType()][ValueType.NULL.getType()] = FALSE_F;
        lte[ValueType.LONG.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() <= (Long)rhs.getValue();
            }
        };
        lte[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() <= Long.parseLong((String)(rhs.getValue()));
            }
        };
        lte[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) <= (Long)rhs.getValue();
            }
        };
        lte[ValueType.STRING.getType()][ValueType.DATE.getType()] = lte[ValueType.STRING.getType()][ValueType.LONG.getType()];
        lte[ValueType.STRING.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) <= Long.parseLong((String)(rhs.getValue()));
            }
        };
        lte[ValueType.DATE.getType()][ValueType.DATE.getType()] = lte[ValueType.LONG.getType()][ValueType.LONG.getType()];

        //**************
        // GT
        //**************
        RelFunc[][] gt = functions[RelationOp.GREATER_THAN.getType()];
        gt[ValueType.NULL.getType()][ValueType.NULL.getType()] = FALSE_F;
        gt[ValueType.NULL.getType()][ValueType.LONG.getType()] = FALSE_F;
        gt[ValueType.NULL.getType()][ValueType.STRING.getType()] = FALSE_F;
        gt[ValueType.NULL.getType()][ValueType.DATE.getType()] = FALSE_F;
        gt[ValueType.LONG.getType()][ValueType.NULL.getType()] = FALSE_F;
        gt[ValueType.STRING.getType()][ValueType.NULL.getType()] = FALSE_F;
        gt[ValueType.DATE.getType()][ValueType.NULL.getType()] = FALSE_F;
        gt[ValueType.LONG.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() > (Long)rhs.getValue();
            }
        };
        gt[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() > Long.parseLong((String)(rhs.getValue()));
            }
        };
        gt[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) > (Long)rhs.getValue();
            }
        };
        gt[ValueType.STRING.getType()][ValueType.DATE.getType()] = gt[ValueType.STRING.getType()][ValueType.LONG.getType()];
        gt[ValueType.STRING.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) > Long.parseLong((String)(rhs.getValue()));
            }
        };
        gt[ValueType.DATE.getType()][ValueType.DATE.getType()] = gt[ValueType.LONG.getType()][ValueType.LONG.getType()];

        //**************
        // GTE
        //**************
        RelFunc[][] gte = functions[RelationOp.GREATER_THAN_EQUALS.getType()];
        gte[ValueType.NULL.getType()][ValueType.NULL.getType()] = TRUE_F;
        gte[ValueType.NULL.getType()][ValueType.LONG.getType()] = FALSE_F;
        gte[ValueType.NULL.getType()][ValueType.STRING.getType()] = FALSE_F;
        gte[ValueType.NULL.getType()][ValueType.DATE.getType()] = FALSE_F;
        gte[ValueType.LONG.getType()][ValueType.NULL.getType()] = FALSE_F;
        gte[ValueType.STRING.getType()][ValueType.NULL.getType()] = FALSE_F;
        gte[ValueType.DATE.getType()][ValueType.NULL.getType()] = FALSE_F;
        gte[ValueType.LONG.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() >= (Long)rhs.getValue();
            }
        };
        gte[ValueType.LONG.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long)lhs.getValue() >= Long.parseLong((String)(rhs.getValue()));
            }
        };
        gte[ValueType.STRING.getType()][ValueType.LONG.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return Long.parseLong((String)(lhs.getValue())) >= (Long)rhs.getValue();
            }
        };
        gte[ValueType.STRING.getType()][ValueType.DATE.getType()] = gte[ValueType.STRING.getType()][ValueType.LONG.getType()];
        gte[ValueType.STRING.getType()][ValueType.STRING.getType()] = new RelFunc() {

            public boolean evaluate(IEvalValue lhs, IEvalValue rhs) {
                return (Long.parseLong((String)(lhs.getValue())) >= Long.parseLong((String)(rhs.getValue())));
            }
        };
        gte[ValueType.DATE.getType()][ValueType.DATE.getType()] = gte[ValueType.LONG.getType()][ValueType.LONG.getType()];

        //**************
        // HAS
        //**************
        RelFunc[][] has = functions[RelationOp.HAS.getType()];
        has[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = FALSE_F;
        has[ValueType.MULTIVAL.getType()][ValueType.LONG.getType()] = MULTIVAL_HAS_L;
        has[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_HAS_L;
        has[ValueType.MULTIVAL.getType()][ValueType.DATE.getType()] = MULTIVAL_HAS_L;
        has[ValueType.NULL.getType()][ValueType.MULTIVAL.getType()] = FALSE_F;
        has[ValueType.LONG.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_R;
        has[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_R;
        has[ValueType.DATE.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_R;

        //**************
        // DOES_NOT_HAVE
        //**************
        RelFunc[][] dnh = functions[RelationOp.DOES_NOT_HAVE.getType()];
        dnh[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = FALSE_F;
        dnh[ValueType.MULTIVAL.getType()][ValueType.LONG.getType()] = MULTIVAL_HAS_NOT_L;
        dnh[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_HAS_NOT_L;
        dnh[ValueType.MULTIVAL.getType()][ValueType.DATE.getType()] = MULTIVAL_HAS_NOT_L;
        dnh[ValueType.NULL.getType()][ValueType.MULTIVAL.getType()] = FALSE_F;
        dnh[ValueType.LONG.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_NOT_R;
        dnh[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_NOT_R;
        dnh[ValueType.DATE.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_HAS_NOT_R;

        //*********
        // INCLUDES
        //*********
        RelFunc[][] includes_all = functions[RelationOp.INCLUDES.getType()];
        includes_all[ValueType.MULTIVAL.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_INCLUDES_MULTIVAL_F;
        includes_all[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = TRUE_F;
        includes_all[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_INCLUSION;
        includes_all[ValueType.MULTIVAL.getType()][ValueType.LONG.getType()] = MULTIVAL_INCLUSION;
        includes_all[ValueType.MULTIVAL.getType()][ValueType.DATE.getType()] = MULTIVAL_INCLUSION;
        // It may seem odd to have "SV includes MV", but the person writing the policy
        // side really will contain multiple values or could be just a single value, so we should do our best to make
        // it work
        includes_all[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = VAL_INCLUSION;
        includes_all[ValueType.STRING.getType()][ValueType.STRING.getType()] = STR_MATCH_F;
        includes_all[ValueType.LONG.getType()][ValueType.MULTIVAL.getType()] = VAL_INCLUSION;
        includes_all[ValueType.LONG.getType()][ValueType.LONG.getType()] = EQUALS_F;
        includes_all[ValueType.DATE.getType()][ValueType.MULTIVAL.getType()] = VAL_INCLUSION;
        includes_all[ValueType.DATE.getType()][ValueType.DATE.getType()] = EQUALS_F;

        //*********************
        // MULTIVAL EQUIVALENCE
        //*********************
        RelFunc[][] equiv = functions[RelationOp.EQUALS_UNORDERED.getType()];
        equiv[ValueType.MULTIVAL.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_EQUIV_MULTIVAL;
        equiv[ValueType.MULTIVAL.getType()][ValueType.NULL.getType()] = FALSE_F;
        equiv[ValueType.MULTIVAL.getType()][ValueType.STRING.getType()] = MULTIVAL_EQUIV_SINGLEVAL_L;
        equiv[ValueType.MULTIVAL.getType()][ValueType.LONG.getType()] = MULTIVAL_EQUIV_SINGLEVAL_L;
        equiv[ValueType.MULTIVAL.getType()][ValueType.DATE.getType()] = MULTIVAL_EQUIV_SINGLEVAL_L;
        equiv[ValueType.NULL.getType()][ValueType.MULTIVAL.getType()] = FALSE_F;
        equiv[ValueType.LONG.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_EQUIV_SINGLEVAL_R;
        equiv[ValueType.STRING.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_EQUIV_SINGLEVAL_R;
        equiv[ValueType.DATE.getType()][ValueType.MULTIVAL.getType()] = MULTIVAL_EQUIV_SINGLEVAL_R;
    }

    public Relation(RelationOp op, IExpression lhs, IExpression rhs) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public final boolean match(IArguments arg) {

        IEvalValue lhsVal = lhs.evaluate(arg);
        IEvalValue rhsVal = rhs.evaluate(arg);

        RelFunc f = functions[op.getType()][lhsVal.getType().getType()][rhsVal.getType().getType()];
        return f.evaluate(lhsVal, rhsVal);
    }

    public void accept(IPredicateVisitor visitor, IPredicateVisitor.Order order) {
        visitor.visit(this);
    }

    public RelationOp getOp() {
        return op;
    }

    public IExpression getLHS() {
        return lhs;
    }

    public IExpression getRHS() {
        return rhs;
    }

    private interface RelFunc {

        boolean evaluate(IEvalValue lhs, IEvalValue rhs);
    }

    /**
     * @param op
     */
    public void setOp(RelationOp op) {
        this.op = op;
    }

    /**
     * @param lhs
     */
    public void setLHS(IExpression lhs) {
        this.lhs = lhs;
    }

    /**
     * Sets the rhs
     * @param rhs The rhs to set.
     */
    public void setRHS(IExpression rhs) {
        this.rhs = rhs;
    }

    public String toString() {
        return lhs + " " + op + " " + rhs;
    }
}
