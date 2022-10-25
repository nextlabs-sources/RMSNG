
package com.nextlabs.rms.destiny.services.management.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentProfileDTOQueryTerm complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentProfileDTOQueryTerm"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="agentProfileDTOQueryField" type="{http://bluejungle.com/destiny/services/management/types}AgentProfileDTOQueryField"/&gt;
 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}anyType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentProfileDTOQueryTerm", propOrder = {
    "agentProfileDTOQueryField",
    "value"
})
public class AgentProfileDTOQueryTerm {

    @XmlElement(required = true)
    protected AgentProfileDTOQueryField agentProfileDTOQueryField;
    @XmlElement(required = true)
    protected Object value;

    /**
     * Gets the value of the agentProfileDTOQueryField property.
     * 
     * @return
     *     possible object is
     *     {@link AgentProfileDTOQueryField }
     *     
     */
    public AgentProfileDTOQueryField getAgentProfileDTOQueryField() {
        return agentProfileDTOQueryField;
    }

    /**
     * Sets the value of the agentProfileDTOQueryField property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentProfileDTOQueryField }
     *     
     */
    public void setAgentProfileDTOQueryField(AgentProfileDTOQueryField value) {
        this.agentProfileDTOQueryField = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setValue(Object value) {
        this.value = value;
    }

}
