/*
 * Created on Feb 4, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Instances of this class keep unmodifiable dates.
 *
 * @author sergey
 */

public class UnmodifiableDate extends Date implements java.io.Serializable, Cloneable, Comparable<Date> {

    /**
     * This class is serialized in the same way its base is serialized.
     */
    private static final long serialVersionUID = 1L;

    /** Represents the earliest date the <code>Date</code> class can represent. */
    public static final Date START_OF_TIME = forTime(0);
    /** Represents the date of Dec-31, 9999. */
    public static final Date END_OF_TIME = forDate(getEndOfTime());

    public static UnmodifiableDate forDate(Calendar date) {
        return (date != null) ? forDate(date.getTime()) : null;
    }

    /**
     * Allocates an <code>UnmodifiableDate</code> object
     * and initializes it to represent the same time as the
     * <code>Date</code> parameter.
     *
     * @param   date   the <code>Date</code> object to be copied.
     */
    public static UnmodifiableDate forDate(Date date) {
        return (date != null) ? new UnmodifiableDate(date.getTime()) : null;
    }

    /**
     * Allocates an <code>UnmodifiableDate</code> object
     * and initializes it to represent the specified number
     * of milliseconds since the standard base time
     * known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     *
     * @param   time   the milliseconds since January 1, 1970, 00:00:00 GMT.
     * @see     java.lang.System#currentTimeMillis()
     */
    public static UnmodifiableDate forTime(long time) {
        return new UnmodifiableDate(time);
    }

    /**
     * Allocates an <code>UnmodifiableDate</code> object
     * and initializes it so that it represents the time
     * at which it was allocated, measured to the nearest millisecond.
     *
     * @see     java.lang.System#currentTimeMillis()
     */
    public static UnmodifiableDate current() {
        return new UnmodifiableDate(System.currentTimeMillis());
    }

    /**
     * Allocates an <code>UnmodifiableDate</code> object
     * and initializes it to represent the specified number
     * of milliseconds since the standard base time
     * known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     *
     * @param   time   the milliseconds since January 1, 1970, 00:00:00 GMT.
     * @see     java.lang.System#currentTimeMillis()
     */
    private UnmodifiableDate(long time) {
        super(time);
    }

    /**
     * Tests if this date is after the specified date.
     *
     * @param   when   a date.
     * @return  <code>true</code> if and only if the instant represented
     *          by this <tt>Date</tt> object is strictly later than the
     *          instant represented by <tt>when</tt>;
     *          <code>false</code> otherwise.
     */
    public boolean after(Date when) {
        return super.after(when);
    }

    /**
     * Tests if this date is before the specified date.
     *
     * @param   when   a date.
     * @return  <code>true</code> if and only if the instant of time
     *            represented by this <tt>Date</tt> object is strictly
     *            earlier than the instant represented by <tt>when</tt>;
     *          <code>false</code> otherwise.
     */
    public boolean before(Date when) {
        return super.before(when);
    }

    /**
     * Return a copy of this object.
     */
    public Object clone() {
        return super.clone();
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this <tt>Date</tt> object.
     *
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *          represented by this date.
     */
    public long getTime() {
        return super.getTime();
    }

    /**
     * Compares two Dates for ordering.
     *
     * @param   anotherDate   the <code>Date</code> to be compared.
     * @return  the value <code>0</code> if the argument Date is equal to
     *          this Date; a value less than <code>0</code> if this Date
     *          is before the Date argument; and a value greater than
     *      <code>0</code> if this Date is after the Date argument.
     */
    public int compareTo(Date anotherDate) {
        return super.compareTo(anotherDate);
    }

    /**
     * Compares two dates for equality.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Date</code> object that
     * represents the same point in time, to the millisecond, as this object.
     * <p>
     * Thus, two <code>Date</code> objects are equal if and only if the
     * <code>getTime</code> method returns the same <code>long</code>
     * value for both.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     * @see     java.util.Date#getTime()
     */
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Returns a hash code value for this object. The result is the
     * exclusive OR of the two halves of the primitive <tt>long</tt>
     * value returned by the {@link Date#getTime}
     * method. That is, the hash code is the value of the expression:
     * <blockquote><pre>
     * (int)(this.getTime()^(this.getTime() >>> 32))</pre></blockquote>
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Converts this <code>Date</code> object to a <code>String</code>.
     * The string has the following form:
     * <blockquote><pre>
     * dow mon dd hh:mm:ss zzz yyyy</pre></blockquote>
     * where:<ul>
     * <li><tt>dow</tt> is the day of the week (<tt>Sun, Mon, Tue, Wed,
     *     Thu, Fri, Sat</tt>).
     * <li><tt>mon</tt> is the month (<tt>Jan, Feb, Mar, Apr, May, Jun,
     *     Jul, Aug, Sep, Oct, Nov, Dec</tt>).
     * <li><tt>dd</tt> is the day of the month (<tt>01</tt> through
     *     <tt>31</tt>), as two decimal digits.
     * <li><tt>hh</tt> is the hour of the day (<tt>00</tt> through
     *     <tt>23</tt>), as two decimal digits.
     * <li><tt>mm</tt> is the minute within the hour (<tt>00</tt> through
     *     <tt>59</tt>), as two decimal digits.
     * <li><tt>ss</tt> is the second within the minute (<tt>00</tt> through
     *     <tt>61</tt>, as two decimal digits.
     * <li><tt>zzz</tt> is the time zone (and may reflect daylight saving
     *     time). Standard time zone abbreviations include those
     *     recognized by the method <tt>parse</tt>. If time zone
     *     information is not available, then <tt>zzz</tt> is empty -
     *     that is, it consists of no characters at all.
     * <li><tt>yyyy</tt> is the year, as four decimal digits.
     * </ul>
     *
     * @return  a string representation of this date.
     */
    public String toString() {
        return super.toString();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getDate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getDay() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getHours() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getMinutes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getMonth() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getSeconds() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getTimezoneOffset() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public int getYear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setDate(int date) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setHours(int hours) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setMinutes(int minutes) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setMonth(int month) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setSeconds(int seconds) {
        throw new UnsupportedOperationException();
    }

    /**
     * Since this class represents an unmodifiable date, calling
     * this method results in an exception.
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setTime(long time) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public void setYear(int year) {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public String toGMTString() {
        throw new UnsupportedOperationException();
    }

    /**
     * Do not call this method.
     * This method is deprecated in the base class.
     * @deprecated
     * @throws UnsupportedOperationException when the method is called.
     */
    public String toLocaleString() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the <code>Date</code> of Dec-31, 9999.
     * @return the <code>Date</code> of Dec-31, 9999.
     */
    private static Date getEndOfTime() {
        try {
            return (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).parse("12/31/9999 23:59:59");
        } catch (ParseException pe) {
            pe.printStackTrace();
            return null;
        }
    }
}
