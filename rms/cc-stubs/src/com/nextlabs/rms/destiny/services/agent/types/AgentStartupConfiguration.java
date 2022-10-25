
package com.nextlabs.rms.destiny.services.agent.types;

import com.nextlabs.rms.destiny.services.management.types.AgentProfileDTO;
import com.nextlabs.rms.destiny.services.management.types.CommProfileDTO;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentStartupConfiguration complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentStartupConfiguration"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="id" type="{http://bluejungle.com/destiny/framework/types}ID"/&gt;
 *         &lt;element name="commProfile" type="{http://bluejungle.com/destiny/services/management/types}CommProfileDTO"/&gt;
 *         &lt;element name="agentProfile" type="{http://bluejungle.com/destiny/services/management/types}AgentProfileDTO"/&gt;
 *         &lt;element name="registrationId" type="{http://bluejungle.com/destiny/framework/types}ID"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentStartupConfiguration", propOrder = {
    "id",
    "commProfile",
    "agentProfile",
    "registrationId"
})
public class AgentStartupConfiguration {

    @XmlElement(required = true)
    protected BigInteger id;
    @XmlElement(required = true)
    protected CommProfileDTO commProfile;
    @XmlElement(required = true)
    protected AgentProfileDTO agentProfile;
    @XmlElement(required = true)
    protected BigInteger registrationId;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setId(BigInteger value) {
        this.id = value;
    }

    /**
     * Gets the value of the commProfile property.
     * 
     * @return
     *     possible object is
     *     {@link CommProfileDTO }
     *     
     */
    public CommProfileDTO getCommProfile() {
        return commProfile;
    }

    /**
     * Sets the value of the commProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommProfileDTO }
     *     
     */
    public void setCommProfile(CommProfileDTO value) {
        this.commProfile = value;
    }

    /**
     * Gets the value of the agentProfile property.
     * 
     * @return
     *     possible object is
     *     {@link AgentProfileDTO }
     *     
     */
    public AgentProfileDTO getAgentProfile() {
        return agentProfile;
    }

    /**
     * Sets the value of the agentProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentProfileDTO }
     *     
     */
    public void setAgentProfile(AgentProfileDTO value) {
        this.agentProfile = value;
    }

    /**
     * Gets the value of the registrationId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRegistrationId() {
        return registrationId;
    }

    /**
     * Sets the value of the registrationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRegistrationId(BigInteger value) {
        this.registrationId = value;
    }

}
