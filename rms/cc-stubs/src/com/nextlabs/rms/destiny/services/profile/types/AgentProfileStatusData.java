
package com.nextlabs.rms.destiny.services.profile.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>Java class for AgentProfileStatusData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentProfileStatusData"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="lastCommittedAgentProfileName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="lastCommittedAgentProfileTimestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *         &lt;element name="lastCommittedCommProfileName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="lastCommittedCommProfileTimestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentProfileStatusData", propOrder = {
    "lastCommittedAgentProfileName",
    "lastCommittedAgentProfileTimestamp",
    "lastCommittedCommProfileName",
    "lastCommittedCommProfileTimestamp"
})
public class AgentProfileStatusData {

    protected String lastCommittedAgentProfileName;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastCommittedAgentProfileTimestamp;
    protected String lastCommittedCommProfileName;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastCommittedCommProfileTimestamp;

    /**
     * Gets the value of the lastCommittedAgentProfileName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLastCommittedAgentProfileName() {
        return lastCommittedAgentProfileName;
    }

    /**
     * Sets the value of the lastCommittedAgentProfileName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLastCommittedAgentProfileName(String value) {
        this.lastCommittedAgentProfileName = value;
    }

    /**
     * Gets the value of the lastCommittedAgentProfileTimestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getLastCommittedAgentProfileTimestamp() {
        return lastCommittedAgentProfileTimestamp;
    }

    /**
     * Sets the value of the lastCommittedAgentProfileTimestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setLastCommittedAgentProfileTimestamp(XMLGregorianCalendar value) {
        this.lastCommittedAgentProfileTimestamp = value;
    }

    /**
     * Gets the value of the lastCommittedCommProfileName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLastCommittedCommProfileName() {
        return lastCommittedCommProfileName;
    }

    /**
     * Sets the value of the lastCommittedCommProfileName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLastCommittedCommProfileName(String value) {
        this.lastCommittedCommProfileName = value;
    }

    /**
     * Gets the value of the lastCommittedCommProfileTimestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getLastCommittedCommProfileTimestamp() {
        return lastCommittedCommProfileTimestamp;
    }

    /**
     * Sets the value of the lastCommittedCommProfileTimestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setLastCommittedCommProfileTimestamp(XMLGregorianCalendar value) {
        this.lastCommittedCommProfileTimestamp = value;
    }

}
