
package com.nextlabs.rms.destiny.services.management.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * A list of ActivityJouranlingSettingsDTO instances
 * 
 * <p>Java class for ActivityJournalingSettingsDTOList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivityJournalingSettingsDTOList"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="activityJournalingSettings" type="{http://bluejungle.com/destiny/services/management/types}ActivityJournalingSettingsDTO" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActivityJournalingSettingsDTOList", propOrder = {
    "activityJournalingSettings"
})
public class ActivityJournalingSettingsDTOList {

    protected List<ActivityJournalingSettingsDTO> activityJournalingSettings;

    /**
     * Gets the value of the activityJournalingSettings property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the activityJournalingSettings property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getActivityJournalingSettings().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ActivityJournalingSettingsDTO }
     * 
     * 
     */
    public List<ActivityJournalingSettingsDTO> getActivityJournalingSettings() {
        if (activityJournalingSettings == null) {
            activityJournalingSettings = new ArrayList<ActivityJournalingSettingsDTO>();
        }
        return this.activityJournalingSettings;
    }

}
