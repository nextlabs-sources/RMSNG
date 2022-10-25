
package com.nextlabs.rms.destiny.services.policy.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for SystemUser complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SystemUser"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="userSubjectType" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="systemId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SystemUser", propOrder = {
    "userSubjectType",
    "systemId"
})
public class SystemUser {

    @XmlElement(required = true)
    protected String userSubjectType;
    @XmlElement(required = true)
    protected String systemId;

    /**
     * Gets the value of the userSubjectType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserSubjectType() {
        return userSubjectType;
    }

    /**
     * Sets the value of the userSubjectType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserSubjectType(String value) {
        this.userSubjectType = value;
    }

    /**
     * Gets the value of the systemId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the value of the systemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSystemId(String value) {
        this.systemId = value;
    }

}
