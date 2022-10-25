
package com.nextlabs.rms.destiny.services.management.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for CommProfileDTOQueryTerm complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CommProfileDTOQueryTerm"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="commProfileDTOQueryField" type="{http://bluejungle.com/destiny/services/management/types}CommProfileDTOQueryField"/&gt;
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
@XmlType(name = "CommProfileDTOQueryTerm", propOrder = {
    "commProfileDTOQueryField",
    "value"
})
public class CommProfileDTOQueryTerm {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected CommProfileDTOQueryField commProfileDTOQueryField;
    @XmlElement(required = true)
    protected Object value;

    /**
     * Gets the value of the commProfileDTOQueryField property.
     * 
     * @return
     *     possible object is
     *     {@link CommProfileDTOQueryField }
     *     
     */
    public CommProfileDTOQueryField getCommProfileDTOQueryField() {
        return commProfileDTOQueryField;
    }

    /**
     * Sets the value of the commProfileDTOQueryField property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommProfileDTOQueryField }
     *     
     */
    public void setCommProfileDTOQueryField(CommProfileDTOQueryField value) {
        this.commProfileDTOQueryField = value;
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
