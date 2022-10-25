
package com.nextlabs.rms.destiny.services.policy.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>Java class for DeploymentRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DeploymentRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;element name="policyUsers" type="{http://bluejungle.com/destiny/services/policy/types}SystemUser" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element name="userSubjectTypes" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;/choice&gt;
 *         &lt;element name="agentCapabilities" type="{http://bluejungle.com/destiny/services/policy/types}AgentCapability" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="agentHost" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="agentType" use="required" type="{http://bluejungle.com/destiny/services/policy/types}AgentTypeEnum" /&gt;
 *       &lt;attribute name="timestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeploymentRequest", propOrder = {
    "policyUsers",
    "userSubjectTypes",
    "agentCapabilities"
})
public class DeploymentRequest {

    protected List<SystemUser> policyUsers;
    protected List<String> userSubjectTypes;
    @XmlSchemaType(name = "string")
    protected List<AgentCapability> agentCapabilities;
    @XmlAttribute(name = "agentHost", required = true)
    protected String agentHost;
    @XmlAttribute(name = "agentType", required = true)
    protected AgentTypeEnum agentType;
    @XmlAttribute(name = "timestamp")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timestamp;

    /**
     * Gets the value of the policyUsers property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the policyUsers property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPolicyUsers().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SystemUser }
     * 
     * 
     */
    public List<SystemUser> getPolicyUsers() {
        if (policyUsers == null) {
            policyUsers = new ArrayList<SystemUser>();
        }
        return this.policyUsers;
    }

    public void setPolicyUsers(SystemUser[] policyUsers) {
        this.policyUsers = Arrays.asList(policyUsers);
    }

    /**
     * Gets the value of the userSubjectTypes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the userSubjectTypes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUserSubjectTypes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getUserSubjectTypes() {
        if (userSubjectTypes == null) {
            userSubjectTypes = new ArrayList<String>();
        }
        return this.userSubjectTypes;
    }

    public void setUserSubjectTypes(String[] userSubjectTypes) {
        this.userSubjectTypes = Arrays.asList(userSubjectTypes);
    }

    /**
     * Gets the value of the agentCapabilities property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the agentCapabilities property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAgentCapabilities().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AgentCapability }
     * 
     * 
     */
    public List<AgentCapability> getAgentCapabilities() {
        if (agentCapabilities == null) {
            agentCapabilities = new ArrayList<AgentCapability>();
        }
        return this.agentCapabilities;
    }

    public void setAgentCapabilities(AgentCapability[] agentCapabilities) {
        this.agentCapabilities = Arrays.asList(agentCapabilities);
    }

    /**
     * Gets the value of the agentHost property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAgentHost() {
        return agentHost;
    }

    /**
     * Sets the value of the agentHost property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAgentHost(String value) {
        this.agentHost = value;
    }

    /**
     * Gets the value of the agentType property.
     * 
     * @return
     *     possible object is
     *     {@link AgentTypeEnum }
     *     
     */
    public AgentTypeEnum getAgentType() {
        return agentType;
    }

    /**
     * Sets the value of the agentType property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentTypeEnum }
     *     
     */
    public void setAgentType(AgentTypeEnum value) {
        this.agentType = value;
    }

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

}
