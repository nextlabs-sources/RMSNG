
package com.nextlabs.rms.destiny.services.management.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentProfileDTOQueryTermSet complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentProfileDTOQueryTermSet"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="agentProfileDTOQueryTerm" type="{http://bluejungle.com/destiny/services/management/types}AgentProfileDTOQueryTerm" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentProfileDTOQueryTermSet", propOrder = {
    "agentProfileDTOQueryTerm"
})
public class AgentProfileDTOQueryTermSet {

    @XmlElement(required = true)
    protected List<AgentProfileDTOQueryTerm> agentProfileDTOQueryTerm;

    /**
     * Gets the value of the agentProfileDTOQueryTerm property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the agentProfileDTOQueryTerm property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAgentProfileDTOQueryTerm().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AgentProfileDTOQueryTerm }
     * 
     * 
     */
    public List<AgentProfileDTOQueryTerm> getAgentProfileDTOQueryTerm() {
        if (agentProfileDTOQueryTerm == null) {
            agentProfileDTOQueryTerm = new ArrayList<AgentProfileDTOQueryTerm>();
        }
        return this.agentProfileDTOQueryTerm;
    }

}
