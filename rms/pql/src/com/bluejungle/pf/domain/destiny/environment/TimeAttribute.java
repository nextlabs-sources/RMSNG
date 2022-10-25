/*
 * Created on Apr 20, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.environment;

import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IAttribute;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IExpressionVisitor.Order;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.patterns.EnumBase;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/environment/TimeAttribute.java#1 $:
 */

public abstract class TimeAttribute extends EnumBase implements IAttribute {

    public static final TimeAttribute IDENTITY = new TimeAttribute("identity") {

        public IEvalValue evaluate(IArguments arg) {
            return EvalValue.build(new Date());
        }

        public IExpression build(String value) {
            Date dateVal;
            try {
                dateVal = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).parse(value);
            } catch (ParseException e) {
                throw new RuntimeException("invalid date/time specified: " + value, e);
            }
            return Constant.build(dateVal, '"' + value + '"');
        }
    };

    public static final TimeAttribute YEAR = new TimeAttribute("year") {

        public IExpression build(String value) {
            try {
                long num = Long.parseLong(value);
                return Constant.build(num);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid year specified: " + value + " expected a number");
            }
        }

        public IEvalValue evaluate(IArguments arg) {
            return EvalValue.build(Calendar.getInstance().get(Calendar.YEAR));
        }

    };
    public static final TimeAttribute WEEKDAY = new TimeAttribute("weekday") {

        // Weekday is stored as a long value
        public IExpression build(String value) {
            // first check if number
            try {
                long num = Long.parseLong(value);
                return Constant.build(num);
            } catch (NumberFormatException e) {
                // if not number, could still be ok
            }
            Long num = (Long)WEEKDAY_MAP.get(value.toLowerCase());
            if (num == null) {
                throw new RuntimeException("Invalid weekday specified: " + value);
            }
            return Constant.build(num.longValue(), '"' + value + '"');

        }

        public IEvalValue evaluate(IArguments arg) {
            return EvalValue.build(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        }

    };

    /**
     * Time of day, i.e. 05:00 PM
     */
    public static final TimeAttribute TIME = new TimeAttribute("time") {

        // time of day is stored as number of milliseconds, between 0 and MILLIS_PER_DAY - 1;
        public IExpression build(String value) {
            Date timeVal;
            DateFormat df;
            try {
                df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                timeVal = df.parse(value);
            } catch (ParseException e) {
                throw new RuntimeException("invalid time specified: " + value, e);
            }
            return Constant.build(timeVal.getTime() + df.getTimeZone().getRawOffset(), '"' + value + '"');
        }

        public IEvalValue evaluate(IArguments arg) {
            Calendar now = Calendar.getInstance();
            long time = now.get(Calendar.HOUR_OF_DAY) * MILLIS_PER_HOUR + now.get(Calendar.MINUTE) * MILLIS_PER_MINUTE +
                    now.get(Calendar.SECOND) * MILLIS_PER_SECOND + now.get(Calendar.MILLISECOND);
            return EvalValue.build(time);
        }

    };

    /**
     * date within a month, i.e. 16th
     */
    public static final TimeAttribute DATE = new TimeAttribute("date") {

        // date is simply a long

        public IExpression build(String value) {
            try {
                return Constant.build(Long.parseLong(value), value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("invalid date specified: " + value, e);
            }
        }

        public IEvalValue evaluate(IArguments arg) {
            return EvalValue.build(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }

    };

    /**
     * day of week in month, i.e. the number 2 in "every 2nd monday of the month"
     * negative numbers count from the end of the month, so "last monday of the month" is -1
     */
    public static final TimeAttribute DOWIM = new TimeAttribute("day_in_month") {

        // stored as long
        public IExpression build(String value) {
            try {
                return Constant.build(Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new RuntimeException("invalid day_in_month specified: " + value, e);
            }
        }

        public IEvalValue evaluate(IArguments arg) {
            return EvalValue.build(Calendar.getInstance().get(Calendar.DAY_OF_WEEK_IN_MONTH));
        }

    };

    public IRelation buildRelation(RelationOp op, String value) {
        IExpression rhs = build(value);
        return new Relation(op, this, rhs);
    }

    public IRelation buildRelation(RelationOp op, IExpression expr) {
        if (expr instanceof Constant) {
            return buildRelation(op, ((Constant)expr).getRepresentation());
        } else {
            return new Relation(op, this, expr);
        }
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#acceptVisitor(com.bluejungle.framework.expressions.IExpressionVisitor, com.bluejungle.framework.expressions.IExpressionVisitor.Order)
     */
    public void acceptVisitor(IExpressionVisitor visitor, Order order) {
        visitor.visit((IAttribute)this);
    }

    /**
     * @see com.bluejungle.framework.expressions.IAttribute#getObjectTypeName()
     */
    public String getObjectTypeName() {
        return "CURRENT_TIME";
    }

    /**
     * @see com.bluejungle.framework.expressions.IAttribute#getObjectTypeName()
     */
    public String getObjectSubTypeName() {
        return null;
    }

    public static TimeAttribute getElement(String name) {
        return getElement(name, TimeAttribute.class);
    }

    public static boolean existsElement(String name) {
        return existsElement(name, TimeAttribute.class);
    }

    private TimeAttribute(String name) {
        super(name, TimeAttribute.class);
    }

    public abstract IExpression build(String value);

    protected static final HashMap<String, Long> WEEKDAY_MAP = new HashMap<String, Long>();
    protected static final long MILLIS_PER_SECOND = 1000;
    protected static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    protected static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

    static {
        String[] weekdays = new DateFormatSymbols(Locale.US).getWeekdays();
        for (int i = 0; i < weekdays.length; i++) {
            WEEKDAY_MAP.put(weekdays[i].toLowerCase(), new Long(i));
        }
    }
}
