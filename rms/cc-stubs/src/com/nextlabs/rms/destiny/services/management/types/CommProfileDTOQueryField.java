
package com.nextlabs.rms.destiny.services.management.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for CommProfileDTOQueryField.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CommProfileDTOQueryField"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="id"/&gt;
 *     &lt;enumeration value="createdDate"/&gt;
 *     &lt;enumeration value="modifiedDate"/&gt;
 *     &lt;enumeration value="name"/&gt;
 *     &lt;enumeration value="agentType"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "CommProfileDTOQueryField")
@XmlEnum
public enum CommProfileDTOQueryField {

    @XmlEnumValue("id")
    ID("id"),
    @XmlEnumValue("createdDate")
    CREATED_DATE("createdDate"),
    @XmlEnumValue("modifiedDate")
    MODIFIED_DATE("modifiedDate"),
    @XmlEnumValue("name")
    NAME("name"),
    @XmlEnumValue("agentType")
    AGENT_TYPE("agentType");

    private final String value;

    CommProfileDTOQueryField(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CommProfileDTOQueryField fromValue(String v) {
        for (CommProfileDTOQueryField c : CommProfileDTOQueryField.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
