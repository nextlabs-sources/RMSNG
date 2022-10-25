package com.nextlabs.rms.hibernate;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LikeExpression;
import org.hibernate.criterion.MatchMode;

public final class EscapedLikeRestrictions {

    private EscapedLikeRestrictions() {
    }

    public static Criterion like(String propertyName, String value, MatchMode matchMode) {
        return like(propertyName, value, matchMode, false);
    }

    public static Criterion ilike(String propertyName, String value, MatchMode matchMode) {
        return like(propertyName, value, matchMode, true);
    }

    @SuppressWarnings("serial")
    private static Criterion like(String propertyName, String value, MatchMode matchMode, boolean ignoreCase) {
        return new LikeExpression(propertyName, escape(value), matchMode, '!', ignoreCase) {
        };
    }

    private static String escape(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_").replaceAll(" +", "%");
    }
}
