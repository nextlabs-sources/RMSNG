
package com.nextlabs.rms.destiny.services.management.types;

import com.nextlabs.rms.destiny.domain.types.ActionTypeDTOList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * 				Journaling settings specifying what actions will lead to log entries
 * 			
 * 
 * <p>Java class for ActivityJournalingSettingsDTO complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivityJournalingSettingsDTO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="loggedActivities" type="{http://bluejungle.com/domain/types}ActionTypeDTOList"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActivityJournalingSettingsDTO", propOrder = {
    "name",
    "loggedActivities"
})
public class ActivityJournalingSettingsDTO {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(required = true)
    protected ActionTypeDTOList loggedActivities;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the loggedActivities property.
     * 
     * @return
     *     possible object is
     *     {@link ActionTypeDTOList }
     *     
     */
    public ActionTypeDTOList getLoggedActivities() {
        return loggedActivities;
    }

    /**
     * Sets the value of the loggedActivities property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActionTypeDTOList }
     *     
     */
    public void setLoggedActivities(ActionTypeDTOList value) {
        this.loggedActivities = value;
    }

}
