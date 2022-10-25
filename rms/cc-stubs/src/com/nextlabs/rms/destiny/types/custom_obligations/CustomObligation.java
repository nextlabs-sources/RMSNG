
package com.nextlabs.rms.destiny.types.custom_obligations;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for CustomObligation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CustomObligation"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DisplayName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="RunAt" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="RunBy" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="InvocationString" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomObligation", propOrder = {
    "displayName",
    "runAt",
    "runBy",
    "invocationString"
})
public class CustomObligation {

    @XmlElement(name = "DisplayName", required = true)
    protected String displayName;
    @XmlElement(name = "RunAt", required = true)
    protected String runAt;
    @XmlElement(name = "RunBy", required = true)
    protected String runBy;
    @XmlElement(name = "InvocationString", required = true)
    protected String invocationString;

    /**
     * Gets the value of the displayName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the value of the displayName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisplayName(String value) {
        this.displayName = value;
    }

    /**
     * Gets the value of the runAt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRunAt() {
        return runAt;
    }

    /**
     * Sets the value of the runAt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRunAt(String value) {
        this.runAt = value;
    }

    /**
     * Gets the value of the runBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRunBy() {
        return runBy;
    }

    /**
     * Sets the value of the runBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRunBy(String value) {
        this.runBy = value;
    }

    /**
     * Gets the value of the invocationString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInvocationString() {
        return invocationString;
    }

    /**
     * Sets the value of the invocationString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInvocationString(String value) {
        this.invocationString = value;
    }

}
