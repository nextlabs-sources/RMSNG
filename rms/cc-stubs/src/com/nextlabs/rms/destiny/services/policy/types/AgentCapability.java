
package com.nextlabs.rms.destiny.services.policy.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentCapability.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AgentCapability"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="FILESYSTEM"/&gt;
 *     &lt;enumeration value="PORTAL"/&gt;
 *     &lt;enumeration value="EMAIL"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AgentCapability")
@XmlEnum
public enum AgentCapability {

    FILESYSTEM,
    PORTAL,
    EMAIL;

    public String value() {
        return name();
    }

    public static AgentCapability fromValue(String v) {
        return valueOf(v);
    }

}
