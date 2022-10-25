
package com.nextlabs.rms.destiny.services.policy.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentTypeEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AgentTypeEnum"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="DESKTOP"/&gt;
 *     &lt;enumeration value="FILE_SERVER"/&gt;
 *     &lt;enumeration value="PORTAL"/&gt;
 *     &lt;enumeration value="ACTIVE_DIRECTORY"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AgentTypeEnum")
@XmlEnum
public enum AgentTypeEnum {

    DESKTOP,
    FILE_SERVER,
    PORTAL,
    ACTIVE_DIRECTORY;

    public String value() {
        return name();
    }

    public static AgentTypeEnum fromValue(String v) {
        return valueOf(v);
    }

}
