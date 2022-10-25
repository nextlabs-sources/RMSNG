
package com.nextlabs.rms.destiny.services.management.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for CommProfileDTOList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CommProfileDTOList"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://bluejungle.com/destiny/services/management/types}BaseProfileDTOList"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="commProfileDTO" type="{http://bluejungle.com/destiny/services/management/types}CommProfileDTO" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommProfileDTOList", propOrder = {
    "commProfileDTO"
})
public class CommProfileDTOList
        extends BaseProfileDTOList {

    protected List<CommProfileDTO> commProfileDTO;

    /**
     * Gets the value of the commProfileDTO property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the commProfileDTO property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCommProfileDTO().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CommProfileDTO }
     * 
     * 
     */
    public List<CommProfileDTO> getCommProfileDTO() {
        if (commProfileDTO == null) {
            commProfileDTO = new ArrayList<CommProfileDTO>();
        }
        return this.commProfileDTO;
    }

}
