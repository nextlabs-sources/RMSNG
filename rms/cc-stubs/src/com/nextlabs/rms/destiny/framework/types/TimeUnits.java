
package com.nextlabs.rms.destiny.framework.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for time-units.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="time-units"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="days"/&gt;
 *     &lt;enumeration value="hours"/&gt;
 *     &lt;enumeration value="minutes"/&gt;
 *     &lt;enumeration value="seconds"/&gt;
 *     &lt;enumeration value="milliseconds"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "time-units")
@XmlEnum
public enum TimeUnits {

    @XmlEnumValue("days")
    DAYS("days"),
    @XmlEnumValue("hours")
    HOURS("hours"),
    @XmlEnumValue("minutes")
    MINUTES("minutes"),
    @XmlEnumValue("seconds")
    SECONDS("seconds"),
    @XmlEnumValue("milliseconds")
    MILLISECONDS("milliseconds");

    private final String value;

    TimeUnits(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TimeUnits fromValue(String v) {
        for (TimeUnits c : TimeUnits.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
