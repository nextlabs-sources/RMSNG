
package com.nextlabs.rms.destiny.domain.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for ActionTypeDTO.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ActionTypeDTO"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="OPEN"/&gt;
 *     &lt;enumeration value="EDIT"/&gt;
 *     &lt;enumeration value="DELETE"/&gt;
 *     &lt;enumeration value="CHANGE_ATTRIBUTES"/&gt;
 *     &lt;enumeration value="CHANGE_SECURITY"/&gt;
 *     &lt;enumeration value="PASTE"/&gt;
 *     &lt;enumeration value="MOVE"/&gt;
 *     &lt;enumeration value="RENAME"/&gt;
 *     &lt;enumeration value="COPY"/&gt;
 *     &lt;enumeration value="EMBED"/&gt;
 *     &lt;enumeration value="EMAIL"/&gt;
 *     &lt;enumeration value="IM"/&gt;
 *     &lt;enumeration value="PRINT"/&gt;
 *     &lt;enumeration value="STOP_AGENT"/&gt;
 *     &lt;enumeration value="START_AGENT"/&gt;
 *     &lt;enumeration value="AGENT_USER_LOGIN"/&gt;
 *     &lt;enumeration value="AGENT_USER_LOGOUT"/&gt;
 *     &lt;enumeration value="ACCESS_AGENT_CONFIG"/&gt;
 *     &lt;enumeration value="ACCESS_AGENT_LOGS"/&gt;
 *     &lt;enumeration value="ACCESS_AGENT_BINARIES"/&gt;
 *     &lt;enumeration value="ACCESS_AGENT_BUNDLE"/&gt;
 *     &lt;enumeration value="ABNORMAL_AGENT_SHUTDOWN"/&gt;
 *     &lt;enumeration value="INVALID_BUNDLE"/&gt;
 *     &lt;enumeration value="BUNDLE_RECEIVED"/&gt;
 *     &lt;enumeration value="EXPORT"/&gt;
 *     &lt;enumeration value="ATTACH"/&gt;
 *     &lt;enumeration value="RUN"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ActionTypeDTO")
@XmlEnum
public enum ActionTypeDTO {

    OPEN,
    EDIT,
    DELETE,
    CHANGE_ATTRIBUTES,
    CHANGE_SECURITY,
    PASTE,
    MOVE,
    RENAME,
    COPY,
    EMBED,
    EMAIL,
    IM,
    PRINT,
    STOP_AGENT,
    START_AGENT,
    AGENT_USER_LOGIN,
    AGENT_USER_LOGOUT,
    ACCESS_AGENT_CONFIG,
    ACCESS_AGENT_LOGS,
    ACCESS_AGENT_BINARIES,
    ACCESS_AGENT_BUNDLE,
    ABNORMAL_AGENT_SHUTDOWN,
    INVALID_BUNDLE,
    BUNDLE_RECEIVED,
    EXPORT,
    ATTACH,
    RUN;

    public String value() {
        return name();
    }

    public static ActionTypeDTO fromValue(String v) {
        return valueOf(v);
    }

}
