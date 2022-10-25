
package com.nextlabs.rms.destiny.services.management.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentProfileDTOList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentProfileDTOList"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://bluejungle.com/destiny/services/management/types}BaseProfileDTOList"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="agentProfileDTO" type="{http://bluejungle.com/destiny/services/management/types}AgentProfileDTO" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentProfileDTOList", propOrder = {
    "agentProfileDTO"
})
public class AgentProfileDTOList
        extends BaseProfileDTOList {

    protected List<AgentProfileDTO> agentProfileDTO;

    /**
     * Gets the value of the agentProfileDTO property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the agentProfileDTO property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAgentProfileDTO().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AgentProfileDTO }
     * 
     * 
     */
    public List<AgentProfileDTO> getAgentProfileDTO() {
        if (agentProfileDTO == null) {
            agentProfileDTO = new ArrayList<AgentProfileDTO>();
        }
        return this.agentProfileDTO;
    }

}
