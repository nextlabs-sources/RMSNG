
package com.nextlabs.rms.destiny.services.agent.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentStartupAttachments.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AgentStartupAttachments"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="AgentKeyStore"/&gt;
 *     &lt;enumeration value="AgentTrustStore"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AgentStartupAttachments")
@XmlEnum
public enum AgentStartupAttachments {

    @XmlEnumValue("AgentKeyStore")
    AGENT_KEY_STORE("AgentKeyStore"),
    @XmlEnumValue("AgentTrustStore")
    AGENT_TRUST_STORE("AgentTrustStore");

    private final String value;

    AgentStartupAttachments(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AgentStartupAttachments fromValue(String v) {
        for (AgentStartupAttachments c : AgentStartupAttachments.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
