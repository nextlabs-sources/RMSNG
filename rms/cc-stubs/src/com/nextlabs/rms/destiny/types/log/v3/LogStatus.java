package com.nextlabs.rms.destiny.types.log.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for LogStatus.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LogStatus"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Success"/&gt;
 *     &lt;enumeration value="Failed"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 *
 */
@XmlType(name = "LogStatus", namespace = "http://nextlabs.com/destiny/types/log/v3")
@XmlEnum
public enum LogStatus {

    @XmlEnumValue("Success")
    SUCCESS("Success"),
    @XmlEnumValue("Failed")
    FAILED("Failed");

    private final String value;

    LogStatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LogStatus fromValue(String v) {
        for (LogStatus c : LogStatus.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
